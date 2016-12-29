package com.mse.wifiposition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SaveFingerprints extends AppCompatActivity {

    private TextView mText = null;
    WifiManager mWifiManager;
    WifiReceiver mReceiverWifi;
    private Button mButton = null;
    private EditText mRoom = null;
    private EditText mPointName = null;
    private boolean mReccording = false;
    private Switch mSwitchBluetooh = null;
    private Switch mSwitchWifi = null;
    private Spinner mSpinnerFloors = null;
    private Spinner mSpinnerBuildings = null;
    private ArrayList<SpinnerItem> mFloorsList = null;
    private ArrayList<SpinnerItem> mBuildingsList = null;
    private ArrayAdapter<SpinnerItem> mFloorsAdapter = null;
    private ArrayAdapter<SpinnerItem> mBuildingsAdapter = null;
    private BeaconManager mBeaconManager;
    private Region mRegion;
    private final Handler mHandler = new Handler();
    /*private final int PERMISSIONS_REQUEST_WIFI = 0x1234;
    private String[] mPermissions = new String[]{
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };*/


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
        mSpinnerFloors = (Spinner) findViewById(R.id.spinnerFloors);
        mSpinnerBuildings = (Spinner) findViewById(R.id.spinnerBuildings);
        mButton.setOnClickListener(v -> changeRecordingState());

        // Spinners
        mBuildingsList = new ArrayList<>();
        mBuildingsAdapter = new ArrayAdapter<>(SaveFingerprints.this, android.R.layout.simple_spinner_item, mBuildingsList);
        mBuildingsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerBuildings.setAdapter(mBuildingsAdapter);
        mSpinnerBuildings.setOnItemSelectedListener(OnSelectionListener);

        mFloorsList = new ArrayList<>();
        mFloorsAdapter = new ArrayAdapter<>(SaveFingerprints.this, android.R.layout.simple_spinner_item, mFloorsList);
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

        /*if(checkSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(android.Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(mPermissions, PERMISSIONS_REQUEST_WIFI);
        }*/
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
    /*public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_WIFI  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            doInback();
        }
    }*/

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiverWifi);
        if(mReccording && mSwitchBluetooh.isChecked()) {
            mBeaconManager.stopRanging(mRegion);
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

        super.onResume();
    }

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
            Toast.makeText(SaveFingerprints.this, "Enregistrement des empreintes" , Toast.LENGTH_SHORT).show();
            mButton.setText(R.string.stop);
            mSwitchBluetooh.setEnabled(false);
            mSwitchWifi.setEnabled(false);
        }else{
            mSwitchBluetooh.setEnabled(true);
            mSwitchWifi.setEnabled(true);
            if(mSwitchBluetooh.isChecked()) {
                mBeaconManager.stopRanging(mRegion);
            }
            Toast.makeText(SaveFingerprints.this, "Arrêt de l'enregistrement des empreintes" , Toast.LENGTH_SHORT).show();
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

    /*public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Refresh");
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        mWifiManager.startScan();
        text.setText("Starting Scan");
        return super.onOptionsItemSelected(item);
    }*/

    private void getBuildings(){
        AndroidNetworking.get("http://api.ukonectdev.com/v1/buildings")
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(new JSONObjectRequestListener() {
                @Override
                public void onResponse(JSONObject response) {
                    //Toast.makeText(SaveFingerprints.this, response.toString(), Toast.LENGTH_SHORT).show();
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
                    //Toast.makeText(SaveFingerprints.this, response.toString(), Toast.LENGTH_SHORT).show();
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
                             Toast.makeText(SaveFingerprints.this, response.toString(), Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(SaveFingerprints.this, response.toString(), Toast.LENGTH_SHORT).show();
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
