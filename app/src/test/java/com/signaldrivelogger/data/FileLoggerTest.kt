package com.signaldrivelogger.data

import com.signaldrivelogger.domain.models.SignalRecord
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.time.Instant

/**
 * Unit tests for FileLogger.
 * Note: These are simplified tests. In a real scenario, you'd use a test context.
 */
class FileLoggerTest {

    @Test
    fun testSignalRecordToCsv() {
        val record = SignalRecord(
            timestamp = Instant.parse("2023-11-04T12:34:56Z"),
            latitude = 37.7749,
            longitude = -122.4194,
            signalStrength = -85,
            cellId = 12345,
            dataRateKbps = 20000,
            networkType = "LTE"
        )

        val csvRow = record.toCsvRow()
        assertTrue(csvRow.contains("-85"))
        assertTrue(csvRow.contains("37.7749"))
        assertTrue(csvRow.contains("-122.4194"))
        assertTrue(csvRow.contains("LTE"))
    }

    @Test
    fun testSignalRecordToGpx() {
        val record = SignalRecord(
            timestamp = Instant.parse("2023-11-04T12:34:56Z"),
            latitude = 37.7749,
            longitude = -122.4194,
            signalStrength = -85,
            cellId = 12345,
            dataRateKbps = 20000,
            networkType = "LTE"
        )

        val gpxTrackpoint = record.toGpxTrackpoint()
        assertTrue(gpxTrackpoint.contains("37.7749"))
        assertTrue(gpxTrackpoint.contains("-122.4194"))
        assertTrue(gpxTrackpoint.contains("-85"))
        assertTrue(gpxTrackpoint.contains("LTE"))
    }

    @Test
    fun testNormalizedStrength() {
        val strongSignal = SignalRecord(
            timestamp = Instant.now(),
            latitude = 0.0,
            longitude = 0.0,
            signalStrength = -50, // Strong signal
            cellId = 0,
            dataRateKbps = 0,
            networkType = "LTE"
        )

        val weakSignal = SignalRecord(
            timestamp = Instant.now(),
            latitude = 0.0,
            longitude = 0.0,
            signalStrength = -140, // Weak signal
            cellId = 0,
            dataRateKbps = 0,
            networkType = "LTE"
        )

        val strongNormalized = strongSignal.getNormalizedStrength()
        val weakNormalized = weakSignal.getNormalizedStrength()

        assertTrue(strongNormalized > weakNormalized)
        assertTrue(strongNormalized >= 0f && strongNormalized <= 1f)
        assertTrue(weakNormalized >= 0f && weakNormalized <= 1f)
    }
}
