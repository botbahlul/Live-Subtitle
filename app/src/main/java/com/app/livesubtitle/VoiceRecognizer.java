package com.app.livesubtitle;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// HOW TO USE MLKIT TRANSLATE
// ==========================
//options = new TranslatorOptions.Builder()
//      .setSourceLanguage(LANGUAGE.SRC)
//      .setTargetLanguage(LANGUAGE.DST)
//      .build();
//translator = Translation.getClient(options);
//translator.translate(lastVoiceString)
//        .addOnSuccessListener(s -> {
//        translatedLastVoiceString = s.toLowerCase(Locale.forLanguageTag(LANGUAGE.DST));
//          // DO UI STUFFS
//        })
//        .addOnFailureListener(e -> {
//          Log.e(TAG, "Translation failed", e);
//        });


public class VoiceRecognizer extends Service {

    public VoiceRecognizer() {}

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static final String TAG = "VoiceRecognizer";
    private SpeechRecognizer speechRecognizer = null;
    public static Intent speechRecognizerIntent;
    private Translator translator;
    private TranslatorOptions options;
    private DownloadConditions conditions;
    private String mlkit_status_message = "";
    private Timer timer;
    private TimerTask timerTask;
    public String lastVoiceString = "";
    public String translatedLastVoiceString = "";

