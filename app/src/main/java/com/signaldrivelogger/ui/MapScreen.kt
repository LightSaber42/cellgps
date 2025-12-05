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
import kotlinx.coroutines.delay
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
    // Fix: Include records with invalid subscriptionId (-1) when filtering, as they may be from older logs
    // or single-SIM devices. Only filter out records that have a valid subscriptionId that doesn't match.
    val filteredRecords = remember(records, selectedSimIds) {
        if (selectedSimIds.isEmpty()) {
            records // Show all if none selected
        } else {
            records.filter { record ->
                // Include if subscriptionId is invalid (-1) or matches selected SIMs
                record.subscriptionId < 0 || record.subscriptionId in selectedSimIds
            }
        }
    }

    // Store map controller, map view, and location overlay references
    val mapControllerRef = remember { mutableStateOf<IMapController?>(null) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val locationOverlayRef = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Track autocentering state - enable by default when logging
    val isLogging by viewModel.isLogging.collectAsState()
    var isAutoCenteringEnabled by remember { mutableStateOf(true) }

    // Reset autocentering to enabled when logging starts
    LaunchedEffect(isLogging) {
        if (isLogging) {
            isAutoCenteringEnabled = true
        }
    }

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }


    // Autocentering: Periodically check location and center map when enabled
    LaunchedEffect(isAutoCenteringEnabled, isLogging, locationOverlayRef.value, mapControllerRef.value) {
        if (!isAutoCenteringEnabled || !isLogging) return@LaunchedEffect

        val overlay = locationOverlayRef.value
        val controller = mapControllerRef.value

        if (overlay == null || controller == null) return@LaunchedEffect

        var lastLocation: android.location.Location? = null

        // Check for location updates every 1 second
        while (isAutoCenteringEnabled && isLogging) {
            val currentOverlay = locationOverlayRef.value
            val currentController = mapControllerRef.value

            if (currentOverlay == null || currentController == null) break

            val overlayLocation = currentOverlay.lastFix
            if (overlayLocation != null) {
                // Only center if location has changed significantly (more than 10 meters)
                val shouldCenter = lastLocation == null ||
                    overlayLocation.distanceTo(lastLocation) > 10.0f

                if (shouldCenter) {
                    val point = GeoPoint(overlayLocation.latitude, overlayLocation.longitude)
                    currentController.animateTo(point)
                    lastLocation = overlayLocation
                }
            }
            delay(1000) // Check every second
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 3.0
                    maxZoomLevel = 21.0

                    // Store references
                    mapViewRef.value = this

                    // Set initial position
                    val initialRecords = if (selectedSimIds.isEmpty()) {
                        records
                    } else {
                        records.filter { record ->
                            // Include if subscriptionId is invalid (-1) or matches selected SIMs
                            record.subscriptionId < 0 || record.subscriptionId in selectedSimIds
                        }
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
                // Store references
                mapViewRef.value = mapView
                mapControllerRef.value = mapView.controller

                // Update map when filtered records change
                updateMapWithRecords(mapView, filteredRecords)
            }
        )

        // Center on location button - toggles autocentering and centers once
        IconButton(
            onClick = {
                val controller = mapControllerRef.value
                if (controller != null) {
                    // Toggle autocentering
                    isAutoCenteringEnabled = !isAutoCenteringEnabled

                    // Center on current location
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
                                // If no location available, center on first record
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
                .padding(top = 8.dp, end = 8.dp)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = if (isAutoCenteringEnabled) "Disable autocentering" else "Enable autocentering",
                tint = if (isAutoCenteringEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
 * OPTIMIZED: Batches consecutive segments of the same color into single Polyline objects
 * to reduce overlay count and improve rendering performance on long drives.
 */
private fun updateMapWithRecords(mapView: MapView, records: List<SignalRecord>) {
    // Remove existing polylines (except my location overlay)
    mapView.overlays.removeAll { it is Polyline }

    if (records.size < 2) return

    val polylines = mutableListOf<Polyline>()

    // Batch consecutive segments of the same color into single Polyline objects
    // This significantly reduces the number of overlay objects (from N-1 to ~N/10 for typical drives)
    var currentPolyline: Polyline? = null
    var lastColor: Int? = null

    for (i in 0 until records.size - 1) {
        val record1 = records[i]
        val record2 = records[i + 1]

        val color = getSignalColorAndroid(record1.signalStrength)

        if (currentPolyline != null && color == lastColor) {
            // Extend current line (same color segment)
            currentPolyline.addPoint(GeoPoint(record2.latitude, record2.longitude))
        } else {
            // Finish previous polyline
            currentPolyline?.let { polylines.add(it) }

            // Start new polyline for this color segment
            currentPolyline = Polyline().apply {
                outlinePaint.color = color
                outlinePaint.strokeWidth = 12f
                // Add start and end points of this segment
                addPoint(GeoPoint(record1.latitude, record1.longitude))
                addPoint(GeoPoint(record2.latitude, record2.longitude))
            }
            lastColor = color
        }
    }

    // Add the last polyline if it exists
    currentPolyline?.let { polylines.add(it) }

    mapView.overlays.addAll(polylines)
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
            sims.forEach { sim ->
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
