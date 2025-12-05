package com.signaldrivelogger.data

import android.content.Context
import android.location.Location
import com.signaldrivelogger.data.database.SignalDatabase
import com.signaldrivelogger.data.database.dao.SessionDao
import com.signaldrivelogger.data.database.dao.SignalRecordDao
import com.signaldrivelogger.data.database.dao.SimProfileDao
import com.signaldrivelogger.data.database.entities.SessionEntity
import com.signaldrivelogger.data.database.entities.SimProfileEntity
import com.signaldrivelogger.data.database.mappers.SignalRecordMapper
import com.signaldrivelogger.domain.models.SignalRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

/**
 * Repository that combines location and signal data into SignalRecord objects.
 * Refactored to use Room database with channel-based sequential writing (Phase 1.1).
 */
class SignalRepository(
    private val context: Context,
    private val locationProvider: LocationProvider,
    private val telephonyMonitor: TelephonyMonitor,
    private val fileLogger: FileLogger
) {
    private val database = SignalDatabase.getDatabase(context)
    private val signalRecordDao: SignalRecordDao = database.signalRecordDao()
    private val sessionDao: SessionDao = database.sessionDao()
    private val simProfileDao: SimProfileDao = database.simProfileDao()

    // Channel-based writing for sequential database writes (Phase 1.1)
    private val writeChannel = Channel<SignalRecord>(Channel.UNLIMITED)
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Current session ID
    private var currentSessionId: String? = null
    private var currentFilename: String = "signal_log_${System.currentTimeMillis()}"

    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    // Flow of all records for UI observation (from database)
    val records: Flow<List<SignalRecord>> = signalRecordDao.getAllRecords()
        .map { entities ->
            entities.map { entity ->
                val simProfile = entity.subscription_id?.let { subId ->
                    simProfileDao.getSimProfile(subId)
                }
                SignalRecordMapper.toDomain(
                    entity,
                    simProfile?.let {
                        com.signaldrivelogger.data.database.mappers.SimProfileInfo(
                            simOperatorName = it.sim_operator_name,
                            simMcc = it.sim_mcc,
                            simMnc = it.sim_mnc,
                            simDisplayName = it.sim_display_name,
                            isEmbedded = it.is_embedded
                        )
                    }
                )
            }
        }

    // Phase 2: Flow of unsynced record count for UI sync status indicator
    val unsyncedCount: Flow<Int> = signalRecordDao.getUnsyncedCount()

    init {
        // Start channel consumer for sequential database writes
        startChannelConsumer()
    }

    /**
     * Channel consumer that writes records to database sequentially.
     * This ensures thread-safe, sequential writes without blocking the main flow.
     */
    private fun startChannelConsumer() {
        writeScope.launch {
            for (record in writeChannel) {
                try {
                    val sessionId = currentSessionId ?: return@launch
                    val entity = SignalRecordMapper.toEntity(record, sessionId)
                    signalRecordDao.insert(entity)

                    // Also log to file if logging is active
                    if (_isLogging.value) {
                        fileLogger.logToCsv(record, currentFilename)
                        fileLogger.logToGpx(record, currentFilename)
                    }
                } catch (e: Exception) {
                    // Log error but continue processing
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Starts logging by creating a session and starting file logging.
     */
    suspend fun startLogging(filename: String? = null) {
        if (_isLogging.value) return

        currentFilename = filename ?: "signal_log_${System.currentTimeMillis()}"
        currentSessionId = UUID.randomUUID().toString()

        _isLogging.value = true

        // Create session in database
        val deviceIdManager = DeviceIdManager(context)
        val session = SessionEntity(
            session_id = currentSessionId!!,
            start_time = System.currentTimeMillis(),
            device_id = deviceIdManager.getDeviceId()
        )
        sessionDao.insert(session)

        // Start buffered file logging session
        fileLogger.startNewSession(currentFilename)
    }

    /**
     * Stops logging and closes the session.
     */
    suspend fun stopLogging() {
        _isLogging.value = false

        // Update session end time
        currentSessionId?.let { sessionId ->
            val session = sessionDao.getSession(sessionId)
            session?.let {
                sessionDao.update(it.copy(end_time = System.currentTimeMillis()))
            }
        }

        // Close buffered file logging session
        fileLogger.closeSession()

        currentSessionId = null
    }

    /**
     * Creates a SignalRecord from location and signal data.
     * Updated to include speed, bearing, and GPS accuracy (Phase 2.2).
     */
    suspend fun createRecord(
        location: Location,
        signalData: SignalData,
        simSlotIndex: Int = 0,
        subscriptionId: Int = -1,
        simDisplayName: String = "",
        isEmbedded: Boolean = false
    ): SignalRecord? {
        val strength = signalData.signalStrength

        // Ensure SIM profile exists in database
        if (subscriptionId >= 0) {
            val simProfile = SimProfileEntity(
                subscription_id = subscriptionId,
                sim_slot_index = simSlotIndex,
                sim_operator_name = signalData.simOperatorName,
                sim_mcc = signalData.simMcc,
                sim_mnc = signalData.simMnc,
                sim_display_name = simDisplayName,
                is_embedded = isEmbedded,
                updated_at = System.currentTimeMillis()
            )
            simProfileDao.insertOrUpdate(simProfile)
        }

        return SignalRecord(
            timestamp = Instant.now(),
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            signalStrength = strength,
            cellId = signalData.cellId,
            networkType = signalData.networkType,
            asu = 0, // Will be calculated if needed
            // Phase 2.2: Extract speed, bearing, and accuracy from Location
            speedMps = if (location.hasSpeed()) location.speed else null,
            bearing = if (location.hasBearing()) location.bearing else null,
            gpsAccuracy = if (location.hasAccuracy()) location.accuracy else null,
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
            timingAdvance = signalData.timingAdvance,
            // Phase 2.3: Extended radio data
            isEndcAvailable = signalData.isEndcAvailable,
            nrState = signalData.nrState,
            overrideNetworkType = signalData.overrideNetworkType,
            lteBandwidth = signalData.lteBandwidth,
            nrCsiRsrp = signalData.nrCsiRsrp,
            nrCsiSinr = signalData.nrCsiSinr,
            nrSsRsrp = signalData.nrSsRsrp,
            nrSsSinr = signalData.nrSsSinr
        )
    }

    /**
     * Adds a record via channel for sequential database writing.
     * This is thread-safe and non-blocking.
     */
    suspend fun addRecord(record: SignalRecord) {
        if (!_isLogging.value) return

        // Send to channel for sequential processing
        writeChannel.trySend(record)
    }

    /**
     * Saves all records from current session to file.
     * If no current session, saves all records from database.
     */
    suspend fun saveRecords(filename: String, format: String = "csv") {
        val entities = if (currentSessionId != null) {
            // Save records from current session
            signalRecordDao.getRecordsBySessionSync(currentSessionId!!)
        } else {
            // No active session, save all records from database
            signalRecordDao.getAllRecordsSync()
        }

        val records = entities.map { entity ->
            val simProfile = entity.subscription_id?.let { subId ->
                simProfileDao.getSimProfile(subId)
            }
            SignalRecordMapper.toDomain(
                entity,
                simProfile?.let {
                    com.signaldrivelogger.data.database.mappers.SimProfileInfo(
                        simOperatorName = it.sim_operator_name,
                        simMcc = it.sim_mcc,
                        simMnc = it.sim_mnc,
                        simDisplayName = it.sim_display_name,
                        isEmbedded = it.is_embedded
                    )
                }
            )
        }

        when (format.lowercase()) {
            "csv" -> fileLogger.saveRecordsToCsv(records, filename)
            "gpx" -> fileLogger.saveRecordsToGpx(records, filename)
        }
    }

    /**
     * Clears all records from database (use with caution).
     */
    suspend fun clearRecords() {
        currentSessionId?.let { sessionId ->
            signalRecordDao.deleteRecordsBySession(sessionId)
        }
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
     * Imports records from a CSV file and saves them to database.
     */
    suspend fun importFromCsv(inputStream: java.io.InputStream): Result<String> {
        return try {
            val parseResult = CsvParser.parse(inputStream)
            when (parseResult) {
                is CsvParseResult.Success -> {
                    // Create a new session for imported records
                    val importSessionId = UUID.randomUUID().toString()
                    val session = SessionEntity(
                        session_id = importSessionId,
                        start_time = System.currentTimeMillis(),
                        end_time = System.currentTimeMillis()
                    )
                    sessionDao.insert(session)

                    // Insert all records
                    val entities = parseResult.records.map { record ->
                        SignalRecordMapper.toEntity(record, importSessionId)
                    }
                    signalRecordDao.insertAll(entities)

                    Result.success("Successfully imported ${parseResult.records.size} records")
                }
                is CsvParseResult.Error -> {
                    Result.failure(Exception(parseResult.message))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            inputStream.close()
        }
    }

    /**
     * Cleanup on destroy.
     */
    fun cleanup() {
        writeScope.cancel()
        writeChannel.close()
    }
}
