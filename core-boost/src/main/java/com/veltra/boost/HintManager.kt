package com.veltra.boost

import android.content.Context
import android.os.Build
import android.os.PerformanceHintManager

/**
 * ADPF — Android Dynamic Performance Framework.
 * Kasih tahu scheduler: "30 detik ke depan kita butuh frame penuh".
 * Ini SATU-SATUNYA cara legal & aman bikin game lebih stabil di Android 12+.
 */
object HintManager {

    private var session: PerformanceHintManager.Session? = null
    private var targetFps = 60

    /** 
     * Kalau lagi live + main game, JANGAN paksa 60fps.
     * Encoder video butuh jatah hardware yang sama dengan renderer game.
     * TikTok live juga mentok 30fps di sisi penonton — jadi 30fps itu cukup.
     */
    fun setTargetFps(fps: Int) {
        targetFps = fps.coerceIn(20, 120)
        val ns = 1_000_000_000L / targetFps
        runCatching { session?.updateTargetWorkDuration(ns) }
    }

    fun acquire(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching {
            val manager = ctx.getSystemService(Context.PERFORMANCE_HINT_SERVICE)
                    as? PerformanceHintManager ?: return
            val targetNs = 1_000_000_000L / targetFps
            if (session == null) {
                session = manager.createHintSession(
                    intArrayOf(android.os.Process.myTid()),
                    targetNs
                )
            }
            session?.reportActualWorkDuration(targetNs)
            session?.updateTargetWorkDuration(targetNs)
        }
    }

    fun release() {
        runCatching { session?.close() }
        session = null
    }
}
