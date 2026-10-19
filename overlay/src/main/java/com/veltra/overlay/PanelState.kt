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

    /**
     * Apakah window overlay lagi hidup.
     * Dibaca TileService (ubin Quick Settings) — karena ubin dan panel jalan di
     * proses yang sama, cukup pakai flag ini, nggak perlu IPC.
     */
    @Volatile
    var panelRunning: Boolean = false

    /** Nama paket game terakhir yang profilnya sudah diterapkan (dipakai overlay). */
    @Volatile
    var lastProfilePkg: String? = null

    /**
     * Pengaturan panel terakhir kali dinyalakan dari aplikasi.
     * Disimpan di sini supaya ubin Quick Settings bisa menyalakan panel dengan
     * gaya yang sama tanpa harus membaca DataStore milik modul app.
     */
    @Volatile
    var lastAutoHide: Boolean = true

    @Volatile
    var lastMode: PanelMode = PanelMode.SIDEBAR

    @Volatile
    var lastSide: SidebarSide = SidebarSide.RIGHT
}
