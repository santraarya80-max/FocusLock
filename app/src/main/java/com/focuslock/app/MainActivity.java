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
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends Activity {

    private TextView tvStatusBadge;
    private TextView tvTimerDisplay;
    private TextView tvTodayStudyTime;
    private EditText etMinutesInput;
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

        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay);
        tvTodayStudyTime = findViewById(R.id.tvTodayStudyTime);
        etMinutesInput = findViewById(R.id.etMinutesInput);
        btnStartFocus = findViewById(R.id.btnStartFocus);
        btnPauseResume = findViewById(R.id.btnPauseResume);
        btnEnableBlocker = findViewById(R.id.btnEnableBlocker);
        btnSelectApps = findViewById(R.id.btnSelectApps);
        btnViewHistory = findViewById(R.id.btnViewHistory);

        // Ask permission to show the small top-bar timer notification on Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            }
        }

        // Start Focus Lock Button
        btnStartFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFocusSession();
            }
        });

        // Strict Pause / Resume Button (Pauses timer, keeps apps locked)
        btnPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StorageHelper.isSessionActive(MainActivity.this)) {
                    boolean currentlyPaused = StorageHelper.isSessionPaused(MainActivity.this);
                    StorageHelper.setSessionPaused(MainActivity.this, !currentlyPaused);
                    if (!currentlyPaused) {
                        Toast.makeText(MainActivity.this, "Timer Paused. Distracting apps remain LOCKED!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Study Timer Resumed!", Toast.LENGTH_SHORT).show();
                    }
                    updateUI();
                }
            }
        });

        // Enable Accessibility App Blocker Button
        btnEnableBlocker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "Find 'Installed apps' -> 'Focus Lock App Blocker' and turn it ON", Toast.LENGTH_LONG).show();
            }
        });

        // Select Distracting Apps Button
        btnSelectApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StorageHelper.isSessionActive(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "Locked! You cannot change blocked apps until the timer hits 00:00.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MainActivity.this, AppSelectActivity.class);
                startActivity(intent);
            }
        });

        // View Study History Button
        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });
    }

    private void startFocusSession() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Please turn ON the Blocker Permission first!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            return;
        }

        Set<String> blockedApps = StorageHelper.getBlockedApps(this);
        if (blockedApps.isEmpty()) {
            Toast.makeText(this, "Please select at least 1 distracting app to block first!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, AppSelectActivity.class);
            startActivity(intent);
            return;
        }

        String input = etMinutesInput.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            Toast.makeText(this, "Please enter study minutes (for example: 90)", Toast.LENGTH_SHORT).show();
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number of minutes", Toast.LENGTH_SHORT).show();
            return;
        }

        if (minutes <= 0) {
            Toast.makeText(this, "Minutes must be at least 1", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save session in database and start the background countdown service
        StorageHelper.startSession(this, minutes);
        Intent serviceIntent = new Intent(this, TimerService.class);
        startForegroundService(serviceIntent);

        Toast.makeText(this, "Focus Lock started! Selected apps are now locked until 00:00.", Toast.LENGTH_LONG).show();
        updateUI();
    }

    private void updateUI() {
        // Update Today's Study Time card
        int todayMins = StorageHelper.getTodayStudyMinutes(this);
        tvTodayStudyTime.setText("Today's Study Time: " + StorageHelper.formatMinutesReadable(todayMins));

        // Update Accessibility Permission button text
        if (isAccessibilityServiceEnabled()) {
            btnEnableBlocker.setText("Blocker Permission: ON (Active)");
        } else {
            btnEnableBlocker.setText("Blocker Permission: OFF (Tap to Turn ON)");
        }

        // Check if study timer is currently running or paused
        if (StorageHelper.isSessionActive(this)) {
            etMinutesInput.setVisibility(View.GONE);
            btnStartFocus.setVisibility(View.GONE);
            btnPauseResume.setVisibility(View.VISIBLE);

            btnSelectApps.setEnabled(false);
            btnSelectApps.setText("Blocked Apps Locked Until 00:00");

            long remainingSeconds = StorageHelper.getRemainingSeconds(this);
            tvTimerDisplay.setText(StorageHelper.formatTimeMMSS(remainingSeconds));

            if (StorageHelper.isSessionPaused(this)) {
                tvStatusBadge.setText("PAUSED — APPS ARE STILL LOCKED");
                btnPauseResume.setText("RESUME STUDY TIMER");
            } else {
                tvStatusBadge.setText("FOCUSING — APPS LOCKED");
                btnPauseResume.setText("PAUSE TIMER (APPS STAY LOCKED)");
            }
        } else {
            etMinutesInput.setVisibility(View.VISIBLE);
            btnStartFocus.setVisibility(View.VISIBLE);
            btnPauseResume.setVisibility(View.GONE);

            int count = StorageHelper.getBlockedApps(this).size();
            btnSelectApps.setEnabled(true);
            btnSelectApps.setText("Select Distracting Apps to Block (" + count + " selected)");

            tvStatusBadge.setText("Ready to Study");

            String input = etMinutesInput.getText().toString().trim();
            try {
                int mins = Integer.parseInt(input);
                if (mins > 0) {
                    tvTimerDisplay.setText(StorageHelper.formatTimeMMSS(mins * 60L));
                } else {
                    tvTimerDisplay.setText("00:00");
                }
            } catch (NumberFormatException e) {
                tvTimerDisplay.setText("00:00");
            }
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String serviceName = getPackageName() + "/" + BlockerAccessibilityService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        return enabledServices != null && enabledServices.contains(serviceName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (StorageHelper.isSessionActive(this)) {
            Intent serviceIntent = new Intent(this, TimerService.class);
            startForegroundService(serviceIntent);
        }
        uiHandler.post(uiUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHandler.removeCallbacks(uiUpdater);
    }
}
