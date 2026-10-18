package com.veltra.boost

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deteksi game yang sedang berjalan di depan.
 *
 * Kenapa butuh UsageStats:
 *  - `ActivityManager.getRunningAppProcesses()` sejak Android 5.1 cuma ngasih proses sendiri.
 *  - `getRunningTasks()` diblokir total.
 *  - Satu-satunya cara legal tanpa root dan tanpa Accessibility Service ya UsageStatsManager.
 *
 * Play Store: pemakaian `PACKAGE_USAGE_STATS` wajib ada **prominent disclosure** di dalam app.
 * Karena itu VELTRA selalu jelaskan dulu sebelum minta izin (lihat PermissionCard).
 */
@Singleton
class GameDetector @Inject constructor(
    @ApplicationContext private val ctx: Context
) {

    /** Sudah dikasih izin "Akses Penggunaan" belum? */
    fun hasUsageAccess(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return false
        val aom = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            aom.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ctx.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            aom.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ctx.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Paket aplikasi yang sedang di depan, atau null kalau belum ada izin / tidak ketemu.
     * `end - begin = 30 detik` — cukup cepat buat deteksi, cukup hemat buat HP kentang.
     */
    fun foregroundPackage(): String? {
        if (!hasUsageAccess()) return null
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 30_000
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
            ?: return null
        return stats
            .filter { it.lastTimeUsed >= begin }
            .maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    /** Heuristik: kategori GAME, atau awalan paket yang dikenal. */
    fun isGame(pkg: String): Boolean {
        if (pkg == ctx.packageName) return false
        val appInfo = runCatching {
            ctx.packageManager.getApplicationInfo(pkg, 0)
        }.getOrNull() ?: return false
        val byCategory =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_GAME
        return byCategory || KNOWN_PREFIXES.any { pkg.startsWith(it) }
    }

    /** Apakah sekarang user sedang di dalam game? */
    fun isInGame(): Boolean {
        val pkg = foregroundPackage() ?: return false
        return isGame(pkg)
    }

    companion object {
        val KNOWN_PREFIXES = listOf(
            "com.mobile.legends", "com.tencent.ig", "com.dts.freefireth", "com.dts.freefiremax",
            "com.activision.callofduty", "com.garena", "com.supercell", "com.mojang",
            "com.riotgames", "com.epicgames", "com.ea.game", "com.nexon", "com.netmarble",
            "com.miHoYo", "com.HoYoverse", "com.levelinfinite", "com.digitalsky"
        )
    }
}
