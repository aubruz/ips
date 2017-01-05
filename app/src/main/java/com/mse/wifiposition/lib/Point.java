package com.mse.wifiposition.lib;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mse.wifiposition.listener.OnMapViewClickListener;

import java.util.ArrayList;

/**
 * information bubble class
 */
public class Point
{
    Area mA;
    String mText;
    float mX;
    float mY;
    int mRadius;
    int mHeight;
    int mWidth;
    int mBaseline;
    float mTop;
    float mLeft;

    public Point(float x, float y, int radius, float resizeFactorX, float resizeFactorY, int scrollLeft, int scrollTop)
    {
        mX = (x - scrollLeft) / resizeFactorX;
        mY = (y - scrollTop) / resizeFactorY;
        mRadius = radius;
    }

    public boolean isInArea(float x, float y) {
        boolean ret = false;

        if ((x>mLeft) && (x<(mLeft+mWidth))) {
            if ((y>mTop)&&(y<(mTop+mHeight))) {
                ret = true;
            }
        }

        return ret;
    }

    public void onDraw(Canvas canvas, float resizeFactorX, float resizeFactorY, int scrollLeft, int scrollTop, Paint textPaint)
    {
        canvas.drawCircle(mX * resizeFactorX + scrollLeft, mY * resizeFactorY + scrollTop, 50, textPaint);

    }

    public void onTapped(ArrayList<OnMapViewClickListener> callbackList) {
        // bubble was tapped, notify listeners
        if (callbackList != null) {
            for (OnMapViewClickListener h : callbackList) {
                h.onBubbleClicked(mA.getId());
            }
        }
    }
}