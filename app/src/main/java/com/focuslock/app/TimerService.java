package com.focuslock.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class TimerService extends Service {

    private static final String CHANNEL_ID = "FocusLockTimerChannelV2";
    private static final int NOTIFICATION_ID = 1002;

    private static final String PREF_NAME = "FocusLockDatabaseV2";
    private static final String KEY_PAUSE_STARTED_AT = "pause_started_at";
    private static final long MAX_PAUSE_DURATION_MS = 10L * 60L * 1000L;

    private final Handler timerHandler =
            new Handler(Looper.getMainLooper());

    private boolean loopRunning = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {

            if (!StorageHelper.isSessionActive(TimerService.this)) {
                clearPauseTimestamp();
                stopForeground(true);
                stopSelf();
                loopRunning = false;
                return;
            }

            boolean isPaused =
                    StorageHelper.isSessionPaused(TimerService.this);

            long remainingSeconds;

            if (isPaused) {

                long pauseStartedAt = getPauseStartedAt();

                if (pauseStartedAt <= 0L) {
                    pauseStartedAt = System.currentTimeMillis();
                    savePauseStartedAt(pauseStartedAt);
                }

                long elapsedPause =
                        System.currentTimeMillis() - pauseStartedAt;

                if (elapsedPause >= MAX_PAUSE_DURATION_MS) {

                    StorageHelper.setSessionPaused(
                            TimerService.this, false);

                    clearPauseTimestamp();

                    isPaused = false;

                    remainingSeconds =
                            StorageHelper.tickOneSecond(
                                    TimerService.this);

                } else {

                    remainingSeconds =
                            StorageHelper.getRemainingSeconds(
                                    TimerService.this);
                }

            } else {

                clearPauseTimestamp();

                remainingSeconds =
                        StorageHelper.tickOneSecond(
                                TimerService.this);
            }

            if (remainingSeconds <= 0L) {
                clearPauseTimestamp();
                stopForeground(true);
                stopSelf();
                loopRunning = false;
                return;
            }

            updateNotification(remainingSeconds, isPaused);

            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createSilentNotificationChannel();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (!StorageHelper.isSessionActive(this)) {
            clearPauseTimestamp();
            stopSelf();
            return START_NOT_STICKY;
        }

        long remaining =
                StorageHelper.getRemainingSeconds(this);

        boolean paused =
                StorageHelper.isSessionPaused(this);

        startForeground(
                NOTIFICATION_ID,
                buildNotification(remaining, paused)
        );

        if (!loopRunning) {
            loopRunning = true;
            timerHandler.postDelayed(tickRunnable, 1000);
        }

        return START_STICKY;
    }

    private SharedPreferences getTimerPreferences() {
        return getSharedPreferences(
                PREF_NAME,
                MODE_PRIVATE
        );
    }

    private long getPauseStartedAt() {
        return getTimerPreferences().getLong(
                KEY_PAUSE_STARTED_AT,
                0L
        );
    }

    private void savePauseStartedAt(long timestamp) {
        getTimerPreferences().edit()
                .putLong(KEY_PAUSE_STARTED_AT, timestamp)
                .apply();
    }

    private void clearPauseTimestamp() {
        getTimerPreferences().edit()
                .remove(KEY_PAUSE_STARTED_AT)
                .apply();
    }

    private void createSilentNotificationChannel() {

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "FocusLock Timer",
                NotificationManager.IMPORTANCE_LOW
        );

        channel.setDescription(
                "Shows remaining study time and subject");

        channel.setSound(null, null);
        channel.enableVibration(false);

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(
            long remainingSeconds,
            boolean isPaused) {

        Intent openAppIntent =
                new Intent(this, MainActivity.class);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        String subject =
                StorageHelper.getCurrentSubject(this);

        String timeFormatted =
                StorageHelper.formatTimeMMSS(remainingSeconds);

        String title = isPaused
                ? "Paused (" + subject + ")"
                : "Studying: " + subject;

        String body = "Time Left: " + timeFormatted;

        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(
            long remainingSeconds,
            boolean isPaused) {

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        if (manager != null) {
            manager.notify(
                    NOTIFICATION_ID,
                    buildNotification(remainingSeconds, isPaused)
            );
        }
    }

    @Override
    public void onDestroy() {
        timerHandler.removeCallbacks(tickRunnable);
        loopRunning = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

