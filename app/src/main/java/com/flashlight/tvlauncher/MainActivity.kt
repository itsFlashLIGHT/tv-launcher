package com.flashlight.tvlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.flashlight.tvlauncher.ui.HomeScreen
import com.flashlight.tvlauncher.ui.theme.TvLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvLauncherTheme {
                HomeScreen()
            }
        }
    }
}
