package com.veltra.launcher.qs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.veltra.boost.MonitorService
import com.veltra.overlay.OverlayService

/**
 * Aktivitas bening buat jalan keluar kedua ubin Quick Settings.
 *
 * Android 15 ke atas cuma memberi pengecualian "mulai foreground service dari
 * background" kalau aplikasi punya izin overlay **dan** sedang menampilkan
 * jendela overlay. Kalau panel belum menyala, pengecualian itu belum berlaku —
 * makanya ubin membuka aktivitas ini dulu. Begitu aktivitas tampil, aplikasi sah
 * dianggap di depan, service boleh dinyalakan, lalu aktivitas langsung tutup.
 *
 * Tidak ada animasi, tidak masuk recents, tidak kelihatan sama sekali.
 */
class TileActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent?.action) {
            ACTION_START_PANEL -> {
                if (OverlayService.hasPermission(this)) {
                    runCatching {
                        MonitorService.start(this)
                        OverlayService.startSaved(this)
                    }
                }
            }
        }
        VeltraTileService.requestRefresh(this)
        finish()
        // Hilangkan transisi masuk/keluar supaya benar-benar tak terlihat.
        overridePendingTransition(0, 0)
    }

    companion object {
        const val ACTION_START_PANEL = "com.veltra.action.QS_START_PANEL"

        fun intent(ctx: Context, action: String): Intent =
            Intent(ctx, TileActionActivity::class.java)
                .setAction(action)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
    }
}
