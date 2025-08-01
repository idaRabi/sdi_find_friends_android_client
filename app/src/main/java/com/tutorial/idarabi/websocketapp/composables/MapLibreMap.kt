package com.tutorial.idarabi.websocketapp.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun MapLibreMap() {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                getMapAsync { mapboxMap ->
                    mapboxMap.setDebugActive(true)
                    mapboxMap.setStyle(
                        Style.Builder().fromUri("http://10.0.2.2:8090/styles/basic-preview/style.json")
                    ) {
                        // This block runs when the style has finished loading
                        val defaultLatLng = LatLng(52.52, 13.405) // Berlin, for example
                        val cameraPosition = CameraPosition.Builder()
                            .target(defaultLatLng)
                            .zoom(12.0)
                            .build()
                        mapboxMap.cameraPosition = cameraPosition
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}