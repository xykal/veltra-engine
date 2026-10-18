package com.veltra.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow

private val Obsidian = Color(0xFF0A0E14)
private val Graphite = Color(0xFF161C28)
private val Cobalt = Color(0xFF2A64F6)
private val IonCyan = Color(0xFF00D1FF)
private val Titanium = Color(0xFFE6E8EC)
private val Amber = Color(0xFFFFB800)
private val NeoMint = Color(0xFF00E6A0)

/**
 * HUD Premium V2 — super gaming, bukan card biasa.
 * Collapsed: kapsul 42dp dengan pulse.
 * Expanded : frame HUD 300x420 — FPS gauge, ping wave, temp, STABILIZE hexagon.
 */
@Composable
fun VeltraHud(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    fpsFlow: StateFlow<Int>,
    pingFlow: StateFlow<Int>,
    tempFlow: StateFlow<Int>,
    statusFlow: StateFlow<String>,
    liveFlow: StateFlow<Boolean?>,
    onStabilize: () -> Unit,
    onClose: () -> Unit
) {
    val fps by fpsFlow.collectAsState()
    val ping by pingFlow.collectAsState()
    val temp by tempFlow.collectAsState()
    val status by statusFlow.collectAsState()
    val live by liveFlow.collectAsState()

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        Modifier
            .then(if (expanded) Modifier.size(300.dp, 420.dp) else Modifier.size(42.dp))
            .clip(RoundedCornerShape(if (expanded) 24.dp else 21.dp))
            .background(
                Brush.verticalGradient(listOf(Graphite.copy(0.94f), Obsidian.copy(0.97f)))
            )
            .hudBorder(Cobalt, pulse)
            .clickable { onExpandChange(!expanded) }
    ) {
        if (expanded) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                // --- Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("VELTRA", color = Titanium, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (status == "STABLE") NeoMint else Amber)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("✕", color = Color(0xFF6B7588), fontSize = 12.sp,
                        modifier = Modifier.clickable { onClose() })
                }
                Spacer(Modifier.height(16.dp))

                // --- LIVE GUARD chip
                live?.let { alive ->
                    LiveChip(alive)
                    Spacer(Modifier.height(10.dp))
                }

                // --- FPS GAUGE (circular)
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    FpsGauge(fps = fps, good = fps >= 50, pulse = pulse)
                }
                Spacer(Modifier.height(14.dp))

                // --- Ping + Temp row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricTile("PING", "${if (ping < 0) "--" else ping}ms",
                        if (ping in 1..60) NeoMint else Amber)
                    MetricTile("TEMP", "${temp}°C",
                        if (temp <= 42) NeoMint else Amber)
                }
                Spacer(Modifier.height(10.dp))
                PingWaveform(ping)
                Spacer(Modifier.weight(1f))

                // --- STABILIZE hexagon
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Cobalt, IonCyan.copy(0.85f))
                            )
                        )
                        .clickable { onStabilize() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡  STABILIZE", color = Color.White,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text("Tap panel untuk minimize", color = Color(0xFF6B7588),
                    fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        } else {
            // Collapsed kapsul
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("V", color = IonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FpsGauge(fps: Int, good: Boolean, pulse: Float) {
    val color = if (good) IonCyan else Amber
    val sweep = animateFloatAsState(targetValue = (fps.coerceIn(0, 120) / 120f) * 270f, label = "sweep")
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(120.dp)) {
            drawArc(
                color = Color.White.copy(0.06f),
                startAngle = 135f, sweepAngle = 270f, useCenter = false,
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(color.copy(0.35f), color)),
                startAngle = 135f, sweepAngle = sweep.value, useCenter = false,
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = color.copy(alpha = 0.10f * pulse),
                radius = 52f * pulse,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$fps", color = Titanium, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("FPS", color = Color(0xFF6B7588), fontSize = 9.sp, letterSpacing = 2.sp)
        }
    }
}

/** Indikator live: hijau = aman, merah kedip = live-mu udah dibunuh sistem. */
@Composable
private fun LiveChip(alive: Boolean) {
    val blink = rememberInfiniteTransition(label = "liveblink")
    val a by blink.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "blink"
    )
    val color = if (alive) Color(0xFFFF3B5C) else Color(0xFFFF3B5C).copy(alpha = a)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(if (alive) 0.16f else 0.10f * a))
            .padding(vertical = 7.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(if (alive) color else Color(0xFF6B7588)))
        Spacer(Modifier.width(8.dp))
        Text(
            if (alive) "LIVE — TERJAGA" else "LIVE PUTUS — BUKA APLIKASI",
            color = if (alive) Color(0xFFFF6B85) else Color(0xFF6B7588),
            fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun MetricTile(label: String, value: String, tint: Color) {
    Column(
        Modifier
            .width(124.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.04f))
            .padding(10.dp)
    ) {
        Text(label, color = Color(0xFF6B7588), fontSize = 9.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PingWaveform(ping: Int) {
    val history = remember { mutableStateListOf<Int>() }
    LaunchedEffect(ping) {
        history.add(ping.coerceAtLeast(0))
        if (history.size > 28) history.removeAt(0)
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        if (history.size < 2) return@Canvas
        val max = history.maxOrNull()?.coerceAtLeast(60) ?: 60
        val step = size.width / (history.size - 1)
        for (i in 0 until history.size - 1) {
            val y1 = size.height - (history[i].toFloat() / max) * size.height
            val y2 = size.height - (history[i + 1].toFloat() / max) * size.height
            drawLine(
                color = Color(0xFF00E6A0).copy(0.85f),
                start = Offset(i * step, y1),
                end = Offset((i + 1) * step, y2),
                strokeWidth = 3f, cap = StrokeCap.Round
            )
        }
    }
}

/** Border HUD glowing — bukan border datar. */
private fun Modifier.hudBorder(color: Color, pulse: Float) = this
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(
                brush = Brush.linearGradient(
                    listOf(color.copy(0.9f), color.copy(0.25f)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                style = Stroke(width = (1.6f * pulse).dp.toPx())
            )
        }
    }
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(listOf(color.copy(0.6f), Color.Transparent)),
        shape = RoundedCornerShape(24.dp)
    )
