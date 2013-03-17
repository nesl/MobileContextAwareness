package edu.ucla.nesl.mca.feature;

import edu.ucla.nesl.mca.TypeManager;

public class MinFeature extends Feature {

    private static final String name = "Min";
    private static final int outputType = TypeManager.TYPE_DOUBLE;

    public MinFeature() {
        super(name, outputType);
    }

    @Override
    public boolean careSampleRate() {
        return false;
    }

    @Override
    public Object evaluate(Object[] input, int inputType, String config) {
        if (inputType == TypeManager.TYPE_DOUBLE) {
                
            double result = Double.MAX_VALUE;
            for (Object obj : input) {
                double value = (Double) obj;
                if (value < result)
                    result = value;
            }
            return result;
        } else {
            return null;
        }
    }

}
