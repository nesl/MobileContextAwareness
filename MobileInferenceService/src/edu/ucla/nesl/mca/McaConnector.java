package edu.ucla.nesl.mca;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

public class McaConnector {
    private static final String TAG = "McaConnector";
    
    public static final String MCASERVICE_INTENT = "edu.ucla.nesl.mca.MCASERVICE";
    
    private final Context parentContext;
    
    private final ArrayList<ClassifierHandle> classifierHandles = 
            new ArrayList<ClassifierHandle>();
    private final HashMap<ClassifierHandle, ClassifierListener> listeners = 
            new HashMap<ClassifierHandle, ClassifierListener>();
    
    /** Flag indicating whether we have called bind on the service. */
    boolean isMCABound;
    
    /**
     * Messenger of our class to receive reply from service.
     */
    private final Messenger mcaConnectorMessenger = 
            new Messenger(new McaIncomingHandler(this));
    
    /** Messenger of the McaService. */
    private Messenger mcaServiceMessenger = null;
    
    /**
     * Handler of incoming messages from service.
     */
    private static class McaIncomingHandler extends Handler {
        private final WeakReference<McaConnector> myService;

        McaIncomingHandler(McaConnector service) {
            myService = new WeakReference<McaConnector>(service);
        }
        
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: " + msg.what);
            McaConnector service = myService.get();
            
            Log.d(TAG, msg.toString());
            switch (msg.what) {
            case McaService.MSG_REGISTER:
                ArrayList<Parcelable> parcels = msg.getData().getParcelableArrayList("Handles");
                if (parcels != null) {
                    for (Parcelable p : parcels) {
                        service.classifierHandles.add((ClassifierHandle) p);
                    }
                }
                break;
            case McaService.MSG_DATA:
                ClassifierHandle handle = (ClassifierHandle) msg.getData().getParcelable("Handle");
                Object data = msg.getData().get("Data");
                service.reportClassifierData(handle, data);
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mMCAConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected.");
            
            mcaServiceMessenger = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected.");
            
            mcaServiceMessenger = null;
        }
    };
    
    public McaConnector(Context parentContext) {
        this.parentContext = parentContext;
    }
    
    public void doBindMcaService() {
        Log.d(TAG, "doBindMcaService.");
        Intent intent = new Intent(MCASERVICE_INTENT);
        
        parentContext.bindService(intent, 
                mMCAConnection, Context.BIND_AUTO_CREATE);
        isMCABound = true;
    }
    
    public void doUnbindMcaService() {
        Log.d(TAG, "doUnbindMcaService.");
        
        if (isMCABound) {
            parentContext.unbindService(mMCAConnection);
            isMCABound = false;
        }
    }
    
    public boolean registerClassifier(String JSONFilepath) {
        Log.d(TAG, "register:" + JSONFilepath);
        if (mcaServiceMessenger != null) {
            try {
                Bundle b = new Bundle();
                b.putString("JSONFile", JSONFilepath);
                Message msg = Message.obtain(null, McaService.MSG_REGISTER);
                msg.setData(b);
                msg.replyTo = mcaConnectorMessenger;
                mcaServiceMessenger.send(msg);
                return true;
            } catch (RemoteException e) {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public boolean activateClassifier(ClassifierHandle handle) {
        return activateClassifier(handle, 0, null);
    }
    
    public boolean activateClassifier(ClassifierHandle handle, 
            int evaluationRate, ClassifierListener listener) {
        Log.d(TAG, "activate:" + handle.toString() + ";" + evaluationRate + ";");
        if (mcaServiceMessenger != null) {
            try {
                Bundle b = new Bundle();
                b.putParcelable("Handle", handle);
                b.putInt("EvalRate", evaluationRate);
                Message msg = Message.obtain(null, McaService.MSG_ACTIVATE);
                msg.setData(b);
                mcaServiceMessenger.send(msg);
                if (listener != null)
                    listeners.put(handle, listener);
                return true;
            } catch (RemoteException e) {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public boolean pauseClassifier(ClassifierHandle handle) {
        Log.d(TAG, "pause:" + handle.toString());
        if (mcaServiceMessenger != null) {
            try {
                Bundle b = new Bundle();
                b.putParcelable("Handle", handle);
                Message msg = Message.obtain(null, McaService.MSG_PAUSE);
                msg.setData(b);
                mcaServiceMessenger.send(msg);
                return true;
            } catch (RemoteException e) {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public boolean destroyClassifier(ClassifierHandle handle) {
        Log.d(TAG, "pause:" + handle.toString());
        if (mcaServiceMessenger != null) {
            try {
                Bundle b = new Bundle();
                b.putParcelable("Handle", handle);
                Message msg = Message.obtain(null, McaService.MSG_DESTROY);
                msg.setData(b);
                mcaServiceMessenger.send(msg);
                return true;
            } catch (RemoteException e) {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public ArrayList<ClassifierHandle> getClassifierHandles() {
        return classifierHandles;
    }
    
    private void reportClassifierData(ClassifierHandle handle, Object data) {
        ClassifierListener listener = listeners.get(handle);
        if (listener != null)
            listener.onReceiveData(data);
    }

}
