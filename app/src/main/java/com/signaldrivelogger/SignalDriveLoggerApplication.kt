package com.signaldrivelogger

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.signaldrivelogger.data.FileLogger
import com.signaldrivelogger.data.LocationProvider
import com.signaldrivelogger.data.MultiSimMonitor
import com.signaldrivelogger.data.SignalRepository
import com.signaldrivelogger.data.TelephonyMonitor
import com.signaldrivelogger.data.sync.SyncWorker
import java.util.concurrent.TimeUnit

class SignalDriveLoggerApplication : Application() {
    // Shared repository instance
    lateinit var signalRepository: SignalRepository
        private set

    lateinit var locationProvider: LocationProvider
        private set

    lateinit var telephonyMonitor: TelephonyMonitor
        private set

    lateinit var fileLogger: FileLogger
        private set

    lateinit var multiSimMonitor: MultiSimMonitor
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize shared instances
        locationProvider = LocationProvider(this)
        telephonyMonitor = TelephonyMonitor(this)
        fileLogger = FileLogger(this)
        multiSimMonitor = MultiSimMonitor(this)
        signalRepository = SignalRepository(this, locationProvider, telephonyMonitor, fileLogger)

        // Phase 1: Schedule periodic background sync
        scheduleBackgroundSync()
    }

    /**
     * Phase 1: Schedules periodic background sync using WorkManager.
     * Syncs every 15 minutes when network is available and battery is not low.
     */
    private fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CloudSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
