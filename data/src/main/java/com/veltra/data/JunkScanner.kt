package com.veltra.data

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Pembersih sampah — jujur soal batasnya:
 *
 *  - Android 11+ **memblokir** akses ke `Android/data` & `Android/obb` milik aplikasi
 *    lain tanpa MANAGE_EXTERNAL_STORAGE (izin yang ditolak Play Store).
 *  - Jadi yang bisa dibersihkan tanpa akses tambahan: berkas sampah di folder publik
 *    (Download, DCIM/.thumbnails, .temp/.tmp/.log, sisa .apk, folder kosong, file besar).
 *  - Dengan Shizuku/root: `pm trim-caches` buat pangkas cache semua aplikasi.
 *
 * PERFORMA (penting buat HP kentang):
 *  - **Satu kali** jalan (dulu dua kali: cari file + cari folder kosong) — separuh biaya.
 *  - Ada **batas kedalaman** (6 folder) dan **batas waktu** (10 detik) / **batas entri**
 *    (25 ribu). Habis itu berhenti dan laporkan apa yang sudah ketemu — bukan nge-freeze.
 *  - Folder `Android/data`, `Android/obb`, `.git`, `node_modules` dilewati.
 */
data class JunkItem(
    val path: String,
    val name: String,
    val size: Long,
    val category: JunkCategory
)

enum class JunkCategory(val label: String) {
    APK("Sisa berkas pemasang (.apk)"),
    TEMP("Berkas sementara (.temp/.tmp/.log)"),
    THUMB("Cache thumbnail"),
    EMPTY("Folder kosong"),
    BIG("Berkas besar (>100 MB)"),
    DUPLICATE("Berkas duplikat")
}

data class JunkReport(
    val items: List<JunkItem>,
    val totalBytes: Long,
    val scanned: Int = 0,
    val truncated: Boolean = false
)

@Singleton
class JunkScanner @Inject constructor(
    @ApplicationContext private val ctx: Context
) {

    private val tempExt = setOf("temp", "tmp", "log", "crash", "bak", "old", "part", "download")
    private val skipDirs = setOf(
        "Android", ".git", "node_modules", ".thumbnails_nomedia", "system", "proc"
    )
    private const val MAX_DEPTH = 6
    private const val MAX_ENTRIES = 25_000
    private const val MAX_MILLIS = 10_000L
    private val bigThreshold = 100L * 1024 * 1024

    suspend fun scan(onProgress: ((Int) -> Unit)? = null): JunkReport =
        withContext(Dispatchers.IO) {
            val items = ArrayList<JunkItem>(512)
            val seen = HashSet<String>(1024)
            var count = 0
            var truncated = false
            val startAt = System.currentTimeMillis()

            for (root in publicRoots()) {
                count = walkSmart(root, items, seen, count, startAt) { n ->
                    onProgress?.invoke(n)
                }
                if (count >= MAX_ENTRIES || System.currentTimeMillis() - startAt > MAX_MILLIS) {
                    truncated = true
                    break
                }
                if (!coroutineContext.isActive) break
            }

            JunkReport(
                items = items.sortedByDescending { it.size },
                totalBytes = items.sumOf { it.size },
                scanned = count,
                truncated = truncated
            )
        }

    /** Satu kali jalan: sekalian deteksi file sampah dan folder kosong. */
    private fun walkSmart(
        root: File,
        out: MutableList<JunkItem>,
        seen: MutableSet<String>,
        startCount: Int,
        startAt: Long,
        onProgress: ((Int) -> Unit)?
    ): Int {
        var count = startCount
        val stack = ArrayDeque<Pair<File, Int>>()
        if (root.isDirectory) stack += root to 0

        while (stack.isNotEmpty()) {
            if (count >= MAX_ENTRIES || System.currentTimeMillis() - startAt > MAX_MILLIS) return count
            val (dir, depth) = stack.removeLast()
            if (depth > MAX_DEPTH) continue

            val children = runCatching { dir.listFiles() }.getOrNull()
            if (children.isNullOrEmpty()) {
                if (depth > 0 && dir != root) out += JunkItem(dir.absolutePath, dir.name, 0L, JunkCategory.EMPTY)
                continue
            }
            if (children.isEmpty() && depth > 0) {
                out += JunkItem(dir.absolutePath, dir.name, 0L, JunkCategory.EMPTY)
                continue
            }

            var hasFile = false
            for (f in children) {
                count++
                if (seen.add(f.absolutePath).not()) continue
                if (f.isDirectory) {
                    if (f.name in skipDirs) continue
                    if (f.isJunkDir()) {
                        out += JunkItem(f.absolutePath, f.name, dirSize(f), JunkCategory.THUMB)
                        continue
                    }
                    stack += f to (depth + 1)
                } else {
                    hasFile = true
                    val size = runCatching { f.length() }.getOrDefault(0L)
                    when {
                        f.extension.lowercase() == "apk" ->
                            out += JunkItem(f.absolutePath, f.name, size, JunkCategory.APK)
                        f.extension.lowercase() in tempExt ->
                            out += JunkItem(f.absolutePath, f.name, size, JunkCategory.TEMP)
                        size >= bigThreshold ->
                            out += JunkItem(f.absolutePath, f.name, size, JunkCategory.BIG)
                    }
                }
                if (count % 400 == 0) onProgress?.invoke(count)
            }
            if (!hasFile && children.all { it.isDirectory } && depth > 0) {
                // folder yang isinya cuma folder kosong — biarkan rekursi yang menilai
            }
        }
        return count
    }

    private fun dirSize(dir: File): Long = runCatching {
        var total = 0L
        dir.walkTopDown().onFail { _, _ -> }.take(2_000).forEach { if (it.isFile) total += it.length() }
        total
    }.getOrDefault(0L)

    /** Hapus item yang dipilih. Yang gagal diabaikan (biasanya batas Android 11+). */
    suspend fun clean(items: List<JunkItem>): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var ok = 0
        var freed = 0L
        for (item in items) {
            val f = File(item.path)
            runCatching {
                val size = if (f.isFile) f.length() else dirSize(f)
                if (f.isDirectory) f.deleteRecursively() else f.delete()
                if (!f.exists()) { ok++; freed += size }
            }
        }
        ok to freed
    }

    private fun File.isJunkDir(): Boolean =
        name.equals(".thumbnails", true) || name.equals("thumbnails", true) ||
                name.startsWith(".cache", true) || name.equals("cache", true) ||
                name.equals(".trash", true) || name.equals("lost.dir", true)

    private fun publicRoots(): List<File> {
        val list = mutableListOf<File>()
        @Suppress("DEPRECATION")
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let { list += it }
        @Suppress("DEPRECATION")
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.let { list += it }
        @Suppress("DEPRECATION")
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)?.let { list += it }
        @Suppress("DEPRECATION")
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)?.let { list += it }
        @Suppress("DEPRECATION")
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.let { list += it }
        ctx.getExternalFilesDir(null)?.let { list += it }
        return list.distinctBy { it.absolutePath }.filter { it.exists() && it.canRead() }
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "0 B"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}
