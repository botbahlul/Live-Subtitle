package com.app.livesubtitle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
//import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

public class GlobalOverlay {
    //private static final String LOGCAT_TAG = "GlobalOverlay";
    /*private static void log(String message) {
        Log.d(LOGCAT_TAG, message);
    }*/

    private final Context mContext;
    private final WindowManager mWindowManager;
    private final View mRemoveView;
    private View mOverlayView;

    private View.OnClickListener mOnClickListener;
    private OnRemoveOverlayListener mOnRemoveOverlayListener;
    private WindowManager.LayoutParams mOverlayLayoutParams;
    public static int DisplayWidth;
    public static int DisplayHeight;
    //public static int X;
    //public static int Y;

    private final DisplayMetrics display = new DisplayMetrics();

    public GlobalOverlay(Context context) {
        this(context, newRemoveView(context));
    }

    public GlobalOverlay(Context context, View RemoveView) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mContext = context;
        mRemoveView = RemoveView;
        setupRemoveView(mRemoveView);
    }

    /** Return the view to use for the "remove view" that appears at the bottom of the screen.
     * Override this to change the image for the remove view. Returning null will throw a
     * NullPointerException in a subsequent method.*/
    @SuppressLint("InflateParams")
    private static View newRemoveView(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.overlay_remove_view, null);
    }

    /** Sets this view to the bottom of the screen and only visible when user is dragging an
     * overlay view. This modifies the instance passed in. */
    private void setupRemoveView(View RemoveView) {
        RemoveView.setVisibility(View.GONE);
        mWindowManager.addView(RemoveView, newWindowManagerLayoutParamsForRemoveView());
    }

    /** Add a global floating view.
     *
     * @param view the view to overlay across all apps and activities
     * @param onClickListener get notified of a click, set null to ignore
     */
    public final void addOverlayView(View view, int Widht, int Height, int X, int Y,
                                     View.OnClickListener onClickListener) {
        addOverlayView(view, X, Y, Widht, Height, onClickListener, null, null);
    }

    /** Add a global floating view.
     *
     * @param view the view to overlay across all apps and activities
     * @param onClickListener get notified of a click, set null to ignore
     * @param onLongClickListener not implemented yet, just set as null
     * @param onRemoveOverlayListener get notified when overlay is removed (not from a destroyed service though)
     */
    public final void addOverlayView(View view, int Widht, int Height, int X, int Y,
                                     View.OnClickListener onClickListener,
                                     View.OnLongClickListener onLongClickListener,
                                     OnRemoveOverlayListener onRemoveOverlayListener) {

        DisplayWidth = display.widthPixels;
        DisplayHeight = display.heightPixels;
        //float d = display.density;

        mOverlayView = view;
        mOnClickListener = onClickListener;
        mOnRemoveOverlayListener = onRemoveOverlayListener;
        mOverlayLayoutParams = newWindowManagerLayoutParams();
        mOverlayLayoutParams.width = Widht;
        mOverlayLayoutParams.height = Height;
        mOverlayLayoutParams.x = X;
        mOverlayLayoutParams.y = Y;
        View.OnTouchListener mOnTouchListener = newSimpleOnTouchListener();
        mWindowManager.addView(mOverlayView, mOverlayLayoutParams);
        mOverlayView.setOnTouchListener(mOnTouchListener);
        // DEBUG
        /*MainActivity.textview_debug.setText(
                "DisplayWidth="+
                DisplayWidth+
                System.getProperty("line.separator")+
                "DisplayHeight"+
                DisplayHeight+
                System.getProperty("line.separator")+
                "mOverlayLayoutParams.width="+
                mOverlayLayoutParams.width+
                System.getProperty("line.separator")+
                "mOverlayLayoutParams.height="+
                mOverlayLayoutParams.height+
                System.getProperty("line.separator")+
                "mOverlayLayoutParams.x="+
                mOverlayLayoutParams.x+
                System.getProperty("line.separator")+
                "mOverlayLayoutParams.y="+
                mOverlayLayoutParams.y);*/

    }

    /** Manually remove an overlay without destroying the service. */
    public final void removeOverlayView(View view) {
        removeOverlayView(view, false);
    }

    /** Remove a overlay without destroying the service. */
    public final void removeOverlayView(View view, boolean isRemovedByUser) {
        if (view != null) {
            if (mOnRemoveOverlayListener != null) {
                mOnRemoveOverlayListener.onRemoveOverlay(view, isRemovedByUser);
            }
            mWindowManager.removeView(view);
        }
    }

