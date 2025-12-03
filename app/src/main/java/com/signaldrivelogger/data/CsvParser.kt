package com.signaldrivelogger.data

import com.signaldrivelogger.domain.models.SignalRecord
import java.io.InputStream
import java.time.Instant

/**
 * Result of CSV parsing operation.
 */
sealed class CsvParseResult {
    data class Success(val records: List<SignalRecord>) : CsvParseResult()
    data class Error(val message: String, val lineNumber: Int? = null) : CsvParseResult()
}

/**
 * Parser for CSV files containing SignalRecord data.
 */
class CsvParser {
    companion object {
        // Expected CSV header
        private val EXPECTED_HEADER = listOf(
            "timestamp", "latitude", "longitude", "altitude_m", "signal_strength_dbm",
            "cell_id", "data_rate_kbps", "network_type", "asu",
            "data_state", "data_activity", "roaming", "sim_state", "sim_operator_name",
            "sim_mcc", "sim_mnc", "operator_name", "mcc", "mnc", "phone_type",
            "sim_slot_index", "subscription_id", "sim_display_name", "is_embedded"
        )

        /**
         * Parses a CSV file from an InputStream.
         */
        fun parse(inputStream: InputStream): CsvParseResult {
            return try {
                val lines = inputStream.bufferedReader().readLines()
                if (lines.isEmpty()) {
                    return CsvParseResult.Error("CSV file is empty")
                }

                // Validate header
                val header = lines[0].split(",").map { it.trim() }
                if (header.size != EXPECTED_HEADER.size) {
                    return CsvParseResult.Error(
                        "Invalid CSV format: Expected ${EXPECTED_HEADER.size} columns, found ${header.size}",
                        lineNumber = 1
                    )
                }

                // Check if header matches (case-insensitive, allow some flexibility)
                val headerLower = header.map { it.lowercase() }
                val expectedLower = EXPECTED_HEADER.map { it.lowercase() }
                if (headerLower != expectedLower) {
                    // Try to match column by column for better error message
                    val mismatches = headerLower.zip(expectedLower)
                        .mapIndexedNotNull { index, (actual, expected) ->
                            if (actual != expected) index else null
                        }
                    if (mismatches.isNotEmpty()) {
                        return CsvParseResult.Error(
                            "Invalid CSV header: Columns don't match expected format at positions ${mismatches.joinToString()}. " +
                                    "Expected: ${EXPECTED_HEADER.joinToString()}, " +
                                    "Found: ${header.joinToString()}",
                            lineNumber = 1
                        )
                    }
                }

                // Parse data rows
                val records = mutableListOf<SignalRecord>()
                for (i in 1 until lines.size) {
                    val line = lines[i].trim()
                    if (line.isEmpty()) continue // Skip empty lines

                    val parseResult = parseRow(line, i + 1)
                    when (parseResult) {
                        is CsvParseResult.Success -> {
                            records.add(parseResult.records.first())
                        }
                        is CsvParseResult.Error -> {
                            return parseResult // Return first error encountered
                        }
                    }
                }

                if (records.isEmpty()) {
                    CsvParseResult.Error("No valid data rows found in CSV file")
                } else {
                    CsvParseResult.Success(records)
                }
            } catch (e: Exception) {
                CsvParseResult.Error("Error reading CSV file: ${e.message}")
            }
        }

        /**
         * Parses a single CSV row.
         */
        private fun parseRow(line: String, lineNumber: Int): CsvParseResult {
            try {
                // Handle quoted fields and commas within quotes
                val fields = parseCsvLine(line)

                if (fields.size != EXPECTED_HEADER.size) {
                    return CsvParseResult.Error(
                        "Invalid row: Expected ${EXPECTED_HEADER.size} columns, found ${fields.size}",
                        lineNumber = lineNumber
                    )
                }

                // Parse timestamp (epoch milliseconds)
                val timestamp = try {
                    Instant.ofEpochMilli(fields[0].toLong())
                } catch (e: Exception) {
                    return CsvParseResult.Error(
                        "Invalid timestamp format at line $lineNumber: ${fields[0]}",
                        lineNumber = lineNumber
                    )
                }

                // Parse latitude
                val latitude = try {
                    fields[1].toDouble()
                } catch (e: Exception) {
                    return CsvParseResult.Error(
                        "Invalid latitude at line $lineNumber: ${fields[1]}",
                        lineNumber = lineNumber
                    )
                }

                // Parse longitude
                val longitude = try {
                    fields[2].toDouble()
                } catch (e: Exception) {
                    return CsvParseResult.Error(
                        "Invalid longitude at line $lineNumber: ${fields[2]}",
                        lineNumber = lineNumber
                    )
                }

                // Parse altitude
                val altitude = try {
                    fields[3].toDoubleOrNull() ?: 0.0
                } catch (e: Exception) {
                    return CsvParseResult.Error(
                        "Invalid altitude at line $lineNumber: ${fields[3]}",
                        lineNumber = lineNumber
                    )
                }

                // Parse signal strength
                val signalStrength = try {
                    fields[4].toInt()
                } catch (e: Exception) {
                    return CsvParseResult.Error(
                        "Invalid signal strength at line $lineNumber: ${fields[4]}",
                        lineNumber = lineNumber
                    )
                }

                // Parse cell ID
                val cellId = try {
                    fields[5].toInt()
                } catch (e: Exception) {
                    return CsvParseResult.Error(
                        "Invalid cell ID at line $lineNumber: ${fields[5]}",
                        lineNumber = lineNumber
                    )
                }

                // Parse data rate
                val dataRateKbps = try {
                    fields[6].toInt()
                } catch (e: Exception) {
                    return CsvParseResult.Error(
                        "Invalid data rate at line $lineNumber: ${fields[6]}",
                        lineNumber = lineNumber
                    )
                }

                // Parse network type (string)
                val networkType = fields[7]

                // Parse ASU
                val asu = try {
                    fields[8].toIntOrNull() ?: 0
                } catch (e: Exception) {
                    0
                }

                // Parse remaining fields
                val dataState = fields[9].ifEmpty { "Unknown" }
                val dataActivity = fields[10].ifEmpty { "None" }
                val isRoaming = try {
                    fields[11].toBoolean()
                } catch (e: Exception) {
                    fields[11].lowercase() == "true"
                }
                val simState = fields[12].ifEmpty { "Unknown" }
                val simOperatorName = fields[13].removeSurrounding("\"")
                val simMcc = fields[14]
                val simMnc = fields[15]
                val operatorName = fields[16].removeSurrounding("\"")
                val mcc = fields[17]
                val mnc = fields[18]
                val phoneType = fields[19].ifEmpty { "Unknown" }
                val simSlotIndex = try {
                    fields[20].toIntOrNull() ?: 0
                } catch (e: Exception) {
                    0
                }
                val subscriptionId = try {
                    fields[21].toIntOrNull() ?: -1
                } catch (e: Exception) {
                    -1
                }
                val simDisplayName = fields[22].removeSurrounding("\"")
                val isEmbedded = try {
                    fields[23].toBoolean()
                } catch (e: Exception) {
                    fields[23].lowercase() == "true"
                }

                val record = SignalRecord(
                    timestamp = timestamp,
                    latitude = latitude,
                    longitude = longitude,
                    altitude = altitude,
                    signalStrength = signalStrength,
                    cellId = cellId,
                    dataRateKbps = dataRateKbps,
                    networkType = networkType,
                    asu = asu,
                    dataState = dataState,
                    dataActivity = dataActivity,
                    isRoaming = isRoaming,
                    simState = simState,
                    simOperatorName = simOperatorName,
                    simMcc = simMcc,
                    simMnc = simMnc,
                    operatorName = operatorName,
                    mcc = mcc,
                    mnc = mnc,
                    phoneType = phoneType,
                    simSlotIndex = simSlotIndex,
                    subscriptionId = subscriptionId,
                    simDisplayName = simDisplayName,
                    isEmbedded = isEmbedded
                )

                return CsvParseResult.Success(listOf(record))
            } catch (e: Exception) {
                return CsvParseResult.Error(
                    "Error parsing line $lineNumber: ${e.message}",
                    lineNumber = lineNumber
                )
            }
        }

        /**
         * Parses a CSV line handling quoted fields.
         */
        private fun parseCsvLine(line: String): List<String> {
            val fields = mutableListOf<String>()
            var currentField = StringBuilder()
            var insideQuotes = false
            var i = 0

            while (i < line.length) {
                val char = line[i]
                when {
                    char == '"' -> {
                        if (insideQuotes && i + 1 < line.length && line[i + 1] == '"') {
                            // Escaped quote
                            currentField.append('"')
                            i += 2
                        } else {
                            // Toggle quote state
                            insideQuotes = !insideQuotes
                            i++
                        }
                    }
                    char == ',' && !insideQuotes -> {
                        // Field separator
                        fields.add(currentField.toString().trim())
                        currentField.clear()
                        i++
                    }
                    else -> {
                        currentField.append(char)
                        i++
                    }
                }
            }
            // Add last field
            fields.add(currentField.toString().trim())

            return fields
        }
    }
}
