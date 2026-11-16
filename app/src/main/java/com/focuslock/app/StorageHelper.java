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
import java.util.Random;

public class StorageHelper {

    private static final String PREF_NAME = "FocusLockDatabaseV2";
    private static final String KEY_BLOCKED_APPS = "blocked_apps";
    private static final String KEY_SESSION_ACTIVE = "session_active";
    private static final String KEY_SESSION_PAUSED = "session_paused";
    private static final String KEY_REMAINING_SECONDS = "remaining_seconds";
    private static final String KEY_UNRECORDED_SECONDS = "unrecorded_seconds";
    private static final String KEY_PAUSE_COUNT = "pause_count";
    private static final String KEY_CURRENT_SUBJECT = "current_subject";
    private static final String HISTORY_PREFIX = "history_";
    private static final String SUBJECT_PREFIX = "subject_";
    private static final String KEY_LAST_STUDY_DATE = "last_study_date";
    private static final String KEY_STREAK_DAYS = "streak_days";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveBlockedApps(Context context, Set<String> packageNames) {
        getPrefs(context).edit()
                .putStringSet(KEY_BLOCKED_APPS, new HashSet<>(packageNames))
                .apply();
    }

    public static Set<String> getBlockedApps(Context context) {
        Set<String> saved = getPrefs(context).getStringSet(KEY_BLOCKED_APPS, null);
        if (saved == null) {
            return new HashSet<>();
        }
        return new HashSet<>(saved);
    }

    public static void startSession(Context context, int minutes, String subject) {
        long totalSeconds = minutes * 60L;
        getPrefs(context).edit()
                .putBoolean(KEY_SESSION_ACTIVE, true)
                .putBoolean(KEY_SESSION_PAUSED, false)
                .putLong(KEY_REMAINING_SECONDS, totalSeconds)
                .putInt(KEY_UNRECORDED_SECONDS, 0)
                .putInt(KEY_PAUSE_COUNT, 0)
                .putString(KEY_CURRENT_SUBJECT, subject)
                .apply();
    }

    public static boolean isSessionActive(Context context) {
        return getPrefs(context).getBoolean(KEY_SESSION_ACTIVE, false)
                && getPrefs(context).getLong(KEY_REMAINING_SECONDS, 0L) > 0L;
    }

    public static boolean isSessionPaused(Context context) {
        return getPrefs(context).getBoolean(KEY_SESSION_PAUSED, false);
    }

    public static int getPauseCount(Context context) {
        return getPrefs(context).getInt(KEY_PAUSE_COUNT, 0);
    }

    public static void incrementPauseCount(Context context) {
        int current = getPauseCount(context);
        getPrefs(context).edit().putInt(KEY_PAUSE_COUNT, current + 1).apply();
    }

    public static void setSessionPaused(Context context, boolean paused) {
        getPrefs(context).edit()
                .putBoolean(KEY_SESSION_PAUSED, paused)
                .apply();
    }

    public static String getCurrentSubject(Context context) {
        return getPrefs(context).getString(KEY_CURRENT_SUBJECT, "Maths");
    }

    public static long getRemainingSeconds(Context context) {
        return Math.max(0L, getPrefs(context).getLong(KEY_REMAINING_SECONDS, 0L));
    }

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

        if (unrecorded >= 60) {
            unrecorded = unrecorded - 60;
            String todayDate = getTodayDateString();
            String todayKey = HISTORY_PREFIX + todayDate;
            int currentTodayMins = prefs.getInt(todayKey, 0);
            editor.putInt(todayKey, currentTodayMins + 1);

            // Add to Subject Breakdown
            String subject = getCurrentSubject(context);
            String subjKey = SUBJECT_PREFIX + todayDate + "_" + subject;
            int subjMins = prefs.getInt(subjKey, 0);
            editor.putInt(subjKey, subjMins + 1);

            updateStreak(prefs, todayDate);
        }

        editor.putInt(KEY_UNRECORDED_SECONDS, unrecorded);
        editor.apply();

        if (remaining <= 0L) {
            completeSession(context);
        }

        return remaining;
    }

    private static void updateStreak(SharedPreferences prefs, String todayDate) {
        String lastDate = prefs.getString(KEY_LAST_STUDY_DATE, "");
        int streak = prefs.getInt(KEY_STREAK_DAYS, 0);

        if (!todayDate.equals(lastDate)) {
            if (lastDate.isEmpty()) {
                streak = 1;
            } else {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date d1 = sdf.parse(lastDate);
                    Date d2 = sdf.parse(todayDate);
                    long diffDays = (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24);
                    if (diffDays == 1) {
                        streak += 1;
                    } else if (diffDays > 1) {
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
        return getPrefs(context).getInt(KEY_STREAK_DAYS, 0);
    }

    public static void completeSession(Context context) {
        getPrefs(context).edit()
                .putBoolean(KEY_SESSION_ACTIVE, false)
                .putBoolean(KEY_SESSION_PAUSED, false)
                .putLong(KEY_REMAINING_SECONDS, 0L)
                .putInt(KEY_UNRECORDED_SECONDS, 0)
                .putInt(KEY_PAUSE_COUNT, 0)
                .apply();
    }

    public static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    public static int getTodayStudyMinutes(Context context) {
        String todayKey = HISTORY_PREFIX + getTodayDateString();
        return getPrefs(context).getInt(todayKey, 0);
    }

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

    public static String getSubjectBreakdownText(Context context) {
        Map<String, ?> allEntries = getPrefs(context).getAll();
        java.util.HashMap<String, Integer> subjectTotals = new java.util.HashMap<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(SUBJECT_PREFIX) && entry.getValue() instanceof Integer) {
                String subName = entry.getKey().substring(entry.getKey().lastIndexOf("_") + 1);
                int mins = (Integer) entry.getValue();
                int existing = subjectTotals.containsKey(subName) ? subjectTotals.get(subName) : 0;
                subjectTotals.put(subName, existing + mins);
            }
        }

        if (subjectTotals.isEmpty()) {
            return "Study Time by Subject:\nNo subjects recorded yet.";
        }

        StringBuilder sb = new StringBuilder("Study Time by Subject:\n");
        for (Map.Entry<String, Integer> entry : subjectTotals.entrySet()) {
            sb.append("• ").append(entry.getKey()).append(": ").append(formatMinutesReadable(entry.getValue())).append("\n");
        }
        return sb.toString().trim();
    }

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
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

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
