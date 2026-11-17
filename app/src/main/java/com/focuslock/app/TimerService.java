
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

    private static final String CHANNEL_ID =
            "FocusLockTimerChannelV2";

    private static final int NOTIFICATION_ID = 1002;

    private final Handler timerHandler =
            new Handler(Looper.getMainLooper());

    private boolean loopRunning = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {

            long remaining =
                    StorageHelper.syncSession(TimerService.this);

            if (!StorageHelper.isSessionActive(TimerService.this)
                    || remaining <= 0L) {

                stopTimerService();
                return;
            }

            boolean paused =
                    StorageHelper.isSessionPaused(
                            TimerService.this
                    );

            updateNotification(remaining, paused);

            timerHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        long remaining =
                StorageHelper.syncSession(this);

        if (!StorageHelper.isSessionActive(this)
                || remaining <= 0L) {

            stopSelf();
            return START_NOT_STICKY;
        }

        boolean paused =
                StorageHelper.isSessionPaused(this);

        startForeground(
                NOTIFICATION_ID,
                buildNotification(remaining, paused)
        );

        if (!loopRunning) {
            loopRunning = true;
            timerHandler.post(tickRunnable);
        }

        return START_STICKY;
    }

    private void stopTimerService() {
        timerHandler.removeCallbacks(tickRunnable);
        loopRunning = false;
        stopForeground(true);
        stopSelf();
    }

    private void createNotificationChannel() {

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "FocusLock Timer",
                        NotificationManager.IMPORTANCE_LOW
                );

        channel.setDescription(
                "Shows remaining study time, subject, chapter and session type"
        );

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

        String chapter =
                StorageHelper.getCurrentChapter(this);

        String sessionType =
                StorageHelper.getCurrentSessionType(this);

        String formattedTime =
                StorageHelper.formatTimeMMSS(remainingSeconds);

        String title = isPaused
                ? "Paused: " + subject
                : "Studying: " + subject;

        StringBuilder details = new StringBuilder();

        if (chapter != null && !chapter.trim().isEmpty()) {
            details.append(chapter).append(" • ");
        }

        if (sessionType != null && !sessionType.trim().isEmpty()) {
            details.append(sessionType).append(" • ");
        }

        details.append("Time left: ").append(formattedTime);

        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(details.toString())
                .setStyle(
                        new Notification.BigTextStyle()
                                .bigText(details.toString())
                )
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
                    buildNotification(
                            remainingSeconds,
                            isPaused
                    )
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
