package com.veltra.overlay.crosshair

import android.content.Intent

/** Bentuk-bentuk crosshair yang kepakai buat tembak-menembak. */
enum class CrosshairStyle(val label: String) {
    DOT("Titik"),
    CROSS("Silang"),
    CROSS_DOT("Silang + titik"),
    CIRCLE_DOT("Lingkar + titik"),
    CROSS_CIRCLE("Silang + lingkar"),
    T_SHAPE("Huruf T"),
    PRECISION("Presisi (4 garis)")
}

/**
 * Konfigurasi crosshair. Semua disimpan di DataStore (lihat SettingsRepository),
 * dikirim ke [CrosshairService] lewat extra Intent.
 *
 * CATATAN JUJUR:
 * Crosshair ini cuma **gambar di atas layar** (izin SYSTEM_ALERT_WINDOW), persis
 * seperti yang dilakukan aplikasi perekam layar / chat head. VELTRA **tidak**
 * menyentuh memori game, tidak membaca nilai aim, dan tidak mengubah apa pun di
 * dalam game. Konsekuensinya: presisinya ngikutin layar, bukan "auto-aim".
 */
data class CrosshairConfig(
    val enabled: Boolean = false,
    val style: CrosshairStyle = CrosshairStyle.CROSS_DOT,
    val sizeDp: Int = 24,          // panjang lengan / diameter
    val thicknessDp: Int = 2,      // tebal garis
    val gapDp: Int = 5,            // celah tengah
    val colorArgb: Long = 0xFF00E6A0, // NeoMint
    val alpha: Float = 0.92f,
    val offsetXDp: Int = 0,
    val offsetYDp: Int = 0,
    val onlyInGame: Boolean = true // sembunyikan otomatis di luar game
) {
    fun toIntent(base: Intent): Intent = base.apply {
        putExtra(EXTRA_STYLE, style.name)
        putExtra(EXTRA_SIZE, sizeDp)
        putExtra(EXTRA_THICKNESS, thicknessDp)
        putExtra(EXTRA_GAP, gapDp)
        putExtra(EXTRA_COLOR, colorArgb)
        putExtra(EXTRA_ALPHA, alpha)
        putExtra(EXTRA_OFFSET_X, offsetXDp)
        putExtra(EXTRA_OFFSET_Y, offsetYDp)
        putExtra(EXTRA_ONLY_GAME, onlyInGame)
    }

    companion object {
        const val EXTRA_STYLE = "ch_style"
        const val EXTRA_SIZE = "ch_size"
        const val EXTRA_THICKNESS = "ch_thick"
        const val EXTRA_GAP = "ch_gap"
        const val EXTRA_COLOR = "ch_color"
        const val EXTRA_ALPHA = "ch_alpha"
        const val EXTRA_OFFSET_X = "ch_ox"
        const val EXTRA_OFFSET_Y = "ch_oy"
        const val EXTRA_ONLY_GAME = "ch_only_game"

        fun from(i: Intent?): CrosshairConfig {
            if (i == null) return CrosshairConfig()
            return CrosshairConfig(
                enabled = true,
                style = runCatching { CrosshairStyle.valueOf(i.getStringExtra(EXTRA_STYLE) ?: "") }
                    .getOrDefault(CrosshairStyle.CROSS_DOT),
                sizeDp = i.getIntExtra(EXTRA_SIZE, 24),
                thicknessDp = i.getIntExtra(EXTRA_THICKNESS, 2),
                gapDp = i.getIntExtra(EXTRA_GAP, 5),
                colorArgb = i.getLongExtra(EXTRA_COLOR, 0xFF00E6A0),
                alpha = i.getFloatExtra(EXTRA_ALPHA, 0.92f),
                offsetXDp = i.getIntExtra(EXTRA_OFFSET_X, 0),
                offsetYDp = i.getIntExtra(EXTRA_OFFSET_Y, 0),
                onlyInGame = i.getBooleanExtra(EXTRA_ONLY_GAME, true)
            )
        }
    }
}
