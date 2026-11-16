
package com.focuslock.app.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class V2DataMigrator {

    private static final String OLD_PREFS = "FocusLockDatabaseV2";
    private static final String MIGRATION_KEY = "v2_history_imported";

    private V2DataMigrator() {
    }

    public static boolean migrate(Context context) {

        SharedPreferences oldPrefs =
                context.getSharedPreferences(
                        OLD_PREFS, Context.MODE_PRIVATE
                );

        Map<String, ?> allEntries = oldPrefs.getAll();

        HashMap<String, Integer> dailyTotals = new HashMap<>();
        HashMap<String, HashMap<String, Integer>> subjectTotals =
                new HashMap<>();

        Set<String> dates = new HashSet<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {

            String key = entry.getKey();
            Object value = entry.getValue();

            if (!(value instanceof Integer)) continue;

            int minutes = (Integer) value;
            if (minutes <= 0) continue;

            if (key.startsWith("history_")) {

                String date = key.substring("history_".length());

                dailyTotals.put(date, minutes);
                dates.add(date);

            } else if (key.startsWith("subject_")) {

                String remainder =
                        key.substring("subject_".length());

                if (remainder.length() < 12) continue;

                String date = remainder.substring(0, 10);

                if (remainder.charAt(10) != '_') continue;

                String subject = remainder.substring(11);

                if (subject.trim().isEmpty()) continue;

                HashMap<String, Integer> subjects =
                        subjectTotals.get(date);

                if (subjects == null) {
                    subjects = new HashMap<>();
                    subjectTotals.put(date, subjects);
                }

                subjects.put(subject, minutes);
                dates.add(date);
            }
        }

        StudyDatabaseHelper helper =
                new StudyDatabaseHelper(context);

        SQLiteDatabase db = helper.getWritableDatabase();

        try {

            db.beginTransaction();

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS migration_log (" +
                    "migration_key TEXT PRIMARY KEY, " +
                    "completed_at INTEGER NOT NULL)"
            );

            Cursor check = db.query(
                    "migration_log",
                    new String[]{"migration_key"},
                    "migration_key = ?",
                    new String[]{MIGRATION_KEY},
                    null, null, null
            );

            boolean alreadyMigrated = check.moveToFirst();
            check.close();

            if (alreadyMigrated) {
                db.setTransactionSuccessful();
                db.endTransaction();
                return true;
            }

            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            dateFormat.setLenient(false);

            for (String date : dates) {

                long dayStart;

                try {
                    Date parsed = dateFormat.parse(date);
                    if (parsed == null) continue;
                    dayStart = parsed.getTime();

                } catch (Exception e) {
                    continue;
                }

                HashMap<String, Integer> subjects =
                        subjectTotals.get(date);

                int importedMinutes = 0;
                long offsetMinutes = 0;

                if (subjects != null) {

                    for (Map.Entry<String, Integer> subject :
                            subjects.entrySet()) {

                        int minutes = subject.getValue();

                        insertSession(
                                db,
                                subject.getKey(),
                                dayStart + offsetMinutes * 60000L,
                                minutes
                        );

                        importedMinutes += minutes;
                        offsetMinutes += minutes;
                    }
                }

                int dailyMinutes =
                        dailyTotals.containsKey(date)
                                ? dailyTotals.get(date) : 0;

                int unallocated =
                        dailyMinutes - importedMinutes;

                if (unallocated > 0) {

                    insertSession(
                            db,
                            "Unallocated V2 History",
                            dayStart + offsetMinutes * 60000L,
                            unallocated
                    );
                }
            }

            ContentValues marker = new ContentValues();
            marker.put("migration_key", MIGRATION_KEY);
            marker.put("completed_at", System.currentTimeMillis());

            db.insertOrThrow(
                    "migration_log",
                    null,
                    marker
            );

            db.setTransactionSuccessful();
            db.endTransaction();

            return true;

        } catch (Exception e) {

            if (db.inTransaction()) {
                db.endTransaction();
            }

            return false;

        } finally {
            helper.close();
        }
    }

    private static void insertSession(
            SQLiteDatabase db,
            String subject,
            long startTime,
            int durationMinutes
    ) {

        if (durationMinutes <= 0) return;

        ContentValues values = new ContentValues();

        values.put("subject_name", subject);
        values.put("start_time", startTime);
        values.put(
                "end_time",
                startTime + durationMinutes * 60000L
        );
        values.put("duration_minutes", durationMinutes);
        values.put("notes", "Imported from FocusLock V2 history");

        db.insertOrThrow(
                "study_sessions",
                null,
                values
        );
    }
}
