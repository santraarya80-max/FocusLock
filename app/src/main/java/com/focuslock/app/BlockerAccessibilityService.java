package com.focuslock.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;
import java.util.Set;

public class BlockerAccessibilityService extends AccessibilityService {

    private long lastBlockTimestamp = 0L;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Check if study session is active (remains true both while running AND while paused!)
        if (!StorageHelper.isSessionActive(this)) {
            return;
        }

        Set<String> blockedApps = StorageHelper.getBlockedApps(this);
        if (blockedApps.isEmpty()) {
            return;
        }

        boolean blockedAppDetected = false;

        // 1. Check the main app event
        if (event != null && event.getPackageName() != null) {
            String openedPackage = event.getPackageName().toString();
            if (isPackageBlocked(openedPackage, blockedApps)) {
                blockedAppDetected = true;
            }
        }

        // 2. Also scan all floating mini-players (Picture-in-Picture) and split-screen windows
        if (!blockedAppDetected) {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo window : windows) {
                    AccessibilityNodeInfo root = window.getRoot();
                    if (root != null && root.getPackageName() != null) {
                        String windowPkg = root.getPackageName().toString();
                        if (isPackageBlocked(windowPkg, blockedApps)) {
                            blockedAppDetected = true;
                            break;
                        }
                    }
                }
            }
        }

        if (blockedAppDetected) {
            long now = System.currentTimeMillis();
            if (now - lastBlockTimestamp < 400L) {
                return;
            }
            lastBlockTimestamp = now;

            // Force-pause any video or reel playing in the blocked app
            stopBlockedMedia();

            // Press Back first to close any open video player so it cannot shrink into a mini-player
            performGlobalAction(GLOBAL_ACTION_BACK);
            performGlobalAction(GLOBAL_ACTION_HOME);

            // Launch the full-screen red Focus Lock warning screen
            Intent blockIntent = new Intent(this, BlockedActivity.class);
            blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(blockIntent);
        }
    }

    private boolean isPackageBlocked(String pkg, Set<String> blockedApps) {
        if (pkg == null) return false;
        if (pkg.equals(getPackageName()) || pkg.equals("com.android.systemui")) {
            return false;
        }
        return blockedApps.contains(pkg);
    }

    private void stopBlockedMedia() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.requestAudioFocus(
                        null,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                );
                KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE);
                audioManager.dispatchMediaKeyEvent(downEvent);
                KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE);
                audioManager.dispatchMediaKeyEvent(upEvent);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterrupt() {
        // Required by Android AccessibilityService
    }
}
