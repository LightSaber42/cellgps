package com.signaldrivelogger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import com.signaldrivelogger.domain.models.SignalRecord

/**
 * Map screen showing the route colored by signal strength using osmdroid.
 */
@Composable
fun MapScreen(
    viewModel: LoggingViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.records.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val availableSims by viewModel.availableSims.collectAsState()
    val selectedSimIds by viewModel.selectedSimIds.collectAsState()
    val context = LocalContext.current

    // Load SIMs on first composition
    LaunchedEffect(Unit) {
        viewModel.loadAvailableSims()
    }

    // Get filtered records based on selected SIMs
    val filteredRecords = remember(records, selectedSimIds) {
        if (selectedSimIds.isEmpty()) {
            records // Show all if none selected
        } else {
            records.filter { it.subscriptionId in selectedSimIds }
        }
    }

    // Store map controller and location overlay references for centering
    val mapControllerRef = remember { mutableStateOf<IMapController?>(null) }
    val locationOverlayRef = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 3.0
                    maxZoomLevel = 21.0

                    // Set initial position
                    val initialRecords = if (selectedSimIds.isEmpty()) {
                        records
                    } else {
                        records.filter { it.subscriptionId in selectedSimIds }
                    }
                    val initialPoint = if (initialRecords.isNotEmpty()) {
                        GeoPoint(initialRecords.first().latitude, initialRecords.first().longitude)
                    } else if (currentLocation != null) {
                        GeoPoint(currentLocation!!.latitude, currentLocation!!.longitude)
                    } else {
                        GeoPoint(0.0, 0.0) // Default
                    }

                    val controller = controller
                    mapControllerRef.value = controller
                    controller.setZoom(15.0)
                    controller.setCenter(initialPoint)

                    // Add my location overlay
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    locationOverlayRef.value = locationOverlay
                    overlays.add(locationOverlay)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                // Store controller reference
                mapControllerRef.value = mapView.controller

                // Update map when filtered records change
                updateMapWithRecords(mapView, filteredRecords)
            }
        )

        // Center on location button
        IconButton(
            onClick = {
                val controller = mapControllerRef.value
                if (controller != null) {
                    // Try to get location from ViewModel first
                    val location = currentLocation
                    if (location != null) {
                        val point = GeoPoint(location.latitude, location.longitude)
                        controller.animateTo(point)
                    } else {
                        // Fallback to location overlay's current location
                        val overlay = locationOverlayRef.value
                        overlay?.let {
                            val lastFix = it.lastFix
                            if (lastFix != null) {
                                val point = GeoPoint(lastFix.latitude, lastFix.longitude)
                                controller.animateTo(point)
                            } else {
                                // If no location available, center on first record or show message
                                if (filteredRecords.isNotEmpty()) {
                                    val firstRecord = filteredRecords.first()
                                    val point = GeoPoint(firstRecord.latitude, firstRecord.longitude)
                                    controller.animateTo(point)
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Center on current location",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        // SIM selector buttons
        if (availableSims.isNotEmpty()) {
            SimSelectorButtons(
                sims = availableSims,
                selectedSimIds = selectedSimIds,
                onSimToggled = { subId -> viewModel.toggleSimSelection(subId) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }

        // Color scale legend
        ColorScaleLegend(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

/**
 * Updates the map with signal strength colored polylines.
 */
private fun updateMapWithRecords(mapView: MapView, records: List<SignalRecord>) {
    // Remove existing polylines (except my location overlay)
    val overlaysToRemove = mapView.overlays.filter { it !is MyLocationNewOverlay }
    overlaysToRemove.forEach { mapView.overlays.remove(it) }

    if (records.size < 2) return

    // Create polylines colored by signal strength
    for (i in 0 until records.size - 1) {
        val record1 = records[i]
        val record2 = records[i + 1]

        val polyline = Polyline().apply {
            addPoint(GeoPoint(record1.latitude, record1.longitude))
            addPoint(GeoPoint(record2.latitude, record2.longitude))

            val color = getSignalColorAndroid(record1.signalStrength)
            // Use outlinePaint for osmdroid 6.1.17+ (paint is deprecated)
            outlinePaint.color = color
            outlinePaint.strokeWidth = 12f
        }

        mapView.overlays.add(polyline)
    }

    mapView.invalidate()
}

/**
 * Gets Android Color based on signal strength (green = strong, red = weak).
 */
private fun getSignalColorAndroid(signalStrength: Int): Int {
    // RSRP range: -140 dBm (weak) to -50 dBm (strong)
    val normalized = ((signalStrength + 140) / 90f).coerceIn(0f, 1f)

    return when {
        normalized > 0.75f -> android.graphics.Color.rgb(76, 175, 80)   // Green (strong)
        normalized > 0.5f -> android.graphics.Color.rgb(139, 195, 74)   // Light green
        normalized > 0.25f -> android.graphics.Color.rgb(255, 193, 7)   // Yellow
        else -> android.graphics.Color.rgb(244, 67, 54)                 // Red (weak)
    }
}

/**
 * Gets Compose Color based on signal strength (for legend).
 */
private fun getSignalColor(signalStrength: Int): androidx.compose.ui.graphics.Color {
    // RSRP range: -140 dBm (weak) to -50 dBm (strong)
    val normalized = ((signalStrength + 140) / 90f).coerceIn(0f, 1f)

    return when {
        normalized > 0.75f -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green (strong)
        normalized > 0.5f -> androidx.compose.ui.graphics.Color(0xFF8BC34A) // Light green
        normalized > 0.25f -> androidx.compose.ui.graphics.Color(0xFFFFC107) // Yellow
        else -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red (weak)
    }
}

/**
 * SIM selector buttons for filtering map display.
 */
@Composable
private fun SimSelectorButtons(
    sims: List<SimInfo>,
    selectedSimIds: Set<Int>,
    onSimToggled: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sims.forEachIndexed { index, sim ->
                val isSelected = sim.subscriptionId in selectedSimIds
                IconButton(
                    onClick = { onSimToggled(sim.subscriptionId) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${sim.slotIndex + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Color scale legend showing signal strength gradient.
 */
@Composable
private fun ColorScaleLegend(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Signal Strength",
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.padding(vertical = 4.dp))

            // Gradient bar
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green
                                androidx.compose.ui.graphics.Color(0xFF8BC34A), // Light green
                                androidx.compose.ui.graphics.Color(0xFFFFC107), // Yellow
                                androidx.compose.ui.graphics.Color(0xFFF44336)  // Red
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "-50 dBm",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                )
                Text(
                    text = "-140 dBm",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                )
            }
        }
    }
}
