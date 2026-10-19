package com.veltra.launcher.live

import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.veltra.boost.MetricsBus
import com.veltra.boost.live.StreamApps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DETEKSI LIVE OTOMATIS — tanpa daftar manual.
 *
 * Android tidak memberi cara resmi buat tahu "aplikasi ini sedang live".
 * Yang bisa dipakai: **notifikasi yang berjalan terus** (ongoing). TikTok /
 * TikTok Lite / Instagram / Bigo selalu memasang notifikasi ongoing selama
 * siaran berlangsung, dan mencabutnya begitu siaran berhenti.
 *
 * Jadi VELTRA memantaunya lewat NotificationListenerService — izin harus
 * dinyalakan sendiri oleh pengguna di
 *   Pengaturan → Notifikasi → Akses notifikasi → VELTRA
 * (fitur ini opsional; kalau izin nggak diberikan, Live Guard tetap jalan
 * pakai daftar aplikasi + watchdog `/proc`.)
 *
 * Tidak ada isi notifikasi yang disimpan atau dikirim ke mana pun.
 * VELTRA cuma melihat **apakah notifikasinya masih ada**.
 */
class LiveDetectService : NotificationListenerService() {

    /** Keadaan live yang terdeteksi. */
    object LiveState {
        private val _livePackage = MutableStateFlow<String?>(null)
        val livePackage: StateFlow<String?> = _livePackage.asStateFlow()

        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        fun set(pkg: String?) { _livePackage.value = pkg }
        fun connected(v: Boolean) { _connected.value = v }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        LiveState.connected(true)
        rescan()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        LiveState.connected(false)
        LiveState.set(null)
        MetricsBus.publishLive(null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        if (pkg !in StreamApps.ALL) return
        if (isLiveOngoing(sbn)) {
            LiveState.set(pkg)
            MetricsBus.publishLive(true)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        if (pkg == LiveState.livePackage.value) {
            // notifikasi ongoing hilang → kemungkinan besar siarannya berhenti
            rescan()
        }
    }

    /** Periksa ulang semua notifikasi yang masih menempel. */
    private fun rescan() {
        val active = runCatching { activeNotifications }.getOrNull() ?: emptyArray()
        val live = active.firstOrNull { it.packageName in StreamApps.ALL && isLiveOngoing(it) }
        LiveState.set(live?.packageName)
        MetricsBus.publishLive(if (live == null) null else true)
    }

    /**
     * Ciri notifikasi live: berjalan terus (ongoing) atau masuk kategori
     * service/call, atau teksnya menyebut "live".
     */
    private fun isLiveOngoing(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification ?: return false
        val ongoing = (n.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        if (ongoing) return true
        if (Build.VERSION.SDK_INT >= 26 && n.category in listOf(
                android.app.Notification.CATEGORY_SERVICE,
                android.app.Notification.CATEGORY_CALL,
                android.app.Notification.CATEGORY_TRANSPORT
            )
        ) return true
        val text = runCatching {
            n.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
                ?: n.extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
                ?: ""
        }.getOrDefault("")
        return text.contains("live", true) || text.contains("siaran", true) ||
                text.contains("sedang", true)
    }

    companion object {
        /** Intent buat membuka layar izin akses notifikasi. */
        fun settingsIntent(): Intent =
            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
    }
}
