package edu.ucla.nesl.mca.feature;

import edu.ucla.nesl.mca.TypeManager;

public class EnergyCoefFeature extends Feature {

    private static final String name = "Energy Coefficient";
    private static final int outputType = TypeManager.TYPE_DOUBLE;
    
    public EnergyCoefFeature() {
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
