package com.mse.wifiposition;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class GetLocation extends AppCompatActivity {
    private TextView mRoom = null;
    private TextView mPointName = null;
    private Button mStartStopButton = null;
    private TextView mScanResults = null;
    private TextView mRoomValue = null;
    private TextView mPointNameValue = null;
    private boolean mFindingLocation = false;
    private Switch mSwitchBluetooh = null;
    private Switch mSwitchWifi = null;
    private final Handler handler = new Handler();
    WifiManager mWifiManager;
    WifiReceiver mReceiverWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_location);

        mRoom = (TextView) findViewById(R.id.room_value);
        mPointName = (TextView) findViewById(R.id.point_name_value);
        mRoomValue = (TextView) findViewById(R.id.room_value);
        mPointNameValue = (TextView) findViewById(R.id.point_name_value);
        mSwitchBluetooh = (Switch) findViewById(R.id.switchBlutetooth);
        mSwitchWifi = (Switch) findViewById(R.id.switchWifi);
        mScanResults = (TextView) findViewById(R.id.text_scan_results);
        mStartStopButton = (Button) findViewById(R.id.btn_start_stop_find_location);
        mStartStopButton.setOnClickListener(v -> startStopFindLocation());

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
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    private void startStopFindLocation(){
        mFindingLocation = !mFindingLocation;
        if(mFindingLocation) {
            if(mSwitchWifi.isChecked()) {
                doInback();
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

    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {

            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> scanResults = mWifiManager.getScanResults();
                StringBuilder wn = new StringBuilder("Scan Results:\n");
                wn.append("-------------\n");

                for (ScanResult result : scanResults) {
                    wn.append(result.SSID + " " + result.level + " dBM " + result.BSSID + "\n" );

                }
                mScanResults.setText(wn);
                if(mFindingLocation) {
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
                        data.put("technology", "wifi");
                        data.put("samples", samples);

                    }catch(JSONException e){
                        e.printStackTrace();
                    }

                    //Send request to get current location
                    AndroidNetworking.post("http://api.ukonectdev.com/v1/find/location")
                        //.addPathParameter("buildingID", "1388740055")
                        .addJSONObjectBody(data)
                        .setPriority(Priority.MEDIUM)
                        .build()
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                //mTextTest.setText(response.toString());
                                try {
                                    mRoomValue.setText(response.getString("room"));
                                    mPointName.setText(response.getString("point_name"));
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
