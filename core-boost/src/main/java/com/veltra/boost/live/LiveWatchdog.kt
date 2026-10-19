package com.veltra.boost.live

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.veltra.boost.MetricsBus
import kotlinx.coroutines.*

/**
 * Jantung LIVE GUARD.
 * Cek tiap 5 detik: aplikasi live masih hidup atau sudah dibunuh sistem?
 * Kalau mati → langsung notifikasi. Live yang sudah putus TIDAK bisa disambung
 * dari sisi kita, jadi yang diselamatkan adalah kecepatan user sadar & mulai ulang.
 */
object LiveWatchdog {

    private const val CHANNEL = "veltra_live_guard"
    private const val NOTIF = 4321
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Sedang menjaga live? Dipakai profil per-game supaya nggak start dobel. */
    fun isRunning(): Boolean = job?.isActive == true

    fun start(ctx: Context, target: String, intervalMs: Long = 5_000) {
        if (job?.isActive == true) return
        val guard = LiveGuardProvider.get(ctx)
        createChannel(ctx)

        job = scope.launch {
            var missStreak = 0
            while (isActive) {
                val alive = guard.isAlive(target)
                MetricsBus.publishLive(alive)
                if (!alive) {
                    missStreak++
                    // 2x berturut-turut baru yakin (hindari false alarm saat transisi)
                    if (missStreak >= 2) {
                        notifyDead(ctx, target)
                        missStreak = 0
                    }
                } else missStreak = 0
                delay(intervalMs)
            }
        }
    }

    fun stop(ctx: Context) {
        job?.cancel()
        job = null
        MetricsBus.publishLive(false)
        runCatching {
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIF)
        }
    }

    private fun notifyDead(ctx: Context, pkg: String) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("LIVE PUTUS — ${StreamApps.label(pkg)} dibunuh sistem")
            .setContentText("RAM penuh atau battery saver ROM. Ketuk untuk buka lagi.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .apply {
                ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { intent ->
                    setContentIntent(
                        android.app.PendingIntent.getActivity(
                            ctx, 0, intent,
                            android.app.PendingIntent.FLAG_IMMUTABLE or
                                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                }
            }
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF, notif)
    }

    private fun createChannel(ctx: Context) {
        val ch = NotificationChannel(
            CHANNEL, "VELTRA Live Guard", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Peringatan kalau live-mu mati di tengah game" }
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(ch)
    }
}

/** Supaya LiveGuard (butuh Hilt) bisa dipakai dari object statis. */
object LiveGuardProvider {
    private var instance: LiveGuard? = null
    fun bind(guard: LiveGuard) { instance = guard }
    fun get(ctx: Context): LiveGuard = instance
        ?: LiveGuard(ctx).also { bind(it) }
}
