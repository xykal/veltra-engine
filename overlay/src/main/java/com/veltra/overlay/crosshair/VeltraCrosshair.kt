package com.veltra.overlay.crosshair

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Crosshair presisi VELTRA.
 *
 * Digambar dengan Canvas Compose di window overlay yang **tidak bisa disentuh**
 * (FLAG_NOT_TOUCHABLE) — sentuhan jari langsung tembus ke game, jadi nggak ada
 * gangguan sama sekali saat aiming.
 *
 * Bayangan hitam tipis di bawah garis supaya tetap kelihatan di background terang.
 */
@Composable
fun VeltraCrosshair(
    config: CrosshairConfig,
    modifier: Modifier = Modifier,
    preview: Boolean = false
) {
    val base = Color(config.colorArgb)
    val color = base.copy(alpha = config.alpha)
    val shadow = Color(0xFF000000).copy(alpha = config.alpha * 0.35f)

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val arm = config.sizeDp.dp.toPx() / 2f
            val gap = config.gapDp.dp.toPx()
            val w = config.thicknessDp.dp.toPx().coerceAtLeast(1f)

            // bayangan (digeser 1px) biar kontras di map terang
            drawCrosshair(config.style, cx + 1f, cy + 1f, arm, gap, w, shadow)
            drawCrosshair(config.style, cx, cy, arm, gap, w, color)

            if (preview) {
                // penanda tengah saat preview
                drawCircle(Color.White.copy(0.10f), radius = arm * 2.2f,
                    center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrosshair(
    style: CrosshairStyle,
    cx: Float,
    cy: Float,
    arm: Float,
    gap: Float,
    w: Float,
    color: Color
) {
    val cap = StrokeCap.Butt
    val stroke = Stroke(width = w, cap = cap)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = w, cap = cap)

    when (style) {
        CrosshairStyle.DOT -> {
            drawCircle(color, radius = (w * 1.6f).coerceAtLeast(1.5f), center = Offset(cx, cy))
        }
        CrosshairStyle.CROSS, CrosshairStyle.CROSS_DOT -> {
            line(cx - arm, cy, cx - gap, cy)      // kiri
            line(cx + gap, cy, cx + arm, cy)      // kanan
            line(cx, cy - arm, cx, cy - gap)      // atas
            line(cx, cy + gap, cx, cy + arm)      // bawah
            if (style == CrosshairStyle.CROSS_DOT) {
                drawCircle(color, radius = (w * 0.9f).coerceAtLeast(1f), center = Offset(cx, cy))
            }
        }
        CrosshairStyle.CIRCLE_DOT -> {
            drawCircle(color, radius = arm, center = Offset(cx, cy), style = stroke)
            drawCircle(color, radius = (w * 0.9f).coerceAtLeast(1f), center = Offset(cx, cy))
        }
        CrosshairStyle.CROSS_CIRCLE -> {
            drawCircle(color, radius = arm, center = Offset(cx, cy), style = stroke)
            line(cx - arm * 0.55f, cy, cx - gap * 0.6f, cy)
            line(cx + gap * 0.6f, cy, cx + arm * 0.55f, cy)
            line(cx, cy - arm * 0.55f, cx, cy - gap * 0.6f)
            line(cx, cy + gap * 0.6f, cx, cy + arm * 0.55f)
            drawCircle(color, radius = (w * 0.8f).coerceAtLeast(1f), center = Offset(cx, cy))
        }
        CrosshairStyle.T_SHAPE -> {
            line(cx - arm, cy, cx + arm, cy)
            line(cx, cy - gap, cx, cy - arm * 1.1f)
        }
        CrosshairStyle.PRECISION -> {
            // 4 garis pendek di tiap sudut + titik — favorit pemain presisi
            line(cx - arm, cy, cx - gap, cy)
            line(cx + gap, cy, cx + arm, cy)
            line(cx, cy - arm, cx, cy - gap)
            line(cx, cy + gap, cx, cy + arm)
            val r = arm * 0.62f
            drawCircle(color, radius = r, center = Offset(cx, cy),
                style = Stroke(width = w * 0.6f))
            drawCircle(color, radius = (w * 0.9f).coerceAtLeast(1f), center = Offset(cx, cy))
        }
    }
}
