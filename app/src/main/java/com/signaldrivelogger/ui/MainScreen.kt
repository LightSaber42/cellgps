package com.signaldrivelogger.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.signaldrivelogger.data.SignalData

/**
 * Main screen with controls and signal information display.
 */
@Composable
fun MainScreen(
    viewModel: LoggingViewModel,
    onNavigateToMap: () -> Unit,
    onImportFile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLogging by viewModel.isLogging.collectAsState()
    val records by viewModel.records.collectAsState()
    val currentSignalDataBySim by viewModel.currentSignalDataBySim.collectAsState()
    val filename by viewModel.filename.collectAsState()

    var filenameInput by remember { mutableStateOf(filename) }
    val importError by viewModel.importError.collectAsState()
    val importSuccess by viewModel.importSuccess.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle import error
    LaunchedEffect(importError) {
        importError?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar("Import Error: $error")
            }
            viewModel.clearImportError()
        }
    }

    // Handle import success
    LaunchedEffect(importSuccess) {
        importSuccess?.let { success ->
            scope.launch {
                snackbarHostState.showSnackbar(success)
            }
            viewModel.clearImportSuccess()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Cell Signal Logger",
                style = MaterialTheme.typography.headlineMedium
            )

            // Current Signal Info Card
            SignalInfoCard(signalDataBySim = currentSignalDataBySim)

            // Filename Input
            OutlinedTextField(
                value = filenameInput,
                onValueChange = { filenameInput = it },
                label = { Text("Log Filename") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLogging,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.setFilename(filenameInput)
                        if (isLogging) {
                            viewModel.stopLogging()
                        } else {
                            viewModel.startLogging()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isLogging) "Stop Logging" else "Start Logging")
                }

                Button(
                    onClick = onNavigateToMap,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Map")
                }
            }

            // Import Button
            Button(
                onClick = { onImportFile() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLogging
            ) {
                Text("Import CSV File")
            }

            // File Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.setFilename(filenameInput)
                        viewModel.saveFile("csv")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = records.isNotEmpty() && !isLogging
                ) {
                    Text("Save CSV")
                }

                Button(
                    onClick = {
                        viewModel.setFilename(filenameInput)
                        viewModel.saveFile("gpx")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = records.isNotEmpty() && !isLogging
                ) {
                    Text("Save GPX")
                }
            }

            // Export/Share Button
            Button(
                onClick = {
                    val file = viewModel.exportFile()
                    file?.let {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            it
                        )
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, uri)
                            type = "text/csv"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share log file"))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = records.isNotEmpty() && !isLogging
            ) {
                Text("Export/Share File")
            }

            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Records: ${records.size}")
                    if (records.isNotEmpty()) {
                        val avgSignal = records.map { it.signalStrength }.average().toInt()
                        Text("Avg Signal: $avgSignal dBm")
                        val minSignal = records.minOfOrNull { it.signalStrength } ?: 0
                        val maxSignal = records.maxOfOrNull { it.signalStrength } ?: 0
                        Text("Signal Range: $minSignal to $maxSignal dBm")
                    }
                }
            }

            // Clear Button
            TextButton(
                onClick = { viewModel.clearRecords() },
                modifier = Modifier.fillMaxWidth(),
                enabled = records.isNotEmpty() && !isLogging
            ) {
                Text("Clear Records")
            }
        }

    }
}

/**
 * Card displaying current signal information for all SIMs.
 */
@Composable
private fun SignalInfoCard(signalDataBySim: Map<Int, SignalData>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Current Signal",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (signalDataBySim.isNotEmpty()) {
                // Concatenate values from all SIMs
                val strengths = signalDataBySim.values.map { it.signalStrength }.joinToString("/")
                val networks = signalDataBySim.values.map { it.networkType }.joinToString("/")
                val cellIds = signalDataBySim.values.map { it.cellId }.joinToString("/")

                Text("Strength: $strengths dBm")
                Text("Network: $networks")
                Text("Cell ID: $cellIds")
            } else {
                Text("No signal data available")
            }
        }
    }
}
