package com.veltra.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

data class GameEntity(val packageName: String, val label: String)

/**
 * Deteksi game ter-install — ALL DEVICE, ALL GAMES.
 * Pakai category GAME + fallback heuristic (known launchers / opengl activity).
 */
@Singleton
class GameRepository @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val pm: PackageManager get() = ctx.packageManager

    fun observeGames(): Flow<List<GameEntity>> = flow {
        while (true) {
            emit(scan())
            delay(10_000)
        }
    }

    fun scan(): List<GameEntity> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val apps: List<android.content.pm.ResolveInfo> =
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }

        return apps.mapNotNull { ri ->
            val pkg = ri.activityInfo.packageName
            val appInfo = runCatching {
                pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
            }.getOrNull() ?: return@mapNotNull null

            val isGame = if (android.os.Build.VERSION.SDK_INT >= 26)
                appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_GAME
            else false

            if (isGame || KNOWN_GAMES.any { pkg.startsWith(it) }) {
                GameEntity(pkg, pm.getApplicationLabel(appInfo).toString())
            } else null
        }.distinctBy { it.packageName }
    }

    /** Launcher populer yang wajib di-whitelist (jangan di-kill). */
    val protectedPackages = setOf(
        "com.android.launcher", "com.google.android.launcher",
        "com.miui.home", "com.sec.android.app.launcher",
        "com.huawei.android.launcher", "com.oppo.launcher",
        "com.vivo.launcher", "com.oneplus.launcher"
    )

    companion object {
        val KNOWN_GAMES = listOf(
            "com.mobile.legends", "com.tencent.ig", "com.dts.freefireth",
            "com.activision.callofduty.shooter", "com.supercell.clashofclans",
            "com.mojang.minecraftpe", "com.garena.game.kgvn", "com.riotgames.league.wildrift"
        )
    }
}
