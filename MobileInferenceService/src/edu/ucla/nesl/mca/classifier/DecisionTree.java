package edu.ucla.nesl.mca.classifier;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import edu.ucla.nesl.mca.DataRequest;
import edu.ucla.nesl.mca.McaService;
import edu.ucla.nesl.mca.TypeManager;

public class DecisionTree extends Classifier {

    private static final String TAG = "DecisionTree";
    
    class TreeNode {
        
        private static final String TAG = "TreeNode";
                
        /** ID for this node */
        private final int m_id;
                
        /** Data request for this node's feature */
        private final int m_featureIndex;
        
        /** Type of this node's feature: SET or REAL */
        private final int m_featureType;
        
        /** Operator if type is REAL */
        private final RealOperator m_realOp;
        
        /** Threshold if type is REAL */
        private final double m_realThes;
        
        /** Operator if type is SET */
        private final SetOperator m_setOp;
        
        /** Threshold if type is SET */
        private final ArrayList<Integer> m_set;
        
        /** result value */
        private final Object m_result;

		/** Child nodes of this node */
        private final TreeNode[] m_childNodes;
        
        /** Temperate array to store children IDs, not exported to XDR */
        private final int[] childList;

        protected TreeNode(JSONObject nodeObj, HashMap<Integer, DataRequest> 
                    featureListMap, DecisionTree parent) throws JSONException {
            m_id = nodeObj.getInt("ID");
            Log.d(TAG, "Node ID=" + m_id);
            if (nodeObj.has("FeatureID")) {
                int featureID = nodeObj.getInt("FeatureID");
                DataRequest request = featureListMap.get(featureID);
                if (request == null)
                    throw new JSONException("Feature ID " + Integer.toString(featureID) + " is undefined.");
                if (!parent.inputRequests.contains(request)) {
                    parent.inputRequests.add(request);
                    m_featureIndex = parent.inputRequests.size() - 1;
                } else {
                    m_featureIndex = parent.inputRequests.indexOf(request);
                }
                Log.d(TAG, "Feature=" + request.toString());
                
                m_featureType = parent.homeService.getFeatureManager().
                        getFeature(request).OutputType;                
                
                if (m_featureType == TypeManager.TYPE_DOUBLE) {
                    String opString = nodeObj.getString("Operator");
                    RealOperator temp = null;
                    for (RealOperator o : RealOperator.values()) {
                        if (o.toString().equals(opString)) {
                            temp = o;
                            break;
                          }
                    }
                    if (temp == null) 
                        throw new JSONException("Undefined Operator " + opString);
                    m_realOp = temp;
                    m_realThes = nodeObj.getDouble("Value");
                    Log.d(TAG, "OP: " + m_realOp.toString() + ", Value: " + Double.toString(m_realThes));
                    
                    m_setOp = null;
                    m_set = null;
                }
                else if (m_featureType > TypeManager.TYPE_ENUM_BASE) {
                    String opString = nodeObj.getString("Operator");
                    SetOperator temp = null;
                    for (SetOperator o : SetOperator.values()) {
                        if (o.toString().equals(opString)) {
                            temp = o;
                            break;
                          }
                    }
                    if (temp == null) 
                        throw new JSONException("Undefined Operator " + opString);
                    m_setOp = temp;
                    m_set = new ArrayList<Integer>();
                    JSONArray setValues = nodeObj.getJSONArray("Value");
                    for (int i = 0; i < setValues.length(); i++) {
                        m_set.add(setValues.getInt(i));
                    }
                    Log.d(TAG, "OP: " + m_setOp.toString() + ", Value: " + m_set.toString());
                    
                    m_realOp = null;
                    m_realThes = Double.NaN;
                    
                } else {
                    throw new JSONException("Decision tree feature type must be one of double or set.");
                }
                
                JSONArray childNodeList = nodeObj.getJSONArray("ChildNode");
                if (childNodeList.length() != 2)
                    throw new JSONException("Decision tree must have two nodes.");
                m_childNodes = new TreeNode[2];
                childList = new int[2];
                childList[0] = childNodeList.getInt(0);
                childList[1] = childNodeList.getInt(1);
                
                m_result = null;
            } 
            else if (nodeObj.has("Result")) {
                int resultType = parent.OutputType;
                if (resultType == TypeManager.TYPE_DOUBLE) {
                    m_result = nodeObj.getDouble("Result");
                } else if (resultType > TypeManager.TYPE_STRING) {
                    m_result = nodeObj.getString("Result");
                } else if (resultType > TypeManager.TYPE_ENUM_BASE) {
                    m_result = nodeObj.getString("Result");
                } else {
                    throw new JSONException("Undefined result Type.");
                }
                Log.d(TAG, "Result = " + m_result.toString());
                
                m_featureIndex = -1;
                m_featureType = TypeManager.TYPE_UNKNOWN;
                m_realOp = null;
                m_realThes = Double.NaN;
                m_setOp = null;
                m_set = null;
                m_childNodes = null;
                childList = null;
            } 
            else {
                throw new JSONException("Cannot have a node with no Feature nor Result defined.");
            }
        }
        
        protected void updateChild(HashMap<Integer, TreeNode> nodeDict) throws JSONException {
            if (childList != null) {
                for (int i = 0; i < 2; i++) {
                    TreeNode temp = nodeDict.get(childList[i]);
                    if (temp == null)
                        throw new JSONException("Node ID " + 
                                Integer.toString(childList[i]) + " does not exist.");
                    m_childNodes[i] = temp;
                }
            }
        }
        
