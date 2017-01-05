package com.mse.wifiposition.lib;

/**
 * Rectangle Area
 */
public class RectArea extends Area {
    float mLeft;
    float mTop;
    float mRight;
    float mBottom;


    public RectArea(int id, String name, float left, float top, float right, float bottom) {
        super(id,name);
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
    }

    public boolean isInArea(float x, float y) {
        boolean ret = false;
        if ((x > mLeft) && (x < mRight)) {
            if ((y > mTop) && (y < mBottom)) {
                ret = true;
            }
        }
        return ret;
    }

    public float getOriginX() {
        return mLeft;
    }

    public float getOriginY() {
        return mTop;
    }
}
