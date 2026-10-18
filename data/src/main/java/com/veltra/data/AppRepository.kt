package com.veltra.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.os.storage.StorageManager
import android.os.storage.StorageStatsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
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
    val appSize: Long,      // APK + data (butuh izin Akses Penggunaan)
    val cacheSize: Long,
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

    /** Ikon asli aplikasi (Drawable) — dipanggil di UI. */
    fun icon(pkg: String): Drawable? = runCatching {
        pm.getApplicationIcon(pkg)
    }.getOrNull()

    suspend fun all(): List<AppEntry> = withContext(Dispatchers.IO) {
        val flags = PackageManager.GET_META_DATA
        @Suppress("DEPRECATION")
        val installed = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            pm.getInstalledPackages(flags)
        }
        installed.mapNotNull { pi ->
            val ai = pi.applicationInfo ?: return@mapNotNull null
            val sizes = querySizes(pi.packageName)
            AppEntry(
                packageName = pi.packageName,
                label = pm.getApplicationLabel(ai).toString(),
                versionName = pi.versionName,
                versionCode = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode
                else @Suppress("DEPRECATION") pi.versionCode.toLong(),
                isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isGame = if (Build.VERSION.SDK_INT >= 26)
                    ai.category == ApplicationInfo.CATEGORY_GAME else false,
                apkPath = ai.sourceDir,
                apkSize = runCatching { File(ai.sourceDir).length() }.getOrDefault(0L),
                appSize = sizes.first,
                cacheSize = sizes.second,
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
        }.sortedByDescending { it.appSize.coerceAtLeast(it.apkSize) }
    }

    /** Ukuran app + cache. Butuh PACKAGE_USAGE_STATS; tanpa itu hanya ukuran APK. */
    private fun querySizes(pkg: String): Pair<Long, Long> {
        return runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@runCatching 0L to 0L
            val ssm = ctx.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val user: UserHandle = android.os.Process.myUserHandle()
            val uuid: UUID = (ctx.getSystemService(Context.STORAGE_SERVICE) as StorageManager)
                .getUuidForPath(File(ctx.filesDir.absolutePath)) ?: StorageManager.UUID_DEFAULT
            val stats = ssm.queryStatsForPackage(uuid, pkg, user)
            stats.appBytes + stats.dataBytes to stats.cacheBytes
        }.getOrDefault(0L to 0L)
    }

    /** Salin APK ke folder Download publik (APK extractor / backup). */
    suspend fun extractApk(entry: AppEntry): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                ), "VELTRA/APK"
            ).apply { mkdirs() }
            val out = File(dir, "${entry.label.replace(Regex("[^A-Za-z0-9._-]"), "_")}-" +
                    "${entry.versionName ?: entry.versionCode}.apk")
            File(entry.apkPath).inputStream().use { inp -> out.outputStream().use { inp.copyTo(it) } }
            out
        }.getOrNull()
    }
}
