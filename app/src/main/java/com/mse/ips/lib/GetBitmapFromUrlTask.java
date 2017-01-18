package com.mse.ips.lib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.Toast;

import com.mse.ips.listener.OnBitmapRetrievedListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class GetBitmapFromUrlTask extends AsyncTask<String, Void, Bitmap> {
    private ArrayList<OnBitmapRetrievedListener> mListeners = null;

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
        if(mListeners != null) {
            for (OnBitmapRetrievedListener bl : mListeners) {
                bl.onImageRetrieved(bitmap);
            }
        }
    }

    /*
     * On retrieved listener add/remove support
     */
    public void addOnBitmapRetrievedListener( OnBitmapRetrievedListener listener ) {
        if (listener != null) {
            if (mListeners == null) {
                mListeners = new ArrayList<>();
            }
            mListeners.add(listener);
        }
    }

    public void removeOnBitmapRetrievedListener( OnBitmapRetrievedListener listener ) {
        if (mListeners != null) {
            if (listener != null) {
                mListeners.remove(listener);
            }
        }
    }
}
