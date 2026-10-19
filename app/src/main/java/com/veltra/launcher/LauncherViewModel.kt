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
    private val advanced: com.veltra.boost.advanced.AdvancedManager,
    private val profiles: com.veltra.boost.profile.ProfileStore
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
        settings.panelMode.stateIn(viewModelScope, SharingStarted.Eagerly, "SIDEBAR")
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
            .getOrDefault(com.veltra.overlay.PanelMode.SIDEBAR)

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
        // teruskan ke window overlay (panel dalam game)
        com.veltra.overlay.PanelState.crosshair = c
        com.veltra.overlay.PanelState.crosshairOn = c.enabled
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
    private val _dndOn = MutableStateFlow(false)
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

    fun unprotectLive() {
        viewModelScope.launch {
            val pkg = streamApps.firstOrNull()
            if (pkg == null) {
                _advResult.value = "Aplikasi live tidak ditemukan."
                return@launch
            }
            val res = advanced.actions().unprotectLive(pkg)
            _advResult.value = if (res.ok) "✓ Proteksi dilepas." else "Gagal: ${res.detail}"
        }
    }

    fun readTemp() {
        viewModelScope.launch {
            val t = advanced.actions().batteryTempC()
            _advResult.value = t?.let { "Suhu baterai: ${"%.1f".format(it)}°C" }
                ?: "Suhu tidak terbaca di perangkat ini."
        }
    }

    fun trimCaches() {
        viewModelScope.launch {
            val res = advanced.actions().trimCaches(1024L * 1024 * 1024)
            _advResult.value = if (res.ok) "✓ ${res.detail}" else "Gagal: ${res.detail}"
        }
    }

    /** Persingkat animasi sistem — efek paling kerasa bikin HP terasa "enteng". */
    fun fastAnimations() {
        viewModelScope.launch {
            val res = advanced.actions().setAnimationScale(0.5f)
            _advResult.value = if (res.ok) "✓ Skala animasi 0.5x." else "Gagal: ${res.detail}"
        }
    }

    fun toggleDnd() {
        viewModelScope.launch {
            _dndOn.value = !_dndOn.value
            val res = advanced.actions().dnd(_dndOn.value)
            _advResult.value = if (res.ok) "✓ Mode senyap ${if (_dndOn.value) "aktif" else "mati"}."
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

    /**
     * PERFORMA: dua loop pemantau di bawah cuma jalan kalau aplikasi lagi di
     * depan. Dulu jalan terus walau layar mati → makan CPU & RAM di HP kentang.
     */
    private val foreground = MutableStateFlow(false)
    fun setForeground(v: Boolean) { foreground.value = v }

    init {
        // baca status depan/belakang dari bus (murah: cuma baca volatile)
        viewModelScope.launch {
            while (true) {
                foreground.value = ForegroundBus.foreground
                delay(700)
            }
        }
    }

    init {
        // Status izin bisa berubah setelah user balik dari Settings
        viewModelScope.launch {
            while (true) {
                if (foreground.value) {
                    _overlayGranted.value = OverlayService.hasPermission(ctx)
                    _usageGranted.value = gameDetector.hasUsageAccess()
                }
                delay(if (foreground.value) 2_000 else 10_000)
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
        com.veltra.overlay.PanelState.lastAutoHide = autoHide.value
        com.veltra.overlay.PanelState.lastMode = panelMode()
        com.veltra.overlay.PanelState.lastSide = panelSide()
        // sinkronkan keadaan panel (crosshair/FPS/senyap) sebelum window dibuat
        com.veltra.overlay.PanelState.crosshair = crosshair.value
        com.veltra.overlay.PanelState.crosshairOn = crosshair.value.enabled
        OverlayService.start(ctx, autoHide.value, panelMode(), panelSide())
        // crosshair nyala bareng panel kalau memang diaktifkan
        if (crosshair.value.enabled) CrosshairService.start(ctx, crosshair.value)
        com.veltra.launcher.qs.VeltraTileService.requestRefresh(ctx)
    }

    fun stopPanel() {
        OverlayService.stopAll(ctx)
        CrosshairService.stop(ctx)
        _panelRunning.value = false
        com.veltra.launcher.qs.VeltraTileService.requestRefresh(ctx)
    }

    // ---------- LIVE GUARD ----------
    val streamApps: List<String> = StreamApps.installedStreamApps(ctx)
    val liveAlive: StateFlow<Boolean?> = MetricsBus.live

    /** Aplikasi live yang terdeteksi otomatis (null = nggak ada siaran berjalan). */
    val detectedLive: StateFlow<String?> =
        com.veltra.launcher.live.LiveDetectService.LiveState.livePackage
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun openNotificationAccess() {
        runCatching {
            ctx.startActivity(
                com.veltra.launcher.live.LiveDetectService.settingsIntent()
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    val fpsHistory: StateFlow<List<Int>> = MetricsBus.fpsHistory

    private val _memory = MutableStateFlow<LiveGuard.Memory?>(null)
    val memory: StateFlow<LiveGuard.Memory?> = _memory.asStateFlow()

    private val _checks = MutableStateFlow<List<LiveGuard.Check>>(emptyList())
    val checks: StateFlow<List<LiveGuard.Check>> = _checks.asStateFlow()

    val recommendedFps: Int get() = liveGuard.recommendedGameFps()

    init {
        viewModelScope.launch {
            while (true) {
                if (foreground.value) {
                    _memory.value = liveGuard.memory()
                    _checks.value = liveGuard.preflight()
                }
                delay(if (foreground.value) 5_000 else 15_000)
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
    /**
     * Buka game lewat layar animasi VELTRA (GameLaunchActivity).
     * Di situlah RAM dibersihkan, panel & crosshair dinyalakan, baru game dibuka —
     * jadi pengguna lihat urutannya, bukan cuma nunggu layar hitam.
     */
    fun launch(packageName: String) {
        // 1. bersihkan RAM & nyalakan panel dulu (harus saat VELTRA masih di depan)
        launchDirectPreparation(packageName)
        // 2. baru tampilkan animasi, dan game dibuka di akhir animasi
        val i = Intent(ctx, GameLaunchActivity::class.java)
            .putExtra(GameLaunchActivity.EXTRA_PKG, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(i) }
            .onFailure { launchDirect(packageName) }
    }

    // ---------- PROFIL PER-GAME ----------
    val gameProfiles: StateFlow<Map<String, com.veltra.boost.profile.GameProfile>> =
        profiles.profiles.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun saveProfile(p: com.veltra.boost.profile.GameProfile) {
        viewModelScope.launch { profiles.save(p) }
    }

    fun removeProfile(pkg: String) {
        viewModelScope.launch { profiles.remove(pkg) }
    }

    /**
     * Terapkan profil sebuah game SEBELUM game dibuka.
     * Sebagian besar juga diterapkan OverlayService saat game muncul di depan —
     * ini cuma memastikan hint FPS & mode senyap sudah benar sejak detik pertama.
     */
    private fun applyProfile(packageName: String) {
        val p = gameProfiles.value[packageName] ?: return
        if (!p.isCustom) return

        com.veltra.overlay.PanelState.targetFps = p.targetFps
        com.veltra.boost.HintManager.setTargetFps(p.targetFps)

        when (p.boost) {
            com.veltra.boost.profile.GameProfile.BoostLevel.HEMAT ->
                boost.runSafeBoost(com.veltra.boost.BoostEngine.Mode.HEMAT)
            com.veltra.boost.profile.GameProfile.BoostLevel.SEIMBANG ->
                boost.runSafeBoost(com.veltra.boost.BoostEngine.Mode.SAFE)
            com.veltra.boost.profile.GameProfile.BoostLevel.MAKSIMAL ->
                boost.runSafeBoost(com.veltra.boost.BoostEngine.Mode.AGGRESSIVE)
        }

        if (p.fpsLock > 0 || p.hzLock > 0) {
            viewModelScope.launch {
                runCatching {
                    if (p.hzLock > 0) advanced.actions().lockRefreshRate(p.hzLock)
                    if (p.fpsLock > 0) advanced.actions().lockGameFps(packageName, p.fpsLock)
                }
            }
        }
    }

    private fun launchDirectPreparation(packageName: String) {
        applyProfile(packageName)
        boost.prepareFor(packageName)
        if (OverlayService.hasPermission(ctx) && autoStart.value) {
            MonitorService.start(ctx)
            com.veltra.overlay.PanelState.crosshair = crosshair.value
            // simpan gaya panel terakhir → ubin Quick Settings bisa pakai gaya yang sama
            com.veltra.overlay.PanelState.lastAutoHide = autoHide.value
            com.veltra.overlay.PanelState.lastMode = panelMode()
            com.veltra.overlay.PanelState.lastSide = panelSide()
            OverlayService.start(ctx, autoHide.value, panelMode(), panelSide())
            _panelRunning.value = true
        }
    }

    /** Buka game langsung tanpa animasi (dipakai dari layar animasi & cadangan). */
    fun launchDirect(packageName: String) {
        boost.prepareFor(packageName)
        if (OverlayService.hasPermission(ctx) && autoStart.value) {
            MonitorService.start(ctx)
            com.veltra.overlay.PanelState.crosshair = crosshair.value
            OverlayService.start(ctx, autoHide.value, panelMode(), panelSide())
            _panelRunning.value = true
        }
        ctx.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { ctx.startActivity(it) }
    }
}
