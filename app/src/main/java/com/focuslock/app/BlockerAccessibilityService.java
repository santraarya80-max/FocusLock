
package com.focuslock.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
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
                    if (window == null) {
                        continue;
                    }

                    AccessibilityNodeInfo root = window.getRoot();

                    if (root != null && root.getPackageName() != null) {
                        String windowPackage =
                                root.getPackageName().toString();

                        if (isPackageBlocked(windowPackage, blockedApps)) {
                            blockedAppDetected = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!blockedAppDetected) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastBlockTimestamp < 400L) {
            return;
        }

        lastBlockTimestamp = now;

        performGlobalAction(GLOBAL_ACTION_BACK);
        performGlobalAction(GLOBAL_ACTION_HOME);

        Intent blockIntent = new Intent(this, BlockedActivity.class);
        blockIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        startActivity(blockIntent);
    }

    private boolean isPackageBlocked(
            String packageName,
            Set<String> blockedApps) {

        if (packageName == null || blockedApps == null) {
            return false;
        }

        // Never block FocusLock or essential Android system functions.
        if (isProtectedPackage(packageName)) {
            return false;
        }

        return blockedApps.contains(packageName);
    }

    private boolean isProtectedPackage(String packageName) {

        if (packageName.equals(getPackageName())) {
            return true;
        }

        // Android system interface and settings.
        if (packageName.equals("com.android.systemui")
                || packageName.equals("com.android.settings")) {
            return true;
        }

        // Phone and emergency calling.
        if (packageName.equals("com.android.phone")
                || packageName.equals("com.android.dialer")
                || packageName.equals("com.google.android.dialer")
                || packageName.equals("com.samsung.android.dialer")) {
            return true;
        }

        // Permission management and app installation.
        if (packageName.equals("com.android.permissioncontroller")
                || packageName.equals("com.google.android.permissioncontroller")
                || packageName.contains("packageinstaller")
                || packageName.contains("package.installer")
                || packageName.contains("installer")) {
            return true;
        }

        return false;
    }

    @Override
    public void onInterrupt() {
        // No action required.
    }
}
