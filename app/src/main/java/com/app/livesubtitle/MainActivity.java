package com.app.livesubtitle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final Integer RecordAudioRequestCode = 1;
    private DisplayMetrics display;

    private TextView textview_src_dialect;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_src;
    private TextView textview_dst_dialect;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_dst;
    private ArrayList<String> arraylist_languages;
    private String [] countries;
    private String [] dialects;
    private Map<String, String> countries_dialects;
    private Spinner spinner_src_languages;
    private Spinner spinner_dst_languages;
    @SuppressLint("StaticFieldLeak")
    public static CheckBox checkbox_offline_mode;
    @SuppressLint("StaticFieldLeak")
    public static EditText voice_text;
    @SuppressLint("StaticFieldLeak")
    public static AudioManager audio;
    public static int mStreamVolume;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_recognizing;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_overlaying;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_debug;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_debug2;

    private String string_en_src_folder;
    private String string_en_dst_folder;
    private String string_src_en_folder;
    private String string_dst_en_folder;
    private File file_en_src_folder;
    private File file_en_dst_folder;
    private File file_src_en_folder;
    private File file_dst_en_folder;
    private String mlkit_status_message = "";

    //DON'T FORGET TO MODIFY AndroidManifest.xml
    //         <activity
    //            android:name=".MainActivity"
    //            android:configChanges="keyboardHidden|screenSize|orientation|screenLayout|navigation"

    @SuppressLint({"ClickableViewAccessibility", "QueryPermissionsNeeded", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION);
        AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        am.setSpeakerphoneOn(true);
        spinner_src_languages = findViewById(R.id.spinner_src_languages);
        checkbox_offline_mode = findViewById(R.id.checkbox_offline_mode);
        spinner_dst_languages = findViewById(R.id.spinner_dst_languages);
        Button button_toggle_overlay = findViewById(R.id.button_toggle_overlay);
        textview_src_dialect = findViewById(R.id.textview_src_dialect);
        textview_src = findViewById(R.id.textview_src);
        textview_dst_dialect = findViewById(R.id.textview_dst_dialect);
        textview_dst = findViewById(R.id.textview_dst);
        voice_text = findViewById(R.id.voice_text);
        textview_recognizing = findViewById(R.id.textview_recognizing);
        textview_overlaying = findViewById(R.id.textview_overlaying);
        textview_debug = findViewById(R.id.textview_debug);
        textview_debug2 = findViewById(R.id.textview_debug2);
        VOICE_TEXT.STRING = "";
        TRANSLATION_TEXT.STRING = "";
        PREFER_OFFLINE_STATUS.OFFLINE = checkbox_offline_mode.isChecked();
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mStreamVolume = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

        display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(display);
        float d = display.density;
        DISPLAY_METRIC.DISPLAY_WIDTH = display.widthPixels;
        DISPLAY_METRIC.DISPLAY_HEIGHT = display.heightPixels;
        DISPLAY_METRIC.DISPLAY_DENSITY = d;

        RECOGNIZING_STATUS.RECOGNIZING = false;
        OVERLAYING_STATUS.OVERLAYING = false;
        String string_recognizing = "recognizing=" + RECOGNIZING_STATUS.RECOGNIZING;
        textview_recognizing.setText(string_recognizing);
        String string_overlaying = "overlaying=" + OVERLAYING_STATUS.OVERLAYING;
        textview_overlaying.setText(string_overlaying);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            getSupportActionBar().setCustomView(R.layout.actionbar_layout);
        }

        checkRecordAudioPermission();

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }

        final Intent ri = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        PackageManager pm = getPackageManager();
        boolean isInstalled = isPackageInstalled("com.google.android.googlequicksearchbox", pm);
        if (!isInstalled) Toast.makeText(this,"Please install Googple app (com.google.android.googlequicksearchbox)",Toast.LENGTH_SHORT).show();
        if (isInstalled) ri.setPackage("com.google.android.googlequicksearchbox");

        this.sendOrderedBroadcast(ri,null,new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final Bundle extra = getResultExtras(false);
                        if (getResultCode() == Activity.RESULT_OK && extra.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
                            arraylist_languages = extra.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
                            dialects = arraylist_languages.toArray(new String[0]);
                            for (int i = 0; i < dialects.length; i++) {
                                dialects[i] = dialects[i].trim();
                            }
                            if (arraylist_languages != null) {
                                for (int i = 0; i < arraylist_languages.size(); i++) {
                                    Locale locale = Locale.forLanguageTag(arraylist_languages.get(i));
                                    arraylist_languages.set(i, locale.getDisplayName().trim());
                                }
                                countries = arraylist_languages.toArray(new String[0]);
                                for (int i = 0; i < countries.length; i++) {
                                    countries[i] = countries[i].trim();
                                }
                                setup_spinner(arraylist_languages);
                            }
                        }
                    }
                },
                null,
                Activity.RESULT_OK,
                null,
                null
        );

        spinner_src_languages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                stop_voice_recognizer();
                if (RECOGNIZING_STATUS.RECOGNIZING) {
                    start_voice_recognizer();
                }
                stop_create_overlay_translation_text();
                if (OVERLAYING_STATUS.OVERLAYING) start_create_overlay_translation_text();

                stop_create_overlay_mic_button();
                if (OVERLAYING_STATUS.OVERLAYING) start_create_overlay_mic_button();

                String src_country = spinner_src_languages.getSelectedItem().toString();
                LANGUAGE.SRC_DIALECT = countries_dialects.get(src_country);
                if (LANGUAGE.SRC_DIALECT != null) {
                    LANGUAGE.SRC = LANGUAGE.SRC_DIALECT.split("-")[0];
                    if (LANGUAGE.SRC_DIALECT.equals("yue-Hant-HK") || LANGUAGE.SRC_DIALECT.equals("cmn-Hans-CN") || LANGUAGE.SRC_DIALECT.equals("cmn-Hans-HK") || LANGUAGE.SRC_DIALECT.equals("cmn-Hant-TW")) {
                        LANGUAGE.SRC = "zh";
                    }
                }
                textview_src_dialect.setText(LANGUAGE.SRC_DIALECT);
                textview_src.setText(LANGUAGE.SRC);

                String dst_country = spinner_dst_languages.getSelectedItem().toString();
                LANGUAGE.DST_DIALECT = countries_dialects.get(dst_country);
                if (LANGUAGE.DST_DIALECT != null) {
                    LANGUAGE.DST = LANGUAGE.DST_DIALECT.split("-")[0];
                    if (LANGUAGE.DST_DIALECT.equals("yue-Hant-HK") || LANGUAGE.DST_DIALECT.equals("cmn-Hans-CN") || LANGUAGE.DST_DIALECT.equals("cmn-Hans-HK") || LANGUAGE.DST_DIALECT.equals("cmn-Hant-TW")) {
                        LANGUAGE.DST = "zh";
                    }
                }
                textview_dst_dialect.setText(LANGUAGE.DST_DIALECT);
                textview_dst.setText(LANGUAGE.DST);

                string_en_src_folder = Environment.getDataDirectory() + "/data/" + getApplicationContext().getPackageName() + "/no_backup/com.google.mlkit.translate.models/" + "en" + "_" + textview_src.getText();
                string_en_dst_folder = Environment.getDataDirectory() + "/data/" + getApplicationContext().getPackageName() + "/no_backup/com.google.mlkit.translate.models/" + "en" + "_" + textview_dst.getText();
                string_src_en_folder = Environment.getDataDirectory() + "/data/" + getApplicationContext().getPackageName() + "/no_backup/com.google.mlkit.translate.models/" + textview_src.getText() + "_" + "en" ;
                string_dst_en_folder = Environment.getDataDirectory() + "/data/" + getApplicationContext().getPackageName() + "/no_backup/com.google.mlkit.translate.models/" + textview_dst.getText() + "_" + "en" ;
                file_en_src_folder = new File(string_en_src_folder);
                file_en_dst_folder = new File(string_en_dst_folder);
                file_src_en_folder = new File(string_src_en_folder);
                file_dst_en_folder = new File(string_dst_en_folder);
                check_mlkit_dictionary();

                int h;
                if (Objects.equals(LANGUAGE.DST, "ja") || Objects.equals(LANGUAGE.DST, "zh")) {
                    h = 75;
                }
                else {
                    h = 62;
                }
                voice_text.setHeight((int) (h * getResources().getDisplayMetrics().density));

                stop_voice_recognizer();
                stop_create_overlay_translation_text();
                stop_create_overlay_mic_button();
                if (OVERLAYING_STATUS.OVERLAYING) {
                    if (!RECOGNIZING_STATUS.RECOGNIZING) {
                        if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setImageResource(R.drawable.ic_mic_black_off);
                    } else {
                        start_voice_recognizer();
                        if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setImageResource(R.drawable.ic_mic_black_on);
                    }
                    start_create_overlay_mic_button();
                    if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setBackgroundColor(Color.parseColor("#80000000"));

                    start_create_overlay_translation_text();
                    if (TRANSLATION_TEXT.STRING.length() > 0) {
                        if (create_overlay_translation_text.overlay_translation_text != null) create_overlay_translation_text.overlay_translation_text.setText(TRANSLATION_TEXT.STRING);
                    }
                }

                String string_recognizing = "recognizing=" + RECOGNIZING_STATUS.RECOGNIZING;
                textview_recognizing.setText(string_recognizing);
                String string_overlaying =  "overlaying=" + OVERLAYING_STATUS.OVERLAYING;
                textview_overlaying.setText(string_overlaying);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                String src_country = spinner_src_languages.getSelectedItem().toString();
                LANGUAGE.SRC_DIALECT = countries_dialects.get(src_country);
                if (LANGUAGE.SRC_DIALECT != null) {
                    LANGUAGE.SRC = LANGUAGE.SRC_DIALECT.split("-")[0];
                    if (LANGUAGE.SRC_DIALECT.equals("yue-Hant-HK") || LANGUAGE.SRC_DIALECT.equals("cmn-Hans-CN") || LANGUAGE.SRC_DIALECT.equals("cmn-Hans-HK") || LANGUAGE.SRC_DIALECT.equals("cmn-Hant-TW")) {
                        LANGUAGE.SRC = "zh";
                    }
                }
                textview_src_dialect.setText(LANGUAGE.SRC_DIALECT);
                textview_src.setText(LANGUAGE.SRC);

                String dst_country = spinner_dst_languages.getSelectedItem().toString();
                LANGUAGE.DST_DIALECT = countries_dialects.get(dst_country);
                if (LANGUAGE.DST_DIALECT != null) {
                    LANGUAGE.DST = LANGUAGE.DST_DIALECT.split("-")[0];
                    if (LANGUAGE.DST_DIALECT.equals("yue-Hant-HK") || LANGUAGE.DST_DIALECT.equals("cmn-Hans-CN") || LANGUAGE.DST_DIALECT.equals("cmn-Hans-HK") || LANGUAGE.DST_DIALECT.equals("cmn-Hant-TW")) {
                        LANGUAGE.DST = "zh";
                    }
                }
                textview_dst_dialect.setText(LANGUAGE.DST_DIALECT);
                textview_dst.setText(LANGUAGE.DST);
            }
        });

        spinner_dst_languages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                stop_voice_recognizer();
                if (RECOGNIZING_STATUS.RECOGNIZING) {
                    start_voice_recognizer();
                }
                stop_create_overlay_translation_text();
                if (OVERLAYING_STATUS.OVERLAYING) start_create_overlay_translation_text();

                stop_create_overlay_mic_button();
                if (OVERLAYING_STATUS.OVERLAYING) start_create_overlay_mic_button();

                String src_country = spinner_src_languages.getSelectedItem().toString();
                LANGUAGE.SRC_DIALECT = countries_dialects.get(src_country);
                if (LANGUAGE.SRC_DIALECT != null) {
                    LANGUAGE.SRC = LANGUAGE.SRC_DIALECT.split("-")[0];
                    if (LANGUAGE.SRC_DIALECT.equals("yue-Hant-HK") || LANGUAGE.SRC_DIALECT.equals("cmn-Hans-CN") || LANGUAGE.SRC_DIALECT.equals("cmn-Hans-HK") || LANGUAGE.SRC_DIALECT.equals("cmn-Hant-TW")) {
                        LANGUAGE.SRC = "zh";
                    }
                }
                textview_src_dialect.setText(LANGUAGE.SRC_DIALECT);
                textview_src.setText(LANGUAGE.SRC);

                String dst_country = spinner_dst_languages.getSelectedItem().toString();
                LANGUAGE.DST_DIALECT = countries_dialects.get(dst_country);
                if (LANGUAGE.DST_DIALECT != null) {
                    LANGUAGE.DST = LANGUAGE.DST_DIALECT.split("-")[0];
                    if (LANGUAGE.DST_DIALECT.equals("yue-Hant-HK") || LANGUAGE.DST_DIALECT.equals("cmn-Hans-CN") || LANGUAGE.DST_DIALECT.equals("cmn-Hans-HK") || LANGUAGE.DST_DIALECT.equals("cmn-Hant-TW")) {
                        LANGUAGE.DST = "zh";
                    }
                }
                textview_dst_dialect.setText(LANGUAGE.DST_DIALECT);
                textview_dst.setText(LANGUAGE.DST);
                string_en_src_folder = Environment.getDataDirectory() + "/data/" + getApplicationContext().getPackageName() + "/no_backup/com.google.mlkit.translate.models/" + "en" + "_" + textview_src.getText();
                string_en_dst_folder = Environment.getDataDirectory() + "/data/" + getApplicationContext().getPackageName() + "/no_backup/com.google.mlkit.translate.models/" + "en" + "_" + textview_dst.getText();
                string_src_en_folder = Environment.getDataDirectory() + "/data/" + getApplicationContext().getPackageName() + "/no_backup/com.google.mlkit.translate.models/" + textview_src.getText() + "_" + "en" ;
                string_dst_en_folder = Environment.getDataDirectory() + "/data/" + getApplicationContext().getPackageName() + "/no_backup/com.google.mlkit.translate.models/" + textview_dst.getText() + "_" + "en" ;
                file_en_src_folder = new File(string_en_src_folder);
                file_en_dst_folder = new File(string_en_dst_folder);
                file_src_en_folder = new File(string_src_en_folder);
                file_dst_en_folder = new File(string_dst_en_folder);
                check_mlkit_dictionary();

                int h;
                if (Objects.equals(LANGUAGE.DST, "ja") || Objects.equals(LANGUAGE.DST, "zh")) {
                    h = 75;
                }
                else {
                    h = 62;
                }
                voice_text.setHeight((int) (h * getResources().getDisplayMetrics().density));

                stop_voice_recognizer();
                stop_create_overlay_translation_text();
                stop_create_overlay_mic_button();
                if (OVERLAYING_STATUS.OVERLAYING) {
                    if (!RECOGNIZING_STATUS.RECOGNIZING) {
                        if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setImageResource(R.drawable.ic_mic_black_off);
                    } else {
                        start_voice_recognizer();
                        if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setImageResource(R.drawable.ic_mic_black_on);
                    }
                    start_create_overlay_mic_button();
                    if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setBackgroundColor(Color.parseColor("#80000000"));

                    start_create_overlay_translation_text();
                    if (TRANSLATION_TEXT.STRING.length() > 0) {
                        if (create_overlay_translation_text.overlay_translation_text != null) create_overlay_translation_text.overlay_translation_text.setText(TRANSLATION_TEXT.STRING);
                    }
                }

                String string_recognizing = "recognizing=" + RECOGNIZING_STATUS.RECOGNIZING;
                textview_recognizing.setText(string_recognizing);
                String string_overlaying =  "overlaying=" + OVERLAYING_STATUS.OVERLAYING;
                textview_overlaying.setText(string_overlaying);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {

                String src_country = spinner_src_languages.getSelectedItem().toString();
                LANGUAGE.SRC_DIALECT = countries_dialects.get(src_country);
                if (LANGUAGE.SRC_DIALECT != null) {
                    LANGUAGE.SRC = LANGUAGE.SRC_DIALECT.split("-")[0];
                    if (LANGUAGE.SRC_DIALECT.equals("yue-Hant-HK") || LANGUAGE.SRC_DIALECT.equals("cmn-Hans-CN") || LANGUAGE.SRC_DIALECT.equals("cmn-Hans-HK") || LANGUAGE.SRC_DIALECT.equals("cmn-Hant-TW")) {
                        LANGUAGE.SRC = "zh";
                    }
                }
                textview_src_dialect.setText(LANGUAGE.SRC_DIALECT);
                textview_src.setText(LANGUAGE.SRC);

                String dst_country = spinner_dst_languages.getSelectedItem().toString();
                LANGUAGE.DST_DIALECT = countries_dialects.get(dst_country);
                if (LANGUAGE.DST_DIALECT != null) {
                    LANGUAGE.DST = LANGUAGE.DST_DIALECT.split("-")[0];
                    if (LANGUAGE.DST_DIALECT.equals("yue-Hant-HK") || LANGUAGE.DST_DIALECT.equals("cmn-Hans-CN") || LANGUAGE.DST_DIALECT.equals("cmn-Hans-HK") || LANGUAGE.DST_DIALECT.equals("cmn-Hant-TW")) {
                        LANGUAGE.DST = "zh";
                    }
                }
                textview_dst_dialect.setText(LANGUAGE.DST_DIALECT);
                textview_dst.setText(LANGUAGE.DST);
            }
        });

        button_toggle_overlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    OVERLAYING_STATUS.OVERLAYING = !OVERLAYING_STATUS.OVERLAYING;
                    String string_overlaying = "overlaying=" + OVERLAYING_STATUS.OVERLAYING;
                    textview_overlaying.setText(string_overlaying);
                    checkDrawOverlayPermission();
                    if (OVERLAYING_STATUS.OVERLAYING) {
                        start_create_overlay_mic_button();
                        start_create_overlay_translation_text();
                    } else {
                        stop_voice_recognizer();
                        stop_create_overlay_translation_text();
                        stop_create_overlay_mic_button();
                        RECOGNIZING_STATUS.RECOGNIZING = false;
                        String string_recognizing = "recognizing=" + RECOGNIZING_STATUS.RECOGNIZING;
                        textview_recognizing.setText(string_recognizing);
                        string_overlaying = "overlaying=" + OVERLAYING_STATUS.OVERLAYING;
                        textview_overlaying.setText(string_overlaying);
                        MainActivity.textview_debug.setText("");
                        VOICE_TEXT.STRING = "";
                        TRANSLATION_TEXT.STRING = "";
                        MainActivity.voice_text.setText("");
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
                        MainActivity.textview_debug.setText("");
                        VOICE_TEXT.STRING = "";
                        TRANSLATION_TEXT.STRING = "";
                        MainActivity.voice_text.setText("");
                        string_recognizing = "recognizing=" + RECOGNIZING_STATUS.RECOGNIZING;
                        textview_recognizing.setText(string_recognizing);
                        string_overlaying = "overlaying=" + OVERLAYING_STATUS.OVERLAYING;
                        textview_overlaying.setText(string_overlaying);
                        hints = "Recognized words";
                        MainActivity.voice_text.setHint(hints);
                    }
                }
                return false;
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void setup_spinner(ArrayList<String> supported_languages) {
        countries_dialects = new HashMap<>();
        for (int i=0;i<supported_languages.size();i++) {
            countries_dialects.put(supported_languages.get(i), dialects[i]);
        }

        Collections.sort(supported_languages);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_textview_align, supported_languages);
        adapter.setDropDownViewResource(R.layout.spinner_textview_align);
        spinner_src_languages.setAdapter(adapter);
        spinner_src_languages.setSelection(supported_languages.indexOf("Indonesian (Indonesia)"));
        spinner_dst_languages.setAdapter(adapter);
        spinner_dst_languages.setSelection(supported_languages.indexOf("English (United States)"));
    }

    private void checkRecordAudioPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
        }
    }

    private void checkDrawOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(myIntent);
        }
    }

    private void start_create_overlay_mic_button() {
        Intent i = new Intent(this, create_overlay_mic_button.class);
        startService(i);
    }

    private void stop_create_overlay_mic_button() {
        stopService(new Intent(this, create_overlay_mic_button.class));
    }

    private void start_create_overlay_translation_text() {
        Intent i = new Intent(this, create_overlay_translation_text.class);
        startService(i);
    }

    private void stop_create_overlay_translation_text() {
        stopService(new Intent(this, create_overlay_translation_text.class));
    }

    private void start_voice_recognizer() {
        Intent i = new Intent(this, VoiceRecognizer.class);
        startService(i);
    }

    private void stop_voice_recognizer() {
        stopService(new Intent(this, VoiceRecognizer.class));
    }

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void check_mlkit_dictionary() {
        if (Objects.equals(textview_src.getText(), textview_dst.getText())) {
            MLKIT_DICTIONARY.READY = true;
            mlkit_status_message = "";
        }
        if (Objects.equals(textview_src.getText(), "en")) {
            if (file_en_dst_folder.exists() || file_dst_en_folder.exists()) {
                MLKIT_DICTIONARY.READY = true;
                mlkit_status_message = "Dictionary is ready";
            } else {
                MLKIT_DICTIONARY.READY = false;
                mlkit_status_message = "Dictionary is not ready";
            }
        }
        if (Objects.equals(textview_dst.getText(), "en")) {
            if (file_en_src_folder.exists() || file_src_en_folder.exists()) {
                MLKIT_DICTIONARY.READY = true;
                mlkit_status_message = "Dictionary is ready";
            } else {
                MLKIT_DICTIONARY.READY = false;
                mlkit_status_message = "Dictionary is not ready";
            }
        }
        if (!(Objects.equals(textview_src.getText(), "en")) && !(Objects.equals(textview_dst.getText(), "en"))) {
            if ((file_en_src_folder.exists() || file_src_en_folder.exists()) && (file_en_dst_folder.exists()) || file_dst_en_folder.exists()) {
                MLKIT_DICTIONARY.READY = true;
                mlkit_status_message = "Dictionary is ready";
            }
            else if ((file_en_src_folder.exists() || file_src_en_folder.exists()) && !file_dst_en_folder.exists() && !file_en_dst_folder.exists()) {
                MLKIT_DICTIONARY.READY = false;
                mlkit_status_message = "Dictionary is not ready";
            }
            else if ((file_en_dst_folder.exists() || file_dst_en_folder.exists()) && !file_src_en_folder.exists() && !file_en_src_folder.exists()) {
                MLKIT_DICTIONARY.READY = false;
                mlkit_status_message = "Dictionary is not ready";
            }
            else if (!file_en_src_folder.exists() && !file_en_dst_folder.exists() && !file_src_en_folder.exists() && !file_dst_en_folder.exists()) {
                MLKIT_DICTIONARY.READY = false;
                mlkit_status_message = "Dictionary is not ready";
            }
        }
        textview_debug2.setText(mlkit_status_message);
    }

    private void toast(String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
