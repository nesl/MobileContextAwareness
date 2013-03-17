package edu.ucla.nesl.mca.sensor;

import java.util.HashMap;

import edu.ucla.nesl.mca.McaService;
import edu.ucla.nesl.mca.sensor.BasicDataService.BasicDataBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class SensorReaderManager {
    private static final String TAG = "SensorReaderManager";
    
    private static final Class<?>[] SensorClasses = {
        AccelerometerSonsor.class
    };
    
    private McaService homeService;
    private ServiceConnection[] connections;
    private HashMap<String, BasicDataService> sensorMap;
    
    public SensorReaderManager(McaService homeService) {
        this.homeService = homeService;
    }
    
    public boolean isSensorAvailable(String sensorName) {
        if (sensorMap.containsKey(sensorName))
            return sensorMap.get(sensorName).isAvailable();
        else
            return false;
    }
    
    public BasicDataService getSensor(String sensorName) {
        return sensorMap.get(sensorName);
    }
    
    public void init() {
        bindAllServices();
    }
    
    public void cleanup() {
        unbindAllServices();
    }
    
    public int getSensorCount() {
        int count = 0;
        for (BasicDataService sensor : sensorMap.values())
            if (sensor.isAvailable())
                count++;
        return count;
    }
    
    public int getFeatureCount() {
        int count = 0;
        for (BasicDataService sensor : sensorMap.values())
            if (sensor.isAvailable())
                count += sensor.getRequestCount();
        return count;
    }
    
    private void bindAllServices() {
        connections = new ServiceConnection[SensorClasses.length];
        sensorMap = new HashMap<String, BasicDataService>();
        for (int i = 0; i < SensorClasses.length; i++) {
            connections[i] = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className,
                        IBinder iBinder) {
                    Log.i(TAG, "onServiceConnected: " + className.toString());
                    BasicDataBinder binder = (BasicDataBinder) iBinder;
                    BasicDataService service = binder.getService();
                    service.setHomeService(homeService);
                    sensorMap.put(service.Name, service);
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    // If any of the service got disconnected, whole thing collapse
                    Log.i(TAG, "onServiceDisconnected: " + className.toString());
                }
            };
            Intent intent = new Intent(homeService, SensorClasses[i]);
            homeService.bindService(intent, connections[i], Context.BIND_AUTO_CREATE);
        }
    }
    
    private void unbindAllServices() {
        for (ServiceConnection c : connections) {
            homeService.unbindService(c);
        }
    }
}
