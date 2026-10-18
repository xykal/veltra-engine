package com.veltra.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veltra.core.VeltraColors

/**
 * Dashboard VELTRA — LOCKED DESIGN.
 * Obsidian bg, graphite card, cobalt STABILIZE, mono metrics.
 */
@Composable
fun LauncherScreen(onSettings: () -> Unit, vm: LauncherViewModel = hiltViewModel()) {
    val games by vm.games.collectAsState()
    val stabilizing by vm.stabilizing.collectAsState()

    Scaffold(
        containerColor = VeltraColors.Obsidian,
        bottomBar = { StabilizeBar(stabilizing) { vm.stabilizeAll() } }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .background(VeltraColors.Obsidian)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(28.dp))
            Text(
                "VELTRA",
                color = VeltraColors.Titanium,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )
            Text(
                "GAME PERFORMANCE SYSTEM",
                color = VeltraColors.Muted,
                fontSize = 10.sp,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(16.dp))
            PermissionCard(vm = vm)
            Spacer(Modifier.height(12.dp))
            LiveGuardCard(vm = vm)
            Spacer(Modifier.height(18.dp))
            Text(
                "${games.size} GAMES DETECTED",
                color = VeltraColors.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(games.size) { i ->
                    val g = games[i]
                    GameCard(g) { vm.launch(g.packageName) }
                }
            }
        }
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
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(VeltraColors.Cobalt, VeltraColors.IonCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(game.name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(game.name, color = VeltraColors.Titanium, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "${game.lastFps} FPS  •  ${game.ping}ms",
                color = VeltraColors.Muted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun StabilizeBar(active: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(VeltraColors.Obsidian).padding(20.dp)) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) VeltraColors.NeoMint else VeltraColors.Cobalt
            )
        ) {
            Text(
                if (active) "STABLE — ALL SYSTEMS OPTIMAL" else "STABILIZE",
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}
