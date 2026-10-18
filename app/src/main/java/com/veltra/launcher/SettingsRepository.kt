package com.veltra.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.veltra.overlay.crosshair.CrosshairConfig
import com.veltra.overlay.crosshair.CrosshairStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.veltraSettings: DataStore<Preferences> by preferencesDataStore("veltra_settings")

/** Preferensi VELTRA — disimpan lokal, tidak pernah dikirim ke mana pun. */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private object Keys {
        val AUTO_HIDE = booleanPreferencesKey("auto_hide")
        val AUTO_START = booleanPreferencesKey("auto_start")
        val PANEL_MODE = stringPreferencesKey("panel_mode")
        val PANEL_SIDE = stringPreferencesKey("panel_side")

        // crosshair
        val CH_ENABLED = booleanPreferencesKey("ch_enabled")
        val CH_STYLE = stringPreferencesKey("ch_style")
        val CH_SIZE = intPreferencesKey("ch_size")
        val CH_THICK = intPreferencesKey("ch_thick")
        val CH_GAP = intPreferencesKey("ch_gap")
        val CH_COLOR = longPreferencesKey("ch_color")
        val CH_ALPHA = floatPreferencesKey("ch_alpha")
        val CH_OX = intPreferencesKey("ch_ox")
        val CH_OY = intPreferencesKey("ch_oy")
        val CH_ONLY_GAME = booleanPreferencesKey("ch_only_game")

        // catatan sensitivitas per game: "pkg=isi;pkg2=isi2"
        val SENS = stringPreferencesKey("sens_profile")
        val HZ_LOCK = intPreferencesKey("hz_lock")
    }

    // ---------- PANEL ----------
    val autoHide: Flow<Boolean> = ctx.veltraSettings.data.map { it[Keys.AUTO_HIDE] ?: true }
    val autoStart: Flow<Boolean> = ctx.veltraSettings.data.map { it[Keys.AUTO_START] ?: true }

    suspend fun setAutoHide(v: Boolean) { ctx.veltraSettings.edit { it[Keys.AUTO_HIDE] = v } }
    suspend fun setAutoStart(v: Boolean) { ctx.veltraSettings.edit { it[Keys.AUTO_START] = v } }

    val panelMode: Flow<String> = ctx.veltraSettings.data.map { it[Keys.PANEL_MODE] ?: "HUD" }
    val panelSide: Flow<String> = ctx.veltraSettings.data.map { it[Keys.PANEL_SIDE] ?: "RIGHT" }

    suspend fun setPanelMode(v: String) { ctx.veltraSettings.edit { it[Keys.PANEL_MODE] = v } }
    suspend fun setPanelSide(v: String) { ctx.veltraSettings.edit { it[Keys.PANEL_SIDE] = v } }

    // ---------- CROSSHAIR ----------
    val crosshair: Flow<CrosshairConfig> = ctx.veltraSettings.data.map { p ->
        CrosshairConfig(
            enabled = p[Keys.CH_ENABLED] ?: false,
            style = runCatching { CrosshairStyle.valueOf(p[Keys.CH_STYLE] ?: "") }
                .getOrDefault(CrosshairStyle.CROSS_DOT),
            sizeDp = p[Keys.CH_SIZE] ?: 24,
            thicknessDp = p[Keys.CH_THICK] ?: 2,
            gapDp = p[Keys.CH_GAP] ?: 5,
            colorArgb = p[Keys.CH_COLOR] ?: 0xFF00E6A0,
            alpha = p[Keys.CH_ALPHA] ?: 0.92f,
            offsetXDp = p[Keys.CH_OX] ?: 0,
            offsetYDp = p[Keys.CH_OY] ?: 0,
            onlyInGame = p[Keys.CH_ONLY_GAME] ?: true
        )
    }

    suspend fun setCrosshair(c: CrosshairConfig) {
        ctx.veltraSettings.edit { p ->
            p[Keys.CH_ENABLED] = c.enabled
            p[Keys.CH_STYLE] = c.style.name
            p[Keys.CH_SIZE] = c.sizeDp
            p[Keys.CH_THICK] = c.thicknessDp
            p[Keys.CH_GAP] = c.gapDp
            p[Keys.CH_COLOR] = c.colorArgb
            p[Keys.CH_ALPHA] = c.alpha
            p[Keys.CH_OX] = c.offsetXDp
            p[Keys.CH_OY] = c.offsetYDp
            p[Keys.CH_ONLY_GAME] = c.onlyInGame
        }
    }

    // ---------- CATATAN SENSITIVITAS PER GAME ----------
    val sensitivity: Flow<Map<String, String>> = ctx.veltraSettings.data.map { p ->
        (p[Keys.SENS] ?: "").split(";")
            .mapNotNull { row ->
                val i = row.indexOf('=')
                if (i <= 0 || i == row.length - 1) null
                else row.substring(0, i) to row.substring(i + 1)
            }.toMap()
    }

    suspend fun setSensitivity(pkg: String, note: String) {
        ctx.veltraSettings.edit { p ->
            val map = (p[Keys.SENS] ?: "").split(";").mapNotNull { row ->
                val i = row.indexOf('=')
                if (i <= 0) null else row.substring(0, i) to row.substring(i + 1)
            }.toMutableMap()
            map[pkg] = note
            p[Keys.SENS] = map.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    /** Refresh rate terakhir yang dikunci (0 = lepas). */
    val lockedHz: Flow<Int> = ctx.veltraSettings.data.map { it[Keys.HZ_LOCK] ?: 0 }

    suspend fun setLockedHz(hz: Int) {
        ctx.veltraSettings.edit { it[Keys.HZ_LOCK] = hz }
    }
}
