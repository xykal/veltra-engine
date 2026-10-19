package com.veltra.boost

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import com.veltra.boost.live.LiveGuard
import com.veltra.boost.live.LiveGuardProvider
import com.veltra.boost.live.LiveWatchdog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inti VELTRA.
 *
 * Prinsip jujur: aplikasi pihak ketiga TIDAK bisa overclock hardware.
 * Yang VELTRA lakukan adalah menghilangkan gangguan:
 *   1. Bebaskan RAM (kill proses cached, launcher & sistem dilindungi)
 *   2. Minta mode PERFORMANCE via GameMode API (Android 13+)
 *   3. Kasih hint ke scheduler lewat ADPF PerformanceHint (Android 12+)
 *   4. Pantau thermal + FPS real-time
 *
 * Anti-banned: TIDAK ada hook, injeksi, modifikasi memori, atau sentuhan file game.
 */
@Singleton
class BoostEngine @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val liveGuard: LiveGuard
) {
    private val am: ActivityManager =
        ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    init { LiveGuardProvider.bind(liveGuard) }

    enum class Mode { HEMAT, SAFE, AGGRESSIVE }

    /**
     * Bersihkan gangguan, lalu minta performa ke sistem.
     *
     * - HEMAT     : tidak membunuh proses apa pun (cocok buat HP 2GB atau lagi live)
     * - SAFE      : satu putaran pembersihan proses cache
     * - AGGRESSIVE: dua putaran (proses cache generasi kedua ikut kebuang)
     */
    fun runSafeBoost(mode: Mode = Mode.SAFE) {
        if (mode != Mode.HEMAT) {
            trimMemory()
            if (mode == Mode.AGGRESSIVE) trimMemory()
        }
        requestPerformanceHint()
    }

    /** Mode LIVE: bebasin RAM, tapi aplikasi live + FPS target diturunkan. */
    fun runLiveBoost(streamPkg: String) {
        trimMemory()
        HintManager.setTargetFps(liveGuard.recommendedGameFps())
        requestPerformanceHint()
        LiveWatchdog.start(ctx, streamPkg)
    }

    fun stopLiveBoost() {
        LiveWatchdog.stop(ctx)
        HintManager.setTargetFps(60)
    }

    fun prepareFor(packageName: String) {
        runSafeBoost()
        applyGameMode(packageName)
    }

    /** 1. Bebaskan RAM — hanya proses CACHED, tidak pernah sentuh foreground/launcher. */
    private fun trimMemory() {
        runCatching {
            val protected = GameGuard.PROTECTED + ctx.packageName
            am.runningAppProcesses
                .filter { it.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED }
                .flatMap { it.pkgList.toList() }
                .filter { it !in protected }
                .forEach { pkg -> runCatching { am.killBackgroundProcesses(pkg) } }
        }
    }

    /**
     * 2. GameMode API (Android 13+).
     * Catatan jujur: Android 35 TIDAK mengekspos setGameMode() ke aplikasi pihak ketiga —
     * yang tersedia publik hanya getGameMode() dan setGameState().
     * Jadi kita laporkan state "sedang loading game yang tidak boleh diganggu",
     * dan di situ sistem (yang mendukung) akan menaikkan performa sendiri.
     */
    private fun applyGameMode(packageName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        try {
            val gm = ctx.getSystemService(Context.GAME_SERVICE) as? android.app.GameManager
            gm?.setGameState(
                android.app.GameState(
                    true, // isLoading: minta sistem jangan ganggu proses loading
                    android.app.GameState.MODE_GAMEPLAY_UNINTERRUPTIBLE
                )
            )
            // Baca mode aktif — dipakai buat statistik & panel
            val mode = gm?.getGameMode()
            if (mode == android.app.GameManager.GAME_MODE_STANDARD) {
                // Sistem belum kasih mode performa — tidak bisa dipaksa dari pihak ketiga.
            }
        } catch (_: Throwable) { /* ROM tertentu tidak punya GameManager */ }
    }

    /** 3. ADPF PerformanceHint (Android 12+) — hint ke scheduler saat frame kritis. */
    private fun requestPerformanceHint() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching {
            HintManager.acquire(ctx)
        }
    }

    fun isThermalThrottled(): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
    }
}

/** Paket yang TIDAK boleh dibunuh: launcher tiap brand + sistem inti. */
object GameGuard {
    val PROTECTED = setOf(
        "com.android.launcher", "com.google.android.launcher", "com.android.systemui",
        "com.miui.home", "com.miui.securitycenter",
        "com.sec.android.app.launcher", "com.samsung.android.game.gamehome",
        "com.huawei.android.launcher", "com.oppo.launcher", "com.coloros.gamespace",
        "com.vivo.launcher", "com.oneplus.launcher", "com.transsion.updater"
    )

    fun isLauncher(ctx: Context, pkg: String): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val ri = ctx.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        ri?.activityInfo?.packageName == pkg
    }.getOrDefault(false)
}
