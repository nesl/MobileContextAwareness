package edu.ucla.nesl.mca.sensor;

public class TimestampedObject {
    private final Object mValue;
    private final long mTimestamp;  // millisecond since Epoch
    
    public TimestampedObject(Object value) {
        mValue = value;
        mTimestamp = getCurrentTimestamp();
    }
    
    public TimestampedObject(Object value, long timestamp) {
        mValue = value;
        mTimestamp = timestamp;
    }
    
    public long getTimeStamp() {
        return mTimestamp;
    }
    
    public Object getValue() {
        return mValue;
    }
    
    /**
     * Get the time difference (age of the data) between its time stamp and now
     * 
     * @return Age in millisecond
     */
    public long getAge() {
        return getCurrentTimestamp() - mTimestamp;
    }
    
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
}
