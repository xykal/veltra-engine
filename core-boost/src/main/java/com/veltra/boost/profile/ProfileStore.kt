package com.veltra.boost.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.veltraProfiles: DataStore<Preferences> by preferencesDataStore("veltra_profiles")

/**
 * Tempat simpan profil per-game.
 *
 * Sengaja ditaruh di modul `core-boost` (bukan di modul app) karena dua tempat
 * perlu membacanya:
 *   1. Layar PROFIL GAME di aplikasi.
 *   2. `OverlayService` — supaya profil bisa diterapkan otomatis saat game
 *      muncul di depan, tanpa harus membuka VELTRA dulu.
 *
 * Tidak ada batasan jumlah profil, dan semuanya tersimpan lokal di perangkat.
 */
@Singleton
class ProfileStore @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private object Keys {
        val PROFILES = stringSetPreferencesKey("profiles")
    }

    /** Semua profil yang pernah disimpan, terindeks package name. */
    val profiles: Flow<Map<String, GameProfile>> = ctx.veltraProfiles.data.map { p ->
        (p[Keys.PROFILES] ?: emptySet())
            .mapNotNull { GameProfile.decode(it) }
            .associateBy { it.pkg }
    }

    suspend fun save(profile: GameProfile) {
        ctx.veltraProfiles.edit { p ->
            val set = (p[Keys.PROFILES] ?: emptySet())
                .filter { it.substringBefore('|') != profile.pkg }
                .toMutableSet()
            set.add(GameProfile.encode(profile))
            p[Keys.PROFILES] = set
        }
    }

    suspend fun remove(pkg: String) {
        ctx.veltraProfiles.edit { p ->
            p[Keys.PROFILES] = (p[Keys.PROFILES] ?: emptySet())
                .filter { it.substringBefore('|') != pkg }.toSet()
        }
    }
}
