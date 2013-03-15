package edu.ucla.nesl.mcaTest;

import edu.ucla.nesl.mca.ClassifierHandle;
import edu.ucla.nesl.mca.ClassifierListener;
import edu.ucla.nesl.mca.McaConnector;

import java.text.DecimalFormat;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    
    private final McaConnector mcaConnector = new McaConnector(this);
    
    private int status = 0;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        
        mcaConnector.doBindMcaService();
        
        Button button1 = (Button)findViewById(R.id.button1);
        button1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                
                if (status == 0) {
                    Toast.makeText(getApplicationContext(), "register", Toast.LENGTH_SHORT).show();
                    mcaConnector.registerClassifier("mlapi/JSON_TestNew.txt");
                    status++;
                } else if (status == 1) {
                    Toast.makeText(getApplicationContext(), "activate", Toast.LENGTH_SHORT).show();
                    for (ClassifierHandle handle: mcaConnector.getClassifierHandles()) {
                        if (handle.toString().equals("Acc_x"))
                            mcaConnector.activateClassifier(handle, 100, changeX);
                        else if (handle.toString().equals("Acc_y"))
                            mcaConnector.activateClassifier(handle, 100, changeY);
                        else if (handle.toString().equals("Acc_z"))
                            mcaConnector.activateClassifier(handle, 100, changeZ);
                        else if (handle.toString().equals("Transportation Model"))
                            mcaConnector.activateClassifier(handle, 200, changeTrans);
                    }
                    status++;
                } else if (status == 2) {
                    Toast.makeText(getApplicationContext(), "pause", Toast.LENGTH_SHORT).show();
                    for (ClassifierHandle handle: mcaConnector.getClassifierHandles()) {
                        mcaConnector.pauseClassifier(handle);
                    }
                    status++;
                } else if (status == 3) {
                    Toast.makeText(getApplicationContext(), "destroy", Toast.LENGTH_SHORT).show();
                    for (ClassifierHandle handle: mcaConnector.getClassifierHandles()) {
                        mcaConnector.destroyClassifier(handle);
                    }
                    status++;
                } else {
                    Toast.makeText(getApplicationContext(), "finish", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }           
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    private ClassifierListener changeX = new ClassifierListener() {
        @Override
        public void onReceiveData(Object data) {
            TextView text = (TextView)findViewById(R.id.TextView10);
            DecimalFormat fmt = new DecimalFormat("0.000"); 
            String str = fmt.format((Double) data);
            text.setText(str);
        }
    };
    
    private ClassifierListener changeY = new ClassifierListener() {
        @Override
        public void onReceiveData(Object data) {
            TextView text = (TextView)findViewById(R.id.TextView11);
            DecimalFormat fmt = new DecimalFormat("0.000"); 
            String str = fmt.format((Double) data);
            text.setText(str);
        }
    };
    
    private ClassifierListener changeZ = new ClassifierListener() {
        @Override
        public void onReceiveData(Object data) {
            TextView text = (TextView)findViewById(R.id.TextView12);
            DecimalFormat fmt = new DecimalFormat("0.000"); 
            String str = fmt.format((Double) data);
            text.setText(str);
        }
    };
    
    private ClassifierListener changeTrans = new ClassifierListener() {
        @Override
        public void onReceiveData(Object data) {
            TextView text = (TextView)findViewById(R.id.classifierInfo);
            text.setText((String)data);
        }
    };
    
//    private BroadcastReceiver receiver = new BroadcastReceiver() { 
//        @Override
//        public void onReceive(Context context, Intent intent) { 
//            if (intent.getAction().equals(MCAService.DISPLAY_RESULT)) {
//                String res = intent.getCharSequenceExtra("mode").toString();
//                TextView mode = (TextView)findViewById(R.id.classifierInfo);
//                mode.setText(res);
//                String res1 = intent.getCharSequenceExtra("indoor").toString();
//                TextView indoor = (TextView)findViewById(R.id.TextView03);
//                if (res1.equals("Outdoor")) {
//                    indoor.setText("Outdoor");
//                }
//                else {
//                    indoor.setText("Indoor");
//                }
//                
//                String res2 = intent.getCharSequenceExtra("gps").toString();
//                TextView gpsStatus= (TextView)findViewById(R.id.TextView02);
//                gpsStatus.setText(res2);
//            }
//            else if (intent.getAction().equals(MCAService.UPDATE_DATA)) {
//                TextView textX = (TextView)findViewById(R.id.TextView10);
//                TextView textY = (TextView)findViewById(R.id.TextView11);
//                TextView textZ = (TextView)findViewById(R.id.TextView12);
//                DecimalFormat fmt = new DecimalFormat("0.000");  
//                if (intent.hasExtra("Acc_x")) {
//                    String strX = fmt.format(intent.getDoubleExtra("Acc_x", 0.0));
//                    textX.setText(strX);
//                }
//                if (intent.hasExtra("Acc_y")) {
//                    String strY = fmt.format(intent.getDoubleExtra("Acc_y", 0.0));
//                    textY.setText(strY);
//                }
//                if (intent.hasExtra("Acc_z")) {
//                    String strZ = fmt.format(intent.getDoubleExtra("Acc_z", 0.0));
//                    textZ.setText(strZ);
//                }
//            }
//            else if (intent.getAction().equals(MCAService.UPDATE_LOCATION)) {
//                TextView textLa = (TextView)findViewById(R.id.textView2);
//                TextView textLo = (TextView)findViewById(R.id.TextView06);
//                Log.i("MainActivity", intent.getStringExtra("lat"));
//                if (intent.getStringExtra("lat").equals("None")) {
//                    textLa.setText("Off");
//                    textLo.setText("Off");
//                }
//                else if (intent.getStringExtra("lat").equals("No Signal")) {
//                    textLa.setText("No Signal");
//                    textLo.setText("No Signal");
//                }
//                else {
//                    String strLa = Double.valueOf(intent.getDoubleExtra("lat", 0.0)).toString();
//                    String strLo = Double.valueOf(intent.getDoubleExtra("lot", 0.0)).toString();
//                    textLa.setText(strLa);
//                    textLo.setText(strLo);
//                }
//            }
//        } 
//    }; 
    
    @Override
    protected void onResume() { 
        super.onResume(); 
    }
    
    @Override
    protected void onPause() {
        super.onResume(); 
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mcaConnector.doUnbindMcaService();
    }
}
