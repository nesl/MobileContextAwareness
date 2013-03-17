package edu.ucla.nesl.mca.sensor;

import edu.ucla.nesl.mca.TypeManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

public class AccelerometerSonsor extends SensorReader {

    private static final String TAG = "AccelerometerSonsor";
    
    private static final String name = "Acc";
    private static final String[] components = {"x", "y", "z", "len"};
    private static final int[] outputTypes = {TypeManager.TYPE_DOUBLE};
    
    public AccelerometerSonsor() {
        super(name, components, outputTypes);
    }

    @Override
    protected Sensor getSensor() {
        return mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected SensorEventListener getSensorEventListener() {
        return new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                removeOldData();

                for (int i = 0; i < 3; i++) {
                    sensorDataQueues.get(i).add(new TimestampedObject(Double.valueOf((double) event.values[i])));
                }
                Double len = Math.sqrt(
                            event.values[0] * event.values[0] + 
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]);
                sensorDataQueues.get(3).add(new TimestampedObject(len));

                Log.d(TAG, "Data: " + event.values[0] + ", " + event.values[1] 
                        + ", " + event.values[2] + ", " + len);
            }
        };
    }

}
