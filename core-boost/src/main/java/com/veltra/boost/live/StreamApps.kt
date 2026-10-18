package com.veltra.boost.live

import android.content.Context
import android.content.pm.PackageManager

/**
 * Aplikasi LIVE yang WAJIB dilindungi.
 * VELTRA tidak pernah membunuh proses di daftar ini — walau mode AGGRESSIVE.
 */
object StreamApps {

    const val TIKTOK = "com.zhiliaoapp.musically"        // TikTok global
    const val TIKTOK_LITE = "com.zhiliaoapp.musically.go" // TikTok Lite — jauh lebih ringan
    const val TIKTOK_TRILL = "com.ss.android.ugc.trill"   // varian regional
    const val INSTAGRAM = "com.instagram.android"
    const val FACEBOOK = "com.facebook.katana"
    const val YOUTUBE = "com.google.android.youtube"
    const val TWITCH = "tv.twitch.android.app"
    const val BIGO = "sg.bigo.live"
    const val SHOPEE = "com.shopee.id"

    /** Urutan prioritas — yang paling ringan duluan buat HP kentang. */
    val ALL = listOf(
        TIKTOK, TIKTOK_LITE, TIKTOK_TRILL, INSTAGRAM, FACEBOOK, YOUTUBE, TWITCH, BIGO, SHOPEE
    )

    fun label(pkg: String): String = when (pkg) {
        TIKTOK -> "TikTok"
        TIKTOK_LITE -> "TikTok Lite"
        TIKTOK_TRILL -> "TikTok (regional)"
        INSTAGRAM -> "Instagram"
        FACEBOOK -> "Facebook"
        YOUTUBE -> "YouTube"
        TWITCH -> "Twitch"
        BIGO -> "Bigo Live"
        SHOPEE -> "Shopee Live"
        else -> pkg
    }

    fun isInstalled(ctx: Context, pkg: String): Boolean = runCatching {
        ctx.packageManager.getApplicationInfo(pkg, 0).enabled
    }.getOrDefault(false)

    fun installedStreamApps(ctx: Context): List<String> =
        ALL.filter { isInstalled(ctx, it) }

    /** Rekomendasi: HP ≤3GB RAM disuruh pakai Lite. */
    fun recommend(ctx: Context, totalRamMb: Long): String? =
        if (totalRamMb <= 3_072L && isInstalled(ctx, TIKTOK_LITE)) TIKTOK_LITE else null
}
