package com.mse.wifiposition.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.mse.wifiposition.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class GetLocation extends AppCompatActivity {
    private Button mStartStopButton = null;
    private TextView mScanResults = null;
    private TextView mRoomValue = null;
    private TextView mPointNameValue = null;
    private boolean mFindingLocation = false;
    private Switch mSwitchBluetooh = null;
    private Switch mSwitchWifi = null;
    private List<ScanResult> mLastWifiScanResult = null;
    private BeaconManager mBeaconManager;
    private Region mRegion;
    private final Handler handler = new Handler();
    WifiManager mWifiManager;
    WifiReceiver mReceiverWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_location);

        mRoomValue = (TextView) findViewById(R.id.room_value);
        mPointNameValue = (TextView) findViewById(R.id.point_name_value);
        mSwitchBluetooh = (Switch) findViewById(R.id.switchBlutetooth);
        mSwitchWifi = (Switch) findViewById(R.id.switchWifi);
        mScanResults = (TextView) findViewById(R.id.text_scan_results);
        mStartStopButton = (Button) findViewById(R.id.btn_start_stop_find_location);
        mStartStopButton.setOnClickListener(v -> startStopFindLocation());

        // Bluetooth initialization
        mBeaconManager = new BeaconManager(this);
        mBeaconManager.setBackgroundScanPeriod(5000,5000);
        mBeaconManager.setRangingListener((Region region, List<Beacon> list) -> sendBluetoothFingerprints(list));
        mRegion = new Region("Ranged region", UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);

        // Initialization of Wifi
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mReceiverWifi = new WifiReceiver();
        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if(!mWifiManager.isWifiEnabled())
        {
            mWifiManager.setWifiEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiverWifi);
        if(mFindingLocation && mSwitchBluetooh.isChecked()) {
            mBeaconManager.stopRanging(mRegion);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        if(mFindingLocation && mSwitchBluetooh.isChecked()) {
            mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
        }
        super.onResume();
    }

    private void startStopFindLocation(){
        mFindingLocation = !mFindingLocation;
        if(mFindingLocation) {
            if(mSwitchWifi.isChecked()) {
                doInback();
            }
            if(mSwitchBluetooh.isChecked()) {
                mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
            }
            Toast.makeText(GetLocation.this, "Recherche de la localisation en cours" , Toast.LENGTH_SHORT).show();
            mStartStopButton.setText(R.string.stop);
        }else{
            Toast.makeText(GetLocation.this, "Arrêt de la recherche" , Toast.LENGTH_SHORT).show();
            mStartStopButton.setText(R.string.start);
        }
    }

    public void doInback()
    {
        handler.postDelayed(() -> {
            if(mFindingLocation) {
                find();
                doInback();
            }
        }, 1000);
    }

    private void find(){
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (mReceiverWifi == null) {
            mReceiverWifi = new WifiReceiver();
        }
        mWifiManager.startScan();
    }

    private void sendBluetoothFingerprints(List<Beacon> list)
    {
        StringBuilder wn = new StringBuilder("Scan Results:\n");
        if (!list.isEmpty() && mFindingLocation && mSwitchBluetooh.isChecked()) {
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

                data.put("bluetooth", samples);

                // If wifi option is enable at the same time as bluetooth
                if(mSwitchWifi.isChecked()) {
                    samples = new JSONArray();
                    try {
                        for (ScanResult result : mLastWifiScanResult) {
                            sample = new JSONObject();
                            sample.put("rssi", result.level);
                            sample.put("bssid", result.BSSID);
                            samples.put(sample);
                        }
                        data.put("wifi", samples);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                //Send request to save the fingerprint
                AndroidNetworking.post("http://api.ukonectdev.com/v1/find/location")
                    .addJSONObjectBody(data)
                    .setPriority(Priority.MEDIUM)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                         @Override
                         public void onResponse(JSONObject response) {
                             //mTextTest.setText(response.toString());
                             try {
                                 mRoomValue.setText(response.getString("room"));
                                 mPointNameValue.setText(response.getString("point_name"));
                             }catch(JSONException e){
                                 e.printStackTrace();
                             }
                         }
                         @Override
                         public void onError(ANError error) {
                             // handle error
                             error.printStackTrace();
                         }
                    }
                );

            }catch(JSONException e){
                e.printStackTrace();
            }

        }else{
            wn.append("nothing");
        }
        mScanResults.setText(wn);
    }

    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {

            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mLastWifiScanResult = mWifiManager.getScanResults();
                StringBuilder wn = new StringBuilder("Scan Results:\n");
                wn.append("-------------\n");

                for (ScanResult result : mLastWifiScanResult) {
                    String str = result.SSID + " " + result.level + " dBM " + result.BSSID + "\n";
                    wn.append(str);

                }
                mScanResults.setText(wn);
                // Send Wifi samples only if Wifi is the only option checked
                // Otherwise the samples will be sent at the same time as the bluetooth ones
                if(mFindingLocation && mSwitchWifi.isChecked() && !mSwitchBluetooh.isChecked()) {
                    JSONObject data = new JSONObject();
                    JSONArray samples = new JSONArray();
                    JSONObject sample;
                    try {
                        for (ScanResult result : mLastWifiScanResult) {
                            sample = new JSONObject();
                            sample.put("rssi", result.level);
                            sample.put("bssid", result.BSSID);
                            samples.put(sample);
                        }
                        data.put("wifi", samples);

                    }catch(JSONException e){
                        e.printStackTrace();
                    }

                    //Send request to get current location
                    AndroidNetworking.post("http://api.ukonectdev.com/v1/find/location")
                        .addJSONObjectBody(data)
                        .setPriority(Priority.MEDIUM)
                        .build()
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                //mTextTest.setText(response.toString());
                                try {
                                    mRoomValue.setText(response.getString("room"));
                                    mPointNameValue.setText(response.getString("point_name"));
                                }catch(JSONException e){
                                    e.printStackTrace();
                                }
                            }
                            @Override
                            public void onError(ANError error) {
                                // handle error
                                error.printStackTrace();
                                //mTextTest.setText(error.toString());
                            }
                        });
                }
            }
        }

    }
}