//    /** Remove all views. This instance becomes unusable after calling this. */
//    public void destroy() {
//      TODO: Remove all views, when it's possible to have multiple overlays.
//    }

    /** Provides the drag ability for the overlay view. This touch listener
     * allows user to drag the view anywhere on screen. */
    private View.OnTouchListener newSimpleOnTouchListener() {
        return new View.OnTouchListener() {
            //            private long timeStart; // Maybe use in the future, with ViewConfiguration's getLongClickTime or whatever it is called.
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private final int[] overlayViewLocation = {0,0};

            private boolean isOverRemoveView;
            private final int[] removeViewLocation = {0,0};

            private final int touchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //timeStart = System.currentTimeMillis();
                        initialX = mOverlayLayoutParams.x;
                        initialY = mOverlayLayoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        mRemoveView.setVisibility(View.VISIBLE);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        mOverlayLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        mOverlayLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mOverlayView, mOverlayLayoutParams);

                        mOverlayView.getLocationOnScreen(overlayViewLocation);
                        mRemoveView.getLocationOnScreen(removeViewLocation);
                        isOverRemoveView = isPointInArea(overlayViewLocation[0], overlayViewLocation[1],
                                removeViewLocation[0], removeViewLocation[1], mRemoveView.getWidth());
                        IS_OVER_REMOVEVIEW.IS_OVER = isOverRemoveView;
                        //if (isOverRemoveView) {
                            // TODO: Maybe, make it look like the overlay view is perfectly on the remove view.
                        //}

                        return true;
                    case MotionEvent.ACTION_UP:
                        if (isOverRemoveView) {
                            removeOverlayView(v, true);
                            // Not sure if setting to null is the best way to handle this. Though,
                            // currently it's needed to prevent a `IllegalArgumentException ... not attached to window manager`
//                            v = null;
//                            destroy();
                        } else {
                            if (mOnClickListener != null && Math.abs(initialTouchY - event.getRawY()) <= touchSlop) {
                                mOnClickListener.onClick(v);
                            }
                        }

                        mRemoveView.setVisibility(View.GONE);
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        mRemoveView.setVisibility(View.GONE);
                        return true;
                }
                return false;
            }
        };
    }

    /** Return true if point (x1,y1) is in the square defined by (x2,y2) with radius, otherwise false.  */
    private boolean isPointInArea(int x1, int y1, int x2, int y2, int radius) {
//        log("isPointInArea(). x1=" + x1 + ",y1=" + y1);
//        log("isPointInArea(). x2=" + x2 + ",y2=" + y2 + ",radius=" + radius);
        return x1 >= x2 - radius && x1 <= x2 + radius && y1 >= y2 - radius && y1 <=  y2 + radius;
    }

    /** Returns the default layout params for the overlay views. */
    private static WindowManager.LayoutParams newWindowManagerLayoutParams() {
        WindowManager.LayoutParams params=null;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.START;
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.START;
            params.setColorMode(ActivityInfo.COLOR_MODE_HDR);
        }
        //MainActivity.textview_debug.setText(String.valueOf(DISPLAY_METRIC.WIDTH));
        //MainActivity.textview_debug.setText(String.valueOf(DISPLAY_METRIC.DISPLAY_WIDTH)+","+String.valueOf(DISPLAY_METRIC.DISPLAY_HEIGHT)+','+String.valueOf(DISPLAY_METRIC.DISPLAY_DENSITY));
        return params;
    }


    private static WindowManager.LayoutParams newWindowManagerLayoutParamsForRemoveView() {
        WindowManager.LayoutParams params=null;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            params.y = 56;
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            params.y = 56;
        }
        return params;
    }

    /** Interface definition for when an overlay view has been removed. */
    public interface OnRemoveOverlayListener {
        /** This overlay has been removed.
         * @param v the removed view
         * @param isRemovedByUser true if user manually removed view, false if removed another way */
        void onRemoveOverlay(View v, boolean isRemovedByUser);
    }

}
