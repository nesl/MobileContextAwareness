package edu.ucla.nesl.mca.classifier;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import edu.ucla.nesl.mca.ClassifierHandle;
import edu.ucla.nesl.mca.DataRequest;
import edu.ucla.nesl.mca.McaService;
import edu.ucla.nesl.mca.TypeManager;
import edu.ucla.nesl.mca.sensor.BasicDataService;

public abstract class Classifier {
    // Currently only support multiple in, single out
    private static final String TAG = "Classifier";
    
    public final String Name;
    public final String Type;
    public final int OutputType;
    public final ArrayList<String> OutputSet;
    
    protected ArrayList<DataRequest> inputRequests;
    protected McaService homeService;
    protected ClassifierHandle handle;

    protected ArrayList<String> inputRequestIDs;
    protected ArrayList<Object[]> inputValues;
    
    private long nextEvalTime = 0;  // in millisecond
    private int evaluationRate = 0; // in millisecond
    
    private final Object evaluationLock = new Object();
    private volatile boolean stopEvaluation = true;
    private volatile boolean suspendEvaluation = true;
    
    public Classifier(JSONObject modelObj, McaService homeService) throws JSONException {
        Name = modelObj.getString("Name");
        Type = modelObj.getString("Type");
        
        String outputTypeString = modelObj.getString("ResultType");
        if (outputTypeString.equals("Double")) {
            OutputType = TypeManager.TYPE_DOUBLE;
            OutputSet = null;
        } else if (outputTypeString.equals("String")) {
            OutputType = TypeManager.TYPE_STRING;
            OutputSet = null;
        } else if (outputTypeString.equals("Set")) {
            JSONArray res = modelObj.getJSONArray("ResultSet");
            OutputType = TypeManager.TYPE_ENUM_BASE + res.length();
            OutputSet = new ArrayList<String>(res.length());
            for (int j = 0; j < res.length(); j++) {
                OutputSet.add(res.getString(j));
            }
        } else {
            throw new JSONException("Model has Unrecognized ResultType: " + outputTypeString);
        }
        
        inputRequests = new ArrayList<DataRequest>();
        this.homeService = homeService;
        
        inputRequestIDs = new ArrayList<String>();
        inputValues = new ArrayList<Object[]>();

        stopEvaluation = false;
        evaluationThread.start();
    }

    public void setHandle(ClassifierHandle handle) {
        this.handle = handle;        
    }

	public static String getJSONModelType(JSONObject modelObj) throws JSONException {
        return modelObj.getString("Type");
	}

    private final Thread evaluationThread = new Thread() {
        @Override
        public void run() {            
            while (!stopEvaluation) {
                try {
                    if (suspendEvaluation) {
                        synchronized (evaluationLock) {
                            evaluationLock.wait();
                        }
                    } else {
                        if (nextEvalTime <= System.currentTimeMillis()) {
                            Object result = doEvaluation();
                            Log.d(TAG, Name + " result: " + result);
                            
                            if (result != null) {
                                homeService.reportData(handle, result);
                            }
                            nextEvalTime = System.currentTimeMillis() + evaluationRate;
                        } 
                        synchronized (evaluationLock) {
                            evaluationLock.wait(nextEvalTime - System.currentTimeMillis());
                        }
                    }
                }
                catch (InterruptedException e)
                {
                    Log.d(TAG, "evaluationThread Interrupted");
                }
            }
        }
    };
    
    private boolean getInputs() {
        inputValues.clear();
        for (int i = 0; i < inputRequests.size(); i++) {
            BasicDataService sensor = homeService.getSensorManager().getSensor(
                    inputRequests.get(i).getSensorName());
            Object[] objs = sensor.getData(inputRequestIDs.get(i));
            if (objs == null)
                return false;
            inputValues.add(objs);
        }
        return true;
    }
    
    public void activate(int evaluationRate) {
        Log.d(TAG, "activate");
        
        this.evaluationRate = evaluationRate;
        inputRequestIDs.clear();
        for (int i = 0; i < inputRequests.size(); i++) {
            DataRequest request = inputRequests.get(i);
            BasicDataService sensor = homeService.getSensorManager().getSensor(
                    request.getSensorName());
            inputRequestIDs.add(sensor.register(request));
        }
        if (evaluationRate > 0) {
            suspendEvaluation = false;
            synchronized (evaluationLock) {
                evaluationLock.notifyAll();
            }
        }
    }
    
    public void deactivate() {
        Log.d(TAG, "deactivate");
        
        for (int i = 0; i < inputRequests.size(); i++) {
            BasicDataService sensor = homeService.getSensorManager().getSensor(
                    inputRequests.get(i).getSensorName());
            sensor.unregister(inputRequestIDs.get(i));
        }
        inputRequestIDs.clear();
        suspendEvaluation = true;
    }
    
    public void destroy() {
        if (!suspendEvaluation)
            deactivate();
        
        stopEvaluation = true;
        evaluationThread.interrupt();
    }
    
    public Object doEvaluation() {
        if (getInputs())
            return evaluate();
        else
            return null;
    }
    
    public boolean isActivated() {
        return !suspendEvaluation;
    }
    
    protected abstract Object evaluate();
}
