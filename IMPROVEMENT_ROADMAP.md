Based on the code review (ignoring the outdated compilation errors), the core logic for the architecture, database, and signal tracking is **implemented and solid**.

However, there are specific **functional gaps** where the new features (Sync, Velocity) are implemented in the data layer but **not connected** to the application logic or UI.

Here is the revised plan to reach Release Readiness.

### Status Overview

| Feature | Code Status | Action Required |
| :--- | :--- | :--- |
| **Stability (Room/Channel)** | 🟢 **Complete** | Database and threading logic is sound. |
| **Data Structure** | 🟢 **Complete** | Velocity, Bearing, and 5G metrics are in the Entity. |
| **Cloud Sync Logic** | 🟡 **Disconnected** | `SyncWorker` exists but is **never scheduled**. API URL is a placeholder. |
| **User Interface** | 🟠 **Incomplete** | Velocity is captured but not shown. No UI for Sync status. |
| **Privacy/Ids** | 🟠 **Needs Change** | Uses `ANDROID_ID`, which Google discourages for this use case. |

---

### Revised Plan: Path to Release

#### Phase 1: Activate Cloud Sync (The "Wiring" Phase)

The `SyncWorker` class exists but is currently dead code because nothing triggers it.

1.  **Externalize API Configuration**
    *   **Task:** Move the hardcoded URL `https://your-railway-app.app/...` out of `SyncWorker.kt`.
    *   **Action:** Add `buildConfigField` in `app/build.gradle.kts` so the URL can be swapped for Dev/Prod builds easily.

2.  **Schedule Background Sync**
    *   **Task:** Initialize WorkManager.
    *   **Location:** `SignalDriveLoggerApplication.kt` -> `onCreate()`.
    *   **Implementation:**
        ```kotlin
        // Example logic to add
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CloudSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        ```

3.  **Add Manual Trigger**
    *   **Task:** Allow users to force a sync (e.g., after finishing a drive).
    *   **Location:** `LoggingViewModel` and `MainScreen`.
    *   **Action:** Add a `syncNow()` function in ViewModel that enqueues a `OneTimeWorkRequest`.

#### Phase 2: UI Visibility (Data Visualization)

The data is being captured, but the user cannot see the new metrics.

1.  **Display Velocity & Bearing**
    *   **Task:** Update `SignalInfoCard` in `MainScreen.kt`.
    *   **Action:** Observe `speedMps` from the latest record. Convert m/s to mph/kph based on locale or preference.
    *   *Note:* Display "0" or "Stationary" if speed is null/low to avoid UI noise.

2.  **Add Sync Status Indicator**
    *   **Task:** Show the user if data is pending upload.
    *   **Action:**
        *   In `SignalRecordDao`, add a Flow: `getUnsyncedCount(): Flow<Int>`.
        *   In `MainScreen`, add a small icon/text:
            *   ✅ (All Synced)
            *   ⏳ (50 Pending)

#### Phase 3: Privacy & ID Management

1.  **Replace Android ID**
    *   **Task:** Stop using `Settings.Secure.ANDROID_ID` in `SignalRepository` and `SyncWorker`.
    *   **Reason:** Google Play policies restrict persistent hardware identifiers. Using them can trigger extra review scrutiny or rejection.
    *   **Action:** Generate a random UUID on the first app launch, save it to `SharedPreferences` (or EncryptedSharedPreferences), and use that as the `device_id`.

#### Phase 4: Final Validation

1.  **Verify WakeLock Logic**
    *   The `LoggingService` now refreshes the WakeLock every 30 minutes. Verify this logic releases the lock properly when the user taps "Stop Logging" manually to prevent battery drain.

2.  **Database Migration Check**
    *   **Critical:** You are currently using `.fallbackToDestructiveMigration()`. This will **delete all user data** if you change the schema schema again.
    *   **Decision:** Before releasing v1.0, ensure the schema is final. If you release v1.0 and then change the schema for v1.1, you must remove `fallbackToDestructiveMigration` and provide a migration path, or users will lose their logs on update.

### Recommended Order of Execution

1.  **Privacy Fix:** Replace `ANDROID_ID` with UUID (low effort, high safety).
2.  **Wiring:** Schedule the `SyncWorker` in the Application class.
3.  **Configuration:** Set the real API URL in Gradle.
4.  **UI:** Add the Speed display to the main screen.
5.  **Release:** Generate Signed Bundle (`.aab`).