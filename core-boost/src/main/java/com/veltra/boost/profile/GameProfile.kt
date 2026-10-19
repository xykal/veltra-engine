package com.veltra.boost.profile

/**
 * PROFIL PER-GAME.
 *
 * Satu baris pengaturan yang dipakai OTOMATIS tiap kali game ini ada di depan —
 * jadi lu nggak perlu setel ulang FPS, crosshair, dan mode senyap setiap kali main.
 *
 * Jujur soal arti tiap field:
 *  - `targetFps`  → hint ke scheduler lewat ADPF (Android 12+). Bukan perintah,
 *                   bukan jaminan. Game yang punya batas FPS sendiri tetap menang.
 *  - `fpsLock`    → Game Mode intervention (`cmd game set --fps`). Butuh Shizuku/ADB
 *                   atau root. Efeknya nyata di game yang mendukung, dan butuh restart game.
 *  - `hzLock`     → kunci refresh rate layar. Butuh Shizuku/ADB atau root. Panel 60Hz
 *                   tetap 60Hz — nggak ada aplikasi yang bisa mengubah panel fisik.
 *  - `boost`      → seberapa agresif VELTRA membersihkan proses cache di belakang layar.
 *  - `crosshair`  → gambar bantu bidik (overlay VELTRA sendiri, tidak menyentuh game).
 *  - `quiet`      → mode senyap: DND kalau izin ada, atau silent ringer kalau belum.
 *  - `liveGuard`  → langsung nyalakan penjaga live (buat lu yang sambil TikTok LIVE).
 *  - `note`       → catatan bebas (mis. angka sensitivitas lu). VELTRA TIDAK menulis
 *                   ke data game; ini murni catatan pribadi lu.
 */
data class GameProfile(
    val pkg: String,
    val targetFps: Int = 60,
    val fpsLock: Int = 0,                       // 0 = tidak dikunci
    val hzLock: Int = 0,                        // 0 = ikut sistem
    val boost: BoostLevel = BoostLevel.SEIMBANG,
    val crosshair: Boolean = false,
    val quiet: Boolean = false,
    val liveGuard: Boolean = false,
    val note: String = ""
) {
    enum class BoostLevel(val label: String, val hint: String) {
        HEMAT("HEMAT", "tidak membunuh proses — paling aman buat HP 2GB & sambil live"),
        SEIMBANG("SEIMBANG", "bersihkan proses cache yang aman, lalu kasih hint performa"),
        MAKSIMAL("MAKSIMAL", "pembersihan dua putaran + hint performa — RAM paling lega")
    }

    /** Profil ini pernah disetel sendiri oleh user, atau masih bawaan? */
    val isCustom: Boolean get() =
        targetFps != 60 || fpsLock != 0 || hzLock != 0 ||
            boost != BoostLevel.SEIMBANG || crosshair || quiet || liveGuard || note.isNotBlank()

    companion object {
        val DEFAULT_FPS_CHOICES = listOf(30, 45, 60, 90, 120)
        val LOCK_FPS_CHOICES = listOf(0, 30, 45, 60, 90)
        val HZ_CHOICES = listOf(0, 60, 90, 120)

        fun defaultFor(pkg: String) = GameProfile(pkg = pkg)

        /**
         * Format simpan: `pkg|targetFps|fpsLock|hzLock|boost|crosshair|quiet|liveGuard|note`
         * Catatan ditaruh paling akhir dan dipisah dengan `limit = 9`, jadi aman
         * walau isinya mengandung karakter `|`.
         */
        fun encode(p: GameProfile): String = listOf(
            p.pkg, p.targetFps, p.fpsLock, p.hzLock, p.boost.name,
            p.crosshair, p.quiet, p.liveGuard, p.note
        ).joinToString("|")

        fun decode(raw: String): GameProfile? {
            val f = raw.split("|", limit = 9)
            if (f.size < 8) return null
            val pkg = f[0]
            if (pkg.isBlank()) return null
            val int = { i: Int -> f[i].toIntOrNull() ?: 0 }
            val bool = { i: Int -> f[i].toBooleanStrictOrNull() ?: false }
            return GameProfile(
                pkg = pkg,
                targetFps = int(1).coerceIn(24, 240),
                fpsLock = int(2),
                hzLock = int(3),
                boost = runCatching { BoostLevel.valueOf(f[4]) }.getOrDefault(BoostLevel.SEIMBANG),
                crosshair = bool(5),
                quiet = bool(6),
                liveGuard = bool(7),
                note = f.getOrNull(8).orEmpty()
            )
        }
    }
}
