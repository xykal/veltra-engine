package com.veltra.launcher.qs

import com.veltra.R

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.veltra.boost.BoostEngine
import com.veltra.boost.profile.GameProfile
import com.veltra.boost.profile.ProfileStore
import com.veltra.overlay.PanelState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Ubin Quick Settings #2 — STABILKAN.
 *
 * Sekali ketuk: bersihkan proses cache yang aman, lalu kirim hint performa ke
 * scheduler lewat ADPF. Tidak menyalakan panel, tidak butuh izin overlay.
 *
 * Kalau game yang terakhir dibuka punya profil sendiri, level pembersihannya
 * mengikuti profil itu (HEMAT / SEIMBANG / MAKSIMAL).
 */
@AndroidEntryPoint
class VeltraBoostTileService : TileService() {

    @Inject lateinit var boost: BoostEngine
    @Inject lateinit var profiles: ProfileStore

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = getString(R.string.tile_boost_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = getString(R.string.tile_boost_hint)
            }
            updateTile()
        }
    }

    override fun onClick() {
        val mode = lastBoostMode()
        runCatching { boost.runSafeBoost(mode) }

        // Kedip sebentar biar user tahu ketukannya masuk.
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = getString(R.string.tile_boost_done)
            }
            updateTile()
        }
        mainHandler.postDelayed({
            qsTile?.apply { state = Tile.STATE_INACTIVE; updateTile() }
        }, 1_200)
    }

    /**
     * Level pembersihan: kalau ada game terakhir yang tercatat dan game itu punya
     * profil, pakai level dari profilnya. Kalau tidak, SEIMBANG.
     */
    private fun lastBoostMode(): BoostEngine.Mode {
        val pkg = PanelState.lastProfilePkg ?: return BoostEngine.Mode.SAFE
        val p: GameProfile = runCatching {
            runBlocking(Dispatchers.IO) { profiles.profiles.first()[pkg] }
        }.getOrNull() ?: return BoostEngine.Mode.SAFE

        return when (p.boost) {
            GameProfile.BoostLevel.HEMAT -> BoostEngine.Mode.HEMAT
            GameProfile.BoostLevel.SEIMBANG -> BoostEngine.Mode.SAFE
            GameProfile.BoostLevel.MAKSIMAL -> BoostEngine.Mode.AGGRESSIVE
        }
    }
}
