package com.mse.wifiposition;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {
    private final float alpha = (float) 0.8;
    private float gravity[] = new float[3];
    private float magnetic[] = new float[3];
    private SensorManager mSensorManager = null;
    private Sensor mMagneticField = null;
    private Sensor mAccelerometer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mBtnSaveFingerprints = (Button) findViewById(R.id.btn_save_fingerprints);
        Button mBtnGetMyLocation = (Button) findViewById(R.id.btn_find_location);
        mBtnSaveFingerprints.setOnClickListener(v -> goToSaveFingerprintsActivity());
        mBtnGetMyLocation.setOnClickListener(v -> goToFindLocationActivity());

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    }

    private void goToSaveFingerprintsActivity(){
        Intent intent = new Intent(MainActivity.this, SaveFingerprints.class);
        startActivity(intent);
    }

    private void goToFindLocationActivity(){
        Intent intent = new Intent(MainActivity.this, GetLocation.class);
        startActivity(intent);
    }

    final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                // Isolate the force of gravity with the low-pass filter.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
                //Log.d("Field","\nXX :"+event.values[0]+"\nYY :"+event.values[1]+"\nZZ :"+event.values[2]);
            } else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

                magnetic[0] = event.values[0];
                magnetic[1] = event.values[1];
                magnetic[2] = event.values[2];

                float[] R = new float[9];
                float[] I = new float[9];
                SensorManager.getRotationMatrix(R, I, gravity, magnetic);
                float [] A_D = event.values.clone();
                float [] A_W = new float[3];
                A_W[0] = R[0] * A_D[0] + R[1] * A_D[1] + R[2] * A_D[2]; //Should be 0 or close
                A_W[1] = R[3] * A_D[0] + R[4] * A_D[1] + R[5] * A_D[2];
                A_W[2] = R[6] * A_D[0] + R[7] * A_D[1] + R[8] * A_D[2];

                Log.d("Field","\nX :"+A_W[0]+"\nY :"+A_W[1]+"\nZ :"+A_W[2]);

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onResume(){
        super.onResume();
        mSensorManager.registerListener(mSensorEventListener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(mSensorEventListener, mMagneticField);
        mSensorManager.unregisterListener(mSensorEventListener, mAccelerometer);
    }
}
