package com.veltra.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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

private val HexagonShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
    val w = size.width; val h = size.height
    moveTo(w * 0.10f, h * 0.50f)
    lineTo(w * 0.26f, h * 0.06f)
    lineTo(w * 0.74f, h * 0.06f)
    lineTo(w * 0.90f, h * 0.50f)
    lineTo(w * 0.74f, h * 0.94f)
    lineTo(w * 0.26f, h * 0.94f)
    close()
}

/**
 * PANEL GAYA ROG — sidebar di kiri/kanan layar.
 *
 * Pola pakai (mirip Game Genie / Edge panel):
 *  - Nempel di tepi layar sebagai **handle tipis** (20dp × 118dp)
 *  - Ketuk handle → panel geser keluar (232dp) berisi HUD vertikal
 *  - Geser handle naik-turun buat pindah posisi
 *  - Bisa dipasang di kiri ATAU kanan (setelan)
 */
@Composable
fun VeltraSidebar(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onDrag: (Float) -> Unit,
    fpsFlow: StateFlow<Int>,
    pingFlow: StateFlow<Int>,
    tempFlow: StateFlow<Int>,
    statusFlow: StateFlow<String>,
    liveFlow: StateFlow<Boolean?>,
    onStabilize: () -> Unit,
    onClose: () -> Unit,
    side: SidebarSide
) {
    val fps by fpsFlow.collectAsState()
    val ping by pingFlow.collectAsState()
    val temp by tempFlow.collectAsState()
    val status by statusFlow.collectAsState()
    val live by liveFlow.collectAsState()

    val inf = rememberInfiniteTransition(label = "sb")
    val pulse by inf.animateFloat(0.75f, 1.25f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "p")

    val panelShape = RoundedCornerShape(18.dp)
    val isRight = side == SidebarSide.RIGHT

    Box(contentAlignment = if (isRight) Alignment.CenterEnd else Alignment.CenterStart) {

        // ---- Panel isi (geser keluar) ----
        AnimatedVisibility(
            visible = expanded,
            enter = slideInHorizontally(
                initialOffsetX = { if (isRight) it else -it },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { if (isRight) it else -it },
                animationSpec = tween(180)
            ) + fadeOut()
        ) {
            Column(
                Modifier
                    .size(232.dp, 460.dp)
                    .clip(panelShape)
                    .background(
                        Brush.horizontalGradient(
                            if (isRight) listOf(Graphite.copy(0.97f), Obsidian.copy(0.98f))
                            else listOf(Obsidian.copy(0.98f), Graphite.copy(0.97f))
                        )
                    )
                    .border(1.dp, Cobalt.copy(0.55f * pulse), panelShape)
                    .padding(14.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("VELTRA", color = Titanium, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(6.dp).clip(CircleShape)
                        .background(if (status == "STABLE") NeoMint else Amber))
                    Spacer(Modifier.width(8.dp))
                    Text("›", color = Dim, fontSize = 14.sp,
                        modifier = Modifier.clickable { onExpandChange(false) })
                    Spacer(Modifier.width(8.dp))
                    Text("✕", color = Dim, fontSize = 11.sp,
                        modifier = Modifier.clickable { onClose() })
                }

                Spacer(Modifier.height(12.dp))
                live?.let { LiveChipSb(alive = it); Spacer(Modifier.height(12.dp)) }

                // FPS mini gauge
                Box(Modifier.fillMaxWidth().height(104.dp),
                    contentAlignment = Alignment.Center) { FpsGaugeSb(fps, fps >= 50, pulse) }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricSb("PING", "${if (ping < 0) "--" else ping}ms",
                        if (ping in 1..60) NeoMint else Amber, Modifier.weight(1f))
                    MetricSb("TEMP", "${temp}°C",
                        if (temp <= 42) NeoMint else Amber, Modifier.weight(1f))
                }

                Spacer(Modifier.height(10.dp))
                Text("TOGGLES", color = Dim, fontSize = 8.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                QuickTogglesSb()

                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(HexagonShape)
                        .background(Brush.horizontalGradient(listOf(Cobalt, IonCyan.copy(0.9f))))
                        .clickable { onStabilize() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 13.sp)
                        Spacer(Modifier.width(7.dp))
                        Text("STABILIZE", color = Color.White, fontWeight = FontWeight.Bold,
                            letterSpacing = 2.5.sp, fontSize = 12.5.sp)
                    }
                }
            }
        }

        // ---- Handle (selalu kelihatan) ----
        Column(
            Modifier
                .size(20.dp, 118.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Cobalt.copy(0.92f), IonCyan.copy(0.75f))
                    )
                )
                .drawBehindGlow(IonCyan, pulse)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount -> onDrag(dragAmount.y) }
                }
                .clickable { onExpandChange(!expanded) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("V", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Box(Modifier.width(2.dp).height(26.dp).background(Color.White.copy(0.55f)))
            Spacer(Modifier.height(5.dp))
            Text(if (expanded) "›" else "‹", color = Color.White, fontSize = 10.sp)
        }
    }
}

