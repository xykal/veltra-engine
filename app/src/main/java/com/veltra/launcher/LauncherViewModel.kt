package com.veltra.launcher

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veltra.boost.BoostEngine
import com.veltra.boost.GameDetector
import com.veltra.boost.MetricsBus
import com.veltra.boost.MonitorService
import com.veltra.boost.live.LiveGuard
import com.veltra.boost.live.StreamApps
import com.veltra.data.GameRepository
import com.veltra.overlay.crosshair.CrosshairConfig
import com.veltra.overlay.crosshair.CrosshairService
import com.veltra.overlay.OverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameInfo(
    val packageName: String,
    val name: String,
    val lastFps: Int = 60,
    val ping: Int = 24
)

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val repo: GameRepository,
    private val boost: BoostEngine,
    private val liveGuard: LiveGuard,
    private val gameDetector: GameDetector,
    private val settings: SettingsRepository,
    private val advanced: com.veltra.boost.advanced.AdvancedManager
) : ViewModel() {

    val games: StateFlow<List<GameInfo>> = repo.observeGames()
        .map { list -> list.map { GameInfo(it.packageName, it.label) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _stabilizing = MutableStateFlow(false)
    val stabilizing: StateFlow<Boolean> = _stabilizing.asStateFlow()

    // ---------- PREFERENSI ----------
    val autoHide: StateFlow<Boolean> =
        settings.autoHide.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val autoStart: StateFlow<Boolean> =
        settings.autoStart.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val panelMode: StateFlow<String> =
        settings.panelMode.stateIn(viewModelScope, SharingStarted.Eagerly, "HUD")
    val panelSide: StateFlow<String> =
        settings.panelSide.stateIn(viewModelScope, SharingStarted.Eagerly, "RIGHT")

    fun setAutoHide(v: Boolean) { viewModelScope.launch { settings.setAutoHide(v) } }
    fun setAutoStart(v: Boolean) { viewModelScope.launch { settings.setAutoStart(v) } }
    fun setPanelMode(v: String) { viewModelScope.launch { settings.setPanelMode(v) } }
    fun setPanelSide(v: String) { viewModelScope.launch { settings.setPanelSide(v) } }

    // ---------- MODE LANJUT (Shizuku / root) ----------
    private val _advLabel = mutableStateOf(advanced.label())
    val advancedLabel: State<String> get() = _advLabel

    private val _advResult = mutableStateOf("")
    val advancedResult: State<String> get() = _advResult

    val advancedReady: Boolean get() = advanced.level() != com.veltra.boost.advanced.PrivilegedShell.Level.NONE

    fun refreshAdvanced() { _advLabel.value = advanced.label() }

    fun requestAdvancedPermission() {
        advanced.requestShizukuPermission()
        viewModelScope.launch {
            delay(3_000)
            _advLabel.value = advanced.label()
        }
    }

    /** Game yang akan di-inject Game Mode-nya (default Free Fire Max). */
    private val _advTarget = mutableStateOf("com.dts.freefiremax")
    val advancedTarget: State<String> get() = _advTarget
    fun setAdvancedTarget(pkg: String) { _advTarget.value = pkg }

    fun runAdvanced(action: AdvancedAction) {
        viewModelScope.launch {
            _advResult.value = "Menjalankan…"
            val livePkg = streamApps.firstOrNull()
            val res: com.veltra.boost.advanced.AdvancedActions.Result = when (action) {
                AdvancedAction.PROTECT_LIVE -> livePkg?.let { advanced.actions().protectLive(it) }
                    ?: com.veltra.boost.advanced.AdvancedActions.Result(false, "Aplikasi live tidak ditemukan")
                AdvancedAction.COOLING -> advanced.actions().coolingMode(true, false)
                AdvancedAction.RES_DOWN -> advanced.actions().setResolutionScale(0.85f)
                AdvancedAction.RES_RESET -> advanced.actions().resetResolution()
                AdvancedAction.FREEZE_BLOAT -> advanced.actions().freezeBloat(advanced.commonBloat())
                AdvancedAction.TURBO_GAME -> advanced.actions().turboGame(_advTarget.value, 0.75f)
                AdvancedAction.RESET_GAME -> advanced.actions().resetGameMode(_advTarget.value)
                AdvancedAction.COMPILE_GAME -> advanced.actions().compileGame(_advTarget.value, "speed")
            }
            _advResult.value = if (res.ok) "✓ ${res.detail}" else "Gagal: ${res.detail}"
        }
    }

    private fun panelMode(): com.veltra.overlay.PanelMode =
        runCatching { com.veltra.overlay.PanelMode.valueOf(panelMode.value) }
            .getOrDefault(com.veltra.overlay.PanelMode.HUD)

    private fun panelSide(): com.veltra.overlay.SidebarSide =
        runCatching { com.veltra.overlay.SidebarSide.valueOf(panelSide.value) }
            .getOrDefault(com.veltra.overlay.SidebarSide.RIGHT)

    // ---------- CROSSHAIR ----------
    val crosshair: StateFlow<CrosshairConfig> =
        settings.crosshair.stateIn(viewModelScope, SharingStarted.Eagerly, CrosshairConfig())

    val sensitivity: StateFlow<Map<String, String>> =
        settings.sensitivity.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val lockedHz: StateFlow<Int> =
        settings.lockedHz.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun setCrosshair(c: CrosshairConfig) {
        viewModelScope.launch {
            settings.setCrosshair(c)
            if (!c.enabled) {
                CrosshairService.stop(ctx)
            } else if (CrosshairService.hasPermission(ctx)) {
                CrosshairService.start(ctx, c)
            } else {
                // belum diizinkan tampil di atas aplikasi lain → buka pengaturan
                requestOverlay()
            }
        }
    }

    fun saveSensitivity(pkg: String, note: String) {
        viewModelScope.launch { settings.setSensitivity(pkg, note) }
    }

    // ---------- KUNCI REFRESH RATE / FPS ----------
    fun lockHz(hz: Int) {
        viewModelScope.launch {
            _advResult.value = "Menjalankan…"
            val res = advanced.actions().lockRefreshRate(hz)
            settings.setLockedHz(if (res.ok) hz else 0)
            _advResult.value = if (res.ok) "✓ ${res.detail}" else "Gagal: ${res.detail}"
        }
    }

    fun unlockHz() {
        viewModelScope.launch {
            val res = advanced.actions().unlockRefreshRate()
            settings.setLockedHz(0)
            _advResult.value = if (res.ok) "✓ Kunci refresh rate dilepas." else "Gagal: ${res.detail}"
        }
    }

    fun readHz() {
        viewModelScope.launch {
            _advResult.value = "Membaca…"
            _advResult.value = advanced.actions().readRefreshRate()
        }
    }

    fun lockFps(fps: Int) {
        viewModelScope.launch {
            _advResult.value = "Menjalankan…"
            val res = advanced.actions().lockGameFps(_advTarget.value, fps)
            _advResult.value = if (res.ok)
                "✓ Target FPS ${_advTarget.value} dikunci ke $fps. Restart gamenya."
            else "Gagal: ${res.detail}"
        }
    }

    fun unlockFps() {
        viewModelScope.launch {
            val res = advanced.actions().unlockGameFps(_advTarget.value)
            _advResult.value = if (res.ok) "✓ Batas FPS dilepas. Restart gamenya."
            else "Gagal: ${res.detail}"
        }
    }

    // ---------- IZIN & PANEL ----------
    private val _overlayGranted = mutableStateOf(OverlayService.hasPermission(ctx))
    val overlayGranted: State<Boolean> = _overlayGranted

    private val _usageGranted = mutableStateOf(gameDetector.hasUsageAccess())
    val usageGranted: State<Boolean> = _usageGranted

    private val _panelRunning = mutableStateOf(false)
    val panelRunning: State<Boolean> = _panelRunning

    init {
        // Status izin bisa berubah setelah user balik dari Settings
        viewModelScope.launch {
            while (true) {
                _overlayGranted.value = OverlayService.hasPermission(ctx)
                _usageGranted.value = gameDetector.hasUsageAccess()
                delay(2_000)
            }
        }
    }

    fun requestOverlay() {
        runCatching { ctx.startActivity(PermissionIntents.overlay(ctx)) }
    }

    fun requestUsage() {
        runCatching { ctx.startActivity(PermissionIntents.usage(ctx)) }
    }

    fun startPanel() {
        if (!OverlayService.hasPermission(ctx)) return
        _panelRunning.value = true
        MonitorService.start(ctx)
        OverlayService.start(ctx, autoHide.value, panelMode(), panelSide())
        // crosshair nyala bareng panel kalau memang diaktifkan
        if (crosshair.value.enabled) CrosshairService.start(ctx, crosshair.value)
    }

    fun stopPanel() {
        OverlayService.stop(ctx)
        MonitorService.stop(ctx)
        CrosshairService.stop(ctx)
        _panelRunning.value = false
    }

    // ---------- LIVE GUARD ----------
    val streamApps: List<String> = StreamApps.installedStreamApps(ctx)
    val liveAlive: StateFlow<Boolean?> = MetricsBus.live
    val fpsHistory: StateFlow<List<Int>> = MetricsBus.fpsHistory

    private val _memory = MutableStateFlow<LiveGuard.Memory?>(null)
    val memory: StateFlow<LiveGuard.Memory?> = _memory.asStateFlow()

    private val _checks = MutableStateFlow<List<LiveGuard.Check>>(emptyList())
    val checks: StateFlow<List<LiveGuard.Check>> = _checks.asStateFlow()

    val recommendedFps: Int get() = liveGuard.recommendedGameFps()

    init {
        viewModelScope.launch {
            while (true) {
                _memory.value = liveGuard.memory()
                _checks.value = liveGuard.preflight()
                delay(5_000)
            }
        }
    }

    fun fix(check: LiveGuard.Check) {
        check.action?.let { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun startLiveGuard(pkg: String) {
        // Urutan penting: buka aplikasi live dulu (biar FGS kamera jalan saat app di depan),
        // baru amankan RAM & nyalakan penjaga.
        liveGuard.launch(pkg)
        boost.runLiveBoost(pkg)
    }

    fun stopLiveGuard() = boost.stopLiveBoost()

    // ---------- BOOST & LAUNCH ----------
    fun stabilizeAll() {
        _stabilizing.value = true
        boost.runSafeBoost()
    }

    /**
     * Alur utama: bersihkan RAM → nyalakan panel → baru buka game.
     * Panel dinyalakan SELAGI VELTRA masih di depan, karena Android 12+ melarang
     * memulai foreground service dari background.
     */
    fun launch(packageName: String) {
        boost.prepareFor(packageName)
        if (OverlayService.hasPermission(ctx) && autoStart.value) {
            MonitorService.start(ctx)
            OverlayService.start(ctx, autoHide.value, panelMode(), panelSide())
            _panelRunning.value = true
        }
        ctx.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { ctx.startActivity(it) }
    }
}
