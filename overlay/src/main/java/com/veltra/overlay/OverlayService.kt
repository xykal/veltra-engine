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
import kotlinx.coroutines.flow.MutableStateFlow
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
    @Inject lateinit var profileStore: com.veltra.boost.profile.ProfileStore
    @Inject lateinit var advanced: com.veltra.boost.advanced.AdvancedManager

    private lateinit var wm: WindowManager
    private var panelView: ComposeView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry get() = savedState.savedStateRegistry

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var expanded by mutableStateOf(false)
    private var visible by mutableStateOf(true)
    private val ramFree = MutableStateFlow(-1)

    private var mode = PanelMode.HUD
    private var side = SidebarSide.RIGHT
    private var autoHide = true
    private var posY = 140

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        PanelState.panelRunning = true
        startAsForeground()
        attachPanel()
        startProfileWatcher()
        startAutoHideWatcher()
        startRamWatcher()
    }

    // ─────────────────────────────────────────────────────────────
    //  PROFIL PER-GAME — diterapkan otomatis saat game muncul di depan
    // ─────────────────────────────────────────────────────────────

    /** Cache profil di memori: overlay nggak perlu baca DataStore tiap 3 detik. */
    private var profileMap: Map<String, com.veltra.boost.profile.GameProfile> = emptyMap()

    private fun startProfileWatcher() {
        scope.launch {
            profileStore.profiles.collect { profileMap = it }
        }
        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            while (isActive) {
                val fg = gameDetector.foregroundPackage()
                val inGame = fg != null && gameDetector.isGame(fg)
                if (inGame && fg != PanelState.lastProfilePkg) {
                    PanelState.lastProfilePkg = fg
                    applyProfile(fg!!)
                } else if (!inGame && fg != null && PanelState.lastProfilePkg != null) {
                    // keluar dari game → lepas penanda, biar masuk lagi nanti diterapkan ulang
                    PanelState.lastProfilePkg = null
                }
                delay(3_000)
            }
        }
    }

    /**
     * Terapkan profil sebuah game.
     *
     * Urutannya penting: hint FPS dipasang TERAKHIR supaya nggak ketimpa
     * nilai bawaan dari Live Guard atau dari Game Mode.
     */
    private fun applyProfile(pkg: String) {
        val p = profileMap[pkg]
            ?: com.veltra.boost.profile.GameProfile.defaultFor(pkg)
        if (!p.isCustom) return

        // 1. Bersihkan gangguan sesuai level
        val mode = when (p.boost) {
            com.veltra.boost.profile.GameProfile.BoostLevel.HEMAT ->
                com.veltra.boost.BoostEngine.Mode.HEMAT
            com.veltra.boost.profile.GameProfile.BoostLevel.SEIMBANG ->
                com.veltra.boost.BoostEngine.Mode.SAFE
            com.veltra.boost.profile.GameProfile.BoostLevel.MAKSIMAL ->
                com.veltra.boost.BoostEngine.Mode.AGGRESSIVE
        }
        boostEngine.runSafeBoost(mode)

        // 2. Mode senyap
        if (p.quiet != PanelState.quietOn) setQuiet(p.quiet)

        // 3. Crosshair
        if (p.crosshair && !PanelState.crosshairOn) toggleCrosshair()
        else if (!p.crosshair && PanelState.crosshairOn) toggleCrosshair()

        // 4. Penjaga live (butuh aplikasi live terpasang)
        if (p.liveGuard) {
            val stream = com.veltra.boost.live.StreamApps.installedStreamApps(this).firstOrNull()
            if (stream != null && com.veltra.boost.live.LiveWatchdog.isRunning().not()) {
                boostEngine.runLiveBoost(stream)
            }
        }

        // 5. Kunci refresh rate & FPS — cuma jalan kalau Shizuku/root aktif
        if (p.hzLock > 0 || p.fpsLock > 0) {
            scope.launch {
                runCatching {
                    if (p.hzLock > 0) advanced.actions().lockRefreshRate(p.hzLock)
                    if (p.fpsLock > 0) advanced.actions().lockGameFps(pkg, p.fpsLock)
                }
            }
        }

        // 6. Hint FPS — paling akhir
        PanelState.targetFps = p.targetFps
        com.veltra.boost.HintManager.setTargetFps(p.targetFps)
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
                        ramFlow = ramFree,
                        crosshairOn = PanelState.crosshairOn,
                        quietOn = PanelState.quietOn,
                        targetFps = PanelState.targetFps,
                        onStabilize = {
                            boostEngine.runSafeBoost()
                            com.veltra.boost.HintManager.setTargetFps(PanelState.targetFps)
                        },
                        onCrosshair = { toggleCrosshair() },
                        onQuiet = { toggleQuiet() },
                        onFps = { fps ->
                            PanelState.targetFps = fps
                            com.veltra.boost.HintManager.setTargetFps(fps)
                        },
                        onOpenApp = { openVeltra() },
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

    /** RAM bebas diperbarui tiap 5 detik — cukup murah, dan cuma jalan saat panel hidup. */
    private fun startRamWatcher() {
        scope.launch {
            val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            while (isActive) {
                val mi = android.app.ActivityManager.MemoryInfo()
                runCatching { am.getMemoryInfo(mi) }
                ramFree.value = (mi.availMem / (1024 * 1024)).toInt()
                delay(5_000)
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

    /** Toggle crosshair langsung dari panel dalam game. */
    private fun toggleCrosshair() {
        if (!com.veltra.overlay.crosshair.CrosshairService.hasPermission(this)) {
            PanelState.crosshairOn = false
            return
        }
        if (PanelState.crosshairOn) {
            com.veltra.overlay.crosshair.CrosshairService.stop(this)
            PanelState.crosshairOn = false
        } else {
            val cfg = PanelState.crosshair.copy(enabled = true)
            com.veltra.overlay.crosshair.CrosshairService.start(this, cfg)
            PanelState.crosshairOn = true
        }
    }

    /**
     * Mode senyap. Kalau izin "Jangan Ganggu" sudah diberikan, pakai
     * interruption filter (DND beneran). Kalau belum, pakai ringer mode
     * (senyap sederhana) — jujur, ini bukan DND penuh.
     */
    /** Nyalakan/matikan mode senyap (dipakai tombol panel dan profil per-game). */
    fun toggleQuiet() = setQuiet(!PanelState.quietOn)

    private fun setQuiet(on: Boolean) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val hasPolicy = if (Build.VERSION.SDK_INT >= 23) nm.isNotificationPolicyAccessGranted else false
        PanelState.quietOn = on
        runCatching {
            if (hasPolicy) {
                nm.setInterruptionFilter(
                    if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                    else NotificationManager.INTERRUPTION_FILTER_ALL
                )
            } else {
                val am = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
                am.ringerMode = if (on) android.media.AudioManager.RINGER_MODE_SILENT
                else android.media.AudioManager.RINGER_MODE_NORMAL
            }
        }
    }

    /** Buka aplikasi VELTRA dari dalam game. */
    private fun openVeltra() {
        runCatching {
            val i = packageManager.getLaunchIntentForPackage("com.veltra.engine")
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (i != null) startActivity(i)
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
        PanelState.panelRunning = false
        PanelState.lastProfilePkg = null
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
        const val SIDEBAR_W = 268
        const val SIDEBAR_H = 476
        const val HANDLE_W = 40
        const val HANDLE_H = 128

        const val CHANNEL_ID = "veltra_panel"
        const val NOTIF_ID = 4318

        const val EXTRA_AUTO_HIDE = "auto_hide"
        const val EXTRA_MODE = "panel_mode"
        const val EXTRA_SIDE = "panel_side"

        fun hasPermission(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

        /**
         * Nyalakan panel memakai pengaturan terakhir yang dipakai aplikasi.
         * Dipakai ubin Quick Settings, yang nggak bisa baca DataStore modul app.
         */
        fun startSaved(ctx: Context) {
            start(ctx, PanelState.lastAutoHide, PanelState.lastMode, PanelState.lastSide)
        }

        /** Matikan panel + monitor + crosshair sekaligus. */
        fun stopAll(ctx: Context) {
            stop(ctx)
            com.veltra.boost.MonitorService.stop(ctx)
            com.veltra.overlay.crosshair.CrosshairService.stop(ctx)
        }

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
