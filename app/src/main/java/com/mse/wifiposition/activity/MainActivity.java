package com.mse.wifiposition.activity;

import com.mse.wifiposition.R;
import com.mse.wifiposition.lib.GetBitmapFromUrlTask;
import com.mse.wifiposition.listener.OnBitmapRetrievedListener;
import com.mse.wifiposition.listener.OnMapViewClickListener;
import com.mse.wifiposition.view.MapView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;


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