    @Override
    public void onCreate() {
        super.onCreate();
        int h;
        if (Objects.equals(LANGUAGE.SRC, "ja") || Objects.equals(LANGUAGE.SRC, "zh")) {
            h = 122;
        }
        else {
            h = 109;
        }

        MainActivity.voice_text.setHeight((int) (h * getResources().getDisplayMetrics().density));
        MainActivity.full_voice_text.setHeight((int) (h * getResources().getDisplayMetrics().density));
        MainActivity.full_voice_text.setGravity(Gravity.BOTTOM);

        if(speechRecognizer != null) speechRecognizer.destroy();
        //setText(MainActivity.textview_debug, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this));

        RECOGNIZING_STATUS.STRING = "RECOGNIZING_STATUS.IS_RECOGNIZING = " + RECOGNIZING_STATUS.IS_RECOGNIZING;
        setText(MainActivity.textview_recognizing, RECOGNIZING_STATUS.STRING);
        MIC_BUTTON_STATUS.STRING = "MIC_BUTTON_STATUS.IS_OVERLAYING = " + MIC_BUTTON_STATUS.IS_OVERLAYING;
        setText(MainActivity.textview_overlaying, MIC_BUTTON_STATUS.STRING);

        options = new TranslatorOptions.Builder()
                .setSourceLanguage(LANGUAGE.SRC)
                .setTargetLanguage(LANGUAGE.DST)
                .build();
        translator = Translation.getClient(options);
        conditions = new DownloadConditions.Builder().build();

        if (!MLKIT_DICTIONARY.READY) {
            mlkit_status_message = "Downloading MLKIT dictionary, please be patient";
            setText(MainActivity.textview_mlkit_status, mlkit_status_message);

            translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener(unused -> {
                        MLKIT_DICTIONARY.READY = true;
                        mlkit_status_message = "MLKIT dictionary is ready";
                        setText(MainActivity.textview_mlkit_status, mlkit_status_message);
                        setText(MainActivity.textview_output_messages, "");
                    })
                    .addOnFailureListener(e -> {});
        }
        else {
            new Handler(Looper.getMainLooper()).post(() -> {
                mlkit_status_message = "MLKIT dictionary is ready";
                setText(MainActivity.textview_mlkit_status, mlkit_status_message);
                setText(MainActivity.textview_output_messages, "");
            });
        }

        if(SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, Objects.requireNonNull(getClass().getPackage()).getName());
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            //speechRecognizerIntent.putExtra("android.speech.extra.HIDE_PARTIAL_TRAILING_PUNCTUATION", true);
            speechRecognizerIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
            //speechRecognizerIntent.putExtra("android.speech.extra.AUDIO_SOURCE",true);
            speechRecognizerIntent.putExtra("android.speech.extra.AUDIO_SOURCE", 1); // 1 = Media/Default Source
            speechRecognizerIntent.putExtra("android.speech.extra.RECEIVE_AUDIO_RECORD_NOTIFICATION", false);
            speechRecognizerIntent.putExtra("android.speech.extra.REQUEST_AUDIO_FOCUS", false); // Mencegah Google meminta Audio Focus
            //speechRecognizerIntent.putExtra("android.speech.extra.GET_AUDIO",true);
            //speechRecognizerIntent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", AudioFormat.ENCODING_PCM_8BIT);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE.SRC_DIALECT);
            //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,3600000);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
            if (PREFER_OFFLINE_STATUS.OFFLINE) {
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            }
            //speechRecognizerIntent.putExtra("android.speech.extra.SEGMENTED_SESSION", true);
            //speechRecognizerIntent.putExtra("android.speech.extra.SEGMENTED_SESSION", RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS);
            //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.google.android.googlequicksearchbox");
            //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle arg0) {
                    setText(MainActivity.textview_debug, "onReadyForSpeech");
                    if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                        speechRecognizer.stopListening();
                    } else {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (RECOGNIZING_STATUS.IS_RECOGNIZING) {
                                //sendMediaPlay();
                                MyNotificationListenerService.ensureOperaPlaying();
                            }
                        }, 200);
                    }
                }

                @Override
                public void onBeginningOfSpeech() {
                    setText(MainActivity.textview_debug, "onBeginningOfSpeech");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    //setText(MainActivity.textview_debug, "onRmsChanged: " + rmsdB);
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    setText(MainActivity.textview_debug, "onBufferReceived: " + Arrays.toString(buffer));
                }


                @Override
                public void onEndOfSpeech() {
                    setText(MainActivity.textview_debug, "onEndOfSpeech");
                    if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                        speechRecognizer.stopListening();
                    } else {
                        Log.d(TAG, "onEndOfSpeech: Restarting speechRecognizer");
                        if (!VOICE_TEXT.STRING.isEmpty()) {
                            lastVoiceString = VOICE_TEXT.STRING + ".\n";
                            //Log.d(TAG, "onEndOfSpeech: lastVoiceString = VOICE_TEXT.STRING = " + lastVoiceString);
                        }
                        if (MainActivity.full_voice_text != null && !lastVoiceString.isEmpty()) {
                            appendText(MainActivity.full_voice_text, lastVoiceString);
                            MainActivity.full_voice_text.setSelection(MainActivity.full_voice_text.getText().length());
                            MainActivity.full_voice_text.setGravity(Gravity.BOTTOM);

                            if (lastVoiceString != null && !Objects.equals(lastVoiceString, "") && MLKIT_DICTIONARY.READY) {
                                translator.translate(lastVoiceString)
                                        .addOnSuccessListener(s -> {
                                            translatedLastVoiceString = s.toLowerCase(Locale.forLanguageTag(LANGUAGE.DST));
                                            if (RECOGNIZING_STATUS.IS_RECOGNIZING) {
                                                if (translatedLastVoiceString.length() != 0 && create_overlay_translation_text.overlay_full_translation_text_container != null) {
                                                    create_overlay_translation_text.overlay_full_translation_text_container.setVisibility(View.VISIBLE);
                                                    create_overlay_translation_text.overlay_full_translation_text_container.setBackgroundColor(Color.parseColor("#80000000"));
                                                    create_overlay_translation_text.overlay_full_translation_text.setVisibility(View.VISIBLE);
                                                    create_overlay_translation_text.overlay_full_translation_text.setBackgroundColor(Color.TRANSPARENT);
                                                    create_overlay_translation_text.overlay_full_translation_text.setTextIsSelectable(true);
                                                    create_overlay_translation_text.overlay_full_translation_text.setHighlightColor(Color.TRANSPARENT);
                                                    appendText(create_overlay_translation_text.overlay_full_translation_text, translatedLastVoiceString);
                                                    create_overlay_translation_text.overlay_full_translation_text.setSelection(create_overlay_translation_text.overlay_full_translation_text.getText().length());
                                                    create_overlay_translation_text.overlay_full_translation_text.setTextColor(Color.parseColor("#FF00FFFF"));
                                                    create_overlay_translation_text.overlay_full_translation_text.setGravity(Gravity.BOTTOM);
                                                }
                                            } else {
                                                lastVoiceString = "";
                                                translatedLastVoiceString = "";
                                                if (create_overlay_translation_text.overlay_translation_text_container != null) {
                                                    create_overlay_translation_text.overlay_full_translation_text.setVisibility(View.INVISIBLE);
                                                    create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            // Log error atau handle jika translasi gagal
                                        });
                            }
                            lastVoiceString = "";
                        }
                        speechRecognizer.startListening(speechRecognizerIntent);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (RECOGNIZING_STATUS.IS_RECOGNIZING) {
                                //sendMediaPlay();
                                MyNotificationListenerService.ensureOperaPlaying();
                            }
                        }, 200);
                    }
                }

                @Override
                public void onError(int errorCode) {
                    setText(MainActivity.textview_debug, "onError");

                    if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                        speechRecognizer.stopListening();
                    } else {
                        if (Objects.equals(getErrorText(errorCode), "Insufficient permissions")) {
                            setText(MainActivity.textview_output_messages, "Please give RECORD AUDIO PERMISSION (USE MICROPHONE PERMISSION) to GOOGLE APP");
                        } else {
                            setText(MainActivity.textview_debug, "onError : " + getErrorText(errorCode));
                        }
                        speechRecognizer.startListening(speechRecognizerIntent);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (RECOGNIZING_STATUS.IS_RECOGNIZING) {
                                //sendMediaPlay();
                                MyNotificationListenerService.ensureOperaPlaying();
                            }
                        }, 200);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    setText(MainActivity.textview_debug, "onResults");
                    if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                        //speechRecognizer.stopListening();
                        speechRecognizer.destroy();
                    } else {
                        setText(MainActivity.textview_output_messages, "onResults");
                        if (!VOICE_TEXT.STRING.isEmpty()) {
                            lastVoiceString = VOICE_TEXT.STRING + ".\n";
                            Log.d(TAG, "onResults: lastVoiceString = VOICE_TEXT.STRING = " + lastVoiceString);
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partial) {
                    setText(MainActivity.textview_debug, "onPartialResults");
                    if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                        speechRecognizer.stopListening();
                    } else {
                        ArrayList<String> data = partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (PREFER_OFFLINE_STATUS.OFFLINE) {
                            ArrayList<String> unstableData = partial.getStringArrayList("android.speech.extra.UNSTABLE_TEXT");
                            if (unstableData != null) {
                                VOICE_TEXT.STRING = data.get(0).toLowerCase(Locale.forLanguageTag(LANGUAGE.SRC)) + unstableData.get(0).toLowerCase(Locale.forLanguageTag(LANGUAGE.SRC));
                            }
                        } else {
                            StringBuilder text = new StringBuilder();
                            for (String result : data)
                                text.append(result);
                            VOICE_TEXT.STRING = text.toString().toLowerCase(Locale.forLanguageTag(LANGUAGE.SRC));
                        }
                        if (!VOICE_TEXT.STRING.isEmpty()) {
                            lastVoiceString = VOICE_TEXT.STRING.trim();
                        }
                        MainActivity.voice_text.setText(VOICE_TEXT.STRING);
                        MainActivity.voice_text.setSelection(MainActivity.voice_text.getText().length());
                    }
                }

                @Override
                public void onEvent(int arg0, Bundle arg1) {
                    setText(MainActivity.textview_output_messages, "onEvent");
                }

                public String getErrorText(int errorCode) {
                    String message;
                    switch (errorCode) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            message = "Audio recording error";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            message = "Client side error";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            message = "Insufficient permissions";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            message = "Network error";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            message = "Network timeout";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            message = "No match";
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            message = "RecognitionService busy";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                            message = "error from server";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            message = "No speechRecognizer input";
                            break;
                        default:
                            message = "Didn't understand, please try again.";
                            break;
                    }
                    return message;
                }
            });
        }

        if (speechRecognizer == null) {
            setText(MainActivity.textview_output_messages, "Speech recognition not available");
            stopSelf();
            return;
        }

        if (RECOGNIZING_STATUS.IS_RECOGNIZING) {
            if (translatedLastVoiceString.length() != 0) {
                if (create_overlay_translation_text.overlay_full_translation_text_container != null) {
                    create_overlay_translation_text.overlay_full_translation_text.setVisibility(View.VISIBLE);
                    create_overlay_translation_text.overlay_full_translation_text_container.setVisibility(View.VISIBLE);
                }
            }

            speechRecognizer.startListening(speechRecognizerIntent);

            Handler handler = new Handler(Looper.getMainLooper());
            Executor executor = Executors.newSingleThreadExecutor();
            speechRecognizer.startListening(speechRecognizerIntent);
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (VOICE_TEXT.STRING != null && MLKIT_DICTIONARY.READY) {
                        executor.execute(() -> translator.translate(VOICE_TEXT.STRING).addOnSuccessListener(s -> TRANSLATION_TEXT.STRING = s.toLowerCase(Locale.forLanguageTag(LANGUAGE.DST))).addOnFailureListener(e -> {}));
                        handler.post(() -> {
                            if (RECOGNIZING_STATUS.IS_RECOGNIZING) {
                                if (TRANSLATION_TEXT.STRING.length() != 0) {
                                    if (create_overlay_translation_text.overlay_translation_text_container != null) {
                                        create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.VISIBLE);
                                        create_overlay_translation_text.overlay_translation_text_container.setBackgroundColor(Color.parseColor("#80000000"));
                                        create_overlay_translation_text.overlay_translation_text.setVisibility(View.VISIBLE);
                                        create_overlay_translation_text.overlay_translation_text.setBackgroundColor(Color.TRANSPARENT);
                                        create_overlay_translation_text.overlay_translation_text.setTextIsSelectable(true);
                                        create_overlay_translation_text.overlay_translation_text.setHighlightColor(Color.TRANSPARENT);
                                        create_overlay_translation_text.overlay_translation_text.setText(TRANSLATION_TEXT.STRING);
                                        create_overlay_translation_text.overlay_translation_text.setSelection(create_overlay_translation_text.overlay_translation_text.getText().length());
                                        create_overlay_translation_text.overlay_translation_text.setTextColor(Color.parseColor("#FFFF00"));
                                    }
                                }
                            } else {
                                VOICE_TEXT.STRING = "";
                                TRANSLATION_TEXT.STRING = "";
                                if (create_overlay_translation_text.overlay_translation_text_container != null) {
                                    create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                                    create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    } else {
                        TRANSLATION_TEXT.STRING = "";
                        if (create_overlay_translation_text.overlay_translation_text_container != null) {
                            create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                            create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            };
            timer.schedule(timerTask,0,2000);
        } else {
            speechRecognizer.stopListening();
            if (timerTask != null) timerTask.cancel();
            if (timer != null) {
                timer.cancel();
                timer.purge();
            }
            if (translator != null) translator.close();
            stopSelf();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (timer != null) { timer.cancel(); timer.purge(); }
        if (translator != null) translator.close();
    }

    public void setText(final TextView tv, final String text){
        if (tv != null) {
            new Handler(Looper.getMainLooper()).post(() -> tv.setText(text));
        }
    }

    private void appendText(final TextView tv, final String text){
        if (tv != null) {
            new Handler(Looper.getMainLooper()).post(() -> tv.append(text));
        }
    }

    /*private void toast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }*/

}
