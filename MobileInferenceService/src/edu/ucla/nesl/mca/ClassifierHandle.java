package edu.ucla.nesl.mca;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class ClassifierHandle implements Parcelable, Serializable {
    
    private static final long serialVersionUID = -5733978668105648585L;
    
    private final String m_classifierName;
    
    public ClassifierHandle(String classifierName) {
        m_classifierName = classifierName;
    }
    
    public ClassifierHandle(Parcel in) {
        m_classifierName = in.readString();
    }

    public String getClassifierName() {
        return m_classifierName;
    }
    
    @Override
    public int hashCode() {
        return m_classifierName.hashCode();
    }
    
    @Override
    public String toString() {
        return m_classifierName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(m_classifierName);
        
    }

    public static final Parcelable.Creator<ClassifierHandle> CREATOR
            = new Parcelable.Creator<ClassifierHandle>() {
        public ClassifierHandle createFromParcel(Parcel in) {
            return new ClassifierHandle(in);
        }

        public ClassifierHandle[] newArray(int size) {
            return new ClassifierHandle[size];
        }
    };
}
