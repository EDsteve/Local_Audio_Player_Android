package com.localplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import com.localplayer.ui.LocalPlayerTheme
import com.localplayer.ui.MainScaffold
import com.localplayer.ui.PlayerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            
            // Keep screen on while scanning/loading (genre enhancement can take a while)
            KeepScreenOn(enabled = uiState.isLoading)
            
            LocalPlayerTheme {
                Surface {
                    MainScaffold(viewModel)
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * Composable that keeps the screen on while enabled.
 * Uses FLAG_KEEP_SCREEN_ON to prevent the screen from dimming or turning off
 * during long-running operations like library scanning with genre enhancement.
 */
@androidx.compose.runtime.Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        if (enabled) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = false
        }
    }
}
