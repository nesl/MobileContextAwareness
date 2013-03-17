package edu.ucla.nesl.mca;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ControlActivity extends Activity {	
    private static final String TAG = "ControlActivity";
    
    private static final int POLL_RATE_DEFAULT = 1000;
    private static final int POLL_RATE_MAX = 3600000;

    /** Messenger for communicating with service. */
    private Messenger mcaService = null;
    /** Flag indicating whether we have called bind on the service. */
    private boolean isMCABound = false;

    private final Object evaluationLock = new Object();
    private volatile boolean stopPoll = true;
    private volatile boolean suspendPoll = true;
    private volatile int pollRate = POLL_RATE_DEFAULT;
    
    /**
     * Handler of incoming messages from service.
     */
    private static class MCAIncomingHandler extends Handler {
        private final WeakReference<ControlActivity> myService;

        MCAIncomingHandler(ControlActivity service) {
            myService = new WeakReference<ControlActivity>(service);
        }
        
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: " + msg.what);
            switch (msg.what) {
            case McaService.MSG_STATUS:
                myService.get().updateStatus(msg.getData());
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMCAActivityMessenger = 
            new Messenger(new MCAIncomingHandler(this));

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mMCAConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mcaService = new Messenger(service);
            ((TextView) findViewById(R.id.txtStatus)).setText(
                    getString(R.string.status_running));
            
            suspendPoll = false;
            synchronized (evaluationLock) {
                evaluationLock.notify();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected");
            mcaService = null;
            ((TextView) findViewById(R.id.txtStatus)).setText(
                    getString(R.string.status_disconnected));
        }
    };
    
    private OnClickListener btnStartOnClickListener =  new OnClickListener() {
        @Override
        public void onClick(View v) {
            startMcaService();
            doBindMcaService();
        }
    };
    
    private OnClickListener btnStopOnClickListener =  new OnClickListener() {
        @Override
        public void onClick(View v) {
            stopMcaService();
        }
    };
    
    private OnClickListener btnChangeRateOnClickListener =  new OnClickListener() {
        @Override
        public void onClick(View v) {
            EditText editText = (EditText) findViewById(R.id.edtRate);
            double newDoubleRate = Double.parseDouble(editText.getText().toString());
            newDoubleRate *= 1000;    // second -> millisecond
            if (newDoubleRate > POLL_RATE_MAX)
                newDoubleRate = POLL_RATE_MAX;
            int newIntRate = (int) Math.round(newDoubleRate);
            newDoubleRate = newIntRate;
            newDoubleRate /= 1000;
            editText.setText(Double.toString(newDoubleRate), TextView.BufferType.EDITABLE);
            pollRate = newIntRate;
        }
    };
    
    private void startMcaService() {
        startService(new Intent(ControlActivity.this, McaService.class));
        
        Button btn = (Button) findViewById(R.id.btnOnOff);
        btn.setText(getString(R.string.action_stop));
        btn.setOnClickListener(btnStopOnClickListener);
    }
    
    private void stopMcaService() {
        doUnbindMcaService();
        
        stopService(new Intent(this, McaService.class));

        ((TextView) findViewById(R.id.txtStatus)).setText(
                getString(R.string.status_stopped));
        
        Button btn = (Button) findViewById(R.id.btnOnOff);
        btn.setText(getString(R.string.action_start));
        btn.setOnClickListener(btnStartOnClickListener);
    }
    
    private void doBindMcaService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(this, McaService.class), mMCAConnection, 0);
        isMCABound = true;
    }

    private void doUnbindMcaService() {
        if (isMCABound) {
            suspendPoll = true;
            unbindService(mMCAConnection);
            isMCABound = false;
        }
    }
    
    private void updateStatus(Bundle data) {
        updateStatusHelper(data, "classifierRegCount", R.id.txtClassifierRegCount);
        updateStatusHelper(data, "classifierActCount", R.id.txtClassifierCount);
        updateStatusHelper(data, "featureCount", R.id.txtFeatureCount);
        updateStatusHelper(data, "sensorCount", R.id.txtSensorCount);
    }
    
    private void updateStatusHelper(Bundle data, String key, int textViewId) {
        int value = data.getInt("key", -1);
        TextView textView = (TextView) findViewById(textViewId);
        
        if (value == -1)
            textView.setText(getString(R.string.default_value));
        else
            textView.setText(Integer.toString(value));        
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_control);
        findViewById(R.id.btnChangeRate).setOnClickListener(btnChangeRateOnClickListener);
        ((EditText) findViewById(R.id.edtRate)).setText(
                Double.toString(POLL_RATE_DEFAULT / 1000.0), TextView.BufferType.EDITABLE);
                
        startMcaService();
        
        stopPoll = false;
        pollThread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
	protected void onStart() { 
    	super.onStart(); 
        Log.d(TAG, "onStart");
    	doBindMcaService();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
        Log.d(TAG, "onStop");
        doUnbindMcaService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        stopPoll = true;
        pollThread.interrupt();
    }

    private final Thread pollThread = new Thread() {
        @Override
        public void run() {            
            while (!stopPoll) {
                try {
                    if (suspendPoll) {
                        synchronized (evaluationLock) {
                            evaluationLock.wait();
                        }
                    } else {
                        Message msg = Message.obtain(null, McaService.MSG_STATUS);
                        msg.replyTo = mMCAActivityMessenger;
                        try {
                            mcaService.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        synchronized (evaluationLock) {
                            evaluationLock.wait(pollRate);
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
}
