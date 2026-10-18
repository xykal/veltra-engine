package com.veltra.launcher.tools

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veltra.boost.advanced.AdvancedManager
import com.veltra.data.JunkItem
import com.veltra.data.JunkScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JunkUiState(
    val scanning: Boolean = false,
    val cleaning: Boolean = false,
    val items: List<JunkItem> = emptyList(),
    val selected: Set<String> = emptySet(),
    val totalBytes: Long = 0L,
    val freedBytes: Long = 0L,
    val note: String = ""
)

@HiltViewModel
class JunkViewModel @Inject constructor(
    app: Application,
    private val scanner: JunkScanner,
    private val advanced: AdvancedManager
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(JunkUiState())
    val state: StateFlow<JunkUiState> = _state

    init { scan() }

    fun scan() = viewModelScope.launch {
        _state.value = JunkUiState(scanning = true, freedBytes = _state.value.freedBytes)
        val report = runCatching { scanner.scan() }.getOrNull()
        _state.value = _state.value.copy(
            scanning = false,
            items = report?.items.orEmpty(),
            totalBytes = report?.totalBytes ?: 0L,
            selected = emptySet(),
            note = if (report?.items.isNullOrEmpty())
                "Tidak ada sampah yang bisa dibersihkan tanpa akses tambahan. " +
                "Android 11+ memblokir folder Android/data aplikasi lain."
            else "Pilih yang mau dibuang, satu-satu atau semua."
        )
    }

    fun toggle(path: String) {
        val s = _state.value.selected.toMutableSet()
        if (!s.add(path)) s.remove(path)
        _state.value = _state.value.copy(selected = s)
    }

    fun selectAll(on: Boolean) {
        _state.value = _state.value.copy(
            selected = if (on) _state.value.items.map { it.path }.toSet() else emptySet()
        )
    }

    fun clean() = viewModelScope.launch {
        val picked = _state.value.items.filter { it.path in _state.value.selected }
        if (picked.isEmpty()) return@launch
        _state.value = _state.value.copy(cleaning = true)
        val (ok, freed) = scanner.clean(picked)
        var note = "$ok dari ${picked.size} item dibuang."
        // Dengan root/Shizuku kita bisa sekalian pangkas cache semua aplikasi.
        if (advanced.level() != com.veltra.boost.advanced.PrivilegedShell.Level.NONE) {
            advanced.actions().trimCaches(1024L * 1024 * 1024)
            note += " Cache aplikasi juga dipangkas lewat ${advanced.label()}."
        }
        _state.value = _state.value.copy(
            cleaning = false,
            freedBytes = _state.value.freedBytes + freed,
            selected = emptySet(),
            note = note
        )
        scan()
    }
}
