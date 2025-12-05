package com.signaldrivelogger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.signaldrivelogger.data.database.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SessionEntity.
 */
@Dao
interface SessionDao {
    /**
     * Insert a session.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    /**
     * Update a session.
     */
    @Update
    suspend fun update(session: SessionEntity)

    /**
     * Get a session by ID.
     */
    @Query("SELECT * FROM sessions WHERE session_id = :sessionId")
    suspend fun getSession(sessionId: String): SessionEntity?

    /**
     * Get all sessions.
     */
    @Query("SELECT * FROM sessions ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    /**
     * Get all unsynced sessions.
     */
    @Query("SELECT * FROM sessions WHERE is_synced = 0 ORDER BY start_time ASC")
    suspend fun getUnsyncedSessions(): List<SessionEntity>

    /**
     * Mark a session as synced.
     */
    @Query("UPDATE sessions SET is_synced = 1 WHERE session_id = :sessionId")
    suspend fun markSessionAsSynced(sessionId: String)

    /**
     * Delete a session.
     */
    @Query("DELETE FROM sessions WHERE session_id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    /**
     * Delete old sessions (older than specified days).
     */
    @Query("DELETE FROM sessions WHERE start_time < :cutoffTimestamp")
    suspend fun deleteOldSessions(cutoffTimestamp: Long): Int
}
