
package com.focuslock.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends Activity {

    private TextView tvStatusBadge;
    private TextView tvStreakBadge;
    private TextView tvTimerDisplay;
    private TextView tvCurrentSubjectLabel;
    private TextView tvPauseInfo;
    private TextView tvTodayStudyTime;
    private TextView tvGoalProgressText;

    private EditText etMinutesInput;
    private EditText etSubjectInput;

    private LinearLayout layoutSetupControls;

    private ProgressBar progressBarTimerRing;
    private ProgressBar progressBarDailyGoal;

    private Button btnStartFocus;
    private Button btnPauseResume;
    private Button btnEnableBlocker;
    private Button btnSelectApps;
    private Button btnViewHistory;

    private PopupWindow navigationDrawer;

    private final Handler uiHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable uiUpdater = new Runnable() {
        @Override
        public void run() {
            updateUI();
            uiHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvStreakBadge = findViewById(R.id.tvStreakBadge);
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay);
        tvCurrentSubjectLabel = findViewById(R.id.tvCurrentSubjectLabel);
        tvPauseInfo = findViewById(R.id.tvPauseInfo);
        tvTodayStudyTime = findViewById(R.id.tvTodayStudyTime);
        tvGoalProgressText = findViewById(R.id.tvGoalProgressText);

        etMinutesInput = findViewById(R.id.etMinutesInput);
        etSubjectInput = findViewById(R.id.etSubjectInput);

        layoutSetupControls = findViewById(R.id.layoutSetupControls);

        progressBarTimerRing =
                findViewById(R.id.progressBarTimerRing);

        progressBarDailyGoal =
                findViewById(R.id.progressBarDailyGoal);

        btnStartFocus = findViewById(R.id.btnStartFocus);
        btnPauseResume = findViewById(R.id.btnPauseResume);
        btnEnableBlocker = findViewById(R.id.btnEnableBlocker);
        btnSelectApps = findViewById(R.id.btnSelectApps);
        btnViewHistory = findViewById(R.id.btnViewHistory);

        addNavigationButton();

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(
                    "android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                "android.permission.POST_NOTIFICATIONS"
                        },
                        101
                );
            }
        }

        findViewById(R.id.btnSubjMaths)
                .setOnClickListener(v ->
                        etSubjectInput.setText("Maths"));

        findViewById(R.id.btnSubjPhysics)
                .setOnClickListener(v ->
                        etSubjectInput.setText("Physics"));

        findViewById(R.id.btnSubjChem)
                .setOnClickListener(v ->
                        etSubjectInput.setText("Chem"));

        findViewById(R.id.btnSubjOther)
                .setOnClickListener(v ->
                        etSubjectInput.setText("Revision"));

        findViewById(R.id.btnPreset30)
                .setOnClickListener(v ->
                        etMinutesInput.setText("30"));

        findViewById(R.id.btnPreset60)
                .setOnClickListener(v ->
                        etMinutesInput.setText("60"));

        findViewById(R.id.btnPreset90)
                .setOnClickListener(v ->
                        etMinutesInput.setText("90"));

        findViewById(R.id.btnPreset120)
                .setOnClickListener(v ->
                        etMinutesInput.setText("120"));

        btnStartFocus.setOnClickListener(v ->
                startFocusSession());

        btnPauseResume.setOnClickListener(v ->
                togglePauseResume());

        btnEnableBlocker.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Settings.ACTION_ACCESSIBILITY_SETTINGS);

            startActivity(intent);

            Toast.makeText(
                    MainActivity.this,
                    "Find FocusLock App Blocker and turn it ON",
                    Toast.LENGTH_LONG
            ).show();
        });

        btnSelectApps.setOnClickListener(v -> {
            if (StorageHelper.isSessionActive(MainActivity.this)) {
                Toast.makeText(
                        MainActivity.this,
                        "Cannot change apps during an active session.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            startActivity(new Intent(
                    MainActivity.this,
                    AppSelectActivity.class
            ));
        });

        btnViewHistory.setOnClickListener(v ->
                openAnalytics());
    }

    // ADD MENU BUTTON TO EXISTING HEADER

    private void addNavigationButton() {

        View root = findViewById(android.R.id.content);

        if (!(root instanceof ViewGroup)) {
            return;
        }

        View scrollView = ((ViewGroup) root).getChildAt(0);

        if (!(scrollView instanceof android.widget.ScrollView)) {
            return;
        }

        View content =
                ((android.widget.ScrollView) scrollView).getChildAt(0);

        if (!(content instanceof LinearLayout)) {
            return;
        }

        LinearLayout mainLayout = (LinearLayout) content;

        if (mainLayout.getChildCount() == 0) {
            return;
        }

        View headerView = mainLayout.getChildAt(0);

        if (!(headerView instanceof LinearLayout)) {
            return;
        }

        LinearLayout header = (LinearLayout) headerView;

        TextView menuButton = new TextView(this);

        menuButton.setText("☰");
        menuButton.setTextSize(25);
        menuButton.setTextColor(Color.WHITE);
        menuButton.setGravity(Gravity.CENTER);
        menuButton.setBackground(createRoundedBackground(
                "#123D30", 15));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        dp(44),
                        dp(44)
                );

        params.setMargins(0, 0, dp(12), 0);

        header.addView(menuButton, 0, params);

        menuButton.setOnClickListener(v ->
                showNavigationDrawer(menuButton));
    }

    // NAVIGATION DRAWER

    private void showNavigationDrawer(View anchor) {

        if (navigationDrawer != null
                && navigationDrawer.isShowing()) {
            navigationDrawer.dismiss();
            return;
        }

        LinearLayout drawer = new LinearLayout(this);

        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setPadding(
                dp(18), dp(24), dp(18), dp(18));

        drawer.setBackgroundColor(Color.rgb(5, 25, 19));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0, 0, 0, dp(20));

        TextView brand = makeText(
                "FOCUSLOCK",
                23,
                "#FFFFFF",
                true
        );

        TextView subtitle = makeText(
                "YOUR STUDY COMMAND CENTRE",
                10,
                "#55D9B0",
                true
        );

        header.addView(brand);
        header.addView(subtitle);

        drawer.addView(header);

        View separator = new View(this);
        separator.setBackgroundColor(Color.rgb(35, 75, 60));

        drawer.addView(separator,
                new LinearLayout.LayoutParams(
                        -1, dp(1)));

        android.widget.ScrollView menuScroll =
                new android.widget.ScrollView(this);

        LinearLayout menuItems = new LinearLayout(this);
        menuItems.setOrientation(LinearLayout.VERTICAL);
        menuItems.setPadding(0, dp(12), 0, dp(12));

        addDrawerItem(menuItems, "⌂", "Home",
                "Available", this::closeDrawer);

        addDrawerItem(menuItems, "◔", "Analytics",
                "Available", this::openAnalytics);

        addDrawerItem(menuItems, "▦", "Calendar & Exams",
                "Coming soon", null);

        addDrawerItem(menuItems, "▤", "Study Planner",
                "Coming soon", null);

        addDrawerItem(menuItems, "▣", "Subjects & Chapters",
                "Coming soon", null);

        addDrawerItem(menuItems, "↻", "Revision Tracker",
                "Coming soon", null);

        addDrawerItem(menuItems, "⬡", "App Blocker",
                "Available", this::openAppBlocker);

        addDrawerItem(menuItems, "◎", "Goals",
                "Coming soon", null);

        addDrawerItem(menuItems, "⚙", "Settings",
                "Coming soon", null);

        menuScroll.addView(menuItems);

        drawer.addView(
                menuScroll,
                new LinearLayout.LayoutParams(
                        -1, 0, 1f
                )
        );

        TextView footer = makeText(
                "FOCUSLOCK V3  •  PERSONAL EDITION",
                9,
                "#648D7D",
                true
        );

        footer.setPadding(0, dp(16), 0, dp(8));

        drawer.addView(footer);

        navigationDrawer = new PopupWindow(
                drawer,
                dp(300),
                ViewGroup.LayoutParams.MATCH_PARENT,
                true
        );

        navigationDrawer.setBackgroundDrawable(
                createRoundedBackground("#051913", 0));

        navigationDrawer.setOutsideTouchable(true);
        navigationDrawer.setElevation(dp(24));

        navigationDrawer.setAnimationStyle(
                android.R.style.Animation_Activity
        );

        navigationDrawer.showAtLocation(
                findViewById(android.R.id.content),
                Gravity.START | Gravity.TOP,
                0,
                0
        );
    }

    private void addDrawerItem(
            LinearLayout parent,
            String symbol,
            String title,
            String status,
            Runnable action) {

        LinearLayout item = new LinearLayout(this);

        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(
                dp(10), dp(13), dp(8), dp(13));

        TextView icon = makeText(
                symbol,
                21,
                "#55E6B2",
                true
        );

        icon.setGravity(Gravity.CENTER);
        icon.setBackground(
                createRoundedBackground("#123A2D", 12));

        item.addView(icon,
                new LinearLayout.LayoutParams(
                        dp(42), dp(42)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);

        TextView name = makeText(
                title,
                14,
                "#FFFFFF",
                true
        );

        TextView state = makeText(
                status,
                10,
                status.equals("Available")
                        ? "#55D9B0" : "#82958D",
                false
        );

        labels.addView(name);
        labels.addView(state);

        LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        labelParams.setMargins(dp(12), 0, 0, 0);

        item.addView(labels, labelParams);

        item.setBackground(
                createRoundedBackground("#0B251C", 13));

        LinearLayout.LayoutParams itemParams =
                new LinearLayout.LayoutParams(
                        -1,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        itemParams.setMargins(0, dp(3), 0, dp(3));

        parent.addView(item, itemParams);

        if (action != null) {
            item.setOnClickListener(v -> action.run());
        } else {
            item.setAlpha(0.65f);
        }
    }

    private void closeDrawer() {
        if (navigationDrawer != null
                && navigationDrawer.isShowing()) {
            navigationDrawer.dismiss();
        }
    }

    private void openAnalytics() {
        closeDrawer();

        startActivity(new Intent(
                this,
                HistoryActivity.class
        ));
    }

    private void openAppBlocker() {
        closeDrawer();

        if (StorageHelper.isSessionActive(this)) {
            Toast.makeText(
                    this,
                    "App settings are locked during a focus session.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        startActivity(new Intent(
                this,
                AppSelectActivity.class
        ));
    }

    // TIMER CONTROLS

    private void togglePauseResume() {

        if (!StorageHelper.isSessionActive(this)) {
            return;
        }

        boolean currentlyPaused =
                StorageHelper.isSessionPaused(this);

        if (!currentlyPaused) {

            int pauses =
                    StorageHelper.getPauseCount(this);

            if (pauses >= 2) {
                Toast.makeText(
                        this,
                        "Strict Rule: Maximum 2 pauses per session!",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            StorageHelper.incrementPauseCount(this);

            StorageHelper.setSessionPaused(this, true);

            Toast.makeText(
                    this,
                    "Timer paused. The 10-minute auto-resume rule is active.",
                    Toast.LENGTH_LONG
            ).show();

        } else {

            StorageHelper.setSessionPaused(this, false);

            Toast.makeText(
                    this,
                    "Study Timer Resumed!",
                    Toast.LENGTH_SHORT
            ).show();
        }

        updateUI();
    }

    private void startFocusSession() {

        String inputMins =
                etMinutesInput.getText().toString().trim();

        String subject =
                etSubjectInput.getText().toString().trim();

        if (TextUtils.isEmpty(subject)) {
            subject = "General Study";
        }

        int minutes;

        try {
            minutes = Integer.parseInt(inputMins);
        } catch (NumberFormatException e) {
            Toast.makeText(
                    this,
                    "Please enter a valid number of minutes",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (minutes <= 0) {
            Toast.makeText(
                    this,
                    "Minutes must be at least 1",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        StorageHelper.startSession(
                this,
                minutes,
                subject
        );

        Intent serviceIntent =
                new Intent(this, TimerService.class);

        startForegroundService(serviceIntent);

        boolean blockerActive =
                isAccessibilityServiceEnabled()
                && !StorageHelper.getBlockedApps(this).isEmpty();

        String message = blockerActive
                ? "FocusLock V3 started. Selected apps are blocked."
                : "FocusLock V3 timer started. App blocking is OFF.";

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();

        updateUI();
    }

    // DASHBOARD UPDATES

    private void updateUI() {

        int todayMins =
                StorageHelper.getTodayStudyMinutes(this);

        tvTodayStudyTime.setText(
                "Today's Study Time: "
                        + StorageHelper.formatMinutesReadable(
                                todayMins)
        );

        int streak =
                StorageHelper.getStreakDays(this);

        tvStreakBadge.setText(
                "🔥 " + streak + " DAYS"
        );

        int goalMins = 240;

        int progressPercent =
                Math.min(100,
                        (todayMins * 100) / goalMins);

        progressBarDailyGoal.setProgress(
                Math.min(240, todayMins));

        tvGoalProgressText.setText(
                "Daily Goal (4 hrs): "
                        + progressPercent + "% completed"
        );

        boolean blockerActive =
                isAccessibilityServiceEnabled();

        if (blockerActive) {
            btnEnableBlocker.setText(
                    "Blocker Permission: ON (Active)");
        } else {
            btnEnableBlocker.setText(
                    "Blocker Permission: OFF (Tap to Turn ON)");
        }

        if (StorageHelper.isSessionActive(this)) {

            layoutSetupControls.setVisibility(View.GONE);

            btnStartFocus.setVisibility(View.GONE);

            btnPauseResume.setVisibility(View.VISIBLE);

            btnSelectApps.setEnabled(false);

            String subject =
                    StorageHelper.getCurrentSubject(this);

            tvCurrentSubjectLabel.setText(
                    "Current Subject: " + subject);

            tvCurrentSubjectLabel.setVisibility(View.VISIBLE);

            long remaining =
                    StorageHelper.getRemainingSeconds(this);

            tvTimerDisplay.setText(
                    StorageHelper.formatTimeMMSS(remaining));

            progressBarTimerRing.setProgress(
                    (int) remaining);

            int pausesLeft =
                    2 - StorageHelper.getPauseCount(this);

            if (StorageHelper.isSessionPaused(this)) {

                tvStatusBadge.setText(
                        "PAUSED (Auto-resumes in 10m)");

                btnPauseResume.setText(
                        "RESUME STUDY TIMER");

                tvPauseInfo.setText(
                        "Pauses left: " + pausesLeft + " / 2");

            } else {

                Set<String> blockedApps =
                        StorageHelper.getBlockedApps(this);

                if (blockerActive && !blockedApps.isEmpty()) {
                    tvStatusBadge.setText(
                            "FOCUSING — SELECTED APPS BLOCKED");
                } else {
                    tvStatusBadge.setText(
                            "FOCUSING — TIMER ONLY");
                }

                btnPauseResume.setText(
                        "PAUSE TIMER (" + pausesLeft + " left)");

                tvPauseInfo.setText(
                        "Strict Mode: Max 2 pauses (10m auto-resume)");
            }

        } else {

            layoutSetupControls.setVisibility(View.VISIBLE);

            btnStartFocus.setVisibility(View.VISIBLE);

            btnPauseResume.setVisibility(View.GONE);

            btnSelectApps.setEnabled(true);

            tvCurrentSubjectLabel.setVisibility(View.GONE);

            tvStatusBadge.setText("● READY TO FOCUS");

            String input =
                    etMinutesInput.getText().toString().trim();

            try {
                int mins = Integer.parseInt(input);

                if (mins > 0) {

                    tvTimerDisplay.setText(
                            StorageHelper.formatTimeMMSS(
                                    mins * 60L));

                    progressBarTimerRing.setMax(mins * 60);

                    progressBarTimerRing.setProgress(
                            mins * 60);

                } else {
                    tvTimerDisplay.setText("00:00");
                }

            } catch (NumberFormatException e) {
                tvTimerDisplay.setText("00:00");
            }
        }
    }

    private boolean isAccessibilityServiceEnabled() {

        String serviceName =
                getPackageName() + "/"
                        + BlockerAccessibilityService.class
                        .getCanonicalName();

        String enabledServices =
                Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                );

        return enabledServices != null
                && enabledServices.contains(serviceName);
    }

    // UI HELPERS

    private TextView makeText(
            String value,
            int size,
            String color,
            boolean bold) {

        TextView text = new TextView(this);

        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(Color.parseColor(color));

        if (bold) {
            text.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        return text;
    }

    private GradientDrawable createRoundedBackground(
            String color,
            int radius) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(Color.parseColor(color));
        drawable.setCornerRadius(dp(radius));

        return drawable;
    }

    private int dp(int value) {
        return (int) (value
                * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (StorageHelper.isSessionActive(this)) {
            startForegroundService(
                    new Intent(this, TimerService.class));
        }

        uiHandler.post(uiUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();

        uiHandler.removeCallbacks(uiUpdater);
    }

    @Override
    public void onBackPressed() {

        if (navigationDrawer != null
                && navigationDrawer.isShowing()) {
            navigationDrawer.dismiss();
            return;
        }

        super.onBackPressed();
    }
}
