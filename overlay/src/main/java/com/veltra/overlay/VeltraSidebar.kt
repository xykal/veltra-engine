package com.veltra.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsStateimport androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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

/**
 * PANEL GAYA ROG / GAME GENIE — sidebar nempel di tepi layar.
 *
 * Perbaikan dari keluhan pemakaian nyata:
 *  - Tombolnya **kotak 1:1 (72dp)** dengan ikon + teks, bukan baris gepeng yang
 *    ketutup isi panel. Nggak ada lagi tombol yang "gepeng nggak ketampil".
 *  - Isinya lengkap: STABILKAN • CROSSHAIR • SENYAP • FPS • BUKA VELTRA • TUTUP,
 *    plus metrik FPS/PING/SUHU/RAM dan status LIVE.
 *  - Handle tetap bisa diseret naik-turun.
 *
 * Satu-satunya animasi yang berjalan terus ada di **satu** elemen (cincin FPS).
 * Sisanya statis — sengaja, biar nggak makan CPU saat game jalan.
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
    ramFlow: StateFlow<Int>? = null,
    crosshairOn: Boolean = false,
    quietOn: Boolean = false,
    targetFps: Int = 60,
    onStabilize: () -> Unit,
    onCrosshair: () -> Unit,
    onQuiet: () -> Unit,
    onFps: (Int) -> Unit,
    onOpenApp: () -> Unit,
    onClose: () -> Unit,
    side: SidebarSide
) {
    val fps by fpsFlow.collectAsState()
    val ping by pingFlow.collectAsState()
    val temp by tempFlow.collectAsState()
    val status by statusFlow.collectAsState()
    val live by liveFlow.collectAsState()
    val ramFree by (ramFlow ?: kotlinx.coroutines.flow.MutableStateFlow(-1)).collectAsState()

    val isRight = side == SidebarSide.RIGHT
    val panelShape = RoundedCornerShape(18.dp)

    Box(contentAlignment = if (isRight) Alignment.CenterEnd else Alignment.CenterStart) {

        // ── Panel isi ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = slideInHorizontally(
                initialOffsetX = { if (isRight) it else -it },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(160)),
            exit = slideOutHorizontally(
                targetOffsetX = { if (isRight) it else -it },
                animationSpec = tween(160)
            ) + fadeOut(tween(140))
        ) {
            Column(
                Modifier
                    .size(268.dp, 476.dp)
                    .clip(panelShape)
                    .background(
                        Brush.horizontalGradient(
                            if (isRight) listOf(Graphite.copy(0.97f), Obsidian.copy(0.98f))
                            else listOf(Obsidian.copy(0.98f), Graphite.copy(0.97f))
                        )
                    )
                    .border(1.dp, Cobalt.copy(0.5f), panelShape)
                    .padding(13.dp)
            ) {
                // Kepala
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("VELTRA", color = Titanium, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Spacer(Modifier.width(7.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape)
                        .background(if (status == "STABLE") NeoMint else Amber))
                    Spacer(Modifier.weight(1f))
                    Text(status, color = Dim, fontSize = 8.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("›", color = Dim, fontSize = 15.sp,
                        modifier = Modifier.clickable { onExpandChange(false) })
                    Spacer(Modifier.width(9.dp))
                    Icon(Icons.Rounded.Close, null, tint = Dim,
                        modifier = Modifier.size(13.dp).clickable { onClose() })
                }

                Spacer(Modifier.height(10.dp))
                live?.let { LiveChip(alive = it) }

                Spacer(Modifier.height(10.dp))

                // Hero: gauge FPS + 2 metrik
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                        FpsGauge(fps, fps >= 50)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        StatBox("PING", "${if (ping < 0) "--" else ping}ms",
                            if (ping in 1..80) NeoMint else if (ping <= 0) Muted else Amber)
                        Spacer(Modifier.height(7.dp))
                        StatBox("SUHU", "${if (temp <= 0) "--" else temp}°C",
                            when { temp <= 0 -> Muted; temp >= 45 -> LiveRed; temp >= 41 -> Amber; else -> NeoMint })
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Tombol aksi: kotak 1:1, 3 kolom ────────────────────
                Row(Modifier.fillMaxWidth()) {
                    ActionTile("STABILKAN", Icons.Rounded.Bolt, NeoMint, Modifier.weight(1f)) { onStabilize() }
                    Spacer(Modifier.width(8.dp))
                    ActionTile("CROSSHAIR", Icons.Rounded.CenterFocusStrong,
                        if (crosshairOn) NeoMint else Titanium, Modifier.weight(1f)) { onCrosshair() }
                    Spacer(Modifier.width(8.dp))
                    ActionTile("SENYAP", Icons.Rounded.DoNotDisturbOn,
                        if (quietOn) Amber else Titanium, Modifier.weight(1f)) { onQuiet() }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    ActionTile("FPS $targetFps", Icons.Rounded.Speed, IonCyan, Modifier.weight(1f)) {
                        onFps(if (targetFps >= 60) 30 else 60)
                    }
                    Spacer(Modifier.width(8.dp))
                    ActionTile("BUKA", Icons.Rounded.OpenInNew, Titanium, Modifier.weight(1f)) { onOpenApp() }
                    Spacer(Modifier.width(8.dp))
                    ActionTile("TUTUP", Icons.Rounded.Close, LiveRed, Modifier.weight(1f)) { onClose() }
                }

                Spacer(Modifier.height(12.dp))

                // RAM
                RamStrip(ramFree)

                Spacer(Modifier.weight(1f))
                Text("VELTRA • ${if (isRight) "kanan" else "kiri"} • geser handle buat pindah",
                    color = Dim, fontSize = 8.sp)
            }
        }

        // ── Handle (nempel di tepi) ───────────────────────────────────
        if (!expanded) {
            Column(
                Modifier
                    .size(40.dp, 128.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Graphite.copy(.95f), Obsidian.copy(.95f))
                        )
                    )
                    .border(1.dp, Cobalt.copy(.45f), RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            onDrag(drag.y)
                        }
                    }
                    .clickable { onExpandChange(true) }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("$fps", color = if (fps >= 50) NeoMint else Amber,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
                Text("FPS", color = Muted, fontSize = 7.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(2.dp).height(20.dp).background(Color.White.copy(0.35f)))
                Spacer(Modifier.height(6.dp))
                Text(if (isRight) "‹" else "›", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

/* ─────────────── KOMPONEN PANEL ─────────────── */

