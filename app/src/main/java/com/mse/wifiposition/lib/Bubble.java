package com.mse.wifiposition.lib;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseArray;

import java.util.ArrayList;

/**
 * information bubble class
 */
public class Bubble
{
    Area _a;
    String _text;
    float _x;
    float _y;
    int _h;
    int _w;
    int _baseline;
    float _top;
    float _left;

    public Bubble(String text, float x, float y, float resizeFactorX, float resizeFactorY, Paint textPaint, int viewWidth, int expandWidth)
    {
        init(text,x,y, resizeFactorX, resizeFactorY, textPaint, viewWidth, expandWidth);
    }

    public Bubble(String text, int areaId, float resizeFactorX, float resizeFactorY, Paint textPaint, int viewWidth, int expandWidth, SparseArray<Area> idToArea)
    {
        _a = idToArea.get(areaId);
        if (_a != null) {
            float x = _a.getOriginX();
            float y = _a.getOriginY();
            init(text,x,y, resizeFactorX, resizeFactorY, textPaint, viewWidth, expandWidth);
        }
    }

    private void init(String text, float x, float y, float resizeFactorX, float resizeFactorY, Paint textPaint, int viewWidth, int expandWidth)
    {
        _text = text;
        _x = x*resizeFactorX;
        _y = y*resizeFactorY;
        Rect bounds = new Rect();
        textPaint.setTextScaleX(1.0f);
        textPaint.getTextBounds(text, 0, _text.length(), bounds);
        _h = bounds.bottom-bounds.top+20;
        _w = bounds.right-bounds.left+20;

        if (_w>viewWidth) {
            // too long for the display width...need to scale down
            float newscale=((float)viewWidth/(float)_w);
            textPaint.setTextScaleX(newscale);
            textPaint.getTextBounds(text, 0, _text.length(), bounds);
            _h = bounds.bottom-bounds.top+20;
            _w = bounds.right-bounds.left+20;
        }

        _baseline = _h-bounds.bottom;
        _left = _x - (_w/2);
        _top = _y - _h - 30;

        // try to keep the bubble on screen
        if (_left < 0) {
            _left = 0;
        }
        if ((_left + _w) > expandWidth) {
            _left = expandWidth - _w;
        }
        if (_top < 0) {
            _top = _y + 20;
        }
    }

    public boolean isInArea(float x, float y) {
        boolean ret = false;

        if ((x>_left) && (x<(_left+_w))) {
            if ((y>_top)&&(y<(_top+_h))) {
                ret = true;
            }
        }

        return ret;
    }

    public void onDraw(Canvas canvas, float scrollLeft, float scrollTop, Paint shadowPaint, Paint bubblePaint, Paint textPaint)
    {
        if (_a != null) {
            // Draw a shadow of the bubble
            float l = _left + scrollLeft + 4;
            float t = _top + scrollTop + 4;
            canvas.drawRoundRect(new RectF(l,t,l+_w,t+_h), 20.0f, 20.0f, shadowPaint);
            Path path = new Path();
            float ox=_x+ scrollLeft+ 1;
            float oy=_y+scrollTop+ 1;
            int yoffset=-35;
            if (_top > _y) {
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
            l = _left + scrollLeft;
            t = _top + scrollTop;
            canvas.drawRoundRect(new RectF(l,t,l+_w,t+_h), 20.0f, 20.0f, bubblePaint);
            path = new Path();
            ox=_x+ scrollLeft;
            oy=_y+scrollTop;
            yoffset=-35;
            if (_top > _y)
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
            canvas.drawText(_text,l+(_w/2),t+_baseline-10,textPaint);
        }
    }

    public void onTapped(ArrayList<OnImageMapClickedHandler> callbackList) {
        // bubble was tapped, notify listeners
        if (callbackList != null) {
            for (OnImageMapClickedHandler h : callbackList) {
                h.onBubbleClicked(_a.getId());
            }
        }
    }
}