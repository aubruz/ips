package com.mse.wifiposition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity {
    private ImageMap mImageView = null;
    private GetImageTask mGetImageTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mBtnSaveFingerprints = (Button) findViewById(R.id.btn_save_fingerprints);
        Button mBtnGetMyLocation = (Button) findViewById(R.id.btn_find_location);
        mBtnSaveFingerprints.setOnClickListener(v -> goToSaveFingerprintsActivity());
        mBtnGetMyLocation.setOnClickListener(v -> goToFindLocationActivity());
        mImageView = (ImageMap) findViewById(R.id.imageView);
        mGetImageTask = new GetImageTask();
        mGetImageTask.execute("https://ukonect-dev.s3.amazonaws.com/blueprints/43284381");
        mImageView.addShape( "circle", "Name", "50,50,50", "area1");
        mImageView.addOnImageMapClickedHandler(new ImageMap.OnImageMapClickedHandler()
        {
            @Override
            public void onImageMapClicked(int id, ImageMap imageMap)
            {
                // when the area is tapped, show the name in a
                // text bubble
                imageMap.showBubble(id);
                imageMap.addShape( "circle", "Name", "50,50,50", "area1");
            }

            @Override
            public void onBubbleClicked(int id)
            {
                // react to info bubble for area being tapped
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

    class GetImageTask extends AsyncTask<String, Void, Bitmap> {
        private WeakReference<MapView> mActivity = null;

        public void link(MapView activity){
            mActivity = new WeakReference<MapView>(activity);
        }


        @Override
        protected Bitmap doInBackground(String... voids) {

            Bitmap bitmap=null;
            try {
                URL url = new URL(voids[0]);
                HttpURLConnection connection= (HttpURLConnection) url.openConnection();
                InputStream inputStream= connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mImageView.setImageBitmap(bitmap);
        }
    }

}
