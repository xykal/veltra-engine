package com.veltra.core

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Launch animation — versi Compose murni (fallback kalau file Lottie belum ada).
 * Energy ring orbit + logo pulse, 1.4 detik, lalu lanjut ke dashboard.
 */
@Composable
fun LaunchSplash(onFinished: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "launch")
    val orbit by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "orbit"
    )
    val pulse by inf.animateFloat(
        initialValue = 0.7f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(1400)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF101828), VeltraColors.Obsidian)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(190.dp)) {
            val cx = size.width / 2
            val cy = size.height / 2
            // ring dasar
            drawArc(
                color = Color.White.copy(0.07f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = 3.dp.toPx())
            )
            // orbit energy
            rotate(orbit, pivot = Offset(cx, cy)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color.Transparent, VeltraColors.Cobalt, VeltraColors.IonCyan)
                    ),
                    startAngle = 0f, sweepAngle = 150f, useCenter = false,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            rotate(-orbit * 0.6f, pivot = Offset(cx, cy)) {
                drawArc(
                    color = VeltraColors.IonCyan.copy(0.35f),
                    startAngle = 180f, sweepAngle = 70f, useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // inner glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(VeltraColors.Cobalt.copy(0.30f * pulse), Color.Transparent)
                ),
                radius = 62f * pulse,
                center = Offset(cx, cy)
            )
            // logo V
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx - 26f, cy - 26f)
                    lineTo(cx, cy + 30f)
                    lineTo(cx + 26f, cy - 26f)
                    lineTo(cx + 12f, cy - 26f)
                    lineTo(cx, cy + 6f)
                    lineTo(cx - 12f, cy - 26f)
                    close()
                },
                color = VeltraColors.IonCyan
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 74.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("VELTRA", color = VeltraColors.Titanium, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
            Spacer(Modifier.height(8.dp))
            Text("STABILIZING…", color = VeltraColors.Muted, fontSize = 9.sp,
                letterSpacing = 4.sp)
        }
    }
}
