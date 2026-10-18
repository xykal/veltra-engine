package com.veltra.overlay

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.veltra.boost.MetricsBus

/**
 * Floating Panel VELTRA — HUD Premium (V2).
 *
 * UX:
 *  - Kapsul mini 42dp nempel di pinggir (auto-dim 70% kalau idle)
 *  - Tap → expand jadi HUD 300x420 (FPS gauge, ping wave, temp, STABILIZE hexagon)
 *  - Drag bebas, posisi diingat
 *  - TIDAK menutup analog: default di kanan atas, bisa pindah kiri
 */
class OverlayService : Service(), LifecycleOwner {

    private lateinit var wm: WindowManager
    private var panelView: ComposeView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private var expanded by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        attachPanel()
    }

    private fun attachPanel() {
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                VeltraHud(
                    expanded = expanded,
                    onExpandChange = { expanded = it; relayout() },
                    fpsFlow = MetricsBus.fps,
                    pingFlow = MetricsBus.ping,
                    tempFlow = MetricsBus.temp,
                    statusFlow = MetricsBus.status,
                    liveFlow = MetricsBus.live,
                    onStabilize = { /* panggil BoostEngine */ },
                    onClose = { stopSelf() }
                )
            }
        }
        panelView = view
        wm.addView(view, buildParams(expanded))
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
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

    override fun onDestroy() {
        panelView?.let { runCatching { wm.removeView(it) } }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PANEL_W = 300
        const val PANEL_H = 420
        const val COLLAPSED_W = 42
        const val COLLAPSED_H = 42

        fun hasPermission(ctx: Context): Boolean =
            Settings.canDrawOverlays(ctx)

        fun start(ctx: Context) {
            if (!hasPermission(ctx)) return
            val i = Intent(ctx, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, OverlayService::class.java))
    }
}
