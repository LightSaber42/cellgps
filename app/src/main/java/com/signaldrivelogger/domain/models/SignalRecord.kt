package com.signaldrivelogger.domain.models

import java.time.Instant

/**
 * Represents a single signal measurement record with location and telephony data.
 * Updated per Phase 2: Removed dataRateKbps (estimated data), added velocity and extended radio data.
 */
data class SignalRecord(
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0, // GPS altitude in meters
    val signalStrength: Int, // RSRP in dBm (negative value, e.g., -85)
    val cellId: Int,
    // REMOVED: dataRateKbps - was estimated data, not real measurement
    val networkType: String, // "4G", "5G", "LTE", etc.
    val asu: Int = 0, // Additional Signal Unit (optional)

    // Phase 2.2: Velocity & Location Metadata
    val speedMps: Float? = null, // Meters per second (raw from GPS)
    val bearing: Float? = null, // Direction of travel (0-360)
    val gpsAccuracy: Float? = null, // GPS accuracy in meters
    // Extended cell details
    val dataState: String = "Unknown", // Connected, Disconnected, etc.
    val dataActivity: String = "None", // None, Up, Down, InOut
    val isRoaming: Boolean = false,
    val simState: String = "Unknown", // Ready, Absent, etc.
    val simOperatorName: String = "",
    val simMcc: String = "",
    val simMnc: String = "",
    val operatorName: String = "",
    val mcc: String = "",
    val mnc: String = "",
    val phoneType: String = "Unknown", // GSM, CDMA, etc.
    // Multi-SIM support
    val simSlotIndex: Int = 0, // 0, 1, 2, etc.
    val subscriptionId: Int = -1, // Subscription ID
    val simDisplayName: String = "", // Display name for the SIM
    val isEmbedded: Boolean = false, // true for eSIM, false for physical SIM
    // LTE/5G cell details
    val ci: Int = 0, // Cell Identity
    val enb: Int = 0, // eNodeB ID
    val tac: Int = 0, // Tracking Area Code
    val pci: Int = 0, // Physical Cell ID
    val bandwidth: Int = 0, // Bandwidth in kHz
    val earfcn: Int = 0, // E-UTRA Absolute Radio Frequency Channel Number (LTE)
    val nrarfcn: Int = 0, // NR Absolute Radio Frequency Channel Number (5G)
    val rssi: Int = 0, // Received Signal Strength Indicator in dBm
    val rsrq: Int = 0, // Reference Signal Received Quality in dB
    val snr: Int = 0, // Signal-to-Noise Ratio in dB
    val cqi: Int = 0, // Channel Quality Indicator
    val timingAdvance: Int = 0, // Timing Advance

    // Phase 2.3: Extended Radio Data (5G / Advanced LTE)
    val isEndcAvailable: Boolean = false, // "EN-DC Available"
    val nrState: String? = null, // "NR State" (NONE, RESTRICTED, NOT_RESTRICTED, CONNECTED)
    val overrideNetworkType: String? = null, // e.g., LTE_CA, NR_NSA, NR_ADVANCED
    val lteBandwidth: Int? = null, // DL Bandwidth for LTE
    val nrCsiRsrp: Int? = null, // NR CSI-RSRP
    val nrCsiSinr: Int? = null, // NR CSI-SINR
    val nrSsRsrp: Int? = null, // NR SS-RSRP
    val nrSsSinr: Int? = null // NR SS-SINR
) {
    /**
     * Converts signal strength to a normalized value (0.0 to 1.0) for color mapping.
     * MOVED TO UI LAYER per Phase 2.1 - This is a UI presentation concern.
     * Typical RSRP range: -140 dBm (weak) to -50 dBm (strong)
     *
     * @deprecated Use UI layer calculation instead. This method is kept for backward compatibility.
     */
    @Deprecated("Calculate normalized strength in UI layer instead", ReplaceWith("calculateNormalizedStrength(signalStrength)"))
    fun getNormalizedStrength(): Float {
        return calculateNormalizedStrength(signalStrength)
    }

    /**
     * Static helper for UI layer to calculate normalized strength.
     */
    companion object {
        fun calculateNormalizedStrength(signalStrength: Int): Float {
            val minStrength = -140f
            val maxStrength = -50f
            val normalized = (signalStrength - minStrength) / (maxStrength - minStrength)
            return normalized.coerceIn(0f, 1f)
        }
    }

    /**
     * Converts to CSV format
     */
    fun toCsvRow(): String {
        // Updated CSV format: removed dataRateKbps, added speed/bearing/accuracy and extended radio data
        return "${timestamp.toEpochMilli()},$latitude,$longitude,$altitude,${speedMps ?: ""},${bearing ?: ""},${gpsAccuracy ?: ""}," +
                "$signalStrength,$cellId,$networkType,$asu," +
                "$dataState,$dataActivity,$isRoaming,$simState,\"$simOperatorName\",$simMcc,$simMnc,\"$operatorName\",$mcc,$mnc,$phoneType," +
                "$simSlotIndex,$subscriptionId,\"$simDisplayName\",$isEmbedded," +
                "$ci,$enb,$tac,$pci,${lteBandwidth ?: bandwidth},$earfcn,$nrarfcn,$rssi,$rsrq,$snr,$cqi,$timingAdvance," +
                "$isEndcAvailable,${nrState ?: ""},${overrideNetworkType ?: ""},${nrCsiRsrp ?: ""},${nrCsiSinr ?: ""},${nrSsRsrp ?: ""},${nrSsSinr ?: ""}"
    }

    /**
     * Converts to GPX trackpoint format
     */
    fun toGpxTrackpoint(): String {
        return """
            <trkpt lat="$latitude" lon="$longitude">
                <ele>$altitude</ele>
                <time>${timestamp.toString()}</time>
                <extensions>
                    <signal:strength>$signalStrength</signal:strength>
                    <signal:cellId>$cellId</signal:cellId>
                    <signal:speedMps>${speedMps ?: ""}</signal:speedMps>
                    <signal:bearing>${bearing ?: ""}</signal:bearing>
                    <signal:gpsAccuracy>${gpsAccuracy ?: ""}</signal:gpsAccuracy>
                    <signal:networkType>$networkType</signal:networkType>
                    <signal:asu>$asu</signal:asu>
                    <signal:dataState>$dataState</signal:dataState>
                    <signal:dataActivity>$dataActivity</signal:dataActivity>
                    <signal:roaming>$isRoaming</signal:roaming>
                    <signal:simState>$simState</signal:simState>
                    <signal:simOperatorName>$simOperatorName</signal:simOperatorName>
                    <signal:simMcc>$simMcc</signal:simMcc>
                    <signal:simMnc>$simMnc</signal:simMnc>
                    <signal:operatorName>$operatorName</signal:operatorName>
                    <signal:mcc>$mcc</signal:mcc>
                    <signal:mnc>$mnc</signal:mnc>
                    <signal:phoneType>$phoneType</signal:phoneType>
                    <signal:simSlotIndex>$simSlotIndex</signal:simSlotIndex>
                    <signal:subscriptionId>$subscriptionId</signal:subscriptionId>
                    <signal:simDisplayName>$simDisplayName</signal:simDisplayName>
                    <signal:isEmbedded>$isEmbedded</signal:isEmbedded>
                    <signal:ci>$ci</signal:ci>
                    <signal:enb>$enb</signal:enb>
                    <signal:tac>$tac</signal:tac>
                    <signal:pci>$pci</signal:pci>
                    <signal:bandwidth>$bandwidth</signal:bandwidth>
                    <signal:earfcn>$earfcn</signal:earfcn>
                    <signal:nrarfcn>$nrarfcn</signal:nrarfcn>
                    <signal:rssi>$rssi</signal:rssi>
                    <signal:rsrq>$rsrq</signal:rsrq>
                    <signal:snr>$snr</signal:snr>
                    <signal:cqi>$cqi</signal:cqi>
                    <signal:timingAdvance>$timingAdvance</signal:timingAdvance>
                    <signal:isEndcAvailable>$isEndcAvailable</signal:isEndcAvailable>
                    <signal:nrState>${nrState ?: ""}</signal:nrState>
                    <signal:overrideNetworkType>${overrideNetworkType ?: ""}</signal:overrideNetworkType>
                    <signal:lteBandwidth>${lteBandwidth ?: ""}</signal:lteBandwidth>
                    <signal:nrCsiRsrp>${nrCsiRsrp ?: ""}</signal:nrCsiRsrp>
                    <signal:nrCsiSinr>${nrCsiSinr ?: ""}</signal:nrCsiSinr>
                    <signal:nrSsRsrp>${nrSsRsrp ?: ""}</signal:nrSsRsrp>
                    <signal:nrSsSinr>${nrSsSinr ?: ""}</signal:nrSsSinr>
                </extensions>
            </trkpt>
        """.trimIndent()
    }
}
