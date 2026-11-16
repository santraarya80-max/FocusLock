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
        if (!StorageHelper.isSessionActive(this)) {
            return;
        }

        Set<String> blockedApps = StorageHelper.getBlockedApps(this);
        boolean blockedAppDetected = false;

        if (event != null && event.getPackageName() != null) {
            String openedPackage = event.getPackageName().toString();
            if (isPackageBlocked(openedPackage, blockedApps)) {
                blockedAppDetected = true;
            }
        }

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

            stopBlockedMedia();
            performGlobalAction(GLOBAL_ACTION_BACK);
            performGlobalAction(GLOBAL_ACTION_HOME);

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

        // ANTI-CHEAT: Automatically block phone Settings and package installers during study time!
        if (pkg.equals("com.android.settings") || pkg.contains("installer") || pkg.contains("packageinstaller")) {
            return true;
        }

        return blockedApps.contains(pkg);
    }

    private void stopBlockedMedia() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
                audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterrupt() {
    }
}
