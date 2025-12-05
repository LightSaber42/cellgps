package com.signaldrivelogger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.signaldrivelogger.data.database.entities.SignalRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SignalRecordEntity.
 */
@Dao
interface SignalRecordDao {
    /**
     * Insert a signal record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SignalRecordEntity): Long

    /**
     * Insert multiple signal records in a transaction.
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<SignalRecordEntity>)

    /**
     * Get all records for a session.
     */
    @Query("SELECT * FROM signal_logs WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun getRecordsBySession(sessionId: String): Flow<List<SignalRecordEntity>>

    /**
     * Get all unsynced records.
     */
    @Query("SELECT * FROM signal_logs WHERE session_id IN (SELECT session_id FROM sessions WHERE is_synced = 0) ORDER BY timestamp ASC")
    suspend fun getUnsyncedRecords(): List<SignalRecordEntity>

    /**
     * Get unsynced records for a specific session.
     */
    @Query("SELECT * FROM signal_logs WHERE session_id = :sessionId AND session_id IN (SELECT session_id FROM sessions WHERE is_synced = 0) ORDER BY timestamp ASC")
    suspend fun getUnsyncedRecordsBySession(sessionId: String): List<SignalRecordEntity>

    /**
     * Get all records as Flow for UI observation.
     */
    @Query("SELECT * FROM signal_logs ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<SignalRecordEntity>>

    /**
     * Get all records synchronously (non-Flow for batch operations).
     */
    @Query("SELECT * FROM signal_logs ORDER BY timestamp ASC")
    suspend fun getAllRecordsSync(): List<SignalRecordEntity>

    /**
     * Get records by session ID (non-Flow for batch operations).
     */
    @Query("SELECT * FROM signal_logs WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getRecordsBySessionSync(sessionId: String): List<SignalRecordEntity>

    /**
     * Delete records older than specified days.
     */
    @Query("DELETE FROM signal_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldRecords(cutoffTimestamp: Long): Int

    /**
     * Delete records for a specific session.
     */
    @Query("DELETE FROM signal_logs WHERE session_id = :sessionId")
    suspend fun deleteRecordsBySession(sessionId: String): Int

    /**
     * Delete all records from the database.
     */
    @Query("DELETE FROM signal_logs")
    suspend fun deleteAllRecords(): Int

    /**
     * Get count of records for a session.
     */
    @Query("SELECT COUNT(*) FROM signal_logs WHERE session_id = :sessionId")
    suspend fun getRecordCount(sessionId: String): Int

    /**
     * Phase 2: Get count of unsynced records as a Flow for UI observation.
     */
    @Query("SELECT COUNT(*) FROM signal_logs WHERE session_id IN (SELECT session_id FROM sessions WHERE is_synced = 0)")
    fun getUnsyncedCount(): Flow<Int>
}
