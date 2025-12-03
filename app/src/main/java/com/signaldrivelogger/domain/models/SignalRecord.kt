package com.signaldrivelogger.domain.models

import java.time.Instant

/**
 * Represents a single signal measurement record with location and telephony data.
 */
data class SignalRecord(
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0, // GPS altitude in meters
    val signalStrength: Int, // RSRP in dBm (negative value, e.g., -85)
    val cellId: Int,
    val dataRateKbps: Int, // Estimated data rate in Kbps
    val networkType: String, // "4G", "5G", "LTE", etc.
    val asu: Int = 0, // Additional Signal Unit (optional)
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
    val isEmbedded: Boolean = false // true for eSIM, false for physical SIM
) {
    /**
     * Converts signal strength to a normalized value (0.0 to 1.0) for color mapping.
     * Typical RSRP range: -140 dBm (weak) to -50 dBm (strong)
     */
    fun getNormalizedStrength(): Float {
        val minStrength = -140f
        val maxStrength = -50f
        val normalized = (signalStrength - minStrength) / (maxStrength - minStrength)
        return normalized.coerceIn(0f, 1f)
    }

    /**
     * Converts to CSV format
     */
    fun toCsvRow(): String {
        return "${timestamp.toEpochMilli()},$latitude,$longitude,$altitude,$signalStrength,$cellId,$dataRateKbps,$networkType,$asu," +
                "$dataState,$dataActivity,$isRoaming,$simState,\"$simOperatorName\",$simMcc,$simMnc,\"$operatorName\",$mcc,$mnc,$phoneType," +
                "$simSlotIndex,$subscriptionId,\"$simDisplayName\",$isEmbedded"
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
                    <signal:dataRate>$dataRateKbps</signal:dataRate>
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
                </extensions>
            </trkpt>
        """.trimIndent()
    }
}
