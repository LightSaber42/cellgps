package com.signaldrivelogger.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.signaldrivelogger.R
import com.signaldrivelogger.data.FileLogger
import com.signaldrivelogger.data.LocationProvider
import com.signaldrivelogger.data.MultiSimMonitor
import com.signaldrivelogger.data.SignalRepository
import com.signaldrivelogger.data.SignalData
import com.signaldrivelogger.data.SimSignalData
import com.signaldrivelogger.data.TelephonyMonitor
import com.signaldrivelogger.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Foreground service that continuously logs signal and location data.
 */
class LoggingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var locationProvider: LocationProvider
    private lateinit var telephonyMonitor: TelephonyMonitor
    private lateinit var multiSimMonitor: MultiSimMonitor
    private lateinit var signalRepository: SignalRepository
    private lateinit var fileLogger: FileLogger

    private var currentLocation: Location? = null
    private val currentSignalDataBySim = mutableMapOf<Int, SignalData>() // Map of subscriptionId -> SignalData
    private var currentIntent: Intent? = null

    // WakeLock to keep CPU awake during logging
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeLockRefreshJob: kotlinx.coroutines.Job? = null
    private var isLoggingActive = false

    override fun onCreate() {
        super.onCreate()

        // Use shared instances from Application
        val app = application as com.signaldrivelogger.SignalDriveLoggerApplication
        locationProvider = app.locationProvider
        telephonyMonitor = app.telephonyMonitor
        multiSimMonitor = app.multiSimMonitor
        fileLogger = app.fileLogger
        signalRepository = app.signalRepository

        // Initialize WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CellSignalLogger::LoggingWakelock"
        )

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Starting logging..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentIntent = intent
        when (intent?.action) {
            ACTION_START_LOGGING -> startLogging()
            ACTION_STOP_LOGGING -> stopLogging()
        }
        return START_STICKY // Restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLogging() {
        isLoggingActive = true

        // Acquire WakeLock with timeout-based refresh (Phase 1.3)
        // Refresh every 30 minutes instead of hardcoded 10 hours
        wakeLock?.acquire(30 * 60 * 1000L) // 30 minutes

        // Start periodic refresh job
        wakeLockRefreshJob = serviceScope.launch {
            while (isLoggingActive) {
                kotlinx.coroutines.delay(25 * 60 * 1000L) // Refresh every 25 minutes
                if (isLoggingActive && wakeLock?.isHeld == true) {
                    wakeLock?.release()
                    wakeLock?.acquire(30 * 60 * 1000L)
                }
            }
        }

        val filename = currentIntent?.getStringExtra(EXTRA_FILENAME) ?: "signal_log_${System.currentTimeMillis()}"
        signalRepository.setFilename(filename)
        serviceScope.launch {
            signalRepository.startLogging(filename)
        }

        // Start location updates
        locationProvider.getLocationUpdates()
            .onEach { location ->
                currentLocation = location
                updateNotification("Logging: ${location.latitude.format(6)}, ${location.longitude.format(6)}")
                // Records are created when signal data arrives for each SIM
            }
            .launchIn(serviceScope)

        // Start signal updates from all SIMs
        multiSimMonitor.getAllSimSignalUpdates()
            .onEach { simSignalData: SimSignalData ->
                currentSignalDataBySim[simSignalData.subscriptionId] = simSignalData.signalData
                val simCount = currentSignalDataBySim.size
                val signalData = simSignalData.signalData
                updateNotification("SIM ${simSignalData.subscriptionId}: ${signalData.signalStrength} dBm (${signalData.networkType}) - $simCount SIM(s)")
                tryCreateRecordForSim(simSignalData.subscriptionId, simSignalData.signalData)
            }
            .launchIn(serviceScope)
    }

    private fun stopLogging() {
        isLoggingActive = false
        wakeLockRefreshJob?.cancel()
        wakeLockRefreshJob = null

        // Stop collecting first
        locationProvider.stopLocationUpdates()
        telephonyMonitor.stopMonitoring()
        currentSignalDataBySim.clear()

        // Launch cleanup job - wait for all file operations to complete before stopping service
        serviceScope.launch {
            // 1. Stop repo (closes active session buffers)
            signalRepository.stopLogging()

            // 2. Finalize GPX (appends footer if needed)
            fileLogger.finalizeGpx(signalRepository.getCurrentFilename())

            // 3. Now it is safe to stop
            updateNotification("Logging stopped")
            stopForeground(STOP_FOREGROUND_REMOVE)

            // Release WakeLock immediately (Phase 1.3)
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }

            stopSelf()
        }
    }

    private fun tryCreateRecordForSim(subscriptionId: Int, signalData: SignalData) {
        val location = currentLocation ?: return

        serviceScope.launch {
            val simSlotIndex = multiSimMonitor.getSimSlotIndex(subscriptionId)
            val simDisplayName = multiSimMonitor.getSimDisplayName(subscriptionId)
            val isEmbedded = multiSimMonitor.isEmbeddedSim(subscriptionId)

            val record = signalRepository.createRecord(
                location = location,
                signalData = signalData,
                simSlotIndex = simSlotIndex,
                subscriptionId = subscriptionId,
                simDisplayName = simDisplayName,
                isEmbedded = isEmbedded
            )

            record?.let {
                signalRepository.addRecord(it)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Signal Logging Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service for logging signal strength"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cell Signal Logger")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        locationProvider.stopLocationUpdates()
        telephonyMonitor.stopMonitoring()

        // Release WakeLock if still held
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    companion object {
        const val ACTION_START_LOGGING = "com.signaldrivelogger.START_LOGGING"
        const val ACTION_STOP_LOGGING = "com.signaldrivelogger.STOP_LOGGING"
        const val EXTRA_FILENAME = "filename"
        private const val CHANNEL_ID = "logging_service_channel"
        private const val NOTIFICATION_ID = 1

        fun startLogging(context: Context, filename: String? = null) {
            val intent = Intent(context, LoggingService::class.java).apply {
                action = ACTION_START_LOGGING
                filename?.let { putExtra(EXTRA_FILENAME, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopLogging(context: Context) {
            val intent = Intent(context, LoggingService::class.java).apply {
                action = ACTION_STOP_LOGGING
            }
            context.stopService(intent)
        }
    }
}

// Extension function to format doubles
private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
