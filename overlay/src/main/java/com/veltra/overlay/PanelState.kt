package com.veltra.overlay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.veltra.overlay.crosshair.CrosshairConfig

/**
 * Keadaan panel yang dibagi antara aplikasi (pengaturan) dan window overlay.
 * Service overlay nggak bisa baca DataStore milik modul app, jadi status
 * crosshair / target FPS / mode senyap ditaruh di sini dan diset dari aplikasi.
 */
object PanelState {
    var crosshairOn by mutableStateOf(false)
    var crosshair: CrosshairConfig = CrosshairConfig()
    var quietOn by mutableStateOf(false)
    var targetFps by mutableStateOf(60)
}
