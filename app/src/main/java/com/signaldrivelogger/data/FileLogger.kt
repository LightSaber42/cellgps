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
 */
class FileLogger(private val context: Context) {
    private val recordsDir: File = File(context.getExternalFilesDir(null), "signal_logs")

    init {
        recordsDir.mkdirs()
    }

    /**
     * Logs a record to CSV file (append mode).
     */
    suspend fun logToCsv(record: SignalRecord, filename: String) = withContext(Dispatchers.IO) {
        val file = File(recordsDir, "$filename.csv")
        val header = "timestamp,latitude,longitude,altitude_m,signal_strength_dbm,cell_id,data_rate_kbps,network_type,asu," +
                "data_state,data_activity,roaming,sim_state,sim_operator_name,sim_mcc,sim_mnc," +
                "operator_name,mcc,mnc,phone_type,sim_slot_index,subscription_id,sim_display_name,is_embedded," +
                "ci,enb,tac,pci,bandwidth_khz,earfcn,nrarfcn,rssi_dbm,rsrq_db,snr_db,cqi,timing_advance\n"

        if (!file.exists()) {
            file.writeText(header)
        }

        file.appendText("${record.toCsvRow()}\n")
    }

    /**
     * Logs a record to GPX file (append mode).
     */
    suspend fun logToGpx(record: SignalRecord, filename: String) = withContext(Dispatchers.IO) {
        val file = File(recordsDir, "$filename.gpx")

        if (!file.exists()) {
            // Write GPX header
            file.writeText("""<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="Cell Signal Logger">
    <trk>
        <name>Signal Drive Log</name>
        <trkseg>
""")
        }

        // Append trackpoint
        val trackpoint = record.toGpxTrackpoint()
        file.appendText("            $trackpoint\n")
    }

    /**
     * Finalizes GPX file by closing the XML tags.
     */
    suspend fun finalizeGpx(filename: String) = withContext(Dispatchers.IO) {
        val file = File(recordsDir, "$filename.gpx")
        if (file.exists()) {
            file.appendText("""        </trkseg>
    </trk>
</gpx>
""")
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
