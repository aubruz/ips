package com.mse.ips.lib;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.mse.ips.listener.OnMapViewClickListener;

import java.util.ArrayList;

/**
 * Point class
 */
public class Point
{
    private int mId = 0;
    private float mX;
    private float mY;
    private final float mRadius = 25.0f;
    private boolean mIsActive = true;

    public Point(float x, float y, float resizeFactorX, float resizeFactorY, int scrollLeft, int scrollTop)
    {
        mX = (x - scrollLeft) / resizeFactorX;
        mY = (y - scrollTop) / resizeFactorY;
    }

    public void onDraw(Canvas canvas, float resizeFactorX, float resizeFactorY, int scrollLeft, int scrollTop, Paint textPaint)
    {
        if(mIsActive){
            textPaint.setColor(Color.parseColor("#5cc7c0"));
            canvas.drawCircle(mX * resizeFactorX + scrollLeft, mY * resizeFactorY + scrollTop, mRadius + 10, textPaint);
        }
        textPaint.setColor(Color.BLACK);
        canvas.drawCircle(mX * resizeFactorX + scrollLeft, mY * resizeFactorY + scrollTop, mRadius, textPaint);

    }

    public boolean isTouched(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx + dy <= mRadius){
            return true;
        }
        if(dx > mRadius) {
            return false;
        }
        if(dy > mRadius){
            return false;
        }
        if( Math.pow(dx,2) +  Math.pow(dy,2) <= Math.pow(mRadius,2) ) {
            return true;
        }

        return false;
    }

    public void onSelected(ArrayList<OnMapViewClickListener> callbackList) {
        mIsActive = true;
        if (callbackList != null) {
            for (OnMapViewClickListener h : callbackList) {
                h.onPointSelected(this);
            }
        }
    }

    public float getX() {
        return this.mX;
    }

    public float getY() {
        return this.mY;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public int getId() {
        return this.mId;
    }

    public void deactivate(){
        this.mIsActive = false;
    }
}