package com.mse.ips.activity;

import com.mse.ips.R;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Buttons
        Button mBtnSaveFingerprints = (Button) findViewById(R.id.btn_save_fingerprints);
        Button mBtnGetMyLocation = (Button) findViewById(R.id.btn_find_location);
        Button mBtnShowInfo = (Button) findViewById(R.id.btn_show_info);
        Button mBtnComputePrecision = (Button) findViewById(R.id.btn_compute_precision);

        // On click
        mBtnSaveFingerprints.setOnClickListener(v -> goToSaveFingerprintsActivity());
        mBtnGetMyLocation.setOnClickListener(v -> goToFindLocationActivity());
        mBtnShowInfo.setOnClickListener(v -> goToShowInfoActivity());
        mBtnComputePrecision.setOnClickListener(v -> goToComputePrecisionActivity());
    }

    private void goToSaveFingerprintsActivity(){
        Intent intent = new Intent(MainActivity.this, SaveFingerprintsActivity.class);
        startActivity(intent);
    }

    private void goToFindLocationActivity(){
        Intent intent = new Intent(MainActivity.this, GetLocationActivity.class);
        startActivity(intent);
    }

    private void goToShowInfoActivity(){
        Intent intent = new Intent(MainActivity.this, ShowInfoActivity.class);
        startActivity(intent);
    }

    private void goToComputePrecisionActivity(){
        Intent intent = new Intent(MainActivity.this, ComputePrecision.class);
        startActivity(intent);
    }

}
