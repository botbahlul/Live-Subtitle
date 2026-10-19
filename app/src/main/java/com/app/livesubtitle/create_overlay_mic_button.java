package com.app.livesubtitle;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


public class create_overlay_mic_button extends Service {

    public create_overlay_mic_button() {}

    private String TAG = "create_overlay_mic_button";
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

        if (!MIC_BUTTON_STATUS.IS_OVERLAYING) {
            stop_voice_recognizer();
            stop_create_overlay_translation_text();
            RECOGNIZING_STATUS.IS_RECOGNIZING = false;
            RECOGNIZING_STATUS.STRING = "RECOGNIZING_STATUS.IS_RECOGNIZING = " + RECOGNIZING_STATUS.IS_RECOGNIZING;
            MainActivity.textview_recognizing.setText(RECOGNIZING_STATUS.STRING);
            MIC_BUTTON_STATUS.IS_OVERLAYING = false;
            MIC_BUTTON_STATUS.STRING = "MIC_BUTTON_STATUS.IS_OVERLAYING = " + MIC_BUTTON_STATUS.IS_OVERLAYING;
            MainActivity.textview_overlaying.setText(MIC_BUTTON_STATUS.STRING);
            MainActivity.textview_debug.setText("");
            VOICE_TEXT.STRING = "";
            TRANSLATION_TEXT.STRING = "";
            MainActivity.voice_text.setText("");
            MainActivity.voice_text.setHint("Recognized words");
            MainActivity.full_voice_text.setText("");
            MainActivity.full_voice_text.setHint("Recognized sentences");
            //MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (int)Double.parseDouble(String.valueOf((long)(MainActivity.audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) / 2))), 0);
            //MainActivity.audio.setStreamVolume(AudioManager.STREAM_MUSIC, (int)Double.parseDouble(String.valueOf((long)(MainActivity.audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2))), 0);
            MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, MainActivity.mStreamVolume, 0);
            Log.d(TAG, "onDestroy: mStreamVolume = " + MainActivity.mStreamVolume);
            //Log.d(TAG, "onDestroy: mMusicVolume = " + MainActivity.mMusicVolume);
            if (create_overlay_translation_text.overlay_translation_text != null) {
                create_overlay_translation_text.overlay_translation_text.setText("");
                create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
            }
            if (create_overlay_translation_text.overlay_full_translation_text != null) {
                create_overlay_translation_text.overlay_full_translation_text.setText("");
                create_overlay_translation_text.overlay_full_translation_text.setVisibility(View.INVISIBLE);
                create_overlay_translation_text.overlay_full_translation_text_container.setVisibility(View.INVISIBLE);
            }
            if (create_overlay_mic_button.mic_button != null) {
                create_overlay_mic_button.mic_button.setVisibility(View.INVISIBLE);
            }
        }
        MainActivity.textview_debug.setText("");
        VOICE_TEXT.STRING = "";
        TRANSLATION_TEXT.STRING = "";
        RECOGNIZING_STATUS.STRING = "RECOGNIZING_STATUS.IS_RECOGNIZING = " + RECOGNIZING_STATUS.IS_RECOGNIZING;
        MainActivity.textview_recognizing.setText(RECOGNIZING_STATUS.STRING);
        MIC_BUTTON_STATUS.STRING = "MIC_BUTTON_STATUS.IS_OVERLAYING = " + MIC_BUTTON_STATUS.IS_OVERLAYING;
        MainActivity.textview_overlaying.setText(MIC_BUTTON_STATUS.STRING);
        MainActivity.voice_text.setText("");
        MainActivity.voice_text.setHint("Recognized words");
        MainActivity.full_voice_text.setText("");
        MainActivity.full_voice_text.setHint("Recognized sentences");
        Log.d("create_overlay_mic_button", "Stopping create_overlay_mic_button service");
    }

    private void create_mic_button() {
        Log.d("create_overlay_mic_button", "Starting create_overlay_mic_button service");
        if (MainActivity.checkbox_offline_mode != null) {
            PREFER_OFFLINE_STATUS.OFFLINE = MainActivity.checkbox_offline_mode.isChecked();
        }
        mGlobalOverlay_mic_button = new GlobalOverlay(this);
        mic_button = new ImageView(this);
        MIC_BUTTON_STATUS.IS_OVERLAYING = true;
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
                        MainActivity.textview_recognizing.setText(RECOGNIZING_STATUS.STRING);
                        MIC_BUTTON_STATUS.STRING = "MIC_BUTTON_STATUS.IS_OVERLAYING = " + MIC_BUTTON_STATUS.IS_OVERLAYING;
                        MainActivity.textview_overlaying.setText(MIC_BUTTON_STATUS.STRING);
                        MainActivity.textview_debug.setText("");
                        MainActivity.textview_output_messages.setText("");
                        if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                            stop_voice_recognizer();
                            mic_button.setImageResource(R.drawable.ic_mic_black_off);
                            VOICE_TEXT.STRING = "";
                            TRANSLATION_TEXT.STRING = "";
                            if (create_overlay_translation_text.overlay_translation_text_container != null) {
                                setText(create_overlay_translation_text.overlay_translation_text, "");
                                create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                                create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                            }
                            if (create_overlay_translation_text.overlay_full_translation_text_container != null) {
                                setText(create_overlay_translation_text.overlay_full_translation_text, "");
                                create_overlay_translation_text.overlay_full_translation_text.setVisibility(View.INVISIBLE);
                                create_overlay_translation_text.overlay_full_translation_text_container.setVisibility(View.INVISIBLE);
                            }
                            MainActivity.voice_text.setText("");
                            MainActivity.voice_text.setHint("Recognized words");
                            MainActivity.full_voice_text.setText("");
                            MainActivity.full_voice_text.setHint("Recognized sentences");
                            //MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, MainActivity.mStreamVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                            MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, MainActivity.mStreamVolume, 0);
                            //MainActivity.audio.setStreamVolume(AudioManager.STREAM_MUSIC, MainActivity.mMusicVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                            Log.d(TAG, "onClick: mStreamVolume = " + MainActivity.mStreamVolume);
                            //Log.d(TAG, "onClick: mMusicVolume = " + MainActivity.mMusicVolume);
                        } else {
                            MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                            start_voice_recognizer();
                            mic_button.setImageResource(R.drawable.ic_mic_black_on);
                            if (TRANSLATION_TEXT.STRING.length() == 0) {
                                if (create_overlay_translation_text.overlay_translation_text_container != null) {
                                    create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                                    create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                                }
                            } else {
                                if (create_overlay_translation_text.overlay_translation_text_container != null) {
                                    create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.VISIBLE);
                                    create_overlay_translation_text.overlay_translation_text.setVisibility(View.VISIBLE);
                                }
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
                    public void onRemoveOverlay(View view, boolean isRemovedByUser) {
                        if (view == mic_button && isRemovedByUser) {
                            MIC_BUTTON_STATUS.IS_OVERLAYING = false;
                            RECOGNIZING_STATUS.IS_RECOGNIZING = false;
                            //toast("onRemoveOverlay");
                            stop_create_overlay_translation_text();
                            stop_voice_recognizer();
                            stopSelf();
                        }
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
