package com.mse.wifiposition;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;


public class MapView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener{
    // Le holder
    SurfaceHolder mSurfaceHolder;
    // Le thread dans lequel le dessin se fera
    DrawingThread mThread;
    GetImageTask mGetImageTask = null;

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;
    private Bitmap mBitmap;
    private Canvas  mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint mPaint;

    public MapView (Context context) {
        super(context);
        //init();
    }

    public MapView (Context context, AttributeSet attrs) {
        super(context, attrs);
        //init();
    }

    private void init(){
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mPaint = new Paint();
        mPaint.setColor(0xFFFF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(12);
        this.invalidate();

        this.setOnTouchListener(this);

        mGetImageTask = new GetImageTask();
        mGetImageTask.execute("https://ukonect-dev.s3.amazonaws.com/blueprints/43284381");//https://ukonect-dev.s3.amazonaws.com/blueprints/2059237562");
        if(mGetImageTask != null){
            mGetImageTask.link(this);
        }

        mThread = new DrawingThread();
    }

    @Override
    public void draw(Canvas canvas) {
        // Dessinez ici !
        canvas.drawColor(Color.YELLOW);
        canvas.drawPath(mPath, mPaint);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

        //canvas.drawBitmap(bmp, 10, 10, null);
    }

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }
    private void touch_up() {
        mPath.lineTo(mX, mY);
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        // kill this so we don't double draw
        mPath.reset();
    }

    @Override
    public boolean onTouch(View v,MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Que faire quand le surface change ? (L'utilisateur tourne son téléphone par exemple)
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mThread.keepDrawing = true;
        mThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mThread.keepDrawing = false;

        boolean joined = false;
        while (!joined) {
            try {
                mThread.join();
                joined = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class DrawingThread extends Thread {
        // Utilisé pour arrêter le dessin quand il le faut
        boolean keepDrawing = true;

        @Override
        public void run() {

            while (keepDrawing) {
                Canvas canvas = null;

                try {
                    // On récupère le canvas pour dessiner dessus
                    canvas = mSurfaceHolder.lockCanvas();
                    // On s'assure qu'aucun autre thread n'accède au holder
                    synchronized (mSurfaceHolder) {
                        // Et on dessine
                        draw(canvas);
                    }
                } finally {
                    // Notre dessin fini, on relâche le Canvas pour que le dessin s'affiche
                    if (canvas != null)
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                }

                // Pour dessiner à 50 fps
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void imageCallback(Bitmap image){
        if(image != null && mCanvas != null) {
            mCanvas.drawBitmap(image, 0, 0, null);
        }
        //mCanvas = new Canvas(mBitmap);
    }

    static class GetImageTask extends AsyncTask<String, Void, Bitmap> {
        private WeakReference<MapView> mActivity = null;

        public void link(MapView activity){
            mActivity = new WeakReference<MapView>(activity);
        }


        @Override
        protected Bitmap doInBackground(String... voids) {

            Bitmap bitmap=null;
            try {
                URL url = new URL(voids[0]);
                HttpURLConnection connection= (HttpURLConnection) url.openConnection();
                InputStream inputStream= connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mActivity.get().imageCallback(bitmap);
        }
    }
}