        protected void evaluate() {
        	
        }
    }

    // Basic assumptions
    // This is always a Classification
    // NO MissingValueStrategy implemented
    // NO MissingValuePenalty implemented
    // NoTrueChild NOT allowed
    // SplitCharacteristic treat binarySplit as MultiSplit

    /** The root of the tree */
    private final TreeNode m_root;
    
    public DecisionTree(JSONObject modelObj, McaService homeService, 
            HashMap<Integer, DataRequest> featureListMap) throws JSONException {
        super(modelObj, homeService);

        JSONArray nodeList = modelObj.getJSONArray("Nodes");
        if (nodeList.length() == 0)
            throw new JSONException("Nodes array is empty.");
        
        TreeNode[] nodeArray = new TreeNode[nodeList.length()];
        HashMap<Integer, TreeNode> nodeMap = new HashMap<Integer, TreeNode>();
        Log.d(TAG, "node length=" + nodeList.length());
        
        // Read and build all the tree nodes
        for (int i = 0; i < nodeList.length(); i++) {
            nodeArray[i] = new TreeNode(nodeList.getJSONObject(i), featureListMap, this);
            nodeMap.put(nodeArray[i].m_id, nodeArray[i]);
        }
        m_root = nodeArray[0];
        Log.d(TAG, "Root Node ID=" + m_root.m_id);
        
        // Need to loop the node list once more to construct node hierarchy
        for (int i = 0; i < nodeList.length(); i++) {
            nodeArray[i].updateChild(nodeMap);
        }
    }

    public TreeNode getM_root() {
		return m_root;
	}

    public ArrayList<TreeNode> preOrderTraversal(TreeNode node) {
        // Perform a pre-order traversal of the tree
        // return list of visited nodes
        ArrayList<TreeNode> r = new ArrayList<TreeNode>();
        r.add(node);
        for (TreeNode child : node.m_childNodes) {
            r.addAll(preOrderTraversal(child));
        }
        return r;
    }

    public ArrayList<TreeNode> traversal() {
        return preOrderTraversal(m_root);
    }

	@Override
	protected Object evaluate() {
		// TODO Auto-generated method stub
		TreeNode cur = m_root;
		while (true) {
			// do the evaluation in decision tree
			if (cur.m_featureIndex == -1) {
			    return cur.m_result;
			}
			else {
			    // feature node
			    // Decision tree only use the latest value
                Object [] objs = this.inputValues.get(cur.m_featureIndex);
                Object obj = objs[objs.length - 1];
				if (cur.m_featureType == TypeManager.TYPE_DOUBLE) {
				    double var = (Double) obj;
					Log.d("DecisionTreeEvaluate", "value=" + var + " threshold=" + cur.m_realThes);
					if (cur.m_realOp.evaluate(var, cur.m_realThes)) {
						Log.d("DecisionTreeEvaluate", "go to left child");
						cur = cur.m_childNodes[0];
					}
					else {
						Log.d("DecisionTreeEvaluate", "go to right child");
                        cur = cur.m_childNodes[1];
					}
				}
				else if (cur.m_featureType > TypeManager.TYPE_ENUM_BASE) {
                    int var = (Integer) obj;
                    Log.d("DecisionTreeEvaluate", "value=" + var + " set=" + cur.m_set);
                    if (cur.m_setOp.evaluate(var, cur.m_set)) {
                        Log.d("DecisionTreeEvaluate", "go to left child");
                        cur = cur.m_childNodes[0];
                    }
                    else {
                        Log.d("DecisionTreeEvaluate", "go to right child");
                        cur = cur.m_childNodes[1];
                    }
				}
			}
		}
	}
	
//  @Override
//  public void writeXDR(XDRDataOutput output) throws IOException {
//      // Write the name of the classifier
//      // Note: the length of the classifier name must be unified
//      output.writeString("TREE");
//
//      // Perform an pre-order traversal
//      ArrayList<TreeNode> nodeList = traversal();
//      // Write the number of node to XDR
//      output.writeInt(nodeList.size());
//
//      for (TreeNode node : nodeList) {
//          node.writeXDR(output);
//      }
//      output.close();
//  }
//
//  @Override
//  public void readXDR(XDRDataInput input) throws IOException {
//      // Check classifier type
//      String classifier = input.readString();
//      if (classifier.equals("TREE")) {
//          // read number of nodes
//          int n = input.readInt();
//          // System.out.println(n);
//          // map the IDs to each node
//          ArrayList<TreeNode> list = new ArrayList<TreeNode>();
//          HashMap<Integer, TreeNode> map = new HashMap<Integer, TreeNode>();
//          // read all nodes
//          for (int i = 0; i < n; i++) {
//              TreeNode node = new TreeNode();
//              node.readXDR(input);
//              map.put(node.m_id, node);
//              list.add(node);
//          }
//
//          // build the hierarchy of the tree
//          m_root = list.get(0);
//          for (int i = 0; i < n; i++) {
//              TreeNode node = list.get(i);
//              int[] children = node.childList;
//              for (int j = 0; j < children.length; j++) {
//                  //System.out.print(children[j] + " ");
//                  node.m_childNodes.add(map.get(children[j]));
//              }
//              //System.out.println();
//          }
//      }
//  }
}
