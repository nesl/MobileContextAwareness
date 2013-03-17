package edu.ucla.nesl.mca;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import edu.ucla.nesl.mca.feature.FeatureManager;
import edu.ucla.nesl.mca.sensor.BasicDataService;
import edu.ucla.nesl.mca.sensor.SensorReaderManager;
import android.os.Parcel;
import android.os.Parcelable;

public class DataRequest implements Comparable<DataRequest>, Parcelable, 
Serializable {
    
    private static final long serialVersionUID = -4431297248385194030L;

    public static final int DEFAULT_SAMPLERATE = 200;
    
    /**
     * Which feature to be used to calculate result, default: raw sensor data.
     */
    private final String featureName;
    
    /**
     * Supplement feature configuration string, formated in JSON.
     */
    private final String featureConfig;
    
    /**
     * How often to evaluate the feature, unit in millisecond. Default value is
     * 0, meaning evaluation upon explicit request.
     */
    private final int featureEvalRate;
    
    /**
     * How long the history data from sensor to be used to calculate this
     * feature, unit in millisecond. Default value is 0, meaning only the 
     * latest one value is used.
     */
    private final int sensorDataWindowSize;
    
    /**
     * Which sensor the data come from. Must be not null.
     */
    private final String sensorName;
    
    /**
     * Which data component of sensor to be used. Default value is 0, 
     * meaning the internally first component of the sensor, should be used
     * when there is only one component for the sensor.
     */
    private final int sensorComponent;
    
    /**
     * Sensor sampling rate. Default to DEFAULT_SAMPLERATE.
     */
    private final int sensorSampleRate;
    
    /**
     * Multiplied scalar applied to sensor value when calculate feature.
     * Default value is 1.
     */
    private final double sensorDataScaler;
    
    /**
     * Unique identifier generated from other final fields
     */
    private final String identifier;
    
    /**
     * Next system time that the request result need to be evaluated, 
     * unit in millisecond
     */
    private long nextEvaluationTime = 0;
    
    
    private DataRequest(Parcel in) {
        featureName  = in.readString();
        
        String featureConfigTemp = in.readString();
        if (featureConfigTemp.equals("")) {
            featureConfig = null;
        } else {
            featureConfig = featureConfigTemp;
        }

        featureEvalRate = in.readInt();
        sensorDataWindowSize = in.readInt();
        sensorName = in.readString();
        sensorComponent = in.readInt();
        sensorSampleRate = in.readInt();
        sensorDataScaler = in.readDouble();
        identifier = genIdentifier();
    }
    
    public DataRequest(JSONObject featureObj, SensorReaderManager sensorManager)
            throws JSONException {
        if (featureObj.has("Feature")) {
            featureName = featureObj.getString("Feature");
            if (!FeatureManager.isFeatureAvailable(featureName))
                throw new JSONException(
                        "Feature " + featureName + " is undefined.");
        } else {
            featureName = FeatureManager.RawDataFeatureName;
        }

        if (featureObj.has("Parameters")) {
            JSONObject featureParamters = 
                    featureObj.getJSONObject("Parameters");
            featureConfig = featureParamters.toString();
        } else {
            featureConfig = null;
        }
        
        if (featureObj.has("EvalRate")) {
            featureEvalRate = featureObj.getInt("EvalRate");
        } else {
            featureEvalRate = 0;
        }
        
        if (featureObj.has("WindowSize")) {
            sensorDataWindowSize = featureObj.getInt("WindowSize");
        } else {
            sensorDataWindowSize = 0;
        }

        sensorName = featureObj.getString("Sensor");
        if (!sensorManager.isSensorAvailable(sensorName))
            throw new JSONException(
                    "Sensor " + sensorName + " is not available.");

        BasicDataService sensor = sensorManager.getSensor(sensorName);
        if (featureObj.has("Component")) {
            String componentName = featureObj.getString("Component");
            int index = sensor.Components.indexOf(componentName);
            if (index == -1)
                throw new JSONException("Component " + componentName + 
                        " for sensor " + sensorName + " is not available.");
            
            sensorComponent = index;
        } else {
            if (sensor.Components.size() > 0)
                sensorComponent = 0;
            else
                throw new JSONException(
                        "Sensor " + sensorName + " has no component.");
        }
        
        if (featureObj.has("SampleRate")) {
            sensorSampleRate = featureObj.getInt("SampleRate");
        } else {
            sensorSampleRate = DEFAULT_SAMPLERATE;
        }
        
        if (featureObj.has("DataScaler")) {
            sensorDataScaler = featureObj.getDouble("DataScaler");
        } else {
            sensorDataScaler = 1.0;
        }
        
        identifier = genIdentifier();
    }
    
    public String getFeatureName() {
        return featureName;
    }
    
    public String getFeatureConfig() {
        return featureConfig;
    }
    
    public String getSensorName() {
        return sensorName;
    }
    
    public int getSensorComponent() {
        return sensorComponent;
    }
    
    public int getWindowSize() {
        return sensorDataWindowSize;
    }
    
    public int getEvalRate() {
        return featureEvalRate;
    }
    
    public int getSampleRate() {
        return sensorSampleRate;
    }
    
    public long getNextEvalTime() {
        return nextEvaluationTime;
    }

    public void setNextEvalTime(long value) {
        nextEvaluationTime = value;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    private String genIdentifier() {
        String result = sensorName;
        result += "[" + Integer.toString(sensorComponent) + "]->";
        result += featureName;
        result += (featureConfig == null? "" : "(" + featureConfig + ")");
        result += "?" + featureEvalRate + "&" + sensorDataWindowSize;
        result += "&" + sensorSampleRate + "&" + sensorDataScaler;
        return result;
    }
    
    @Override
    public String toString() {
        String result = sensorName;
        result += "[" + Integer.toString(sensorComponent) + "]->";
        result += featureName;
        result += (featureConfig == null? "" : "(" + featureConfig + ")");
        result += "?evalrate=" + featureEvalRate;
        result += "&window=" + sensorDataWindowSize;
        result += "&samplerate=" + sensorSampleRate;
        result += "&scaler=" + sensorDataScaler;
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        return this.getIdentifier().equals(((DataRequest)obj).getIdentifier());
    }

    @Override
    public int compareTo(DataRequest another) {
        return Long.signum(
                this.nextEvaluationTime - another.nextEvaluationTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(featureName);
        if (featureConfig == null) {
            out.writeString("");
        } else {
            out.writeString(featureConfig);
        }
        out.writeInt(featureEvalRate);
        out.writeInt(sensorDataWindowSize);
        out.writeString(sensorName);
        out.writeInt(sensorComponent);
        out.writeInt(sensorSampleRate);
        out.writeDouble(sensorDataScaler);
    }

    public static final Parcelable.Creator<DataRequest> CREATOR
            = new Parcelable.Creator<DataRequest>() {
        public DataRequest createFromParcel(Parcel in) {
            return new DataRequest(in);
        }

        public DataRequest[] newArray(int size) {
            return new DataRequest[size];
        }
    };
}
