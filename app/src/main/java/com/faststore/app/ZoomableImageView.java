package com.faststore.app;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * A simple pinch-to-zoom + pan + double-tap-to-zoom ImageView.
 * Used for the full-screen product image viewer.
 */
public class ZoomableImageView extends AppCompatImageView {

    private final Matrix matrix = new Matrix();
    private float scale = 1f;
    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 5f;

    private float lastTouchX, lastTouchY;
    private int activePointerId = -1;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float newScale = scale * detector.getScaleFactor();
            newScale = Math.max(MIN_SCALE, Math.min(newScale, MAX_SCALE));
            float factor = newScale / scale;
            scale = newScale;
            matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            setImageMatrix(matrix);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnDoubleTapListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (scale > MIN_SCALE) {
                scale = MIN_SCALE;
                matrix.reset();
            } else {
                scale = 2.5f;
                matrix.postScale(scale, scale, e.getX(), e.getY());
            }
            setImageMatrix(matrix);
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                activePointerId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                if (scale > MIN_SCALE) {
                    int pointerIndex = event.findPointerIndex(activePointerId);
                    if (pointerIndex != -1) {
                        float x = event.getX(pointerIndex);
                        float y = event.getY(pointerIndex);
                        if (!scaleDetector.isInProgress()) {
                            float dx = x - lastTouchX;
                            float dy = y - lastTouchY;
                            matrix.postTranslate(dx, dy);
                            setImageMatrix(matrix);
                        }
                        lastTouchX = x;
                        lastTouchY = y;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointerId = -1;
                break;
        }
        return true;
    }

    public void resetZoom() {
        scale = 1f;
        matrix.reset();
        setImageMatrix(matrix);
    }
}
