package com.mse.ips.activity;

import android.content.Context;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.CheckBox;
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
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class SaveFingerprintsActivity extends AppCompatActivity{
    private Menu mMenu = null;
    private int mIndex = 0;
    private MapView mImageView = null;
    private GetBitmapFromUrlTask mGetImageTask = null;
    private boolean mCanAddPoint = false;
    private Point mCurrentPoint = null;
    private WifiManager mWifiManager;
    private WifiReceiver mReceiverWifi;
    private Button mButton = null;
    private EditText mRoom = null;
    private EditText mPointName = null;
    private boolean mIsReccording = false;
    private CheckBox mSwitchBluetooth = null;
    private CheckBox mSwitchWifi = null;
    private CheckBox mSwitchMagneticField = null;
    private List<ScanResult> mLastWifiScanResult = null;
    private List<Beacon> mLastBluetoothScanResult = null;
    private LinkedList<Point> mLastAddedPoints = null;
    private Spinner mSpinnerFloors = null;
    private Spinner mSpinnerBuildings = null;
    private ArrayList<Floor> mFloorsList = null;
    private ArrayList<Building> mBuildingsList = null;
    private ArrayAdapter<Floor> mFloorsAdapter = null;
    private ArrayAdapter<Building> mBuildingsAdapter = null;
    private float mGravity[] = new float[3];
    private float mMagnetic[] = new float[3];
    private SensorManager mSensorManager = null;
    private Sensor mMagneticField = null;
    private Sensor mAccelerometer = null;
    float [] mNewBasis = null;
    private BeaconManager mBeaconManager;
    private Region mRegion;
    private final Handler mHandler = new Handler();
    private int mNumberOfFingerprintsTaken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_fingerprints);

        //Menu
        ActionBar ab = getSupportActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // Point list to enable revert option
        mLastAddedPoints = new LinkedList<>();

        // Widgets
        mButton = findViewById(R.id.button);
        mRoom = findViewById(R.id.room);
        mPointName = findViewById(R.id.point_name);
        mSwitchBluetooth = findViewById(R.id.switchBlutetooth);
        mSwitchWifi = findViewById(R.id.switchWifi);
        mSwitchMagneticField = findViewById(R.id.switchMagneticField);
        mSpinnerFloors = findViewById(R.id.spinnerFloors);
        mSpinnerBuildings =  findViewById(R.id.spinnerBuildings);
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
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mReceiverWifi = new WifiReceiver(mWifiManager);
        mReceiverWifi.addOnReceiveWifiScanResult(this::saveWifiScanResult);
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
        mImageView = findViewById(R.id.imageView);
        mGetImageTask = new GetBitmapFromUrlTask();
        mGetImageTask.execute("https://ukonect-dev.s3.amazonaws.com/blueprints/43284381");
        mGetImageTask.addOnBitmapRetrievedListener(bitmap -> {
            if(bitmap != null) {
                mImageView.setImageBitmap(bitmap);
            }else{
                Toast.makeText(SaveFingerprintsActivity.this, "Il n'y a pas de plan pour cet étage, veuillez un charger un!", Toast.LENGTH_SHORT).show();
            }
        });
        mImageView.addOnMapViewClickedListener(new OnMapViewClickListener()
        {
            @Override
            public void onPointSelected(Point point){
                if(!mIsReccording) {
                    mCurrentPoint = point;
                }
            }

            @Override
            public void onScreenTapped(float x, float y)
            {
                mCurrentPoint = null;
                if(mCanAddPoint) {
                    mCurrentPoint = mImageView.addPoint(x, y);
                    mLastAddedPoints.push(mCurrentPoint);
                    mMenu.findItem(R.id.cancel).setIcon(R.drawable.ic_menu_revert_active);
                    mMenu.findItem(R.id.cancel).setEnabled(true);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiverWifi);
        if(mIsReccording && mSwitchBluetooth.isChecked()) {
            mBeaconManager.stopRanging(mRegion);
        }
        if(mIsReccording && mSwitchMagneticField.isChecked()) {
            mSensorManager.unregisterListener(mSensorEventListener, mMagneticField);
            mSensorManager.unregisterListener(mSensorEventListener, mAccelerometer);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        if(mIsReccording && mSwitchBluetooth.isChecked()) {
            mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
        }
        if(mIsReccording && mSwitchMagneticField.isChecked()) {
            mSensorManager.registerListener(mSensorEventListener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_fingerprints, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case  R.id.refresh:
                // Refresh buildings list
                disableEditMode();
                getBuildings();
                break;
            case R.id.add_point:
                toggleEditMode();
                break;
            case R.id.cancel:
                if(mLastAddedPoints.size() != 0) {
                    Point lastPointAdded = mLastAddedPoints.pop();
                    if (lastPointAdded != null) {
                        if (lastPointAdded == mCurrentPoint) {
                            mCurrentPoint = null;
                        }
                        mImageView.removePoint(lastPointAdded);
                    }
                }
                if(mLastAddedPoints.size() == 0){
                    mMenu.findItem(R.id.cancel).setEnabled(false);
                    mMenu.findItem(R.id.cancel).setIcon(R.drawable.ic_menu_revert_inactive);
                }

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleEditMode(){
        if(!mCanAddPoint){
            enableEditMode();
            Toast.makeText(this, "Mode ajout de points", Toast.LENGTH_SHORT).show();
        }else{
            disableEditMode();
            Toast.makeText(this, "Lecture de la carte seulement", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableEditMode(){
        mCanAddPoint = true;
        MenuItem item = mMenu.findItem(R.id.add_point);
        item.setIcon(R.drawable.ic_add_point_active);
        if(mLastAddedPoints.size() != 0){
            mMenu.findItem(R.id.cancel).setEnabled(true);
            mMenu.findItem(R.id.cancel).setIcon(R.drawable.ic_menu_revert_active);
        }
    }

    private void disableEditMode() {
        mCanAddPoint = false;
        MenuItem item = mMenu.findItem(R.id.add_point);
        item.setIcon(R.drawable.ic_add_point);
        mMenu.findItem(R.id.cancel).setEnabled(false);
        mMenu.findItem(R.id.cancel).setIcon(R.drawable.ic_menu_revert_inactive);
    }

    AdapterView.OnItemSelectedListener OnSelectionListener =  new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (parent.getId()) {
                case R.id.spinnerBuildings:
                    Building building = (Building) parent.getItemAtPosition(position);
                    disableEditMode();
                    getFloorsFromBuildingId(building.getId());
                    //Toast.makeText(parent.getContext(), item.getName() + " " + item.getTag(), Toast.LENGTH_SHORT).show();
                    break;
                case R.id.spinnerFloors:
                    Floor floor = (Floor) parent.getItemAtPosition(position);
                    disableEditMode();
                    loadBlueprint(floor.getId());
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //
        }
    };

    private void loadBlueprint(int floorId){
        mGetImageTask = new GetBitmapFromUrlTask();
        mGetImageTask.execute("https://ukonect-dev.s3.amazonaws.com/blueprints/"+floorId);
        mGetImageTask.addOnBitmapRetrievedListener(bitmap -> mImageView.setImageBitmap(bitmap));

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
                        mLastAddedPoints.clear();
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
                        JSONObject building = buildings.getJSONObject(i);
                        mBuildingsList.add(new Building(building));
                    }
                    mBuildingsAdapter.notifyDataSetChanged();
                    Toast.makeText(SaveFingerprintsActivity.this, R.string.loading_finished, Toast.LENGTH_SHORT).show();
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
                            JSONObject floor = buildings.getJSONObject(i);
                            mFloorsList.add(new Floor(floor));
                        }
                        mFloorsAdapter.notifyDataSetChanged();
                        loadBlueprint(mFloorsList.get(0).getId());
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

    private void changeRecordingState()
    {
        mIsReccording = !mIsReccording;

        if(mIsReccording) {
            if(mCurrentPoint == null){
                Toast.makeText(this, "Veuillez sélectionner un point d'abord.", Toast.LENGTH_SHORT).show();
                mIsReccording = false;
                return;
            }

            if(!mSwitchWifi.isChecked() && !mSwitchBluetooth.isChecked() && !mSwitchMagneticField.isChecked()){
                Toast.makeText(this, "Aucune technologie n'a été choisie!", Toast.LENGTH_SHORT).show();
                return;
            }

            if(mCanAddPoint){
                disableEditMode();
            }

            mCurrentPoint.setLocation(mRoom.getText().toString());
            mCurrentPoint.setName(mPointName.getText().toString());

            //Prevent user to change point while reccording data
            mImageView.disableClick();

            if(mSwitchWifi.isChecked()) {
                doWifiScanInBackground();
            }
            if(mSwitchBluetooth.isChecked()) {
                mBeaconManager.connect(() -> mBeaconManager.startRanging(mRegion));
            }
            if(mSwitchMagneticField.isChecked()){
                mSensorManager.registerListener(mSensorEventListener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
            Toast.makeText(SaveFingerprintsActivity.this, "Enregistrement des empreintes" , Toast.LENGTH_SHORT).show();
            mNumberOfFingerprintsTaken = 0;
            mButton.setText(R.string.stop);
            // Disable switches
            mSwitchBluetooth.setEnabled(false);
            mSwitchWifi.setEnabled(false);
            mSwitchMagneticField.setEnabled(false);
        }else{
            mImageView.enableClick();
            // Enable switches
            mSwitchBluetooth.setEnabled(true);
            mSwitchWifi.setEnabled(true);
            mSwitchMagneticField.setEnabled(true);
            if(mSwitchBluetooth.isChecked()) {
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

    public void doWifiScanInBackground()
    {
        mHandler.postDelayed(() -> {
            if(mIsReccording && mSwitchWifi.isChecked()) {
                scanWifi();
                doWifiScanInBackground();
            }
        }, 1000);
    }

    private void scanWifi(){
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mReceiverWifi == null) {
            mReceiverWifi = new WifiReceiver(mWifiManager);
        }
        mWifiManager.startScan();
    }

    public void saveWifiScanResult(List<ScanResult> scanResults){
        if(mIsReccording && mSwitchWifi.isChecked() && !scanResults.isEmpty()) {
            mLastWifiScanResult = scanResults;
            sendFingerprints();
        }
    }

    private void saveBluetoothFingerprints(List<Beacon> list)
    {
        if (!list.isEmpty() && mIsReccording && mSwitchBluetooth.isChecked()) {
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
            if(mIsReccording && mSwitchMagneticField.isChecked()) {

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
                    mNewBasis = new float[3];
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
            // WIFI
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
            if(mSwitchBluetooth.isChecked() && mLastBluetoothScanResult != null) {
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
                sample = new JSONObject();
                sample.put("x", mMagnetic[0]);
                sample.put("y", mMagnetic[1]);
                sample.put("z", mMagnetic[2]);
                sample.put("north", mNewBasis[1]);
                sample.put("sky", mNewBasis[2]);
                data.put("magnetic", sample);
            }
            // Current point
            if(mCurrentPoint != null) {
                data.put("point", mCurrentPoint.toJSONObject());
            }

        }catch (JSONException e){
            e.printStackTrace();
        }

        Floor floor = (Floor) mSpinnerFloors.getSelectedItem();
        AndroidNetworking.post("http://api.ukonectdev.com/v1/floors/{floorID}/fingerprints")
            .addPathParameter("floorID", String.valueOf(floor.getId()))
            .addJSONObjectBody(data)
            .addHeaders("accept", "application/json")
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(new JSONObjectRequestListener() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        JSONObject point = response.getJSONObject("point");

                        mCurrentPoint.setId(point.getInt("id"));

                    }catch(JSONException e){
                        e.printStackTrace();
                    }
                    mNumberOfFingerprintsTaken ++;
                    Toast.makeText(SaveFingerprintsActivity.this, mNumberOfFingerprintsTaken + " empreinte(s) prise(s)", Toast.LENGTH_SHORT).show();
                    if(mNumberOfFingerprintsTaken >= 10){
                        changeRecordingState();
                        // IF the point name is an integer, we increment it
                        if(Tools.isInteger(mPointName.getText().toString())){
                            int nextValue = Integer.parseInt(mPointName.getText().toString()) + 1;
                            mPointName.setText(String.valueOf(nextValue));
                        }
                    }
                }

                @Override
                public void onError(ANError error) {
                    if(error.getErrorCode() == 404){
                        Toast.makeText(SaveFingerprintsActivity.this, "Erreur " + error.getErrorCode(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

}
