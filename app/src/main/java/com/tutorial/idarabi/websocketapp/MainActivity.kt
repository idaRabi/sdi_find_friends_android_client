package com.tutorial.idarabi.websocketapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tutorial.idarabi.websocketapp.composables.MapLibreMap
import com.tutorial.idarabi.websocketapp.ui.theme.WebsocketappTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(
            applicationContext,
            "pk.1234567890", // You can use any dummy string if not using a tile server that requires an API key
            WellKnownTileServer.MapTiler // or use CUSTOM if you're using localhost/local tiles
        )
        enableEdgeToEdge()
        setContent {
            WebsocketappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapLibreMap()
                }
            }
        }
    }
}
