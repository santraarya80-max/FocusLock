package com.focuslock.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StorageHelper {

    private static final String PREF_NAME = "FocusLockDatabase";
    private static final String KEY_BLOCKED_APPS = "blocked_apps";
    private static final String KEY_SESSION_ACTIVE = "session_active";
    private static final String KEY_SESSION_PAUSED = "session_paused";
    private static final String KEY_REMAINING_SECONDS = "remaining_seconds";
    private static final String KEY_UNRECORDED_SECONDS = "unrecorded_seconds";
    private static final String HISTORY_PREFIX = "history_";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Save the checklist of distracting apps
    public static void saveBlockedApps(Context context, Set<String> packageNames) {
        getPrefs(context).edit()
                .putStringSet(KEY_BLOCKED_APPS, new HashSet<>(packageNames))
                .apply();
    }

    // Get the saved checklist of distracting apps
    public static Set<String> getBlockedApps(Context context) {
        Set<String> saved = getPrefs(context).getStringSet(KEY_BLOCKED_APPS, null);
        if (saved == null) {
            return new HashSet<>();
        }
        return new HashSet<>(saved);
    }

    // Start a new strict study session
    public static void startSession(Context context, int minutes) {
        long totalSeconds = minutes * 60L;
        getPrefs(context).edit()
                .putBoolean(KEY_SESSION_ACTIVE, true)
                .putBoolean(KEY_SESSION_PAUSED, false)
                .putLong(KEY_REMAINING_SECONDS, totalSeconds)
                .putInt(KEY_UNRECORDED_SECONDS, 0)
                .apply();
    }

    // Check if the study timer is currently active (running or paused before 00:00)
    public static boolean isSessionActive(Context context) {
        return getPrefs(context).getBoolean(KEY_SESSION_ACTIVE, false)
                && getPrefs(context).getLong(KEY_REMAINING_SECONDS, 0L) > 0L;
    }

    // Check if the timer countdown is paused
    public static boolean isSessionPaused(Context context) {
        return getPrefs(context).getBoolean(KEY_SESSION_PAUSED, false);
    }

    // Pause or resume the countdown clock (apps remain locked either way)
    public static void setSessionPaused(Context context, boolean paused) {
        getPrefs(context).edit()
                .putBoolean(KEY_SESSION_PAUSED, paused)
                .apply();
    }

    // Get remaining countdown seconds
    public static long getRemainingSeconds(Context context) {
        return Math.max(0L, getPrefs(context).getLong(KEY_REMAINING_SECONDS, 0L));
    }

    // Called every 1 second by the background timer when not paused
    public static long tickOneSecond(Context context) {
        SharedPreferences prefs = getPrefs(context);
        long remaining = prefs.getLong(KEY_REMAINING_SECONDS, 0L);
        int unrecorded = prefs.getInt(KEY_UNRECORDED_SECONDS, 0);

        if (remaining <= 0L) {
            completeSession(context);
            return 0L;
        }

        remaining = remaining - 1L;
        unrecorded = unrecorded + 1;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_REMAINING_SECONDS, remaining);

        // Every 60 seconds of active study, add 1 minute to today's study total
        if (unrecorded >= 60) {
            unrecorded = unrecorded - 60;
            String todayKey = HISTORY_PREFIX + getTodayDateString();
            int currentTodayMins = prefs.getInt(todayKey, 0);
            editor.putInt(todayKey, currentTodayMins + 1);
        }

        editor.putInt(KEY_UNRECORDED_SECONDS, unrecorded);
        editor.apply();

        if (remaining <= 0L) {
            completeSession(context);
        }

        return remaining;
    }

    // Quietly finish the session and unlock apps when the clock hits 00:00
    public static void completeSession(Context context) {
        getPrefs(context).edit()
                .putBoolean(KEY_SESSION_ACTIVE, false)
                .putBoolean(KEY_SESSION_PAUSED, false)
                .putLong(KEY_REMAINING_SECONDS, 0L)
                .putInt(KEY_UNRECORDED_SECONDS, 0)
                .apply();
    }

    // Get today's date in YYYY-MM-DD format
    public static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    // Get how many minutes have been studied today
    public static int getTodayStudyMinutes(Context context) {
        String todayKey = HISTORY_PREFIX + getTodayDateString();
        return getPrefs(context).getInt(todayKey, 0);
    }

    // Get total all-time study minutes across all days
    public static int getTotalStudyMinutes(Context context) {
        Map<String, ?> allEntries = getPrefs(context).getAll();
        int total = 0;
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(HISTORY_PREFIX) && entry.getValue() instanceof Integer) {
                total += (Integer) entry.getValue();
            }
        }
        return total;
    }

    // Get the day-by-day history list sorted from newest date to oldest date
    public static List<String> getStudyHistoryList(Context context) {
        Map<String, ?> allEntries = getPrefs(context).getAll();
        List<String> dates = new ArrayList<>();

        for (String key : allEntries.keySet()) {
            if (key.startsWith(HISTORY_PREFIX)) {
                dates.add(key.substring(HISTORY_PREFIX.length()));
            }
        }

        Collections.sort(dates, Collections.reverseOrder());

        List<String> displayList = new ArrayList<>();
        SharedPreferences prefs = getPrefs(context);
        for (String date : dates) {
            int mins = prefs.getInt(HISTORY_PREFIX + date, 0);
            displayList.add(date + "   —   " + formatMinutesReadable(mins));
        }

        if (displayList.isEmpty()) {
            displayList.add("No study sessions recorded yet. Start your first timer!");
        }

        return displayList;
    }

    // Format seconds into MM:SS (for example: 90:00 or 04:09)
    public static String formatTimeMMSS(long totalSeconds) {
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    // Format minutes into a friendly text (for example: "1 hr 30 mins" or "45 mins")
    public static String formatMinutesReadable(int totalMinutes) {
        if (totalMinutes < 60) {
            return totalMinutes + " mins";
        }
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (mins == 0) {
            return hours + (hours == 1 ? " hr" : " hrs");
        }
        return hours + (hours == 1 ? " hr " : " hrs ") + mins + " mins";
    }
}
