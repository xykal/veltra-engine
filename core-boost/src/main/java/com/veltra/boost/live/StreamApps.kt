package com.veltra.boost.live

import android.content.Context
import android.content.pm.PackageManager

/**
 * Aplikasi LIVE yang WAJIB dilindungi.
 * VELTRA tidak pernah membunuh proses di daftar ini — walau mode AGGRESSIVE.
 *
 * Daftar package ini **diverifikasi langsung** (Oktober 2026):
 *   TikTok          → com.zhiliaoapp.musically      (v47.4.3)
 *   TikTok regional → com.ss.android.ugc.trill
 *   TikTok Lite     → com.zhiliaoapp.musically.go   (v47.4.2)  ← paling ringan
 *   Douyin (CN)     → com.ss.android.ugc.aweme
 */
object StreamApps {

    const val TIKTOK = "com.zhiliaoapp.musically"        // TikTok global
    const val TIKTOK_LITE = "com.zhiliaoapp.musically.go" // TikTok Lite — jauh lebih ringan
    const val TIKTOK_TRILL = "com.ss.android.ugc.trill"   // varian regional (Asia)
    const val TIKTOK_DOUYIN = "com.ss.android.ugc.aweme"  // Douyin (China)
    const val INSTAGRAM = "com.instagram.android"
    const val FACEBOOK = "com.facebook.katana"
    const val YOUTUBE = "com.google.android.youtube"
    const val TWITCH = "tv.twitch.android.app"
    const val BIGO = "sg.bigo.live"
    const val SHOPEE = "com.shopee.id"
    const val LAZADA = "com.lazada.android"
    const val FACEBOOK_LITE = "com.facebook.lite"
    const val TIKTOK_BUSINESS = "com.tiktok.business"

    /** Urutan prioritas — yang paling ringan duluan buat HP kentang. */
    val ALL = listOf(
        TIKTOK, TIKTOK_LITE, TIKTOK_TRILL, TIKTOK_DOUYIN, TIKTOK_BUSINESS,
        INSTAGRAM, FACEBOOK, FACEBOOK_LITE, YOUTUBE, TWITCH, BIGO, SHOPEE, LAZADA
    )

    /** Paket TikTok saja (semua varian) — buat deteksi cepat. */
    val TIKTOK_FAMILY = setOf(TIKTOK, TIKTOK_LITE, TIKTOK_TRILL, TIKTOK_DOUYIN, TIKTOK_BUSINESS)

    fun label(pkg: String): String = when (pkg) {
        TIKTOK -> "TikTok"
        TIKTOK_LITE -> "TikTok Lite"
        TIKTOK_TRILL -> "TikTok (regional)"
        TIKTOK_DOUYIN -> "Douyin"
        INSTAGRAM -> "Instagram"
        FACEBOOK -> "Facebook"
        YOUTUBE -> "YouTube"
        TWITCH -> "Twitch"
        BIGO -> "Bigo Live"
        SHOPEE -> "Shopee Live"
        LAZADA -> "Lazada Live"
        FACEBOOK_LITE -> "Facebook Lite"
        TIKTOK_BUSINESS -> "TikTok Business"
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
