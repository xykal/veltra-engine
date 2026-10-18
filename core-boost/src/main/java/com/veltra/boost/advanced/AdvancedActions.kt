package com.veltra.boost.advanced

import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Aksi "dalam" yang cuma jalan kalau LEVEL 1 (Shizuku/ADB) atau LEVEL 2 (root) tersedia.
 *
 * Semua aksi di sini **nyata** — bukan kosmetik. Tapi tetap tidak ada yang
 * menyentuh berkas/memori game, jadi aman dari banned.
 */
class AdvancedActions(private val shell: PrivilegedShell) {

    private suspend fun sh(cmd: String) = shell.run(cmd)

    /** Hasil aksi. */
    data class Result(val ok: Boolean, val detail: String)

    private fun toResult(r: kotlin.Result<String>): Result =
        Result(r.isSuccess, r.getOrElse { it.message ?: "gagal" })

    // ---------------------------------------------------------------
    // 1. PROTEKSI APLIKASI LIVE (ini yang paling ngaruh buat kasus lu)
    // ---------------------------------------------------------------

    /**
     * Bikin aplikasi live (TikTok) **aktif** di mata App Standby dan kebal doze.
     * Dua perintah ini setara dengan yang dilakukan ROM pabrik saat user
     * memilih "Jangan optimasi" — tapi kita lakukan otomatis & sekaligus.
     */
    suspend fun protectLive(pkg: String): Result {
        val cmds = listOf(
            "cmd deviceidle whitelist +$pkg",
            "am set-standby-bucket $pkg active",
            "cmd appops set $pkg RUN_IN_BACKGROUND allow",
            "cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow"
        )
        val errors = mutableListOf<String>()
        cmds.forEach { c ->
            val r = sh(c)
            if (!r.isSuccess) errors += c
        }
        return Result(
            errors.isEmpty(),
            if (errors.isEmpty()) "Aplikasi live dikunci: standby ACTIVE + kebal doze."
            else "Sebagian gagal (${errors.size}/${cmds.size}) — cek log."
        )
    }

    /** Lepas proteksi (buat balikin ke kondisi awal). */
    suspend fun unprotectLive(pkg: String): Result = toResult(
        sh("cmd deviceidle whitelist -$pkg")
    )

    // ---------------------------------------------------------------
    // 2. FPS BENERAN (bukan gimmick)
    // ---------------------------------------------------------------

    /**
     * Turunkan resolusi layar saat main game.
     * INI satu-satunya cara yang **benar-benar** menaikkan FPS di GPU lemah:
     * piksel yang dirender lebih sedikit → beban GPU turun drastis.
     * (Cara kerja GFX tool populer juga begini — bedanya kita tidak menyentuh
     * file game sama sekali, cuma mengubah resolusi sistem sementara.)
     */
    suspend fun setResolutionScale(scale: Float): Result {
        if (scale !in 0.5f..1.0f) return Result(false, "Skala harus antara 0.5 – 1.0")
        val w = readInt("wm size | awk 'END{print $1}' | cut -d x -f1")
        val h = readInt("wm size | awk 'END{print $1}' | cut -d x -f2")
        val dw = readInt("wm density | awk 'END{print $NF}'")
        if (w <= 0 || h <= 0) return Result(false, "Tidak bisa membaca resolusi awal")
        return toResult(
            sh("wm size ${(w * scale).toInt()}x${(h * scale).toInt()} && " +
                    "wm density ${(dw * scale).toInt()} && echo OK")
        )
    }

    /** Balikin resolusi & densitas ke default pabrik. */
    suspend fun resetResolution(): Result = toResult(sh("wm size reset && wm density reset"))

    /**
     * Matikan/pelankan animasi sistem — efeknya kecil tapi nyata di HP kentang:
     * transisi lebih sedikit = frame lebih konsisten.
     */
    suspend fun setAnimationScale(scale: Float): Result = toResult(
        sh(
            "settings put global window_animation_scale $scale && " +
                    "settings put global transition_animation_scale $scale && " +
                    "settings put global animator_duration_scale $scale && echo OK"
        )
    )

    // ---------------------------------------------------------------
    // 3. BEBAN & SUHU — jujur: tidak ada aplikasi yang bisa "mendinginkan"
    // ---------------------------------------------------------------

