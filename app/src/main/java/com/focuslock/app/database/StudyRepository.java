
package com.focuslock.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class StudyRepository {

    private final StudyDatabaseHelper helper;

    public StudyRepository(Context context) {
        helper = new StudyDatabaseHelper(context.getApplicationContext());
    }

    // SUBJECTS

    public long addSubject(String name, String color) {
        if (name == null || name.trim().isEmpty()) return -1;

        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("color", color);
        values.put("created_at", System.currentTimeMillis());

        return helper.getWritableDatabase().insertWithOnConflict(
            "subjects", null, values,
            SQLiteDatabase.CONFLICT_IGNORE
        );
    }

    public Cursor getAllSubjects() {
        return helper.getReadableDatabase().query(
            "subjects", null, null, null,
            null, null, "name ASC"
        );
    }

    // CHAPTERS

    public long addChapter(long subjectId, String name) {
        if (name == null || name.trim().isEmpty()) return -1;

        ContentValues values = new ContentValues();
        values.put("subject_id", subjectId);
        values.put("name", name.trim());
        values.put("created_at", System.currentTimeMillis());

        return helper.getWritableDatabase().insertWithOnConflict(
            "chapters", null, values,
            SQLiteDatabase.CONFLICT_IGNORE
        );
    }

    public Cursor getChapters(long subjectId) {
        return helper.getReadableDatabase().query(
            "chapters", null, "subject_id = ?",
            new String[]{String.valueOf(subjectId)},
            null, null, "id ASC"
        );
    }

    // TOPICS

    public long addTopic(long chapterId, String name) {
        if (name == null || name.trim().isEmpty()) return -1;

        ContentValues values = new ContentValues();
        values.put("chapter_id", chapterId);
        values.put("name", name.trim());
        values.put("created_at", System.currentTimeMillis());

        return helper.getWritableDatabase().insertWithOnConflict(
            "topics", null, values,
            SQLiteDatabase.CONFLICT_IGNORE
        );
    }

    public Cursor getTopics(long chapterId) {
        return helper.getReadableDatabase().query(
            "topics", null, "chapter_id = ?",
            new String[]{String.valueOf(chapterId)},
            null, null, "id ASC"
        );
    }

    public int markTopicCompleted(long topicId, boolean completed) {
        ContentValues values = new ContentValues();
        values.put("is_completed", completed ? 1 : 0);

        return helper.getWritableDatabase().update(
            "topics", values, "id = ?",
            new String[]{String.valueOf(topicId)}
        );
    }

    public int markChapterCompleted(long chapterId, boolean completed) {
        ContentValues values = new ContentValues();
        values.put("is_completed", completed ? 1 : 0);

        return helper.getWritableDatabase().update(
            "chapters", values, "id = ?",
            new String[]{String.valueOf(chapterId)}
        );
    }

    // STUDY SESSIONS

    public long addStudySession(
        Long subjectId,
        Long chapterId,
        Long topicId,
        String subjectName,
        long startTime,
        long endTime,
        int durationMinutes,
        String notes
    ) {
        ContentValues values = new ContentValues();

        if (subjectId != null) values.put("subject_id", subjectId);
        if (chapterId != null) values.put("chapter_id", chapterId);
        if (topicId != null) values.put("topic_id", topicId);

        values.put("subject_name", subjectName);
        values.put("start_time", startTime);
        values.put("end_time", endTime);
        values.put("duration_minutes", durationMinutes);
        values.put("notes", notes);

        return helper.getWritableDatabase().insert(
            "study_sessions", null, values
        );
    }

    public Cursor getAllStudySessions() {
        return helper.getReadableDatabase().query(
            "study_sessions", null, null, null,
            null, null, "start_time DESC"
        );
    }

    // REVISION CENTRE

    public long scheduleRevision(long topicId, long dueAt) {
        ContentValues values = new ContentValues();
        values.put("topic_id", topicId);
        values.put("due_at", dueAt);
        values.put("interval_days", 1);

        return helper.getWritableDatabase().insert(
            "revision_items", null, values
        );
    }

    public Cursor getDueRevisions(long currentTime) {
        return helper.getReadableDatabase().query(
            "revision_items", null, "due_at <= ?",
            new String[]{String.valueOf(currentTime)},
            null, null, "due_at ASC"
        );
    }

    public void completeRevision(long revisionId, int nextIntervalDays) {
        if (nextIntervalDays < 1) return;

        SQLiteDatabase db = helper.getWritableDatabase();

        long now = System.currentTimeMillis();
        long nextDue = now
            + (long) nextIntervalDays * 24 * 60 * 60 * 1000;

        db.execSQL(
            "UPDATE revision_items SET " +
            "last_reviewed_at = ?, " +
            "review_count = review_count + 1, " +
            "interval_days = ?, " +
            "due_at = ? " +
            "WHERE id = ?",
            new Object[]{
                now,
                nextIntervalDays,
                nextDue,
                revisionId
            }
        );
    }

    public void close() {
        helper.close();
    }
}