/** Glow tipis di sekeliling handle biar kelihatan "nyala" tanpa norak. */
private fun Modifier.drawBehindGlow(color: Color, pulse: Float) = this
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRoundRect(
                color = color.copy(alpha = 0.35f * pulse),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(width = 1.4f * pulse)
            )
        }
    }

@Composable
private fun FpsGaugeSb(fps: Int, good: Boolean, pulse: Float) {
    val color = if (good) IonCyan else Amber
    val sweep by animateFloatAsState(
        targetValue = (fps.coerceIn(0, 120) / 120f) * 270f,
        animationSpec = tween(400), label = "sweep"
    )
    Box(contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(104.dp)) {
            drawArc(Color.White.copy(0.07f), 135f, 270f, false,
                style = Stroke(width = 10f, cap = StrokeCap.Round))
            drawArc(
                brush = Brush.sweepGradient(listOf(color.copy(0.3f), color)),
                startAngle = 135f, sweepAngle = sweep, useCenter = false,
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(0.16f * pulse), Color.Transparent)
                ),
                radius = 48f * pulse,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$fps", color = Titanium, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                fontFamily = Mono)
            Text("FPS", color = Dim, fontSize = 8.sp, letterSpacing = 3.sp)
        }
    }
}

@Composable
private fun MetricSb(label: String, value: String, tint: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(0.045f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = Dim, fontSize = 8.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            fontFamily = Mono)
    }
}

@Composable
private fun QuickTogglesSb() {
    val items = listOf("PERF", "FOCUS", "NET", "TOUCH", "REC", "BRIGHT", "DND", "COOL")
    var states by remember { mutableStateOf(listOf(true, true, true, false, false, false, true, true)) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0..3) {
                    val i = row * 4 + col
                    val on = states[i]
                    Box(
                        Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (on) NeoMint.copy(0.13f) else Color.White.copy(0.04f))
                            .border(1.dp, if (on) NeoMint.copy(0.55f) else Color.White.copy(0.06f),
                                RoundedCornerShape(8.dp))
                            .clickable { states = states.toMutableList().also { it[i] = !on } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(items[i], color = if (on) NeoMint else Dim, fontSize = 7.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveChipSb(alive: Boolean) {
    val blink = rememberInfiniteTransition(label = "sb-live")
    val a by blink.animateFloat(0.4f, 1f,
        infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "b")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(LiveRed.copy(if (alive) 0.16f else 0.10f * a))
            .border(1.dp, LiveRed.copy(0.45f * a), RoundedCornerShape(9.dp))
            .padding(vertical = 7.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape)
            .background(if (alive) LiveRed else Dim.copy(a)))
        Spacer(Modifier.width(8.dp))
        Text(
            if (alive) "LIVE — TERJAGA" else "LIVE PUTUS",
            color = if (alive) Color(0xFFFF6B85) else Dim,
            fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
        )
    }
}

/** Sisi tempat sidebar nempel. */
enum class SidebarSide { LEFT, RIGHT }

/** Gaya panel: HUD mengambang (bisa dipindah) atau sidebar tepi layar (gaya ROG). */
enum class PanelMode { HUD, SIDEBAR }
