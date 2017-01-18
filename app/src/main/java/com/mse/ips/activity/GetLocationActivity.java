package com.mse.ips.activity;

import android.content.Context;
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
import android.util.Log;
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
import com.mse.ips.R;
import com.mse.ips.lib.Building;
import com.mse.ips.lib.Floor;
import com.mse.ips.lib.GetBitmapFromUrlTask;
import com.mse.ips.lib.Point;
import com.mse.ips.lib.WifiReceiver;
import com.mse.ips.listener.OnMapViewClickListener;
import com.mse.ips.view.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class GetLocationActivity extends AppCompatActivity {
    private int mIndex = 0;
    private Button mStartStopButton = null;
    private TextView mRoomValue = null;
    private TextView mPointNameValue = null;
    private boolean mIsFindingLocation = false;
    private Switch mSwitchBluetooth = null;
    private Switch mSwitchWifi = null;
    private Switch mSwitchMagneticField = null;
    private List<ScanResult> mLastWifiScanResult = null;
    private List<Beacon> mLastBluetoothScanResult = null;
    private BeaconManager mBeaconManager;
    private Region mRegion;
    private final Handler handler = new Handler();
    WifiManager mWifiManager;
    WifiReceiver mReceiverWifi;
    private float mGravity[] = new float[3];
    private float mMagnetic[] = new float[3];
    private SensorManager mSensorManager = null;
    private Sensor mMagneticField = null;
    private Sensor mAccelerometer = null;
    private float [] mNewBasis = new float[3];;
    private Building mCurrentBuilding = null;
    private Floor mCurrentFloor = null;
    private Point mCurrentPoint  = null;
    private MapView mImageView = null;
    private GetBitmapFromUrlTask mGetImageTask = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_location);

        // Menu
        ActionBar ab = getSupportActionBar();
        if(ab !=null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // Widgets
        mRoomValue = (TextView) findViewById(R.id.room_value);
        mPointNameValue = (TextView) findViewById(R.id.point_name_value);
        mSwitchBluetooth = (Switch) findViewById(R.id.switchBlutetooth);
        mSwitchWifi = (Switch) findViewById(R.id.switchWifi);
        mSwitchMagneticField = (Switch) findViewById(R.id.switchMagneticField);
        mStartStopButton = (Button) findViewById(R.id.btn_start_stop_find_location);
        mStartStopButton.setOnClickListener(v -> startStopFindLocation());
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);


        // Bluetooth initialization
        mBeaconManager = new BeaconManager(this);
        mBeaconManager.setBackgroundScanPeriod(5000,5000);
        mBeaconManager.setRangingListener((Region region, List<Beacon> list) -> sendBluetoothFingerprints(list));
        mRegion = new Region("Ranged region", UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);

        // Initialization of Wifi
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mReceiverWifi = new WifiReceiver(mWifiManager);
        mReceiverWifi.addOnReceiveWifiScanResult(this::sendWifiResults);
        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if(!mWifiManager.isWifiEnabled())
        {
            mWifiManager.setWifiEnabled(true);
        }

        // MapView
        mImageView = (MapView) findViewById(R.id.imageView);
        mGetImageTask = new GetBitmapFromUrlTask();
        mGetImageTask.addOnBitmapRetrievedListener(bitmap -> {
            if(bitmap != null) {
                mImageView.setImageBitmap(bitmap);
            }else{
                Toast.makeText(GetLocationActivity.this, "Il n'y a pas de plan pour cet étage!", Toast.LENGTH_SHORT).show();
            }
        });
        mImageView.addOnMapViewClickedListener(new OnMapViewClickListener()
        {
            @Override
            public void onPointSelected(Point point){

            }

            @Override
            public void onScreenTapped(float x, float y)
            {

            }
        });
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiverWifi);
        if(mIsFindingLocation && mSwitchBluetooth.isChecked()) {
            mBeaconManager.stopRanging(mRegion);
        }
        if(mIsFindingLocation && mSwitchMagneticField.isChecked()) {
            mSensorManager.unregisterListener(mSensorEventListener, mMagneticField);
            mSensorManager.unregisterListener(mSensorEventListener, mAccelerometer);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        if(mIsFindingLocation && mSwitchBluetooth.isChecked()) {
            mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
        }
        if(mIsFindingLocation && mSwitchMagneticField.isChecked()) {
            mSensorManager.registerListener(mSensorEventListener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        super.onResume();
    }

    private void startStopFindLocation(){
        mIsFindingLocation = !mIsFindingLocation;
        if(mIsFindingLocation) {
            if(mSwitchWifi.isChecked()) {
                doInback();
            }
            if(mSwitchBluetooth.isChecked()) {
                mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
            }
            if(mSwitchMagneticField.isChecked()){
                mSensorManager.registerListener(mSensorEventListener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }

            Toast.makeText(GetLocationActivity.this, "Recherche de la localisation en cours" , Toast.LENGTH_SHORT).show();
            mStartStopButton.setText(R.string.stop);

            mSwitchWifi.setEnabled(false);
            mSwitchBluetooth.setEnabled(false);
            mSwitchMagneticField.setEnabled(false);
        }else{
            if(mSwitchBluetooth.isChecked()) {
                mBeaconManager.stopRanging(mRegion);
            }
            if(mSwitchMagneticField.isChecked()) {
                mSensorManager.unregisterListener(mSensorEventListener, mMagneticField);
                mSensorManager.unregisterListener(mSensorEventListener, mAccelerometer);
            }
            mSwitchWifi.setEnabled(true);
            mSwitchBluetooth.setEnabled(true);
            mSwitchMagneticField.setEnabled(true);
            Toast.makeText(GetLocationActivity.this, "Arrêt de la recherche" , Toast.LENGTH_SHORT).show();
            mStartStopButton.setText(R.string.start);
        }
    }

    public void doInback()
    {
        handler.postDelayed(() -> {
            if(mIsFindingLocation) {
                find();
                doInback();
            }
        }, 1000);
    }

    private void find(){
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (mReceiverWifi == null) {
            mReceiverWifi = new WifiReceiver(mWifiManager);
        }
        mWifiManager.startScan();
    }

    private void sendWifiResults(List<ScanResult> results){
        // Send Wifi samples only if Wifi is the only option checked
        // Otherwise the samples will be sent at the same time as the bluetooth ones
        if(mIsFindingLocation && mSwitchWifi.isChecked()) {
            mLastWifiScanResult = results;
            sendFingerprints();
        }
    }

    private void sendBluetoothFingerprints(List<Beacon> list)
    {
        if (!list.isEmpty() && mIsFindingLocation && mSwitchBluetooth.isChecked()) {
            mLastBluetoothScanResult = list;
            if(!mSwitchWifi.isChecked()) {
                sendFingerprints();
            }
        }
    }

    final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
            if(mIsFindingLocation && mSwitchMagneticField.isChecked()) {

                if (sensor.getType() == Sensor.TYPE_GRAVITY) {
                    mGravity[0] = event.values[0];
                    mGravity[1] = event.values[1];
                    mGravity[2] = event.values[2];
                } else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

                    mMagnetic[0] = event.values[0];
                    mMagnetic[1] = event.values[1];
                    mMagnetic[2] = event.values[2];

                    float[] R = new float[9];
                    float[] I = new float[9];
                    SensorManager.getRotationMatrix(R, I, mGravity, mMagnetic);
                    float[] A_D = event.values.clone();

                    // We don't need to calculate A_W[0] because the value should be 0 or close
                    //mNewBasis[0] = R[0] * A_D[0] + R[1] * A_D[1] + R[2] * A_D[2];
                    mNewBasis[1] = R[3] * A_D[0] + R[4] * A_D[1] + R[5] * A_D[2];
                    mNewBasis[2] = R[6] * A_D[0] + R[7] * A_D[1] + R[8] * A_D[2];

                    if (mIndex % 5 == 0) { // Refresh every second
                        if(!mSwitchWifi.isChecked() && !mSwitchBluetooth.isChecked()) {
                            sendFingerprints();
                        }
                    }
                    mIndex = (mIndex + 1) % 5;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Auto generated stub
        }
    };
    
    private void sendFingerprints(){
        JSONObject data = new JSONObject();
        JSONArray samples = new JSONArray();
        JSONObject sample;
        try {
            // Add current point
            if(mCurrentPoint != null){
                data.put("point", mCurrentPoint.toJSONObject());
            }

            //Wifi
            if(mSwitchWifi.isChecked() && mLastWifiScanResult != null) {
                for (ScanResult result : mLastWifiScanResult) {
                    sample = new JSONObject();
                    sample.put("rssi", result.level);
                    sample.put("bssid", result.BSSID);
                    samples.put(sample);
                }
                data.put("wifi", samples);
            }
            // Bluetooth
            if(mSwitchBluetooth.isChecked() && mLastBluetoothScanResult != null){
                samples = new JSONArray();
                for (Beacon result : mLastBluetoothScanResult) {
                    sample = new JSONObject();
                    sample.put("rssi", result.getRssi());
                    sample.put("uuid", result.getProximityUUID());
                    sample.put("major", result.getMajor());
                    sample.put("minor", result.getMinor());
                    samples.put(sample);
                }
                data.put("bluetooth", samples);
            }
            // Magnetic field
            if(mSwitchMagneticField.isChecked() && mMagnetic != null && mNewBasis != null) {
                Log.d("x", String.valueOf(mMagnetic[0]));
                sample = new JSONObject();
                sample.put("x", mMagnetic[0]);
                sample.put("y", mMagnetic[1]);
                sample.put("z", mMagnetic[2]);
                sample.put("north", mNewBasis[1]);
                sample.put("sky", mNewBasis[2]);
                data.put("magnetic", sample);
            }

        }catch(JSONException e){
            e.printStackTrace();
        }

        
        //Send request to get current location
        AndroidNetworking.post("http://api.ukonectdev.com/v1/find/location")
            .addJSONObjectBody(data)
            .setPriority(Priority.MEDIUM)
            .addHeaders("accept", "application/json")
            .build()
            .getAsJSONObject(new JSONObjectRequestListener() {
                @Override
                public void onResponse(JSONObject response) {

                    try {
                        if(mCurrentFloor == null || mCurrentFloor.getId() != response.getJSONObject("floor").getInt("id")){
                            mCurrentBuilding = new Building(response.getJSONObject("building"));
                            mCurrentFloor = new Floor(response.getJSONObject("floor"));

                            Toast.makeText(GetLocationActivity.this, "Position trouvée à " + mCurrentBuilding.getName() + "\n Chargement du plan.", Toast.LENGTH_SHORT).show();
                            mGetImageTask = new GetBitmapFromUrlTask();
                            mGetImageTask.execute(mCurrentFloor.getBlueprint());
                            mGetImageTask.addOnBitmapRetrievedListener(bitmap -> {
                                if(bitmap != null) {
                                    mImageView.setImageBitmap(bitmap);
                                }else{
                                    Toast.makeText(GetLocationActivity.this, "Il n'y a pas de plan pour cet étage!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        // Update position on the map
                        mCurrentPoint = new Point(response.getJSONObject("point"));

                        mImageView.clearPoints();

                        mImageView.addPoint(mCurrentPoint);

                        mRoomValue.setText(mCurrentPoint.getLocation());
                        mPointNameValue.setText(mCurrentPoint.getName());
                    }catch(JSONException e){
                        e.printStackTrace();
                    }
                }
                @Override
                public void onError(ANError error) {
                    if(error.getErrorCode() == 404){
                        mImageView.clearPoints();
                        Toast.makeText(GetLocationActivity.this, "Position introuvable.", Toast.LENGTH_SHORT).show();
                        Log.d("error", error.getErrorBody());
                    }else {
                        error.printStackTrace();
                        Log.d("error", error.getErrorBody());
                    }
                }
            });
    }
}
