package com.signaldrivelogger.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a logging session (drive/tracking session).
 * Groups signal logs by session for cloud sync and data management.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val session_id: String,
    val start_time: Long,
    val end_time: Long? = null,
    val device_id: String? = null,
    val is_synced: Boolean = false,
    val created_at: Long = System.currentTimeMillis()
)
