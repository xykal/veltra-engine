package com.veltra.boost.advanced

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lapisan "akses tambahan" VELTRA — OPSIONAL.
 *
 * ```
 * LEVEL 0  NONE     → semua fitur dasar tetap jalan (overlay, boost, live guard)
 * LEVEL 1  SHIZUKU  → bisa menjalankan perintah shell level ADB (tanpa root)
 * LEVEL 2  ROOT     → su tersedia (Magisk / temp root / KernelSU / APatch)
 * ```
 *
 * Catatan jujur:
 *  - Shizuku harus dihidupkan user via **Wireless Debugging** (Android 11+) atau ADB,
 *    dan **perlu diulang setiap reboot** kecuali perangkat di-root.
 *  - Tanpa ini pun VELTRA tetap berguna. Yang hilang cuma "fitur dalam".
 */
interface PrivilegedShell {
    enum class Level { NONE, SHIZUKU, ROOT }

    /** Level akses saat ini. */
    fun level(): Level

    /** Apakah siap dipakai (izin sudah dikasih). */
    fun isReady(): Boolean

    /** Jalankan perintah shell. Mengembalikan stdout, atau error. */
    suspend fun run(cmd: String): Result<String>

    /** Nama manusiawi buat ditampilkan di UI. */
    fun label(): String = when (level()) {
        Level.NONE -> "Tanpa akses tambahan"
        Level.SHIZUKU -> "Shizuku / ADB aktif"
        Level.ROOT -> "Root aktif"
    }
}

/** Implementasi tanpa akses — semua perintah gagal dengan pesan jelas. */
object NoShell : PrivilegedShell {
    override fun level() = PrivilegedShell.Level.NONE
    override fun isReady() = false
    override suspend fun run(cmd: String): Result<String> =
        Result.failure(IllegalStateException("Belum ada akses Shizuku atau root"))
}

/** Pakai binary `su` langsung (Magisk / KernelSU / APatch / temp root). */
object RootShell : PrivilegedShell {
    override fun level() = PrivilegedShell.Level.ROOT
    override fun isReady() = detect()

    fun detect(): Boolean = runCatching {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id -u"))
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out.trim() == "0"
    }.getOrDefault(false)

    override suspend fun run(cmd: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            val code = p.waitFor()
            if (code == 0) out.trim() else throw IllegalStateException(err.ifBlank { "exit $code" })
        }
    }
}

/**
 * Pakai Shizuku (tanpa root, level ADB).
 * Butuh dependensi:
 *   implementation("dev.rikka.shizuku:api:13.1.5")
 *   implementation("dev.rikka.shizuku:provider:13.1.5")
 *
 * Kelas ini memakai refleksi supaya modul tetap bisa di-compile
 * walaupun dependensi Shizuku belum ditambahkan.
 */
class ShizukuShell(private val ctx: Context) : PrivilegedShell {

    private val api = runCatching {
        Class.forName("rikka.shizuku.Shizuku")
    }.getOrNull()

    override fun level() = if (isReady()) PrivilegedShell.Level.SHIZUKU
    else PrivilegedShell.Level.NONE

    override fun isReady(): Boolean {
        val cls = api ?: return false
        return runCatching {
            val ping = cls.getMethod("pingBinder").invoke(null) as Boolean
            if (!ping) return false
            val granted = cls.getMethod("checkPermission").invoke(null) as Int
            granted == 0 // PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    override suspend fun run(cmd: String): Result<String> = withContext(Dispatchers.IO) {
        val cls = api ?: return@withContext Result.failure(
            IllegalStateException("Shizuku belum terpasang")
        )
        if (!isReady()) return@withContext Result.failure(
            IllegalStateException("Izin Shizuku belum diberikan")
        )
        runCatching {
            val newProcess = cls.getMethod(
                "newProcess", Array<String>::class.java, Map::class.java, String::class.java
            )
            @Suppress("UNCHECKED_CAST")
            val process = newProcess.invoke(
                null, arrayOf("sh", "-c", cmd), null, null
            ) as Process
            val out = process.inputStream.bufferedReader().readText()
            val err = process.errorStream.bufferedReader().readText()
            val code = process.waitFor()
            if (code == 0) out.trim() else throw IllegalStateException(err.ifBlank { "exit $code" })
        }
    }
}

/** Pilih shell terbaik yang tersedia: root > shizuku > none. */
class ShellProvider(private val ctx: Context) {

    fun current(): PrivilegedShell = when {
        RootShell.detect() -> RootShell
        ShizukuShell(ctx).isReady() -> ShizukuShell(ctx)
        else -> NoShell
    }
}
