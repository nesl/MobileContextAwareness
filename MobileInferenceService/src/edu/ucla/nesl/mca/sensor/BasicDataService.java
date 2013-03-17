package edu.ucla.nesl.mca.sensor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

import edu.ucla.nesl.mca.DataRequest;
import edu.ucla.nesl.mca.McaService;
import edu.ucla.nesl.mca.feature.Feature;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public abstract class BasicDataService extends Service {

    private static final String TAG = "BasicDataService";
    
    public final String Name;
    public final ArrayList<Integer> OutputTypes;
    public final ArrayList<String> Components;
    protected final ArrayList<LinkedBlockingDeque<TimestampedObject>> sensorDataQueues;

    protected McaService homeService;
    
    protected ConcurrentMap<String, DataRequest> requestMap = 
            new ConcurrentHashMap<String, DataRequest>();
    
    protected ConcurrentMap<String, ArrayBlockingQueue<Object>> requestDataArrayMap = 
            new ConcurrentHashMap<String, ArrayBlockingQueue<Object>>();
    
    protected PriorityBlockingQueue<DataRequest> evaluationQueue = 
            new PriorityBlockingQueue<DataRequest>();
    protected final Object evaluationLock = new Object();
    
    private final IBinder mBinder = new BasicDataBinder();
    
    private int sensorDataMaxAgeToKeep = 0;         // in millisecond
    protected int sensorSampleRate = 0;      // in microsecond
    private volatile boolean stopEvaluation = true;

    public class BasicDataBinder extends Binder {
        public BasicDataService getService() {
            return BasicDataService.this;
        }
    }
    
    public BasicDataService (String name, String[] components, int[] outputTypes) {
        this.Name = name;
        Components = new ArrayList<String>(components.length);
        OutputTypes = new ArrayList<Integer>(components.length);
        sensorDataQueues = new ArrayList<LinkedBlockingDeque<TimestampedObject>>(components.length);
        for (int i = 0; i < components.length; i++) {
            Components.add(i, components[i]);
            // if outputTypes has less elements, auto-fill with the last
            if (i < outputTypes.length)
                OutputTypes.add(i, outputTypes[i]);
            else
                OutputTypes.add(i, outputTypes[outputTypes.length - 1]);
            sensorDataQueues.add(new LinkedBlockingDeque<TimestampedObject>());
        }
    }
    

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        
        return mBinder;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        stopEvaluation = false;
        evaluationThread.start();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        stopEvaluation = true;
        evaluationThread.interrupt();
    }
    
    public abstract boolean isAvailable();
    
    public void setHomeService(McaService homeService) {
        this.homeService = homeService;
    }
    
    /**
     * Register the specific Feature request. If the request already registered, 
     * this function will see it as a success
     * @param request Request to be registered
     * @return the Identifier for the request, null if failed
     */
    public String register(DataRequest request) {
        Log.d(TAG, "register: " + request.toString());
        
        // Error checking
        if (!request.getSensorName().equals(Name)) return null;
        if (!isAvailable()) return null;

        // If the request already included, ignore the register call
        String requestID = request.getIdentifier();
        if (requestMap.containsKey(requestID)) return requestID;
        
        // Add the request
        int dataArraySize = 1;
        if (request.getWindowSize() > 0 && request.getEvalRate() > 0) {
            dataArraySize = request.getEvalRate() / request.getWindowSize();
        }
        requestMap.put(requestID, request);
        requestDataArrayMap.put(requestID, 
                new ArrayBlockingQueue<Object>(dataArraySize));
        if (request.getEvalRate() > 0) {
            request.setNextEvalTime(0);
            synchronized (evaluationLock) {
                evaluationQueue.add(request);
                evaluationLock.notifyAll();
            }
        }
        adjustRates();
        onRequestMapChange();
        return requestID;
    }
    
    public void unregister(String requestID) {
        if (requestMap.containsKey(requestID)) {
            DataRequest request = requestMap.get(requestID);
            requestMap.remove(requestID);
            requestDataArrayMap.remove(requestID);
            if (request.getEvalRate() > 0) {
                synchronized (evaluationLock) {
                    evaluationQueue.remove(request);
                    evaluationLock.notifyAll();
                }
            }
            adjustRates();
            onRequestMapChange();
        }
    }
    
    public int getRequestCount() {
        return requestMap.size();
    }
    
    private void adjustRates() {
        Log.d(TAG, "adjustRates");
        //long oldMaxAgeToKeep = mMaxAgeToKeep;
        //long oldEvaluationRate = mEvaluationRate;
        sensorDataMaxAgeToKeep = 0;
        sensorSampleRate = Integer.MAX_VALUE;
        for (DataRequest request : requestMap.values()) {
            if (request.getWindowSize() > sensorDataMaxAgeToKeep) {
                sensorDataMaxAgeToKeep = request.getWindowSize();
            }
            if (request.getSampleRate() < sensorSampleRate) {
                sensorSampleRate = request.getSampleRate();
            }
        }
    }
    
    protected abstract void onRequestMapChange();
    
    public Object[] getData(String requestID) {
        Log.d(TAG, "getData: " + requestID);
        if (!requestMap.containsKey(requestID)) return null;
        
        DataRequest request = requestMap.get(requestID);
        
        // check if explicit evaluation is required
        if (request.getEvalRate() == 0) {
            doEvaluation(request);
        }
        
        // check data availability
        ArrayBlockingQueue<Object> requestDataQueue = requestDataArrayMap.get(requestID);
        if (requestDataQueue.remainingCapacity() > 0) {
            // not enough data
            return null;
        }
        
        // get data
        return requestDataQueue.toArray();
    }
    
    protected void removeOldData() {
        for (LinkedBlockingDeque<TimestampedObject> sensorDataQueue : sensorDataQueues) {
            synchronized (sensorDataQueue) {
                if (sensorDataMaxAgeToKeep == 0) {
                    // special case: only keep the latest one
                    while(sensorDataQueue.size() > 1) {
                        sensorDataQueue.remove();
                    }
                } else {
                    while(!sensorDataQueue.isEmpty()) {
                        TimestampedObject first = sensorDataQueue.peek();
                        if (first.getAge() < sensorDataMaxAgeToKeep)
                            break;
                        sensorDataQueue.remove();
                    }
                }
            }
        }
    }
    
    protected List<Object> convertSampleRate(DataRequest request, 
            List<TimestampedObject> input) {
        // TODO: improve interpolation method, currently no!!!!
        List<Object> result = new LinkedList<Object>();
        for (TimestampedObject value : input) {
            result.add(value.getValue());
        }
        return result;
    }
    
    protected void doEvaluation(DataRequest request) {  
        Log.d(TAG, "doEvaluation" + request.toString());      
        String requestID = request.getIdentifier();
        if (!requestDataArrayMap.containsKey(requestID)) {
            // not registered, ignore
            Log.w(TAG, "doEvaluation has unregistered request");
            return;
        }

        Feature feature = homeService.getFeatureManager().getFeature(request);
        int compIndex = request.getSensorComponent();
        long windowSize = request.getWindowSize();
        LinkedBlockingDeque<TimestampedObject> sensorDataQueue = sensorDataQueues.get(compIndex);
        List<Object> input = new LinkedList<Object>();
        boolean careSampleRate = feature.careSampleRate();
        
        
        if (windowSize == 0) {
            // special case: always return the latest value
            synchronized (sensorDataQueue) {
                if (sensorDataQueue.size() > 0) {
                    input.add(sensorDataQueue.getLast().getValue());
                } else {
                    // not enough input value
                    return;
                }
            }
        } else {
            List<TimestampedObject> timeStampedInput = new LinkedList<TimestampedObject>();
            synchronized (sensorDataQueue) {
                if (sensorDataQueue.size() == 0) {
                    // not enough input value
                    return;
                }
                
                if (sensorDataQueue.peek().getAge() < windowSize - request.getSampleRate()) {
                    // not enough input value
                    return;
                }
                
                for(TimestampedObject value : sensorDataQueue) {
                    if (value.getAge() < windowSize) {
                        if (careSampleRate)
                            timeStampedInput.add(value);
                        else
                            input.add(value.getValue());
                    }
                }
            }
            if (careSampleRate) {
                input = convertSampleRate(request, timeStampedInput);
            }
        }

        Object result = feature.evaluate(input.toArray(), OutputTypes.get(compIndex), request.getFeatureConfig());
        
        ArrayBlockingQueue<Object> requestDataQueue = requestDataArrayMap.get(requestID);
        synchronized (requestDataQueue) {
            if (requestDataQueue.remainingCapacity() == 0)
                requestDataQueue.remove();
            requestDataQueue.add(result);
        }
        
    }

    
    private final Thread evaluationThread = new Thread() {
        @Override
        public void run() {            
            while (!stopEvaluation) {
                try {
                    DataRequest request = evaluationQueue.peek();
                    if (request == null) {
                        synchronized (evaluationLock) {
                            evaluationLock.wait();
                        }
                        continue;
                    }
                    
                    long nextEvalTime = request.getNextEvalTime();
                    if (nextEvalTime <= System.currentTimeMillis()) {
                        synchronized (evaluationLock) {
                            doEvaluation(request);
                            evaluationQueue.remove();
                            nextEvalTime = System.currentTimeMillis() + request.getEvalRate();
                            request.setNextEvalTime(nextEvalTime);
                            evaluationQueue.add(request);
                        }
                        continue;
                    }
                    
                    synchronized (evaluationLock) {
                        evaluationLock.wait(nextEvalTime - System.currentTimeMillis());
                    }
                }
                catch (InterruptedException e)
                {
                    Log.d(TAG, "evaluationThread Interrupted");
                }
            }
        }
    };

}
