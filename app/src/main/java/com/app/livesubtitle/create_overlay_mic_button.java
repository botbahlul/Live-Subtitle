package com.app.livesubtitle;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;

public class create_overlay_mic_button extends Service {
    public create_overlay_mic_button() {}

    private GlobalOverlay mGlobalOverlay_mic_button;
    @SuppressLint("StaticFieldLeak")
    public static ImageView mic_button;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        create_mic_button();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (RECOGNIZING_STATUS.RECOGNIZING) {
                mic_button.setImageResource(R.drawable.ic_mic_black_on);
            }
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            mic_button.setForegroundGravity(Gravity.CENTER_HORIZONTAL);
            if (RECOGNIZING_STATUS.RECOGNIZING) {
                mic_button.setImageResource(R.drawable.ic_mic_black_on);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGlobalOverlay_mic_button != null) {
            mGlobalOverlay_mic_button.removeOverlayView(mic_button);
        }
        if (IS_OVER_REMOVEVIEW.IS_OVER) {
            RECOGNIZING_STATUS.RECOGNIZING = false;
            OVERLAYING_STATUS.OVERLAYING = false;
            VOICE_TEXT.STRING = "";
            TRANSLATION_TEXT.STRING = "";
            MainActivity.voice_text.setText("");
            if (create_overlay_translation_text.overlay_translation_text != null) {
                create_overlay_translation_text.overlay_translation_text.setText("");
                create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
            }
            stop_voice_recognizer();
        }
    }

    private void create_mic_button() {
        if (MainActivity.checkbox_offline_mode != null) PREFER_OFFLINE_STATUS.OFFLINE = MainActivity.checkbox_offline_mode.isChecked();
        mGlobalOverlay_mic_button = new GlobalOverlay(this);
        mic_button = new ImageView(this);
        if (!RECOGNIZING_STATUS.RECOGNIZING) {
            mic_button.setImageResource(R.drawable.ic_mic_black_off);
        } else {
            mic_button.setImageResource(R.drawable.ic_mic_black_on);
        }
        mic_button.setBackgroundColor(Color.parseColor("#80000000"));
        mGlobalOverlay_mic_button.addOverlayView(mic_button,
                96,
                96,
                (int) ((0.5 * DISPLAY_METRIC.DISPLAY_WIDTH) - (0.5 * 96)),
                0,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RECOGNIZING_STATUS.RECOGNIZING = !RECOGNIZING_STATUS.RECOGNIZING;
                        if (!RECOGNIZING_STATUS.RECOGNIZING) {
                            stop_voice_recognizer();
                            mic_button.setImageResource(R.drawable.ic_mic_black_off);
                            VOICE_TEXT.STRING = "";
                            TRANSLATION_TEXT.STRING = "";
                            create_overlay_translation_text.overlay_translation_text.setText("");
                            create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                            create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                            MainActivity.voice_text.setText("");
                            MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, MainActivity.mStreamVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        } else {
                            MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                            start_voice_recognizer();
                            mic_button.setImageResource(R.drawable.ic_mic_black_on);
                            if (TRANSLATION_TEXT.STRING.length() == 0) {
                                create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                                create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                            } else {
                                create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.VISIBLE);
                                create_overlay_translation_text.overlay_translation_text.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                },
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return false;
                    }
                },
                new GlobalOverlay.OnRemoveOverlayListener() {
                    @Override
                    public void onRemoveOverlay(View mic_button, boolean isRemovedByUser) {
                        //toast("onRemoveOverlay");
                        stopSelf();
                    }
                });

    }

    private void start_voice_recognizer() {
        Intent i = new Intent(this, VoiceRecognizer.class);
        startService(i);
    }

    private void stop_voice_recognizer() {
        stopService(new Intent(this, VoiceRecognizer.class));
    }

    /*private void start_create_overlay_translation_text() {
        Intent i = new Intent(this, create_overlay_translation_text.class);
        startService(i);
    }

    private void stop_create_overlay_translation_text() {
        stopService(new Intent(this, create_overlay_translation_text.class));
    }*/

}
