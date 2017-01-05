package com.mse.wifiposition.listener;

import android.graphics.Bitmap;

/**
 * When an image has been retrieved
 */
public interface OnBitmapRetrievedListener {

    /**
     * When the image has been retrieved
     * @param bitmap Bitmap
     */
    void onImageRetrieved(Bitmap bitmap);
}
