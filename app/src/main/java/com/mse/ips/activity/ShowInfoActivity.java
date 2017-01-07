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
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.mse.ips.R;

import java.util.List;
import java.util.UUID;

public class ShowInfoActivity extends AppCompatActivity {
    private RadioGroup mTechnologies = null;
    private RadioButton mRadioBtnWifi = null;
    private RadioButton mRadioBtnMagnetic = null;
    private RadioButton mRadioBtnBluetooth = null;
    private Button mButton = null;
    private TextView mScanResults = null;
    private boolean mIsReccording = false;
    private WifiManager mWifiManager;
    private WifiReceiver mReceiverWifi;
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
        setContentView(R.layout.activity_show_info);

        //Menu
        ActionBar ab = getSupportActionBar();
        if(ab !=null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        //Widgets
        mScanResults = (TextView) findViewById(R.id.text_scan_results);
        mRadioBtnWifi = (RadioButton) findViewById(R.id.radio_wifi);
        mRadioBtnMagnetic = (RadioButton) findViewById(R.id.radio_magnetic);
        mRadioBtnBluetooth = (RadioButton) findViewById(R.id.radio_bluetooth);
        mTechnologies = (RadioGroup) findViewById(R.id.radio_group_technology);
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(v -> changeRecordingState());

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
        mBeaconManager.setRangingListener((Region region, List<Beacon> list) -> showBluetoothResults(list));
        mRegion = new Region("Ranged region", UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiverWifi);
        if(mIsReccording && mRadioBtnBluetooth.isChecked()) {
            mBeaconManager.stopRanging(mRegion);
        }
        if(mIsReccording && mRadioBtnMagnetic.isChecked()) {
            mSensorManager.unregisterListener(mSensorEventListener, mMagneticField);
            mSensorManager.unregisterListener(mSensorEventListener, mAccelerometer);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        if(mIsReccording && mRadioBtnBluetooth.isChecked()) {
            mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
        }
        if(mIsReccording && mRadioBtnMagnetic.isChecked()) {
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
                StringBuilder results = new StringBuilder("Scan Results:\n");
                results.append("-------------\n");
                results.append("x:");
                results.append(magnetic[0]);
                results.append("\ny:");
                results.append(magnetic[1]);
                results.append("\nz:");
                results.append(magnetic[2]);
                results.append("\nNorth:");
                results.append(A_W[1]);
                results.append("\nSky:");
                results.append(A_W[2]);
                mScanResults.setText(results);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void changeRecordingState()
    {
        mIsReccording = !mIsReccording;

        if(mIsReccording) {

            if(mRadioBtnWifi.isChecked()) {
                doInback();
            }
            if(mRadioBtnBluetooth.isChecked()) {
                mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
            }
            if(mRadioBtnMagnetic.isChecked()){
                mSensorManager.registerListener(mSensorEventListener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
            Toast.makeText(ShowInfoActivity.this, "Affichage des empreintes" , Toast.LENGTH_SHORT).show();
            mButton.setText(R.string.stop);
            // Disable radio buttons
            mTechnologies.setEnabled(false);
        }else{
            // Enable switches
            mTechnologies.setEnabled(true);
            if(mRadioBtnBluetooth.isChecked()) {
                mBeaconManager.stopRanging(mRegion);
            }
            if(mRadioBtnMagnetic.isChecked()) {
                mSensorManager.unregisterListener(mSensorEventListener, mMagneticField);
                mSensorManager.unregisterListener(mSensorEventListener, mAccelerometer);
            }
            Toast.makeText(ShowInfoActivity.this, "Arrêt de l'affichage des empreintes" , Toast.LENGTH_SHORT).show();
            mButton.setText(R.string.start);
        }
    }

    public void doInback()
    {
        mHandler.postDelayed(() -> {
            if(mIsReccording && mRadioBtnWifi.isChecked()) {
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

    private void showBluetoothResults(List<Beacon> list)
    {
        StringBuilder results = new StringBuilder("Scan Results:\n");
        results.append("-------------\n");
        if (!list.isEmpty() && mIsReccording && mRadioBtnBluetooth.isChecked()) {

            for (Beacon result : list) {
                String str = result.getRssi() + " " + result.getMajor() + " dBM " + result.getMinor() + "\n";
                results.append(str);
            }
        }
        mScanResults.setText(results);
    }

    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {

            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> scanResults = mWifiManager.getScanResults();
                StringBuilder results = new StringBuilder("Scan Results:\n");
                results.append("-------------\n");

                if(mIsReccording && mRadioBtnWifi.isChecked()) {
                    for (ScanResult result : scanResults) {
                        String str = result.SSID + " " + result.level + " dBM " + result.BSSID + "\n";
                        results.append(str);
                    }
                }
                mScanResults.setText(results);
            }
        }

    }
}
