package com.veltra.launcher.tools

import android.app.Application
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veltra.data.AppEntry
import com.veltra.data.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class AppFilter { GAMES, USER, SYSTEM, ALL }

data class AppsUiState(
    val loading: Boolean = true,
    val all: List<AppEntry> = emptyList(),
    val sizes: Map<String, Pair<Long, Long>> = emptyMap(),
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

    private var sizeJob: Job? = null

    init { load() }

    /**
     * PERFORMA: daftar aplikasi dimuat dulu (cepat), ukuran menyusul bertahap
     * per 20 aplikasi. Dulu semua ukuran dihitung sekaligus di awal → ratusan
     * panggilan IPC ke sistem dan layar membeku beberapa detik.
     */
    fun load() = viewModelScope.launch {
        _state.value = AppsUiState(loading = true, filter = _state.value.filter)
        val list = runCatching { repo.list() }.getOrDefault(emptyList())
        _state.value = _state.value.copy(
            loading = false,
            all = list,
            note = if (list.isEmpty()) "Tidak ada aplikasi terbaca."
            else "${list.size} aplikasi terpasang"
        )
        loadSizesProgressive(list.map { it.packageName })
    }

    private fun loadSizesProgressive(pkgs: List<String>) {
        sizeJob?.cancel()
        sizeJob = viewModelScope.launch {
            val acc = HashMap<String, Pair<Long, Long>>()
            pkgs.chunked(20).forEach { chunk ->
                if (!isActive) return@launch
                val part = runCatching { repo.sizesFor(chunk) }.getOrDefault(emptyMap())
                acc.putAll(part)
                _state.value = _state.value.copy(sizes = acc.toMap())
            }
            if (acc.isEmpty()) {
                _state.value = _state.value.copy(
                    note = "Ukuran detail butuh izin Akses Penggunaan (yang ditampilkan ukuran APK)."
                )
            }
        }
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

    /** Ikon dimuat di background + di-cache — tidak pernah di main thread. */
    suspend fun icon(pkg: String): Drawable? = withContext(Dispatchers.Default) { repo.icon(pkg) }

    fun launch(pkg: String): Boolean {
        val intent = getApplication<Application>().packageManager.getLaunchIntentForPackage(pkg)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { getApplication<Application>().startActivity(intent) }
        return true
    }

    fun openDetails(pkg: String) {
        val i = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$pkg")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
