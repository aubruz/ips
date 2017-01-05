package com.mse.ips.activity;

import com.mse.wifiposition.R;
import com.mse.ips.lib.GetBitmapFromUrlTask;
import com.mse.ips.listener.OnMapViewClickListener;
import com.mse.ips.view.MapView;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;



public class MainActivity extends AppCompatActivity {
    private MapView mImageView = null;
    private GetBitmapFromUrlTask mGetImageTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mBtnSaveFingerprints = (Button) findViewById(R.id.btn_save_fingerprints);
        Button mBtnGetMyLocation = (Button) findViewById(R.id.btn_find_location);
        mBtnSaveFingerprints.setOnClickListener(v -> goToSaveFingerprintsActivity());
        mBtnGetMyLocation.setOnClickListener(v -> goToFindLocationActivity());

        mImageView = (MapView) findViewById(R.id.imageView);
        mGetImageTask = new GetBitmapFromUrlTask();
        mGetImageTask.execute("https://ukonect-dev.s3.amazonaws.com/blueprints/43284381");
        mGetImageTask.addOnBitmapRetrievedListener(bitmap -> mImageView.setImageBitmap(bitmap));
        mImageView.addPoint(150, 150, 50);
        mImageView.addOnMapViewClickedListener(new OnMapViewClickListener()
        {
            @Override
            public void onImageMapClicked(int id, MapView imageMap)
            {
                // when the area is tapped, show the name in a
            }

            @Override
            public void onBubbleClicked(int id)
            {
                // react to info bubble for area being tapped
            }

            @Override
            public void onScreenTapped(float x, float y)
            {
                mImageView.addPoint(x, y, 50);
                Log.d("Screen", " Taped on X: " + x + " Y: " + y);
            }
        });

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