@Composable
private fun FpsGauge(fps: Int, good: Boolean) {
    val color = if (fps >= 50) NeoMint else if (fps >= 30) Amber else LiveRed
    val target = (fps.coerceIn(0, 120) / 120f) * 270f
    val sweep by animateFloatAsState(target, tween(350), label = "fps")

    Box(contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val w = 8.dp.toPx()
            drawArc(Color.White.copy(.07f), 135f, 270f, false, style = Stroke(w, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(listOf(color.copy(.35f), color)),
                135f, sweep, false,
                style = Stroke(w, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$fps", color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                fontFamily = Mono)
            Text("FPS", color = Muted, fontSize = 8.sp, letterSpacing = 1.6.sp)
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFF101720))
            .border(1.dp, Color(0xFF232A38), RoundedCornerShape(11.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(label, color = Muted, fontSize = 8.sp, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            fontFamily = Mono)
    }
}

/** Tombol aksi kotak — ikon di atas, teks di bawah. Nggak ada yang gepeng. */
@Composable
private fun ActionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.93f else 1f, tween(90), label = "tile")

    Column(
        modifier
            .scale(scale)
            .aspectRatio(1f)
            .defaultMinSize(minHeight = 66.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (isPressed) Color(0xFF16202C) else Color(0xFF101720))
            .border(1.dp, Color(0xFF232A38), RoundedCornerShape(13.dp))
            .clickable(src, null) { onClick() }
            .padding(vertical = 9.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, color = tint, fontSize = 8.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp, maxLines = 1)
    }
}

@Composable
private fun LiveChip(alive: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (alive) NeoMint.copy(.12f) else LiveRed.copy(.14f))
            .border(1.dp, if (alive) NeoMint.copy(.5f) else LiveRed.copy(.6f),
                RoundedCornerShape(11.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape)
            .background(if (alive) NeoMint else LiveRed))
        Spacer(Modifier.width(8.dp))
        Text(if (alive) "LIVE TERJAGA" else "LIVE PUTUS",
            color = if (alive) NeoMint else LiveRed,
            fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun RamStrip(freeMb: Int) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("RAM", color = Muted, fontSize = 8.sp, letterSpacing = 1.4.sp)
            Spacer(Modifier.weight(1f))
            Text(if (freeMb < 0) "--" else "bebas ${freeMb}MB",
                color = if (freeMb in 1..700) Amber else Titanium,
                fontSize = 9.sp, fontFamily = Mono)
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(.07f))
        ) {
            val ratio = if (freeMb <= 0) 0.5f else (freeMb / 4096f).coerceIn(0.05f, 1f)
            Box(
                Modifier
                    .fillMaxWidth(ratio)
                    .height(5.dp)
                    .background(Brush.horizontalGradient(listOf(Cobalt, IonCyan)))
            )
        }
    }
}
