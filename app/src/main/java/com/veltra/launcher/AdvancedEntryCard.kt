package com.veltra.launcher

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veltra.core.VeltraColors

/**
 * Pintu masuk MODE LANJUT di dashboard — jelas, besar, dan jujur soal statusnya.
 * Shizuku/root sekarang fitur utama, bukan lagi menu karang di Settings.
 *
 * Catatan performa: cuma SATU elemen di dashboard yang punya animasi berjalan
 * (kilau menyapu di kartu ini). Sisanya statis.
 */
@Composable
fun AdvancedEntryCard(
    vm: LauncherViewModel = hiltViewModel(),
    onClick: () -> Unit
) {
    val level by vm.advancedLabel
    val ready = vm.advancedReady

    val accent = when {
        level.contains("Root", true) -> VeltraColors.NeoMint
        level.contains("Shizuku", true) -> VeltraColors.IonCyan
        else -> VeltraColors.Amber
    }

    val inf = rememberInfiniteTransition(label = "advCard")
    val sweep by inf.animateFloat(
        -0.35f, 1.35f,
        infiniteRepeatable(tween(4600, easing = LinearEasing)), label = "sweep"
    )

    val shape = RoundedCornerShape(16.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VeltraColors.Graphite)
            .border(1.dp, if (ready) accent.copy(.45f) else VeltraColors.Border, shape)
            .cardClick { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(shape = RoundedCornerShape(12.dp))
                    .background(accent.copy(.13f))
                    .border(1.dp, accent.copy(.42f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (ready) Icons.Rounded.Verified else Icons.Rounded.Shield,
                    null, tint = accent, modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("MODE LANJUT", color = VeltraColors.Titanium, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(3.dp))
                Text(level, color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (ready) "Game mode, kunci Hz, proteksi live, pangkas cache"
                    else "Aktifkan Shizuku buat buka semua aksi sistem",
                    color = VeltraColors.Muted, fontSize = 9.sp, lineHeight = 13.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (ready) listOf(accent.copy(.20f), accent.copy(.10f))
                            else listOf(VeltraColors.Cobalt, Color(0xFF1B4FD8))
                        )
                    )
                    .border(
                        1.dp,
                        if (ready) accent.copy(.5f) else VeltraColors.IonCyan.copy(.35f),
                        RoundedCornerShape(11.dp)
                    )
                    .then(
                        Modifier.background(
                            // kilau menyapu pelan biar kartu ini kerasa "hidup"
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(.13f), Color.Transparent),
                                start = Offset(160f * sweep - 60f, 0f),
                                end = Offset(160f * sweep, 60f)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(if (ready) "BUKA" else "AKTIFKAN",
                    color = if (ready) accent else Color.White,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            }
        }
    }
}
