package com.veltra.boost.live

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.veltra.boost.GameGuard
import javax.inject.Singleton

/**
 * LIVE GUARD — alasan fitur ini ada:
 *
 * "HP kentang, live TikTok mati sendiri pas main game."
 *
 * Yang SEBENARNYA terjadi:
 *   1. Game makan RAM besar → Android bunuh proses background (LMK) → TikTok mati → live putus.
 *   2. ROM China (MIUI/ColorOS/Funtouch) punya "battery saver" yang jauh lebih ganas dari
 *      Android standar — TikTok dibunuh walau RAM masih longgar.
 *   3. Encoder video (720p/30) + game 60fps rebutan hardware encoder → overheat → throttle.
 *
 * Yang BISA VELTRA lakukan:
 *   ✅ Bebaskan RAM SEBELUM live dimulai (bukan saat live jalan)
 *   ✅ Tidak pernah membunuh TikTok — whitelist permanen
 *   ✅ Pantau nyawa TikTok tiap 5 detik, kasih peringatan kalau mati
 *   ✅ Turunkan target FPS game (ADPF hint) biar encoder live kebagian jatah
 *   ✅ Tuntun user setting OEM yang bikin TikTok dibunuh
 *
 * Yang TIDAK BISA VELTRA lakukan (dan tidak ada app pihak ketiga yang bisa):
 *   ❌ Mencekal Low Memory Killer sistem
 *   ❌ Memaksa split-screen / floating window
 *   ❌ Menghidupkan lagi live yang sudah putus (live putus = tamat, harus mulai baru)
 */
@Singleton
class LiveGuard @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    /** Paket yang dilindungi saat Live Mode aktif. */
    fun protectedPackages(): Set<String> =
        StreamApps.ALL.toSet() + GameGuard.PROTECTED

    // ---------- MEMORY ----------

    data class Memory(
        val totalMb: Long,
        val availMb: Long,
        val isLow: Boolean,
        val thresholdMb: Long
    )

    fun memory(): Memory {
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val totalMb = mi.totalMem / (1024 * 1024)
        val availMb = mi.availMem / (1024 * 1024)
        val thresholdMb = mi.threshold / (1024 * 1024)
        // Aman kalau masih punya headroom ≥ 350MB di atas threshold sistem
        return Memory(totalMb, availMb, availMb < mi.threshold / (1024 * 1024) + 350, thresholdMb)
    }

    /** Ramah buat HP kentang: 30fps itu cukup, TikTok live juga mentok 30fps. */
    fun recommendedGameFps(): Int = when {
        memory().totalMb <= 2_048 -> 24   // HP 2GB: fatal kalau maksa 60
        memory().totalMb <= 3_072 -> 30
        memory().totalMb <= 4_096 -> 40
        else -> 60
    }

    // ---------- PREFLIGHT ----------

    data class Check(val id: String, val ok: Boolean, val title: String, val action: Intent? = null)

    fun preflight(): List<Check> {
        val m = memory()
        return listOf(
            Check(
                "ram", m.availMb >= 900,
                "RAM bebas ${m.availMb}MB / ${m.totalMb}MB" +
                        if (m.availMb < 900) " — kurang, live bakal putus" else " — aman",
                action = null
            ),
            Check(
                "battery", isIgnoringBatteryOptimizations(),
                "Baterai: ${if (isIgnoringBatteryOptimizations()) "tanpa batasan" else "MASIH dibatasi sistem"}",
                action = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${ctx.packageName}".toUri()
                }
            ),
            Check(
                "streamapp", StreamApps.installedStreamApps(ctx).isNotEmpty(),
                "Aplikasi live: ${StreamApps.installedStreamApps(ctx)
                    .joinToString { StreamApps.label(it) }.ifEmpty { "tidak ada" }}"
            )
        )
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    // ---------- PROSES ----------

    /** Best-effort cek apakah proses aplikasi live masih hidup. */
    fun isAlive(pkg: String): Boolean = runCatching {
        // getRunningAppProcesses() dibatasi sejak Android 5.1 untuk aplikasi pihak ketiga,
        // jadi pindai /proc — world-readable dan tidak butuh permission.
        java.io.File("/proc").listFiles()?.forEach { dir ->
            if (!dir.isDirectory) return@forEach
            val pid = dir.name.toIntOrNull() ?: return@forEach
            runCatching {
                val cmd = java.io.File(dir, "cmdline").readText()
                if (cmd.contains(pkg)) return true
            }
        }
        false
    }.getOrDefault(false)

    fun launch(pkg: String) {
        ctx.packageManager.getLaunchIntentForPackage(pkg)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            ?.let { ctx.startActivity(it) }
    }

    fun openAppSettings() {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
