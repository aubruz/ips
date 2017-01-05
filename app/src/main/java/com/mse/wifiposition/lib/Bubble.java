package com.mse.wifiposition.lib;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseArray;

import com.mse.wifiposition.listener.OnMapViewClickListener;

import java.util.ArrayList;

/**
 * information bubble class
 */
public class Bubble
{
    private Area mA;
    private String mText;
    private float mX;
    private float mY;
    private int mHeight;
    private int mWidth;
    private int mBaseline;
    private float mTop;
    private float mLeft;

    public Bubble(String text, float x, float y, float resizeFactorX, float resizeFactorY, Paint textPaint, int viewWidth, int expandWidth)
    {
        init(text,x,y, resizeFactorX, resizeFactorY, textPaint, viewWidth, expandWidth);
    }

    public Bubble(String text, int areaId, float resizeFactorX, float resizeFactorY, Paint textPaint, int viewWidth, int expandWidth, SparseArray<Area> idToArea)
    {
        mA = idToArea.get(areaId);
        if (mA != null) {
            float x = mA.getOriginX();
            float y = mA.getOriginY();
            init(text,x,y, resizeFactorX, resizeFactorY, textPaint, viewWidth, expandWidth);
        }
    }

    private void init(String text, float x, float y, float resizeFactorX, float resizeFactorY, Paint textPaint, int viewWidth, int expandWidth)
    {
        mText = text;
        mX = x*resizeFactorX;
        mY = y*resizeFactorY;
        Rect bounds = new Rect();
        textPaint.setTextScaleX(1.0f);
        textPaint.getTextBounds(text, 0, mText.length(), bounds);
        mHeight = bounds.bottom-bounds.top+20;
        mWidth = bounds.right-bounds.left+20;

        if (mWidth>viewWidth) {
            // too long for the display width...need to scale down
            float newscale=((float)viewWidth/(float)mWidth);
            textPaint.setTextScaleX(newscale);
            textPaint.getTextBounds(text, 0, mText.length(), bounds);
            mHeight = bounds.bottom-bounds.top+20;
            mWidth = bounds.right-bounds.left+20;
        }

        mBaseline = mHeight-bounds.bottom;
        mLeft = mX - (mWidth/2);
        mTop = mY - mHeight - 30;

        // try to keep the bubble on screen
        if (mLeft < 0) {
            mLeft = 0;
        }
        if ((mLeft + mWidth) > expandWidth) {
            mLeft = expandWidth - mWidth;
        }
        if (mTop < 0) {
            mTop = mY + 20;
        }
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

    public void onDraw(Canvas canvas, float scrollLeft, float scrollTop, Paint shadowPaint, Paint bubblePaint, Paint textPaint)
    {
        if (mA != null) {
            // Draw a shadow of the bubble
            float l = mLeft + scrollLeft + 4;
            float t = mTop + scrollTop + 4;
            canvas.drawRoundRect(new RectF(l,t,l+mWidth,t+mHeight), 20.0f, 20.0f, shadowPaint);
            Path path = new Path();
            float ox=mX+ scrollLeft+ 1;
            float oy=mY+scrollTop+ 1;
            int yoffset=-35;
            if (mTop > mY) {
                yoffset=35;
            }
            // draw shadow of pointer to origin
            path.moveTo(ox,oy);
            path.lineTo(ox-5,oy+yoffset);
            path.lineTo(ox+5+4,oy+yoffset);
            path.lineTo(ox, oy);
            path.close();
            canvas.drawPath(path, shadowPaint);

            // draw the bubble
            l = mLeft + scrollLeft;
            t = mTop + scrollTop;
            canvas.drawRoundRect(new RectF(l,t,l+mWidth,t+mHeight), 20.0f, 20.0f, bubblePaint);
            path = new Path();
            ox=mX+ scrollLeft;
            oy=mY+scrollTop;
            yoffset=-35;
            if (mTop > mY)
            {
                yoffset=35;
            }
            // draw pointer to origin
            path.moveTo(ox,oy);
            path.lineTo(ox-5,oy+yoffset);
            path.lineTo(ox+5,oy+yoffset);
            path.lineTo(ox, oy);
            path.close();
            canvas.drawPath(path, bubblePaint);

            // draw the message
            canvas.drawText(mText,l+(mWidth/2),t+mBaseline-10,textPaint);
        }
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