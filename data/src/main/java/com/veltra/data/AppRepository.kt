package com.veltra.data

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Environment as OsEnv
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Semua aplikasi yang terpasang — lengkap dengan ikon asli, versi, ukuran,
 * tanggal instal/update, dan jalur APK-nya (buat ekstrak/backup).
 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystem: Boolean,
    val isGame: Boolean,
    val apkPath: String,
    val apkSize: Long,
    val appSize: Long = 0,      // APK + data (butuh izin Akses Penggunaan)
    val cacheSize: Long = 0,
    val firstInstall: Long,
    val lastUpdate: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val installer: String?
)

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val pm get() = ctx.packageManager

    /** Cache ikon — biar scroll daftar aplikasi nggak decode ulang terus-terusan. */
    private val iconCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, Drawable?>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Drawable?>?): Boolean =
                size > 180
        }
    )

    /**
     * Ikon asli aplikasi. Dipanggil dari background (Dispatchers.Default) —
     * dulu ini jalan di main thread dan bikin daftar aplikasi ngelag parah.
     */
    suspend fun icon(pkg: String): Drawable? = withContext(Dispatchers.Default) {
        iconCache[pkg] ?: runCatching {
            pm.getApplicationIcon(pkg).also { iconCache[pkg] = it }
        }.getOrNull()
    }

    fun canReadSizes(): Boolean = runCatching {
        val am = ctx.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        if (Build.VERSION.SDK_INT >= 30) {
            am.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), ctx.packageName
            ) == android.app.AppOpsManager.MODE_ALLOWED
        } else @Suppress("DEPRECATION") {
            am.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), ctx.packageName
            ) == android.app.AppOpsManager.MODE_ALLOWED
        }
    }.getOrDefault(false)

    /**
     * Daftar aplikasi — TANPA ukuran (cepat). Ukuran dimuat terpisah & bertahap
     * lewat [sizesFor], karena tiap aplikasi butuh satu panggilan IPC ke sistem.
     */
    suspend fun list(): List<AppEntry> = withContext(Dispatchers.IO) {
        val flags = PackageManager.GET_META_DATA
        @Suppress("DEPRECATION")
        val installed = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            pm.getInstalledPackages(flags)
        }
        installed.mapNotNull { pi ->
            val ai = pi.applicationInfo ?: return@mapNotNull null
            AppEntry(
                packageName = pi.packageName,
                label = runCatching { pm.getApplicationLabel(ai).toString() }
                    .getOrDefault(pi.packageName),
                versionName = pi.versionName,
                versionCode = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode
                else @Suppress("DEPRECATION") pi.versionCode.toLong(),
                isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isGame = if (Build.VERSION.SDK_INT >= 26)
                    ai.category == ApplicationInfo.CATEGORY_GAME
                else (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0,
                apkPath = ai.sourceDir,
                apkSize = runCatching { File(ai.sourceDir).length() }.getOrDefault(0L),
                firstInstall = pi.firstInstallTime,
                lastUpdate = pi.lastUpdateTime,
                minSdk = if (Build.VERSION.SDK_INT >= 24) ai.minSdkVersion else 0,
                targetSdk = ai.targetSdkVersion,
                installer = if (Build.VERSION.SDK_INT >= 30)
                    runCatching { pm.getInstallSourceInfo(pi.packageName).installingPackageName }
                        .getOrNull()
                else @Suppress("DEPRECATION") runCatching { pm.getInstallerPackageName(pi.packageName) }
                    .getOrNull()
            )
        }.sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
    }

    /**
     * Ukuran aplikasi (APK + data + cache). Berat: satu IPC per aplikasi,
     * jadi dipanggil bertahap per kelompok kecil, bukan sekaligus.
     */
    suspend fun sizesFor(pkgs: List<String>): Map<String, Pair<Long, Long>> =
        withContext(Dispatchers.IO) {
            if (!canReadSizes()) return@withContext emptyMap()
            val ssm = ctx.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val uuid: UUID = runCatching {
                (ctx.getSystemService(Context.STORAGE_SERVICE) as StorageManager)
                    .getUuidForPath(File(ctx.filesDir.absolutePath))
            }.getOrNull() ?: StorageManager.UUID_DEFAULT
            val user = Process.myUserHandle()
            val out = HashMap<String, Pair<Long, Long>>(pkgs.size)
            for (pkg in pkgs) {
                val s = runCatching {
                    val st = ssm.queryStatsForPackage(uuid, pkg, user)
                    (st.appBytes + st.dataBytes) to st.cacheBytes
                }.getOrNull() ?: (0L to 0L)
                out[pkg] = s
            }
            out
        }

    /** Salin APK ke folder Download publik (APK extractor / backup). */
    suspend fun extractApk(entry: AppEntry): File? = withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            val base = OsEnv.getExternalStoragePublicDirectory(OsEnv.DIRECTORY_DOWNLOADS)
            val dir = File(base, "VELTRA/APK").apply { mkdirs() }
            val out = File(
                dir,
                "${entry.label.replace(Regex("[^A-Za-z0-9._-]"), "_")}-" +
                        "${entry.versionName ?: entry.versionCode}.apk"
            )
            File(entry.apkPath).inputStream().use { inp -> out.outputStream().use { inp.copyTo(it) } }
            out
        }.getOrNull()
    }
}
