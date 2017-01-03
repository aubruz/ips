package com.mse.wifiposition;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mBtnSaveFingerprints = (Button) findViewById(R.id.btn_save_fingerprints);
        Button mBtnGetMyLocation = (Button) findViewById(R.id.btn_find_location);
        mBtnSaveFingerprints.setOnClickListener(v -> goToSaveFingerprintsActivity());
        mBtnGetMyLocation.setOnClickListener(v -> goToFindLocationActivity());
    }

    private void goToSaveFingerprintsActivity(){
        Intent intent = new Intent(MainActivity.this, SaveFingerprints.class);
        startActivity(intent);
    }

    private void goToFindLocationActivity(){
        Intent intent = new Intent(MainActivity.this, GetLocation.class);
        startActivity(intent);
    }

}
