# FocusLock V3 Rebuild — Milestone 1

## What is included

- A new layered Java Android project structure.
- A session state engine based on Android's monotonic elapsed clock.
- A persisted two-pause limit and ten-minute automatic resume deadline.
- Safe pause-on-reboot behaviour so powered-off time is not counted as study time.
- A foreground timer service and ongoing notification.
- A compatibility adapter reading the existing `FocusLockDatabaseV2` SharedPreferences file.
- Non-destructive import of `history_YYYY-MM-DD` and `subject_YYYY-MM-DD_Subject` totals into a new SQLite database.
- Idempotent completed-session inserts keyed by a stable session UUID.
- User-selected app blocking, with Settings, phone, emergency, system UI and home launcher excluded.
- A basic dashboard, app selector, blocked screen and study history screen.
- A GitHub Actions build workflow pinned to Gradle 8.9 / Android Gradle Plugin 8.7.3.

## Preservation and installation warning

The application ID remains `com.focuslock.app`, and the old preference filename and keys are retained. The old preference file is read but not deleted. The legacy history is copied into new SQLite tables while the source values remain untouched.

An in-place Android app update also requires the new APK to be signed with the same signing certificate as the installed V1/V2 APK. The repository does not contain the original signing keystore, so signing continuity has not been verified. Do not uninstall the old app or install this rebuild over an existing installation until the signing key and a recoverable backup are confirmed.

The old V2 app's ZIP backup implementation was export-only and its UI controls were inert. This milestone does not yet provide a complete restore workflow. Therefore the old app's private data must be treated as valuable and must not be cleared.

## Verification completed

- 10 XML files parsed successfully.
- Project ZIP integrity check passed.
- Source files and manifest paths were inspected.
- No Android APK build was run: this execution environment has no Gradle installation and no Android SDK.

## Not yet implemented

- Full validated backup + restore with rollback.
- Complete migration audit/report UI and migration tests.
- Planner and calendar.
- Subject / chapter / topic management.
- Revision centre and spaced repetition.
- Analytics dashboards.
- Custom notification/reminder scheduler.
- Full instrumentation/unit test suite.
- Samsung physical-device testing.
- Release signing and APK generation.

## GitHub status

The connected GitHub integration returned HTTP 403 for branch creation and file creation. No remote branch or repository files were changed. This archive is a standalone rebuild project based on the inspected V3 source and audit. It must be reviewed and built before it is merged into the repository.
