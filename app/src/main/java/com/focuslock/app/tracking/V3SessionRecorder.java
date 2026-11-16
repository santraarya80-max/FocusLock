
package com.focuslock.app.tracking;

import android.content.Context;
import android.content.SharedPreferences;

import com.focuslock.app.database.StudyRepository;

public final class V3SessionRecorder {

    private static final String PREFS = "FocusLockV3SessionTracking";
    private static final String KEY_START = "session_start";
    private static final String KEY_SUBJECT = "session_subject";
    private static final String KEY_RECORDED_START = "last_recorded_start";

    private V3SessionRecorder() {
    }

    public static void startSession(Context context, String subject) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        );

        prefs.edit()
                .putLong(KEY_START, System.currentTimeMillis())
                .putString(KEY_SUBJECT,
                        subject == null ? "General Study" : subject.trim())
                .apply();
    }

    public static void recordCompletedSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        );

        long startTime = prefs.getLong(KEY_START, 0L);

        if (startTime <= 0L) return;

        if (prefs.getLong(KEY_RECORDED_START, 0L) == startTime) {
            return;
        }

        long endTime = System.currentTimeMillis();

        int durationMinutes = (int) Math.max(
                1L, (endTime - startTime) / 60000L
        );

        String subject = prefs.getString(
                KEY_SUBJECT, "General Study"
        );

        StudyRepository repository = new StudyRepository(context);

        try {
            repository.addStudySession(
                    null,
                    null,
                    null,
                    subject,
                    startTime,
                    endTime,
                    durationMinutes,
                    "Completed focus timer"
            );

            prefs.edit()
                    .putLong(KEY_RECORDED_START, startTime)
                    .remove(KEY_START)
                    .remove(KEY_SUBJECT)
                    .apply();

        } finally {
            repository.close();
        }
    }
}
