package edu.ucla.nesl.mca.feature;

import edu.ucla.nesl.mca.TypeManager;

public class MeanFeature extends Feature {

    private static final String name = "Mean";
    private static final int outputType = TypeManager.TYPE_DOUBLE;

    public MeanFeature() {
        super(name, outputType);
    }

    @Override
    public boolean careSampleRate() {
        return false;
    }

    @Override
    public Object evaluate(Object[] input, int inputType, String config) {
        if (inputType == TypeManager.TYPE_DOUBLE) {
            
            double sum = 0;
            for (Object obj : input) {
                double value = (Double) obj;
                sum += value;
            }            
            return sum / input.length;
        } else {
            return null;
        }
    }

}
