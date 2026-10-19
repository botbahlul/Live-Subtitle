package com.app.livesubtitle;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import java.util.Objects;

public class create_overlay_translation_text extends Service {

    public create_overlay_translation_text() {}

    private String TAG = "create_overlay_translation_text";
    @SuppressLint("StaticFieldLeak")
    public static GlobalOverlay mGlobalOverlay_overlay_translation_text;
    @SuppressLint("StaticFieldLeak")
    public static View overlay_translation_text_container;
    @SuppressLint("StaticFieldLeak")
    public static EditText overlay_translation_text;
    @SuppressLint("StaticFieldLeak")
    public static View overlay_full_translation_text_container;
    @SuppressLint("StaticFieldLeak")
    public static EditText overlay_full_translation_text;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void onCreate() {
        super.onCreate();
        Log.d("create_overlay_translation_text", "Starting create_overlay_translation_text service");
        create_translation_text_window();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlay_translation_text != null) {
            mGlobalOverlay_overlay_translation_text.removeOverlayView(overlay_translation_text);
        }
        if (overlay_translation_text_container != null) {
            mGlobalOverlay_overlay_translation_text.removeOverlayView(overlay_translation_text_container);
        }
        if (overlay_full_translation_text != null) {
            mGlobalOverlay_overlay_translation_text.removeOverlayView(overlay_full_translation_text);
        }
        if (overlay_full_translation_text_container != null) {
            mGlobalOverlay_overlay_translation_text.removeOverlayView(overlay_full_translation_text_container);
        }
    }

    @SuppressLint("InflateParams")
    private void create_translation_text_window() {
        mGlobalOverlay_overlay_translation_text = new GlobalOverlay(this);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //android.graphics.drawable.GradientDrawable subtitleBackground = new android.graphics.drawable.GradientDrawable();
        //subtitleBackground.setColor(Color.parseColor("#80000000"));
        //overlay_translation_text.setBackground(subtitleBackground);
        //overlay_full_translation_text.setBackground(subtitleBackground.getConstantState().newDrawable().mutate());

        overlay_translation_text_container = layoutInflater.inflate(R.layout.overlay_translation_text_container, null);
        overlay_translation_text_container.setMinimumWidth((int) (0.8* DISPLAY_METRIC.DISPLAY_WIDTH));
        overlay_translation_text_container.setBackgroundColor(Color.parseColor("#00000000"));
        overlay_translation_text_container.setVisibility(View.INVISIBLE);
        overlay_translation_text = overlay_translation_text_container.findViewById(R.id.overlay_translation_text);
        overlay_translation_text.setBackgroundColor(Color.parseColor("#80000000"));
        overlay_translation_text.setTextColor(Color.YELLOW);
        overlay_translation_text.setVisibility(View.INVISIBLE);

        overlay_full_translation_text_container = layoutInflater.inflate(R.layout.overlay_full_translation_text_container, null);
        overlay_full_translation_text_container.setMinimumWidth((int) (0.8* DISPLAY_METRIC.DISPLAY_WIDTH));
        overlay_full_translation_text_container.setBackgroundColor(Color.parseColor("#00000000"));
        overlay_full_translation_text_container.setVisibility(View.INVISIBLE);
        overlay_full_translation_text = overlay_full_translation_text_container.findViewById(R.id.overlay_full_translation_text);
        overlay_full_translation_text.setBackgroundColor(Color.parseColor("#80000000"));
        overlay_full_translation_text.setTextColor(Color.CYAN);
        overlay_full_translation_text.setVisibility(View.INVISIBLE);

        if (RECOGNIZING_STATUS.IS_RECOGNIZING) {
            MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            if (TRANSLATION_TEXT.STRING.length() == 0) {
                overlay_translation_text.setVisibility(View.INVISIBLE);
                overlay_translation_text_container.setVisibility(View.INVISIBLE);
                overlay_full_translation_text.setVisibility(View.INVISIBLE);
                overlay_full_translation_text_container.setVisibility(View.INVISIBLE);
            } else {
                overlay_translation_text.setVisibility(View.VISIBLE);
                overlay_translation_text_container.setVisibility(View.VISIBLE);
                overlay_full_translation_text.setVisibility(View.VISIBLE);
                overlay_full_translation_text_container.setVisibility(View.VISIBLE);
            }
        } else {
            MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, MainActivity.mStreamVolume, 0);
            //MainActivity.audio.setStreamVolume(AudioManager.STREAM_MUSIC, MainActivity.mMusicVolume, 0);
            Log.d(TAG, "create_translation_text_window: mStreamVolume = " + MainActivity.mStreamVolume);
            //Log.d(TAG, "create_translation_text_window: mMusicVolume = " + MainActivity.mMusicVolume);
            if (overlay_translation_text_container != null) {
                overlay_translation_text.setVisibility(View.INVISIBLE);
                overlay_translation_text_container.setVisibility(View.INVISIBLE);
            }
            if (overlay_full_translation_text_container != null) {
                overlay_full_translation_text.setVisibility(View.INVISIBLE);
                overlay_full_translation_text_container.setVisibility(View.INVISIBLE);
            }
        }

        int h;
        if (Objects.equals(LANGUAGE.DST, "ja") || Objects.equals(LANGUAGE.DST, "zh-Hans") || Objects.equals(LANGUAGE.DST, "zh-Hant")) {
            //h = 80;
            //h = 124;
            h = 75;
        } else {
            //h = 64;
            //h = 107;
            h = 62;
        }

        int overlayWidth = (int) (0.85 * DISPLAY_METRIC.DISPLAY_WIDTH);
        int overlayHeight = (int) (2 * h * getResources().getDisplayMetrics().density);
        int translationY = (int) (0.3 * DISPLAY_METRIC.DISPLAY_HEIGHT);
        int fullTranslationY = translationY - overlayHeight;

        mGlobalOverlay_overlay_translation_text.addOverlayView(
                overlay_full_translation_text_container,
                overlayWidth,
                overlayHeight,
                0,
                fullTranslationY - 8,
                null,
                null,
                new GlobalOverlay.OnRemoveOverlayListener() {
                    @Override public void onRemoveOverlay(View v, boolean isRemovedByUser) {
                        //stopSelf();
                    }
                }
        );
        mGlobalOverlay_overlay_translation_text.addOverlayView(
                overlay_translation_text_container,
                overlayWidth,
                overlayHeight,
                0,
                translationY,
                null,
                null,
                new GlobalOverlay.OnRemoveOverlayListener() {
                    @Override public void onRemoveOverlay(View v, boolean isRemovedByUser) {
                        //stopSelf();
                    }
                }
        );
    }

}
