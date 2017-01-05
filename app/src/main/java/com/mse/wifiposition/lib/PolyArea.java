package com.mse.wifiposition.lib;

import java.util.ArrayList;

/**
 * Polygon area
 */
public class PolyArea extends Area {
    ArrayList<Integer> mXPoints = new ArrayList<Integer>();
    ArrayList<Integer> mYPoints = new ArrayList<Integer>();

    // centroid point for this poly
    float mX;
    float mY;

    // number of points (don't rely on array size)
    int mPoints;

    // bounding box
    int mTop=-1;
    int mBottom=-1;
    int mLeft=-1;
    int mRight=-1;

    public PolyArea(int id, String name, String coords) {
        super(id,name);

        // split the list of coordinates into points of the
        // polygon and compute a bounding box
        String[] v = coords.split(",");

        int i=0;
        while ((i+1)<v.length) {
            int x = Integer.parseInt(v[i]);
            int y = Integer.parseInt(v[i+1]);
            mXPoints.add(x);
            mYPoints.add(y);
            mTop=(mTop==-1)?y:Math.min(mTop,y);
            mBottom=(mBottom==-1)?y:Math.max(mBottom,y);
            mLeft=(mLeft==-1)?x:Math.min(mLeft,x);
            mRight=(mRight==-1)?x:Math.max(mRight,x);
            i+=2;
        }
        mPoints=mXPoints.size();

        // add point zero to the end to make
        // computing area and centroid easier
        mXPoints.add(mXPoints.get(0));
        mYPoints.add(mYPoints.get(0));

        computeCentroid();
    }

    /**
     * area() and computeCentroid() are adapted from the implementation
     * of polygon.java  published from a princeton case study
     * The study is here: http://introcs.cs.princeton.edu/java/35purple/
     * The polygon.java source is here: http://introcs.cs.princeton.edu/java/35purple/Polygon.java.html
     */

    // return area of polygon
    public double area() {
        double sum = 0.0;
        for (int i = 0; i < mPoints; i++) {
            sum = sum + (mXPoints.get(i) * mYPoints.get(i+1)) - (mYPoints.get(i) * mXPoints.get(i+1));
        }
        sum = 0.5 * sum;
        return Math.abs(sum);
    }

    // compute the centroid of the polygon
    public void computeCentroid() {
        double cx = 0.0, cy = 0.0;
        for (int i = 0; i < mPoints; i++) {
            cx = cx + (mXPoints.get(i) + mXPoints.get(i+1)) * (mYPoints.get(i) * mXPoints.get(i+1) - mXPoints.get(i) * mYPoints.get(i+1));
            cy = cy + (mYPoints.get(i) + mYPoints.get(i+1)) * (mYPoints.get(i) * mXPoints.get(i+1) - mXPoints.get(i) * mYPoints.get(i+1));
        }
        cx /= (6 * area());
        cy /= (6 * area());
        mX=Math.abs((int)cx);
        mY=Math.abs((int)cy);
    }


    @Override
    public float getOriginX() {
        return mX;
    }

    @Override
    public float getOriginY() {
        return mY;
    }

    /**
     * This is a java port of the
     * W. Randolph Franklin algorithm explained here
     * http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
     */
    @Override
    public boolean isInArea(float testx, float testy)
    {
        int i, j;
        boolean c = false;
        for (i = 0, j = mPoints-1; i < mPoints; j = i++) {
            if ( ((mYPoints.get(i)>testy) != (mYPoints.get(j)>testy)) &&
                    (testx < (mXPoints.get(j)-mXPoints.get(i)) * (testy-mYPoints.get(i)) / (mYPoints.get(j)-mYPoints.get(i)) + mXPoints.get(i)) )
                c = !c;
        }
        return c;
    }

    // For debugging maps, it is occasionally helpful to see the
    // bounding box for the polygons
                /*
                @Override
                public void onDraw(Canvas canvas) {
                    // draw the bounding box
                        canvas.drawRect(mLeft * mResizeFactorX + mScrollLeft,
                                                mTop * mResizeFactorY + mScrollTop,
                                                mRight * mResizeFactorX + mScrollLeft,
                                                mBottom * mResizeFactorY + mScrollTop,
                                                textOutlinePaint);
                }
                */
}
