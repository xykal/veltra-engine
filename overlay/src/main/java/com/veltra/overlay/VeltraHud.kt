package com.veltra.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
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
private val LiveRed = Color(0xFFFF3B5C)
private val Muted = Color(0xFF9AA3B3)
private val Dim = Color(0xFF6B7588)

private val Mono = FontFamily.Monospace

/** Segi enam — bentuk khas tombol utama VELTRA. */
private val HexagonShape = GenericShape { size: Size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.10f, h * 0.50f)
    lineTo(w * 0.26f, h * 0.06f)
    lineTo(w * 0.74f, h * 0.06f)
    lineTo(w * 0.90f, h * 0.50f)
    lineTo(w * 0.74f, h * 0.94f)
    lineTo(w * 0.26f, h * 0.94f)
    close()
}

/**
 * HUD Premium V3 — sesuai referensi visual:
 * frame HUD dengan bracket sudut, border glowing, gauge FPS melingkar,
 * waveform ping, tile suhu, tombol STABILIZE segi enam dengan energi,
 * 8 quick toggle, dan chip LIVE.
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
        initialValue = 0.75f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val scan by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "scan"
    )

    Box(
        Modifier
            .then(if (expanded) Modifier.size(300.dp, 468.dp) else Modifier.size(46.dp))
            .clip(RoundedCornerShape(if (expanded) 22.dp else 23.dp))
            .background(Brush.verticalGradient(listOf(Graphite.copy(0.96f), Obsidian.copy(0.98f))))
            .hudFrame(Cobalt, pulse, expanded)
            .clickable { onExpandChange(!expanded) }
    ) {
        if (expanded) {
            HudBrackets(IonCyan, scan)
            Column(Modifier.fillMaxSize().padding(16.dp)) {

                // ---- Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("VELTRA", color = Titanium, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .width(46.dp).height(1.dp)
                            .background(Brush.horizontalGradient(listOf(Cobalt, Color.Transparent)))
                    )
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(7.dp).clip(CircleShape)
                        .background(if (status == "STABLE") NeoMint else Amber))
                    Spacer(Modifier.width(10.dp))
                    Text("✕", color = Dim, fontSize = 12.sp,
                        modifier = Modifier.clickable { onClose() })
                }

                Spacer(Modifier.height(12.dp))

                live?.let { alive ->
                    LiveChip(alive)
                    Spacer(Modifier.height(10.dp))
                }

                // ---- FPS gauge
                Box(Modifier.fillMaxWidth().height(126.dp), contentAlignment = Alignment.Center) {
                    FpsGauge(fps = fps, good = fps >= 50, pulse = pulse)
                }

                Spacer(Modifier.height(12.dp))

                // ---- Ping + Temp
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile("PING", "${if (ping < 0) "--" else ping}ms",
                        if (ping in 1..60) NeoMint else Amber, Modifier.weight(1f))
                    MetricTile("TEMP", "${temp}°C",
                        if (temp <= 42) NeoMint else Amber, Modifier.weight(1f))
                }

                Spacer(Modifier.height(8.dp))
                PingWaveform(ping)

                Spacer(Modifier.height(12.dp))

                // ---- Quick toggles 4x2
                QuickToggles()

                Spacer(Modifier.height(12.dp))

                // ---- STABILIZE hexagon
                StabilizeButton(onClick = onStabilize, pulse = pulse)

                Spacer(Modifier.height(8.dp))
                Text("ketuk panel untuk minimize", color = Dim, fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(46.dp).drawWithCache {
                        onDrawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(IonCyan.copy(0.35f * pulse), Color.Transparent)
                                ),
                                radius = size.minDimension / 2
                            )
                        }
                    }
                )
                Text("V", color = IonCyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StabilizeButton(onClick: () -> Unit, pulse: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(HexagonShape)
            .background(
                Brush.horizontalGradient(listOf(Cobalt, IonCyan.copy(0.92f)))
            )
            .drawWithCache {
                onDrawBehind {
                    drawRoundRect(
                        color = IonCyan.copy(alpha = 0.28f * pulse),
                        cornerRadius = CornerRadius(28f, 28f),
                        size = Size(size.width, size.height),
                        style = Stroke(width = 2f * pulse)
                    )
                }
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚡", fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            Text("STABILIZE", color = Color.White, fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp, fontSize = 14.sp)
        }
    }
}

@Composable
private fun QuickToggles() {
    val items = listOf("PERF", "FOCUS", "NET", "TOUCH", "REC", "BRIGHT", "DND", "COOL")
    var states by remember {
        mutableStateOf(listOf(true, true, true, false, false, false, true, true))
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0..3) {
                    val i = row * 4 + col
                    val on = states[i]
                    Box(
                        Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (on) NeoMint.copy(0.13f) else Color.White.copy(0.04f))
                            .border(
                                width = 1.dp,
                                color = if (on) NeoMint.copy(0.55f) else Color.White.copy(0.06f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                states = states.toMutableList().also { it[i] = !on }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            items[i],
                            color = if (on) NeoMint else Dim,
                            fontSize = 7.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }
}

/** Bracket sudut ala HUD — bikin kerangka panel terasa "gaming". */
@Composable
private fun BoxScope.HudBrackets(color: Color, scan: Float) {
    Canvas(Modifier.matchParentSize().padding(6.dp)) {
        val len = 16.dp.toPx()
        val w = size.width
        val h = size.height
        val c = color.copy(0.55f + 0.25f * scan)
        val st = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Square)
        // kiri atas
        drawLine(c, Offset(0f, len), Offset(0f, 0f), strokeWidth = 2.dp.toPx())
        drawLine(c, Offset(0f, 0f), Offset(len, 0f), strokeWidth = 2.dp.toPx())
        // kanan atas
        drawLine(c, Offset(w - len, 0f), Offset(w, 0f), strokeWidth = 2.dp.toPx())
        drawLine(c, Offset(w, 0f), Offset(w, len), strokeWidth = 2.dp.toPx())
        // kiri bawah
        drawLine(c, Offset(0f, h - len), Offset(0f, h), strokeWidth = 2.dp.toPx())
        drawLine(c, Offset(0f, h), Offset(len, h), strokeWidth = 2.dp.toPx())
        // kanan bawah
        drawLine(c, Offset(w - len, h), Offset(w, h), strokeWidth = 2.dp.toPx())
        drawLine(c, Offset(w, h), Offset(w, h - len), strokeWidth = 2.dp.toPx())
    }
    Unit
}

