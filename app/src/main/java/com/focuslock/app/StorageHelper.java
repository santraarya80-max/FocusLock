package com.focuslock.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class StorageHelper {

    private static final String PREF_NAME = "FocusLockDatabaseV2";

    private static final String KEY_BLOCKED_APPS = "blocked_apps";
    private static final String KEY_SESSION_ACTIVE = "session_active";
    private static final String KEY_SESSION_PAUSED = "session_paused";
    private static final String KEY_REMAINING_SECONDS = "remaining_seconds";
    private static final String KEY_TOTAL_SESSION_SECONDS = "total_session_seconds";
    private static final String KEY_UNRECORDED_SECONDS = "unrecorded_seconds";
    private static final String KEY_PAUSE_COUNT = "pause_count";
    private static final String KEY_CURRENT_SUBJECT = "current_subject";

    private static final String HISTORY_PREFIX = "history_";
    private static final String SUBJECT_PREFIX = "subject_";
    private static final String KEY_LAST_STUDY_DATE = "last_study_date";
    private static final String KEY_STREAK_DAYS = "streak_days";

    // New timer recovery data. Existing study data is not replaced.
    private static final String KEY_LAST_TICK_ELAPSED = "timer_last_tick_elapsed";
    private static final String KEY_PAUSE_STARTED_ELAPSED = "timer_pause_started_elapsed";
    private static final String KEY_TIMER_BOOT_COUNT = "timer_boot_count";

    private static final long MAX_PAUSE_DURATION_MS =
            10L * 60L * 1000L;

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    private static int getCurrentBootCount(Context context) {
        try {
            return Settings.Global.getInt(
                    context.getContentResolver(),
                    Settings.Global.BOOT_COUNT
            );
        } catch (Exception e) {
            return -1;
        }
    }

    public static void saveBlockedApps(
            Context context,
            Set<String> packageNames) {

        getPrefs(context).edit()
                .putStringSet(
                        KEY_BLOCKED_APPS,
                        new HashSet<>(packageNames)
                )
                .apply();
    }

    public static Set<String> getBlockedApps(Context context) {
        Set<String> saved = getPrefs(context).getStringSet(
                KEY_BLOCKED_APPS,
                null
        );

        if (saved == null) {
            return new HashSet<>();
        }

        return new HashSet<>(saved);
    }

    public static void startSession(
            Context context,
            int minutes,
            String subject) {

        long totalSeconds = minutes * 60L;
        long now = SystemClock.elapsedRealtime();
        int bootCount = getCurrentBootCount(context);

        getPrefs(context).edit()
                .putBoolean(KEY_SESSION_ACTIVE, true)
                .putBoolean(KEY_SESSION_PAUSED, false)
                .putLong(KEY_REMAINING_SECONDS, totalSeconds)
                .putLong(KEY_TOTAL_SESSION_SECONDS, totalSeconds)
                .putInt(KEY_UNRECORDED_SECONDS, 0)
                .putInt(KEY_PAUSE_COUNT, 0)
                .putString(KEY_CURRENT_SUBJECT, subject)
                .putLong(KEY_LAST_TICK_ELAPSED, now)
                .putLong(KEY_PAUSE_STARTED_ELAPSED, 0L)
                .putInt(KEY_TIMER_BOOT_COUNT, bootCount)
                .apply();
    }

    public static boolean isSessionActive(Context context) {
        SharedPreferences prefs = getPrefs(context);

        return prefs.getBoolean(KEY_SESSION_ACTIVE, false)
                && prefs.getLong(KEY_REMAINING_SECONDS, 0L) > 0L;
    }

    public static boolean isSessionPaused(Context context) {
        return getPrefs(context).getBoolean(
                KEY_SESSION_PAUSED,
                false
        );
    }

    public static int getPauseCount(Context context) {
        return getPrefs(context).getInt(KEY_PAUSE_COUNT, 0);
    }

    public static void incrementPauseCount(Context context) {
        SharedPreferences prefs = getPrefs(context);

        int current = prefs.getInt(KEY_PAUSE_COUNT, 0);

        prefs.edit()
                .putInt(KEY_PAUSE_COUNT, current + 1)
                .apply();
    }

    public static void setSessionPaused(
            Context context,
            boolean paused) {

        SharedPreferences prefs = getPrefs(context);
        long now = SystemClock.elapsedRealtime();

        SharedPreferences.Editor editor = prefs.edit();

        if (paused) {
            editor.putBoolean(KEY_SESSION_PAUSED, true);
            editor.putLong(KEY_PAUSE_STARTED_ELAPSED, now);
        } else {
            editor.putBoolean(KEY_SESSION_PAUSED, false);
            editor.putLong(KEY_PAUSE_STARTED_ELAPSED, 0L);
            editor.putLong(KEY_LAST_TICK_ELAPSED, now);
        }

        editor.putInt(
                KEY_TIMER_BOOT_COUNT,
                getCurrentBootCount(context)
        );

        editor.apply();
    }

    public static String getCurrentSubject(Context context) {
        return getPrefs(context).getString(
                KEY_CURRENT_SUBJECT,
                "Maths"
        );
    }

    public static long getRemainingSeconds(Context context) {
        return Math.max(
                0L,
                getPrefs(context).getLong(
                        KEY_REMAINING_SECONDS,
                        0L
                )
        );
    }

    public static long getTotalSessionSeconds(Context context) {
        SharedPreferences prefs = getPrefs(context);

        long savedTotal = prefs.getLong(
                KEY_TOTAL_SESSION_SECONDS,
                0L
        );

        // Compatibility with older saved sessions.
        if (savedTotal <= 0L && isSessionActive(context)) {
            return getRemainingSeconds(context);
        }

        return Math.max(0L, savedTotal);
    }

    /**
     * Synchronises the saved timer with Android's monotonic clock.
     *
     * This works even if the service was temporarily stopped.
     * Time spent paused is not deducted from the study timer.
     */
    public static synchronized long syncSession(Context context) {

        SharedPreferences prefs = getPrefs(context);

        if (!prefs.getBoolean(KEY_SESSION_ACTIVE, false)) {
            return 0L;
        }

        long remaining = Math.max(
                0L,
                prefs.getLong(KEY_REMAINING_SECONDS, 0L)
        );

        if (remaining <= 0L) {
            completeSession(context);
            return 0L;
        }

        long now = SystemClock.elapsedRealtime();

        long lastTick = prefs.getLong(
                KEY_LAST_TICK_ELAPSED,
                0L
        );

        long pauseStarted = prefs.getLong(
                KEY_PAUSE_STARTED_ELAPSED,
                0L
        );

        int savedBootCount = prefs.getInt(
                KEY_TIMER_BOOT_COUNT,
                -1
        );

        int currentBootCount = getCurrentBootCount(context);

        boolean rebootDetected =
                savedBootCount != currentBootCount
                        || lastTick <= 0L
                        || now < lastTick;

        if (rebootDetected) {
            SharedPreferences.Editor recovery = prefs.edit();

            recovery.putLong(KEY_LAST_TICK_ELAPSED, now);
            recovery.putInt(
                    KEY_TIMER_BOOT_COUNT,
                    currentBootCount
            );

            if (prefs.getBoolean(KEY_SESSION_PAUSED, false)) {
                recovery.putLong(
                        KEY_PAUSE_STARTED_ELAPSED,
                        now
                );
            } else {
                recovery.putLong(
                        KEY_PAUSE_STARTED_ELAPSED,
                        0L
                );
            }

            recovery.apply();

            // Do not guess how much time passed while the phone was off.
            return remaining;
        }

        boolean paused = prefs.getBoolean(
                KEY_SESSION_PAUSED,
                false
        );

        if (paused) {

            if (pauseStarted <= 0L || now < pauseStarted) {
                prefs.edit()
                        .putLong(KEY_PAUSE_STARTED_ELAPSED, now)
                        .apply();

                return remaining;
            }

            long pauseElapsed = now - pauseStarted;

            if (pauseElapsed < MAX_PAUSE_DURATION_MS) {
                return remaining;
            }

            // Automatically resume after the ten-minute pause limit.
            long automaticResumeTime =
                    pauseStarted + MAX_PAUSE_DURATION_MS;

            prefs.edit()
                    .putBoolean(KEY_SESSION_PAUSED, false)
                    .putLong(
                            KEY_PAUSE_STARTED_ELAPSED,
                            0L
                    )
                    .putLong(
                            KEY_LAST_TICK_ELAPSED,
                            automaticResumeTime
                    )
                    .apply();

            lastTick = automaticResumeTime;
        }

        long elapsedMillis = Math.max(0L, now - lastTick);
        long elapsedSeconds = elapsedMillis / 1000L;

        if (elapsedSeconds <= 0L) {
            return remaining;
        }

        long secondsToApply = Math.min(
                elapsedSeconds,
                remaining
        );

        remaining -= secondsToApply;

        long updatedLastTick =
                lastTick + (elapsedSeconds * 1000L);

        // Never move the saved clock beyond the current clock.
        updatedLastTick = Math.min(updatedLastTick, now);

        int unrecorded = prefs.getInt(
                KEY_UNRECORDED_SECONDS,
                0
        );

        long combinedSeconds =
                unrecorded + secondsToApply;

        long completedMinutes = combinedSeconds / 60L;

        unrecorded = (int) (combinedSeconds % 60L);

        SharedPreferences.Editor editor = prefs.edit();

        editor.putLong(KEY_REMAINING_SECONDS, remaining);
        editor.putLong(KEY_LAST_TICK_ELAPSED, updatedLastTick);
        editor.putInt(KEY_UNRECORDED_SECONDS, unrecorded);

        if (completedMinutes > 0L) {

            String todayDate = getTodayDateString();

            String todayKey = HISTORY_PREFIX + todayDate;

            int currentTodayMinutes = prefs.getInt(
                    todayKey,
                    0
            );

            editor.putInt(
                    todayKey,
                    currentTodayMinutes + (int) completedMinutes
            );

            String subject = getCurrentSubject(context);

            String subjectKey =
                    SUBJECT_PREFIX + todayDate + "_" + subject;

            int subjectMinutes = prefs.getInt(
                    subjectKey,
                    0
            );

            editor.putInt(
                    subjectKey,
                    subjectMinutes + (int) completedMinutes
            );
        }

        editor.apply();

        if (completedMinutes > 0L) {
            updateStreak(
                    prefs,
                    getTodayDateString()
            );
        }

        if (remaining <= 0L) {
            completeSession(context);
        }

        return remaining;
    }

    public static long tickOneSecond(Context context) {
        return syncSession(context);
    }

    private static void updateStreak(
            SharedPreferences prefs,
            String todayDate) {

        String lastDate = prefs.getString(
                KEY_LAST_STUDY_DATE,
                ""
        );

        int streak = prefs.getInt(KEY_STREAK_DAYS, 0);

        if (!todayDate.equals(lastDate)) {

            if (lastDate.isEmpty()) {
                streak = 1;
            } else {
                try {
                    SimpleDateFormat sdf =
                            new SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.US
                            );

                    sdf.setLenient(false);

                    Date d1 = sdf.parse(lastDate);
                    Date d2 = sdf.parse(todayDate);

                    long diffDays =
                            (d2.getTime() - d1.getTime())
                                    / (1000L * 60L * 60L * 24L);

                    if (diffDays == 1L) {
                        streak += 1;
                    } else if (diffDays > 1L) {
                        streak = 1;
                    }

                } catch (Exception e) {
                    streak = 1;
                }
            }

            prefs.edit()
                    .putString(KEY_LAST_STUDY_DATE, todayDate)
                    .putInt(KEY_STREAK_DAYS, Math.max(1, streak))
                    .apply();
        }
    }

    public static int getStreakDays(Context context) {
        return getPrefs(context).getInt(
                KEY_STREAK_DAYS,
                0
        );
    }

    public static void completeSession(Context context) {
        getPrefs(context).edit()
                .putBoolean(KEY_SESSION_ACTIVE, false)
                .putBoolean(KEY_SESSION_PAUSED, false)
                .putLong(KEY_REMAINING_SECONDS, 0L)
                .putLong(KEY_TOTAL_SESSION_SECONDS, 0L)
                .putInt(KEY_UNRECORDED_SECONDS, 0)
                .putInt(KEY_PAUSE_COUNT, 0)
                .putLong(KEY_LAST_TICK_ELAPSED, 0L)
                .putLong(KEY_PAUSE_STARTED_ELAPSED, 0L)
                .apply();
    }

    public static String getTodayDateString() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        return sdf.format(new Date());
    }

    public static int getTodayStudyMinutes(Context context) {
        String todayKey =
                HISTORY_PREFIX + getTodayDateString();

        return getPrefs(context).getInt(todayKey, 0);
    }

    public static int getTotalStudyMinutes(Context context) {
        Map<String, ?> allEntries =
                getPrefs(context).getAll();

        int total = 0;

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(HISTORY_PREFIX)
                    && entry.getValue() instanceof Integer) {
                total += (Integer) entry.getValue();
            }
        }

        return total;
    }

    public static String getSubjectBreakdownText(Context context) {
        Map<String, ?> allEntries =
                getPrefs(context).getAll();

        HashMap<String, Integer> subjectTotals =
                new HashMap<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {

            if (entry.getKey().startsWith(SUBJECT_PREFIX)
                    && entry.getValue() instanceof Integer) {

                String subjectName =
                        entry.getKey().substring(
                                entry.getKey().lastIndexOf("_") + 1
                        );

                int minutes = (Integer) entry.getValue();

                int existing = subjectTotals.containsKey(subjectName)
                        ? subjectTotals.get(subjectName) : 0;

                subjectTotals.put(
                        subjectName,
                        existing + minutes
                );
            }
        }

        if (subjectTotals.isEmpty()) {
            return "Study Time by Subject:\nNo subjects recorded yet.";
        }

        StringBuilder sb =
                new StringBuilder("Study Time by Subject:\n");

        for (Map.Entry<String, Integer> entry
                : subjectTotals.entrySet()) {

            sb.append("• ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(formatMinutesReadable(entry.getValue()))
                    .append("\n");
        }

        return sb.toString().trim();
    }

    public static List<String> getStudyHistoryList(Context context) {
        Map<String, ?> allEntries =
                getPrefs(context).getAll();

        List<String> dates = new ArrayList<>();

        for (String key : allEntries.keySet()) {
            if (key.startsWith(HISTORY_PREFIX)) {
                dates.add(key.substring(HISTORY_PREFIX.length()));
            }
        }

        Collections.sort(
                dates,
                Collections.reverseOrder()
        );

        List<String> displayList = new ArrayList<>();

        SharedPreferences prefs = getPrefs(context);

        for (String date : dates) {

            int minutes = prefs.getInt(
                    HISTORY_PREFIX + date,
                    0
            );

            displayList.add(
                    date + "   —   " + formatMinutesReadable(minutes)
            );
        }

        if (displayList.isEmpty()) {
            displayList.add(
                    "No study sessions recorded yet. Start your first timer!"
            );
        }

        return displayList;
    }

    public static String getRandomMotivationalQuote() {
        String[] quotes = {
                "Discipline is choosing between what you want now and what you want most.",
                "Success isn't always about greatness. It's about consistency.",
                "The secret of getting ahead is getting started.",
                "Your future is created by what you do today, not tomorrow.",
                "Push yourself, because no one else is going to do it for you.",
                "Don't watch the clock; do what it does. Keep going."
        };

        int index = new Random().nextInt(quotes.length);

        return quotes[index];
    }

    public static String formatTimeMMSS(long totalSeconds) {
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        return String.format(
                Locale.US,
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    public static String formatMinutesReadable(int totalMinutes) {

        if (totalMinutes < 60) {
            return totalMinutes + " mins";
        }

        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        if (minutes == 0) {
            return hours + (hours == 1 ? " hr" : " hrs");
        }

        return hours
                + (hours == 1 ? " hr " : " hrs ")
                + minutes
                + " mins";
    }
}

