package com.veltra.overlay.crosshair

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
import com.veltra.boost.GameDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Crosshair mengambang buat bantuin aiming.
 *
 * Yang penting:
 *  - Window-nya **FLAG_NOT_TOUCHABLE** → sentuhan tembus 100% ke game.
 *    Nggak ada ghost touch, nggak ada area mati.
 *  - Ukurannya sekecil mungkin (persegi di tengah layar), transparan.
 *  - Posisi digeser dari dalam aplikasi VELTRA (offset X/Y), bukan dengan
 *    menyeret window — karena window-nya memang tidak menerima sentuhan.
 *  - Bisa otomatis hilang di luar game (butuh izin Akses Penggunaan).
 *
 * Jujur: ini murni gambar di atas layar. VELTRA tidak membaca memori game,
 * tidak mengubah nilai aim, dan tidak melakukan auto-aim apa pun.
 */
@AndroidEntryPoint
class CrosshairService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    @Inject lateinit var gameDetector: GameDetector

    private lateinit var wm: WindowManager
    private var view: ComposeView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry get() = savedState.savedStateRegistry

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)

    private var config by mutableStateOf(CrosshairConfig(enabled = true))
    private var visible by mutableStateOf(true)

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        isRunning = true
        attach()
        startGameWatcher()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        config = CrosshairConfig.from(intent)
        relayout()
        return START_STICKY
    }

    private fun attach() {
        val v = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@CrosshairService)
            setViewTreeSavedStateRegistryOwner(this@CrosshairService)
            setContent {
                if (visible) VeltraCrosshair(config)
            }
        }
        view = v
        runCatching { wm.addView(v, buildParams()) }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    /**
     * Sembunyikan di luar game. Sama aturan amannya dengan panel:
     * kalau aplikasi depan tidak dikenali, jangan pernah sembunyi.
     */
    private fun startGameWatcher() {
        scope.launch {
            var outsideStreak = 0
            while (isActive) {
                val usage = gameDetector.hasUsageAccess()
                val fg = if (usage) gameDetector.foregroundPackage() else null
                val inGame = fg != null && gameDetector.isGame(fg)
                val shouldShow = when {
                    !config.onlyInGame -> true
                    !usage -> true
                    fg == null -> true
                    inGame -> true.also { outsideStreak = 0 }
                    else -> { outsideStreak++; outsideStreak < 3 }
                }
                if (shouldShow != visible) {
                    visible = shouldShow
                    applyVisibility(shouldShow)
                }
                delay(3_000)
            }
        }
    }

    private fun applyVisibility(show: Boolean) {
        if (show) {
            if (view == null) attach() else relayout()
        } else {
            view?.let { runCatching { wm.removeView(it) } }
            view = null
        }
    }

    private fun relayout() {
        view?.let { v -> runCatching { wm.updateViewLayout(v, buildParams()) } }
    }

    private fun buildParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val px = (config.sizeDp * 2.6f).roundToInt().coerceAtLeast(72)
        val dm = resources.displayMetrics

        return WindowManager.LayoutParams(
            px, px, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or   // sentuhan tembus ke game
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = (config.offsetXDp * dm.density).roundToInt()
            y = (config.offsetYDp * dm.density).roundToInt()
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(
                CHANNEL_ID, "VELTRA Crosshair", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VELTRA Crosshair aktif")
            .setContentText("Gambar bantu bidik — tidak menyentuh game")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        isRunning = false
        job.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "veltra_crosshair"
        const val NOTIF_ID = 4319
        const val ACTION_STOP = "com.veltra.crosshair.STOP"

        fun hasPermission(ctx: Context): Boolean =
            Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(ctx)

        fun start(ctx: Context, config: CrosshairConfig) {
            if (!hasPermission(ctx)) return
            val i = config.toIntent(Intent(ctx, CrosshairService::class.java))
            ctx.startForegroundService(i)
        }

        /** Kirim konfigurasi baru tanpa matiin service (langsung berubah di layar). */
        fun update(ctx: Context, config: CrosshairConfig) {
            if (!isRunning) return
            ctx.startService(config.toIntent(Intent(ctx, CrosshairService::class.java)))
        }

        fun stop(ctx: Context) =
            ctx.startService(Intent(ctx, CrosshairService::class.java).setAction(ACTION_STOP))

        @Volatile var isRunning = false
            internal set
    }

}
