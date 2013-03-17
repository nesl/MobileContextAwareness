package edu.ucla.nesl.mca.classifier;

import java.util.ArrayList;

public enum SetOperator {
    IN("in")  {
        public boolean evaluate(int featureValue, ArrayList<Integer> set) {
            return set.contains(featureValue);
        }
    },
    NOTIN("out") {
        public boolean evaluate(int featureValue, ArrayList<Integer> set) {
            return !set.contains(featureValue);
        }
    };
    
    public abstract boolean evaluate(int featureValue, ArrayList<Integer> set);
    
    private final String m_stringVal;
    
    SetOperator(String name) {
        m_stringVal = name;
    }
          
    public String toString() {
        return m_stringVal;
    }
}
