
package com.focuslock.app;

import android.content.Context;
import android.net.Uri;
import android.database.sqlite.SQLiteDatabase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class V2BackupManager {

    private static final String PREF_FILE =
            "FocusLockDatabaseV2.xml";

    private static final String DATABASE_FILE =
            "focuslock_v3.db";

    private V2BackupManager() {
    }

    public static boolean exportBackup(Context context, Uri destination) {
        File tempDir = new File(context.getCacheDir(), "focuslock_backup");

        try {
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                return false;
            }

            File preferences = new File(
                    new File(context.getApplicationInfo().dataDir,
                            "shared_prefs"),
                    PREF_FILE
            );

            File database = context.getDatabasePath(DATABASE_FILE);

            try (OutputStream output =
                         context.getContentResolver()
                                 .openOutputStream(destination);
                 ZipOutputStream zip = new ZipOutputStream(
                         new BufferedOutputStream(output))) {

                if (output == null) return false;

                if (preferences.exists()) {
                    addFile(zip, preferences, PREF_FILE);
                }

                if (database.exists()) {
                    addFile(zip, database, DATABASE_FILE);
                }

                zip.putNextEntry(new ZipEntry("BACKUP_INFO.txt"));
                zip.write(
                        ("FocusLock backup\nCreated: "
                                + System.currentTimeMillis())
                                .getBytes("UTF-8")
                );
                zip.closeEntry();
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private static void addFile(
            ZipOutputStream zip,
            File file,
            String entryName
    ) throws Exception {

        zip.putNextEntry(new ZipEntry(entryName));

        try (InputStream input =
                     new BufferedInputStream(new FileInputStream(file))) {

            byte[] buffer = new byte[8192];
            int count;

            while ((count = input.read(buffer)) != -1) {
                zip.write(buffer, 0, count);
            }
        }

        zip.closeEntry();
    }
}
