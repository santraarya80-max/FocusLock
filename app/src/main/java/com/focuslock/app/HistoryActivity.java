
package com.focuslock.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class HistoryActivity extends Activity {

    private final int BACKGROUND = Color.rgb(5, 22, 17);
    private final int CARD = Color.rgb(13, 43, 33);
    private final int GREEN = Color.rgb(75, 220, 166);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(157, 190, 176);

    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BACKGROUND);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(22), dp(18), dp(30));

        scrollView.addView(content);
        setContentView(scrollView);

        buildAnalyticsScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (content != null) {
            buildAnalyticsScreen();
        }
    }

    private void buildAnalyticsScreen() {
        content.removeAllViews();

        addText("FOCUSLOCK ANALYTICS", 11, GREEN, true);
        addText("Your Study Performance", 27, WHITE, true);
        addText(
                "Understand where your study time goes.",
                13, MUTED, false
        );

        addSpace(20);

        int totalMinutes =
                StorageHelper.getTotalStudyMinutes(this);

        int todayMinutes =
                StorageHelper.getTodayStudyMinutes(this);

        int streak =
                StorageHelper.getStreakDays(this);

        addCardTitle("OVERALL PERFORMANCE");

        addText(
                "Total Study Time",
                13, MUTED, false
        );

        addText(
                StorageHelper.formatMinutesReadable(totalMinutes),
                29, GREEN, true
        );

        addSpace(12);

        addText(
                "Today's Study: "
                        + StorageHelper.formatMinutesReadable(todayMinutes),
                16, WHITE, true
        );

        addText(
                "Current Study Streak: " + streak + " days",
                14, MUTED, false
        );

        addSpace(22);

        addCardTitle("SUBJECT-WISE PERFORMANCE");

        List<String> subjects =
                SubjectStorage.getSubjects(this);

        if (subjects.isEmpty()) {
            addText(
                    "No saved subjects yet. Add subjects and chapters "
                            + "to begin tracking your progress.",
                    14, MUTED, false
            );
        } else {
            for (String subject : subjects) {
                addSubjectSection(subject);
            }
        }

        addSpace(22);

        addCardTitle("DAILY STUDY HISTORY");

        List<String> history =
                StorageHelper.getStudyHistoryList(this);

        for (String item : history) {
            addText(item, 14, WHITE, false);
            addDivider();
        }

        addSpace(24);

        Button backButton = new Button(this);
        backButton.setText("BACK TO FOCUSLOCK");
        backButton.setTextColor(WHITE);
        backButton.setTextSize(14);
        backButton.setAllCaps(false);
        backButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.rgb(23, 99, 76)
                )
        );

        content.addView(
                backButton,
                new LinearLayout.LayoutParams(
                        -1, dp(52)
                )
        );

        backButton.setOnClickListener(v -> finish());
    }

    private void addSubjectSection(String subject) {
        addSpace(12);

        TextView subjectTitle =
                makeText(subject, 19, GREEN, true);

        content.addView(subjectTitle);

        List<String> chapters =
                SubjectStorage.getChapters(this, subject);

        if (chapters.isEmpty()) {
            addText(
                    "No chapters saved for this subject.",
                    13, MUTED, false
            );
            return;
        }

        int subjectTotal = 0;

        for (String chapter : chapters) {
            subjectTotal +=
                    StorageHelper.getChapterStudyMinutes(
                            this, subject, chapter
                    );
        }

        addText(
                "Total tracked chapter time: "
                        + StorageHelper.formatMinutesReadable(subjectTotal),
                13, MUTED, true
        );

        for (String chapter : chapters) {
            int total =
                    StorageHelper.getChapterStudyMinutes(
                            this, subject, chapter
                    );

            int today =
                    StorageHelper.getTodayChapterStudyMinutes(
                            this, subject, chapter
                    );

            LinearLayout chapterCard =
                    new LinearLayout(this);

            chapterCard.setOrientation(
                    LinearLayout.VERTICAL
            );

            chapterCard.setPadding(
                    dp(14), dp(12), dp(14), dp(12)
            );

            chapterCard.setBackground(
                    roundedBackground(CARD, 12)
            );

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            -1,
                            -2
                    );

            cardParams.setMargins(
                    0, dp(7), 0, dp(4)
            );

            content.addView(chapterCard, cardParams);

            TextView chapterName =
                    makeText(chapter, 15, WHITE, true);

            chapterCard.addView(chapterName);

            TextView totalText = makeText(
                    "Total: "
                            + StorageHelper.formatMinutesReadable(total),
                    13, GREEN, false
            );

            totalText.setPadding(0, dp(6), 0, 0);
            chapterCard.addView(totalText);

            TextView todayText = makeText(
                    "Today: "
                            + StorageHelper.formatMinutesReadable(today),
                    12, MUTED, false
            );

            todayText.setPadding(0, dp(3), 0, 0);
            chapterCard.addView(todayText);
        }
    }

    private void addCardTitle(String title) {
        TextView heading =
                makeText(title, 11, GREEN, true);

        heading.setPadding(0, dp(12), 0, dp(10));

        content.addView(heading);

        ViewLine line = new ViewLine(this);
        content.addView(
                line,
                new LinearLayout.LayoutParams(
                        -1, dp(1)
                )
        );
    }

    private void addText(
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView text = makeText(
                value, size, color, bold
        );

        text.setPadding(0, dp(5), 0, dp(5));
        content.addView(text);
    }

    private TextView makeText(
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);

        if (bold) {
            text.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        return text;
    }

    private void addDivider() {
        ViewLine line = new ViewLine(this);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1, dp(1)
                );

        params.setMargins(0, dp(5), 0, dp(5));
        content.addView(line, params);
    }

    private void addSpace(int height) {
        android.view.View space =
                new android.view.View(this);

        content.addView(
                space,
                new LinearLayout.LayoutParams(
                        1, dp(height)
                )
        );
    }

    private GradientDrawable roundedBackground(
            int color,
            int radius
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));

        return drawable;
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density
        );
    }

    private static class ViewLine
            extends android.view.View {

        ViewLine(Activity activity) {
            super(activity);
            setBackgroundColor(
                    Color.rgb(37, 75, 59)
            );
        }
    }
}