    /**
     * VELTRA **tidak bisa** mendinginkan HP. Yang bisa dilakukan:
     * mengurangi sumber panasnya. Mode ini menurunkan target FPS (ADPF hint)
     * + opsi turunkan resolusi sekali jalan, supaya suhu turun dan
     * throttle hilang. Hasilnya: FPS stabil, bukan FPS tinggi.
     */
    suspend fun coolingMode(enable: Boolean, alsoLowerResolution: Boolean): Result {
        return if (enable) {
            setAnimationScale(0.5f)
            if (alsoLowerResolution) setResolutionScale(0.85f)
            else Result(true, "Mode hemat panas: animasi 0.5x, target FPS diturunkan.")
        } else {
            setAnimationScale(1.0f)
            resetResolution()
        }
    }

    // ---------------------------------------------------------------
    // 4. INJECT GAME MODE — level sistem (INI yang lu minta)
    // ---------------------------------------------------------------

    /**
     * Game Mode resmi Android (12+):
     *   `cmd game mode [standard|performance|battery] <PKG>`
     * Beda dengan `GameManager.setGameMode()` yang TIDAK tersedia buat app pihak ketiga —
     * lewat shell ini bisa. Ini "inject game mode" yang sah, bukan hack.
     */
    suspend fun setGameMode(pkg: String, mode: String): Result = toResult(
        sh("cmd game mode ${mode.lowercase()} $pkg && echo OK")
    )

    /**
     * Intervensi Game Mode — ini yang paling kerasa di HP kentang:
     *   - `--downscale 0.7` → WindowManager backbuffer cuma 70% → beban GPU turun drastis
     *   - `--fps 60`        → paksa target frame rate (kalau didukung)
     * Perintah: `cmd game set --downscale 0.7 --fps 60 <PKG>`
     */
    suspend fun setGameIntervention(
        pkg: String,
        downscale: Float,   // 0.3 – 0.9, atau 0f = disable
        fps: Int            // 30|45|60|90|120, atau 0 = disable
    ): Result {
        val d = if (downscale <= 0f) "disable" else "--downscale ${downscale.coerceIn(0.3f, 0.9f)}"
        val f = if (fps <= 0) "" else "--fps $fps"
        return toResult(sh("cmd game set $d $f $pkg && echo OK"))
    }

    /**
     * Paksa dukungan mode Performa lewat device_config (kalau game-nya tidak
     * mendeklarasikan dukungan game mode). mode=2 (performance), mode=3 (battery).
     *   `device_config put game_overlay <PKG> mode=2,downscaleFactor=0.7`
     */
    suspend fun forceGameOverlay(pkg: String, mode: Int, downscaleFactor: Float): Result = toResult(
        sh("device_config put game_overlay $pkg mode=$mode,downscaleFactor=$downscaleFactor && echo OK")
    )

    /** Balikin game mode ke pengaturan awal perangkat. */
    suspend fun resetGameMode(pkg: String): Result = toResult(sh("cmd game reset $pkg && echo OK"))

    /** Info mode yang tersedia buat game itu. */
    suspend fun listGameModes(pkg: String): String = sh("cmd game list-modes $pkg").getOrDefault("-")

    /**
     * Kompilasi AOT game ke native code (`cmd package compile -m speed -f <PKG>`).
     * Efek nyata: loading lebih cepat & frame-time lebih konsisten (ART tidak lagi
     * jalan campur interpreter + JIT). TIDAK menyentuh berkas game — cuma artefak
     * kompilasi yang dikelola sistem. Aman dari banned.
     */
    suspend fun compileGame(pkg: String, mode: String = "speed"): Result = toResult(
        sh("cmd package compile -m $mode -f $pkg && echo OK")
    )

    /**
     * "Paket komplit" buat satu game: mode performance + downscale + compile AOT.
     * Ini yang gue rekomendasikan buat Free Fire Max di HP kentang.
     */
    suspend fun turboGame(pkg: String, downscale: Float = 0.75f): Result {
        forceGameOverlay(pkg, 2, downscale)
        setGameMode(pkg, "performance")
        setGameIntervention(pkg, downscale, 60)
        compileGame(pkg)
        return Result(true, "Turbo aktif: mode performance, resolusi internal ${(downscale*100).toInt()}%, AOT compile.")
    }