@Composable
private fun FpsGauge(fps: Int, good: Boolean, pulse: Float) {
    val color = if (good) IonCyan else Amber
    val sweep by animateFloatAsState(
        targetValue = (fps.coerceIn(0, 120) / 120f) * 270f,
        animationSpec = tween(400), label = "sweep"
    )
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(126.dp)) {
            drawArc(
                color = Color.White.copy(0.07f),
                startAngle = 135f, sweepAngle = 270f, useCenter = false,
                style = Stroke(width = 11f, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(color.copy(0.30f), color)),
                startAngle = 135f, sweepAngle = sweep, useCenter = false,
                style = Stroke(width = 11f, cap = StrokeCap.Round)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = 0.18f * pulse), Color.Transparent)
                ),
                radius = 58f * pulse,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$fps", color = Titanium, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                fontFamily = Mono)
            Text("FPS", color = Dim, fontSize = 9.sp, letterSpacing = 3.sp)
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(0.045f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = Dim, fontSize = 8.5.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = tint, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            fontFamily = Mono)
    }
}

@Composable
private fun PingWaveform(ping: Int) {
    val history = remember { mutableStateListOf<Int>() }
    LaunchedEffect(ping) {
        history.add(ping.coerceAtLeast(0))
        if (history.size > 32) history.removeAt(0)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(0.03f))
    ) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 5.dp)) {
            if (history.size < 2) return@Canvas
            val max = (history.maxOrNull() ?: 60).coerceAtLeast(60)
            val step = size.width / (history.size - 1)
            for (i in 0 until history.size - 1) {
                val y1 = size.height - (history[i].toFloat() / max) * size.height
                val y2 = size.height - (history[i + 1].toFloat() / max) * size.height
                drawLine(
                    brush = Brush.verticalGradient(listOf(IonCyan, NeoMint.copy(0.5f))),
                    start = Offset(i * step, y1),
                    end = Offset((i + 1) * step, y2),
                    strokeWidth = 2.5f, cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun LiveChip(alive: Boolean) {
    val blink = rememberInfiniteTransition(label = "liveblink")
    val a by blink.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "blink"
    )
    val alpha = if (alive) 1f else a
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(LiveRed.copy(if (alive) 0.16f else 0.10f * alpha))
            .border(1.dp, LiveRed.copy(0.45f * alpha), RoundedCornerShape(9.dp))
            .padding(vertical = 7.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape)
            .background(if (alive) LiveRed else Dim.copy(alpha)))
        Spacer(Modifier.width(8.dp))
        Text(
            if (alive) "LIVE — TERJAGA" else "LIVE PUTUS — BUKA APLIKASI",
            color = if (alive) Color(0xFFFF6B85) else Dim,
            fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
        )
    }
}

/** Border HUD glowing + frame luar tipis. */
private fun Modifier.hudFrame(color: Color, pulse: Float, expanded: Boolean) = this
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(color.copy(0.95f), IonCyan.copy(0.35f), color.copy(0.15f)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = CornerRadius(if (expanded) 22.dp.toPx() else 23.dp.toPx()),
                style = Stroke(width = (1.5f * pulse).dp.toPx())
            )
        }
    }
