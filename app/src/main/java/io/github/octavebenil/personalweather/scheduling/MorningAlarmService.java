package io.github.octavebenil.personalweather.scheduling;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import io.github.octavebenil.personalweather.data.SettingsStore;
import io.github.octavebenil.personalweather.domain.CommuteAdvice;
import io.github.octavebenil.personalweather.notification.Notifications;

import java.io.IOException;

/**
 * Short foreground service started by the morning alarm. It checks the weather,
 * posts the advice and, when enabled, rings until the user stops it (max 1 minute).
 */
public final class MorningAlarmService extends Service {

    public static final String ACTION_STOP = "io.github.octavebenil.personalweather.action.STOP_ALARM";
    private static final long MAX_RING_MS = 60_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private Thread worker;
    private PowerManager.WakeLock wakeLock;
    private volatile boolean stopped;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopEverything();
            return START_NOT_STICKY;
        }
        goForeground();
        acquireWakeLock();
        if (worker == null) {
            worker = new Thread(this::check, "morning-check");
            worker.start();
        }
        return START_NOT_STICKY;
    }

    private void goForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Notifications.ID_CHECKING, Notifications.checkingNotification(this),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE);
        } else {
            startForeground(Notifications.ID_CHECKING, Notifications.checkingNotification(this));
        }
    }

    /** Keeps the CPU awake while the forecast is fetched and the alarm rings. */
    private void acquireWakeLock() {
        if (wakeLock != null) {
            return;
        }
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power != null) {
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "personalweather:morning");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire(MAX_RING_MS + 60_000);
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }

    private void check() {
        CommuteAdvice advice = MorningCheck.run(this);
        SettingsStore settings = new SettingsStore(this);
        // Without notifications the user would have no way to stop the sound.
        boolean ring = settings.isSoundEnabled()
                && Notifications.canPost(this)
                && (!settings.isSoundOnlyWhenRain() || advice.isRainRisk());
        handler.post(() -> {
            if (stopped) {
                return;
            }
            if (ring) {
                startRinging(settings.getRingtoneUri());
                handler.postDelayed(this::stopEverything, MAX_RING_MS);
            } else {
                stopEverything();
            }
        });
    }

    private void startRinging(String ringtoneUri) {
        if (stopped) {
            return;
        }
        Uri uri = ringtoneUri != null ? Uri.parse(ringtoneUri)
                : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        Notifications.updateRingingNotification(this);
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.setOnPreparedListener(prepared -> {
                if (!stopped) {
                    prepared.start();
                }
            });
            player.setOnErrorListener((failed, what, extra) -> {
                stopEverything();
                return true;
            });
            player.prepareAsync();
        } catch (IOException | IllegalStateException | IllegalArgumentException | SecurityException e) {
            releasePlayer();
            stopEverything();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
                // Player was never started.
            }
            player.release();
            player = null;
        }
    }

    private void stopEverything() {
        stopped = true;
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        releaseWakeLock();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onTimeout(int startId) {
        stopEverything();
    }

    @Override
    public void onDestroy() {
        stopped = true;
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        releaseWakeLock();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
