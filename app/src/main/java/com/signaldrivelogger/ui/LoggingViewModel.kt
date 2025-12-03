package com.signaldrivelogger.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.signaldrivelogger.data.FileLogger
import com.signaldrivelogger.data.LocationProvider
import com.signaldrivelogger.data.SignalRepository
import com.signaldrivelogger.data.SignalData
import com.signaldrivelogger.data.TelephonyMonitor
import com.signaldrivelogger.data.MultiSimMonitor
import com.signaldrivelogger.domain.models.SignalRecord
import com.signaldrivelogger.service.LoggingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Data class representing a SIM card for UI display.
 */
data class SimInfo(
    val subscriptionId: Int,
    val displayName: String,
    val slotIndex: Int,
    val isEmbedded: Boolean
)

/**
 * ViewModel for managing logging state and UI data.
 */
class LoggingViewModel(application: Application) : AndroidViewModel(application) {
    // Use shared instances from Application
    private val app = application as com.signaldrivelogger.SignalDriveLoggerApplication
    private val locationProvider = app.locationProvider
    private val telephonyMonitor = app.telephonyMonitor
    private val fileLogger = app.fileLogger
    private val signalRepository = app.signalRepository
    private val multiSimMonitor = app.multiSimMonitor

    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    private val _records = MutableStateFlow<List<SignalRecord>>(emptyList())
    val records: StateFlow<List<SignalRecord>> = _records.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    // Track signal data for all SIMs by subscription ID
    private val _currentSignalDataBySim = MutableStateFlow<Map<Int, SignalData>>(emptyMap())
    val currentSignalDataBySim: StateFlow<Map<Int, SignalData>> = _currentSignalDataBySim.asStateFlow()

    // Legacy single SIM support (for backward compatibility)
    val currentSignalData: StateFlow<SignalData?> = _currentSignalDataBySim.asStateFlow().map {
        it.values.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _filename = MutableStateFlow("signal_log_${System.currentTimeMillis()}")
    val filename: StateFlow<String> = _filename.asStateFlow()

    // SIM selection for map filtering
    private val _availableSims = MutableStateFlow<List<SimInfo>>(emptyList())
    val availableSims: StateFlow<List<SimInfo>> = _availableSims.asStateFlow()

    private val _selectedSimIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedSimIds: StateFlow<Set<Int>> = _selectedSimIds.asStateFlow()

    init {
        // Observe records from repository
        viewModelScope.launch {
            signalRepository.records.collect { records ->
                _records.value = records
            }
        }

        // Observe signal data from all SIMs
        viewModelScope.launch {
            multiSimMonitor.getAllSimSignalUpdates().collect { simSignalData ->
                val currentMap = _currentSignalDataBySim.value.toMutableMap()
                currentMap[simSignalData.subscriptionId] = simSignalData.signalData
                _currentSignalDataBySim.value = currentMap
            }
        }
    }

    fun startLogging() {
        if (_isLogging.value) return

        _isLogging.value = true
        val filename = _filename.value
        LoggingService.startLogging(getApplication(), filename)
    }

    fun stopLogging() {
        if (!_isLogging.value) return

        _isLogging.value = false
        LoggingService.stopLogging(getApplication())
    }

    fun setFilename(filename: String) {
        _filename.value = filename
        signalRepository.setFilename(filename)
    }

    fun saveFile(format: String = "csv") {
        viewModelScope.launch {
            signalRepository.saveRecords(_filename.value, format)
        }
    }

    fun exportFile(): java.io.File? {
        return fileLogger.getFile(_filename.value, "csv")
    }

    fun clearRecords() {
        signalRepository.clearRecords()
    }

    fun hasLocationPermission(): Boolean {
        return locationProvider.hasPermission()
    }

    fun hasPhoneStatePermission(): Boolean {
        return telephonyMonitor.hasPermission()
    }

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _importSuccess = MutableStateFlow<String?>(null)
    val importSuccess: StateFlow<String?> = _importSuccess.asStateFlow()

    /**
     * Imports records from a CSV file.
     */
    suspend fun importCsvFile(inputStream: java.io.InputStream) {
        val result = signalRepository.importFromCsv(inputStream)
        result.fold(
            onSuccess = { message ->
                _importSuccess.value = message
            },
            onFailure = { error ->
                _importError.value = error.message ?: "Unknown error importing CSV file"
            }
        )
    }

    fun clearImportError() {
        _importError.value = null
    }

    fun clearImportSuccess() {
        _importSuccess.value = null
    }

    /**
     * Loads available SIM cards from the device.
     */
    fun loadAvailableSims() {
        viewModelScope.launch {
            val subscriptionIds = multiSimMonitor.getActiveSubscriptionIds()
            val sims = subscriptionIds.map { subId ->
                SimInfo(
                    subscriptionId = subId,
                    displayName = multiSimMonitor.getSimDisplayName(subId),
                    slotIndex = multiSimMonitor.getSimSlotIndex(subId),
                    isEmbedded = multiSimMonitor.isEmbeddedSim(subId)
                )
            }
            _availableSims.value = sims

            // If no SIMs are selected yet, select all by default
            if (_selectedSimIds.value.isEmpty() && sims.isNotEmpty()) {
                _selectedSimIds.value = sims.map { it.subscriptionId }.toSet()
            }
        }
    }

    /**
     * Toggles selection of a SIM card.
     */
    fun toggleSimSelection(subscriptionId: Int) {
        val current = _selectedSimIds.value.toMutableSet()
        if (current.contains(subscriptionId)) {
            current.remove(subscriptionId)
        } else {
            current.add(subscriptionId)
        }
        _selectedSimIds.value = current
    }

    /**
     * Gets filtered records for selected SIMs.
     */
    fun getFilteredRecords(): List<SignalRecord> {
        val selectedIds = _selectedSimIds.value
        if (selectedIds.isEmpty()) {
            return _records.value // Show all if none selected
        }
        return _records.value.filter { it.subscriptionId in selectedIds }
    }
}
