package com.signaldrivelogger.data.sync

import com.signaldrivelogger.data.database.entities.SignalRecordEntity
import kotlinx.serialization.Serializable

/**
 * Phase 3.3: JSON payload structure for cloud sync.
 * Batches records for efficient HTTP POST requests.
 */
@Serializable
data class SyncPayload(
    val device_id: String,
    val session_id: String,
    val records: List<SignalRecordPayload>
)

@Serializable
data class SignalRecordPayload(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed_mps: Float? = null,
    val bearing: Float? = null,
    val gps_accuracy: Float? = null,
    val sim_slot_index: Int,
    val subscription_id: Int? = null,
    val network_type_raw: String,
    val network_operator: String,
    val roaming_state: Boolean,
    val is_endc_available: Boolean = false,
    val nr_state: String? = null,
    val override_network_type: String? = null,
    val dbm: Int,
    val asu: Int = 0,
    val lte_earfcn: Int? = null,
    val lte_bandwidth: Int? = null,
    val lte_cqi: Int? = null,
    val lte_rssi: Int? = null,
    val nr_arfcn: Int? = null,
    val nr_csi_rsrp: Int? = null,
    val nr_csi_sinr: Int? = null,
    val nr_ss_rsrp: Int? = null,
    val nr_ss_sinr: Int? = null,
    val cell_id: Int = 0,
    val ci: Int = 0,
    val enb: Int = 0,
    val tac: Int = 0,
    val pci: Int = 0,
    val rsrq: Int = 0,
    val snr: Int = 0,
    val timing_advance: Int = 0
)

/**
 * Converts SignalRecordEntity to SyncPayload format.
 */
fun SignalRecordEntity.toSyncPayload(): SignalRecordPayload {
    return SignalRecordPayload(
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        speed_mps = speed_mps,
        bearing = bearing,
        gps_accuracy = gps_accuracy,
        sim_slot_index = sim_slot_index,
        subscription_id = subscription_id,
        network_type_raw = network_type_raw,
        network_operator = network_operator,
        roaming_state = roaming_state,
        is_endc_available = is_endc_available,
        nr_state = nr_state,
        override_network_type = override_network_type,
        dbm = dbm,
        asu = asu,
        lte_earfcn = lte_earfcn,
        lte_bandwidth = lte_bandwidth,
        lte_cqi = lte_cqi,
        lte_rssi = lte_rssi,
        nr_arfcn = nr_arfcn,
        nr_csi_rsrp = nr_csi_rsrp,
        nr_csi_sinr = nr_csi_sinr,
        nr_ss_rsrp = nr_ss_rsrp,
        nr_ss_sinr = nr_ss_sinr,
        cell_id = cell_id,
        ci = ci,
        enb = enb,
        tac = tac,
        pci = pci,
        rsrq = rsrq,
        snr = snr,
        timing_advance = timing_advance
    )
}
