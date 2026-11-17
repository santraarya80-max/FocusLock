
package com.focuslock.app;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends Activity {

    private final int BACKGROUND = Color.rgb(5, 22, 17);
    private final int CARD = Color.rgb(13, 43, 33);
    private final int GREEN = Color.rgb(75, 220, 166);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(157, 190, 176);
    private final int LINE = Color.rgb(37, 75, 59);

    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BACKGROUND);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(24), dp(20), dp(30));

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

        addText("FOCUSLOCK ANALYTICS", 12, GREEN, true);
        addText("Your Study Performance", 27, WHITE, true);
        addText(
                "Understand where your study time goes.",
                15, MUTED, false
        );

        addSpace(22);

        int totalMinutes =
                StorageHelper.getTotalStudyMinutes(this);

        int todayMinutes =
                StorageHelper.getTodayStudyMinutes(this);

        int streak =
                StorageHelper.getStreakDays(this);

        addSectionTitle("OVERALL PERFORMANCE");

        LinearLayout overview = createCard();

        addCardText(
                overview,
                "TOTAL STUDY TIME",
                11, MUTED, true
        );

        addCardText(
                overview,
                formatMinutes(totalMinutes),
                30, GREEN, true
        );

        addSpaceInside(overview, 12);

        addCardText(
                overview,
                "Today's Study",
                12, MUTED, false
        );

        addCardText(
                overview,
                formatMinutes(todayMinutes),
                20, WHITE, true
        );

        addSpaceInside(overview, 8);

        addCardText(
                overview,
                "Current Study Streak",
                12, MUTED, false
        );

        addCardText(
                overview,
                streak + (streak == 1 ? " day" : " days"),
                20, GREEN, true
        );

        addSpace(24);

        addSectionTitle("LAST 7 DAYS");

        int weeklyTotal = getWeeklyTotal();

        LinearLayout weeklyCard = createCard();

        addCardText(
                weeklyCard,
                "YOUR WEEKLY STUDY TIME",
                11, MUTED, true
        );

        addCardText(
                weeklyCard,
                formatMinutes(weeklyTotal),
                25, GREEN, true
        );

        WeeklyChart chart = new WeeklyChart(this);

        weeklyCard.addView(
                chart,
                new LinearLayout.LayoutParams(
                        -1, dp(190)
                )
        );

        addSpace(24);

        addSectionTitle("SUBJECT-WISE PERFORMANCE");

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

        addSpace(24);

        addSectionTitle("DAILY STUDY HISTORY");

        List<String> history =
                StorageHelper.getStudyHistoryList(this);

        LinearLayout historyCard = createCard();

        for (int i = 0; i < history.size(); i++) {
            addCardText(
                    historyCard,
                    history.get(i),
                    14, WHITE, false
            );

            if (i < history.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(LINE);

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                -1, dp(1)
                        );

                params.setMargins(0, dp(8), 0, dp(8));
                historyCard.addView(divider, params);
            }
        }

        addSpace(24);

        Button backButton = new Button(this);
        backButton.setText("BACK TO FOCUSLOCK");
        backButton.setTextColor(WHITE);
        backButton.setTextSize(15);
        backButton.setAllCaps(false);

        backButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.rgb(23, 99, 76)
                )
        );

        content.addView(
                backButton,
                new LinearLayout.LayoutParams(
                        -1, dp(56)
                )
        );

        backButton.setOnClickListener(v -> finish());
    }

    private void addSubjectSection(String subject) {
        List<String> chapters =
                SubjectStorage.getChapters(this, subject);

        int subjectTotal = 0;
        int subjectToday = 0;

        for (String chapter : chapters) {
            subjectTotal +=
                    StorageHelper.getChapterStudyMinutes(
                            this, subject, chapter
                    );

            subjectToday +=
                    StorageHelper.getTodayChapterStudyMinutes(
                            this, subject, chapter
                    );
        }

        LinearLayout subjectCard = createCard();

        addCardText(
                subjectCard,
                subject,
                19, GREEN, true
        );

        addCardText(
                subjectCard,
                "Total study time: " + formatMinutes(subjectTotal),
                13, WHITE, true
        );

        addCardText(
                subjectCard,
                "Studied today: " + formatMinutes(subjectToday),
                12, MUTED, false
        );

        addSpaceInside(subjectCard, 8);

        if (chapters.isEmpty()) {
            addCardText(
                    subjectCard,
                    "No chapters saved for this subject.",
                    13, MUTED, false
            );
            return;
        }

        for (String chapter : chapters) {
            int total =
                    StorageHelper.getChapterStudyMinutes(
                            this, subject, chapter
                    );

            int today =
                    StorageHelper.getTodayChapterStudyMinutes(
                            this, subject, chapter
                    );

            addCardText(
                    subjectCard,
                    chapter,
                    14, WHITE, true
            );

            addCardText(
                    subjectCard,
                    "Total: " + formatMinutes(total)
                            + "   •   Today: " + formatMinutes(today),
                    12, MUTED, false
            );

            addSpaceInside(subjectCard, 5);
        }
    }

    private int getWeeklyTotal() {
        SharedPreferences prefs =
                getSharedPreferences(
                        "FocusLockDatabaseV2",
                        MODE_PRIVATE
                );

        int total = 0;
        Calendar calendar = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            String date = new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
            ).format(calendar.getTime());

            total += prefs.getInt("history_" + date, 0);
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }

        return total;
    }

    private int getMinutesForDate(Calendar calendar) {
        String date = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
        ).format(calendar.getTime());

        SharedPreferences prefs =
                getSharedPreferences(
                        "FocusLockDatabaseV2",
                        MODE_PRIVATE
                );

        return prefs.getInt("history_" + date, 0);
    }

    private String formatMinutes(int minutes) {
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " min" : " mins");
        }

        int hours = minutes / 60;
        int remaining = minutes % 60;

        String result = hours + (hours == 1 ? " hr" : " hrs");

        if (remaining > 0) {
            result += " " + remaining
                    + (remaining == 1 ? " min" : " mins");
        }

        return result;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(
                dp(16), dp(14), dp(16), dp(14)
        );
        card.setBackground(roundedBackground(CARD, 16));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1, -2
                );

        params.setMargins(0, dp(7), 0, dp(5));
        content.addView(card, params);

        return card;
    }

    private void addSectionTitle(String title) {
        TextView heading =
                makeText(title, 12, GREEN, true);

        heading.setPadding(0, dp(8), 0, dp(9));
        content.addView(heading);

        View divider = new View(this);
        divider.setBackgroundColor(LINE);

        content.addView(
                divider,
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
        TextView text = makeText(value, size, color, bold);
        text.setPadding(0, dp(5), 0, dp(5));
        content.addView(text);
    }

    private void addCardText(
            LinearLayout parent,
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView text = makeText(value, size, color, bold);
        text.setPadding(0, dp(4), 0, dp(4));
        parent.addView(text);
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

    private void addSpaceInside(
            LinearLayout parent,
            int height
    ) {
        View space = new View(this);

        parent.addView(
                space,
                new LinearLayout.LayoutParams(
                        1, dp(height)
                )
        );
    }

    private void addSpace(int height) {
        View space = new View(this);

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

    private class WeeklyChart extends View {

        private final Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG
        );

        WeeklyChart(Activity activity) {
            super(activity);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float density =
                    getResources().getDisplayMetrics().density;

            float left = 12 * density;
            float right = getWidth() - 12 * density;
            float top = 18 * density;
            float bottom = getHeight() - 30 * density;

            float chartHeight = bottom - top;
            float chartWidth = right - left;

            Calendar calendar = Calendar.getInstance();
            int[] values = new int[7];
            int max = 1;

            for (int i = 6; i >= 0; i--) {
                values[i] = getMinutesForDate(calendar);
                max = Math.max(max, values[i]);
                calendar.add(Calendar.DAY_OF_YEAR, -1);
            }

            paint.setColor(LINE);
            paint.setStrokeWidth(1 * density);

            for (int i = 0; i < 3; i++) {
                float y = top + (chartHeight * i / 2f);
                canvas.drawLine(left, y, right, y, paint);
            }

            float slotWidth = chartWidth / 7f;
            float barWidth = slotWidth * 0.48f;

            calendar = Calendar.getInstance();
            SimpleDateFormat dayFormat =
                    new SimpleDateFormat("EE", Locale.getDefault());

            for (int i = 6; i >= 0; i--) {
                int value = values[i];

                float barHeight =
                        chartHeight * value / max;

                float x = left + (6 - i) * slotWidth
                        + (slotWidth - barWidth) / 2f;

                float y = bottom - barHeight;

                paint.setColor(GREEN);

                RectF bar = new RectF(
                        x,
                        Math.max(top, y),
                        x + barWidth,
                        bottom
                );

                canvas.drawRoundRect(
                        bar,
                        5 * density,
                        5 * density,
                        paint
                );

                paint.setColor(MUTED);
                paint.setTextSize(10 * density);
                paint.setTextAlign(Paint.Align.CENTER);

                String day = dayFormat.format(
                        calendar.getTime()
                );

                canvas.drawText(
                        day,
                        x + barWidth / 2f,
                        getHeight() - 7 * density,
                        paint
                );

                calendar.add(Calendar.DAY_OF_YEAR, -1);
            }
        }
    }
}
