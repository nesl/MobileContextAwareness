package edu.ucla.nesl.mca.classifier;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import edu.ucla.nesl.mca.DataRequest;
import edu.ucla.nesl.mca.McaService;

public class RawDataClassifier extends Classifier {

    public RawDataClassifier(JSONObject modelObj, McaService homeService,
            HashMap<Integer, DataRequest> featureListMap) throws JSONException {
        super(modelObj, homeService);
        int featureID = modelObj.getInt("FeatureID");
        this.inputRequests.add(featureListMap.get(featureID));
    }

    @Override
    protected Object evaluate() {
        return this.inputValues.get(0)[0];
    }

}
