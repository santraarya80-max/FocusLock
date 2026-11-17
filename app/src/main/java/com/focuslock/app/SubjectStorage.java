
package com.focuslock.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SubjectStorage {

    private static final String PREFS =
            "FocusLockSubjectsV3";

    private static final String SUBJECTS =
            "saved_subjects";

    private SubjectStorage() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );
    }

    private static String chapterKey(String subject) {
        String encoded = Base64.encodeToString(
                subject.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_WRAP
        );

        return "chapters_" + encoded;
    }

    public static List<String> getSubjects(Context context) {
        Set<String> saved = prefs(context).getStringSet(
                SUBJECTS,
                new HashSet<>()
        );

        List<String> result = new ArrayList<>(saved);
        Collections.sort(
                result,
                String.CASE_INSENSITIVE_ORDER
        );

        return result;
    }

    public static boolean addSubject(
            Context context,
            String subject
    ) {
        if (subject == null) return false;

        subject = subject.trim();

        if (subject.isEmpty() || subject.length() > 40) {
            return false;
        }

        List<String> existing = getSubjects(context);

        for (String item : existing) {
            if (item.equalsIgnoreCase(subject)) {
                return false;
            }
        }

        Set<String> updated = new HashSet<>(existing);
        updated.add(subject);

        prefs(context).edit()
                .putStringSet(SUBJECTS, updated)
                .apply();

        return true;
    }

    public static boolean removeSubject(
            Context context,
            String subject
    ) {
        if (subject == null) return false;

        Set<String> updated = new HashSet<>(
                getSubjects(context)
        );

        boolean removed = false;

        for (String item : new HashSet<>(updated)) {
            if (item.equalsIgnoreCase(subject)) {
                updated.remove(item);
                prefs(context).edit()
                        .remove(chapterKey(item))
                        .putStringSet(SUBJECTS, updated)
                        .apply();
                removed = true;
                break;
            }
        }

        return removed;
    }

    public static List<String> getChapters(
            Context context,
            String subject
    ) {
        Set<String> saved = prefs(context).getStringSet(
                chapterKey(subject),
                new HashSet<>()
        );

        List<String> result = new ArrayList<>(saved);

        Collections.sort(
                result,
                String.CASE_INSENSITIVE_ORDER
        );

        return result;
    }

    public static boolean addChapter(
            Context context,
            String subject,
            String chapter
    ) {
        if (subject == null || chapter == null) {
            return false;
        }

        chapter = chapter.trim();

        if (chapter.isEmpty() || chapter.length() > 60) {
            return false;
        }

        List<String> existing =
                getChapters(context, subject);

        for (String item : existing) {
            if (item.equalsIgnoreCase(chapter)) {
                return false;
            }
        }

        Set<String> updated = new HashSet<>(existing);
        updated.add(chapter);

        prefs(context).edit()
                .putStringSet(chapterKey(subject), updated)
                .apply();

        return true;
    }

    public static boolean removeChapter(
            Context context,
            String subject,
            String chapter
    ) {
        if (subject == null || chapter == null) {
            return false;
        }

        Set<String> updated = new HashSet<>(
                getChapters(context, subject)
        );

        boolean removed = false;

        for (String item : new HashSet<>(updated)) {
            if (item.equalsIgnoreCase(chapter)) {
                updated.remove(item);
                removed = true;
                break;
            }
        }

        if (removed) {
            prefs(context).edit()
                    .putStringSet(chapterKey(subject), updated)
                    .apply();
        }

        return removed;
    }
}
