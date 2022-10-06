package com.app.livesubtitle;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class VoiceRecognizer extends Service {

    public VoiceRecognizer() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private SpeechRecognizer speechRecognizer = null;
    public static Intent speechRecognizerIntent;
    public static Translator translator;

    @Override
    public void onCreate() {
        super.onCreate();
        //private String LOG_TAG = "VoiceRecognitionActivity";
        String src_dialect = LANGUAGE.SRC_DIALECT;
        Timer timer = new Timer();
        if(speechRecognizer != null) speechRecognizer.destroy();
        //Log.i(LOG_TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this));

        if(SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, Objects.requireNonNull(getClass().getPackage()).getName());
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            speechRecognizerIntent.putExtra("android.speech.extra.HIDE_PARTIAL_TRAILING_PUNCTUATION", true);
            speechRecognizerIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
            //speechRecognizerIntent.putExtra("android.speech.extra.SEGMENTED_SESSION", true);
            //speechRecognizerIntent.putExtra("android.speech.extra.GET_AUDIO_FORMAT",AudioFormat.ENCODING_PCM_8BIT);
            //speechRecognizerIntent.putExtra("android.speech.extra.GET_AUDIO",true);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, src_dialect);
            //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3600000);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle arg0) {
                    //Log.i(LOG_TAG, "onReadyForSpeech");
                }

                @Override
                public void onBeginningOfSpeech() {
                    //Log.i(LOG_TAG, "onBeginningOfSpeech");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    //Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    //Log.i(LOG_TAG, "onBufferReceived: " + buffer);
                }

                @Override
                public void onEndOfSpeech() {
                    //Log.i(LOG_TAG, "onEndOfSpeech");
                    if (!RECOGNIZING_STATUS.RECOGNIZING) {
                        speechRecognizer.stopListening();
                        if (translator != null) translator.close();
                    } else {
                        speechRecognizer.startListening(speechRecognizerIntent);
                    }
                }

                @Override
                public void onError(int errorCode) {
                    String errorMessage = getErrorText(errorCode);
                    MainActivity.textview_debug.setText(errorMessage);
                    //Log.i(LOG_TAG, "FAILED " + errorMessage);
                    if (!RECOGNIZING_STATUS.RECOGNIZING) {
                        speechRecognizer.stopListening();
                        if (translator != null) translator.close();
                    } else {
                        speechRecognizer.startListening(speechRecognizerIntent);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    //Log.i(LOG_TAG, "onResults");
                    /*if (!RECOGNIZING_STATUS.RECOGNIZING) {
                        speechRecognizer.stopListening();
                        if (translator != null) translator.close();
                    } else {
                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        VOICE_TEXT.STRING = matches.get(0);
                        MainActivity.voice_text.setText(VOICE_TEXT.STRING);
                        MainActivity.voice_text.setSelection(MainActivity.voice_text.getText().length());
                        speechRecognizer.startListening(speechRecognizerIntent);
                    }*/
                }

                @Override
                public void onPartialResults(Bundle results) {
                    //Log.i(LOG_TAG, "onPartialResults");
                    if (!RECOGNIZING_STATUS.RECOGNIZING) {
                        speechRecognizer.stopListening();
                        if (translator != null) translator.close();
                    } else {
                        ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (PREFER_OFFLINE_STATUS.OFFLINE) {
                            ArrayList<String> unstableData = results.getStringArrayList("android.speech.extra.UNSTABLE_TEXT");
                            VOICE_TEXT.STRING = data.get(0) + unstableData.get(0);
                        } else {
                            StringBuilder text = new StringBuilder();
                            for (String result : data)
                                text.append(result);
                            VOICE_TEXT.STRING = text.toString();
                        }
                        MainActivity.voice_text.setText(VOICE_TEXT.STRING);
                        MainActivity.voice_text.setSelection(MainActivity.voice_text.getText().length());
                    }
                }

                @Override
                public void onEvent(int arg0, Bundle arg1) {
                    //Log.i(LOG_TAG, "onEvent");
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

        if (RECOGNIZING_STATUS.RECOGNIZING) {
            speechRecognizer.startListening(speechRecognizerIntent);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (VOICE_TEXT.STRING != null) {
                        get_translation(VOICE_TEXT.STRING, LANGUAGE.SRC, LANGUAGE.DST);
                    }
                }
            },0,3000);
        } else {
            speechRecognizer.stopListening();
            if (translator != null) translator.close();
            stopSelf();
        }

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (RECOGNIZING_STATUS.RECOGNIZING) {
                speechRecognizer.startListening(speechRecognizerIntent);
            } else {
                speechRecognizer.stopListening();
                if (translator != null) translator.close();
                stopSelf();
            }
        }

        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            if (RECOGNIZING_STATUS.RECOGNIZING) {
                speechRecognizer.startListening(speechRecognizerIntent);
            } else {
                speechRecognizer.stopListening();
                if (translator != null) translator.close();
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (translator != null) translator.close();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }

    @SuppressLint("SetTextI18n")
    private void get_translation(final String text, String textFrom, String textTo) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(textFrom)
                .setTargetLanguage(textTo)
                .build();

        translator = Translation.getClient(options);

        if (!MLKIT_DICTIONARY.READY) {
            DownloadConditions conditions = new DownloadConditions.Builder().build();
            translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener(unused -> MLKIT_DICTIONARY.READY = true)
                    .addOnFailureListener(e -> {});
        }

        if (MLKIT_DICTIONARY.READY) {
            String downloaded_status_message = "Dictionary is ready";
            MainActivity.textview_debug2.setText(downloaded_status_message);
            translator.translate(text).addOnSuccessListener(s -> {
                TRANSLATION_TEXT.STRING = s;
                if (RECOGNIZING_STATUS.RECOGNIZING) {
                    if (TRANSLATION_TEXT.STRING.length() == 0) {
                        create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                        create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                    } else {
                        create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.VISIBLE);
                        create_overlay_translation_text.overlay_translation_text.setVisibility(View.VISIBLE);
                        create_overlay_translation_text.overlay_translation_text.setText(TRANSLATION_TEXT.STRING);
                        create_overlay_translation_text.overlay_translation_text.setSelection(create_overlay_translation_text.overlay_translation_text.getText().length());
                    }
                } else {
                    create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                    create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                }
            }).addOnFailureListener(e -> {});
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
