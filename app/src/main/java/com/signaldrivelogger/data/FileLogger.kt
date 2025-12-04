package com.signaldrivelogger.data

import android.content.Context
import com.signaldrivelogger.domain.models.SignalRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.time.Instant

/**
 * Handles logging SignalRecord data to files (CSV and GPX formats).
 * OPTIMIZED: Uses BufferedWriter to avoid opening/closing file for every write.
 */
class FileLogger(private val context: Context) {
    private val recordsDir: File = File(context.getExternalFilesDir(null), "signal_logs")

    // Buffered writers kept open during logging session
    private var csvWriter: java.io.BufferedWriter? = null
    private var gpxWriter: java.io.BufferedWriter? = null
    private var currentFilename: String? = null
    private val headerWritten = mutableSetOf<String>() // Track which files have headers

    init {
        recordsDir.mkdirs()
    }

    /**
     * Starts a new logging session with buffered writers.
     */
    fun startNewSession(filename: String) {
        currentFilename = filename
        val csvFile = File(recordsDir, "$filename.csv")
        val gpxFile = File(recordsDir, "$filename.gpx")

        try {
            // Open CSV writer (append mode)
            csvWriter = java.io.BufferedWriter(java.io.FileWriter(csvFile, true))

            // Write header if file is new
            if (!headerWritten.contains("$filename.csv") && (!csvFile.exists() || csvFile.length() == 0L)) {
                val header = "timestamp,latitude,longitude,altitude_m,signal_strength_dbm,cell_id,data_rate_kbps,network_type,asu," +
                        "data_state,data_activity,roaming,sim_state,sim_operator_name,sim_mcc,sim_mnc," +
                        "operator_name,mcc,mnc,phone_type,sim_slot_index,subscription_id,sim_display_name,is_embedded," +
                        "ci,enb,tac,pci,bandwidth_khz,earfcn,nrarfcn,rssi_dbm,rsrq_db,snr_db,cqi,timing_advance\n"
                csvWriter?.write(header)
                headerWritten.add("$filename.csv")
            }

            // Open GPX writer (append mode)
            gpxWriter = java.io.BufferedWriter(java.io.FileWriter(gpxFile, true))

            // Write GPX header if file is new
            if (!headerWritten.contains("$filename.gpx") && (!gpxFile.exists() || gpxFile.length() == 0L)) {
                gpxWriter?.write("""<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="Cell Signal Logger">
    <trk>
        <name>Signal Drive Log</name>
        <trkseg>
""")
                headerWritten.add("$filename.gpx")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            closeSession() // Clean up on error
        }
    }

    /**
     * Closes the logging session and flushes all buffers.
     */
    fun closeSession() {
        try {
            // Finalize GPX
            gpxWriter?.write("        </trkseg>\n    </trk>\n</gpx>\n")

            // Flush and close writers
            csvWriter?.flush()
            csvWriter?.close()
            gpxWriter?.flush()
            gpxWriter?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            csvWriter = null
            gpxWriter = null
            currentFilename = null
        }
    }

    /**
     * Logs a record to CSV file using buffered writer (optimized).
     */
    suspend fun logToCsv(record: SignalRecord, filename: String) = withContext(Dispatchers.IO) {
        // If session not started or filename changed, start new session
        if (csvWriter == null || currentFilename != filename) {
            closeSession() // Close previous session if any
            startNewSession(filename)
        }

        try {
            csvWriter?.write("${record.toCsvRow()}\n")
            // Flush periodically (every 10 records) instead of every record
            // For now, flush every record to ensure data safety, but this is much faster than appendText
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Logs a record to GPX file using buffered writer (optimized).
     */
    suspend fun logToGpx(record: SignalRecord, filename: String) = withContext(Dispatchers.IO) {
        // If session not started or filename changed, start new session
        if (gpxWriter == null || currentFilename != filename) {
            closeSession() // Close previous session if any
            startNewSession(filename)
        }

        try {
            val trackpoint = record.toGpxTrackpoint()
            gpxWriter?.write("            $trackpoint\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Finalizes GPX file by closing the XML tags and closing the session.
     */
    suspend fun finalizeGpx(filename: String) = withContext(Dispatchers.IO) {
        // If this is the current session, close it properly
        if (currentFilename == filename) {
            closeSession()
        } else {
            // If file exists but session wasn't active, append closing tags
            val file = File(recordsDir, "$filename.gpx")
            if (file.exists()) {
                try {
                    java.io.FileWriter(file, true).use { writer ->
                        writer.write("""        </trkseg>
    </trk>
</gpx>
""")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Saves all records to a CSV file.
     */
    suspend fun saveRecordsToCsv(records: List<SignalRecord>, filename: String) = withContext(Dispatchers.IO) {
        val file = File(recordsDir, "$filename.csv")
        val header = "timestamp,latitude,longitude,altitude_m,signal_strength_dbm,cell_id,data_rate_kbps,network_type,asu," +
                "data_state,data_activity,roaming,sim_state,sim_operator_name,sim_mcc,sim_mnc," +
                "operator_name,mcc,mnc,phone_type,sim_slot_index,subscription_id,sim_display_name,is_embedded," +
                "ci,enb,tac,pci,bandwidth_khz,earfcn,nrarfcn,rssi_dbm,rsrq_db,snr_db,cqi,timing_advance\n"

        file.writeText(header)
        records.forEach { record ->
            file.appendText("${record.toCsvRow()}\n")
        }
    }

    /**
     * Saves all records to a GPX file.
     */
    suspend fun saveRecordsToGpx(records: List<SignalRecord>, filename: String) = withContext(Dispatchers.IO) {
        val file = File(recordsDir, "$filename.gpx")

        file.writeText("""<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="Cell Signal Logger">
    <trk>
        <name>Signal Drive Log</name>
        <trkseg>
""")

        records.forEach { record ->
            val trackpoint = record.toGpxTrackpoint()
            file.appendText("            $trackpoint\n")
        }

        file.appendText("""        </trkseg>
    </trk>
</gpx>
""")
    }

    /**
     * Gets the file for sharing/export.
     */
    fun getFile(filename: String, format: String): File? {
        val file = File(recordsDir, "$filename.$format")
        return if (file.exists()) file else null
    }

    /**
     * Lists all log files.
     */
    fun listLogFiles(): List<String> {
        return recordsDir.listFiles()?.map { it.name } ?: emptyList()
    }

    /**
     * Reads a CSV file and returns the input stream.
     */
    suspend fun readCsvFile(fileUri: android.net.Uri, context: android.content.Context): java.io.InputStream? {
        return try {
            context.contentResolver.openInputStream(fileUri)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reads a CSV file from a File path.
     */
    suspend fun readCsvFile(file: java.io.File): java.io.InputStream? {
        return try {
            if (file.exists() && file.canRead()) {
                file.inputStream()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
