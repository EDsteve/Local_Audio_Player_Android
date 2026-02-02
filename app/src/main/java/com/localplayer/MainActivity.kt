package com.localplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import com.localplayer.ui.LocalPlayerTheme
import com.localplayer.ui.MainScaffold
import com.localplayer.ui.PlayerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
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
