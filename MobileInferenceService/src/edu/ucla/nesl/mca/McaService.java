package edu.ucla.nesl.mca;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import edu.ucla.nesl.mca.classifier.Classifier;
import edu.ucla.nesl.mca.classifier.ClassifierBuilder;
import edu.ucla.nesl.mca.feature.FeatureManager;
import edu.ucla.nesl.mca.sensor.SensorReaderManager;

public class McaService extends Service {
    private static final String TAG = "McaService";

    public static final int MSG_REGISTER = 1;
    public static final int MSG_ACTIVATE = 2;
    public static final int MSG_PAUSE = 3;
    public static final int MSG_DESTROY = 4;
    public static final int MSG_DATA = 5;
    public static final int MSG_STATUS = 6;
	
	private final HashMap<ClassifierHandle, Classifier> classifiers = 
	        new HashMap<ClassifierHandle, Classifier>();
    private final HashMap<ClassifierHandle, Messenger> callbackMessengers = 
            new HashMap<ClassifierHandle, Messenger>();
	private final SensorReaderManager sensorManager= new SensorReaderManager(this);
	private final FeatureManager featureManager = new FeatureManager(sensorManager);

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));
    
    /**
     * Handler of incoming messages from clients.
     */
	private static class IncomingHandler extends Handler {
        private final WeakReference<McaService> myService;

        IncomingHandler(McaService service) {
            myService = new WeakReference<McaService>(service);
        }
        
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: " + msg.what);
            McaService service = myService.get();
            switch (msg.what) {
            case MSG_REGISTER:
                ArrayList<ClassifierHandle> handles = service.register((Bundle)msg.getData());
                try {
                    Message reply = Message.obtain(null, MSG_REGISTER);
                    Bundle b = new Bundle();
                    b.putParcelableArrayList("Handles", handles);
                    reply.setData(b);
                    msg.replyTo.send(reply);
                    if (handles != null) {
                        for (ClassifierHandle handle : handles) {
                            service.callbackMessengers.put(handle, msg.replyTo);
                        }
                    }
                } catch (RemoteException e) {
                    for (ClassifierHandle handle : handles) {
                        service.classifiers.get(handle).destroy();
                    }
                }
                break;
            case MSG_ACTIVATE:
                service.activate((Bundle)msg.getData());
                break;
            case MSG_PAUSE:
                service.pause((Bundle)msg.getData());
                break;
            case MSG_DESTROY:
                service.destroy((Bundle)msg.getData());
                break;
            case MSG_STATUS:
                Bundle b = service.reportStatus();
                Message reply = Message.obtain(null, MSG_STATUS);
                reply.setData(b);
                try {
                    msg.replyTo.send(reply);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        sensorManager.init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        for (ClassifierHandle handle : classifiers.keySet()) {
            classifiers.get(handle).destroy();
        }
        sensorManager.cleanup();
    }

    private ArrayList<ClassifierHandle> register(Bundle bundle) {
        Log.d(TAG, "register");
        String fileName = bundle.getString("JSONFile"); // TODO: make "JSONFile" a constant
        
        // Get the JSON input file
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, fileName);
        try {
            /* Build the classifier */
            ArrayList<Classifier> cls = ClassifierBuilder.BuildFromFile(file, this);
            ArrayList<ClassifierHandle> handles = new ArrayList<ClassifierHandle>();
            for (Classifier cl : cls) {
                ClassifierHandle handle = new ClassifierHandle(cl.Name);
                if (classifiers.containsKey(handle)) {
                    classifiers.get(handle).destroy();
                }
                classifiers.put(handle, cl);
                cl.setHandle(handle);
                handles.add(handle);
            }
            return handles;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void activate(Bundle data) {
        Log.d(TAG, "activate");
        ClassifierHandle handle = (ClassifierHandle) data.getParcelable("Handle");
        if (handle == null) return;
        
        int evaluationRate = data.getInt("EvalRate", 0);
        
        if (classifiers.containsKey(handle)) {
            classifiers.get(handle).activate(evaluationRate);
        }
    }

    private void pause(Bundle data) {
        Log.d(TAG, "pause");
        ClassifierHandle handle = (ClassifierHandle) data.getParcelable("Handle");
        if (handle == null) return;
        
        if (classifiers.containsKey(handle)) {
            classifiers.get(handle).deactivate();
        }
    }

    private void destroy(Bundle data) {
        Log.d(TAG, "destroy");
        ClassifierHandle handle = (ClassifierHandle) data.getParcelable("Handle");
        if (handle == null) return;
        
        if (classifiers.containsKey(handle)) {
            classifiers.get(handle).destroy();
            classifiers.remove(handle);
            callbackMessengers.remove(handle);
        }
    }
    
    public void reportData(ClassifierHandle handle, Object data) {
        Log.d(TAG, "reportData:" + handle.toString() + ":" + data.toString());
        Bundle b = new Bundle();
        b.putParcelable("Handle", handle);
        
        Classifier cl = classifiers.get(handle);
        if (cl.OutputType == TypeManager.TYPE_DOUBLE) {
            b.putDouble("Data", (Double) data);
        } else if (cl.OutputType == TypeManager.TYPE_STRING) {
            b.putString("Data", (String) data);
        } else if (cl.OutputType == TypeManager.TYPE_ENUM_BASE + cl.OutputSet.size()) {
            b.putString("Data", (String) data);
        }
        
        try {
            Message reply = Message.obtain(null, MSG_DATA);
            reply.setData(b);
            callbackMessengers.get(handle).send(reply);
        } catch (RemoteException e) {
            // Target no longer exists
            classifiers.get(handle).destroy();
            classifiers.remove(handle);
            callbackMessengers.remove(handle);
        }
    }
    
    public SensorReaderManager getSensorManager() {
        return sensorManager;
    }
    
    public FeatureManager getFeatureManager() {
        return featureManager;
    }
    
    public Bundle reportStatus() {
        Log.d(TAG, "reportStatus");
        Bundle b = new Bundle();
        b.putInt("classifierRegCount", classifiers.size());
        int classifierActivatedCount = 0;
        for (Classifier c : classifiers.values())
            if (c.isActivated())
                classifierActivatedCount++;
        b.putInt("classifierActCount", classifierActivatedCount);
        b.putInt("featureCount", sensorManager.getFeatureCount());
        b.putInt("sensorCount", sensorManager.getSensorCount());
        return b;
    }

}
