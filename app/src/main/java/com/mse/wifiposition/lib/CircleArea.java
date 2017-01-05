package com.mse.wifiposition.lib;

/**
 * Circle Area
 */
public class CircleArea extends Area {
    float mX;
    float mY;
    float mRadius;

    public CircleArea(int id, String name, float x, float y, float radius) {
        super(id,name);
        mX = x;
        mY = y;
        mRadius = radius;

    }

    public boolean isInArea(float x, float y) {
        boolean ret = false;

        float dx = mX-x;
        float dy = mY-y;

        // if tap is less than radius distance from the center
        float d = (float)Math.sqrt((dx*dx)+(dy*dy));
        if (d<mRadius) {
            ret = true;
        }

        return ret;
    }

    public float getOriginX() {
        return mX;
    }

    public float getOriginY() {
        return mY;
    }
}