package edu.ucla.nesl.mca.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public abstract class SensorReader extends BasicDataService {

    public SensorReader(String name, String[] components, int[] outputTypes) {
        super(name, components, outputTypes);
    }

    private static final String TAG = "SensorReader";

    protected SensorManager mSensorManager;
    private Sensor mSensor;
    private SensorEventListener sensorEventListener;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = getSensor();
        sensorEventListener = getSensorEventListener();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mSensorManager.unregisterListener(sensorEventListener, mSensor);
        
    }
    
    protected abstract Sensor getSensor();
    protected abstract SensorEventListener getSensorEventListener();

    @Override
    protected void onRequestMapChange() {
        Log.d(TAG, "onEvaluationRateChange, mRequestList size = " + String.valueOf(requestMap.size()));
        mSensorManager.unregisterListener(sensorEventListener, mSensor);
        if (!requestMap.isEmpty()) {
            mSensorManager.registerListener(sensorEventListener, mSensor,
                    sensorSampleRate * 1000);
        }
    }

    @Override
    public boolean isAvailable() {
        return (mSensor != null);
    }

}
