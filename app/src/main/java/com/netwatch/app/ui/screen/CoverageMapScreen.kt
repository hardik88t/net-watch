package com.netwatch.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.netwatch.app.core.model.NetworkEventType
import com.netwatch.app.core.model.TimelineItem
import com.netwatch.app.ui.theme.NetWatchAccent
import com.netwatch.app.ui.theme.NetWatchBackground
import com.netwatch.app.ui.theme.NetWatchDanger
import com.netwatch.app.ui.theme.NetWatchPrimaryText
import com.netwatch.app.ui.theme.NetWatchSecondaryText
import com.netwatch.app.ui.theme.NetWatchSurface
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.tileprovider.cachemanager.CacheManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CoverageMapScreen(
    timeline: List<TimelineItem>,
    autoCenter: Boolean,
    offlineMinZoom: Int,
    offlineMaxZoom: Int,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var selectedLayer by rememberSaveable { mutableStateOf(MapLayerFilter.ALL) }
    var showRouteLine by rememberSaveable { mutableStateOf(true) }
    var offlineStatus by remember { mutableStateOf<String?>(null) }
    var hasCentered by remember { mutableStateOf(false) }
    var locationPermissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }

    val points = remember(timeline) {
        timeline
            .asSequence()
            .mapNotNull { item ->
                val lat = item.event.latitude ?: return@mapNotNull null
                val lon = item.event.longitude ?: return@mapNotNull null
                CoveragePoint(
                    latitude = lat,
                    longitude = lon,
                    signalDbm = item.event.signalDbm,
                    type = item.event.type,
                    message = item.event.message,
                    timestampMs = item.event.timestampMs,
                )
            }
            .sortedByDescending { it.timestampMs }
            .take(240)
            .toList()
    }

    val filteredPoints = remember(points, selectedLayer) {
        points.filter { selectedLayer.matches(it.type) }
    }

    val averageSignal = filteredPoints.mapNotNull { it.signalDbm }.average().takeIf { !it.isNaN() } ?: -110.0
    val coverageScore = ((averageSignal + 120.0) / 0.6).toInt().coerceIn(0, 100)
    val outageCount = filteredPoints.count { it.type == NetworkEventType.OUTAGE_START }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    locationPermissionGranted = hasLocationPermission(context)
                    mapView?.onResume()
                }

                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .background(NetWatchBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Coverage Map",
                color = NetWatchPrimaryText,
                fontSize = 26.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
        }

        item {
            Text(
                text = "OpenStreetMap layers with local tile caching for frequently visited areas.",
                color = NetWatchSecondaryText,
                fontSize = 14.sp,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(
                    modifier = Modifier.weight(1f),
                    title = "Coverage Score",
                    value = "$coverageScore/100",
                    icon = Icons.Rounded.MyLocation,
                )
                StatPill(
                    modifier = Modifier.weight(1f),
                    title = "Outage Pins",
                    value = outageCount.toString(),
                    icon = Icons.Rounded.NetworkCell,
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MapLayerFilter.entries) { layer ->
                    val selected = layer == selectedLayer
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) NetWatchAccent.copy(alpha = 0.2f) else NetWatchSurface,
                        ),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.clickable { selectedLayer = layer },
                    ) {
                        Text(
                            text = layer.label,
                            color = if (selected) NetWatchAccent else NetWatchSecondaryText,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Route, contentDescription = null, tint = NetWatchAccent)
                            Text(
                                text = "Route layer",
                                color = NetWatchSecondaryText,
                                modifier = Modifier.padding(start = 6.dp),
                                fontSize = 12.sp,
                            )
                        }
                        Switch(checked = showRouteLine, onCheckedChange = { showRouteLine = it })
                    }

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        factory = { viewContext ->
                            MapView(viewContext).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                                controller.setZoom(4.0)
                                controller.setCenter(GeoPoint(39.8283, -98.5795))
                                mapView = this
                            }
                        },
                        update = { view ->
                            mapView = view
                            renderMapOverlays(view, filteredPoints, showRouteLine)
                            if (autoCenter && filteredPoints.isNotEmpty() && !hasCentered) {
                                centerMapToPoints(view, filteredPoints)
                                hasCentered = true
                            }
                        },
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (mapView == null) {
                            offlineStatus = "Map not ready yet."
                            return@Button
                        }
                        cacheVisibleArea(
                            mapView = requireNotNull(mapView),
                            minZoom = offlineMinZoom,
                            maxZoom = offlineMaxZoom,
                            onStatus = { offlineStatus = it },
                        )
                    },
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Save Area Offline")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        mapView?.let { centerMapToPoints(it, filteredPoints) }
                    },
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Recenter")
                }
            }
        }

        offlineStatus?.let { status ->
            item {
                Text(
                    text = status,
                    color = NetWatchSecondaryText,
                    fontSize = 12.sp,
                )
            }
        }

        if (filteredPoints.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "No mapped points yet",
                            color = NetWatchPrimaryText,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        )
                        Text(
                            text = if (locationPermissionGranted) {
                                "Location permission is already granted. Keep monitoring active and move through areas to generate coverage points."
                            } else {
                                "Location permission is required for geotagged map points."
                            },
                            color = NetWatchSecondaryText,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Recent mapped events",
                    color = NetWatchPrimaryText,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
            items(filteredPoints.take(10)) { point ->
                LocationEventRow(point = point)
            }
        }
    }
}

