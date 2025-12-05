package com.signaldrivelogger.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents SIM profile information.
 * Normalizes static SIM data to avoid repeating strings in every signal row.
 */
@Entity(tableName = "sim_profiles")
data class SimProfileEntity(
    @PrimaryKey
    val subscription_id: Int,
    val sim_slot_index: Int,
    val sim_operator_name: String = "",
    val sim_mcc: String = "",
    val sim_mnc: String = "",
    val sim_display_name: String = "",
    val is_embedded: Boolean = false,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)
