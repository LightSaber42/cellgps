package com.signaldrivelogger.data

import android.location.Location
import com.signaldrivelogger.domain.models.SignalRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/**
 * Repository that combines location and signal data into SignalRecord objects.
 */
class SignalRepository(
    private val locationProvider: LocationProvider,
    private val telephonyMonitor: TelephonyMonitor,
    private val fileLogger: FileLogger
) {
    private val _records = MutableStateFlow<List<SignalRecord>>(emptyList())
    val records: StateFlow<List<SignalRecord>> = _records.asStateFlow()

    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    private var currentFilename: String = "signal_log_${System.currentTimeMillis()}"

    /**
     * Starts logging by combining location and signal updates.
     */
    fun startLogging(filename: String? = null) {
        if (_isLogging.value) return

        currentFilename = filename ?: "signal_log_${System.currentTimeMillis()}"
        _isLogging.value = true
    }

    /**
     * Stops logging.
     */
    fun stopLogging() {
        _isLogging.value = false
    }

    /**
     * Creates a SignalRecord from location and signal data.
     */
    suspend fun createRecord(
        location: Location,
        signalData: com.signaldrivelogger.data.SignalData,
        dataRate: Int,
        simSlotIndex: Int = 0,
        subscriptionId: Int = -1,
        simDisplayName: String = "",
        isEmbedded: Boolean = false
    ): SignalRecord? {
        val strength = signalData.signalStrength

        return SignalRecord(
            timestamp = Instant.now(),
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude, // GPS altitude in meters
            signalStrength = strength,
            cellId = signalData.cellId,
            dataRateKbps = dataRate,
            networkType = signalData.networkType,
            dataState = signalData.dataState,
            dataActivity = signalData.dataActivity,
            isRoaming = signalData.isRoaming,
            simState = signalData.simState,
            simOperatorName = signalData.simOperatorName,
            simMcc = signalData.simMcc,
            simMnc = signalData.simMnc,
            operatorName = signalData.operatorName,
            mcc = signalData.mcc,
            mnc = signalData.mnc,
            phoneType = signalData.phoneType,
            simSlotIndex = simSlotIndex,
            subscriptionId = subscriptionId,
            simDisplayName = simDisplayName,
            isEmbedded = isEmbedded,
            ci = signalData.ci,
            enb = signalData.enb,
            tac = signalData.tac,
            pci = signalData.pci,
            bandwidth = signalData.bandwidth,
            earfcn = signalData.earfcn,
            nrarfcn = signalData.nrarfcn,
            rssi = signalData.rssi,
            rsrq = signalData.rsrq,
            snr = signalData.snr,
            cqi = signalData.cqi,
            timingAdvance = signalData.timingAdvance
        )
    }

    /**
     * Adds a record to the list and logs it to file.
     */
    suspend fun addRecord(record: SignalRecord) {
        _records.value = _records.value + record

        if (_isLogging.value) {
            // Log to both CSV and GPX
            fileLogger.logToCsv(record, currentFilename)
            fileLogger.logToGpx(record, currentFilename)
        }
    }

    /**
     * Saves all records to file.
     */
    suspend fun saveRecords(filename: String, format: String = "csv") {
        val records = _records.value
        when (format.lowercase()) {
            "csv" -> fileLogger.saveRecordsToCsv(records, filename)
            "gpx" -> {
                fileLogger.saveRecordsToGpx(records, filename)
            }
        }
    }

    /**
     * Clears all records from memory.
     */
    fun clearRecords() {
        _records.value = emptyList()
    }

    /**
     * Gets the current filename.
     */
    fun getCurrentFilename(): String = currentFilename

    /**
     * Sets the filename for logging.
     */
    fun setFilename(filename: String) {
        currentFilename = filename
    }

    /**
     * Imports records from a CSV file and appends them to existing records.
     */
    suspend fun importFromCsv(inputStream: java.io.InputStream): Result<String> {
        return try {
            val parseResult = com.signaldrivelogger.data.CsvParser.parse(inputStream)
            when (parseResult) {
                is com.signaldrivelogger.data.CsvParseResult.Success -> {
                    // Append imported records to existing ones
                    _records.value = _records.value + parseResult.records
                    Result.success("Successfully imported ${parseResult.records.size} records")
                }
                is com.signaldrivelogger.data.CsvParseResult.Error -> {
                    Result.failure(Exception(parseResult.message))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            inputStream.close()
        }
    }
}
