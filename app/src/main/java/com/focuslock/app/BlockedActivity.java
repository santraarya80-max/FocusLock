package com.focuslock.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class BlockedActivity extends Activity {

    private TextView tvBlockedRemaining;
    private TextView tvBlockedSubject;
    private TextView tvMotivationalQuote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked);

        tvBlockedRemaining = findViewById(R.id.tvBlockedRemaining);
        tvBlockedSubject = findViewById(R.id.tvBlockedSubject);
        tvMotivationalQuote = findViewById(R.id.tvMotivationalQuote);
        Button btnGoHome = findViewById(R.id.btnGoHome);

        tvBlockedSubject.setText("Studying: " + StorageHelper.getCurrentSubject(this));
        tvMotivationalQuote.setText("\"" + StorageHelper.getRandomMotivationalQuote() + "\"");

        updateRemainingText();

        btnGoHome.setOnClickListener(v -> sendToPhoneHomeScreen());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRemainingText();
    }

    private void updateRemainingText() {
        long seconds = StorageHelper.getRemainingSeconds(this);
        String formatted = StorageHelper.formatTimeMMSS(seconds);
        if (StorageHelper.isSessionPaused(this)) {
            tvBlockedRemaining.setText("Time Left: " + formatted + " (PAUSED)");
        } else {
            tvBlockedRemaining.setText("Time Left: " + formatted);
        }
    }

    private void sendToPhoneHomeScreen() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        sendToPhoneHomeScreen();
    }
}
