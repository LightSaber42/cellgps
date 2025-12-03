package com.signaldrivelogger.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.signaldrivelogger.ui.theme.SignalDriveLoggerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LoggingViewModel by viewModels()

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleFileImport(it) }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle shared file intent
        handleIntent(intent)

        setContent {
            SignalDriveLoggerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val permissions = rememberMultiplePermissionsState(
                        permissions = listOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE
                        ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            listOf(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            emptyList()
                        }
                    )

                    LaunchedEffect(Unit) {
                        if (!permissions.allPermissionsGranted) {
                            permissions.launchMultiplePermissionRequest()
                        }
                    }

                    if (permissions.allPermissionsGranted) {
                        AppNavigation(viewModel = viewModel, mainActivity = this@MainActivity)
                    } else {
                        PermissionRequestScreen(
                            onRequestPermissions = { permissions.launchMultiplePermissionRequest() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                // Try EXTRA_STREAM first (for file attachments)
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
                uri?.let { handleFileImport(it) }

                // Also try EXTRA_TEXT for text-based sharing
                if (uri == null) {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    text?.let {
                        // If it's a file path or URI, try to handle it
                        if (it.startsWith("file://") || it.startsWith("content://")) {
                            try {
                                handleFileImport(android.net.Uri.parse(it))
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                intent.data?.let { handleFileImport(it) }
            }
        }
    }

    private fun handleFileImport(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.let {
                CoroutineScope(Dispatchers.Main).launch {
                    viewModel.importCsvFile(it)
                }
            }
        } catch (e: Exception) {
            // Error will be handled by ViewModel
        }
    }

    fun launchFilePicker() {
        filePickerLauncher.launch("text/csv")
    }
}

@Composable
fun AppNavigation(viewModel: LoggingViewModel, mainActivity: MainActivity) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                onNavigateToMap = { navController.navigate("map") },
                onImportFile = { mainActivity.launchFilePicker() }
            )
        }
        composable("map") {
            MapScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestScreen(onRequestPermissions: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("Permissions Required") }
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "This app needs location and phone state permissions to log signal data.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermissions) {
            Text("Grant Permissions")
        }
    }
}
