package com.signaldrivelogger.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single signal measurement record with location and telephony data.
 * Based on the improved data model from Phase 2 of the roadmap.
 */
@Entity(
    tableName = "signal_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SimProfileEntity::class,
            parentColumns = ["subscription_id"],
            childColumns = ["subscription_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["subscription_id"]),
        Index(value = ["timestamp"])
    ]
)
data class SignalRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val session_id: String,
    val timestamp: Long,

    // Location & Velocity
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed_mps: Float? = null, // Velocity in meters per second
    val bearing: Float? = null, // Direction of travel (0-360)
    val gps_accuracy: Float? = null, // GPS accuracy in meters

    // Identifiers
    val sim_slot_index: Int,
    val subscription_id: Int? = null, // Foreign key to SimProfileEntity

    // Network Identity
    val network_type_raw: String, // e.g., LTE, NR
    val network_operator: String, // MCC+MNC (e.g., 23420)
    val roaming_state: Boolean,

    // 5G / Advanced LTE Specifics
    val is_endc_available: Boolean = false, // "EN-DC Available"
    val nr_state: String? = null, // "NR State" (NONE, RESTRICTED, NOT_RESTRICTED, CONNECTED)
    val override_network_type: String? = null, // e.g., LTE_CA, NR_NSA, NR_ADVANCED

    // Signal Metrics
    val dbm: Int, // RSRP in dBm
    val asu: Int = 0, // Additional Signal Unit

    // LTE Specifics
    val lte_earfcn: Int? = null,
    val lte_bandwidth: Int? = null, // DL Bandwidth
    val lte_cqi: Int? = null,
    val lte_rssi: Int? = null,

    // NR (5G) Specifics
    val nr_arfcn: Int? = null,
    val nr_csi_rsrp: Int? = null,
    val nr_csi_sinr: Int? = null,
    val nr_ss_rsrp: Int? = null,
    val nr_ss_sinr: Int? = null,

    // Legacy fields (for backward compatibility during migration)
    val cell_id: Int = 0,
    val ci: Int = 0, // Cell Identity
    val enb: Int = 0, // eNodeB ID
    val tac: Int = 0, // Tracking Area Code
    val pci: Int = 0, // Physical Cell ID
    val rsrq: Int = 0, // Reference Signal Received Quality in dB
    val snr: Int = 0, // Signal-to-Noise Ratio in dB
    val timing_advance: Int = 0 // Timing Advance
)
