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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
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
import com.mse.ips.lib.Tools;
import com.mse.ips.lib.WifiReceiver;
import com.mse.ips.listener.OnMapViewClickListener;
import com.mse.ips.view.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ComputePrecision extends AppCompatActivity {
    private int mIndex = 0;
    private Point mPredictedPoint = null;
    private Button mStartStopButton = null;
    private Button mResetButton = null;
    private TextView mAveragePrecision = null;
    private TextView mStandartDeviation = null;
    private ArrayList<Float> mDistances = new ArrayList<>();
    private boolean mIsFindingLocation = false;
    private CheckBox mCheckboxBluetooth = null;
    private CheckBox mCheckboxWifi = null;
    private CheckBox mCheckboxMagneticField = null;
    private List<ScanResult> mLastWifiScanResult = null;
    private List<Beacon> mLastBluetoothScanResult = null;
    private BeaconManager mBeaconManager;
    private Region mRegion;
    private final Handler handler = new Handler();
    private WifiManager mWifiManager;
    private WifiReceiver mReceiverWifi;
    private float mGravity[] = new float[3];
    private float mMagnetic[] = new float[3];
    private SensorManager mSensorManager = null;
    private Sensor mMagneticField = null;
    private Sensor mAccelerometer = null;
    private float [] mNewBasis = new float[3];
    private Floor mCurrentFloor = null;
    private MapView mImageView = null;
    private Point mCurrentPosition = null;
    private Spinner mSpinnerFloors = null;
    private Spinner mSpinnerBuildings = null;
    private ArrayList<Floor> mFloorsList = null;
    private ArrayList<Building> mBuildingsList = null;
    private ArrayAdapter<Floor> mFloorsAdapter = null;
    private ArrayAdapter<Building> mBuildingsAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compute_precision);

        // Menu
        ActionBar ab = getSupportActionBar();
        if(ab !=null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // Widgets
        mAveragePrecision = (TextView) findViewById(R.id.average_precision);
        mStandartDeviation = (TextView) findViewById(R.id.standart_deviation);
        mCheckboxBluetooth = (CheckBox) findViewById(R.id.switchBlutetooth);
        mCheckboxWifi = (CheckBox) findViewById(R.id.switchWifi);
        mCheckboxMagneticField = (CheckBox) findViewById(R.id.switchMagneticField);
        mStartStopButton = (Button) findViewById(R.id.btn_start_stop_find_location);
        mResetButton = (Button) findViewById(R.id.btn_reset);
        mStartStopButton.setOnClickListener(v -> startStopFindLocation());
        mResetButton.setOnClickListener(v -> resetData());
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mSpinnerFloors = (Spinner) findViewById(R.id.spinnerFloors);
        mSpinnerBuildings = (Spinner) findViewById(R.id.spinnerBuildings);

        // Spinners
        mBuildingsList = new ArrayList<>();
        mBuildingsAdapter = new ArrayAdapter<>(ComputePrecision.this, android.R.layout.simple_spinner_item, mBuildingsList);
        mBuildingsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerBuildings.setAdapter(mBuildingsAdapter);
        mSpinnerBuildings.setOnItemSelectedListener(OnSelectionListener);

        mFloorsList = new ArrayList<>();
        mFloorsAdapter = new ArrayAdapter<>(ComputePrecision.this, android.R.layout.simple_spinner_item, mFloorsList);
        mFloorsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerFloors.setAdapter(mFloorsAdapter);
        mSpinnerFloors.setOnItemSelectedListener(OnSelectionListener);

        getBuildings();

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
        mImageView.addOnMapViewClickedListener(new OnMapViewClickListener()
        {
            @Override
            public void onPointSelected(Point point){
                mCurrentPosition = point;
            }

            @Override
            public void onScreenTapped(float x, float y)
            {
                mCurrentPosition = null;
            }
        });

    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiverWifi);
        if(mIsFindingLocation && mCheckboxBluetooth.isChecked()) {
            mBeaconManager.stopRanging(mRegion);
        }
        if(mIsFindingLocation && mCheckboxMagneticField.isChecked()) {
            mSensorManager.unregisterListener(mSensorEventListener, mMagneticField);
            mSensorManager.unregisterListener(mSensorEventListener, mAccelerometer);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        if(mIsFindingLocation && mCheckboxBluetooth.isChecked()) {
            mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
        }
        if(mIsFindingLocation && mCheckboxMagneticField.isChecked()) {
            mSensorManager.registerListener(mSensorEventListener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        super.onResume();
    }

    AdapterView.OnItemSelectedListener OnSelectionListener =  new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            switch (parent.getId()) {
                case R.id.spinnerBuildings:
                    Building building = (Building) parent.getItemAtPosition(position);
                    getFloorsFromBuildingId(building.getId());
                    break;
                case R.id.spinnerFloors:
                    Floor floor = (Floor) parent.getItemAtPosition(position);
                    loadBlueprint(floor.getId());
                    mCurrentFloor = floor;
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //
        }
    };

    private void loadBlueprint(int floorId){
        GetBitmapFromUrlTask mGetImageTask = new GetBitmapFromUrlTask();
        mGetImageTask.execute("https://ukonect-dev.s3.amazonaws.com/blueprints/"+floorId);
        mGetImageTask.addOnBitmapRetrievedListener(bitmap -> {
            if(bitmap != null) {
                mImageView.setImageBitmap(bitmap);
            }else{
                Toast.makeText(ComputePrecision.this, "Il n'y a pas de plan pour cet étage!", Toast.LENGTH_SHORT).show();
            }
        });

        AndroidNetworking.get("http://api.ukonectdev.com/v1/floors/{floor}/points")
            .addPathParameter("floor", String.valueOf(floorId))
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(new JSONObjectRequestListener() {
                @Override
                public void onResponse(JSONObject response) {
                    try{
                        JSONArray points = (JSONArray) response.get("points");
                        mImageView.loadPointsFromJSON(points);
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
                @Override
                public void onError(ANError error) {
                    error.printStackTrace();
                }
            });
    }

    private void getBuildings(){
        Toast.makeText(this, R.string.loading_buildings, Toast.LENGTH_SHORT).show();
        AndroidNetworking.get("http://api.ukonectdev.com/v1/buildings")
            .addHeaders("accept", "application/json")
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
                            mBuildingsList.add(new Building(row));
                        }
                        mBuildingsAdapter.notifyDataSetChanged();
                        Toast.makeText(ComputePrecision.this, R.string.loading_finished, Toast.LENGTH_SHORT).show();
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
                @Override
                public void onError(ANError error) {
                    error.printStackTrace();
                }
            });
    }

    private void getFloorsFromBuildingId(int buildingId){
        AndroidNetworking.get("http://api.ukonectdev.com/v1/buildings/{buildingID}/floors")
            .addPathParameter("buildingID", String.valueOf(buildingId))
            .addHeaders("accept", "application/json")
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
                             mFloorsList.add(new Floor(row));
                         }
                         mFloorsAdapter.notifyDataSetChanged();
                         loadBlueprint(mFloorsList.get(0).getId());
                         mCurrentFloor = mFloorsList.get(0);
                     }catch (JSONException e){
                         e.printStackTrace();
                     }
                 }
                 @Override
                 public void onError(ANError error) {
                     error.printStackTrace();
                 }
             }
            );
    }

    private void startStopFindLocation(){
        mIsFindingLocation = !mIsFindingLocation;
        if(mIsFindingLocation) {

            if(mCurrentPosition == null){
                Toast.makeText(this, "Veuillez indiquer votre position d'abord.", Toast.LENGTH_SHORT).show();
                mIsFindingLocation = false;
                return;
            }

            if(mCheckboxWifi.isChecked()) {
                doInback();
            }
            if(mCheckboxBluetooth.isChecked()) {
                mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
            }
            if(mCheckboxMagneticField.isChecked()){
                mSensorManager.registerListener(mSensorEventListener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }

            Toast.makeText(ComputePrecision.this, "Calcul de la précision" , Toast.LENGTH_SHORT).show();
            mStartStopButton.setText(R.string.stop);

            mImageView.showCurrentPointOnly(mCurrentPosition);
            mCheckboxWifi.setEnabled(false);
            mCheckboxBluetooth.setEnabled(false);
            mCheckboxMagneticField.setEnabled(false);
            mSpinnerBuildings.setEnabled(false);
            mSpinnerFloors.setEnabled(false);
            mImageView.disableClick();
        }else{
            if(mCheckboxBluetooth.isChecked()) {
                mBeaconManager.stopRanging(mRegion);
            }
            if(mCheckboxMagneticField.isChecked()) {
                mSensorManager.unregisterListener(mSensorEventListener, mMagneticField);
                mSensorManager.unregisterListener(mSensorEventListener, mAccelerometer);
            }
            mImageView.enableClick();
            mImageView.showAllPoints();
            mCheckboxWifi.setEnabled(true);
            mCheckboxBluetooth.setEnabled(true);
            mCheckboxMagneticField.setEnabled(true);
            mSpinnerBuildings.setEnabled(true);
            mSpinnerFloors.setEnabled(true);
            Toast.makeText(ComputePrecision.this, "Arrêt de la recherche" , Toast.LENGTH_SHORT).show();
            mStartStopButton.setText(R.string.start);
        }
    }

    public void resetData(){
        mDistances.clear();
        mAveragePrecision.setText("0");
        mStandartDeviation.setText("0");
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
        if(mIsFindingLocation && mCheckboxWifi.isChecked()) {
            mLastWifiScanResult = results;
            sendFingerprints();
        }
    }

    private void sendBluetoothFingerprints(List<Beacon> list)
    {
        if (!list.isEmpty() && mIsFindingLocation && mCheckboxBluetooth.isChecked()) {
            mLastBluetoothScanResult = list;
            if(!mCheckboxWifi.isChecked()) {
                sendFingerprints();
            }
        }
    }

    final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
            if(mIsFindingLocation && mCheckboxMagneticField.isChecked()) {

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
                        if(!mCheckboxWifi.isChecked() && !mCheckboxBluetooth.isChecked()) {
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
            if(mCurrentPosition != null){
                data.put("point", mCurrentPosition.toJSONObject());
            }

            //Wifi
            if(mCheckboxWifi.isChecked() && mLastWifiScanResult != null) {
                for (ScanResult result : mLastWifiScanResult) {
                    sample = new JSONObject();
                    sample.put("rssi", result.level);
                    sample.put("bssid", result.BSSID);
                    samples.put(sample);
                }
                data.put("wifi", samples);
            }
            // Bluetooth
            if(mCheckboxBluetooth.isChecked() && mLastBluetoothScanResult != null){
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
            if(mCheckboxMagneticField.isChecked() && mMagnetic != null && mNewBasis != null) {
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
                        if(mCurrentFloor != null && mCurrentFloor.getId() != response.getJSONObject("floor").getInt("id")){
                            if(mCurrentFloor.getId() != response.getJSONObject("floor").getInt("id")){
                                Toast.makeText(ComputePrecision.this, "Position introuvable à cet étage.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        if(mPredictedPoint != null && mPredictedPoint.getId() != mCurrentPosition.getId()){
                            mImageView.setPointInvisible(mPredictedPoint);
                        }

                        mPredictedPoint = new Point(response.getJSONObject("point"));

                        if(mPredictedPoint.getId() != mCurrentPosition.getId()) {
                            mImageView.setPointVisible(mPredictedPoint);
                        }

                        mDistances.add(Tools.distance(mCurrentPosition, mPredictedPoint, mCurrentFloor, mImageView));

                        float average = Tools.getAveragePrecision(mDistances);

                        mAveragePrecision.setText(String.format(Locale.FRENCH, "%.2f",average));

                        mStandartDeviation.setText(String.format(Locale.FRENCH, "%.2f", Tools.getStandartDeviation(mDistances, average)));
                        
                    }catch(JSONException e){
                        e.printStackTrace();
                    }
                }
                @Override
                public void onError(ANError error) {
                    if(error.getErrorCode() == 404){
                        mImageView.clearPoints();
                        Toast.makeText(ComputePrecision.this, "Position introuvable.", Toast.LENGTH_SHORT).show();
                        Log.d("error", error.getErrorBody());
                    }else {
                        error.printStackTrace();
                        Log.d("error", error.getErrorBody());
                    }
                }
            });
    }
}
