package com.app.livesubtitle;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Akexorcist on 3/22/2017 AD.
 */

public class BaseActivity extends AppCompatActivity implements ScreenOrientationHelper.ScreenOrientationChangeListener {
    private final ScreenOrientationHelper helper = new ScreenOrientationHelper(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper.onCreate(savedInstanceState);
        helper.setScreenOrientationChangeListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        helper.onStart();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        helper.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        helper.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onScreenOrientationChanged(int orientation) {
        /*if (VoiceRecognizer.mic_button != null) {

        }*/

    }
}
