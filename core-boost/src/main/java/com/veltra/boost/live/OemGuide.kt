package com.veltra.boost.live

import android.os.Build

/**
 * Panduan per-ROM. Sumber utama: dontkillmyapp.com
 * Ini HAL PALING PENTING buat HP kentang — sering kali bukan RAM yang bunuh
 * live-mu, tapi "battery saver" bawaan pabrik.
 */
object OemGuide {

    data class Step(val title: String, val detail: String)

    data class Guide(val rom: String, val steps: List<Step>)

    fun detect(): String = when {
        Build.MANUFACTURER.equals("xiaomi", true) ||
                Build.MANUFACTURER.equals("redmi", true) ||
                Build.MANUFACTURER.equals("poco", true) -> "MIUI / HyperOS (Xiaomi)"
        Build.MANUFACTURER.equals("samsung", true) -> "One UI (Samsung)"
        Build.MANUFACTURER.equals("oppo", true) ||
                Build.MANUFACTURER.equals("realme", true) ||
                Build.MANUFACTURER.equals("oneplus", true) -> "ColorOS / OxygenOS"
        Build.MANUFACTURER.equals("vivo", true) -> "Funtouch / OriginOS (vivo)"
        Build.MANUFACTURER.equals("infinix", true) ||
                Build.MANUFACTURER.equals("tecno", true) ||
                Build.MANUFACTURER.equals("itel", true) ||
                Build.MANUFACTURER.equals("transsion", true) -> "HiOS / XOS (Infinix/Tecno)"
        Build.MANUFACTURER.contains("huawei") ||
                Build.MANUFACTURER.equals("honor", true) -> "EMUI / MagicOS"
        else -> "Android standar"
    }

    fun steps(): List<Step> = when (detect()) {
        "MIUI / HyperOS (Xiaomi)" -> listOf(
            Step("1. Kunci TikTok di Recent Apps",
                "Buka Recent Apps → tahan kartu TikTok sampai muncul ikon GEMBOK → nyalakan. " +
                        "Ini langkah paling krusial di Xiaomi."),
            Step("2. Autostart ON",
                "Settings → Apps → Manage apps → TikTok → Autostart → ON. " +
                        "(Atau lewat app Security → Permissions → Autostart.)"),
            Step("3. Battery saver: No restrictions",
                "Settings → Apps → Manage apps → TikTok → Battery saver → No restrictions."),
            Step("4. Matikan Memory Extension",
                "Settings → About phone → Memory extension → OFF. " +
                        "Fitur ini bikin HP makin ngos-ngosan karena storage dipakai jadi RAM."),
            Step("5. Game Turbo: jangan di-‘boost’ TikTok",
                "Buka Game Turbo/Game Space → pastikan VELTRA & TikTok TIDAK masuk daftar yang dibersihkan.")
        )
        "One UI (Samsung)" -> listOf(
            Step("1. Matikan RAM Plus",
                "Settings → Battery and device care → Memory → RAM Plus → OFF. " +
                        "Di HP tua, RAM Plus sering bikin aplikasi malah di-reload terus."),
            Step("2. Jangan ‘Sleep’ TikTok",
                "Settings → Battery → Background usage limits → Sleeping apps → keluarkan TikTok."),
            Step("3. Optimize battery: OFF",
                "Settings → Apps → TikTok → Battery → Unrestricted."),
            Step("4. Keep open for quick launch (opsional)",
                "Recent Apps → tap ikon TikTok di atas kartu → Keep open. Cuma nahan di RAM, jangan diandalkan sendiri.")
        )
        "ColorOS / OxygenOS" -> listOf(
            Step("1. Matikan battery optimization TikTok",
                "Settings → Battery → App battery management → TikTok → Allow foreground + Allow background."),
            Step("2. Kunci di Recent Apps",
                "Recent Apps → tarik kartu TikTok ke bawah sampai muncul gembok."),
            Step("3. Auto-start ON",
                "Settings → Apps → Auto-start → TikTok → ON."),
            Step("4. Game Space",
                "Tambahkan game ke Game Space, tapi JANGAN nyalakan ‘Competition mode’—itu malah mematikan notifikasi & live.")
        )
        "Funtouch / OriginOS (vivo)" -> listOf(
            Step("1. Background app refresh",
                "Settings → Battery → Background power consumption → TikTok → Allow."),
            Step("2. Kunci di Recent Apps",
                "Recent Apps → tarik kartu TikTok ke bawah."),
            Step("3. iManager / Phone Manager",
                "Buka iManager → App manager → Autostart → TikTok → ON.")
        )
        "HiOS / XOS (Infinix/Tecno)" -> listOf(
            Step("1. Auto-start & Battery",
                "Phone Master → Autostart management → TikTok ON. Lalu Battery → App battery saver → TikTok → No restriction."),
            Step("2. Kunci di Recent Apps",
                "Recent Apps → ikon gembok di kartu TikTok."),
            Step("3. Matikan Ultra Battery Saver",
                "Kalau menyala, semua aplikasi background dibunuh otomatis.")
        )
        "EMUI / MagicOS" -> listOf(
            Step("1. App launch: Manage manually",
                "Settings → Apps → Apps → TikTok → App launch → matikan ‘Manage automatically’, " +
                        "lalu nyalakan Auto-launch, Secondary launch, Run in background."),
            Step("2. Battery: Manual → ON",
                "Phone Manager → Battery → App power details → TikTok → Run in background ON.")
        )
        else -> listOf(
            Step("1. Matikan battery optimization",
                "Settings → Apps → TikTok → Battery → Unrestricted / Don’t optimize."),
            Step("2. Kunci di Recent Apps",
                "Recent Apps → tahan kartu TikTok → Lock / Pin."),
            Step("3. Turunkan beban layar",
                "Wallpaper hidup & animasi 120Hz matikan—ini ngurangin beban GPU yang dipakai bareng encoder live.")
        )
    }
}
