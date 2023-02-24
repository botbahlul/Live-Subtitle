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
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
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

public class VoiceRecognizer extends Service {

    public VoiceRecognizer() {}

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private SpeechRecognizer speechRecognizer = null;
    public static Intent speechRecognizerIntent;
    private Translator translator;
    private TranslatorOptions options;
    private DownloadConditions conditions;
    private String mlkit_status_message = "";
    private Timer timer;
    private TimerTask timerTask;

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

        String src_dialect = LANGUAGE.SRC_DIALECT;
        if(speechRecognizer != null) speechRecognizer.destroy();
        //setText(MainActivity.textview_debug, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this));

        RECOGNIZING_STATUS.STRING = "RECOGNIZING_STATUS.IS_RECOGNIZING = " + RECOGNIZING_STATUS.IS_RECOGNIZING;
        setText(MainActivity.textview_recognizing, RECOGNIZING_STATUS.STRING);
        OVERLAYING_STATUS.STRING = "OVERLAYING_STATUS.IS_OVERLAYING = " + OVERLAYING_STATUS.IS_OVERLAYING;
        setText(MainActivity.textview_overlaying, OVERLAYING_STATUS.STRING);

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

            //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, Objects.requireNonNull(getClass().getPackage()).getName());
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            //speechRecognizerIntent.putExtra("android.speech.extra.HIDE_PARTIAL_TRAILING_PUNCTUATION", true);
            //speechRecognizerIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
            //speechRecognizerIntent.putExtra("android.speech.extra.AUDIO_SOURCE",true);
            //speechRecognizerIntent.putExtra("android.speech.extra.GET_AUDIO",true);
            //speechRecognizerIntent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", AudioFormat.ENCODING_PCM_8BIT);
            //speechRecognizerIntent.putExtra("android.speech.extra.SEGMENTED_SESSION", true);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, src_dialect);
            //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,3600000);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.google.android.googlequicksearchbox");
            //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle arg0) {
                    setText(MainActivity.textview_debug, "onReadyForSpeech");
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
                        if (translator != null) translator.close();
                    } else {
                        speechRecognizer.startListening(speechRecognizerIntent);
                    }
                }

                @Override
                public void onError(int errorCode) {
                    String errorMessage = getErrorText(errorCode);
                    setText(MainActivity.textview_debug, "onError : " + errorMessage);
                    if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                        speechRecognizer.stopListening();
                    } else {
                        if (Objects.equals(getErrorText(errorCode), "Insufficient permissions")) {
                            String msg = "Please give RECORD AUDIO PERMISSION (USE MICROPHONE PERMISSION) to GOOGLE APP";
                            setText(MainActivity.textview_output_messages, msg);
                        }
                        else {
                            setText(MainActivity.textview_debug, "onError : " + errorMessage);
                        }
                        speechRecognizer.startListening(speechRecognizerIntent);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    //setText(MainActivity.textview_debug, "onResults");
                    /*if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                        speechRecognizer.stopListening();
                    } else {
                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        VOICE_TEXT.STRING = matches.get(0).toLowerCase(Locale.forLanguageTag(LANGUAGE.SRC));
                        setText(MainActivity.voice_text, VOICE_TEXT.STRING);
                        MainActivity.voice_text.setSelection(MainActivity.voice_text.getText().length());
                        speechRecognizer.startListening(speechRecognizerIntent);
                    }*/
                }

                @Override
                public void onPartialResults(Bundle results) {
                    if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                        speechRecognizer.stopListening();
                        if (translator != null) translator.close();
                    } else {
                        ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (PREFER_OFFLINE_STATUS.OFFLINE) {
                            ArrayList<String> unstableData = results.getStringArrayList("android.speech.extra.UNSTABLE_TEXT");
                            VOICE_TEXT.STRING = data.get(0).toLowerCase(Locale.forLanguageTag(LANGUAGE.SRC)) + unstableData.get(0).toLowerCase(Locale.forLanguageTag(LANGUAGE.SRC));
                        } else {
                            StringBuilder text = new StringBuilder();
                            for (String result : data)
                                text.append(result);
                            VOICE_TEXT.STRING = text.toString().toLowerCase(Locale.forLanguageTag(LANGUAGE.SRC));
                        }
                        MainActivity.voice_text.setText(VOICE_TEXT.STRING);
                        MainActivity.voice_text.setSelection(MainActivity.voice_text.getText().length());
                    }
                }

                @Override
                public void onEvent(int arg0, Bundle arg1) {
                    //setText(MainActivity.textview_debug, "onEvent");
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

        if (RECOGNIZING_STATUS.IS_RECOGNIZING) {
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
                                if (TRANSLATION_TEXT.STRING.length() == 0) {
                                    create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                                    create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                                } else {
                                    create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.VISIBLE);
                                    create_overlay_translation_text.overlay_translation_text_container.setBackgroundColor(Color.TRANSPARENT);
                                    create_overlay_translation_text.overlay_translation_text.setVisibility(View.VISIBLE);
                                    create_overlay_translation_text.overlay_translation_text.setBackgroundColor(Color.TRANSPARENT);
                                    create_overlay_translation_text.overlay_translation_text.setTextIsSelectable(true);
                                    create_overlay_translation_text.overlay_translation_text.setText(TRANSLATION_TEXT.STRING);
                                    create_overlay_translation_text.overlay_translation_text.setSelection(create_overlay_translation_text.overlay_translation_text.getText().length());
                                    Spannable spannableString = new SpannableStringBuilder(TRANSLATION_TEXT.STRING);
                                    spannableString.setSpan(new ForegroundColorSpan(Color.YELLOW),
                                            0,
                                            create_overlay_translation_text.overlay_translation_text.getSelectionEnd(),
                                            0);
                                    spannableString.setSpan(new BackgroundColorSpan(Color.parseColor("#80000000")),
                                            0,
                                            create_overlay_translation_text.overlay_translation_text.getSelectionEnd(),
                                            0);
                                    create_overlay_translation_text.overlay_translation_text.setText(spannableString);
                                    create_overlay_translation_text.overlay_translation_text.setSelection(create_overlay_translation_text.overlay_translation_text.getText().length());
                                }
                            } else {
                                VOICE_TEXT.STRING = "";
                                TRANSLATION_TEXT.STRING = "";
                                create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                                create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                    else {
                        TRANSLATION_TEXT.STRING = "";
                        create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                        create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                    }
                }
            };
            timer.schedule(timerTask,0,1000);
        }
        else {
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
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        if (translator != null) translator.close();
    }

    public void setText(final TextView tv, final String text){
        new Handler(Looper.getMainLooper()).post(() -> tv.setText(text));
    }

    /*@SuppressLint("SetTextI18n")
    private void get_translation(final String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        if (MLKIT_DICTIONARY.READY) {
            executor.execute(() -> {
                translator.translate(text).addOnSuccessListener(s -> TRANSLATION_TEXT.STRING = s.toLowerCase(Locale.forLanguageTag(LANGUAGE.DST))).addOnFailureListener(e -> {});
                handler.post(() -> {
                    if (RECOGNIZING_STATUS.IS_RECOGNIZING) {
                        if (TRANSLATION_TEXT.STRING.length() == 0) {
                            create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                            create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                        } else {
                            create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.VISIBLE);
                            create_overlay_translation_text.overlay_translation_text_container.setBackgroundColor(Color.TRANSPARENT);
                            create_overlay_translation_text.overlay_translation_text.setVisibility(View.VISIBLE);
                            create_overlay_translation_text.overlay_translation_text.setBackgroundColor(Color.TRANSPARENT);
                            create_overlay_translation_text.overlay_translation_text.setTextIsSelectable(true);
                            create_overlay_translation_text.overlay_translation_text.setText(TRANSLATION_TEXT.STRING);
                            create_overlay_translation_text.overlay_translation_text.setSelection(create_overlay_translation_text.overlay_translation_text.getText().length());
                            Spannable spannableString = new SpannableStringBuilder(TRANSLATION_TEXT.STRING);
                            spannableString.setSpan(new ForegroundColorSpan(Color.YELLOW),
                                    0,
                                    create_overlay_translation_text.overlay_translation_text.getSelectionEnd(),
                                    0);
                            spannableString.setSpan(new BackgroundColorSpan(Color.parseColor("#80000000")),
                                    0,
                                    create_overlay_translation_text.overlay_translation_text.getSelectionEnd(),
                                    0);
                            create_overlay_translation_text.overlay_translation_text.setText(spannableString);
                            create_overlay_translation_text.overlay_translation_text.setSelection(create_overlay_translation_text.overlay_translation_text.getText().length());
                        }
                    } else {
                        create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                        create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                    }
                });
            });
        }
    }*/

    /*private void toast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }*/

}
