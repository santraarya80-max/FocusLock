package com.focuslock.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class SubjectActivity extends Activity {

    private final int BG = Color.rgb(4, 20, 16);
    private final int CARD = Color.rgb(13, 39, 29);
    private final int GREEN = Color.rgb(75, 232, 174);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(160, 184, 171);

    private LinearLayout subjectContainer;
    private TextView countLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));

        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        Button back = makeButton("‹", false);
        back.setTextSize(28);
        back.setOnClickListener(v -> finish());

        header.addView(back, new LinearLayout.LayoutParams(
                dp(48), dp(48)
        ));

        TextView title = makeText(
                "Subjects & Chapters", 23, WHITE, true
        );

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        titleParams.leftMargin = dp(10);
        header.addView(title, titleParams);
        root.addView(header);

        TextView subtitle = makeText(
                "Organise your learning, one topic at a time.",
                13, MUTED, false
        );

        LinearLayout.LayoutParams subParams =
                new LinearLayout.LayoutParams(-1, -2);

        subParams.topMargin = dp(8);
        subParams.bottomMargin = dp(22);
        root.addView(subtitle, subParams);

        Button addSubject = makeButton(
                "+  Add New Subject", true
        );

        addSubject.setOnClickListener(
                v -> showInputDialog(
                        "Create Subject",
                        "Enter subject name",
                        value -> {
                            if (SubjectStorage.addSubject(
                                    this, value
                            )) {
                                refreshSubjects();
                                toast("Subject added");
                            } else {
                                toast("Invalid or duplicate subject");
                            }
                        }
                )
        );

        root.addView(addSubject);

        countLabel = makeText(
                "YOUR SUBJECTS", 12, GREEN, true
        );

        LinearLayout.LayoutParams countParams =
                new LinearLayout.LayoutParams(-1, -2);

        countParams.topMargin = dp(26);
        countParams.bottomMargin = dp(12);
        root.addView(countLabel, countParams);

        subjectContainer = new LinearLayout(this);
        subjectContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(subjectContainer);

        refreshSubjects();
    }

    private void refreshSubjects() {
        subjectContainer.removeAllViews();

        List<String> subjects =
                SubjectStorage.getSubjects(this);

        countLabel.setText(
                "YOUR SUBJECTS  •  " + subjects.size()
        );

        if (subjects.isEmpty()) {
            TextView empty = makeText(
                    "No subjects yet.\n\nAdd your first subject to start organising your chapters.",
                    14,
                    MUTED,
                    false
            );

            empty.setGravity(Gravity.CENTER);
            empty.setPadding(
                    dp(20), dp(35), dp(20), dp(35)
            );

            empty.setBackground(background(CARD, 18));

            subjectContainer.addView(empty);
            return;
        }

        for (String subject : subjects) {
            addSubjectCard(subject);
        }
    }

    private void addSubjectCard(String subject) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(
                dp(15), dp(15), dp(15), dp(15)
        );
        card.setBackground(background(CARD, 18));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(-1, -2);

        cardParams.bottomMargin = dp(13);

        subjectContainer.addView(card, cardParams);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setOrientation(LinearLayout.HORIZONTAL);

        TextView name = makeText(
                subject, 18, WHITE, true
        );

        heading.addView(name, new LinearLayout.LayoutParams(
                0, -2, 1f
        ));

        Button deleteSubject = makeSmallButton("Delete");

        deleteSubject.setOnClickListener(
                v -> confirmDeleteSubject(subject)
        );

        heading.addView(deleteSubject);
        card.addView(heading);

        List<String> chapters =
                SubjectStorage.getChapters(this, subject);

        TextView chapterCount = makeText(
                chapters.size() + " chapters",
                11, MUTED, false
        );

        LinearLayout.LayoutParams countParams =
                new LinearLayout.LayoutParams(-1, -2);

        countParams.topMargin = dp(5);
        card.addView(chapterCount, countParams);

        Button addChapter = makeButton(
                "+  Add Chapter", false
        );

        LinearLayout.LayoutParams addParams =
                new LinearLayout.LayoutParams(-1, dp(43));

        addParams.topMargin = dp(14);

        addChapter.setOnClickListener(
                v -> showInputDialog(
                        "Add Chapter",
                        "Enter chapter or topic name",
                        value -> {
                            if (SubjectStorage.addChapter(
                                    this, subject, value
                            )) {
                                refreshSubjects();
                                toast("Chapter added");
                            } else {
                                toast("Invalid or duplicate chapter");
                            }
                        }
                )
        );

        card.addView(addChapter, addParams);

        if (chapters.isEmpty()) {
            TextView empty = makeText(
                    "No chapters added yet.",
                    12, MUTED, false
            );

            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(-1, -2);

            p.topMargin = dp(12);
            card.addView(empty, p);
        } else {
            for (String chapter : chapters) {
                addChapterRow(card, subject, chapter);
            }
        }
    }

    private void addChapterRow(
            LinearLayout card,
            String subject,
            String chapter
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(
                dp(10), dp(8), dp(5), dp(8)
        );

        row.setBackground(background(
                Color.rgb(20, 53, 40), 10
        ));

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, -2);

        rowParams.topMargin = dp(8);
        card.addView(row, rowParams);

        TextView bullet = makeText(
                "•", 18, GREEN, true
        );

        row.addView(bullet);

        TextView chapterName = makeText(
                chapter, 13, WHITE, false
        );

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        0, -2, 1f
                );

        nameParams.leftMargin = dp(9);
        row.addView(chapterName, nameParams);

        Button remove = makeSmallButton("×");

        remove.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete chapter?")
                    .setMessage(
                            "Remove \"" + chapter + "\"?"
                    )
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (d, w) -> {
                        SubjectStorage.removeChapter(
                                this, subject, chapter
                        );
                        refreshSubjects();
                    })
                    .show();
        });

        row.addView(remove);
    }

    private void confirmDeleteSubject(String subject) {
        new AlertDialog.Builder(this)
                .setTitle("Delete subject?")
                .setMessage(
                        "This will also delete all chapters saved under \""
                                + subject + "\"."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> {
                    SubjectStorage.removeSubject(
                            this, subject
                    );
                    refreshSubjects();
                    toast("Subject deleted");
                })
                .show();
    }

    private interface InputAction {
        void onSubmit(String value);
    }

    private void showInputDialog(
            String title,
            String hint,
            InputAction action
    ) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setTextColor(WHITE);
        input.setHintTextColor(MUTED);
        input.setPadding(
                dp(12), dp(12), dp(12), dp(12)
        );
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(
                dp(22), dp(8), dp(22), dp(4)
        );
        wrapper.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(wrapper)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {
                String value = input.getText()
                        .toString().trim();

                if (value.isEmpty()) {
                    input.setError("Enter a name");
                    return;
                }

                if (value.length() > 60) {
                    input.setError(
                            "Maximum 60 characters"
                    );
                    return;
                }

                action.onSubmit(value);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private Button makeButton(
            String text,
            boolean primary
    ) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        button.setTextColor(
                primary ? Color.rgb(3, 30, 20) : GREEN
        );

        button.setBackground(background(
                primary ? GREEN : Color.rgb(25, 63, 47),
                13
        ));

        return button;
    }

    private Button makeSmallButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(11);
        button.setMinWidth(dp(42));
        button.setMinimumWidth(dp(42));
        button.setPadding(dp(5), 0, dp(5), 0);
        button.setTextColor(GREEN);
        button.setBackground(background(
                Color.rgb(25, 63, 47), 9
        ));

        return button;
    }

    private TextView makeText(
            String text,
            int size,
            int color,
            boolean bold
    ) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        return view;
    }

    private GradientDrawable background(
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

    private void toast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }
}

