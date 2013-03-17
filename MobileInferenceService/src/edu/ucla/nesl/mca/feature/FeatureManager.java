package edu.ucla.nesl.mca.feature;

import java.util.HashMap;

import edu.ucla.nesl.mca.DataRequest;
import edu.ucla.nesl.mca.sensor.BasicDataService;
import edu.ucla.nesl.mca.sensor.SensorReaderManager;

public class FeatureManager {
    public static final String RawDataFeatureName = "Raw";

    private static final HashMap<String, Feature> featureMap = 
            new HashMap<String, Feature>();
    
    private SensorReaderManager sensorManager;
    
    static {
        Feature feature;
        feature = new MaxFeature();
        featureMap.put(feature.Name, feature);
        feature = new MinFeature();
        featureMap.put(feature.Name, feature);
        feature = new MeanFeature();
        featureMap.put(feature.Name, feature);
        feature = new VarianceFeature();
        featureMap.put(feature.Name, feature);
        feature = new EnergyCoefFeature();
        featureMap.put(feature.Name, feature);
    }
    
    public static boolean isFeatureAvailable(String featureName) {
        if (featureName.equals(RawDataFeatureName))
            return true;
        else
            return featureMap.containsKey(featureName);
    }
    
    public FeatureManager(SensorReaderManager sensorManager) {
        this.sensorManager  = sensorManager;
    }
    
    public Feature getFeature(DataRequest request) {
        String featureName = request.getFeatureName();
        
        if (featureMap.containsKey(featureName))
            return featureMap.get(featureName);
        else {
            String sensorName = request.getSensorName();
            int sensorComponent = request.getSensorComponent();
            
            BasicDataService sensor = sensorManager.getSensor(sensorName);
            int outputType = sensor.OutputTypes.get(sensorComponent);
            
            return new RawDataFeature(outputType);
            
        }
        
    }
}
