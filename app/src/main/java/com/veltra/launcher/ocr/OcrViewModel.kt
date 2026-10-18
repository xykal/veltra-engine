package com.veltra.launcher.ocr

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OcrPhase { IDLE, WAITING_PERMISSION, CAPTURING, READING, DONE, EMPTY, ERROR }

data class OcrUiState(
    val phase: OcrPhase = OcrPhase.IDLE,
    val message: String = "",
    val bitmap: Bitmap? = null,
    val result: ReadResult? = null,
    val selected: Set<String> = emptySet()
)

@HiltViewModel
class OcrViewModel @Inject constructor(
    app: Application,
    private val reader: ScreenTextReader
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(OcrUiState())
    val state: StateFlow<OcrUiState> = _state

    private var watch: Job? = null
    /** Hanya baca frame berikutnya kalau memang diminta (hemat, jangan OCR tiap frame). */
    private var armed = false

    fun permissionIntent(): Intent =
        (getApplication<Application>().getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager).createScreenCaptureIntent()

    fun onPermissionResult(granted: Boolean, data: Intent?) {
        if (!granted || data == null) {
            _state.value = _state.value.copy(
                phase = OcrPhase.ERROR,
                message = "Izin tangkap layar ditolak sistem. VELTRA tidak bisa membaca layar tanpanya."
            )
            return
        }
        _state.value = OcrUiState(phase = OcrPhase.CAPTURING, message = "Menunggu tangkapan layar…")
        armed = true
        val ctx = getApplication<Application>()
        val i = OcrCaptureService.intent(
            ctx, OcrCaptureService.ACTION_START,
            rc = android.app.Activity.RESULT_OK, data = data
        )
        if (android.os.Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        watchFrames()
    }

    /** Setiap frame baru langsung dibaca (tanpa disimpan ke disk). */
    private fun watchFrames() {
        watch?.cancel()
        watch = viewModelScope.launch {
            OcrCaptureService.lastBitmap.collectLatest { bmp ->
                if (bmp != null && armed) {
                    armed = false
                    read(bmp)
                }
            }
        }
    }

    fun read(bmp: Bitmap) = viewModelScope.launch {
        _state.value = _state.value.copy(phase = OcrPhase.READING, bitmap = bmp, message = "")
        delay(120) // kasih waktu render
        val res = runCatching { reader.read(bmp, ReadSource.SCREEN) }.getOrNull()
        _state.value = if (res == null) {
            _state.value.copy(phase = OcrPhase.ERROR, message = "Gagal membaca. Coba lagi.")
        } else if (res.fullText.isBlank()) {
            _state.value.copy(
                phase = OcrPhase.EMPTY, result = res,
                message = "Layar tidak terbaca — mungkin layar diamankan (aplikasi bank/rahasia) atau tidak ada teks."
            )
        } else {
            _state.value.copy(phase = OcrPhase.DONE, result = res)
        }
    }

    /** Ambil ulang layar. Kalau sesi masih hidup tinggal ambil frame baru. */
    fun captureAgain(): Boolean {
        if (!OcrCaptureService.running) return false
        armed = true
        _state.value = _state.value.copy(phase = OcrPhase.CAPTURING, message = "Menunggu tangkapan layar…")
        val ctx = getApplication<Application>()
        ctx.startService(OcrCaptureService.intent(ctx, OcrCaptureService.ACTION_CAPTURE))
        return true
    }

    /** Baca ulang gambar terakhir tanpa menangkap layar lagi. */
    fun refresh() { _state.value.bitmap?.let { read(it) } }

    /** Dari galeri — jalur cadangan kalau sistem menolak tangkap layar. */
    fun readUri(uri: Uri) = viewModelScope.launch {
        _state.value = _state.value.copy(phase = OcrPhase.READING, message = "")
        val bmp = runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                val src = android.graphics.ImageDecoder.createSource(
                    getApplication<Application>().contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(src) { d, _, _ -> d.allocator =
                    android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE }
            } else @Suppress("DEPRECATION") {
                MediaStore.Images.Media.getBitmap(getApplication<Application>().contentResolver, uri)
            }
        }.getOrNull()
        if (bmp == null) {
            _state.value = _state.value.copy(phase = OcrPhase.ERROR, message = "Gambar tidak bisa dibuka.")
            return@launch
        }
        val res = runCatching { reader.read(bmp, ReadSource.FILE) }.getOrNull()
        _state.value = _state.value.copy(
            phase = if (res?.fullText.isNullOrBlank()) OcrPhase.EMPTY else OcrPhase.DONE,
            bitmap = bmp, result = res,
            message = if (res?.fullText.isNullOrBlank()) "Teks tidak terdeteksi di gambar ini." else ""
        )
    }

    fun toggle(line: String) {
        val s = _state.value.selected.toMutableSet()
        if (!s.add(line)) s.remove(line)
        _state.value = _state.value.copy(selected = s)
    }

    fun textToUse(): String {
        val st = _state.value
        return if (st.selected.isNotEmpty()) st.selected.joinToString("\n")
        else st.result?.fullText.orEmpty()
    }

    fun copy() {
        val txt = textToUse()
        if (txt.isBlank()) return
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Veltra", txt))
    }

    fun shareIntent(): Intent? {
        val txt = textToUse()
        if (txt.isBlank()) return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, txt)
        }.let { Intent.createChooser(it, "Bagikan teks") }
    }

    fun reset() { OcrCaptureService.clear(); _state.value = OcrUiState() }

    override fun onCleared() {
        runCatching {
            val ctx = getApplication<Application>()
            ctx.startService(OcrCaptureService.intent(ctx, OcrCaptureService.ACTION_STOP))
        }
        super.onCleared()
    }
}
