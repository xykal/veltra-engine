package com.veltra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.veltra.core.VeltraTheme
import com.veltra.launcher.LauncherScreen
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
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = "launcher") {
            composable("launcher") { LauncherScreen(onSettings = { nav.navigate("settings") }) }
            composable("settings") { /* TODO Phase 2 */ }
        }
    }
}
