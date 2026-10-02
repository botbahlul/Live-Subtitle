package com.app.livesubtitle;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import java.util.List;

public class MyNotificationListenerService
        extends NotificationListenerService {

    private static final String TAG = "MyNotificationListener";
    private static MyNotificationListenerService instance;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
        Log.d(TAG, "Notification Listener CONNECTED");
        testMediaSessions();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        if (instance == this) {
            instance = null;
        }
        Log.d(TAG, "Notification Listener DISCONNECTED");
    }

    private void testMediaSessions() {
        try {
            MediaSessionManager mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager == null) {
                Log.e(TAG, "MediaSessionManager == null");
                return;
            }

            List<MediaController> controllers = mediaSessionManager.getActiveSessions(new ComponentName(this, MyNotificationListenerService.class));
            //Log.d(TAG,"Active MediaSessions = " + controllers.size());

            for (MediaController controller : controllers) {
                String packageName = controller.getPackageName();
                Log.d(TAG, "MediaSession package = " + packageName);
                if (packageName != null) {
                    Log.d(TAG, "===== " + packageName + " FOUND =====");
                    PlaybackState state = controller.getPlaybackState();
                    if (state == null) {
                        Log.d(TAG, packageName + " PlaybackState = NULL");
                    } else {
                        Log.d(TAG, packageName + " PlaybackState = " + playbackStateToString(state.getState()));
                        Log.d(TAG, "Position = " + state.getPosition());
                        Log.d(TAG, "Speed = " + state.getPlaybackSpeed());
                        Log.d(TAG, "Actions = " + state.getActions());
                    }
                    MediaMetadata metadata = controller.getMetadata();
                    if (metadata != null) {
                        Log.d(TAG,"Title = " + metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
                        Log.d(TAG, "Artist = " + metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
                    }
                    Log.d(TAG,"========================");
                }
            }

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage(), e);

        } catch (Exception e) {
            Log.e(TAG,"Exception: " + e.getMessage(), e);
        }
    }

    public static void ensureOperaPlaying() {
        if (instance == null) {
            Log.e(TAG, "Notification Listener is not connected");
            return;
        }

        try {
            MediaSessionManager mediaSessionManager = (MediaSessionManager) instance.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager == null) {
                Log.e(TAG, "MediaSessionManager == null");
                return;
            }

            ComponentName notificationListener = new ComponentName(instance, MyNotificationListenerService.class);
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(notificationListener);
            //Log.d(TAG, "Active MediaSessions = " + controllers.size());

            for (MediaController controller : controllers) {
                String packageName = controller.getPackageName();
                if (packageName == null || !packageName.equals("com.opera.browser")) {
                    continue;
                }

                PlaybackState state = controller.getPlaybackState();
                float currentPlaybackSpeed = state.getPlaybackSpeed();
                //Log.d(TAG, "Opera PlaybackSpeed = " + currentPlaybackSpeed);
                if (currentPlaybackSpeed == 0) {
                    //Log.d(TAG, "Operap playback speed is zero -> sending PLAY");
                    controller.getTransportControls().play();

                } else {
                    //Log.d(TAG, "Opera is still playing -> no PLAY command");
                }

                return;
            }
            //Log.d(TAG, "Opera MediaSession not found");
            //Log.d(TAG, "Opera MediaSession not found");

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage(), e);

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage(), e);
        }
    }

    private static String playbackStateToString(int state) {
        switch (state) {
            case PlaybackState.STATE_NONE:
                return "STATE_NONE";
            case PlaybackState.STATE_STOPPED:
                return "STATE_STOPPED";
            case PlaybackState.STATE_PAUSED:
                return "STATE_PAUSED";
            case PlaybackState.STATE_PLAYING:
                return "STATE_PLAYING";
            case PlaybackState.STATE_FAST_FORWARDING:
                return "STATE_FAST_FORWARDING";
            case PlaybackState.STATE_REWINDING:
                return "STATE_REWINDING";
            case PlaybackState.STATE_BUFFERING:
                return "STATE_BUFFERING";
            case PlaybackState.STATE_ERROR:
                return "STATE_ERROR";
            case PlaybackState.STATE_CONNECTING:
                return "STATE_CONNECTING";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }

}