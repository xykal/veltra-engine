package com.veltra.data

import android.content.Context
import android.os.Build
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pembersih sampah — jujur soal batasnya:
 *
 *  - Android 11+ **memblokir** akses ke `Android/data` & `Android/obb` dari aplikasi lain
 *    tanpa MANAGE_EXTERNAL_STORAGE (izin yang ditolak Play Store untuk kasus ini).
 *  - Jadi tanpa akses tambahan yang bisa dibersihkan: file sampah di folder publik
 *    (Download, DCIM/.thumbnails, .temp/.tmp/.log, sisa .apk, folder kosong, file besar).
 *  - Dengan Shizuku/root: `pm trim-caches` buat cache semua aplikasi.
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
    val totalBytes: Long
)

@Singleton
class JunkScanner @Inject constructor(
    @ApplicationContext private val ctx: Context
) {

    private val tempExt = listOf("temp", "tmp", "log", "crash", "bak", "old")
    private val bigThreshold = 100L * 1024 * 1024

    suspend fun scan(): JunkReport = withContext(Dispatchers.IO) {
        val items = mutableListOf<JunkItem>()
        val roots = publicRoots()

        roots.forEach { root ->
            walk(root) { file ->
                when {
                    file.isDirectory && file.isJunkDir() ->
                        items += JunkItem(file.absolutePath, file.name, 0L, JunkCategory.THUMB)
                    file.isFile && file.extension.lowercase() == "apk" ->
                        items += JunkItem(file.absolutePath, file.name, file.length(), JunkCategory.APK)
                    file.isFile && file.extension.lowercase() in tempExt ->
                        items += JunkItem(file.absolutePath, file.name, file.length(), JunkCategory.TEMP)
                    file.isFile && file.length() >= bigThreshold ->
                        items += JunkItem(file.absolutePath, file.name, file.length(), JunkCategory.BIG)
                }
            }
        }

        // folder kosong
        roots.forEach { root ->
            root.walkTopDown().filter { it.isDirectory && it.listFiles().isNullOrEmpty() }
                .forEach { items += JunkItem(it.absolutePath, it.name, 0L, JunkCategory.EMPTY) }
        }

        JunkReport(items.distinctBy { it.path }, items.sumOf { it.size })
    }

    /** Hapus item yang dipilih. Yang gagal dilaporkan (biasanya karena batas Android 11+). */
    suspend fun clean(items: List<JunkItem>): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var ok = 0
        var freed = 0L
        items.forEach { item ->
            val f = File(item.path)
            runCatching {
                val size = if (f.isFile) f.length() else 0L
                if (f.isDirectory) f.deleteRecursively() else f.delete()
                if (!f.exists()) { ok++; freed += size }
            }
        }
        ok to freed
    }

    private fun File.isJunkDir(): Boolean =
        name.equals(".thumbnails", true) || name.equals("thumbnails", true) ||
                name.startsWith(".cache", true) || name.equals("cache", true)

    private fun publicRoots(): List<File> {
        val list = mutableListOf<File>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            Environment.getExternalStorageDirectory()?.let { list += it }
        } else {
            ctx.getExternalFilesDirs(null).mapNotNull { it?.parentFile?.parentFile }
                .forEach { list += it } // /storage/emulated/0/Android/data/../..
        }
        @Suppress("DEPRECATION")
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?.let { list += it }
        @Suppress("DEPRECATION")
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            ?.let { list += it }
        @Suppress("DEPRECATION")
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            ?.let { list += it }
        ctx.getExternalFilesDir(null)?.let { list += it }
        return list.distinctBy { it.absolutePath }.filter { it.exists() && it.canRead() }
    }

    private fun walk(root: File, visit: (File) -> Unit) {
        runCatching {
            root.walkTopDown()
                .onFail { _, _ -> } // abaikan folder yang ditolak sistem
                .forEach { visit(it) }
        }
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "0 B"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}
