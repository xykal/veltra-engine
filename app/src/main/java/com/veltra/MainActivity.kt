package com.veltra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.veltra.core.LaunchSplash
import com.veltra.core.VeltraTheme
import com.veltra.launcher.LauncherScreen
import com.veltra.launcher.SettingsScreen
import com.veltra.launcher.ocr.OcrScreen
import com.veltra.launcher.tools.AppsScreen
import com.veltra.launcher.tools.JunkScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { VeltraRoot() }
    }
}

@Composable
fun VeltraRoot() {
    VeltraTheme {
        var splashDone by remember { mutableStateOf(false) }

        if (!splashDone) {
            LaunchSplash { splashDone = true }
        } else {
            val nav = rememberNavController()
            NavHost(navController = nav, startDestination = "launcher") {
                composable("launcher") {
                    LauncherScreen(
                        onSettings = { nav.navigate("settings") },
                        onOcr = { nav.navigate("ocr") },
                        onJunk = { nav.navigate("junk") },
                        onApps = { nav.navigate("apps") }
                    )
                }
                composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
                composable("ocr") { OcrScreen(onBack = { nav.popBackStack() }) }
                composable("junk") { JunkScreen(onBack = { nav.popBackStack() }) }
                composable("apps") { AppsScreen(onBack = { nav.popBackStack() }) }
            }
        }
    }
}
