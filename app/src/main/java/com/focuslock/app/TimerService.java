package com.focuslock.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class TimerService extends Service {

    private static final String CHANNEL_ID = "FocusLockTimerChannel";
    private static final int NOTIFICATION_ID = 1001;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private boolean loopRunning = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!StorageHelper.isSessionActive(TimerService.this)) {
                stopForeground(true);
                stopSelf();
                loopRunning = false;
                return;
            }

            long remainingSeconds;
            boolean isPaused = StorageHelper.isSessionPaused(TimerService.this);

            if (isPaused) {
                // Strict Pause: Do not subtract seconds, but keep notification & blocker active
                remainingSeconds = StorageHelper.getRemainingSeconds(TimerService.this);
            } else {
                // Active study: Subtract 1 second and record study history
                remainingSeconds = StorageHelper.tickOneSecond(TimerService.this);
            }

            if (remainingSeconds <= 0L) {
                // Silent finish when timer reaches 00:00
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!StorageHelper.isSessionActive(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        long remaining = StorageHelper.getRemainingSeconds(this);
        boolean paused = StorageHelper.isSessionPaused(this);
        startForeground(NOTIFICATION_ID, buildNotification(remaining, paused));

        if (!loopRunning) {
            loopRunning = true;
            timerHandler.postDelayed(tickRunnable, 1000);
        }

        return START_STICKY;
    }

    private void createSilentNotificationChannel() {
        // IMPORTANCE_LOW guarantees a small, completely silent notification with no sound or pop-up
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Focus Lock Study Timer",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Shows remaining study time while Focus Lock is active");
        channel.setSound(null, null);
        channel.enableVibration(false);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(long remainingSeconds, boolean isPaused) {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String timeFormatted = StorageHelper.formatTimeMMSS(remainingSeconds);
        String title = isPaused ? "Focus Lock (Paused — Apps Locked)" : "Focus Lock Active";
        String body = "Time Left: " + timeFormatted;

        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(long remainingSeconds, boolean isPaused) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(remainingSeconds, isPaused));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(tickRunnable);
        loopRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