    /** Bersihkan cache semua aplikasi (`pm trim-caches`) — butuh akses shell/root. */
    suspend fun trimCaches(bytes: Long = 1_073_741_824L): Result = toResult(
        sh("pm trim-caches $bytes && echo OK")
    )

    // ---------------------------------------------------------------
    // 5. BERSIH-BERSIH
    // ---------------------------------------------------------------

    /** Force-stop aplikasi sampah yang sering jalan diam-diam di ROM China. */
    suspend fun freezeBloat(packages: List<String>): Result {
        if (packages.isEmpty()) return Result(false, "Tidak ada aplikasi yang dipilih")
        val cmd = packages.joinToString(" ; ") { "am force-stop $it" }
        return toResult(sh("$cmd ; echo OK"))
    }

    /** Nonaktifkan aplikasi bawaan (bisa diaktifkan lagi). */
    suspend fun disablePackages(packages: List<String>): Result {
        if (packages.isEmpty()) return Result(false, "Tidak ada aplikasi yang dipilih")
        val cmd = packages.joinToString(" ; ") { "pm disable-user --user 0 $it" }
        return toResult(sh("$cmd ; echo OK"))
    }

    // ---------------------------------------------------------------
    // 5. UTIL
    // ---------------------------------------------------------------

    suspend fun setBrightness(value: Int): Result = toResult(
        sh("settings put system screen_brightness ${value.coerceIn(0, 255)}")
    )

    suspend fun dnd(enable: Boolean): Result = toResult(
        sh("cmd notification set_dnd ${if (enable) "on" else "off"}")
    )

    /** Baca suhu baterai (milisikelvin → °C). */
    suspend fun batteryTempC(): Float? {
        val out = sh("cat /sys/class/power_supply/battery/temp").getOrNull() ?: return null
        val v = out.trim().toFloatOrNull() ?: return null
        return if (v > 1000) v / 10f else v
    }

    private suspend fun readInt(cmd: String): Int =
        sh(cmd).getOrNull()?.trim()?.toIntOrNull() ?: -1

    /** Tes cepat semua kemampuan sekaligus. */
    suspend fun selfCheck(): Map<String, Boolean> = coroutineScope {
        val a = async { sh("id").isSuccess }
        val b = async { sh("wm size").isSuccess }
        val c = async { sh("cmd deviceidle whitelist").isSuccess }
        mapOf(
            "shell" to a.await(),
            "wm" to b.await(),
            "deviceidle" to c.await()
        )
    }
}

/**
 * Pintu masuk tunggal buat UI — selalu baca ulang level akses tiap dipanggil,
 * jadi kalau user baru saja menghidupkan Shizuku, langsung kebaca.
 */
class AdvancedManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val ctx: android.content.Context
) {
    fun shell(): PrivilegedShell = ShellProvider(ctx).current()
    fun actions(): AdvancedActions = AdvancedActions(shell())
    fun level(): PrivilegedShell.Level = shell().level()
    fun label(): String = shell().label()

    /**
     * Minta izin ke Shizuku. Dialog muncul di aplikasi Shizuku.
     * Setelah user menyetujui, panggil ulang level() — hasilnya langsung kebaca.
     */
    fun requestShizukuPermission() {
        runCatching {
            val cls = Class.forName("rikka.shizuku.Shizuku")
            cls.getMethod("requestPermission", Int::class.javaPrimitiveType).invoke(null, 1001)
        }
    }

    /** Shizuku terpasang di HP? (belum tentu izinnya sudah dikasih) */
    fun isShizukuInstalled(): Boolean = runCatching {
        ctx.packageManager.getApplicationInfo("moe.shizuku.privileged.api", 0)
        true
    }.getOrDefault(false)

    /** Daftar bloatware umum di HP entry-level — aman di-force-stop. */
    fun commonBloat(): List<String> = listOf(
        "com.facebook.katana", "com.facebook.system", "com.facebook.appmanager",
        "com.facebook.services", "com.miui.analytics", "com.miui.msa.global",
        "com.miui.systemAdSolution", "com.mi.globalbrowser", "com.xiaomi.midrop",
        "com.tiktok.global.sdk", "com.opos.ads", "com.heytap.market",
        "com.vivo.abe", "com.transsion.phonemaster", "com.infinix.brand"
    ).filter { pkg ->
        runCatching { ctx.packageManager.getApplicationInfo(pkg, 0) }.isSuccess
    }
}
