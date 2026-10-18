package com.veltra.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.veltraSettings: DataStore<Preferences> by preferencesDataStore("veltra_settings")

/** Preferensi panel — disimpan lokal, tidak pernah dikirim ke mana pun. */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private object Keys {
        val AUTO_HIDE = booleanPreferencesKey("auto_hide")
        val AUTO_START = booleanPreferencesKey("auto_start")
    }

    val autoHide: Flow<Boolean> = ctx.veltraSettings.data.map { it[Keys.AUTO_HIDE] ?: true }
    val autoStart: Flow<Boolean> = ctx.veltraSettings.data.map { it[Keys.AUTO_START] ?: true }

    suspend fun setAutoHide(v: Boolean) {
        ctx.veltraSettings.edit { it[Keys.AUTO_HIDE] = v }
    }

    suspend fun setAutoStart(v: Boolean) {
        ctx.veltraSettings.edit { it[Keys.AUTO_START] = v }
    }
}
