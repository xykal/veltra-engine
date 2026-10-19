package com.veltra.core

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Launch animation V2 — Compose murni, tanpa aset eksternal.
 *
 * Susunan geraknya:
 *  1. Cahaya inti menyala (0–400ms)
 *  2. Cincin heksagon berotasi + orbit partikel (terus-menerus)
 *  3. Logo V menggambar dirinya sendiri dari kiri ke kanan (400–1000ms)
 *  4. Baris status "boot" muncul bertahap, garis progres penuh (hingga 1.7s)
 *  5. Seluruh lapisan memudar, masuk ke dashboard
 *
 * Ringan sengaja: cuma Canvas + beberapa animasi, aman buat HP kentang.
 */
@Composable
fun LaunchSplash(onFinished: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "launch")

    val orbit by inf.animateFloat(
        0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "orbit"
    )
    val counter by inf.animateFloat(
        0f, -360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "counter"
    )
    val pulse by inf.animateFloat(
        0.82f, 1.12f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse"
    )
    val particles by inf.animateFloat(
        0f, 360f, infiniteRepeatable(tween(3400, easing = LinearEasing)), label = "particles"
    )
    val drift by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Reverse), label = "drift"
    )

    // urutan waktu
    var draw by remember { mutableFloatStateOf(0f) }      // 0→1 menggambar logo
    var boot by remember { mutableIntStateOf(0) }         // baris status
    var fadeOut by remember { mutableStateOf(false) }

    val logo = animateFloatAsState(draw, tween(620, easing = LinearEasing), label = "draw")
    val progress = animateFloatAsState(if (boot >= 3) 1f else boot / 3f, tween(300), label = "prog")
    val alpha = animateFloatAsState(if (fadeOut) 0f else 1f, tween(320), label = "fade")

    LaunchedEffect(Unit) {
        delay(180); draw = 1f
        delay(260); boot = 1
        delay(240); boot = 2
        delay(210); boot = 3
        delay(170); fadeOut = true
        delay(170); onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF111A2B), VeltraColors.Obsidian, Color(0xFF06090E)),
                    radius = 1800f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // lapisan cahaya yang bergerak pelan
        Box(
            Modifier
                .fillMaxSize()
                .offset(x = ((drift * 70f) - 35f).dp, y = ((drift * 40f) - 20f).dp)
                .background(
                    Brush.radialGradient(
                        listOf(VeltraColors.Cobalt.copy(0.09f + 0.05f * drift), Color.Transparent),
                        radius = 760f
                    )
                )
        )

        Canvas(
            Modifier
                .size(230.dp)
                .graphicsLayer { this.alpha = alpha.value }
        ) {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = size.minDimension / 2 - 14.dp.toPx()

            // cincin dasar
            drawArc(Color.White.copy(.05f), 0f, 360f, false,
                style = Stroke(width = 2.dp.toPx()))

            // heksagon luar berotasi
            rotate(counter, pivot = Offset(cx, cy)) {
                drawPath(
                    hexPath(Offset(cx, cy), r),
                    color = VeltraColors.IonCyan.copy(.22f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // busur energi utama
            rotate(orbit, pivot = Offset(cx, cy)) {
                drawArc(
                    Brush.sweepGradient(
                        listOf(Color.Transparent, VeltraColors.Cobalt,
                            VeltraColors.IonCyan, Color.Transparent)
                    ),
                    0f, 170f, false,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            rotate(orbit * 1.7f, pivot = Offset(cx, cy)) {
                drawArc(
                    VeltraColors.IonCyan.copy(.45f), 180f, 62f, false,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // partikel orbit
            for (i in 0 until 7) {
                val ang = Math.toRadians((particles + i * (360f / 7f)).toDouble())
                val pr = (r * (0.86f + 0.06f * sin(ang * 3).toFloat())).toFloat()
                drawCircle(
                    color = if (i % 2 == 0) VeltraColors.IonCyan else VeltraColors.Cobalt,
                    radius = (2.2f + 1.1f * pulse).dp.toPx(),
                    center = Offset(cx + pr * cos(ang).toFloat(), cy + pr * sin(ang).toFloat())
                )
            }

            // cahaya inti
            drawCircle(
                Brush.radialGradient(
                    listOf(VeltraColors.Cobalt.copy(.42f * pulse), Color.Transparent),
                    radius = r * 0.85f
                ),
                radius = r * 0.85f, center = Offset(cx, cy)
            )

            // logo V — menggambar diri sendiri (kiri → kanan)
            val w = 60f
            val h = 70f
            val left = cx - w / 2
            val top = cy - h / 2
            val p = Path().apply {
                moveTo(left, top)
                lineTo(left + w * 0.32f, top)
                lineTo(cx, top + h * 0.66f)
                lineTo(cx + w * 0.32f, top)
                lineTo(left + w, top)
                lineTo(cx + w * 0.14f, top + h)
                lineTo(cx - w * 0.14f, top + h)
                close()
            }
            drawPath(p, VeltraColors.IonCyan.copy(.18f))
            // potong sebagian sesuai progres gambar
            clipRect(right = left + w * logo.value.coerceIn(0.001f, 1f) * 1.6f) {
                drawPath(p, Brush.linearGradient(
                    listOf(VeltraColors.IonCyan, VeltraColors.Titanium)
                ))
            }
        }

        // ── Teks bawah ────────────────────────────────────────────────
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { this.alpha = alpha.value }
                .padding(bottom = 76.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "VELTRA",
                color = VeltraColors.Titanium,
                fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 9.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "ALL DEVICE • ALL GAMES",
                color = VeltraColors.Muted, fontSize = 8.5.sp, letterSpacing = 3.2.sp
            )
            Spacer(Modifier.height(20.dp))

            // baris boot
            val lines = listOf("INIT CORE", "MAP MEMORY", "ARM PANEL")
            lines.forEachIndexed { i, l ->
                if (i < boot) {
                    val a = animateFloatAsState(1f, tween(240), label = "l$i")
                    Text(
                        "› $l " + ".".repeat(3),
                        color = VeltraColors.IonCyan.copy(.75f * a.value),
                        fontSize = 9.sp, letterSpacing = 1.6.sp, fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(3.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .width(140.dp)
                    .height(2.dp)
                    .background(Color.White.copy(.08f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.value)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(VeltraColors.Cobalt, VeltraColors.IonCyan)
                            )
                        )
                )
            }
        }
    }
}

/** Heksagon (bentuk inti identitas VELTRA). */
private fun hexPath(center: Offset, radius: Float): Path = Path().apply {
    for (i in 0 until 6) {
        val a = Math.toRadians((60 * i - 30).toDouble())
        val x = center.x + radius * cos(a).toFloat()
        val y = center.y + radius * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
