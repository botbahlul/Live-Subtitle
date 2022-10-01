package com.app.livesubtitle;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by Akexorcist on 3/22/2017 AD.
 */

public class ScreenOrientationHelper {
    public static final String KEY_LAST_ORIENTATION = "last_orientation";
    private final Activity activity;
    private ScreenOrientationChangeListener listener;
    private int lastOrientation;

    public ScreenOrientationHelper(Activity activity) {
        this.activity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            lastOrientation = activity.getResources().getConfiguration().orientation;
        }
    }

    public void onStart() {
        checkOrientationChanged();
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        restoreLastOrientationState(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
        saveLastOrientationState(outState);
    }

    private void restoreLastOrientationState(Bundle savedInstanceState) {
        lastOrientation = savedInstanceState != null ?
                savedInstanceState.getInt(KEY_LAST_ORIENTATION) :
                activity.getResources().getConfiguration().orientation;
    }

    private void saveLastOrientationState(Bundle outState) {
        if (outState != null) {
            outState.putInt(KEY_LAST_ORIENTATION, lastOrientation);
        }
    }

    private void checkOrientationChanged() {
        int currentOrientation = activity.getResources().getConfiguration().orientation;
        if (currentOrientation != lastOrientation) {
            if (listener != null) {
                listener.onScreenOrientationChanged(currentOrientation);
            }
            lastOrientation = currentOrientation;
        }
    }

    public void setScreenOrientationChangeListener(ScreenOrientationChangeListener listener) {
        this.listener = listener;
    }

    public interface ScreenOrientationChangeListener {
        void onScreenOrientationChanged(int orientation);
    }
}