private fun renderMapOverlays(
    mapView: MapView,
    points: List<CoveragePoint>,
    showRouteLine: Boolean,
) {
    mapView.overlays.removeAll { it is Marker || it is Polyline }

    if (showRouteLine && points.size > 1) {
        val route = Polyline().apply {
            outlinePaint.color = AndroidColor.parseColor("#66FFDB")
            outlinePaint.strokeWidth = 5f
            setPoints(points.reversed().map { GeoPoint(it.latitude, it.longitude) })
        }
        mapView.overlays.add(route)
    }

    points.take(200).forEach { point ->
        mapView.overlays.add(createMarker(mapView, point))
    }

    mapView.invalidate()
}

private fun createMarker(mapView: MapView, point: CoveragePoint): Marker {
    return Marker(mapView).apply {
        position = GeoPoint(point.latitude, point.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = point.message
        snippet = point.signalDbm?.let { "$it dBm" } ?: "signal unavailable"

        val baseIcon = AppCompatResources.getDrawable(
            mapView.context,
            org.osmdroid.library.R.drawable.marker_default,
        )?.mutate()
        if (baseIcon != null) {
            DrawableCompat.setTint(baseIcon, markerTint(point.type))
            icon = baseIcon
        }
    }
}

private fun markerTint(type: NetworkEventType): Int {
    return when (type) {
        NetworkEventType.OUTAGE_START -> AndroidColor.parseColor("#F43F5E")
        NetworkEventType.SPEED_TEST -> AndroidColor.parseColor("#22D3EE")
        NetworkEventType.ANOMALY -> AndroidColor.parseColor("#FBBF24")
        else -> AndroidColor.parseColor("#66FFDB")
    }
}

private fun centerMapToPoints(
    mapView: MapView,
    points: List<CoveragePoint>,
) {
    if (points.isEmpty()) {
        return
    }
    if (points.size == 1) {
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(points.first().latitude, points.first().longitude))
        return
    }

    val maxLat = points.maxOf { it.latitude }
    val minLat = points.minOf { it.latitude }
    val maxLon = points.maxOf { it.longitude }
    val minLon = points.minOf { it.longitude }
    mapView.zoomToBoundingBox(BoundingBox(maxLat, maxLon, minLat, minLon), true)
}

private fun cacheVisibleArea(
    mapView: MapView,
    minZoom: Int,
    maxZoom: Int,
    onStatus: (String) -> Unit,
) {
    val safeMin = minZoom.coerceAtLeast(6)
    val safeMax = maxZoom.coerceAtMost(18).coerceAtLeast(safeMin)
    val bbox = mapView.boundingBox

    CacheManager(mapView).downloadAreaAsync(
        mapView.context,
        bbox,
        safeMin,
        safeMax,
        object : CacheManager.CacheManagerCallback {
            override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
                onStatus("Caching $progress% (zoom $currentZoomLevel)")
            }

            override fun downloadStarted() {
                onStatus("Offline tile download started")
            }

            override fun setPossibleTilesInArea(total: Int) {
                onStatus("Preparing $total tiles for offline cache")
            }

            override fun onTaskComplete() {
                onStatus("Offline map area saved")
            }

            override fun onTaskFailed(errors: Int) {
                onStatus("Offline map save failed ($errors)")
            }
        },
    )
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = NetWatchAccent, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(text = title, color = NetWatchSecondaryText, fontSize = 12.sp)
                Text(text = value, color = NetWatchPrimaryText, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LocationEventRow(point: CoveragePoint) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    Card(
        colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = if (point.type == NetworkEventType.OUTAGE_START) NetWatchDanger else NetWatchAccent,
            )
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(
                    text = point.message,
                    color = NetWatchPrimaryText,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = "${formatter.format(Date(point.timestampMs))}  •  ${"%.5f".format(point.latitude)}, ${"%.5f".format(point.longitude)}",
                    color = NetWatchSecondaryText,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = point.signalDbm?.let { "$it dBm" } ?: "-- dBm",
                color = NetWatchAccent,
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
        }
    }
}

private data class CoveragePoint(
    val latitude: Double,
    val longitude: Double,
    val signalDbm: Int?,
    val type: NetworkEventType,
    val message: String,
    val timestampMs: Long,
)

private enum class MapLayerFilter(
    val label: String,
) {
    ALL("All"),
    OUTAGES("Outages"),
    SPEED_TESTS("Tests"),
    ANOMALIES("Anomalies");

    fun matches(type: NetworkEventType): Boolean {
        return when (this) {
            ALL -> true
            OUTAGES -> type == NetworkEventType.OUTAGE_START || type == NetworkEventType.OUTAGE_END
            SPEED_TESTS -> type == NetworkEventType.SPEED_TEST
            ANOMALIES -> type == NetworkEventType.ANOMALY
        }
    }
}
