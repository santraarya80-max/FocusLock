
package com.focuslock.app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StudyDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "focuslock_v3.db";
    private static final int DATABASE_VERSION = 1;

    public StudyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
            "CREATE TABLE subjects (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL UNIQUE, " +
            "color TEXT, " +
            "created_at INTEGER NOT NULL)"
        );

        db.execSQL(
            "CREATE TABLE chapters (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "subject_id INTEGER NOT NULL, " +
            "name TEXT NOT NULL, " +
            "is_completed INTEGER NOT NULL DEFAULT 0, " +
            "created_at INTEGER NOT NULL, " +
            "FOREIGN KEY(subject_id) REFERENCES subjects(id) " +
            "ON DELETE CASCADE, " +
            "UNIQUE(subject_id, name))"
        );

        db.execSQL(
            "CREATE TABLE topics (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "chapter_id INTEGER NOT NULL, " +
            "name TEXT NOT NULL, " +
            "is_completed INTEGER NOT NULL DEFAULT 0, " +
            "created_at INTEGER NOT NULL, " +
            "FOREIGN KEY(chapter_id) REFERENCES chapters(id) " +
            "ON DELETE CASCADE, " +
            "UNIQUE(chapter_id, name))"
        );

        db.execSQL(
            "CREATE TABLE study_sessions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "subject_id INTEGER, " +
            "chapter_id INTEGER, " +
            "topic_id INTEGER, " +
            "subject_name TEXT NOT NULL, " +
            "start_time INTEGER NOT NULL, " +
            "end_time INTEGER NOT NULL, " +
            "duration_minutes INTEGER NOT NULL, " +
            "notes TEXT, " +
            "FOREIGN KEY(subject_id) REFERENCES subjects(id) " +
            "ON DELETE SET NULL, " +
            "FOREIGN KEY(chapter_id) REFERENCES chapters(id) " +
            "ON DELETE SET NULL, " +
            "FOREIGN KEY(topic_id) REFERENCES topics(id) " +
            "ON DELETE SET NULL)"
        );

        db.execSQL(
            "CREATE TABLE revision_items (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "topic_id INTEGER NOT NULL, " +
            "due_at INTEGER NOT NULL, " +
            "interval_days INTEGER NOT NULL DEFAULT 1, " +
            "review_count INTEGER NOT NULL DEFAULT 0, " +
            "last_reviewed_at INTEGER, " +
            "FOREIGN KEY(topic_id) REFERENCES topics(id) " +
            "ON DELETE CASCADE)"
        );

        db.execSQL(
            "CREATE INDEX index_chapters_subject " +
            "ON chapters(subject_id)"
        );

        db.execSQL(
            "CREATE INDEX index_topics_chapter " +
            "ON topics(chapter_id)"
        );

        db.execSQL(
            "CREATE INDEX index_sessions_start " +
            "ON study_sessions(start_time)"
        );

        db.execSQL(
            "CREATE INDEX index_revision_due " +
            "ON revision_items(due_at)"
        );
    }

    @Override
    public void onUpgrade(
        SQLiteDatabase db,
        int oldVersion,
        int newVersion
    ) {
        // Future database upgrades will be handled here.
        // Existing data must be preserved.
    }
}
