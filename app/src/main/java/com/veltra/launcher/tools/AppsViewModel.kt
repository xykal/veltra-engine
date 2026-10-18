package com.veltra.launcher.tools

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veltra.data.AppEntry
import com.veltra.data.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppFilter { GAMES, USER, SYSTEM, ALL }

data class AppsUiState(
    val loading: Boolean = true,
    val all: List<AppEntry> = emptyList(),
    val filter: AppFilter = AppFilter.GAMES,
    val query: String = "",
    val note: String = ""
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    app: Application,
    private val repo: AppRepository
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(AppsUiState())
    val state: StateFlow<AppsUiState> = _state

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)
        val list = runCatching { repo.all() }.getOrDefault(emptyList())
        _state.value = _state.value.copy(
            loading = false,
            all = list,
            note = if (list.isEmpty()) "Tidak ada aplikasi terbaca. Izinkan Akses Penggunaan untuk detail ukuran."
            else "${list.size} aplikasi terpasang"
        )
    }

    fun filter(f: AppFilter) { _state.value = _state.value.copy(filter = f) }
    fun query(q: String) { _state.value = _state.value.copy(query = q) }

    fun visible(): List<AppEntry> {
        val s = _state.value
        val base = when (s.filter) {
            AppFilter.GAMES -> s.all.filter { it.isGame }
            AppFilter.USER -> s.all.filter { !it.isSystem }
            AppFilter.SYSTEM -> s.all.filter { it.isSystem }
            AppFilter.ALL -> s.all
        }
        return if (s.query.isBlank()) base
        else base.filter { it.label.contains(s.query, true) || it.packageName.contains(s.query, true) }
    }

    fun icon(pkg: String) = repo.icon(pkg)

    fun launch(pkg: String): Boolean {
        val intent = getApplication<Application>().packageManager.getLaunchIntentForPackage(pkg)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { getApplication<Application>().startActivity(intent) }
        return true
    }

    fun openDetails(pkg: String) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { getApplication<Application>().startActivity(i) }
    }

    fun extract(entry: AppEntry) = viewModelScope.launch {
        val out = repo.extractApk(entry)
        _state.value = _state.value.copy(
            note = out?.let { "APK disimpan: ${it.absolutePath}" }
                ?: "Gagal menyalin APK (izin penyimpanan ditolak)."
        )
    }
}
