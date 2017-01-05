package com.mse.ips.lib;

/*
 * A class to track touches
 */
public class TouchPoint {
    int mId;
    float mX;
    float mY;

    public TouchPoint(int id) {
        mId = id;
        mX = 0f;
        mY = 0f;
    }

    public int getTrackingPointer() {
        return mId;
    }

    public void setPosition(float x, float y) {
        if ((mX != x) || (mY != y)) {
            mX = x;
            mY = y;
        }
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }
}
