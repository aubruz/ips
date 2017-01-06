package com.mse.ips.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.WifiManager;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.mse.ips.R;
import com.mse.ips.lib.GetBitmapFromUrlTask;
import com.mse.ips.lib.Point;
import com.mse.ips.lib.SpinnerItem;
import com.mse.ips.listener.OnMapViewClickListener;
import com.mse.ips.view.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SaveFingerprintsActivity extends AppCompatActivity {
    private MapView mImageView = null;
    private GetBitmapFromUrlTask mGetImageTask = null;
    private boolean mCanAddPoint = false;
    private Point mCurrentPoint = null;
    private TextView mText = null;
    WifiManager mWifiManager;
    WifiReceiver mReceiverWifi;
    private Button mButton = null;
    private EditText mRoom = null;
    private EditText mPointName = null;
    private boolean mReccording = false;
    private Switch mSwitchBluetooh = null;
    private Switch mSwitchWifi = null;
    private Switch mSwitchMagneticField = null;
    private Spinner mSpinnerFloors = null;
    private Spinner mSpinnerBuildings = null;
    private ArrayList<SpinnerItem> mFloorsList = null;
    private ArrayList<SpinnerItem> mBuildingsList = null;
    private ArrayAdapter<SpinnerItem> mFloorsAdapter = null;
    private ArrayAdapter<SpinnerItem> mBuildingsAdapter = null;
    private float gravity[] = new float[3];
    private float magnetic[] = new float[3];
    private SensorManager mSensorManager = null;
    private Sensor mMagneticField = null;
    private Sensor mAccelerometer = null;
    private BeaconManager mBeaconManager;
    private Region mRegion;
    private final Handler mHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_fingerprints);

        // Widgets
        mText = (TextView) findViewById(R.id.text);
        mButton = (Button) findViewById(R.id.button);
        mRoom = (EditText) findViewById(R.id.room);
        mPointName = (EditText) findViewById(R.id.point_name);
        mSwitchBluetooh = (Switch) findViewById(R.id.switchBlutetooth);
        mSwitchWifi = (Switch) findViewById(R.id.switchWifi);
        mSwitchMagneticField = (Switch) findViewById(R.id.switchMagneticField);
        mSpinnerFloors = (Spinner) findViewById(R.id.spinnerFloors);
        mSpinnerBuildings = (Spinner) findViewById(R.id.spinnerBuildings);
        mButton.setOnClickListener(v -> changeRecordingState());
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        // Spinners
        mBuildingsList = new ArrayList<>();
        mBuildingsAdapter = new ArrayAdapter<>(SaveFingerprintsActivity.this, android.R.layout.simple_spinner_item, mBuildingsList);
        mBuildingsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerBuildings.setAdapter(mBuildingsAdapter);
        mSpinnerBuildings.setOnItemSelectedListener(OnSelectionListener);

        mFloorsList = new ArrayList<>();
        mFloorsAdapter = new ArrayAdapter<>(SaveFingerprintsActivity.this, android.R.layout.simple_spinner_item, mFloorsList);
        mFloorsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerFloors.setAdapter(mFloorsAdapter);
        mSpinnerFloors.setOnItemSelectedListener(OnSelectionListener);

        getBuildings();

        // Wifi initialization
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mReceiverWifi = new WifiReceiver();
        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if(!mWifiManager.isWifiEnabled())
        {
            mWifiManager.setWifiEnabled(true);
        }

        // Bluetooth initialization
        mBeaconManager = new BeaconManager(this);
        mBeaconManager.setBackgroundScanPeriod(5000,5000);
        mBeaconManager.setRangingListener((Region region, List<Beacon> list) -> saveBluetoothFingerprints(list));
        mRegion = new Region("Ranged region", UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);

        // MapView
        mImageView = (MapView) findViewById(R.id.imageView);
        mGetImageTask = new GetBitmapFromUrlTask();
        mGetImageTask.execute("https://ukonect-dev.s3.amazonaws.com/blueprints/43284381");
        mGetImageTask.addOnBitmapRetrievedListener(bitmap -> mImageView.setImageBitmap(bitmap));
        mImageView.addOnMapViewClickedListener(new OnMapViewClickListener()
        {
            @Override
            public void onImageMapClicked(int id, MapView imageMap)
            {
                // when the area is tapped, show the name in a
            }

            @Override
            public void onBubbleClicked(int id)
            {
                // react to info bubble for area being tapped
            }

            @Override
            public void onPointSelected(Point point){
                mCurrentPoint = point;
            }

            @Override
            public void onScreenTapped(float x, float y)
            {
                mCurrentPoint = mImageView.addPoint(x, y);
            }
        });
    }

    AdapterView.OnItemSelectedListener OnSelectionListener =  new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (parent.getId()) {
                case R.id.spinnerBuildings:
                    SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
                    getFloorsFromBuildingId(item.getTag());
                    //Toast.makeText(parent.getContext(), item.getName() + " " + item.getTag(), Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //
        }
    };

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiverWifi);
        if(mReccording && mSwitchBluetooh.isChecked()) {
            mBeaconManager.stopRanging(mRegion);
        }
        if(mReccording && mSwitchMagneticField.isChecked()) {
            mSensorManager.unregisterListener(mSensorEventListener, mMagneticField);
            mSensorManager.unregisterListener(mSensorEventListener, mAccelerometer);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        if(mReccording && mSwitchBluetooh.isChecked()) {
            mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
        }
        if(mReccording && mSwitchMagneticField.isChecked()) {
            mSensorManager.registerListener(mSensorEventListener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        super.onResume();
    }

    final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
            if (sensor.getType() == Sensor.TYPE_GRAVITY) {
                gravity[0] = event.values[0];
                gravity[1] = event.values[1];
                gravity[2] = event.values[2];
                //Log.d("Field","\nXX :"+gravity[0]+"\nYY :"+gravity[1]+"\nZZ :"+gravity[2]);
            } else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

                magnetic[0] = event.values[0];
                magnetic[1] = event.values[1];
                magnetic[2] = event.values[2];

                float[] R = new float[9];
                float[] I = new float[9];
                SensorManager.getRotationMatrix(R, I, gravity, magnetic);
                float [] A_D = event.values.clone();
                float [] A_W = new float[3];
                // We don't need to calculate A_W[0] because the value should be 0 or close
                //A_W[0] = R[0] * A_D[0] + R[1] * A_D[1] + R[2] * A_D[2];
                A_W[1] = R[3] * A_D[0] + R[4] * A_D[1] + R[5] * A_D[2];
                A_W[2] = R[6] * A_D[0] + R[7] * A_D[1] + R[8] * A_D[2];

                //Log.d("Field","\nX :"+A_W[0]+"\nY :"+A_W[1]+"\nZ :"+A_W[2]);

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void changeRecordingState()
    {
        mReccording = !mReccording;
        if(mReccording) {
            if(mSwitchWifi.isChecked()) {
                doInback();
            }
            if(mSwitchBluetooh.isChecked()) {
                mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
            }
            if(mSwitchMagneticField.isChecked()){
                mSensorManager.registerListener(mSensorEventListener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
            Toast.makeText(SaveFingerprintsActivity.this, "Enregistrement des empreintes" , Toast.LENGTH_SHORT).show();
            mButton.setText(R.string.stop);
            // Disable switches
            mSwitchBluetooh.setEnabled(false);
            mSwitchWifi.setEnabled(false);
            mSwitchMagneticField.setEnabled(false);
        }else{
            // Enable switches
            mSwitchBluetooh.setEnabled(true);
            mSwitchWifi.setEnabled(true);
            mSwitchMagneticField.setEnabled(true);
            if(mSwitchBluetooh.isChecked()) {
                mBeaconManager.stopRanging(mRegion);
            }
            if(mSwitchMagneticField.isChecked()) {
                mSensorManager.unregisterListener(mSensorEventListener, mMagneticField);
                mSensorManager.unregisterListener(mSensorEventListener, mAccelerometer);
            }
            Toast.makeText(SaveFingerprintsActivity.this, "Arrêt de l'enregistrement des empreintes" , Toast.LENGTH_SHORT).show();
            mButton.setText(R.string.start);
        }
    }

    public void doInback()
    {
        mHandler.postDelayed(() -> {
            if(mReccording && mSwitchWifi.isChecked()) {
                scanWifi();
                doInback();
            }
        }, 1000);
    }

    private void scanWifi(){
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (mReceiverWifi == null) {
            mReceiverWifi = new WifiReceiver();
        }
        mWifiManager.startScan();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, R.id.refresh, 0, "Refresh");
        menu.add(0, R.id.add_point, 1, "Add point").setCheckable(true);

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case  R.id.refresh:
                // Refresh buildings list
                getBuildings();
                break;
            case R.id.add_point:
                mCanAddPoint = true;

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getBuildings(){
        AndroidNetworking.get("http://api.ukonectdev.com/v1/buildings")
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(new JSONObjectRequestListener() {
                @Override
                public void onResponse(JSONObject response) {
                    //Toast.makeText(SaveFingerprintsActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
                    try{
                        mBuildingsList.clear();
                        JSONArray buildings = (JSONArray) response.get("buildings");
                        for (int i = 0; i < buildings.length(); i++) {
                            JSONObject row = buildings.getJSONObject(i);
                            mBuildingsList.add(new SpinnerItem(row.getString("name"), row.getString("id")));
                        }
                        mBuildingsAdapter.notifyDataSetChanged();
                    }catch (JSONException e){
                        e.printStackTrace();
                    }

                }
                @Override
                public void onError(ANError error) {
                    // handle error
                    error.printStackTrace();
                    //mTextTest.setText(error.toString());
                }
            }
        );
    }

    private void getFloorsFromBuildingId(String buildingId){
        AndroidNetworking.get("http://api.ukonectdev.com/v1/buildings/{buildingID}/floors")
            .addPathParameter("buildingID", buildingId)
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(new JSONObjectRequestListener() {
                @Override
                public void onResponse(JSONObject response) {
                    //Toast.makeText(SaveFingerprintsActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
                    try{
                        mFloorsList.clear();
                        JSONArray buildings = (JSONArray) response.get("floors");
                        for (int i = 0; i < buildings.length(); i++) {
                            JSONObject row = buildings.getJSONObject(i);
                            mFloorsList.add(new SpinnerItem(row.getString("name"), row.getString("id")));
                        }
                        mFloorsAdapter.notifyDataSetChanged();
                    }catch (JSONException e){
                        e.printStackTrace();
                    }

                }
                @Override
                public void onError(ANError error) {
                    // handle error
                    error.printStackTrace();
                    //mTextTest.setText(error.toString());
                }
            }
        );
    }

    private void saveBluetoothFingerprints(List<Beacon> list)
    {
        StringBuilder wn = new StringBuilder("Scan Results:\n");
        if (!list.isEmpty() && mReccording && mSwitchBluetooh.isChecked()) {
            try {
                JSONObject data = new JSONObject();
                JSONArray samples = new JSONArray();
                JSONObject sample;

                for (Beacon result : list) {
                    String str = result.getRssi() + " " + result.getMajor() + " dBM " + result.getMinor() + "\n";
                    wn.append(str);

                    sample = new JSONObject();
                    sample.put("rssi", result.getRssi());
                    sample.put("uuid", result.getProximityUUID());
                    sample.put("major", result.getMajor());
                    sample.put("minor", result.getMinor());
                    samples.put(sample);
                }

                data.put("location", mRoom.getText().toString());
                data.put("point_name", mPointName.getText().toString());
                data.put("technology", "bluetooth");
                data.put("samples", samples);
                SpinnerItem floor = (SpinnerItem) mSpinnerFloors.getSelectedItem();
                //Send request to save the fingerprint
                AndroidNetworking.post("http://api.ukonectdev.com/v1/floors/{floorID}/fingerprints")
                    .addPathParameter("floorID", floor.getTag())
                    .addJSONObjectBody(data)
                    .setPriority(Priority.MEDIUM)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                         @Override
                         public void onResponse(JSONObject response) {
                             //mTextTest.setText(response.toString());
                             Toast.makeText(SaveFingerprintsActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
                         }
                         @Override
                         public void onError(ANError error) {
                             // handle error
                             error.printStackTrace();
                             //mTextTest.setText(error.toString());
                         }
                     }
                );

            }catch(JSONException e){
                e.printStackTrace();
            }

        }else{
            wn.append("nothing");
        }
        mText.setText(wn);
    }

    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {

            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> scanResults = mWifiManager.getScanResults();
                StringBuilder wn = new StringBuilder("Scan Results:\n");
                wn.append("-------------\n");

                //wn.append(mWifiManager.getConnectionInfo());

                for (ScanResult result : scanResults) {
                    String str = result.SSID + " " + result.level + " dBM " + result.BSSID + "\n";
                    wn.append(str);
                }

                if(mReccording && mSwitchWifi.isChecked()) {
                    mText.setText(wn);
                    JSONObject data = new JSONObject();
                    JSONArray samples = new JSONArray();
                    JSONObject sample;

                    try {
                        for (ScanResult result : scanResults) {
                            sample = new JSONObject();
                            sample.put("rssi", result.level);
                            sample.put("bssid", result.BSSID);
                            samples.put(sample);
                        }

                        data.put("location", mRoom.getText().toString());
                        data.put("point_name", mPointName.getText().toString());
                        data.put("technology", "wifi");
                        data.put("samples", samples);

                    }catch(JSONException e){
                        e.printStackTrace();
                    }

                    SpinnerItem floor = (SpinnerItem) mSpinnerFloors.getSelectedItem();
                    //Send request to save the fingerprint
                    AndroidNetworking.post("http://api.ukonectdev.com/v1/floors/{floorID}/fingerprints")
                        .addPathParameter("floorID", floor.getTag())
                        .addJSONObjectBody(data)
                        .setPriority(Priority.MEDIUM)
                        .build()
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                //mTextTest.setText(response.toString());
                                Toast.makeText(SaveFingerprintsActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onError(ANError error) {
                                // handle error
                                error.printStackTrace();
                                //mTextTest.setText(error.toString());
                            }
                        }
                    );
                }
            }
        }

    }
}
