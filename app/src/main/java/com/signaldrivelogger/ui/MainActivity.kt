package com.signaldrivelogger.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.signaldrivelogger.ui.theme.SignalDriveLoggerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LoggingViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                        AppNavigation(viewModel = viewModel)
                    } else {
                        PermissionRequestScreen(
                            onRequestPermissions = { permissions.launchMultiplePermissionRequest() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: LoggingViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                onNavigateToMap = { navController.navigate("map") }
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
