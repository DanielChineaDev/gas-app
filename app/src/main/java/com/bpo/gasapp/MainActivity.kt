package com.bpo.gasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bpo.gasapp.ui.stations.StationListScreen
import com.bpo.gasapp.ui.theme.GasAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GasAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StationListScreen(onStationClick = { /* TODO: detalle */ })
                }
            }
        }
    }
}
