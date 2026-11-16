
package com.focuslock.app.tracking;

import android.content.Context;
import android.content.SharedPreferences;

import com.focuslock.app.database.StudyRepository;

public final class V3SessionRecorder {

    private static final String PREFS = "FocusLockV3SessionTracking";
    private static final String KEY_START = "session_start";
    private static final String KEY_SUBJECT = "session_subject";
    private static final String KEY_DURATION = "session_duration";
    private static final String KEY_RECORDED_START = "last_recorded_start";

    private V3SessionRecorder() {
    }

    public static void startSession(
            Context context,
            String subject,
            int plannedMinutes
    ) {
        if (plannedMinutes <= 0) return;

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        );

        long startTime = System.currentTimeMillis();

        prefs.edit()
                .putLong(KEY_START, startTime)
                .putString(KEY_SUBJECT,
                        subject == null || subject.trim().isEmpty()
                                ? "General Study" : subject.trim())
                .putInt(KEY_DURATION, plannedMinutes)
                .apply();
    }

    public static void recordCompletedSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        );

        long startTime = prefs.getLong(KEY_START, 0L);
        int durationMinutes = prefs.getInt(KEY_DURATION, 0);

        if (startTime <= 0L || durationMinutes <= 0) return;

        if (prefs.getLong(KEY_RECORDED_START, 0L) == startTime) {
            return;
        }

        long endTime = System.currentTimeMillis();

        String subject = prefs.getString(
                KEY_SUBJECT, "General Study"
        );

        StudyRepository repository = new StudyRepository(context);

        try {
            long result = repository.addStudySession(
                    null,
                    null,
                    null,
                    subject,
                    startTime,
                    endTime,
                    durationMinutes,
                    "Completed focus timer"
            );

            if (result != -1L) {
                prefs.edit()
                        .putLong(KEY_RECORDED_START, startTime)
                        .remove(KEY_START)
                        .remove(KEY_SUBJECT)
                        .remove(KEY_DURATION)
                        .apply();
            }

        } finally {
            repository.close();
        }
    }
}
