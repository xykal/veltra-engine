package com.veltra.boost

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.Choreographer
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlin.math.roundToInt

/**
 * Sampling metrik real-time: FPS (Choreographer), ping (Socket), suhu (Thermal API).
 * Jalan sebagai foreground service — transparan ke user, tidak hidden.
 */
class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var frames = 0
    private var lastTs = 0L
    private var currentFps = 60

    private val choreographerCallback: Choreographer.FrameCallback = Choreographer.FrameCallback { ts ->
        frames++
        if (lastTs == 0L) lastTs = ts
        val elapsedNs = ts - lastTs
        if (elapsedNs >= 1_000_000_000L) {
            currentFps = (frames * 1_000_000_000L / elapsedNs).toFloat().roundToInt()
            frames = 0
            lastTs = ts
            MetricsBus.publishFps(currentFps)
        }
        Choreographer.getInstance().postFrameCallback(this.choreographerCallback)
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        Choreographer.getInstance().postFrameCallback(choreographerCallback)
        scope.launch {
            while (isActive) {
                MetricsBus.publishPing(pingOnce())
                delay(3_000)
            }
        }
    }

    private fun pingOnce(): Int = runCatching {
        val addr = java.net.InetAddress.getByName("1.1.1.1")
        val start = System.currentTimeMillis()
        val reachable = addr.isReachable(1_000)
        if (reachable) (System.currentTimeMillis() - start).toInt() else -1
    }.getOrDefault(-1)

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VELTRA aktif")
            .setContentText("Monitoring performa game • $currentFps FPS")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "VELTRA Monitor", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        Choreographer.getInstance().removeFrameCallback(choreographerCallback)
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "veltra_monitor"
        const val NOTIF_ID = 4317
    }
}
