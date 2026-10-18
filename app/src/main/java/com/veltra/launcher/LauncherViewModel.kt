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
    private val gameDetector: GameDetector
) : ViewModel() {

    val games: StateFlow<List<GameInfo>> = repo.observeGames()
        .map { list -> list.map { GameInfo(it.packageName, it.label) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _stabilizing = MutableStateFlow(false)
    val stabilizing: StateFlow<Boolean> = _stabilizing.asStateFlow()

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
        MonitorService.start(ctx)
        OverlayService.start(ctx)
        _panelRunning.value = true
    }

    fun stopPanel() {
        OverlayService.stop(ctx)
        MonitorService.stop(ctx)
        _panelRunning.value = false
    }

    // ---------- LIVE GUARD ----------
    val streamApps: List<String> = StreamApps.installedStreamApps(ctx)
    val liveAlive: StateFlow<Boolean?> = MetricsBus.live

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
        if (OverlayService.hasPermission(ctx)) {
            MonitorService.start(ctx)
            OverlayService.start(ctx)
            _panelRunning.value = true
        }
        ctx.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { ctx.startActivity(it) }
    }
}
