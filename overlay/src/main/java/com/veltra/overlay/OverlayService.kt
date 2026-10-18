package com.veltra.overlay

import android.app.Notification
import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
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
 * Floating Panel VELTRA.
 *
 * Dua gaya:
 *  - **HUD**     : kapsul mini → panel 300×468 mengambang (bisa digeser)
 *  - **SIDEBAR** : handle 20dp nempel kiri/kanan → panel 232×460 geser keluar (gaya ROG)
 *
 * Perilaku bersama:
 *  - Auto-hide di luar game (butuh izin Akses Penggunaan)
 *  - Jalan sebagai foreground service supaya nggak jadi korban pertama saat
 *    sistem membunuh aplikasi background.
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

    private var mode = PanelMode.HUD
    private var side = SidebarSide.RIGHT
    private var autoHide = true
    private var posY = 140

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
                if (!visible) return@setContent
                when (mode) {
                    PanelMode.SIDEBAR -> VeltraSidebar(
                        expanded = expanded,
                        onExpandChange = { expanded = it; relayout() },
                        onDrag = { dy -> posY = (posY + dy.toInt()).coerceIn(0, 1_200); relayout() },
                        fpsFlow = MetricsBus.fps,
                        pingFlow = MetricsBus.ping,
                        tempFlow = MetricsBus.temp,
                        statusFlow = MetricsBus.status,
                        liveFlow = MetricsBus.live,
                        onStabilize = {
                            boostEngine.runSafeBoost()
                            com.veltra.boost.HintManager.setTargetFps(60)
                        },
                        onClose = { stopSelf() },
                        side = side
                    )
                    PanelMode.HUD -> VeltraHud(
                        expanded = expanded,
                        onExpandChange = { expanded = it; relayout() },
                        fpsFlow = MetricsBus.fps,
                        pingFlow = MetricsBus.ping,
                        tempFlow = MetricsBus.temp,
                        statusFlow = MetricsBus.status,
                        liveFlow = MetricsBus.live,
                        onStabilize = {
                            boostEngine.runSafeBoost()
                            com.veltra.boost.HintManager.setTargetFps(60)
                        },
                        onClose = { stopSelf() }
                    )
                }
            }
        }
        panelView = view
        wm.addView(view, buildParams())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    /**
     * Auto-hide yang AMAN.
     * Aturan baru (hasil bug report user: panel hilang sendiri di dalam game):
     *  - Tidak pernah sembunyi kalau aplikasi depan tidak dikenali (foregroundPackage null)
     *  - Harus 3x berturut-turut yakin bukan game, baru sembunyi
     *  - Grace period 20 detik sejak service nyala (hindari salah deteksi saat game loading)
     */
    private fun startAutoHideWatcher() {
        scope.launch {
            val startAt = System.currentTimeMillis()
            var outsideStreak = 0
            while (isActive) {
                val usageGranted = gameDetector.hasUsageAccess()
                val fg = if (usageGranted) gameDetector.foregroundPackage() else null
                val inGame = fg != null && gameDetector.isGame(fg)

                val shouldShow = when {
                    !autoHide -> true
                    !usageGranted -> true
                    fg == null -> true                              // tidak tahu → jangan sembunyi
                    System.currentTimeMillis() - startAt < 20_000 -> true
                    inGame -> true.also { outsideStreak = 0 }
                    else -> {
                        outsideStreak++
                        outsideStreak < 3                           // butuh 3x yakin (~9 detik)
                    }
                }

                if (shouldShow != visible) {
                    visible = shouldShow
                    applyVisibility(shouldShow)
                }
                delay(3_000)
            }
        }
    }

    private fun relayout() {
        panelView?.let { v -> runCatching { wm.updateViewLayout(v, buildParams()) } }
    }

    /** Sembunyikan = cabut view beneran. Menyisakan window transparan bikin layar mati. */
    private fun applyVisibility(show: Boolean) {
        if (show) {
            if (panelView == null) attachPanel()
            else relayout()
        } else {
            panelView?.let { v ->
                runCatching { wm.removeView(v) }
                panelView = null
            }
        }
    }

    private fun buildParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val (w, h) = when (mode) {
            PanelMode.SIDEBAR ->
                (if (expanded) SIDEBAR_W else HANDLE_W) to (if (expanded) SIDEBAR_H else HANDLE_H)
            PanelMode.HUD ->
                (if (expanded) PANEL_W else COLLAPSED_W) to (if (expanded) PANEL_H else COLLAPSED_H)
        }

        val gravity = when (mode) {
            PanelMode.SIDEBAR -> Gravity.TOP or
                    (if (side == SidebarSide.RIGHT) Gravity.END else Gravity.START)
            PanelMode.HUD -> Gravity.TOP or Gravity.END
        }

        return WindowManager.LayoutParams(
            w, h, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    // KUNCI: tanpa flag ini, seluruh layar dianggap tertutup window
                    // → muncul "area mati" & ghost touch di dalam game.
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            x = 0
            y = posY
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            autoHide = it.getBooleanExtra(EXTRA_AUTO_HIDE, autoHide)
            val modeName = it.getStringExtra(EXTRA_MODE)
            if (!modeName.isNullOrBlank()) mode = runCatching { PanelMode.valueOf(modeName) }
                .getOrDefault(PanelMode.HUD)
            val sideName = it.getStringExtra(EXTRA_SIDE)
            if (!sideName.isNullOrBlank()) side = runCatching { SidebarSide.valueOf(sideName) }
                .getOrDefault(SidebarSide.RIGHT)
        }
        relayout()
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        panelView?.let { runCatching { wm.removeView(it) } }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        // HUD
        const val PANEL_W = 300
        const val PANEL_H = 468
        const val COLLAPSED_W = 46
        const val COLLAPSED_H = 46
        // Sidebar
        const val SIDEBAR_W = 232
        const val SIDEBAR_H = 460
        const val HANDLE_W = 20
        const val HANDLE_H = 118

        const val CHANNEL_ID = "veltra_panel"
        const val NOTIF_ID = 4318

        const val EXTRA_AUTO_HIDE = "auto_hide"
        const val EXTRA_MODE = "panel_mode"
        const val EXTRA_SIDE = "panel_side"

        fun hasPermission(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

        /** Nyalakan panel. Harus dipanggil saat aplikasi masih di depan (Android 12+). */
        fun start(
            ctx: Context,
            autoHide: Boolean = true,
            mode: PanelMode = PanelMode.HUD,
            side: SidebarSide = SidebarSide.RIGHT
        ) {
            if (!hasPermission(ctx)) return
            ctx.startForegroundService(
                Intent(ctx, OverlayService::class.java)
                    .putExtra(EXTRA_AUTO_HIDE, autoHide)
                    .putExtra(EXTRA_MODE, mode.name)
                    .putExtra(EXTRA_SIDE, side.name)
            )
        }

        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, OverlayService::class.java))
    }
}
