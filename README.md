# FocusLock V3 Rebuild

A Java-based Android rebuild using a layered architecture and no third-party runtime dependencies.

## Important preservation notes

- Existing V1/V2 preferences are stored in `FocusLockDatabaseV2`; the rebuild continues to read that exact preference file.
- The existing `blocked_apps` string set is retained and edited in place.
- Existing `history_YYYY-MM-DD` and `subject_YYYY-MM-DD_Subject` entries are imported into the V3 database without deleting or rewriting the originals.
- Legacy active-session preferences are adopted when V3 session state is first initialized.
- The app does not provide an ordinary Stop/End Session action.
- App selection is disabled while a session is active.
- Only apps explicitly selected by the user are blocked. Settings, phone, emergency, system UI and the home launcher are intentionally excluded.

## Architecture

- `domain/SessionEngine`: time-based session state machine, pause limit and auto-resume deadline.
- `data/LegacyPreferences`: compatibility boundary for V1/V2 preferences.
- `data/StudyDatabaseHelper`: versioned SQLite schema and non-destructive legacy-history import.
- `data/SessionRepository`: idempotent session completion and study summaries.
- `service/FocusTimerService`: foreground timer notification and lifecycle.
- `accessibility/FocusAccessibilityService`: user-selected app deterrence.
- `ui`: activities and Android views.

## Build

Open this folder in Android Studio (Jellyfish or newer) and sync the Gradle project. The project uses Android Gradle Plugin 8.7.3, Gradle 8.9, Java 17 and Android SDK 35 (target SDK 34).

CI installs Gradle 8.9 and Android SDK 35, then runs `gradle assembleDebug`.

## Current milestone limitations

This is the first rebuild foundation, not a finished release. It includes timer/session foundations, legacy history import, selected-app blocking, dashboard and history screens. Planner, chapter/topic management, revision workflows, advanced analytics, backup restore, release signing and full device testing remain later milestones.

## Accessibility and permissions

FocusLock uses Android Accessibility Service only to detect when a user-selected app comes to the foreground and redirect away from it during an active session. This is a deterrent, not a system-level lock. It cannot prevent uninstall, force-stop, permission revocation, safe mode or all OS/manufacturer bypasses. Essential device settings and emergency/phone apps remain accessible.
