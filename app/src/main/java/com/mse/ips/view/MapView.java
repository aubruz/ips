package com.mse.ips.view;

import com.mse.ips.lib.Point;
import com.mse.ips.lib.TouchPoint;
import com.mse.ips.listener.OnMapViewClickListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.Scroller;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapView extends ImageView
{
    // mFitImageToScreen
    // if true - initial image resized to fit the screen, aspect ratio may be broken
    // if false- initial image resized so that no empty screen is visible, aspect ratio maintained
    //           image size will likely be larger than screen

    // by default, this is true
    private boolean mFitImageToScreen=false;

    // For certain images, it is best to always resize using the original
    // image bits. This requires keeping the original image in memory along with the
    // current sized version and thus takes extra memory.
    // If you always want to resize using the original, set mScaleFromOriginal to true
    // If you want to use less memory, and the image scaling up and down repeatedly
    // does not blur or loose quality, set mScaleFromOriginal to false

    // by default, this is false
    private boolean mScaleFromOriginal=true;

    private float mMaxSize = 1.5f;
    private final float MAXZOOM = 7.0f;

    /* Touch event handling variables */
    private VelocityTracker mVelocityTracker;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    private Scroller mScroller;

    private boolean mIsBeingDragged = false;
    private boolean mClickEnabled = true;

    SparseArray<TouchPoint> mTouchPoints = new SparseArray<>();
    TouchPoint mMainTouch=null;
    TouchPoint mPinchTouch=null;

    /* Pinch zoom */
    float mInitialDistance;
    boolean mZoomEstablished=false;
    int mLastDistanceChange=0;
    boolean mZoomPending=false;

    /* Paint objects for drawing info bubbles */
    Paint textPaint;
    Paint textOutlinePaint;
    Paint bubblePaint;
    Paint bubbleShadowPaint;
    Paint mBorderPaint;

    Rect mBorder;

    /*
     * Bitmap handling
     */
    Bitmap mImage;
    Bitmap mOriginal;

    // Info about the bitmap (sizes, scroll bounds)
    // initial size
    int mImageHeight;
    int mImageWidth;
    float mAspect;
    // scaled size
    int mExpandWidth;
    int mExpandHeight;
    // the right and bottom edges (for scroll restriction)
    int mRightBound;
    int mBottomBound;
    // the current zoom scaling (X and Y kept separate)
    protected float mResizeFactorX;
    protected float mResizeFactorY;
    // minimum height/width for the image
    int mMinWidth=-1;
    int mMinHeight=-1;
    // maximum height/width for the image
    int mMaxWidth=-1;
    int mMaxHeight=-1;

    // the position of the top left corner relative to the view
    int mScrollTop;
    int mScrollLeft;

    // view height and width
    int mViewHeight=-1;
    int mViewWidth=-1;

    // click handler list
    ArrayList<OnMapViewClickListener> mCallbackList;

    // list of points
    List<Point> mPointsList = new ArrayList<>();

    // accounting for screen density
    protected float densityFactor;

    /*
     * Constructors
     */
    public MapView(Context context) {
        super(context);
        init();
    }

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MapView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    public Point addPoint(Point p)
    {
        mPointsList.add(p);
        return p;
    }

    public Point addPoint(float x, float y)
    {
        Point p = new Point(x, y, mResizeFactorX, mResizeFactorY, mScrollLeft, mScrollTop);
        mPointsList.add(p);
        return p;
    }

    public Point addPoint(float x, float y, String location, String name, int id)
    {
        Point p = new Point(x, y, location, name, id);
        mPointsList.add(p);
        return p;
    }

    public void setPointInvisible(Point pointToChange){
        for(Point point: mPointsList){
            if(point.getId() == pointToChange.getId()){
                point.setInvisible(true);
                invalidate();
                return;
            }
        }
    }

    public void setPointVisible(Point pointToChange){
        for(Point point: mPointsList){
            if(point.getId() == pointToChange.getId()){
                point.setInvisible(false);
                invalidate();
                return;
            }
        }
    }

    public void clearPoints(){
        mPointsList.clear();
        invalidate();
    }

    public boolean removePoint(Point point)
    {
        for(int i=0; i<mPointsList.size(); i++){
            if(mPointsList.get(i).getX() == point.getX() && mPointsList.get(i).getY() == point.getY()){
                if(mPointsList.get(i).getId() != 0){
                    //TODO remove the point from the server as well
                }
                mPointsList.remove(i);
                invalidate();
                return true;
            }
        }
        return false;
    }

    public void loadPointsFromJSON(JSONArray points){
        mPointsList.clear();
        try {
            for (int i = 0; i < points.length(); i++) {
                JSONObject point = points.getJSONObject(i);
                addPoint((float)point.getDouble("x"), (float)point.getDouble("y"), point.getString("location"), point.getString("name"), point.getInt("id"));
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
    }   


    /**
     * initialize the view
     */
    private void init()
    {
        mFitImageToScreen = false;
        mScaleFromOriginal = true;
        mMaxSize = 10.5f;

        // set up paint objects
        initDrawingTools();
        mBorder = new Rect(0, 0, 0, 0);

        // create a scroller for flinging
        mScroller = new Scroller(getContext());

        // get some default values from the system for touch/drag/fling
        final ViewConfiguration configuration = ViewConfiguration
                .get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        //find out the screen density
        densityFactor = getResources().getDisplayMetrics().density;
    }

    /*
     * These methods will be called when images or drawables are set
     * in the XML for the view.  We handle either bitmaps or drawables
     * @see android.widget.ImageView#setImageBitmap(android.graphics.Bitmap)
     */
    @Override
    public void setImageBitmap(Bitmap bm)
    {
        if (mImage==mOriginal)
        {
            mOriginal=null;
        }
        else
        {
            mOriginal.recycle();
            mOriginal=null;
        }
        if (mImage != null)
        {
            mImage.recycle();
            mImage=null;
        }
        mImage = bm;
        mOriginal = bm;
        mImageHeight = mImage.getHeight();
        mImageWidth = mImage.getWidth();
        mAspect = (float)mImageWidth / mImageHeight;
        mMinWidth=-1;
        mMinHeight=-1;
        mMaxWidth=-1;
        mMaxHeight=-1;
        setInitialImageBounds();
    }

    /*
        setImageDrawable() is called by Android when the android:src attribute is set.
        To avoid this and use the more flexible setImageResource(),
        it is advised to omit the android:src attribute and call setImageResource() directly from code.
     */
    @Override
    public void setImageDrawable(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) drawable;
            setImageBitmap(bd.getBitmap());
        }
    }

    /**
     * setup the paint objects for drawing bubbles
     */
    private void initDrawingTools() {
        textPaint = new Paint();
        textPaint.setColor(0xFF000000);
        textPaint.setTextSize(30);
        textPaint.setTypeface(Typeface.SERIF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        textOutlinePaint = new Paint();
        textOutlinePaint.setColor(0xFF000000);
        textOutlinePaint.setTextSize(18);
        textOutlinePaint.setTypeface(Typeface.SERIF);
        textOutlinePaint.setTextAlign(Paint.Align.CENTER);
        textOutlinePaint.setStyle(Paint.Style.STROKE);
        textOutlinePaint.setStrokeWidth(2);

        bubblePaint=new Paint();
        bubblePaint.setColor(0xFFFFFFFF);
        bubbleShadowPaint=new Paint();
        bubbleShadowPaint.setColor(0xFF000000);

        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(10);
        mBorderPaint.setColor(Color.BLACK);
    }

    /*
     * Called by the scroller when flinging
     * @see android.view.View#computeScroll()
     */
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int oldX = mScrollLeft;
            int oldY = mScrollTop;

            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();

            if (oldX != x) {
                moveX(x-oldX);
            }
            if (oldY != y) {
                moveY(y-oldY);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int chosenWidth = chooseDimension(widthMode, widthSize);
        int chosenHeight = chooseDimension(heightMode, heightSize);

        setMeasuredDimension(chosenWidth, chosenHeight);
    }

    private int chooseDimension(int mode, int size) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY)
        {
            return size;
        }
        else
        {
            // (mode == MeasureSpec.UNSPECIFIED)
            return getPreferredSize();
        }
    }


    /**
     * set the initial bounds of the image
     */
    void setInitialImageBounds()
    {
        if (mFitImageToScreen)
        {
            setInitialImageBoundsFitImage();
        }
        else
        {
            setInitialImageBoundsFillScreen();
        }
        invalidate();
    }

    /**
     * setInitialImageBoundsFitImage sets the initial image size to match the
     * screen size.  aspect ratio may be broken
     */
    void setInitialImageBoundsFitImage()
    {
        if (mImage != null)
        {
            if (mViewWidth > 0)
            {
                mMinHeight = mViewHeight;
                mMinWidth = mViewWidth;
                mMaxWidth = (int)(mMinWidth * mMaxSize);
                mMaxHeight = (int)(mMinHeight * mMaxSize);

                mScrollTop = 0;
                mScrollLeft = 0;
                scaleBitmap(mMinWidth, mMinHeight);
            }
        }
    }

    /**
     * setInitialImageBoundsFillScreen sets the initial image size to so that there
     * is no uncovered area of the device
     */
    void setInitialImageBoundsFillScreen()
    {
        if (mImage != null)
        {
            if (mViewWidth > 0)
            {
                boolean resize=false;

                int newWidth=mImageWidth;
                int newHeight=mImageHeight;

                // The setting of these max sizes is very arbitrary
                // Need to find a better way to determine max size
                // to avoid attempts too big a bitmap and throw OOM
                if (mMinWidth==-1)
                {
                    // set minimums so that the largest
                    // direction we always filled (no empty view space)
                    // this maintains initial aspect ratio
                    if (mViewWidth > mViewHeight)
                    {
                        mMinWidth = mViewWidth;
                        mMinHeight = (int)(mMinWidth/mAspect);
                    } else {
                        mMinHeight = mViewHeight;
                        mMinWidth = (int)(mAspect*mViewHeight);
                    }
                    mMaxWidth = (int)(mMinWidth * MAXZOOM);
                    mMaxHeight = (int)(mMinHeight * MAXZOOM);
                }

                if (newWidth < mMinWidth) {
                    newWidth = mMinWidth;
                    newHeight = (int) (((float) mMinWidth / mImageWidth) * mImageHeight);
                    resize = true;
                }
                if (newHeight < mMinHeight) {
                    newHeight = mMinHeight;
                    newWidth = (int) (((float) mMinHeight / mImageHeight) * mImageWidth);
                    resize = true;
                }

                mScrollTop = 0;
                mScrollLeft = 0;

                // scale the bitmap
                if (resize) {
                    scaleBitmap(newWidth, newHeight);
                } else {
                    mExpandWidth=newWidth;
                    mExpandHeight=newHeight;

                    mResizeFactorX = ((float) newWidth / mImageWidth);
                    mResizeFactorY = ((float) newHeight / mImageHeight);
                    mRightBound = 0 - (mExpandWidth - mViewWidth);
                    mBottomBound = 0 - (mExpandHeight - mViewHeight);
                }
            }
        }
    }

    /**
     * Set the image to new width and height
     * create a new scaled bitmap and dispose of the previous one
     * recalculate scaling factor and right and bottom bounds
     * @param newWidth int
     * @param newHeight int
     */
    public void scaleBitmap(int newWidth, int newHeight) {
        // Technically since we always keep aspect ratio intact
        // we should only need to check one dimension.
        // Need to investigate and fix
        if ((newWidth > mMaxWidth) || (newHeight > mMaxHeight)) {
            newWidth = mMaxWidth;
            newHeight = mMaxHeight;
        }
        if ((newWidth < mMinWidth) || (newHeight < mMinHeight)) {
            newWidth = mMinWidth;
            newHeight = mMinHeight;
        }

        if ((newWidth != mExpandWidth) || (newHeight!=mExpandHeight)) {
            // NOTE: depending on the image being used, it may be
            //       better to keep the original image available and
            //       use those bits for resize.  Repeated grow/shrink
            //       can render some images visually non-appealing
            //       see comments at top of file for mScaleFromOriginal
            // try to create a new bitmap
            // If you get a recycled bitmap exception here, check to make sure
            // you are not setting the bitmap both from XML and in code
            Bitmap newbits = Bitmap.createScaledBitmap(mScaleFromOriginal ? mOriginal:mImage, newWidth,
                    newHeight, true);
            // if successful, fix up all the tracking variables
            if (newbits != null) {
                if (mImage!=mOriginal) {
                    mImage.recycle();
                }
                mImage = newbits;
                mExpandWidth=newWidth;
                mExpandHeight=newHeight;
                mResizeFactorX = ((float) newWidth / mImageWidth);
                mResizeFactorY = ((float) newHeight / mImageHeight);

                mRightBound = mExpandWidth>mViewWidth ? 0 - (mExpandWidth - mViewWidth) : 0;
                mBottomBound = mExpandHeight>mViewHeight ? 0 - (mExpandHeight - mViewHeight) : 0;
            }
        }
    }

    void resizeBitmap( int adjustWidth ) {
        int adjustHeight = (int)(adjustWidth / mAspect);
        scaleBitmap( mExpandWidth+adjustWidth, mExpandHeight+adjustHeight);
    }

    /**
     * watch for screen size changes and reset the background image
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // save device height width, we use it a lot of places
        mViewHeight = h;
        mViewWidth = w;

        // fix up the image
        setInitialImageBounds();
    }

    private int getPreferredSize() {
        return 300;
    }

    /**
     * the onDraw routine when we are using a background image
     *
     * @param canvas Canvas
     */
    protected void drawMap(Canvas canvas)
    {
        canvas.save();
        if (mImage != null)
        {
            if (!mImage.isRecycled())
            {
                canvas.drawBitmap(mImage, mScrollLeft, mScrollTop, null);
            }
        }
        canvas.restore();
    }

    protected void drawPoints(Canvas canvas)
    {
        for (Point p: mPointsList)
        {
            /*int key = mPointsList.keyAt(i);
            Point p = mPointsList.get(key);
            if (p != null)
            {*/
            p.onDraw(canvas, mResizeFactorX, mResizeFactorY, mScrollLeft, mScrollTop, textPaint);
            //}
        }
    }

    protected void drawBorder(Canvas canvas)
    {
        mBorder.set(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawRect(mBorder, mBorderPaint);
    }

    /**
     * Paint the view
     *   image first, location decorations next, bubbles on top
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        drawMap(canvas);
        drawPoints(canvas);
        drawBorder(canvas);
    }

    /*
     * Touch handler
     *   This handler manages an arbitrary number of points
     *   and detects taps, moves, flings, and zooms
     */
    public boolean onTouchEvent(MotionEvent ev)
    {
        if(!mClickEnabled){
            return false;
        }

        int id;

        if (mVelocityTracker == null)
        {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();

        int pointerCount = ev.getPointerCount();
        int index = 0;

        if (pointerCount > 1) {
            // If you are using new API (level 8+) use these constants
            // instead as they are much better names
            index = (action & MotionEvent.ACTION_POINTER_INDEX_MASK);
            index = index >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

            // for api 7 and earlier we are stuck with these
            // constants which are poorly named
            // ID refers to INDEX, not the actual ID of the pointer
            // index = (action & MotionEvent.ACTION_POINTER_ID_MASK);
            // index = index >> MotionEvent.ACTION_POINTER_ID_SHIFT;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // Clear all touch points
                // In the case where some view up chain is messing with our
                // touch events, it is possible to miss UP and POINTER_UP
                // events.  Whenever ACTION_DOWN happens, it is intended
                // to always be the first touch, so we will drop tracking
                // for any points that may have been orphaned
                for (int i = 0; i < mTouchPoints.size(); i++)
                {
                    int key = mTouchPoints.keyAt(i);
                    TouchPoint t = mTouchPoints.get(key);
                    if (t != null)
                    {
                        onLostTouch(t.getTrackingPointer());
                    }
                }

                // fall through planned
            case MotionEvent.ACTION_POINTER_DOWN:
                id = ev.getPointerId(index);
                onTouchDown(id,ev.getX(index),ev.getY(index));
                break;

            case MotionEvent.ACTION_MOVE:
                for (int p=0;p<pointerCount;p++) {
                    id = ev.getPointerId(p);
                    TouchPoint t = mTouchPoints.get(id);
                    if (t!=null) {
                        onTouchMove(t,ev.getX(p),ev.getY(p));
                    }
                    // after all moves, check to see if we need
                    // to process a zoom
                    processZoom();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                id = ev.getPointerId(index);
                onTouchUp(id);
                break;
            case MotionEvent.ACTION_CANCEL:
                // Clear all touch points on ACTION_CANCEL
                // according to the google devs, CANCEL means cancel
                // tracking every touch.
                // cf: http://groups.google.com/group/android-developers/browse_thread/thread/8b14591ead5608a0/ad711bf24520e5c4?pli=1
                for (int i = 0; i < mTouchPoints.size(); i++)
                {
                    int key = mTouchPoints.keyAt(i);
                    TouchPoint tp = mTouchPoints.get(key);
                    if (tp != null)
                    {
                        onLostTouch(tp.getTrackingPointer());
                    }
                }
                // let go of the velocity tracker per API Docs
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }
        return true;
    }


    void onTouchDown(int id, float x, float y) {
        // create a new touch point to track this ID
        TouchPoint t;
        synchronized (mTouchPoints) {
            // This test is a bit paranoid and research should
            // be done sot that it can be removed.  We should
            // not find a touch point for the id
            t = mTouchPoints.get(id);
            if (t == null) {
                t = new TouchPoint(id);
                mTouchPoints.put(id,t);
            }

            // for pinch zoom, we need to pick two touch points
            // they will be called Main and Pinch
            if (mMainTouch == null) {
                mMainTouch = t;
            } else {
                if (mPinchTouch == null) {
                    mPinchTouch=t;
                    // second point established, set up to
                    // handle pinch zoom
                    startZoom();
                }
            }
        }
        t.setPosition(x,y);
    }

    /*
     * Track pointer moves
     */
    void onTouchMove(TouchPoint t, float x, float y) {
        // mMainTouch will drag the view, be part of a
        // pinch zoom, or trigger a tap
        if (t == mMainTouch) {
            if (mPinchTouch == null) {
                // only on point down, this is a move
                final int deltaX = (int) (t.getX() - x);
                final int xDiff = (int) Math.abs(t.getX() - x);

                final int deltaY = (int) (t.getY() - y);
                final int yDiff = (int) Math.abs(t.getY() - y);

                if (!mIsBeingDragged) {
                    if ((xDiff > mTouchSlop) || (yDiff > mTouchSlop)) {
                        // start dragging about once the user has
                        // moved the point far enough
                        mIsBeingDragged = true;
                    }
                } else {
                    // being dragged, move the image
                    if (xDiff > 0) {
                        moveX(-deltaX);
                    }
                    if (yDiff > 0) {
                        moveY(-deltaY);
                    }
                    t.setPosition(x, y);
                }
            } else {
                // two fingers down means zoom
                t.setPosition(x, y);
                onZoom();
            }
        } else {
            if (t == mPinchTouch) {
                // two fingers down means zoom
                t.setPosition(x, y);
                onZoom();
            }
        }
    }

    /*
     * touch point released
     */
    void onTouchUp(int id) {
        synchronized (mTouchPoints) {
            TouchPoint t = mTouchPoints.get(id);
            if (t != null) {
                if (t == mMainTouch) {
                    if (mPinchTouch==null) {
                        // This is either a fling or tap
                        if (mIsBeingDragged) {
                            // view was being dragged means this is a fling
                            final VelocityTracker velocityTracker = mVelocityTracker;
                            velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

                            int xVelocity = (int) velocityTracker.getXVelocity();
                            int yVelocity = (int) velocityTracker.getYVelocity();

                            int xfling = Math.abs(xVelocity) > mMinimumVelocity ? xVelocity
                                    : 0;
                            int yfling = Math.abs(yVelocity) > mMinimumVelocity ? yVelocity
                                    : 0;

                            if ((xfling != 0) || (yfling != 0)) {
                                fling(-xfling, -yfling);
                            }

                            mIsBeingDragged = false;

                            // let go of the velocity tracker
                            if (mVelocityTracker != null) {
                                mVelocityTracker.recycle();
                                mVelocityTracker = null;
                            }
                        } else {
                            // no movement - this was a tap
                            onScreenTapped((int)mMainTouch.getX(), (int)mMainTouch.getY());
                        }
                    }
                    mMainTouch=null;
                    mZoomEstablished=false;
                }
                if (t == mPinchTouch) {
                    // lost the 2nd pointer
                    mPinchTouch=null;
                    mZoomEstablished=false;
                }
                mTouchPoints.remove(id);
                // shuffle remaining pointers so that we are still
                // tracking.  This is necessary for proper action
                // on devices that support > 2 touches
                regroupTouches();
            }
        }
    }

    /*
     * Touch handling varies from device to device, we may think we
     * are tracking an id which goes missing
     */
    void onLostTouch(int id) {
        synchronized (mTouchPoints) {
            TouchPoint t = mTouchPoints.get(id);
            if (t != null) {
                if (t == mMainTouch) {
                    mMainTouch=null;
                }
                if (t == mPinchTouch) {
                    mPinchTouch=null;
                }
                mTouchPoints.remove(id);
                regroupTouches();
            }
        }
    }

    /*
     * find a touch pointer that is not being used as main or pinch
     */
    TouchPoint getUnboundPoint() {
        TouchPoint ret=null;
        for (int i = 0; i < mTouchPoints.size(); i++)
        {
            int key = mTouchPoints.keyAt(i);
            TouchPoint tp = mTouchPoints.get(key);
            if (tp != null)
            {
                if ((tp!=mMainTouch)&&(tp!=mPinchTouch)) {
                    ret = tp;
                    break;
                }
            }
        }

        return ret;
    }

    /*
     * go through remaining pointers and try to have
     * MainTouch and then PinchTouch if possible
     */
    void regroupTouches() {
        int s=mTouchPoints.size();
        if (s>0) {
            if (mMainTouch == null) {
                if (mPinchTouch != null) {
                    mMainTouch=mPinchTouch;
                    mPinchTouch=null;
                } else {
                    mMainTouch=getUnboundPoint();
                }
            }
            if (s>1) {
                if (mPinchTouch == null) {
                    mPinchTouch=getUnboundPoint();
                    startZoom();
                }
            }
        }
    }

    /*
     * Called when the second pointer is down indicating that we
     * want to do a pinch-zoom action
     */
    void startZoom() {
        // this boolean tells the system that it needs to
        // initialize itself before trying to zoom
        // This is cleaner than duplicating code
        // see processZoom
        mZoomEstablished=false;
    }

    /*
     * one of the pointers for our pinch-zoom action has moved
     * Remember this until after all touch move actions are processed.
     */
    void onZoom() {
        mZoomPending=true;
    }

    /*
     * All touch move actions are done, do we need to zoom?
     */
    void processZoom() {
        if (mZoomPending) {
            // check pinch distance, set new scale factor
            float dx=mMainTouch.getX()-mPinchTouch.getX();
            float dy=mMainTouch.getY()-mPinchTouch.getY();
            float newDistance=(float)Math.sqrt((dx*dx)+(dy*dy));
            if (mZoomEstablished) {
                // baseline was set, check to see if there is enough
                // movement to resize
                int distanceChange=(int)(newDistance-mInitialDistance);
                int delta=distanceChange-mLastDistanceChange;
                if (Math.abs(delta)>mTouchSlop) {
                    mLastDistanceChange=distanceChange;
                    resizeBitmap(delta);
                    invalidate();
                }
            } else {
                // first run through after touches established
                // just set baseline
                mLastDistanceChange=0;
                mInitialDistance=newDistance;
                mZoomEstablished=true;
            }
            mZoomPending=false;
        }
    }

    /*
     * Screen tapped x, y is screen coord from upper left and does not account
     * for scroll
     */
    void onScreenTapped(int x, int y)
    {
        boolean missed = true;

        for (int i = 0; i<mPointsList.size(); i++)
        {
            Point p = mPointsList.get(i);
            p.deactivate();
            if(missed && p.isTouched(x, y, mResizeFactorX, mResizeFactorY, mScrollLeft, mScrollTop)){
                p.onSelected(mCallbackList);
                missed=false;
            }
        }

        invalidate();
        if (missed)
        {

            if (mCallbackList != null) {
                for (OnMapViewClickListener h : mCallbackList)
                {
                    h.onScreenTapped(x, y);
                }
            }
        }
    }

    // process a fling by kicking off the scroller
    public void fling(int velocityX, int velocityY)
    {
        int startX = mScrollLeft;
        int startY = mScrollTop;

        mScroller.fling(startX, startY, -velocityX, -velocityY, mRightBound, 0,
                mBottomBound, 0);

        invalidate();
    }

    /*
     * move the view to this x, y
     */
    public void moveTo(int x, int y) {
        mScrollLeft = x;
        if (mScrollLeft > 0) {
            mScrollLeft = 0;
        }
        if (mScrollLeft < mRightBound) {
            mScrollLeft = mRightBound;
        }
        mScrollTop=y;
        if (mScrollTop > 0) {
            mScrollTop = 0;
        }
        if (mScrollTop < mBottomBound) {
            mScrollTop = mBottomBound;
        }
        invalidate();
    }

    /*
     * move the view by this delta in X direction
     */
    public void moveX(int deltaX) {
        mScrollLeft = mScrollLeft + deltaX;
        if (mScrollLeft > 0) {
            mScrollLeft = 0;
        }
        if (mScrollLeft < mRightBound) {
            mScrollLeft = mRightBound;
        }
        invalidate();
    }

    /*
     * move the view by this delta in Y direction
     */
    public void moveY(int deltaY) {
        mScrollTop = mScrollTop + deltaY;
        if (mScrollTop > 0) {
            mScrollTop = 0;
        }
        if (mScrollTop < mBottomBound) {
            mScrollTop = mBottomBound;
        }
        invalidate();
    }

    public void showCurrentPointOnly(Point currentPoint){
        for(Point point: mPointsList){
            if(point.getId() != currentPoint.getId()) {
                point.setInvisible(true);
            }
        }
        invalidate();
    }

    public void showAllPoints(){
        for(Point point: mPointsList){
            point.setInvisible(false);
        }
        invalidate();
    }


    /*
     * on clicked handler add/remove support
     */
    public void addOnMapViewClickedListener( OnMapViewClickListener handler ) {
        if (handler != null) {
            if (mCallbackList == null) {
                mCallbackList = new ArrayList<>();
            }
            mCallbackList.add(handler);
        }
    }

    public void removeOnMapViewClickedListener( OnMapViewClickListener handler ) {
        if (mCallbackList != null && handler != null) {
            mCallbackList.remove(handler);
        }
    }

    public void disableClick(){
        mClickEnabled = false;
    }

    public void enableClick(){
        mClickEnabled = true;
    }

    public int getImageHeight() {
        return mImageHeight;
    }

    public int getImageWidth() {
        return mImageWidth;
    }
}