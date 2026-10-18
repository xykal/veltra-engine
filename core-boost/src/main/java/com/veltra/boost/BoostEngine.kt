package com.veltra.boost

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
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

    enum class Mode { SAFE, AGGRESSIVE }

    fun runSafeBoost(mode: Mode = Mode.SAFE) {
        trimMemory()
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

    /** 2. GameMode API (Android 13+) — minta sistem pakai mode performa. */
    private fun applyGameMode(packageName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        runCatching {
            val gm = ctx.getSystemService(Context.GAME_SERVICE) as? android.app.GameManager
            gm?.setGameMode(packageName, android.app.GameManager.GAME_MODE_PERFORMANCE)
        }
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
