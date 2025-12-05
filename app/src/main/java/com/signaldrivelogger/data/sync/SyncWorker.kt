package com.signaldrivelogger.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import com.signaldrivelogger.data.database.SignalDatabase
import com.signaldrivelogger.data.database.dao.SessionDao
import com.signaldrivelogger.data.database.dao.SignalRecordDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Phase 3.2: WorkManager worker for syncing signal records to cloud.
 * Batches records and syncs when network is available and battery is not low.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = SignalDatabase.getDatabase(context)
    private val signalRecordDao: SignalRecordDao = database.signalRecordDao()
    private val sessionDao: SessionDao = database.sessionDao()

    // Batch size for efficient syncing
    private val BATCH_SIZE = 50

    // API endpoint from BuildConfig (Phase 1: Externalized configuration)
    private val API_ENDPOINT = com.signaldrivelogger.BuildConfig.SYNC_API_ENDPOINT

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get device ID using DeviceIdManager (Phase 3: Privacy compliance)
            val deviceIdManager = com.signaldrivelogger.data.DeviceIdManager(applicationContext)
            val deviceId = deviceIdManager.getDeviceId()

            // Get all unsynced sessions
            val unsyncedSessions = sessionDao.getUnsyncedSessions()

            if (unsyncedSessions.isEmpty()) {
                return@withContext Result.success()
            }

            // Process each session
            for (session in unsyncedSessions) {
                val unsyncedRecords = signalRecordDao.getUnsyncedRecordsBySession(session.session_id)

                if (unsyncedRecords.isEmpty()) {
                    // No records to sync, mark session as synced
                    sessionDao.markSessionAsSynced(session.session_id)
                    continue
                }

                // Batch records
                val batches = unsyncedRecords.chunked(BATCH_SIZE)

                for (batch in batches) {
                    // Convert to payload
                    val payload = SyncPayload(
                        device_id = deviceId,
                        session_id = session.session_id,
                        records = batch.map { it.toSyncPayload() }
                    )

                    // Sync to API
                    val success = syncToApi(payload)

                    if (success) {
                        // Mark records as synced (via session)
                        sessionDao.markSessionAsSynced(session.session_id)
                    } else {
                        // Retry on failure
                        return@withContext Result.retry()
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    /**
     * Syncs payload to API endpoint.
     */
    private suspend fun syncToApi(payload: SyncPayload): Boolean {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonPayload = json.encodeToString(payload)

            val url = URL(API_ENDPOINT)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        /**
         * Creates sync constraints (network required, battery not low).
         */
        fun createConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
        }
    }
}
