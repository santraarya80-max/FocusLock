package com.focuslock.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppSelectActivity extends Activity {

    private ListView listViewApps;
    private Button btnSaveApps;
    private final List<AppEntry> appEntries = new ArrayList<>();

    private static class AppEntry {
        String label;
        String packageName;

        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_select);

        // Safety check: Never allow editing blocked apps while the timer is active
        if (StorageHelper.isSessionActive(this)) {
            Toast.makeText(this, "Timer is active! Apps cannot be changed until 00:00.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        listViewApps = findViewById(R.id.listViewApps);
        btnSaveApps = findViewById(R.id.btnSaveApps);

        loadInstalledApps();

        btnSaveApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSelectedApps();
            }
        });
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Find default home launcher so we don't block the Samsung Home Screen
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo homeInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        String homePackage = (homeInfo != null && homeInfo.activityInfo != null)
                ? homeInfo.activityInfo.packageName : "";

        List<ResolveInfo> resolvedList = pm.queryIntentActivities(mainIntent, 0);
        Set<String> seenPackages = new HashSet<>();

        for (ResolveInfo info : resolvedList) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;

            // Skip Focus Lock itself and the phone's Home Screen launcher
            if (pkg.equals(getPackageName()) || pkg.equals(homePackage) || seenPackages.contains(pkg)) {
                continue;
            }
            seenPackages.add(pkg);

            String label = info.loadLabel(pm).toString();
            appEntries.add(new AppEntry(label, pkg));
        }

        // Sort apps A to Z by name
        Collections.sort(appEntries, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });

        List<String> displayNames = new ArrayList<>();
        for (AppEntry entry : appEntries) {
            displayNames.add(entry.label);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_multiple_choice,
                displayNames
        );
        listViewApps.setAdapter(adapter);

        // Pre-check any apps that were already saved as blocked
        Set<String> savedBlocked = StorageHelper.getBlockedApps(this);
        for (int i = 0; i < appEntries.size(); i++) {
            if (savedBlocked.contains(appEntries.get(i).packageName)) {
                listViewApps.setItemChecked(i, true);
            }
        }
    }

    private void saveSelectedApps() {
        Set<String> selectedPackages = new HashSet<>();
        for (int i = 0; i < appEntries.size(); i++) {
            if (listViewApps.isItemChecked(i)) {
                selectedPackages.add(appEntries.get(i).packageName);
            }
        }

        StorageHelper.saveBlockedApps(this, selectedPackages);
        Toast.makeText(this, "Saved " + selectedPackages.size() + " blocked apps!", Toast.LENGTH_SHORT).show();
        finish();
    }
}

