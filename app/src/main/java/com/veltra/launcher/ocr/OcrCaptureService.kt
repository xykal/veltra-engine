package com.veltra.launcher.ocr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * "Ambil teks dari layar" — semua diproses di perangkat, tanpa unggah apa pun.
 *
 * Cara kerjanya (dan ini satu-satunya cara yang didukung Android):
 *  1. Sistem memunculkan dialog "Mulai merekam atau mentransmisikan?" → pengguna
 *     harus menyetujui sendiri. VELTRA tidak bisa menyelinap.
 *  2. Selama sesi aktif ada **ikon siaran** di status bar — jadi aktivitas ini
 *     selalu terlihat, bukan kegiatan diam-diam.
 *  3. Sistem hanya memberi **gambar** layar, bukan teks. Teksnya dibaca offline
 *     oleh ML Kit (OCR) di perangkat.
 *  4. Layar berflag SECURE (aplikasi bank, pembayaran, DRM, obrolan rahasia)
 *     tertangkap hitam — VELTRA akan jujur bilang "tidak terbaca" daripada
 *     mengarang hasil.
 *
 * Hemat: setelah satu frame diambil, virtual display-nya langsung dilepas.
 * Tangkapan tidak pernah disimpan ke disk dan tidak pernah dikirim ke jaringan.
 */
class OcrCaptureService : Service() {

    companion object {
        const val ACTION_START = "com.veltra.ocr.START"
        const val ACTION_CAPTURE = "com.veltra.ocr.CAPTURE"
        const val ACTION_STOP = "com.veltra.ocr.STOP"
        const val EXTRA_RESULT_CODE = "rc"
        const val EXTRA_DATA = "data"

        private const val CHANNEL = "veltra_reader"
        private const val NOTIF_ID = 771

        /** Tangkapan terakhir — dibagikan ke UI lewat StateFlow. */
        private val _lastBitmap = MutableStateFlow<Bitmap?>(null)
        val lastBitmap: StateFlow<Bitmap?> = _lastBitmap

        private val _status = MutableStateFlow("")
        val status: StateFlow<String> = _status

        @Volatile var running = false
            private set

        fun clear() { _lastBitmap.value = null }

        fun intent(ctx: Context, action: String, rc: Int = 0, data: Intent? = null): Intent =
            Intent(ctx, OcrCaptureService::class.java).apply {
                this.action = action
                putExtra(EXTRA_RESULT_CODE, rc)
                if (data != null) putExtra(EXTRA_DATA, data)
            }
    }

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_CAPTURE -> {
                if (projection == null) { stopSelf(); return START_NOT_STICKY }
                _status.value = "Mengambil layar…"
                _lastBitmap.value = null
                grab()
                return START_NOT_STICKY
            }
        }

        val rc = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33)
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        else @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_DATA)

        if (rc == 0 || data == null) { stopSelf(); return START_NOT_STICKY }

        runCatching {
            val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = mgr.getMediaProjection(rc, data)
        }.onFailure {
            _status.value = "Izin tangkap layar ditolak sistem."
            stopSelf(); return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIF_ID, buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else @Suppress("DEPRECATION") {
            startForeground(NOTIF_ID, buildNotification())
        }

        running = true
        _status.value = "Mengambil layar…"
        _lastBitmap.value = null
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                _status.value = "Sesi tangkap layar dihentikan."
                stopSelf()
            }
        }, Handler(Looper.getMainLooper()))
        grab()
        return START_NOT_STICKY
    }

    /** Pasang virtual display, tunggu satu frame, lalu lepas lagi (hemat). */
    private fun grab() {
        releaseDisplay()
        val proj = projection ?: return
        val metrics = DisplayMetrics()
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= 30) {
            wm.currentWindowMetrics.bounds.let { b ->
                metrics.widthPixels = b.width(); metrics.heightPixels = b.height()
            }
            metrics.densityDpi = resources.displayMetrics.densityDpi
        } else @Suppress("DEPRECATION") {
            wm.defaultDisplay.getRealMetrics(metrics)
        }
        // Perkecil di layar lebar supaya OCR tetap cepat di HP kentang.
        val scale = if (metrics.widthPixels > 1080) 1080f / metrics.widthPixels else 1f
        val w = (metrics.widthPixels * scale).roundToInt()
        val h = (metrics.heightPixels * scale).roundToInt()
        val density = (metrics.densityDpi * scale).roundToInt()

        runCatching {
            reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
            virtualDisplay = proj.createVirtualDisplay(
                "veltra-reader", w, h, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader!!.surface, null, Handler(Looper.getMainLooper())
            )
            reader!!.setOnImageAvailableListener({ r ->
                val image = runCatching { r.acquireLatestImage() }.getOrNull()
                    ?: return@setOnImageAvailableListener
                val bmp = decode(image)
                image.close()
                if (bmp != null) {
                    _lastBitmap.value = bmp
                    _status.value = "Layar tertangkap."
                }
                releaseDisplay()            // cukup satu frame
            }, Handler(Looper.getMainLooper()))
        }.onFailure {
            _status.value = "Gagal menangkap layar: ${it.message ?: "ditolak sistem"}"
        }
    }

    private fun releaseDisplay() {
        runCatching { virtualDisplay?.release() }
        runCatching { reader?.close() }
        virtualDisplay = null
        reader = null
    }

    private fun decode(image: Image): Bitmap? = runCatching {
        val plane = image.planes[0]
        val width = image.width
        val height = image.height
        val rowPadding = plane.rowStride - plane.pixelStride * width
        val bmp = Bitmap.createBitmap(
            width + rowPadding / plane.pixelStride, height, Bitmap.Config.ARGB_8888
        )
        bmp.copyPixelsFromBuffer(plane.buffer)
        Bitmap.createBitmap(bmp, 0, 0, width, height)
    }.getOrNull()

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(
                CHANNEL, "Veltra Screen Reader", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Menangkap layar untuk dibaca teksnya di perangkat" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        @Suppress("DEPRECATION")
        val builder = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        return builder
            .setContentTitle("VELTRA membaca layar")
            .setContentText("Diproses di perangkat. Tidak ada yang diunggah.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        releaseDisplay()
        runCatching { projection?.stop() }
        projection = null
        running = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
