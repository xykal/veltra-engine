package com.veltra.launcher.qs

import com.veltra.R

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.veltra.boost.MonitorService
import com.veltra.overlay.OverlayService
import com.veltra.overlay.PanelState

/**
 * Ubin Quick Settings #1 — PANEL.
 *
 * Ketuk sekali: panel performa menyala / mati, tanpa buka VELTRA dulu.
 *
 * Catatan jujur soal Android 12+:
 *   Sistem melarang aplikasi menyalakan foreground service dari background.
 *   VELTRA punya dua jalan keluar yang sah:
 *     1. Memegang izin SYSTEM_ALERT_WINDOW (pengecualian resmi — lihat
 *        developer.android.com/develop/background-work/services/fgs/restrictions-bg-start).
 *        Mulai Android 15 pengecualian ini minta syarat tambahan: harus ada
 *        jendela overlay yang sedang terlihat.
 *     2. Kalau jalan pertama ditolak, ubin membuka `TileActionActivity` —
 *        aktivitas bening yang langsung selesai. Begitu aktivitas tampil,
 *        aplikasi sah dianggap di depan, dan service boleh dinyalakan.
 */
class VeltraTileService : TileService() {

    override fun onStartListening() = refresh()

    override fun onTileAdded() = refresh()

    override fun onClick() {
        if (!OverlayService.hasPermission(this)) {
            // Belum ada izin overlay → antar user ke aplikasi, jangan diam saja.
            collapseTo(
                packageManager.getLaunchIntentForPackage(packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            return
        }

        if (PanelState.panelRunning) {
            OverlayService.stopAll(this)
            refresh()
            return
        }

        val ok = runCatching {
            MonitorService.start(this)
            OverlayService.startSaved(this)
        }.isSuccess

        if (!ok) {
            // Android 15+ tanpa overlay terlihat → lewat aktivitas bening.
            collapseTo(TileActionActivity.intent(this, TileActionActivity.ACTION_START_PANEL))
        } else {
            refresh()
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val on = PanelState.panelRunning
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_panel_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !OverlayService.hasPermission(this) -> getString(R.string.tile_need_permission)
                on -> getString(R.string.tile_on)
                else -> getString(R.string.tile_off)
            }
        }
        tile.updateTile()
    }

    /** Buka aktivitas sambil menutup panel Quick Settings (dua versi API). */
    private fun collapseTo(intent: Intent?) {
        if (intent == null) return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pi = PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pi)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }.onFailure {
            // Layar terkunci / tidak ada izin buka aktivitas → buka biasa saja.
            runCatching { startActivity(intent) }
        }
    }

    companion object {
        /** Minta sistem memuat ulang tampilan ubin (dipakai setelah panel berubah). */
        fun requestRefresh(ctx: Context) {
            runCatching { requestListeningState(ctx, android.content.ComponentName(ctx, VeltraTileService::class.java)) }
        }
    }
}
