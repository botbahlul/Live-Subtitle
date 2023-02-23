package com.app.livesubtitle;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    public void onDestroy() {
        super.onDestroy();
        if (mGlobalOverlay_mic_button != null) {
            mGlobalOverlay_mic_button.removeOverlayView(mic_button);
        }
        if (IS_OVER_REMOVEVIEW.IS_OVER) {
            RECOGNIZING_STATUS.IS_RECOGNIZING = false;
            RECOGNIZING_STATUS.STRING = "RECOGNIZING_STATUS.IS_RECOGNIZING = " + RECOGNIZING_STATUS.IS_RECOGNIZING;
            setText(MainActivity.textview_recognizing, RECOGNIZING_STATUS.STRING);
            OVERLAYING_STATUS.IS_OVERLAYING = false;
            OVERLAYING_STATUS.STRING = "OVERLAYING_STATUS.IS_OVERLAYING = " + OVERLAYING_STATUS.IS_OVERLAYING;
            setText(MainActivity.textview_overlaying, OVERLAYING_STATUS.STRING);
            setText(MainActivity.textview_output_messages, "");
            VOICE_TEXT.STRING = "";
            TRANSLATION_TEXT.STRING = "";
            setText(MainActivity.voice_text, "");
            String hints = "Recognized words";
            MainActivity.voice_text.setHint(hints);
            MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (int)Double.parseDouble(String.valueOf((long)(MainActivity.audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) / 2))), 0);
            if (create_overlay_translation_text.overlay_translation_text != null) {
                create_overlay_translation_text.overlay_translation_text.setText("");
                create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
            }
            if (create_overlay_mic_button.mic_button != null) {
                create_overlay_mic_button.mic_button.setVisibility(View.INVISIBLE);
            }
            stop_voice_recognizer();
            stop_create_overlay_translation_text();
        }
        setText(MainActivity.textview_output_messages, "");
        VOICE_TEXT.STRING = "";
        TRANSLATION_TEXT.STRING = "";
        setText(MainActivity.voice_text, "");
        RECOGNIZING_STATUS.STRING = "RECOGNIZING_STATUS.IS_RECOGNIZING = " + RECOGNIZING_STATUS.IS_RECOGNIZING;
        setText(MainActivity.textview_recognizing, RECOGNIZING_STATUS.STRING);
        OVERLAYING_STATUS.STRING = "OVERLAYING_STATUS.IS_OVERLAYING = " + OVERLAYING_STATUS.IS_OVERLAYING;
        setText(MainActivity.textview_overlaying, OVERLAYING_STATUS.STRING);
        String hints = "Recognized words";
        MainActivity.voice_text.setHint(hints);
    }

    private void create_mic_button() {
        if (MainActivity.checkbox_offline_mode != null) PREFER_OFFLINE_STATUS.OFFLINE = MainActivity.checkbox_offline_mode.isChecked();
        mGlobalOverlay_mic_button = new GlobalOverlay(this);
        mic_button = new ImageView(this);
        if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
            mic_button.setImageResource(R.drawable.ic_mic_black_off);
        } else {
            mic_button.setImageResource(R.drawable.ic_mic_black_on);
        }
        mic_button.setBackgroundColor(Color.parseColor("#80000000"));
        mGlobalOverlay_mic_button.addOverlayView(mic_button,
                96,
                96,
                0,
                0,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RECOGNIZING_STATUS.IS_RECOGNIZING = !RECOGNIZING_STATUS.IS_RECOGNIZING;
                        RECOGNIZING_STATUS.STRING = "RECOGNIZING_STATUS.IS_RECOGNIZING = " + RECOGNIZING_STATUS.IS_RECOGNIZING;
                        setText(MainActivity.textview_recognizing, RECOGNIZING_STATUS.STRING);
                        OVERLAYING_STATUS.STRING = "OVERLAYING_STATUS.IS_OVERLAYING = " + OVERLAYING_STATUS.IS_OVERLAYING;
                        setText(MainActivity.textview_overlaying, OVERLAYING_STATUS.STRING);
                        if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                            stop_voice_recognizer();
                            mic_button.setImageResource(R.drawable.ic_mic_black_off);
                            VOICE_TEXT.STRING = "";
                            TRANSLATION_TEXT.STRING = "";
                            create_overlay_translation_text.overlay_translation_text.setText("");
                            create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                            create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                            setText(MainActivity.voice_text, "");
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

    private void stop_create_overlay_translation_text() {
        stopService(new Intent(this, create_overlay_translation_text.class));
    }

    public void setText(final TextView tv, final String text){
        new Handler(Looper.getMainLooper()).post(() -> tv.setText(text));
    }

    /*private void toast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }*/

}
