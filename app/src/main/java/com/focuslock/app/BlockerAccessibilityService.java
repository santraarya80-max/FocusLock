package com.focuslock.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

import java.util.Set;

public class BlockerAccessibilityService extends AccessibilityService {

    private long lastBlockTimestamp = 0L;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) {
            return;
        }

        // Check if study session is active (remains true both while running AND while paused!)
        if (!StorageHelper.isSessionActive(this)) {
            return;
        }

        String openedPackage = event.getPackageName().toString();

        // Never block Focus Lock itself or Android's system UI bar
        if (openedPackage.equals(getPackageName()) || openedPackage.equals("com.android.systemui")) {
            return;
        }

        Set<String> blockedApps = StorageHelper.getBlockedApps(this);
        if (blockedApps.contains(openedPackage)) {
            long now = System.currentTimeMillis();
            if (now - lastBlockTimestamp < 400L) {
                return;
            }
            lastBlockTimestamp = now;

            // 1. Immediately push the user out of the distracting app to the Home Screen
            performGlobalAction(GLOBAL_ACTION_HOME);

            // 2. Show the full-screen red Focus Lock warning screen
            Intent blockIntent = new Intent(this, BlockedActivity.class);
            blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(blockIntent);
        }
    }

    @Override
    public void onInterrupt() {
        // Required by Android AccessibilityService
    }
}
