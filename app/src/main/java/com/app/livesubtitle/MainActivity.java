package com.app.livesubtitle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.service.notification.NotificationListenerService;

public class MainActivity extends AppCompatActivity {

    private CheckBox checkbox_debug_mode;
    private Spinner spinner_src_languages;
    private TextView textview_src_dialect;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_src;
    private Spinner spinner_dst_languages;
    private TextView textview_dst_dialect;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_dst;
    @SuppressLint("StaticFieldLeak")
    public static CheckBox checkbox_offline_mode;
    @SuppressLint("StaticFieldLeak")
    public static Button button_download_language;
    @SuppressLint("StaticFieldLeak")
    public static EditText full_voice_text;
    @SuppressLint("StaticFieldLeak")
    public static EditText voice_text;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_recognizing;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_overlaying;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_debug;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_output_messages;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_mlkit_status;

    private ArrayList<String> arraylist_languages;
    private String [] countries;
    private String [] dialects;
    private Map<String, String> countries_dialects;

    public static AudioManager audio;
    public static int mStreamVolume;

    private DisplayMetrics display;

    private Translator translator;
    private TranslatorOptions options;
    private DownloadConditions conditions;

    private String string_en_src_folder;
    private String string_en_dst_folder;
    private String string_src_en_folder;
    private String string_dst_en_folder;
    private File file_en_src_folder;
    private File file_en_dst_folder;
    private File file_src_en_folder;
    private File file_dst_en_folder;
    private String mlkit_status_message = "";

    private final String GOOGLE_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox";
    private final Intent ri = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable checkConnection = new Runnable() {
        @Override
        public void run() {
            if (!MyNotificationListenerService.isConnected) {
                Log.w("MainActivity", "Listener belum connected, forceRebind()");
                MyNotificationListenerService.forceRebind(MainActivity.this);
            }
        }
    };

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
        checkbox_debug_mode = findViewById(R.id.checkbox_debug_mode);
        spinner_src_languages = findViewById(R.id.spinner_src_languages);
        checkbox_offline_mode = findViewById(R.id.checkbox_offline_mode);
        button_download_language =  findViewById(R.id.button_download_language);
        button_download_language.setVisibility(View.GONE);
        spinner_dst_languages = findViewById(R.id.spinner_dst_languages);
        Button button_toggle_overlay = findViewById(R.id.button_toggle_overlay);
        textview_src_dialect = findViewById(R.id.textview_src_dialect);
        textview_src = findViewById(R.id.textview_src);
        textview_dst_dialect = findViewById(R.id.textview_dst_dialect);
        textview_dst = findViewById(R.id.textview_dst);
        voice_text = findViewById(R.id.voice_text);
        full_voice_text = findViewById(R.id.full_voice_text);
        textview_recognizing = findViewById(R.id.textview_recognizing);
        textview_overlaying = findViewById(R.id.textview_overlaying);
        textview_debug = findViewById(R.id.textview_debug);
        textview_output_messages = findViewById(R.id.textview_output_messages);
        textview_mlkit_status = findViewById(R.id.textview_mlkit_status);

        VOICE_TEXT.STRING = "";
        TRANSLATION_TEXT.STRING = "";
        PREFER_OFFLINE_STATUS.OFFLINE = checkbox_offline_mode.isChecked();

        checkbox_offline_mode.setOnClickListener(view -> {
            if (((CompoundButton) view).isChecked()) {
                button_download_language.setVisibility(View.VISIBLE);
            } else {
                button_download_language.setVisibility(View.GONE);
            }
        });

