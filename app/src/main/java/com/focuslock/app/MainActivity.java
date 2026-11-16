
package com.focuslock.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.focuslock.app.tracking.V3SessionRecorder;
import com.focuslock.app.database.V2DataMigrator;

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

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

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

        if (!V2DataMigrator.migrate(this)) {
            Toast.makeText(
                    this,
                    "V2 data migration failed. Original data is preserved.",
                    Toast.LENGTH_LONG
            ).show();
        }

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
        progressBarTimerRing = findViewById(R.id.progressBarTimerRing);
        progressBarDailyGoal = findViewById(R.id.progressBarDailyGoal);
        btnStartFocus = findViewById(R.id.btnStartFocus);
        btnPauseResume = findViewById(R.id.btnPauseResume);
        btnEnableBlocker = findViewById(R.id.btnEnableBlocker);
        btnSelectApps = findViewById(R.id.btnSelectApps);
        btnViewHistory = findViewById(R.id.btnViewHistory);

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{"android.permission.POST_NOTIFICATIONS"},
                        101
                );
            }
        }

        findViewById(R.id.btnSubjMaths)
                .setOnClickListener(v -> etSubjectInput.setText("Maths"));
        findViewById(R.id.btnSubjPhysics)
                .setOnClickListener(v -> etSubjectInput.setText("Physics"));
        findViewById(R.id.btnSubjChem)
                .setOnClickListener(v -> etSubjectInput.setText("Chem"));
        findViewById(R.id.btnSubjOther)
                .setOnClickListener(v -> etSubjectInput.setText("Revision"));

        findViewById(R.id.btnPreset30)
                .setOnClickListener(v -> etMinutesInput.setText("30"));
        findViewById(R.id.btnPreset60)
                .setOnClickListener(v -> etMinutesInput.setText("60"));
        findViewById(R.id.btnPreset90)
                .setOnClickListener(v -> etMinutesInput.setText("90"));
        findViewById(R.id.btnPreset120)
                .setOnClickListener(v -> etMinutesInput.setText("120"));

        btnStartFocus.setOnClickListener(v -> startFocusSession());

        btnPauseResume.setOnClickListener(v -> {
            if (StorageHelper.isSessionActive(MainActivity.this)) {
                boolean currentlyPaused =
                        StorageHelper.isSessionPaused(MainActivity.this);

                if (!currentlyPaused) {
                    int pauses =
                            StorageHelper.getPauseCount(MainActivity.this);

                    if (pauses >= 2) {
                        Toast.makeText(
                                MainActivity.this,
                                "Strict Rule: Max 2 pauses allowed per session!",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    StorageHelper.incrementPauseCount(MainActivity.this);
                    StorageHelper.setSessionPaused(MainActivity.this, true);

                    Toast.makeText(
                            MainActivity.this,
                            "Timer Paused (10-min auto-resume rule active). Apps locked!",
                            Toast.LENGTH_LONG
                    ).show();

                } else {
                    StorageHelper.setSessionPaused(MainActivity.this, false);

                    Toast.makeText(
                            MainActivity.this,
                            "Study Timer Resumed!",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                updateUI();
            }
        });

        btnEnableBlocker.setOnClickListener(v -> {
            Intent intent =
                    new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);

            Toast.makeText(
                    MainActivity.this,
                    "Find 'Focus Lock App Blocker' and turn it ON",
                    Toast.LENGTH_LONG
            ).show();
        });

        btnSelectApps.setOnClickListener(v -> {
            if (StorageHelper.isSessionActive(MainActivity.this)) {
                Toast.makeText(
                        MainActivity.this,
                        "Locked! Cannot change apps until timer hits 00:00.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            startActivity(
                    new Intent(MainActivity.this, AppSelectActivity.class)
            );
        });

        btnViewHistory.setOnClickListener(v ->
                startActivity(
                        new Intent(MainActivity.this, HistoryActivity.class)
                )
        );
    }

    private void startFocusSession() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(
                    this,
                    "Please turn ON the Blocker Permission first!",
                    Toast.LENGTH_LONG
            ).show();

            startActivity(
                    new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            );
            return;
        }

        Set<String> blockedApps = StorageHelper.getBlockedApps(this);

        if (blockedApps.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please select at least 1 distracting app to block first!",
                    Toast.LENGTH_LONG
            ).show();

            startActivity(new Intent(this, AppSelectActivity.class));
            return;
        }

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

        StorageHelper.startSession(this, minutes, subject);
        V3SessionRecorder.startSession(this, subject, minutes);

        Intent serviceIntent = new Intent(this, TimerService.class);
        startForegroundService(serviceIntent);

        Toast.makeText(
                this,
                "Focus Lock V3 started! Apps & Settings are locked.",
                Toast.LENGTH_LONG
        ).show();

        updateUI();
    }

    private void updateUI() {
        int todayMins = StorageHelper.getTodayStudyMinutes(this);

        tvTodayStudyTime.setText(
                "Today's Study Time: "
                        + StorageHelper.formatMinutesReadable(todayMins)
        );

        int streak = StorageHelper.getStreakDays(this);
        tvStreakBadge.setText("Streak: " + streak + " Days");

        int goalMins = 240;
        int progressPercent = Math.min(100, (todayMins * 100) / goalMins);

        progressBarDailyGoal.setProgress(Math.min(240, todayMins));

        tvGoalProgressText.setText(
                "Daily Goal (4 hrs): " + progressPercent + "% completed"
        );

        if (isAccessibilityServiceEnabled()) {
            btnEnableBlocker.setText("Blocker Permission: ON (Active)");
        } else {
            btnEnableBlocker.setText(
                    "Blocker Permission: OFF (Tap to Turn ON)"
            );
        }

        if (StorageHelper.isSessionActive(this)) {
            layoutSetupControls.setVisibility(View.GONE);
            btnStartFocus.setVisibility(View.GONE);
            btnPauseResume.setVisibility(View.VISIBLE);
            btnSelectApps.setEnabled(false);

            String sub = StorageHelper.getCurrentSubject(this);

            tvCurrentSubjectLabel.setText("Current Subject: " + sub);
            tvCurrentSubjectLabel.setVisibility(View.VISIBLE);

            long remaining = StorageHelper.getRemainingSeconds(this);

            tvTimerDisplay.setText(
                    StorageHelper.formatTimeMMSS(remaining)
            );

            progressBarTimerRing.setProgress((int) remaining);

            int pausesLeft = 2 - StorageHelper.getPauseCount(this);

            if (StorageHelper.isSessionPaused(this)) {
                tvStatusBadge.setText(
                        "PAUSED (Auto-resumes in 10m) — APPS LOCKED"
                );

                btnPauseResume.setText("RESUME STUDY TIMER");
                tvPauseInfo.setText("Pauses left: " + pausesLeft + " / 2");

            } else {
                tvStatusBadge.setText(
                        "FOCUSING — APPS & SETTINGS LOCKED"
                );

                btnPauseResume.setText(
                        "PAUSE TIMER (" + pausesLeft + " left)"
                );

                tvPauseInfo.setText(
                        "Strict Mode: Max 2 pauses (10m auto-resume)"
                );
            }

        } else {
            layoutSetupControls.setVisibility(View.VISIBLE);
            btnStartFocus.setVisibility(View.VISIBLE);
            btnPauseResume.setVisibility(View.GONE);
            btnSelectApps.setEnabled(true);
            tvCurrentSubjectLabel.setVisibility(View.GONE);

            tvStatusBadge.setText("Ready to Study");

            String input =
                    etMinutesInput.getText().toString().trim();

            try {
                int mins = Integer.parseInt(input);

                if (mins > 0) {
                    tvTimerDisplay.setText(
                            StorageHelper.formatTimeMMSS(mins * 60L)
                    );

                    progressBarTimerRing.setMax(mins * 60);
                    progressBarTimerRing.setProgress(mins * 60);

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
                        + BlockerAccessibilityService.class.getCanonicalName();

        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        return enabledServices != null
                && enabledServices.contains(serviceName);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (StorageHelper.isSessionActive(this)) {
            startForegroundService(
                    new Intent(this, TimerService.class)
            );
        }

        uiHandler.post(uiUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHandler.removeCallbacks(uiUpdater);
    }
}
