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
import android.service.notification.NotificationListenerService;
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
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

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


public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";
    private CheckBox checkbox_debug_mode;
    private Spinner spinner_src_languages;
    private TextView textview_src_dialect;
    @SuppressLint("StaticFieldLeak")
    public static TextView textview_src;
    private Spinner spinner_dst_languages;
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

    ArrayList<String> arraylist_src_languages;
    String [] src_countries;
    String [] src_dialects;
    Map<String, String> src_countries_dialects;

    ArrayList<String> arraylist_dst_languages = new ArrayList<>();
    ArrayList<String> arraylist_dst_code = new ArrayList<>();
    Map<String, String> map_dst_country = new HashMap<>();

    public static AudioManager audio;
    public static int mStreamVolume;

    private DisplayMetrics display;
    //DON'T FORGET TO MODIFY AndroidManifest.xml
    //         <activity
    //            android:name=".MainActivity"
    //            android:configChanges="keyboardHidden|screenSize|orientation|screenLayout|navigation"

    private final String GOOGLE_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox";
    private final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);

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

    public boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(pkgName);
    }

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

    //DON'T FORGET TO MODIFY AndroidManifest.xml
    //         <activity
    //            android:name=".MainActivity"
    //            android:configChanges="keyboardHidden|screenSize|orientation|screenLayout|navigation"

    @SuppressLint({"ClickableViewAccessibility", "QueryPermissionsNeeded", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkbox_debug_mode = findViewById(R.id.checkbox_debug_mode);
        spinner_src_languages = findViewById(R.id.spinner_src_languages);
        checkbox_offline_mode = findViewById(R.id.checkbox_offline_mode);
        button_download_language =  findViewById(R.id.button_download_language);
        button_download_language.setVisibility(View.GONE);
        spinner_dst_languages = findViewById(R.id.spinner_dst_languages);
        Button button_toggle_overlay = findViewById(R.id.button_toggle_overlay);
        textview_src_dialect = findViewById(R.id.textview_src_dialect);
        textview_src = findViewById(R.id.textview_src);
        textview_dst = findViewById(R.id.textview_dst);
        voice_text = findViewById(R.id.voice_text);
        full_voice_text = findViewById(R.id.full_voice_text);
        textview_recognizing = findViewById(R.id.textview_recognizing);
        textview_overlaying = findViewById(R.id.textview_overlaying);
        textview_debug = findViewById(R.id.textview_debug);
        textview_output_messages = findViewById(R.id.textview_output_messages);
        textview_mlkit_status = findViewById(R.id.textview_mlkit_status);

        arraylist_dst_code.add("af");
        arraylist_dst_code.add("sq");
        arraylist_dst_code.add("am");
        arraylist_dst_code.add("ar");
        arraylist_dst_code.add("hy");
        arraylist_dst_code.add("as");
        arraylist_dst_code.add("ay");
        arraylist_dst_code.add("az");
        arraylist_dst_code.add("bm");
        arraylist_dst_code.add("eu");
        arraylist_dst_code.add("be");
        arraylist_dst_code.add("bn");
        arraylist_dst_code.add("bho");
        arraylist_dst_code.add("bs");
        arraylist_dst_code.add("bg");
        arraylist_dst_code.add("ca");
        arraylist_dst_code.add("ceb");
        arraylist_dst_code.add("ny");
        arraylist_dst_code.add("zh");
        arraylist_dst_code.add("zh-CN");
        arraylist_dst_code.add("zh-TW");
        arraylist_dst_code.add("co");
        arraylist_dst_code.add("hr");
        arraylist_dst_code.add("cs");
        arraylist_dst_code.add("da");
        arraylist_dst_code.add("dv");
        arraylist_dst_code.add("doi");
        arraylist_dst_code.add("nl");
        arraylist_dst_code.add("en");
        arraylist_dst_code.add("eo");
        arraylist_dst_code.add("et");
        arraylist_dst_code.add("ee");
        arraylist_dst_code.add("fil");
        arraylist_dst_code.add("fi");
        arraylist_dst_code.add("fr");
        arraylist_dst_code.add("fy");
        arraylist_dst_code.add("gl");
        arraylist_dst_code.add("ka");
        arraylist_dst_code.add("de");
        arraylist_dst_code.add("el");
        arraylist_dst_code.add("gn");
        arraylist_dst_code.add("gu");
        arraylist_dst_code.add("ht");
        arraylist_dst_code.add("ha");
        arraylist_dst_code.add("haw");
        arraylist_dst_code.add("he");
        arraylist_dst_code.add("hi");
        arraylist_dst_code.add("hmn");
        arraylist_dst_code.add("hu");
        arraylist_dst_code.add("is");
        arraylist_dst_code.add("ig");
        arraylist_dst_code.add("ilo");
        arraylist_dst_code.add("id");
        arraylist_dst_code.add("ga");
        arraylist_dst_code.add("it");
        arraylist_dst_code.add("ja");
        arraylist_dst_code.add("jv");
        arraylist_dst_code.add("kn");
        arraylist_dst_code.add("kk");
        arraylist_dst_code.add("km");
        arraylist_dst_code.add("rw");
        arraylist_dst_code.add("gom");
        arraylist_dst_code.add("ko");
        arraylist_dst_code.add("kri");
        arraylist_dst_code.add("kmr");
        arraylist_dst_code.add("ckb");
        arraylist_dst_code.add("ky");
        arraylist_dst_code.add("lo");
        arraylist_dst_code.add("la");
        arraylist_dst_code.add("lv");
        arraylist_dst_code.add("ln");
        arraylist_dst_code.add("lt");
        arraylist_dst_code.add("lg");
        arraylist_dst_code.add("lb");
        arraylist_dst_code.add("mk");
        arraylist_dst_code.add("mg");
        arraylist_dst_code.add("ms");
        arraylist_dst_code.add("ml");
        arraylist_dst_code.add("mt");
        arraylist_dst_code.add("mi");
        arraylist_dst_code.add("mr");
        arraylist_dst_code.add("mni-Mtei");
        arraylist_dst_code.add("lus");
        arraylist_dst_code.add("mn");
        arraylist_dst_code.add("my");
        arraylist_dst_code.add("ne");
        arraylist_dst_code.add("no");
        arraylist_dst_code.add("or");
        arraylist_dst_code.add("om");
        arraylist_dst_code.add("ps");
        arraylist_dst_code.add("fa");
        arraylist_dst_code.add("pl");
        arraylist_dst_code.add("pt");
        arraylist_dst_code.add("pa");
        arraylist_dst_code.add("qu");
        arraylist_dst_code.add("ro");
        arraylist_dst_code.add("ru");
        arraylist_dst_code.add("sm");
        arraylist_dst_code.add("sa");
        arraylist_dst_code.add("gd");
        arraylist_dst_code.add("nso");
        arraylist_dst_code.add("sr");
        arraylist_dst_code.add("st");
        arraylist_dst_code.add("sn");
        arraylist_dst_code.add("sd");
        arraylist_dst_code.add("si");
        arraylist_dst_code.add("sk");
        arraylist_dst_code.add("sl");
        arraylist_dst_code.add("so");
        arraylist_dst_code.add("es");
        arraylist_dst_code.add("su");
        arraylist_dst_code.add("sw");
        arraylist_dst_code.add("sv");
        arraylist_dst_code.add("tg");
        arraylist_dst_code.add("ta");
        arraylist_dst_code.add("tt");
        arraylist_dst_code.add("te");
        arraylist_dst_code.add("th");
        arraylist_dst_code.add("ti");
        arraylist_dst_code.add("ts");
        arraylist_dst_code.add("tr");
        arraylist_dst_code.add("tk");
        arraylist_dst_code.add("tw");
        arraylist_dst_code.add("uk");
        arraylist_dst_code.add("ur");
        arraylist_dst_code.add("ug");
        arraylist_dst_code.add("uz");
        arraylist_dst_code.add("vi");
        arraylist_dst_code.add("cy");
        arraylist_dst_code.add("xh");
        arraylist_dst_code.add("yi");
        arraylist_dst_code.add("yo");
        arraylist_dst_code.add("zu");

        arraylist_dst_languages.add("Afrikaans");
        arraylist_dst_languages.add("Albanian");
        arraylist_dst_languages.add("Amharic");
        arraylist_dst_languages.add("Arabic");
        arraylist_dst_languages.add("Armenian");
        arraylist_dst_languages.add("Assamese");
        arraylist_dst_languages.add("Aymara");
        arraylist_dst_languages.add("Azerbaijani");
        arraylist_dst_languages.add("Bambara");
        arraylist_dst_languages.add("Basque");
        arraylist_dst_languages.add("Belarusian");
        arraylist_dst_languages.add("Bengali");
        arraylist_dst_languages.add("Bhojpuri");
        arraylist_dst_languages.add("Bosnian");
        arraylist_dst_languages.add("Bulgarian");
        arraylist_dst_languages.add("Catalan");
        arraylist_dst_languages.add("Cebuano");
        arraylist_dst_languages.add("Chichewa");
        arraylist_dst_languages.add("Chinese");
        arraylist_dst_languages.add("Chinese (Simplified)");
        arraylist_dst_languages.add("Chinese (Traditional)");
        arraylist_dst_languages.add("Corsican");
        arraylist_dst_languages.add("Croatian");
        arraylist_dst_languages.add("Czech");
        arraylist_dst_languages.add("Danish");
        arraylist_dst_languages.add("Dhivehi");
        arraylist_dst_languages.add("Dogri");
        arraylist_dst_languages.add("Dutch");
        arraylist_dst_languages.add("English");
        arraylist_dst_languages.add("Esperanto");
        arraylist_dst_languages.add("Estonian");
        arraylist_dst_languages.add("Ewe");
        arraylist_dst_languages.add("Filipino");
        arraylist_dst_languages.add("Finnish");
        arraylist_dst_languages.add("French");
        arraylist_dst_languages.add("Frisian");
        arraylist_dst_languages.add("Galician");
        arraylist_dst_languages.add("Georgian");
        arraylist_dst_languages.add("German");
        arraylist_dst_languages.add("Greek");
        arraylist_dst_languages.add("Guarani");
        arraylist_dst_languages.add("Gujarati");
        arraylist_dst_languages.add("Haitian Creole");
        arraylist_dst_languages.add("Hausa");
        arraylist_dst_languages.add("Hawaiian");
        arraylist_dst_languages.add("Hebrew");
        arraylist_dst_languages.add("Hindi");
        arraylist_dst_languages.add("Hmong");
        arraylist_dst_languages.add("Hungarian");
        arraylist_dst_languages.add("Icelandic");
        arraylist_dst_languages.add("Igbo");
        arraylist_dst_languages.add("Ilocano");
        arraylist_dst_languages.add("Indonesian");
        arraylist_dst_languages.add("Irish");
        arraylist_dst_languages.add("Italian");
        arraylist_dst_languages.add("Japanese");
        arraylist_dst_languages.add("Javanese");
        arraylist_dst_languages.add("Kannada");
        arraylist_dst_languages.add("Kazakh");
        arraylist_dst_languages.add("Khmer");
        arraylist_dst_languages.add("Kinyarwanda");
        arraylist_dst_languages.add("Konkani");
        arraylist_dst_languages.add("Korean");
        arraylist_dst_languages.add("Krio");
        arraylist_dst_languages.add("Kurdish (Kurmanji)");
        arraylist_dst_languages.add("Kurdish (Sorani)");
        arraylist_dst_languages.add("Kyrgyz");
        arraylist_dst_languages.add("Lao");
        arraylist_dst_languages.add("Latin");
        arraylist_dst_languages.add("Latvian");
        arraylist_dst_languages.add("Lingala");
        arraylist_dst_languages.add("Lithuanian");
        arraylist_dst_languages.add("Luganda");
        arraylist_dst_languages.add("Luxembourgish");
        arraylist_dst_languages.add("Macedonian");
        arraylist_dst_languages.add("Malagasy");
        arraylist_dst_languages.add("Malay");
        arraylist_dst_languages.add("Malayalam");
        arraylist_dst_languages.add("Maltese");
        arraylist_dst_languages.add("Maori");
        arraylist_dst_languages.add("Marathi");
        arraylist_dst_languages.add("Meiteilon (Manipuri)");
        arraylist_dst_languages.add("Mizo");
        arraylist_dst_languages.add("Mongolian");
        arraylist_dst_languages.add("Myanmar (Burmese)");
        arraylist_dst_languages.add("Nepali");
        arraylist_dst_languages.add("Norwegian");
        arraylist_dst_languages.add("Odiya (Oriya)");
        arraylist_dst_languages.add("Oromo");
        arraylist_dst_languages.add("Pashto");
        arraylist_dst_languages.add("Persian");
        arraylist_dst_languages.add("Polish");
        arraylist_dst_languages.add("Portuguese");
        arraylist_dst_languages.add("Punjabi");
        arraylist_dst_languages.add("Quechua");
        arraylist_dst_languages.add("Romanian");
        arraylist_dst_languages.add("Russian");
        arraylist_dst_languages.add("Samoan");
        arraylist_dst_languages.add("Sanskrit");
        arraylist_dst_languages.add("Scots Gaelic");
        arraylist_dst_languages.add("Sepedi");
        arraylist_dst_languages.add("Serbian");
        arraylist_dst_languages.add("Sesotho");
        arraylist_dst_languages.add("Shona");
        arraylist_dst_languages.add("Sindhi");
        arraylist_dst_languages.add("Sinhala");
        arraylist_dst_languages.add("Slovak");
        arraylist_dst_languages.add("Slovenian");
        arraylist_dst_languages.add("Somali");
        arraylist_dst_languages.add("Spanish");
        arraylist_dst_languages.add("Sundanese");
        arraylist_dst_languages.add("Swahili");
        arraylist_dst_languages.add("Swedish");
        arraylist_dst_languages.add("Tajik");
        arraylist_dst_languages.add("Tamil");
        arraylist_dst_languages.add("Tatar");
        arraylist_dst_languages.add("Telugu");
        arraylist_dst_languages.add("Thai");
        arraylist_dst_languages.add("Tigrinya");
        arraylist_dst_languages.add("Tsonga");
        arraylist_dst_languages.add("Turkish");
        arraylist_dst_languages.add("Turkmen");
        arraylist_dst_languages.add("Twi (Akan)");
        arraylist_dst_languages.add("Ukrainian");
        arraylist_dst_languages.add("Urdu");
        arraylist_dst_languages.add("Uyghur");
        arraylist_dst_languages.add("Uzbek");
        arraylist_dst_languages.add("Vietnamese");
        arraylist_dst_languages.add("Welsh");
        arraylist_dst_languages.add("Xhosa");
        arraylist_dst_languages.add("Yiddish");
        arraylist_dst_languages.add("Yoruba");
        arraylist_dst_languages.add("Zulu");

        for (int i = 0; i < arraylist_dst_languages.size(); i++) {
            map_dst_country.put(arraylist_dst_languages.get(i), arraylist_dst_code.get(i));
        }

        setup_dst_spinner(arraylist_dst_languages);

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

        //audio = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        // 1. Save the original volume before shutting down (to be restored later)
        //mStreamVolume = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION); //Volume of notification sound when we start SpeechRecognizer
        //setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION); WRONG CODE? OR MUTE RINGTONE VOLUME?
        //setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);         // set phone's ringtone volume to zero

        audio = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audio.setSpeakerphoneOn(true);
        // 1. Save the original volume before shutting down (to be restored later)
        mStreamVolume = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION);  //Volume of notification sound when we start SpeechRecognizer
        //mMusicVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);  //Volume of any media being played when we start SpeechRecognizer
        Log.d(TAG, "onCreate: mStreamVolume = " + mStreamVolume);
        //Log.d(TAG, "onCreate: mMusicVolume = " + mMusicVolume);
        // 2. Mute the audio stream that triggers the "beep" sound.
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
        //audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        // 3. Bind physical volume control buttons to voice calls (optional if using VoIP/Speech)
        //setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

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
        } else {
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

        if (!isNotificationServiceEnabled()) {
            Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(i);
        }

        Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        i.setData(Uri.parse("package:" + getPackageName()));
        startActivity(i);

        checkbox_debug_mode.setOnClickListener(view -> {
            if(((CompoundButton) view).isChecked()){
                textview_src_dialect.setVisibility(View.VISIBLE);
                textview_src.setVisibility(View.VISIBLE);
                textview_dst.setVisibility(View.VISIBLE);
                textview_recognizing.setVisibility(View.VISIBLE);
                textview_overlaying.setVisibility(View.VISIBLE);
                textview_debug.setVisibility(View.VISIBLE);
                textview_mlkit_status.setVisibility(View.VISIBLE);
                if (LANGUAGE.SRC_DIALECT != null) {
                    String lsd = "LANGUAGE.SRC_DIALECT = " + LANGUAGE.SRC_DIALECT;
                    textview_src_dialect.setText(lsd);
                } else {
                    textview_src_dialect.setHint("LANGUAGE.SRC_DIALECT");
                }

                if (LANGUAGE.SRC != null) {
                    String ls = "LANGUAGE.SRC = " + LANGUAGE.SRC;
                    textview_src.setText(ls);
                } else {
                    textview_src.setHint("LANGUAGE.SRC");
                }

                if (LANGUAGE.DST != null) {
                    String ld = "LANGUAGE.DST = " + LANGUAGE.DST;
                    textview_dst.setText(ld);
                } else {
                    textview_src.setHint("LANGUAGE.SRC");
                }
            } else {
                textview_src_dialect.setVisibility(View.GONE);
                textview_src.setVisibility(View.GONE);
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
            textview_dst.setVisibility(View.VISIBLE);
            textview_recognizing.setVisibility(View.VISIBLE);
            textview_overlaying.setVisibility(View.VISIBLE);
            textview_debug.setVisibility(View.VISIBLE);
            textview_mlkit_status.setVisibility(View.VISIBLE);
            if (LANGUAGE.SRC_DIALECT != null) {
                String lsd = "LANGUAGE.SRC_DIALECT = " + LANGUAGE.SRC_DIALECT;
                textview_src_dialect.setText(lsd);
            } else {
                textview_src_dialect.setHint("LANGUAGE.SRC_DIALECT");
            }

            if (LANGUAGE.SRC != null) {
                String ls  = "LANGUAGE.SRC = " + LANGUAGE.SRC;
                textview_src.setText(ls);
            } else {
                textview_src.setHint("LANGUAGE.SRC");
            }

            if (LANGUAGE.DST != null) {
                String ld = "LANGUAGE.DST = " + LANGUAGE.DST;
                textview_dst.setText(ld);
            } else {
                textview_src.setHint("LANGUAGE.SRC");
            }
        } else {
            textview_src_dialect.setVisibility(View.GONE);
            textview_src.setVisibility(View.GONE);
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
            speechRecognizerIntent.setPackage(GOOGLE_SEARCH_PACKAGE);
        }
        speechRecognizerIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        final Intent intent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        if (isInstalled) intent.setPackage("com.google.android.googlequicksearchbox");

        button_download_language.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                Intent intentDownload = new Intent();
                intentDownload.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                try {
                    // CORRECT CODE FOR OFFLINE LANGUAGE DOWNLOAD PAGE
                    intentDownload.setComponent(new ComponentName(
                            "com.google.android.googlequicksearchbox",
                            "com.google.android.voicesearch.greco3.languagepack.InstallActivity"
                    ));
                    startActivity(intentDownload);
                } catch (Exception exception) {
                    try {
                        // Alternative backup if the Google App update structure detects different activity
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

        this.sendOrderedBroadcast(speechRecognizerIntent, null, new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Bundle extra = getResultExtras(false);
                        if (extra == null || !extra.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
                            extra = intent.getExtras();
                        }

                        Log.d("MainActivity", "extra = " + extra);
                        if (extra != null && extra.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
                            arraylist_src_languages = extra.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);

                            if (arraylist_src_languages != null && !arraylist_src_languages.isEmpty()) {
                                src_dialects = arraylist_src_languages.toArray(new String[0]);
                                for (int i = 0; i < src_dialects.length; i++) {
                                    src_dialects[i] = src_dialects[i].trim();
                                }

                                ArrayList<String> displayLanguages = new ArrayList<>();
                                for (int i = 0; i < arraylist_src_languages.size(); i++) {
                                    String rawTag = arraylist_src_languages.get(i);
                                    Locale locale = Locale.forLanguageTag(rawTag.replace("_", "-"));
                                    displayLanguages.add(friendlyName(arraylist_src_languages.get(i)));
                                }

                                src_countries = displayLanguages.toArray(new String[0]);
                                for (int i = 0; i < src_countries.length; i++) {
                                    src_countries[i] = src_countries[i].trim();
                                }
                                setup_src_spinner(displayLanguages);
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
                if (spinner_src_languages != null && spinner_src_languages.getSelectedItem() != null) {
                    String src_country = spinner_src_languages.getSelectedItem().toString();
                    LANGUAGE.SRC_DIALECT = src_countries_dialects.get(src_country);
                    if (LANGUAGE.SRC_DIALECT != null) {
                        LANGUAGE.SRC = toMlKitCode(LANGUAGE.SRC_DIALECT);
                    }
                    setText(textview_src_dialect, LANGUAGE.SRC_DIALECT);
                    setText(textview_src, LANGUAGE.SRC);
                }

                if (spinner_dst_languages != null && spinner_dst_languages.getSelectedItem() != null) {
                    LANGUAGE.DST_COUNTRY = spinner_dst_languages.getSelectedItem().toString();
                    LANGUAGE.DST = map_dst_country.get(LANGUAGE.DST_COUNTRY);
                    setText(textview_dst, LANGUAGE.DST);
                }

                int h;
                if (Objects.equals(LANGUAGE.DST, "ja") || Objects.equals(LANGUAGE.DST, "zh")) {
                    h = 75;
                } else {
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
                if (spinner_src_languages != null && spinner_src_languages.getSelectedItem() != null) {
                    String src_country = spinner_src_languages.getSelectedItem().toString();
                    LANGUAGE.SRC_DIALECT = src_countries_dialects.get(src_country);
                    if (LANGUAGE.SRC_DIALECT != null) {
                        LANGUAGE.SRC = toMlKitCode(LANGUAGE.SRC_DIALECT);
                    }
                    setText(textview_src_dialect, LANGUAGE.SRC_DIALECT);
                    setText(textview_src, LANGUAGE.SRC);
                }

                if (spinner_dst_languages != null && spinner_dst_languages.getSelectedItem() != null) {
                    LANGUAGE.DST_COUNTRY = spinner_dst_languages.getSelectedItem().toString();
                    LANGUAGE.DST = map_dst_country.get(LANGUAGE.DST_COUNTRY);
                    setText(textview_dst, LANGUAGE.DST);
                }
            }
        });

        spinner_dst_languages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (spinner_src_languages != null && spinner_src_languages.getSelectedItem() != null) {

                    String src_country = spinner_src_languages.getSelectedItem().toString();
                    LANGUAGE.SRC_DIALECT = src_countries_dialects.get(src_country);
                    if (LANGUAGE.SRC_DIALECT != null) {
                        LANGUAGE.SRC = toMlKitCode(LANGUAGE.SRC_DIALECT);
                    }
                    setText(textview_src_dialect, LANGUAGE.SRC_DIALECT);
                    setText(textview_src, LANGUAGE.SRC);
                }

                if (spinner_dst_languages != null && spinner_dst_languages.getSelectedItem() != null) {
                    LANGUAGE.DST_COUNTRY = spinner_dst_languages.getSelectedItem().toString();
                    LANGUAGE.DST = map_dst_country.get(LANGUAGE.DST_COUNTRY);
                    setText(textview_dst, LANGUAGE.DST);
                }

                int h;
                if (Objects.equals(LANGUAGE.DST, "ja") || Objects.equals(LANGUAGE.DST, "zh")) {
                    h = 75;
                } else {
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
                if (spinner_src_languages != null && spinner_src_languages.getSelectedItem() != null) {
                    String src_country = spinner_src_languages.getSelectedItem().toString();
                    LANGUAGE.SRC_DIALECT = src_countries_dialects.get(src_country);
                    if (LANGUAGE.SRC_DIALECT != null) {
                        LANGUAGE.SRC = toMlKitCode(LANGUAGE.SRC_DIALECT);
                    }
                    setText(textview_src_dialect, LANGUAGE.SRC_DIALECT);
                    setText(textview_src, LANGUAGE.SRC);
                }
                if (spinner_dst_languages != null && spinner_dst_languages.getSelectedItem() != null) {
                    LANGUAGE.DST_COUNTRY = spinner_dst_languages.getSelectedItem().toString();
                    LANGUAGE.DST = map_dst_country.get(LANGUAGE.DST_COUNTRY);
                    setText(textview_dst, LANGUAGE.DST);
                }
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
                    } else {
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
                            } else {
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
                    MainActivity.voice_text.setText("");
                    MainActivity.voice_text.setHint("Recognized words");
                    MainActivity.full_voice_text.setText("");
                    MainActivity.full_voice_text.setHint("Recognized sentences");
                    //audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (int)Double.parseDouble(String.valueOf((long)(audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) / 2))), 0);
                    MainActivity.audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, MainActivity.mStreamVolume, 0);
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
        //audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mStreamVolume, AudioManager.ADJUST_SAME);
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mStreamVolume, 0);
        Log.d(TAG, "onBackPressed: mStreamVolume = " + mStreamVolume);
        //Log.d(TAG, "onBackPressed: mMusicVolume = " + mMusicVolume);

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop_create_overlay_translation_text();
        stop_create_overlay_mic_button();
        stop_voice_recognizer();
        //audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mStreamVolume, AudioManager.ADJUST_SAME);
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mStreamVolume, 0);
        Log.d(TAG, "onDestroy: mStreamVolume = " + mStreamVolume);
        //Log.d(TAG, "onDestroy: mMusicVolume = " + mMusicVolume);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!MyNotificationListenerService.isEnabled(this)) {
            Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(i);

        }
        handler.removeCallbacks(checkConnection);
        handler.postDelayed(checkConnection, 1500);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(checkConnection);
        super.onPause();
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

    private int getSpinnerIndexByCode(ArrayList<String> supportedLanguages, String targetCode) {
        if (targetCode == null || targetCode.isEmpty()) return 0;

        if (targetCode.equalsIgnoreCase("zh-CN")) {
            targetCode = "cmn-Hans-CN";
        }

        for (int i = 0; i < supportedLanguages.size(); i++) {
            String displayName = supportedLanguages.get(i);
            String dialectCode = src_countries_dialects.get(displayName); // Menghasilkan "id-ID", "id", atau "en-US"

            if (dialectCode != null) {
                String languageCode = dialectCode.split("-")[0];
                if (languageCode.equalsIgnoreCase(targetCode)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private int getSpinnerIndexByDialectCode(ArrayList<String> supportedLanguages, String targetDialectCode) {
        if (targetDialectCode == null || targetDialectCode.isEmpty()) return 0;

        if (targetDialectCode.equalsIgnoreCase("zh-CN")) {
            targetDialectCode = "cmn-Hans-CN";
        }

        for (int i = 0; i < supportedLanguages.size(); i++) {
            String displayName = supportedLanguages.get(i);
            String dialectCode = src_countries_dialects.get(displayName); // Menghasilkan "id-ID", "id", atau "en-US"

            if (dialectCode != null) {
                if (dialectCode.equalsIgnoreCase(targetDialectCode)) {
                    return i;
                }
            }
        }
        return 0;
    }

    public void setup_src_spinner(ArrayList<String> supported_languages) {
        src_countries_dialects = new HashMap<>();
        for (int i = 0; i < supported_languages.size(); i++) {
            src_countries_dialects.put(supported_languages.get(i), src_dialects[i]);
        }

        Collections.sort(supported_languages);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_textview_align, supported_languages);
        adapter.setDropDownViewResource(R.layout.spinner_textview_align);

        spinner_src_languages.setAdapter(adapter);

        //spinner_src_languages.setSelection(getSpinnerIndexByCode(supported_languages, "id"));
        spinner_src_languages.setSelection(getSpinnerIndexByDialectCode(supported_languages, "id-ID"));
    }

    public void setup_dst_spinner(ArrayList<String> supported_languages) {
        Collections.sort(supported_languages);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_textview_align, supported_languages);
        adapter.setDropDownViewResource(R.layout.spinner_textview_align);
        spinner_dst_languages.setAdapter(adapter);
        spinner_dst_languages.setSelection(supported_languages.indexOf("English"));
    }

    private void loadLocaleLanguages() {
        final String[] tags = {
                "af-ZA","sq-AL","am-ET","ar-SA","ar-EG","ar-AE","hy-AM","as-IN","az-AZ",
                "eu-ES","be-BY","bn-BD","bn-IN","bs-BA","bg-BG","ca-ES","ceb",
                "cmn-Hans-CN","cmn-Hant-TW","yue-Hant-HK",
                "hr-HR","cs-CZ","da-DK","nl-NL","nl-BE",
                // English dialect
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

        src_dialects = tagList.toArray(new String[0]);
        src_countries = names.toArray(new String[0]);
        setup_src_spinner(names);
    }

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

    // Google dialect tag conversion -> ML Kit code (one place only)
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

    // Match the tags exactly first; if not, use the first entry with the same language code
    private int indexOfTag(java.util.List<String> names, String exactTag, String langFallback) {
        int fallback = -1;
        for (int i = 0; i < names.size(); i++) {
            String tag = src_countries_dialects.get(names.get(i));
            if (tag == null) continue;
            if (tag.equalsIgnoreCase(exactTag)) return i;
            if (fallback < 0 && tag.split("-")[0].equalsIgnoreCase(langFallback)) fallback = i;
        }
        return Math.max(fallback, 0);
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
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                mlkit_status_message = "MLKIT dictionary is ready";
                setText(textview_mlkit_status, mlkit_status_message);
                setText(textview_mlkit_status, "");
                if (translator != null) translator.close();
            });
        }

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

}