        audio = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mStreamVolume = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION);
        audio.setSpeakerphoneOn(true);

        display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(display);
        float d = display.density;
        DISPLAY_METRIC.DISPLAY_WIDTH = display.widthPixels;
        DISPLAY_METRIC.DISPLAY_HEIGHT = display.heightPixels;
        DISPLAY_METRIC.DISPLAY_DENSITY = d;

        RECOGNIZING_STATUS.IS_RECOGNIZING = false;
        RECOGNIZING_STATUS.STRING = "RECOGNIZING_STATUS.IS_RECOGNIZING = " + RECOGNIZING_STATUS.IS_RECOGNIZING;
        setText(textview_recognizing, RECOGNIZING_STATUS.STRING);
        MIC_BUTTON_STATUS.IS_OVERLAYING = false;
        MIC_BUTTON_STATUS.STRING = "MIC_BUTTON_STATUS.IS_OVERLAYING = " + MIC_BUTTON_STATUS.IS_OVERLAYING;
        setText(textview_overlaying, MIC_BUTTON_STATUS.STRING);

        int h;
        if (Objects.equals(LANGUAGE.DST, "ja") || Objects.equals(LANGUAGE.DST, "zh")) {
            h = 75;
        }
        else {
            h = 62;
        }
        voice_text.setHeight((int) (h * getResources().getDisplayMetrics().density));
        full_voice_text.setHeight((int) (h * getResources().getDisplayMetrics().density));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            getSupportActionBar().setCustomView(R.layout.actionbar_layout);
        }

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        i.setData(Uri.parse("package:" + getPackageName()));
        startActivity(i);

        checkbox_debug_mode.setOnClickListener(view -> {
            if(((CompoundButton) view).isChecked()){
                textview_src_dialect.setVisibility(View.VISIBLE);
                textview_src.setVisibility(View.VISIBLE);
                textview_dst_dialect.setVisibility(View.VISIBLE);
                textview_dst.setVisibility(View.VISIBLE);
                textview_recognizing.setVisibility(View.VISIBLE);
                textview_overlaying.setVisibility(View.VISIBLE);
                textview_debug.setVisibility(View.VISIBLE);
                textview_mlkit_status.setVisibility(View.VISIBLE);
                if (LANGUAGE.SRC_DIALECT != null) {
                    String lsd = "LANGUAGE.SRC_DIALECT = " + LANGUAGE.SRC_DIALECT;
                    textview_src_dialect.setText(lsd);
                }
                else {
                    textview_src_dialect.setHint("LANGUAGE.SRC_DIALECT");
                }

                if (LANGUAGE.SRC != null) {
                    String ls = "LANGUAGE.SRC = " + LANGUAGE.SRC;
                    textview_src.setText(ls);
                }
                else {
                    textview_src.setHint("LANGUAGE.SRC");
                }

                if (LANGUAGE.DST_DIALECT != null) {
                    String ldd = "LANGUAGE.DST_DIALECT = " + LANGUAGE.DST_DIALECT;
                    textview_dst_dialect.setText(ldd);
                }
                else {
                    textview_dst_dialect.setHint("LANGUAGE.DST_DIALECT");
                }

                if (LANGUAGE.DST != null) {
                    String ld = "LANGUAGE.DST = " + LANGUAGE.DST;
                    textview_dst.setText(ld);
                }
                else {
                    textview_src.setHint("LANGUAGE.SRC");
                }
            }
            else {
                textview_src_dialect.setVisibility(View.GONE);
                textview_src.setVisibility(View.GONE);
                textview_dst_dialect.setVisibility(View.GONE);
                textview_dst.setVisibility(View.GONE);
                textview_recognizing.setVisibility(View.GONE);
                textview_overlaying.setVisibility(View.GONE);
                textview_debug.setVisibility(View.GONE);
                textview_mlkit_status.setVisibility(View.GONE);
            }
        });

        if(checkbox_debug_mode.isChecked()){
            textview_src_dialect.setVisibility(View.VISIBLE);
            textview_src.setVisibility(View.VISIBLE);
            textview_dst_dialect.setVisibility(View.VISIBLE);
            textview_dst.setVisibility(View.VISIBLE);
            textview_recognizing.setVisibility(View.VISIBLE);
            textview_overlaying.setVisibility(View.VISIBLE);
            textview_debug.setVisibility(View.VISIBLE);
            textview_mlkit_status.setVisibility(View.VISIBLE);
            if (LANGUAGE.SRC_DIALECT != null) {
                String lsd = "LANGUAGE.SRC_DIALECT = " + LANGUAGE.SRC_DIALECT;
                textview_src_dialect.setText(lsd);
            }
            else {
                textview_src_dialect.setHint("LANGUAGE.SRC_DIALECT");
            }

            if (LANGUAGE.SRC != null) {
                String ls  = "LANGUAGE.SRC = " + LANGUAGE.SRC;
                textview_src.setText(ls);
            }
            else {
                textview_src.setHint("LANGUAGE.SRC");
            }

            if (LANGUAGE.DST_DIALECT != null) {
                String ldd = "LANGUAGE.DST_DIALECT = " + LANGUAGE.DST_DIALECT;
                textview_dst_dialect.setText(ldd);
            }
            else {
                textview_dst_dialect.setHint("LANGUAGE.DST_DIALECT");
            }

            if (LANGUAGE.DST != null) {
                String ld = "LANGUAGE.DST = " + LANGUAGE.DST;
                textview_dst.setText(ld);
            }
            else {
                textview_src.setHint("LANGUAGE.SRC");
            }
        }

        else {
            textview_src_dialect.setVisibility(View.GONE);
            textview_src.setVisibility(View.GONE);
            textview_dst_dialect.setVisibility(View.GONE);
            textview_dst.setVisibility(View.GONE);
            textview_recognizing.setVisibility(View.GONE);
            textview_overlaying.setVisibility(View.GONE);
            textview_debug.setVisibility(View.GONE);
            textview_mlkit_status.setVisibility(View.GONE);
        }

        PackageManager pm = getPackageManager();
        boolean isInstalled = isPackageInstalled(GOOGLE_SEARCH_PACKAGE, pm);
        if (!isInstalled) {
            setText(textview_mlkit_status, "Please install Googple app (com.google.android.googlequicksearchbox)");
        } else {
            ri.setPackage(GOOGLE_SEARCH_PACKAGE);
        }
        ri.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        final Intent intent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        if (isInstalled) intent.setPackage("com.google.android.googlequicksearchbox");

        button_download_language.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                Intent intentDownload = new Intent();
                intentDownload.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                try {
                    // KODE YANG BENAR UNTUK HALAMAN PENGUNDUHAN BAHASA OFFLINE
                    intentDownload.setComponent(new ComponentName(
                            "com.google.android.googlequicksearchbox",
                            "com.google.android.voicesearch.greco3.languagepack.InstallActivity"
                    ));
                    startActivity(intentDownload);
                } catch (Exception exception) {
                    try {
                        // Cadangan alternatif jika struktur update Google App mendeteksi activity berbeda
                        intentDownload.setClassName("com.google.android.googlequicksearchbox",
                                "com.google.android.voicesearch.writersync.LanguagePackDownloadActivity");
                        startActivity(intentDownload);
                    } catch (Exception ex) {
                        Toast.makeText(this, "Failed to open automatic download menu.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            return false;
        });

        this.sendOrderedBroadcast(ri, null, new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Bundle extra = getResultExtras(false);
                        if (extra == null || !extra.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
                            extra = intent.getExtras();
                        }

                        Log.d("MainActivity", "extra = " + extra);
                        if (extra != null && extra.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
                            arraylist_languages = extra.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);

                            if (arraylist_languages != null && !arraylist_languages.isEmpty()) {
                                dialects = arraylist_languages.toArray(new String[0]);
                                for (int i = 0; i < dialects.length; i++) {
                                    dialects[i] = dialects[i].trim();
                                }

                                ArrayList<String> displayLanguages = new ArrayList<>();
                                for (int i = 0; i < arraylist_languages.size(); i++) {
                                    String rawTag = arraylist_languages.get(i);
                                    Locale locale = Locale.forLanguageTag(rawTag.replace("_", "-"));

                                    /*
                                    String displayName = locale.getDisplayName().trim();
                                    String displayNameLower = displayName.toLowerCase();
                                    // Detection based on phone's latest logcat or its dialer code
                                    if (rawTag.startsWith("cmn-Hans-CN") || rawTag.startsWith("zh-CN") || displayName.contains("普通话 (中国大陆)")) {
                                        displayName = "Chinese Simplified (China)";
                                    }
                                    else if (rawTag.startsWith("cmn-Hant-TW") || rawTag.startsWith("zh-TW") || displayName.contains("國語 (台灣)")) {
                                        displayName = "Chinese Traditional (Taiwan)";
                                    }
                                    else if (rawTag.startsWith("cmn-Hans-HK") || displayName.contains("普通話 (香港)")) {
                                        displayName = "Chinese Simplified (Hong Kong)";
                                    }
                                    else if (rawTag.startsWith("yue") || displayName.contains("廣東話 (香港)")) {
                                        displayName = "Chinese Cantonese (Hong Kong)";
                                    }
                                    // THERE CAN ONLY BE ONE ADD HERE SO THAT THE DATA SIZE IS BALANCED
                                    displayLanguages.add(displayName);
                                    */

                                    displayLanguages.add(friendlyName(arraylist_languages.get(i)));

                                }

                                countries = displayLanguages.toArray(new String[0]);
                                for (int i = 0; i < countries.length; i++) {
                                    countries[i] = countries[i].trim();
                                }

                                setup_spinner(displayLanguages);

                            } else {
                                loadLocaleLanguages();
                            }

                        } else {
                            loadLocaleLanguages();
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
                String src_country = spinner_src_languages.getSelectedItem().toString();
                LANGUAGE.SRC_DIALECT = countries_dialects.get(src_country);
                if (LANGUAGE.SRC_DIALECT != null) {
                    //LANGUAGE.SRC = LANGUAGE.SRC_DIALECT.split("-")[0];
                    //switch (LANGUAGE.SRC_DIALECT) {
                    //    case "yue-Hant-HK":
                    //    case "cmn-Hant-TW":
                    //        LANGUAGE.SRC = "zh-TW";
                    //        break;
                    //    case "cmn-Hans-CN":
                    //    case "cmn-Hans-HK":
                    //        LANGUAGE.SRC = "zh-CN";
                    //        break;
                    //}
                    LANGUAGE.SRC = toMlKitCode(LANGUAGE.SRC_DIALECT);  // dan LANGUAGE.DST = toMlKitCode(LANGUAGE.DST_DIALECT)
                }

                setText(textview_src_dialect, LANGUAGE.SRC_DIALECT);
                setText(textview_src, LANGUAGE.SRC);

                String dst_country = spinner_dst_languages.getSelectedItem().toString();
                LANGUAGE.DST_DIALECT = countries_dialects.get(dst_country);
                if (LANGUAGE.DST_DIALECT != null) {
                    //LANGUAGE.DST = LANGUAGE.DST_DIALECT.split("-")[0];
                    //switch (LANGUAGE.DST_DIALECT) {
                    //    case "yue-Hant-HK":
                    //    case "cmn-Hant-TW":
                    //        LANGUAGE.DST = "zh-TW";
                    //        break;
                    //    case "cmn-Hans-CN":
                    //    case "cmn-Hans-HK":
                    //        LANGUAGE.DST = "zh-CN";
                    //        break;
                    //}
                    LANGUAGE.DST = toMlKitCode(LANGUAGE.DST_DIALECT);
                }

                setText(textview_dst_dialect, LANGUAGE.DST_DIALECT);
                setText(textview_dst, LANGUAGE.DST);

                int h;
                if (Objects.equals(LANGUAGE.DST, "ja") || Objects.equals(LANGUAGE.DST, "zh")) {
                    h = 75;
                }
                else {
                    h = 62;
                }
                voice_text.setHeight((int) (h * getResources().getDisplayMetrics().density));
                full_voice_text.setHeight((int) (h * getResources().getDisplayMetrics().density));

                stop_voice_recognizer();
                stop_create_overlay_translation_text();
                stop_create_overlay_mic_button();

                if (MIC_BUTTON_STATUS.IS_OVERLAYING) {
                    if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                        if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setImageResource(R.drawable.ic_mic_black_off);
                    } else {
                        start_voice_recognizer();
                        if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setImageResource(R.drawable.ic_mic_black_on);
                    }
                    start_create_overlay_mic_button();
                    if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setBackgroundColor(Color.parseColor("#80000000"));

                    start_create_overlay_translation_text();
                }

                RECOGNIZING_STATUS.STRING = "RECOGNIZING_STATUS.IS_RECOGNIZING = " + RECOGNIZING_STATUS.IS_RECOGNIZING;
                setText(textview_recognizing, RECOGNIZING_STATUS.STRING);
                MIC_BUTTON_STATUS.STRING =  "MIC_BUTTON_STATUS.IS_OVERLAYING = " + MIC_BUTTON_STATUS.IS_OVERLAYING;
                textview_overlaying.setText(MIC_BUTTON_STATUS.STRING);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                String src_country = spinner_src_languages.getSelectedItem().toString();
                LANGUAGE.SRC_DIALECT = countries_dialects.get(src_country);
                if (LANGUAGE.SRC_DIALECT != null) {
                    //LANGUAGE.SRC = LANGUAGE.SRC_DIALECT.split("-")[0];
                    //switch (LANGUAGE.SRC_DIALECT) {
                    //    case "yue-Hant-HK":
                    //    case "cmn-Hant-TW":
                    //        LANGUAGE.SRC = "zh-TW";
                    //        break;
                    //    case "cmn-Hans-CN":
                    //    case "cmn-Hans-HK":
                    //        LANGUAGE.SRC = "zh-CN";
                    //        break;
                    //}
                    LANGUAGE.SRC = toMlKitCode(LANGUAGE.SRC_DIALECT);
                }
                setText(textview_src_dialect, LANGUAGE.SRC_DIALECT);
                setText(textview_src, LANGUAGE.SRC);

                String dst_country = spinner_dst_languages.getSelectedItem().toString();
                LANGUAGE.DST_DIALECT = countries_dialects.get(dst_country);
                if (LANGUAGE.DST_DIALECT != null) {
                    //LANGUAGE.DST = LANGUAGE.DST_DIALECT.split("-")[0];
                    //switch (LANGUAGE.DST_DIALECT) {
                    //    case "yue-Hant-HK":
                    //    case "cmn-Hant-TW":
                    //        LANGUAGE.DST = "zh-TW";
                    //        break;
                    //    case "cmn-Hans-CN":
                    //    case "cmn-Hans-HK":
                    //        LANGUAGE.DST = "zh-CN";
                    //        break;
                    //}
                    LANGUAGE.DST = toMlKitCode(LANGUAGE.DST_DIALECT);
                }
                setText(textview_dst_dialect, LANGUAGE.DST_DIALECT);
                setText(textview_dst, LANGUAGE.DST);
            }
        });

        spinner_dst_languages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String src_country = spinner_src_languages.getSelectedItem().toString();
                LANGUAGE.SRC_DIALECT = countries_dialects.get(src_country);
                if (LANGUAGE.SRC_DIALECT != null) {
                    //LANGUAGE.SRC = LANGUAGE.SRC_DIALECT.split("-")[0];
                    //switch (LANGUAGE.SRC_DIALECT) {
                    //    case "yue-Hant-HK":
                    //    case "cmn-Hant-TW":
                    //        LANGUAGE.SRC = "zh-TW";
                    //        break;
                    //    case "cmn-Hans-CN":
                    //    case "cmn-Hans-HK":
                    //        LANGUAGE.SRC = "zh-CN";
                    //        break;
                    //}
                    LANGUAGE.SRC = toMlKitCode(LANGUAGE.SRC_DIALECT);
                }
                setText(textview_src_dialect, LANGUAGE.SRC_DIALECT);
                setText(textview_src, LANGUAGE.SRC);

                String dst_country = spinner_dst_languages.getSelectedItem().toString();
                LANGUAGE.DST_DIALECT = countries_dialects.get(dst_country);
                if (LANGUAGE.DST_DIALECT != null) {
                    //LANGUAGE.DST = LANGUAGE.DST_DIALECT.split("-")[0];
                    //switch (LANGUAGE.DST_DIALECT) {
                    //    case "yue-Hant-HK":
                    //    case "cmn-Hant-TW":
                    //        LANGUAGE.DST = "zh-TW";
                    //        break;
                    //    case "cmn-Hans-CN":
                    //    case "cmn-Hans-HK":
                    //        LANGUAGE.DST = "zh-CN";
                    //        break;
                    //}
                    LANGUAGE.DST = toMlKitCode(LANGUAGE.DST_DIALECT);
                }
                setText(textview_dst_dialect, LANGUAGE.DST_DIALECT);
                setText(textview_dst, LANGUAGE.DST);

                int h;
                if (Objects.equals(LANGUAGE.DST, "ja") || Objects.equals(LANGUAGE.DST, "zh")) {
                    h = 75;
                }
                else {
                    h = 62;
                }
                voice_text.setHeight((int) (h * getResources().getDisplayMetrics().density));
                full_voice_text.setHeight((int) (h * getResources().getDisplayMetrics().density));

                stop_voice_recognizer();
                stop_create_overlay_translation_text();
                stop_create_overlay_mic_button();
                if (MIC_BUTTON_STATUS.IS_OVERLAYING) {
                    if (!RECOGNIZING_STATUS.IS_RECOGNIZING) {
                        if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setImageResource(R.drawable.ic_mic_black_off);
                    } else {
                        start_voice_recognizer();
                        if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setImageResource(R.drawable.ic_mic_black_on);
                    }
                    start_create_overlay_mic_button();
                    if (create_overlay_mic_button.mic_button != null) create_overlay_mic_button.mic_button.setBackgroundColor(Color.parseColor("#80000000"));

                    start_create_overlay_translation_text();
                }
                RECOGNIZING_STATUS.STRING = "RECOGNIZING_STATUS.IS_RECOGNIZING = " + RECOGNIZING_STATUS.IS_RECOGNIZING;
                setText(textview_recognizing, RECOGNIZING_STATUS.STRING);
                MIC_BUTTON_STATUS.STRING =  "MIC_BUTTON_STATUS.IS_OVERLAYING = " + MIC_BUTTON_STATUS.IS_OVERLAYING;
                textview_overlaying.setText(MIC_BUTTON_STATUS.STRING);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                String src_country = spinner_src_languages.getSelectedItem().toString();
                LANGUAGE.SRC_DIALECT = countries_dialects.get(src_country);
                if (LANGUAGE.SRC_DIALECT != null) {
                    LANGUAGE.SRC = LANGUAGE.SRC_DIALECT.split("-")[0];
                    switch (LANGUAGE.SRC_DIALECT) {
                        case "yue-Hant-HK":
                        case "cmn-Hant-TW":
                            LANGUAGE.SRC = "zh-TW";
                            break;
                        case "cmn-Hans-CN":
                        case "cmn-Hans-HK":
                            LANGUAGE.SRC = "zh-CN";
                            break;
                    }
                }
                setText(textview_src_dialect, LANGUAGE.SRC_DIALECT);
                setText(textview_src, LANGUAGE.SRC);

                String dst_country = spinner_dst_languages.getSelectedItem().toString();
                LANGUAGE.DST_DIALECT = countries_dialects.get(dst_country);
                if (LANGUAGE.DST_DIALECT != null) {
                    LANGUAGE.DST = LANGUAGE.DST_DIALECT.split("-")[0];
                    switch (LANGUAGE.DST_DIALECT) {
                        case "yue-Hant-HK":
                        case "cmn-Hant-TW":
                            LANGUAGE.DST = "zh-TW";
                            break;
                        case "cmn-Hans-CN":
                        case "cmn-Hans-HK":
                            LANGUAGE.DST = "zh-CN";
                            break;
                    }
                }
                setText(textview_dst_dialect, LANGUAGE.DST_DIALECT);
                setText(textview_dst, LANGUAGE.DST);
            }
        });

        button_toggle_overlay.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                MIC_BUTTON_STATUS.IS_OVERLAYING = !MIC_BUTTON_STATUS.IS_OVERLAYING;
                MIC_BUTTON_STATUS.STRING = "MIC_BUTTON_STATUS.IS_OVERLAYING = " + MIC_BUTTON_STATUS.IS_OVERLAYING;
                setText(textview_overlaying, MIC_BUTTON_STATUS.STRING);
                if (MIC_BUTTON_STATUS.IS_OVERLAYING) {
                    if (Settings.canDrawOverlays(getApplicationContext())) {
                        start_create_overlay_mic_button();
                        start_create_overlay_translation_text();
                    }
                    else {
                        Handler handler = new Handler(Looper.getMainLooper());
                        ExecutorService executorService = Executors.newSingleThreadExecutor();
                        Runnable runnable = () -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                        executorService.execute(runnable);
                        handler.postDelayed(() -> {
                            if (Settings.canDrawOverlays(getApplicationContext())) {
                                start_create_overlay_mic_button();
                                start_create_overlay_translation_text();
                                MIC_BUTTON_STATUS.IS_OVERLAYING = true;
                                String os = "Overlay permission granted";
                                setText(textview_output_messages, os);
                            }
                            else {
                                MIC_BUTTON_STATUS.IS_OVERLAYING = false;
                                String os = "Please retry to tap TOGGLE OVERLAY button again";
                                setText(textview_output_messages, os);
                            }
                            MIC_BUTTON_STATUS.STRING = "MIC_BUTTON_STATUS.IS_OVERLAYING = " + MIC_BUTTON_STATUS.IS_OVERLAYING;
                            setText(textview_overlaying, MIC_BUTTON_STATUS.STRING);
                        }, 15000);
                    }
                } else {
                    stop_voice_recognizer();
                    stop_create_overlay_translation_text();
                    stop_create_overlay_mic_button();
                    RECOGNIZING_STATUS.IS_RECOGNIZING = false;
                    RECOGNIZING_STATUS.STRING = "RECOGNIZING_STATUS.IS_RECOGNIZING = " + RECOGNIZING_STATUS.IS_RECOGNIZING;
                    setText(textview_recognizing, RECOGNIZING_STATUS.STRING);
                    MIC_BUTTON_STATUS.STRING = "MIC_BUTTON_STATUS.IS_OVERLAYING = " + MIC_BUTTON_STATUS.IS_OVERLAYING;
                    setText(textview_overlaying, MIC_BUTTON_STATUS.STRING);
                    setText(textview_output_messages, "");
                    VOICE_TEXT.STRING = "";
                    TRANSLATION_TEXT.STRING = "";
                    voice_text.setText("");
                    full_voice_text.setText("");
                    voice_text.setHint("Recognized words");
                    full_voice_text.setHint("Recognized sentences");
                    audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (int)Double.parseDouble(String.valueOf((long)(audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) / 2))), 0);
                    if (create_overlay_translation_text.overlay_translation_text != null) {
                        setText(create_overlay_translation_text.overlay_translation_text, "");
                        create_overlay_translation_text.overlay_translation_text.setVisibility(View.INVISIBLE);
                        create_overlay_translation_text.overlay_translation_text_container.setVisibility(View.INVISIBLE);
                    }
                    if (create_overlay_translation_text.overlay_full_translation_text != null) {
                        setText(create_overlay_translation_text.overlay_full_translation_text, "");
                        create_overlay_translation_text.overlay_full_translation_text.setVisibility(View.INVISIBLE);
                        create_overlay_translation_text.overlay_full_translation_text_container.setVisibility(View.INVISIBLE);
                    }
                    if (create_overlay_mic_button.mic_button != null) {
                        create_overlay_mic_button.mic_button.setVisibility(View.INVISIBLE);
                    }
                    setText(textview_output_messages, "");
                }
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stop_create_overlay_translation_text();
        stop_create_overlay_mic_button();
        stop_voice_recognizer();
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mStreamVolume, AudioManager.ADJUST_SAME);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop_create_overlay_translation_text();
        stop_create_overlay_mic_button();
        stop_voice_recognizer();
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mStreamVolume, AudioManager.ADJUST_SAME);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //if (isNotificationListenerEnabled(this)) {
        //    toggleNotificationListenerService(this); // atau requestRebind(this)
        //} else {
        //    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        //}
        //return;
        if (!MyNotificationListenerService.isEnabled(this)) {
            // Arahkan user ke Settings lewat tombol/dialog, jangan otomatis tiap onResume
            Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(i);

        }
        // Beri waktu 1,5 detik agar sistem sempat bind sendiri; jika belum, paksa
        handler.removeCallbacks(checkConnection);
        handler.postDelayed(checkConnection, 1500);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(checkConnection);
        super.onPause();
    }

    /*
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
    */

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
        if (Objects.equals(LANGUAGE.SRC, LANGUAGE.DST)) {
            MLKIT_DICTIONARY.READY = true;
            mlkit_status_message = "";
        }
        if (Objects.equals(LANGUAGE.SRC, "en")) {
            if (file_en_dst_folder.exists() || file_dst_en_folder.exists()) {
                MLKIT_DICTIONARY.READY = true;
                mlkit_status_message = "MLKIT dictionary is ready";
            } else {
                MLKIT_DICTIONARY.READY = false;
                mlkit_status_message = "MLKIT dictionary is not ready";
            }
        }
        if (Objects.equals(LANGUAGE.DST, "en")) {
            if (file_en_src_folder.exists() || file_src_en_folder.exists()) {
                MLKIT_DICTIONARY.READY = true;
                mlkit_status_message = "MLKIT dictionary is ready";
            } else {
                MLKIT_DICTIONARY.READY = false;
                mlkit_status_message = "MLKIT dictionary is not ready";
            }
        }
        if (!(Objects.equals(LANGUAGE.SRC, "en")) && !(Objects.equals(LANGUAGE.DST, "en"))) {
            if ((file_en_src_folder.exists() || file_src_en_folder.exists()) && (file_en_dst_folder.exists()) || file_dst_en_folder.exists()) {
                MLKIT_DICTIONARY.READY = true;
                mlkit_status_message = "MLKIT dictionary is ready";
            }
            else if ((file_en_src_folder.exists() || file_src_en_folder.exists()) && !file_dst_en_folder.exists() && !file_en_dst_folder.exists()) {
                MLKIT_DICTIONARY.READY = false;
                mlkit_status_message = "MLKIT dictionary is not ready";
            }
            else if ((file_en_dst_folder.exists() || file_dst_en_folder.exists()) && !file_src_en_folder.exists() && !file_en_src_folder.exists()) {
                MLKIT_DICTIONARY.READY = false;
                mlkit_status_message = "MLKIT dictionary is not ready";
            }
            else if (!file_en_src_folder.exists() && !file_en_dst_folder.exists() && !file_src_en_folder.exists() && !file_dst_en_folder.exists()) {
                MLKIT_DICTIONARY.READY = false;
                mlkit_status_message = "MLKIT dictionary is not ready";
            }
        }

        if (!MLKIT_DICTIONARY.READY) {
            mlkit_status_message = "Downloading MLKIT dictionary, please be patient";
            setText(MainActivity.textview_output_messages, mlkit_status_message);

            translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener(unused -> {
                        MLKIT_DICTIONARY.READY = true;
                        String msg = "MLKIT dictionary download completed";
                        setText(MainActivity.textview_output_messages, msg);
                        mlkit_status_message = "MLKIT dictionary is ready";
                        setText(MainActivity.textview_mlkit_status, mlkit_status_message);
                    })
                    .addOnFailureListener(e -> {});
            if (translator != null) translator.close();
        }
        else {
            new Handler(Looper.getMainLooper()).post(() -> {
                mlkit_status_message = "MLKIT dictionary is ready";
                setText(textview_mlkit_status, mlkit_status_message);
                setText(textview_mlkit_status, "");
                if (translator != null) translator.close();
            });
        }

    }

    public void setText(final TextView tv, final String text){
        new Handler(Looper.getMainLooper()).post(() -> {
                // Any UI task, example
                tv.setText(text);
        });
    }

    /*private void checkRecordAudioPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
        }
    }*/

    /*private void checkDrawOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(myIntent);
        }
    }*/

    /*private void toast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }*/

    private int getSpinnerIndexByCode(ArrayList<String> supportedLanguages, String targetCode) {
        if (targetCode == null || targetCode.isEmpty()) return 0;

        if (targetCode.equalsIgnoreCase("zh-CN")) {
            targetCode = "cmn-Hans-CN";
        }

        for (int i = 0; i < supportedLanguages.size(); i++) {
            String displayName = supportedLanguages.get(i);
            String dialectCode = countries_dialects.get(displayName); // Menghasilkan "id-ID", "id", atau "en-US"

            if (dialectCode != null) {
                // Memotong "id-ID" menjadi "id" saja agar cocok dengan input parameter Anda
                String languageCode = dialectCode.split("-")[0];
                if (languageCode.equalsIgnoreCase(targetCode)) {
                    return i;
                }
            }
        }
        return 0;
    }

    public void setup_spinner_bak(ArrayList<String> supported_languages) {
        countries_dialects = new HashMap<>();
        for (int i = 0; i < supported_languages.size(); i++) {
            countries_dialects.put(supported_languages.get(i), dialects[i]);
        }

        Collections.sort(supported_languages);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_textview_align, supported_languages);
        adapter.setDropDownViewResource(R.layout.spinner_textview_align);

        spinner_src_languages.setAdapter(adapter);
        spinner_dst_languages.setAdapter(adapter);

        spinner_src_languages.setSelection(getSpinnerIndexByCode(supported_languages, "id"));
        spinner_dst_languages.setSelection(getSpinnerIndexByCode(supported_languages, "en"));
    }

    private void loadLocaleLanguages_bak() {
        ArrayList<String> localLanguages = new ArrayList<>();
        ArrayList<String> tempDialects = new ArrayList<>();
        ArrayList<String> tempCountries = new ArrayList<>();

        // Array Anda yang sudah ada (biarkan isinya tetap sama)
        String[][] srcLanguages = {
                {"Afrikaans", "af-ZA"},
                {"Albanian", "sq-AL"},
                {"Amharic", "am-ET"},
                {"Arabic", "ar-SA"},
                {"Armenian", "hy-AM"},
                {"Assamese", "as-IN"},
                {"Azerbaijani", "az-AZ"},
                {"Basque", "eu-ES"},
                {"Belarusian", "be-BY"},
                {"Bengali", "bn-BD"},
                {"Bosnian", "bs-BA"},
                {"Bulgarian", "bg-BG"},
                {"Catalan", "ca-ES"},
                {"Cebuano", "ceb"},
                {"Chinese Simplified (China)", "cmn-Hans-CN"},
                {"Chinese Traditional (Taiwan)", "cmn-Hant-TW"},
                {"Chinese Cantonese (Hong Kong)", "yue-Hant-HK"},
                {"Croatian", "hr-HR"},
                {"Czech", "cs-CZ"},
                {"Danish", "da-DK"},
                {"Dutch", "nl-NL"},
                {"English", "en-US"},
                {"Estonian", "et-EE"},
                {"Filipino", "fil-PH"},
                {"Finnish", "fi-FI"},
                {"French", "fr-FR"},
                {"Galician", "gl-ES"},
                {"Georgian", "ka-GE"},
                {"German", "de-DE"},
                {"Greek", "el-GR"},
                {"Gujarati", "gu-IN"},
                {"Hebrew", "he-IL"},
                {"Hindi", "hi-IN"},
                {"Hungarian", "hu-HU"},
                {"Icelandic", "is-IS"},
                {"Indonesian", "id-ID"},
                {"Irish", "ga-IE"},
                {"Italian", "it-IT"},
                {"Japanese", "ja-JP"},
                {"Javanese", "jv-ID"},
                {"Kannada", "kn-IN"},
                {"Kazakh", "kk-KZ"},
                {"Khmer", "km-KH"},
                {"Korean", "ko-KR"},
                {"Kyrgyz", "ky-KG"},
                {"Lao", "lo-LA"},
                {"Latvian", "lv-LV"},
                {"Lingala", "ln-CD"},
                {"Lithuanian", "lt-LT"},
                {"Malay", "ms-MY"},
                {"Malayalam", "ml-IN"},
                {"Maltese", "mt-MT"},
                {"Marathi", "mr-IN"},
                {"Macedonian", "mk-MK"},
                {"Mongolian", "mn-MN"},
                {"Nepali", "ne-NP"},
                {"Norwegian Bokmål", "nb-NO"},
                {"Persian", "fa-IR"},
                {"Polish", "pl-PL"},
                {"Portuguese", "pt-BR"},
                {"Punjabi", "pa-IN"},
                {"Romanian", "ro-RO"},
                {"Russian", "ru-RU"},
                {"Serbian", "sr-RS"},
                {"Sinhala", "si-LK"},
                {"Slovak", "sk-SK"},
                {"Slovenian", "sl-SI"},
                {"Spanish", "es-ES"},
                {"Sundanese", "su-ID"},
                {"Swahili", "sw-TZ"},
                {"Swedish", "sv-SE"},
                {"Tamil", "ta-IN"},
                {"Telugu", "te-IN"},
                {"Thai", "th-TH"},
                {"Turkish", "tr-TR"},
                {"Ukrainian", "uk-UA"},
                {"Urdu", "ur-PK"},
                {"Vietnamese", "vi-VN"},
                {"Zulu", "zu-ZA"}
        };

        for (String[] language : srcLanguages) {
            String languageName = language[0];
            String dialect = language[1];

            String formattedDisplay = languageName;

            String[] parts = dialect.split("-");

            if (parts.length >= 2) {
                String countryCode = parts[parts.length - 1];

                Locale locale = new Locale("", countryCode);
                String countryName = locale.getDisplayCountry(Locale.ENGLISH);

                formattedDisplay = languageName + " (" + countryName + ")";
            }

            localLanguages.add(formattedDisplay);
            tempDialects.add(dialect);
            tempCountries.add(languageName);
        }

        dialects = tempDialects.toArray(new String[0]);
        countries = tempCountries.toArray(new String[0]);

        setup_spinner(localLanguages);
    }

    public static boolean isNotificationListenerEnabled(Context context) {
        String flat = Settings.Secure.getString(
                context.getContentResolver(), "enabled_notification_listeners");
        if (flat == null || flat.isEmpty()) return false;

        for (String name : flat.split(":")) {
            ComponentName cn = ComponentName.unflattenFromString(name);
            if (cn != null && cn.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public static void requestRebind(Context context) {
        if (!isNotificationListenerEnabled(context)) return;

        ComponentName cn = new ComponentName(context, MyNotificationListenerService.class);
        // API 24+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationListenerService.requestRebind(cn);
        }
    }

    public static void toggleNotificationListenerService(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName cn = new ComponentName(context, MyNotificationListenerService.class);

        pm.setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    // Nama tampilan dari tag, dipakai di onReceive menggantikan rantai if/else Mandarin
    private String friendlyName(String rawTag) {
        String tag = rawTag.replace('_', '-');
        switch (tag) {
            case "cmn-Hans-CN": case "zh-CN":
                return "Chinese Simplified (China)";
            case "cmn-Hans-HK":
                return "Chinese Simplified (Hong Kong)";
            case "cmn-Hant-TW": case "zh-TW":
                return "Chinese Traditional (Taiwan)";
            case "yue-Hant-HK":
                return "Chinese Cantonese (Hong Kong)";
        }
        return Locale.forLanguageTag(tag).getDisplayName(Locale.ENGLISH).trim();
    }

    // Konversi tag dialek Google -> kode ML Kit (satu tempat saja)
    public static String toMlKitCode(String dialect) {
        if (dialect == null) return null;
        String lang = dialect.split("-")[0].toLowerCase(Locale.ROOT);
        switch (lang) {
            case "cmn": case "yue": case "zh": return "zh";
            case "fil": return "tl";
            case "nb":  return "no";
            default:    return lang;
        }
    }

    private void loadLocaleLanguages() {
        final String[] tags = {
                "af-ZA","sq-AL","am-ET","ar-SA","ar-EG","ar-AE","hy-AM","as-IN","az-AZ",
                "eu-ES","be-BY","bn-BD","bn-IN","bs-BA","bg-BG","ca-ES","ceb",
                "cmn-Hans-CN","cmn-Hant-TW","yue-Hant-HK",
                "hr-HR","cs-CZ","da-DK","nl-NL","nl-BE",
                // English: dialek
                "en-US","en-GB","en-AU","en-CA","en-IN","en-ID","en-IE","en-NZ",
                "en-PH","en-SG","en-ZA","en-NG","en-KE","en-GH","en-HK","en-PK","en-TZ",
                "et-EE","fil-PH","fi-FI",
                "fr-FR","fr-CA","fr-BE","fr-CH",
                "gl-ES","ka-GE",
                "de-DE","de-AT","de-CH",
                "el-GR","gu-IN","he-IL","hi-IN","hu-HU","is-IS","id-ID","ga-IE","it-IT","it-CH",
                "ja-JP","jv-ID","kn-IN","kk-KZ","km-KH","ko-KR","ky-KG","lo-LA","lv-LV","ln-CD",
                "lt-LT","ms-MY","ml-IN","mt-MT","mr-IN","mk-MK","mn-MN","ne-NP","nb-NO","fa-IR",
                "pl-PL","pt-BR","pt-PT","pa-IN","ro-RO","ru-RU","sr-RS","si-LK","sk-SK","sl-SI",
                "es-ES","es-MX","es-US","es-AR","es-CO","es-CL","es-PE","es-VE",
                "su-ID","sw-TZ","sw-KE","sv-SE","ta-IN","ta-LK","ta-SG","ta-MY","te-IN",
                "th-TH","tr-TR","uk-UA","ur-PK","ur-IN","vi-VN","zu-ZA"
        };

        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> tagList = new ArrayList<>();
        for (String tag : tags) {
            names.add(friendlyName(tag));   // "English (Australia)", "Spanish (Mexico)", dst.
            tagList.add(tag);
        }

        dialects = tagList.toArray(new String[0]);
        countries = names.toArray(new String[0]);
        setup_spinner(names);
    }

// Opsional: guard bahasa yang tidak didukung ML Kit
// if (TranslateLanguage.fromLanguageTag(code) == null) { ...tampilkan pesan... }

    private void loadLocaleLanguages_last() {
        final String[][] srcLanguages = {
                {"Afrikaans", "af-ZA"},
                {"Albanian", "sq-AL"},
                {"Amharic", "am-ET"},
                {"Arabic", "ar-SA"},
                {"Armenian", "hy-AM"},
                {"Assamese", "as-IN"},
                {"Azerbaijani", "az-AZ"},
                {"Basque", "eu-ES"},
                {"Belarusian", "be-BY"},
                {"Bengali", "bn-BD"},
                {"Bosnian", "bs-BA"},
                {"Bulgarian", "bg-BG"},
                {"Catalan", "ca-ES"},
                {"Cebuano", "ceb"},
                {"Chinese Simplified (China)", "cmn-Hans-CN"},
                {"Chinese Traditional (Taiwan)", "cmn-Hant-TW"},
                {"Chinese Cantonese (Hong Kong)", "yue-Hant-HK"},
                {"Croatian", "hr-HR"},
                {"Czech", "cs-CZ"},
                {"Danish", "da-DK"},
                {"Dutch", "nl-NL"},
                {"English", "en-US"},
                {"Estonian", "et-EE"},
                {"Filipino", "fil-PH"},
                {"Finnish", "fi-FI"},
                {"French", "fr-FR"},
                {"Galician", "gl-ES"},
                {"Georgian", "ka-GE"},
                {"German", "de-DE"},
                {"Greek", "el-GR"},
                {"Gujarati", "gu-IN"},
                {"Hebrew", "he-IL"},
                {"Hindi", "hi-IN"},
                {"Hungarian", "hu-HU"},
                {"Icelandic", "is-IS"},
                {"Indonesian", "id-ID"},
                {"Irish", "ga-IE"},
                {"Italian", "it-IT"},
                {"Japanese", "ja-JP"},
                {"Javanese", "jv-ID"},
                {"Kannada", "kn-IN"},
                {"Kazakh", "kk-KZ"},
                {"Khmer", "km-KH"},
                {"Korean", "ko-KR"},
                {"Kyrgyz", "ky-KG"},
                {"Lao", "lo-LA"},
                {"Latvian", "lv-LV"},
                {"Lingala", "ln-CD"},
                {"Lithuanian", "lt-LT"},
                {"Malay", "ms-MY"},
                {"Malayalam", "ml-IN"},
                {"Maltese", "mt-MT"},
                {"Marathi", "mr-IN"},
                {"Macedonian", "mk-MK"},
                {"Mongolian", "mn-MN"},
                {"Nepali", "ne-NP"},
                {"Norwegian Bokmål", "nb-NO"},
                {"Persian", "fa-IR"},
                {"Polish", "pl-PL"},
                {"Portuguese", "pt-BR"},
                {"Punjabi", "pa-IN"},
                {"Romanian", "ro-RO"},
                {"Russian", "ru-RU"},
                {"Serbian", "sr-RS"},
                {"Sinhala", "si-LK"},
                {"Slovak", "sk-SK"},
                {"Slovenian", "sl-SI"},
                {"Spanish", "es-ES"},
                {"Sundanese", "su-ID"},
                {"Swahili", "sw-TZ"},
                {"Swedish", "sv-SE"},
                {"Tamil", "ta-IN"},
                {"Telugu", "te-IN"},
                {"Thai", "th-TH"},
                {"Turkish", "tr-TR"},
                {"Ukrainian", "uk-UA"},
                {"Urdu", "ur-PK"},
                {"Vietnamese", "vi-VN"},
                {"Zulu", "zu-ZA"}
        };

        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> tags = new ArrayList<>();

        for (String[] lang : srcLanguages) {
            String name = lang[0];
            String tag = lang[1];

            // Tambahkan negara hanya jika belum ada kurung dan segmen terakhir adalah kode region (2 huruf)
            if (!name.contains("(")) {
                String[] parts = tag.split("-");
                String last = parts[parts.length - 1];
                if (parts.length >= 2 && last.length() == 2) {
                    String country = new Locale.Builder().setRegion(last).build().getDisplayCountry(Locale.ENGLISH);
                    if (!country.isEmpty()) name += " (" + country + ")";
                }
            }
            names.add(name);
            tags.add(tag);
        }

        dialects = tags.toArray(new String[0]);
        countries = names.toArray(new String[0]);
        setup_spinner(names);
    }

    public void setup_spinner(ArrayList<String> supported_languages) {
        Map<String, String> map = new HashMap<>();
        ArrayList<String> names = new ArrayList<>();

        for (int i = 0; i < supported_languages.size() && i < dialects.length; i++) {
            String name = supported_languages.get(i);
            String tag = dialects[i];
            if (map.containsKey(name)) name = name + " [" + tag + "]"; // cegah tabrakan nama
            map.put(name, tag);
            names.add(name);
        }

        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        countries_dialects = map;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_textview_align, names);
        adapter.setDropDownViewResource(R.layout.spinner_textview_align);
        spinner_src_languages.setAdapter(adapter);
        spinner_dst_languages.setAdapter(adapter);

        spinner_src_languages.setSelection(indexOfTag(names, "id-ID", "id"));
        spinner_dst_languages.setSelection(indexOfTag(names, "en-US", "en"));
    }

    // Cocokkan tag persis dulu; kalau tidak ada, pakai entri pertama dengan kode bahasa yang sama
    private int indexOfTag(java.util.List<String> names, String exactTag, String langFallback) {
        int fallback = -1;
        for (int i = 0; i < names.size(); i++) {
            String tag = countries_dialects.get(names.get(i));
            if (tag == null) continue;
            if (tag.equalsIgnoreCase(exactTag)) return i;
            if (fallback < 0 && tag.split("-")[0].equalsIgnoreCase(langFallback)) fallback = i;
        }
        return Math.max(fallback, 0);
    }

}
