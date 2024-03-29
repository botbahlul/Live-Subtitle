package com.app.livesubtitle;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Objects;

public class create_overlay_translation_text extends Service {
    public create_overlay_translation_text() {
    }

    @SuppressLint("StaticFieldLeak")
    public static GlobalOverlay mGlobalOverlay_overlay_translation_text;
    @SuppressLint("StaticFieldLeak")
    public static View overlay_translation_text_container;
    @SuppressLint("StaticFieldLeak")
    public static EditText overlay_translation_text;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void onCreate() {
        super.onCreate();
        MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, MainActivity.mStreamVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        create_translation_text_window();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlay_translation_text != null) {
            mGlobalOverlay_overlay_translation_text.removeOverlayView(overlay_translation_text);
        }
    }

    @SuppressLint("InflateParams")
    private void create_translation_text_window() {
        mGlobalOverlay_overlay_translation_text = new GlobalOverlay(this);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        overlay_translation_text_container = layoutInflater.inflate(R.layout.overlay_translation_text_container, null);
        overlay_translation_text_container.setMinimumWidth((int) (0.8* DISPLAY_METRIC.DISPLAY_WIDTH));
        overlay_translation_text_container.setBackgroundColor(Color.TRANSPARENT);
        overlay_translation_text_container.setVisibility(View.INVISIBLE);
        overlay_translation_text = overlay_translation_text_container.findViewById(R.id.overlay_translation_text);
        overlay_translation_text.setWidth(overlay_translation_text_container.getWidth());
        overlay_translation_text.setBackgroundColor(Color.TRANSPARENT);
        overlay_translation_text.setTextColor(Color.YELLOW);
        overlay_translation_text.setVisibility(View.INVISIBLE);
        if (RECOGNIZING_STATUS.IS_RECOGNIZING) {
            MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            if (TRANSLATION_TEXT.STRING.length() == 0) {
                overlay_translation_text.setVisibility(View.INVISIBLE);
                overlay_translation_text_container.setVisibility(View.INVISIBLE);
            } else {
                overlay_translation_text.setVisibility(View.VISIBLE);
                overlay_translation_text_container.setVisibility(View.VISIBLE);
            }
        } else {
            overlay_translation_text.setVisibility(View.INVISIBLE);
            overlay_translation_text_container.setVisibility(View.INVISIBLE);
        }
        int h;
        if (Objects.equals(LANGUAGE.DST, "ja") || Objects.equals(LANGUAGE.DST, "zh")) {
            h = 75;
        }
        else {
            h = 62;
        }
        mGlobalOverlay_overlay_translation_text.addOverlayView(overlay_translation_text_container,
                (int) (0.85* DISPLAY_METRIC.DISPLAY_WIDTH),
                (int) (h * getResources().getDisplayMetrics().density),
                0,
                (int) (0.3* DISPLAY_METRIC.DISPLAY_HEIGHT),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //toast("onClick");
                    }
                },
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        //toast("onLongClick not implemented yet");
                        return false;
                    }
                },
                new GlobalOverlay.OnRemoveOverlayListener() {
                    @Override
                    public void onRemoveOverlay(View view, boolean isRemovedByUser) {
                        //toast("onRemoveOverlay");
                        stopSelf();
                    }
                });
    }

    /*private void toast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }*/

}