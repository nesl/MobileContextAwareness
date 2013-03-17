package edu.ucla.nesl.mca.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;
import edu.ucla.nesl.mca.DataRequest;
import edu.ucla.nesl.mca.McaService;

public class ClassifierBuilder {
    private static final String TAG = "ClassifierBuilder";
    
	public static ArrayList<Classifier> BuildFromFile(File file, 
	        McaService homeService) throws IOException {
		String jsonString = "";
		BufferedReader fileInput = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = fileInput.readLine()) != null) {
			jsonString += line + "\n";
		}
		fileInput.close();
		return Build(jsonString, homeService);
	}

	public static ArrayList<Classifier> Build(String jsonString, 
	        McaService homeService) throws IOException {
		ArrayList<Classifier> result = new ArrayList<Classifier>();
		HashMap<Integer, DataRequest> featureListMap = new HashMap<Integer, DataRequest>();
        HashMap<Integer, Classifier> ModelMap = new HashMap<Integer, Classifier>();
		try {
			JSONObject object = (JSONObject) new JSONTokener(jsonString).nextValue();
			
			JSONArray featureList = object.getJSONArray("Feature List");
			for (int i = 0; i < featureList.length(); i++) {
                JSONObject featureObj = featureList.getJSONObject(i);
			    int id = featureObj.getInt("ID");
			    DataRequest request = new DataRequest(featureObj, homeService.getSensorManager());
			    featureListMap.put(id, request);
			}
			
			JSONArray modelList = object.getJSONArray("Model List");
			for (int i = 0; i < modelList.length(); i++) {
				JSONObject curModel = modelList.getJSONObject(i);

                Classifier classifier;
				String type = Classifier.getJSONModelType(curModel);
                if (type.equals("RAW")) {
                    classifier = new RawDataClassifier(curModel, homeService, featureListMap);
                } else if (type.equals("TREE")) {
                    classifier = new DecisionTree(curModel, homeService, featureListMap);
                } else {
                    throw new IOException("Cannot recognize the model type.");
                }
                
                if (curModel.has("ID"))
                {
                    int id = curModel.getInt("ID");
                    ModelMap.put(id, classifier);
                }
				
				result.add(classifier);
			}

		} catch (JSONException ex) {
			Log.i(TAG, ex.toString());
			ex.printStackTrace();
			throw new IOException(ex.getMessage());
		}
		return result;
	}

}
