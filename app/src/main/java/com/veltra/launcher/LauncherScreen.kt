package com.veltra.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veltra.core.VeltraColors

private val Mono = FontFamily.Monospace

/**
 * Dashboard VELTRA — mengikuti referensi visual:
 * header wordmark + subtitle, kartu game dengan monogram gradien & metrik mono,
 * grafik performa di bawah, dan bar STABILIZE cobalt menyala.
 */
@Composable
fun LauncherScreen(
    onSettings: () -> Unit,
    vm: LauncherViewModel = hiltViewModel()
) {
    val games by vm.games.collectAsState()
    val stabilizing by vm.stabilizing.collectAsState()
    val memory by vm.memory.collectAsState()

    Scaffold(
        containerColor = VeltraColors.Obsidian,
        bottomBar = {
            StabilizeBar(
                stabilizing = stabilizing,
                gameCount = games.size,
                onClick = { vm.stabilizeAll() }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .background(VeltraColors.Obsidian)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(26.dp))

            // ---- Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "VELTRA",
                        color = VeltraColors.Titanium,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "GAME PERFORMANCE SYSTEM",
                        color = VeltraColors.Muted,
                        fontSize = 9.sp,
                        letterSpacing = 3.5.sp
                    )
                }
                // Status pill
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VeltraColors.Graphite)
                        .border(1.dp, VeltraColors.Border, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape)
                        .background(if (stabilizing) VeltraColors.NeoMint else VeltraColors.IonCyan))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (stabilizing) "OPTIMAL" else "READY",
                        color = VeltraColors.Titanium,
                        fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(VeltraColors.Graphite)
                        .border(1.dp, VeltraColors.Border, RoundedCornerShape(11.dp))
                        .clickable { onSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚙", color = VeltraColors.Muted, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---- RAM strip
            memory?.let { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VeltraColors.Graphite)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RAM", color = VeltraColors.Muted, fontSize = 9.sp,
                        letterSpacing = 2.sp)
                    Spacer(Modifier.width(10.dp))
                    RamBar(usedRatio = 1f - (m.availMb.toFloat() / m.totalMb.toFloat()))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${m.availMb}/${m.totalMb}MB",
                        color = VeltraColors.Titanium,
                        fontSize = 10.sp, fontFamily = Mono
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            PermissionCard(vm = vm)
            Spacer(Modifier.height(12.dp))
            LiveGuardCard(vm = vm)
            Spacer(Modifier.height(18.dp))

            Text(
                "GAMES  •  ${games.size}",
                color = VeltraColors.Muted,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(games, key = { it.packageName }) { g ->
                    GameCard(g) { vm.launch(g.packageName) }
                }
            }

            Spacer(Modifier.height(12.dp))
            PerformanceGraph(vm = vm)
        }
    }
}

@Composable
private fun RowScope.RamBar(usedRatio: Float) {
    Box(
        Modifier
            .weight(1f)
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(0.07f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(usedRatio.coerceIn(0.05f, 1f))
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(VeltraColors.Cobalt, VeltraColors.IonCyan)
                    )
                )
        )
    }
}

@Composable
private fun GameCard(game: GameInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VeltraColors.Graphite),
        border = androidx.compose.foundation.BorderStroke(1.dp, VeltraColors.Border)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(VeltraColors.Cobalt, VeltraColors.IonCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        game.name.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        game.name,
                        color = VeltraColors.Titanium,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${game.lastFps} FPS  •  ${game.ping}ms",
                        color = VeltraColors.Muted,
                        fontSize = 9.5.sp,
                        fontFamily = Mono
                    )
                }
            }
            Spacer(Modifier.height(11.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(VeltraColors.Cobalt.copy(0.14f))
                    .padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "LAUNCH",
                    color = VeltraColors.IonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

/** Grafik performa bawah — sparkline FPS terakhir. */
@Composable
private fun PerformanceGraph(vm: LauncherViewModel) {
    val history by vm.fpsHistory.collectAsState()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VeltraColors.Graphite)
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PERFORMANCE", color = VeltraColors.Muted, fontSize = 9.sp,
                letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "${history.lastOrNull() ?: 60} FPS",
                color = VeltraColors.IonCyan, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, fontFamily = Mono
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(38.dp)) {
            if (history.size < 2) {
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        drawLine(
                            color = Color.White.copy(0.06f),
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                )
                Text(
                    "menunggu data sesi game…",
                    color = Color(0xFF6B7588),
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    val max = (history.maxOrNull() ?: 60).coerceAtLeast(60)
                    val step = size.width / (history.size - 1)
                    val pts = history.mapIndexed { i, v ->
                        Offset(i * step, size.height - (v.toFloat() / max) * size.height)
                    }
                    // area fill
                    val path = Path().apply {
                        moveTo(pts.first().x, size.height)
                        pts.forEach { lineTo(it.x, it.y) }
                        lineTo(pts.last().x, size.height)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            listOf(VeltraColors.IonCyan.copy(0.28f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            listOf(VeltraColors.IonCyan.copy(0.28f), Color.Transparent)
                        ),
                        style = Stroke(width = 2f)
                    )
                    for (i in 0 until pts.size - 1) {
                        drawLine(
                            color = VeltraColors.IonCyan,
                            start = pts[i], end = pts[i + 1],
                            strokeWidth = 2.5f, cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StabilizeBar(
    stabilizing: Boolean,
    gameCount: Int,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(VeltraColors.Obsidian)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        if (stabilizing)
                            listOf(VeltraColors.NeoMint, VeltraColors.NeoMint.copy(0.75f))
                        else
                            listOf(VeltraColors.Cobalt, VeltraColors.IonCyan.copy(0.85f))
                    )
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (stabilizing) "✓" else "⚡", fontSize = 14.sp)
                Spacer(Modifier.width(9.dp))
                Text(
                    if (stabilizing) "STABLE — $gameCount GAME OPTIMAL"
                    else "STABILIZE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                    fontSize = 13.sp
                )
            }
        }
    }
}
