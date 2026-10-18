package com.veltra.launcher

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veltra.boost.BoostEngine
import com.veltra.boost.MetricsBus
import com.veltra.boost.live.LiveGuard
import com.veltra.boost.live.StreamApps
import com.veltra.data.GameRepository
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
    private val liveGuard: LiveGuard
) : ViewModel() {

    val games: StateFlow<List<GameInfo>> = repo.observeGames()
        .map { list -> list.map { GameInfo(it.packageName, it.label) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _stabilizing = MutableStateFlow(false)
    val stabilizing: StateFlow<Boolean> = _stabilizing.asStateFlow()

    // ---- LIVE GUARD ----
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
        liveGuard.launch(pkg)      // buka TikTok dulu — biar FGS kamera jalan saat app masih depan
        boost.runLiveBoost(pkg)    // baru amankan: RAM dibersihkan, FPS game diturunkan, watchdog jalan
    }

    fun stopLiveGuard() = boost.stopLiveBoost()

    // ---- BOOST & LAUNCH ----
    fun stabilizeAll() {
        _stabilizing.value = true
        boost.runSafeBoost()
    }

    fun launch(packageName: String) {
        boost.prepareFor(packageName)
        ctx.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { ctx.startActivity(it) }
    }
}
