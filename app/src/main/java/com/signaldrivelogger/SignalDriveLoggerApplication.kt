package com.signaldrivelogger

import android.app.Application
import com.signaldrivelogger.data.FileLogger
import com.signaldrivelogger.data.LocationProvider
import com.signaldrivelogger.data.MultiSimMonitor
import com.signaldrivelogger.data.SignalRepository
import com.signaldrivelogger.data.TelephonyMonitor

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
        signalRepository = SignalRepository(locationProvider, telephonyMonitor, fileLogger)
    }
}
