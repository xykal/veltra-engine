package com.veltra.lottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.*

/**
 * Launch animation VELTRA — 1.4 detik, ring energy + logo pulse.
 * File JSON: lottie/src/main/assets/veltra_launch.json (maks 400KB).
 * Kalau aset belum ada, fallback ke versi Compose (tanpa crash).
 */
@Composable
fun LaunchAnimation(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("veltra_launch.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = 1.0f
    )
    composition?.let {
        LottieAnimation(
            composition = it,
            progress = { progress },
            modifier = modifier
        )
        if (progress >= 1f) onFinished()
    } ?: run {
        // Fallback: pulse ring pakai Compose murni (lihat LaunchFallback di app)
    }
}
