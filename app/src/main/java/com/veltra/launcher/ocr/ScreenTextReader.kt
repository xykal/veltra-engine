package com.veltra.launcher.ocr

import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Teks yang berhasil dikenali dari satu layar / satu berkas gambar. */
data class ReadResult(
    val fullText: String,
    val lines: List<String>,
    val blocks: Int,
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val codes: List<String> = emptyList(),  // kemungkinan OTP / kode
    val source: ReadSource
)

enum class ReadSource { SCREEN, FILE }

/**
 * OCR di perangkat (ML Kit via Layanan Google Play, **100% offline setelah model
 * terpasang** — tidak ada gambar atau teks yang dikirim ke server mana pun).
 *
 * Kenapa modelnya tidak dibawa di dalam APK: model OCR berukuran belasan MB.
 * Versi "terbundel" bikin APK VELTRA jadi 44 MB. Dengan versi ini APK tetap
 * ~2 MB dan modelnya diunduh sekali saat instal lewat Layanan Google Play.
 * Konsekuensinya: HP tanpa Layanan Google Play tidak bisa pakai fitur ini.
 *
 * Bitmap-nya diproses di memori dan tidak pernah disimpan ke galeri.
 */
@Singleton
class ScreenTextReader @Inject constructor() {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /** Model tersedia di perangkat ini? (butuh Layanan Google Play) */
    fun available(): Boolean = runCatching {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        true
    }.getOrDefault(false)

    suspend fun read(bitmap: Bitmap, source: ReadSource): ReadResult = withContext(Dispatchers.IO) {
        val vision: Text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        ReadResult(
            fullText = vision.text,
            lines = vision.textBlocks.flatMap { it.lines }.map { it.text },
            blocks = vision.textBlocks.size,
            phones = vision.text.pick(PHONE),
            emails = vision.text.pick(EMAIL),
            links = vision.text.pick(LINK),
            codes = vision.text.pick(CODE),
            source = source
        )
    }

    suspend fun read(uri: Uri, resolver: (Uri) -> Bitmap?, source: ReadSource): ReadResult? =
        withContext(Dispatchers.IO) {
            val bmp = resolver(uri) ?: return@withContext null
            read(bmp, source)
        }

    fun close() = runCatching { recognizer.close() }.let { }

    private fun String.pick(regex: Regex): List<String> =
        regex.findAll(this).map { it.value.trim() }.distinct().take(10).toList()

    companion object {
        val PHONE = Regex("""(?:\+\d{1,3}[\s-]?)?(?:\(\d{2,4}\)[\s-]?)?\d{3,4}[\s-]?\d{3,4}(?:[\s-]?\d{2,4})?""")
        val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
        val LINK = Regex("""(https?://[^\s]+|www\.[^\s]+)""")
        val CODE = Regex("""\b\d{4,8}\b""")
    }
}
