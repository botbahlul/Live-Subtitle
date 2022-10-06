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

public class MainActivity extends BaseActivity {

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

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            DISPLAY_METRIC.DISPLAY_WIDTH = 0;
            DISPLAY_METRIC.DISPLAY_HEIGHT = 0;
            DISPLAY_METRIC.DISPLAY_DENSITY = 0;
            getWindowManager().getDefaultDisplay().getMetrics(display);
            float d = display.density;
            DISPLAY_METRIC.DISPLAY_WIDTH = display.widthPixels;
            DISPLAY_METRIC.DISPLAY_HEIGHT = display.heightPixels;
            DISPLAY_METRIC.DISPLAY_DENSITY = d;
            if (OVERLAYING_STATUS.OVERLAYING) {
                stop_create_overlay_mic_button();
                start_create_overlay_mic_button();
                stop_create_overlay_translation_text();
                start_create_overlay_translation_text();
                if (TRANSLATION_TEXT.STRING.length()>0) {
                    create_overlay_translation_text.overlay_translation_text.setText(TRANSLATION_TEXT.STRING);
                }
            }
            if (RECOGNIZING_STATUS.RECOGNIZING) {
                stop_voice_recognizer();
                start_voice_recognizer();
            }
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            DISPLAY_METRIC.DISPLAY_WIDTH = 0;
            DISPLAY_METRIC.DISPLAY_HEIGHT = 0;
            DISPLAY_METRIC.DISPLAY_DENSITY = 0;
            getWindowManager().getDefaultDisplay().getMetrics(display);
            float d = display.density;
            DISPLAY_METRIC.DISPLAY_WIDTH = display.widthPixels;
            DISPLAY_METRIC.DISPLAY_HEIGHT = display.heightPixels;
            DISPLAY_METRIC.DISPLAY_DENSITY = d;
            if (OVERLAYING_STATUS.OVERLAYING) {
                stop_create_overlay_mic_button();
                start_create_overlay_mic_button();
                stop_create_overlay_translation_text();
                start_create_overlay_translation_text();
                if (TRANSLATION_TEXT.STRING.length()>0) {
                    create_overlay_translation_text.overlay_translation_text.setText(TRANSLATION_TEXT.STRING);
                }
            }
            if (RECOGNIZING_STATUS.RECOGNIZING) {
                stop_voice_recognizer();
                start_voice_recognizer();
            }
        }
    }

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
                    checkDrawOverlayPermission();
                    if (OVERLAYING_STATUS.OVERLAYING) {
                        start_create_overlay_mic_button();
                        start_create_overlay_translation_text();
                    } else {
                        VOICE_TEXT.STRING = "";
                        TRANSLATION_TEXT.STRING = "";
                        voice_text.setText("");
                        create_overlay_translation_text.overlay_translation_text.setText("");
                        stop_voice_recognizer();
                        stop_create_overlay_translation_text();
                        stop_create_overlay_mic_button();
                        RECOGNIZING_STATUS.RECOGNIZING = false;
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
        spinner_src_languages.setSelection
                (
                        supported_languages.indexOf
                                (
                                        "Indonesian (Indonesia)"
                                )
                );
        spinner_dst_languages.setAdapter(adapter);
        spinner_dst_languages.setSelection
                (
                        supported_languages.indexOf
                                (
                                        "English (United States)"
                                )
                );
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
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
