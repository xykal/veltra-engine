package com.veltra.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.veltra.boost.BoostEngine
import com.veltra.boost.GameDetector
import com.veltra.boost.MetricsBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Floating Panel VELTRA — HUD Premium (V2)
 *
 * Perilaku:
 *  - Kapsul mini 42dp nempel di pinggir
 *  - Tap → expand jadi HUD 300x420 (FPS gauge, ping wave, temp, STABILIZE hexagon)
 *  - **Auto-hide di luar game**: kalau user keluar game, panel menghilang sendiri
 *    (butuh izin "Akses Penggunaan"; tanpa izin panel tetap tampil terus)
 *  - Jalan sebagai **foreground service** supaya tidak jadi korban pertama
 *    saat sistem membunuh aplikasi background — ini kunci buat HP kentang.
 */
@AndroidEntryPoint
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    @Inject lateinit var gameDetector: GameDetector
    @Inject lateinit var boostEngine: BoostEngine

    private lateinit var wm: WindowManager
    private var panelView: ComposeView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry get() = savedState.savedStateRegistry

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var expanded by mutableStateOf(false)
    private var visible by mutableStateOf(true)
    private var autoHide = true

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        attachPanel()
        startAutoHideWatcher()
    }

    private fun startAsForeground() {
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    private fun attachPanel() {
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                if (visible) {
                    VeltraHud(
                        expanded = expanded,
                        onExpandChange = { expanded = it; relayout() },
                        fpsFlow = MetricsBus.fps,
                        pingFlow = MetricsBus.ping,
                        tempFlow = MetricsBus.temp,
                        statusFlow = MetricsBus.status,
                        liveFlow = MetricsBus.live,
                        onStabilize = {
                            // Aman: cuma membersihkan proses CACHED — game & aplikasi
                            // yang sedang dipakai tidak pernah disentuh.
                            boostEngine.runSafeBoost()
                            com.veltra.boost.HintManager.setTargetFps(60)
                        },
                        onClose = { stopSelf() }
                    )
                }
            }
        }
        panelView = view
        wm.addView(view, buildParams(expanded))
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    /**
     * Sembunyikan panel kalau user sudah keluar dari game.
     * Tanpa ini panel akan menutupi WhatsApp/Chrome dan bikin user sebel.
     */
    private fun startAutoHideWatcher() {
        scope.launch {
            while (isActive) {
                val inGame = gameDetector.isInGame()
                val usageGranted = gameDetector.hasUsageAccess()
                val shouldShow = if (!autoHide || !usageGranted) true else inGame
                if (shouldShow != visible) {
                    visible = shouldShow
                    panelView?.let { v ->
                        runCatching { wm.updateViewLayout(v, buildParams(expanded)) }
                    }
                }
                delay(3_000)
            }
        }
    }

    private fun relayout() {
        panelView?.let { v ->
            runCatching { wm.updateViewLayout(v, buildParams(expanded)) }
        }
    }

    private fun buildParams(expanded: Boolean): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        return WindowManager.LayoutParams(
            if (expanded) PANEL_W else COLLAPSED_W,
            if (expanded) PANEL_H else COLLAPSED_H,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 8
            y = 140
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VELTRA Panel aktif")
            .setContentText("Panel performa siap di dalam game")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID, "VELTRA Panel", NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    override fun onDestroy() {
        scope.cancel()
        panelView?.let { runCatching { wm.removeView(it) } }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        autoHide = intent?.getBooleanExtra(EXTRA_AUTO_HIDE, true) ?: autoHide
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PANEL_W = 300
        const val PANEL_H = 468
        const val COLLAPSED_W = 42
        const val COLLAPSED_H = 42
        const val CHANNEL_ID = "veltra_panel"
        const val NOTIF_ID = 4318

        fun hasPermission(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

        const val EXTRA_AUTO_HIDE = "auto_hide"

        /** Nyalakan panel. Harus dipanggil saat aplikasi masih di depan (Android 12+). */
        fun start(ctx: Context, autoHide: Boolean = true) {
            if (!hasPermission(ctx)) return
            ctx.startForegroundService(
                Intent(ctx, OverlayService::class.java).putExtra(EXTRA_AUTO_HIDE, autoHide)
            )
        }

        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, OverlayService::class.java))
    }
}
