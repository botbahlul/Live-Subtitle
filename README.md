# Live-Subtitle
ANDROID APP that can RECOGNIZE VLC LIVE AUDIO/VIDEO STREAMING (using free Android Developers Speech Recognition API) then TRANSLATE (using MLKIT TRANSLATE API) and display them as LIVE CAPTIONS / LIVE SUBTITLES.

This app has PROS & CONS compared to other Speech Recognition APIs like VOSK, IBM WATSON, and PREMIUM GOOGPLE SPEECH API

PROS:
It's FREE and supports all languages supported by Google with very good accuracy

CONS:
It currently can only listen if you PLAY YOUTUBE STREAM (NOT ALL STREAMS!) via ANDROID VLC PLAYER
Please note that when you select a VOICE LANGUAGE or TRANSLATION LANGUAGE this app needs to DOWNLOAD MLKIT DICTIONARY first before it can show the translation

![image](https://user-images.githubusercontent.com/88623122/222401306-597cb0d2-bd12-4ddf-8d3c-40da14c77203.png)


VLC UPDATE NOTES : IF YOUR VLC CANNOT PLAY DIRECT YOUTUBE LINK URL ANYMORE, YOU NEED TO UPDATE youtube.lua SCRIPT INSIDE VLC

There are 2 ways to update youtube.lua script :

1. FOR ROOTED ANDROID DEVICE :

Replace youtube.lua in android storage with ES FILE MANAGER or FX FILE MANAGER :

```
/data/data/org.videolan.vlc/app_vlc/.share/lua/playlist/youtube.lua
```

2. FOR NON ROOTED ANDROID DEVICE :

Use APKTOOL https://ibotpeaches.github.io/Apktool/install/ to modify VLC.APK and APK EDITOR STUDIO https://qwertycube.com/apk-editor-studio/download/ to sign it

- download apktool.jar and place it into a folder that has been already added to system environment path (e.g. in C:\Windows\system32)

- create apktool.bat (or executable script if you're linux user) just like described in https://ibotpeaches.github.io/Apktool/install/ and place it in same folder as apktool.jar

- create a working folder in your drive to store apk file and its decompressed contents e.g. in C:\APK

- place vlc.apk into C:\APK (PLEASE USE OLD VERSION OF VLC, DO NOT USE NEWER THAT 3.4.4, I RECOMMEND TO USE VERSION 3.1.5, BECAUSE NEWER VERSION WILL GIVE SLOW PERFORMANCE)

- you can get vlc.apk from here : https://download.videolan.org/pub/videolan/vlc-android/

- open Command Prompt (cmd.exe) and goto C:\APK

- to decompress/decompile vlc.apk type :
```
apktool d vlc.apk -o vlc_mod
```

- replace youtube.lua in C:\APK\apk_mod\assets\lua\playlist\ with the latest youtube.lua from https://code.videolan.org/videolan/vlc/-/raw/master/share/lua/playlist/youtube.lua

- to rebuild apk type :
```
apktool b vlc_mod -o vlc_mod.apk
```

- open APK EDITOR STUDIO, click Tools -> Key Manager to create a sign key

- in Keystroke Path input : C:/APK/MyKey.jks then click Create

- input those all necessary fields (passwords, key name alias,  your name & address, etc) then click OK, then close APK EDITOR STUDIO

- now you can sign that vlc_mod.apk by right click on it Windows Explorer and click Sign APK then install it on your android device

- If you're still insist to use latest VLC.APK you need to add --use-aapt2 in apktool command line e.g :
```
apktool d --use-aapt2 vlc.apk -o vlc_mod
```
- and to build apk :
```
apktool b --use-aapt2 vlc_mod -o vlc_mod.apk
```


For LOCAL VIDEOS on your STORAGE, you need ES MEDIA PLAYER (by installing ES FILE EXPLORER) or FX MEDIA PLAYER (by installing FX FILE MANAGER)

Check my other version (in translation methode) of this app in https://github.com/botbahlul/Live-Subtitle-V2

https://user-images.githubusercontent.com/88623122/193420307-288c6576-050f-451e-af26-45ee7687e53d.mp4

Also check my other SPEECH RECOGNITIION + TRANSLATE PROJECTS in https://botbahlul.github.io

