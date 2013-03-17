package edu.ucla.nesl.mca.feature;

import edu.ucla.nesl.mca.TypeManager;

public class MaxFeature extends Feature {

    private static final String name = "Max";
    private static final int outputType = TypeManager.TYPE_DOUBLE;

    public MaxFeature() {
        super(name, outputType);
    }

    @Override
    public boolean careSampleRate() {
        return false;
    }

    @Override
    public Object evaluate(Object[] input, int inputType, String config) {
        if (inputType == TypeManager.TYPE_DOUBLE) {
            
            double result = Double.MIN_VALUE;
            for (Object obj : input) {
                double value = (Double) obj;
                if (value > result)
                    result = value;
            }
            return result;
        } else {
            return null;
        }
    }

}
