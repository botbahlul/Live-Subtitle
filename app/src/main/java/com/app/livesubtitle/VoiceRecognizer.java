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
import android.util.Log;
import android.view.View;
import android.widget.TextView;
//import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class VoiceRecognizer extends Service {

    public VoiceRecognizer() {}

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private SpeechRecognizer speechRecognizer = null;
    public static Intent speechRecognizerIntent;

    @Override
    public void onCreate() {
        super.onCreate();
            int h;
            if (Objects.equals(LANGUAGE.SRC, "ja") || Objects.equals(LANGUAGE.SRC, "zh-Hans") || Objects.equals(LANGUAGE.SRC, "zh-Hant")) {
                h = 122;
            } else {
                h = 109;
            }
            MainActivity.voice_text.setHeight((int) (h * getResources().getDisplayMetrics().density));

            String src_dialect = LANGUAGE.SRC_DIALECT;

            Timer timer = new Timer();
            //if (speechRecognizer != null) speechRecognizer.destroy();
            String string_recognizing = "RECOGNIZING_STATUS.RECOGNIZING = " + RECOGNIZING_STATUS.RECOGNIZING;
            setText(MainActivity.textview_recognizing, string_recognizing);
            String string_overlaying = "OVERLAYING_STATUS.OVERLAYING = " + OVERLAYING_STATUS.OVERLAYING;
            setText(MainActivity.textview_overlaying, string_overlaying);

            if (SpeechRecognizer.isRecognitionAvailable(this)) {
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
                        if (!RECOGNIZING_STATUS.RECOGNIZING) {
                            speechRecognizer.stopListening();
                        } else {
                            speechRecognizer.startListening(speechRecognizerIntent);
                        }
                    }

                    @Override
                    public void onError(int errorCode) {
                        setText(MainActivity.textview_debug, "onError");
                        if (!RECOGNIZING_STATUS.RECOGNIZING) {
                            speechRecognizer.stopListening();
                        } else {
                            //if (Objects.equals(getErrorText(errorCode), "RecognitionService busy")) {
                                //speechRecognizer.stopListening();
                                //speechRecognizer.startListening(speechRecognizerIntent);
                            //}
                            speechRecognizer.startListening(speechRecognizerIntent);
                            setText(MainActivity.textview_debug2, getErrorText(errorCode));
                        }
                    }

                    @Override
                    public void onResults(Bundle results) {
                        /*setText(MainActivity.textview_debug, "onResults");
                        if (!RECOGNIZING_STATUS.RECOGNIZING) {
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
                        if (!RECOGNIZING_STATUS.RECOGNIZING) {
                            speechRecognizer.stopListening();
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
                        setText(MainActivity.textview_debug, "onEvent");
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
                            //translate(VOICE_TEXT.STRING, LANGUAGE.SRC, LANGUAGE.DST);
                            GoogleTranslate2(VOICE_TEXT.STRING, LANGUAGE.SRC, LANGUAGE.DST);
                        }
                    }
                }, 0, 1000);
            } else {
                speechRecognizer.stopListening();
                stopSelf();
            }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }

    /*public void translate(String t, String src, String dst) {
        GoogleTranslateAPITranslator translate = new GoogleTranslateAPITranslator();
        translate.setOnTranslationCompleteListener(new GoogleTranslateAPITranslator.OnTranslationCompleteListener() {
            @Override
            public void onStartTranslation() {}

            @Override
            public void onCompleted(String translation) {
                TRANSLATION_TEXT.STRING = translation;
                if (RECOGNIZING_STATUS.RECOGNIZING) {
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
            }

            @Override
            public void onError(Exception e) {
                //Toast.makeText(MainActivity.this, "Unknown error", Toast.LENGTH_SHORT).show();
                setText(MainActivity.textview_debug, e.getMessage());
            }
        });
        translate.execute(t, src, dst);
    }*/

    /*public void gtranslate(String t, String src, String dst) {
        GoogleClient5Translator translate = new GoogleClient5Translator();
        translate.setOnTranslationCompleteListener(new GoogleClient5Translator.OnTranslationCompleteListener() {
            @Override
            public void onStartTranslation() {}

            @Override
            public void onCompleted(String translation) {
                if (RECOGNIZING_STATUS.RECOGNIZING) {
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


            }

            @Override
            public void onError(Exception e) {
                //toast("Unknown error");
                //setText(textview_debug, e.getMessage());
            }
        });
        translate.execute(t, src, dst);
    }*/

    /*private void toast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }*/

    public void setText(final TextView tv, final String text){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> tv.setText(text));
    }

    private String GoogleTranslate(String SENTENCE, String SRC, String DST) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        AtomicReference<String> TRANSLATION = new AtomicReference<>("");
        try {
            SENTENCE = URLEncoder.encode(SENTENCE, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String finalSENTENCE = SENTENCE;
        if (RECOGNIZING_STATUS.RECOGNIZING && finalSENTENCE != null) {
            executor.execute(() -> {
                try {
                    String url = "https://translate.googleapis.com/translate_a/";
                    String params = "single?client=gtx&sl=" + SRC + "&tl=" + DST + "&dt=t&q=" + finalSENTENCE;
                    HttpResponse response = new DefaultHttpClient().execute(new HttpGet(url + params));
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == 200) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        response.getEntity().writeTo(byteArrayOutputStream);
                        String stringOfByteArrayOutputStream = Objects.requireNonNull(byteArrayOutputStream).toString();
                        byteArrayOutputStream.close();
                        //JSONArray jSONArray = null;
                        //if (new JSONArray(stringOfByteArrayOutputStream).getJSONArray(0) != null) {
                        //JSONArray jsonArray = new JSONArray(Objects.requireNonNull(stringOfByteArrayOutputStream)).getJSONArray(0);
                        //}
                        for (int i = 0; i < Objects.requireNonNull(new JSONArray(Objects.requireNonNull(stringOfByteArrayOutputStream)).getJSONArray(0)).length(); i++) {
                            //JSONArray jsonArray2 = new JSONArray(Objects.requireNonNull(stringOfByteArrayOutputStream)).getJSONArray(0).getJSONArray(i);
                            TRANSLATION.set(TRANSLATION + new JSONArray(Objects.requireNonNull(stringOfByteArrayOutputStream)).getJSONArray(0).getJSONArray(i).get(0).toString());
                        }
                    } else {
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                } catch (Exception e) {
                    Log.e("GoogleTranslator", e.getMessage());
                    e.printStackTrace();
                    System.out.println();
                }

                handler.post(() -> {
                    TRANSLATION_TEXT.STRING = TRANSLATION.toString();
                    if (RECOGNIZING_STATUS.RECOGNIZING) {
                        if (TRANSLATION_TEXT.STRING.length() == 0) {
                            create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                            create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                            executor.shutdown();
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
        else {
            executor.shutdown();
        }
        return TRANSLATION.toString();
    }


    private void GoogleTranslate2(String SENTENCE, String SRC, String DST) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        AtomicReference<String> TRANSLATION = new AtomicReference<>("");
        try {
            SENTENCE = URLEncoder.encode(SENTENCE, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String finalSENTENCE = SENTENCE;
        if (RECOGNIZING_STATUS.RECOGNIZING && finalSENTENCE != null) {
            executor.execute(() -> {
                try {
                    String url = "https://translate.googleapis.com/translate_a/";
                    String params = "single?client=gtx&sl=" + SRC + "&tl=" + DST + "&dt=t&q=" + finalSENTENCE;
                    HttpResponse response = new DefaultHttpClient().execute(new HttpGet(url + params));
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == 200) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        response.getEntity().writeTo(byteArrayOutputStream);
                        String stringOfByteArrayOutputStream = byteArrayOutputStream.toString();
                        byteArrayOutputStream.close();
                        for (int i = 0; i < Objects.requireNonNull(new JSONArray(Objects.requireNonNull(stringOfByteArrayOutputStream)).getJSONArray(0)).length(); i++) {
                            TRANSLATION.set(TRANSLATION + new JSONArray(Objects.requireNonNull(stringOfByteArrayOutputStream)).getJSONArray(0).getJSONArray(i).get(0).toString());
                        }
                    } else {
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                } catch (Exception e) {
                    //Log.e("GoogleTranslator", e.getMessage());
                    //e.printStackTrace();
                }

                handler.post(() -> {
                    TRANSLATION_TEXT.STRING = TRANSLATION.toString();
                    if (RECOGNIZING_STATUS.RECOGNIZING) {
                        if (TRANSLATION_TEXT.STRING.length() == 0) {
                            create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                            create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                            executor.shutdown();
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
        else {
            executor.shutdown();
        }
    }


}
