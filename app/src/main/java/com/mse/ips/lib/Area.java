package com.mse.ips.lib;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import java.util.HashMap;

/**
 *  Area is abstract Base for tappable map areas
 *   descendants provide hit test and focal point
 */
public abstract class Area {
    int mId;
    String mName;
    HashMap<String,String> mValues;
    Bitmap mDecoration=null;

    public Area(int id, String name) {
        mId = id;
        if (name != null) {
            mName = name;
        }
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    // all xml values for the area are passed to the object
    // the default impl just puts them into a hashmap for
    // retrieval later
    public void addValue(String key, String value) {
        if (mValues == null) {
            mValues = new HashMap<String,String>();
        }
        mValues.put(key, value);
    }

    public String getValue(String key) {
        String value=null;
        if (mValues!=null) {
            value=mValues.get(key);
        }
        return value;
    }

    // a method for setting a simple decorator for the area
    public void setBitmap(Bitmap b) {
        mDecoration = b;
    }

    // an onDraw is set up to provide an extensible way to
    // decorate an area.  When drawing remember to take the
    // scaling and translation into account
    public void onDraw(Canvas canvas, float resizeFactorX, float resizeFactorY, int scrollLeft, int scrollTop)
    {
        if (mDecoration != null)
        {
            float x = (getOriginX() * resizeFactorX) + scrollLeft - 17;
            float y = (getOriginY() * resizeFactorY) + scrollTop - 17;
            canvas.drawBitmap(mDecoration, x, y, null);
        }
    }

    public abstract boolean isInArea(float x, float y);
    public abstract float getOriginX();
    public abstract float getOriginY();
}