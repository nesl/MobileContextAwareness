package edu.ucla.nesl.mca.feature;

public class RawDataFeature extends Feature {

    private static final String name = "Raw";

    public RawDataFeature(int outputType) {
        super(name, outputType);
    }

    @Override
    public boolean careSampleRate() {
        return false;
    }

    @Override
    public Object evaluate(Object[] input, int inputType, String config) {
        return input[input.length - 1];
    }

}
