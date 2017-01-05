package com.mse.ips.view;

import com.mse.ips.lib.Area;
import com.mse.ips.lib.Bubble;
import com.mse.ips.lib.CircleArea;
import com.mse.ips.lib.Point;
import com.mse.ips.lib.PolyArea;
import com.mse.ips.lib.RectArea;
import com.mse.ips.lib.TouchPoint;
import com.mse.ips.listener.OnMapViewClickListener;
import com.mse.ips.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.Scroller;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapView extends ImageView
{
    // mFitImageToScreen
    // if true - initial image resized to fit the screen, aspect ratio may be broken
    // if false- initial image resized so that no empty screen is visible, aspect ratio maintained
    //           image size will likely be larger than screen

    // by default, this is true
    private boolean mFitImageToScreen=true;

    // For certain images, it is best to always resize using the original
    // image bits. This requires keeping the original image in memory along with the
    // current sized version and thus takes extra memory.
    // If you always want to resize using the original, set mScaleFromOriginal to true
    // If you want to use less memory, and the image scaling up and down repeatedly
    // does not blur or loose quality, set mScaleFromOriginal to false

    // by default, this is false
    private boolean mScaleFromOriginal=false;

    // mMaxSize controls the maximum zoom size as a multiplier of the initial size.
    // Allowing size to go too large may result in memory problems.
    //  set this to 1.0f to disable resizing
    // by default, this is 1.5f
    private static final float defaultMaxSize = 1.5f;
    private float mMaxSize = 1.5f;
    private float mMaxZoom = 5.0f;

    /* Touch event handling variables */
    private VelocityTracker mVelocityTracker;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    private Scroller mScroller;

    private boolean mIsBeingDragged = false;

    HashMap<Integer,TouchPoint> mTouchPoints = new HashMap<Integer,TouchPoint>();
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

    /*
     * containers for the image map areas
     * using SparseArray<Area> instead of HashMap for the sake of performance
     */
    ArrayList<Area> mAreaList = new ArrayList<Area>();
    SparseArray<Area> mIdToArea = new SparseArray<Area>();

    // click handler list
    ArrayList<OnMapViewClickListener> mCallbackList;

    // list of open info bubbles
    SparseArray<Bubble> mBubbleMap = new SparseArray<Bubble>();
    List<Point> mPointMap = new ArrayList<Point>();

    // changed this from local variable to class field
    protected String mapName;

    // accounting for screen density
    protected float densityFactor;

    //possible to reduce memory consumption
    protected BitmapFactory.Options options;

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
        //loadAttributes(attrs);
    }

    public MapView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
        //loadAttributes(attrs);
    }

    /**
     * get the map name from the attributes and load areas from xml
     * @param attrs
     */
    /*private void loadAttributes(AttributeSet attrs)
    {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ImageMap);

        this.mFitImageToScreen = a.getBoolean(R.styleable.ImageMap_fitImageToScreen, true);
        this.mScaleFromOriginal = a.getBoolean(R.styleable.ImageMap_scaleFromOriginal, false);
        this.mMaxSize = a.getFloat(R.styleable.ImageMap_maxSizeFactor, defaultMaxSize);

        this.mapName = a.getString(R.styleable.ImageMap_map);
        if (mapName != null)
        {
            loadMap(mapName);
        }
    }*/

    /**
     * parse the maps.xml resource and pull out the areas
     * @param map - the name of the map to load
     */
    /*private void loadMap(String map) {
        boolean loading = false;
        try {
            XmlResourceParser xpp = getResources().getXml(R.xml.maps);

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_DOCUMENT) {
                    // Start document
                    //  This is a useful branch for a debug log if
                    //  parsing is not working
                } else if(eventType == XmlPullParser.START_TAG) {
                    String tag = xpp.getName();

                    if (tag.equalsIgnoreCase("map")) {
                        String mapname = xpp.getAttributeValue(null, "name");
                        if (mapname !=null) {
                            if (mapname.equalsIgnoreCase(map)) {
                                loading=true;
                            }
                        }
                    }
                    if (loading) {
                        if (tag.equalsIgnoreCase("area")) {
                            Area a=null;
                            String shape = xpp.getAttributeValue(null, "shape");
                            String coords = xpp.getAttributeValue(null, "coords");
                            String id = xpp.getAttributeValue(null, "id");

                            // as a name for this area, try to find any of these
                            // attributes
                            //  name attribute is custom to this impl (not standard in html area tag)
                            String name = xpp.getAttributeValue(null, "name");
                            if (name == null) {
                                name = xpp.getAttributeValue(null, "title");
                            }
                            if (name == null) {
                                name = xpp.getAttributeValue(null, "alt");
                            }

                            if ((shape != null) && (coords != null)) {
                                a = addShape(shape,name,coords,id);
                                if (a != null) {
                                    // add all of the area tag attributes
                                    // so that they are available to the
                                    // implementation if needed (see getAreaAttribute)
                                    for (int i=0;i<xpp.getAttributeCount();i++) {
                                        String attrName = xpp.getAttributeName(i);
                                        String attrVal = xpp.getAttributeValue(null,attrName);
                                        a.addValue(attrName, attrVal);
                                    }
                                }
                            }
                        }
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    String tag = xpp.getName();
                    if (tag.equalsIgnoreCase("map")) {
                        loading = false;
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException xppe) {
            // Having trouble loading? Log this exception
        } catch (IOException ioe) {
            // Having trouble loading? Log this exception
        }
    }*/

    /**
     * Create a new area and add to tracking
     * Changed this from private to protected!
     * @param shape
     * @param name
     * @param coords
     * @param id
     * @return
     */
    protected Area addShape( String shape, String name, String coords, String id)
    {
        Area a = null;
        String rid = id.replace("@+id/", "");
        int _id=0;

        try
        {
            Class<R.id> res = R.id.class;
            Field field = res.getField(rid);
            _id = field.getInt(null);
        }
        catch (Exception e)
        {
            _id = 0;
        }
        if (_id != 0)
        {
            if (shape.equalsIgnoreCase("rect"))
            {
                String[] v = coords.split(",");
                if (v.length == 4)
                {
                    a = new RectArea(_id, name, Float.parseFloat(v[0]),
                            Float.parseFloat(v[1]),
                            Float.parseFloat(v[2]),
                            Float.parseFloat(v[3]));
                }
            }
            if (shape.equalsIgnoreCase("circle"))
            {
                String[] v = coords.split(",");
                if (v.length == 3) {
                    a = new CircleArea(_id,name, Float.parseFloat(v[0]),
                            Float.parseFloat(v[1]),
                            Float.parseFloat(v[2])
                    );
                }
            }
            if (shape.equalsIgnoreCase("poly"))
            {
                a = new PolyArea(_id,name, coords);
            }
            if (a != null)
            {
                addArea(a);
            }
        }
        return a;
    }

    public void addArea( Area a )
    {
        mAreaList.add(a);
        mIdToArea.put(a.getId(), a);
    }

    public void addPoint(float x, float y, int radius){
        Point p = new Point(x, y, radius, mResizeFactorX, mResizeFactorY, mScrollLeft, mScrollTop);
        mPointMap.add(p);
    }

    public void addBubble(String text, int areaId )
    {
       /* if (mBubbleMap.get(areaId) == null)
        {
            Bubble b = new Bubble(text,areaId, mResizeFactorX, mResizeFactorY, textPaint, mViewWidth, mExpandWidth, mIdToArea);
            mBubbleMap.put(areaId,b);
        }*/
        Bubble b = new Bubble(text, 150, 150, mResizeFactorX, mResizeFactorY, textPaint, mViewWidth, mExpandWidth);
        mBubbleMap.put(areaId,b);
    }

    public void showBubble(String text, int areaId)
    {
        mBubbleMap.clear();
        addBubble(text,areaId);
        invalidate();
    }

    public void showBubble(int areaId)
    {
        mBubbleMap.clear();
        Area a = mIdToArea.get(areaId);
        if (a != null)
        {
            addBubble(a.getName(),areaId);
        }
        invalidate();
    }

    public void centerArea( int areaId )
    {
        Area a = mIdToArea.get(areaId);
        if (a != null)
        {
            float x = a.getOriginX()*mResizeFactorX;
            float y = a.getOriginY()*mResizeFactorY;
            int left = (int)((mViewWidth/2)-x);
            int top  = (int)((mViewHeight/2)-y);
            moveTo(left,top);
        }
    }

    public void centerAndShowArea(String text, int areaId)
    {
        centerArea(areaId);
        showBubble(text,areaId);
    }

    public void centerAndShowArea(int areaId)
    {
        Area a = mIdToArea.get(areaId);
        if (a != null) {
            centerAndShowArea(a.getName(),areaId);
        }
    }

    public String getAreaAttribute(int areaId, String key)
    {
        String value = null;
        Area a = mIdToArea.get(areaId);
        if (a != null)
        {
            value = a.getValue(key);
        }
        return value;
    }

    /**
     * initialize the view
     */
    private void init()
    {
        this.mFitImageToScreen = false;
        this.mScaleFromOriginal = true;
        this.mMaxSize = 10.5f;
        // set up paint objects
        initDrawingTools();

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
        setInitialImageBounds();
    }


    /*
    @Override
    public void setImageResource(int resId)
    {
        final String imageKey = String.valueOf(resId);
        BitmapHelper bitmapHelper = BitmapHelper.getInstance();
        Bitmap bitmap = bitmapHelper.getBitmapFromMemCache(imageKey);

        // 1 is the default setting, powers of 2 used to decrease image quality (and memory consumption)
        // TODO: enable variable inSampleSize for low-memory devices
        options = new BitmapFactory.Options();
        options.inSampleSize = 1;

        if (bitmap == null)
        {
            bitmap = BitmapFactory.decodeResource(getResources(), resId, options);
            bitmapHelper.addBitmapToMemoryCache(imageKey, bitmap);
        }
        setImageBitmap(bitmap);
    }*/

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
                    mMaxWidth = (int)(mMinWidth * mMaxZoom);
                    mMaxHeight = (int)(mMinHeight * mMaxZoom);
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
     * @param newWidth
     * @param newHeight
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

    void resizeBitmap( int amount ) {
        int adjustWidth = amount;
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
     * @param canvas
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

    protected void drawBubbles(Canvas canvas)
    {
        for (int i = 0; i < mBubbleMap.size(); i++)
        {
            int key = mBubbleMap.keyAt(i);
            Bubble b = mBubbleMap.get(key);
            if (b != null)
            {
                b.onDraw(canvas, mScrollLeft, mScrollTop, bubbleShadowPaint, bubblePaint, textPaint);
            }
        }

        for (Point p: mPointMap)
        {
            /*int key = mPointMap.keyAt(i);
            Point p = mPointMap.get(key);
            if (p != null)
            {*/
            p.onDraw(canvas, mResizeFactorX, mResizeFactorY, mScrollLeft, mScrollTop, textPaint);
            //}
        }
    }

    protected void drawLocations(Canvas canvas)
    {
        for (Area a : mAreaList)
        {
            a.onDraw(canvas, mResizeFactorX, mResizeFactorY, mScrollLeft, mScrollTop);
        }
    }

    /**
     * Paint the view
     *   image first, location decorations next, bubbles on top
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        drawMap(canvas);
        drawLocations(canvas);
        drawBubbles(canvas);
    }

    /*
     * Touch handler
     *   This handler manages an arbitrary number of points
     *   and detects taps, moves, flings, and zooms
     */
    public boolean onTouchEvent(MotionEvent ev)
    {
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
                Log.d("Point", "X: "+ (ev.getX() - mScrollLeft) + " Y: "+ev.getY());
                for ( TouchPoint t: mTouchPoints.values() ) {
                    onLostTouch(t.getTrackingPointer());
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
                for ( TouchPoint t: mTouchPoints.values() ) {
                    onLostTouch(t.getTrackingPointer());
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
        TouchPoint t=null;
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
            } else {
                // lost this ID somehow
                // This happens sometimes due to the way some
                // devices manage touch
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
        for (Integer i : mTouchPoints.keySet()) {
            TouchPoint p = mTouchPoints.get(i);
            if ((p!=mMainTouch)&&(p!=mPinchTouch)) {
                ret = p;
                break;
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
        if (mCallbackList != null) {
            for (OnMapViewClickListener h : mCallbackList)
            {
                h.onScreenTapped(x, y);
            }
        }

        boolean missed = true;
        boolean bubble = false;
        // adjust for scroll
        int testx = x-mScrollLeft;
        int testy = y-mScrollTop;


		/*
			Empirically, this works, but it's not guaranteed to be correct.
			Seems that we need to divide by densityFactor only if the picture is larger than the screen.
			When it is smaller than the screen, we don't need to do that.

			TODO: investigate this in detail.
		 */
        if (mResizeFactorX > 1)
        {
            testx = (int)(((float)testx/mResizeFactorX));
        }
        else
        {
            testx = (int)(((float)testx/mResizeFactorX)/densityFactor);
        }
        if (mResizeFactorY > 1)
        {
            testy = (int)(((float)testy/mResizeFactorY));
        }
        else
        {
            testy = (int)(((float)testy/mResizeFactorY)/densityFactor);
        }

        // check if bubble tapped first
        // in case a bubble covers an area we want it to
        // have precedent
        for (int i = 0 ; i < mBubbleMap.size() ; i++)
        {
            int key = mBubbleMap.keyAt(i);
            Bubble b = mBubbleMap.get(key);
            //it can still be null if there are no bubbles at all
            if (b != null)
            {
                if (b.isInArea((float)x-mScrollLeft,(float)y-mScrollTop))
                {
                    b.onTapped(mCallbackList);
                    bubble=true;
                    missed=false;
                    // only fire tapped for one bubble
                    break;
                }
            }
        }

        if (!bubble)
        {
            // then check for area taps
            for (Area a : mAreaList)
            {
                if (a.isInArea((float)testx,(float)testy))
                {
                    if (mCallbackList != null) {
                        for (OnMapViewClickListener h : mCallbackList)
                        {
                            h.onImageMapClicked(a.getId(), this);
                        }
                    }
                    missed=false;
                    // only fire clicked for one area
                    break;
                }
            }
        }

        if (missed)
        {
            // managed to miss everything, clear bubbles
            mBubbleMap.clear();
            invalidate();
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


    /*
     * on clicked handler add/remove support
     */
    public void addOnMapViewClickedListener( OnMapViewClickListener h ) {
        if (h != null) {
            if (mCallbackList == null) {
                mCallbackList = new ArrayList<OnMapViewClickListener>();
            }
            mCallbackList.add(h);
        }
    }

    public void removeOnMapViewClickedListener( OnMapViewClickListener h ) {
        if (mCallbackList != null) {
            if (h != null) {
                mCallbackList.remove(h);
            }
        }
    }

	/**
	 * Misc getters
	 */

    public float getmMaxSize()
    {
        return mMaxSize;
    }

    public boolean ismScaleFromOriginal()
    {
        return mScaleFromOriginal;
    }

    public boolean ismFitImageToScreen()
    {
        return mFitImageToScreen;
    }
}