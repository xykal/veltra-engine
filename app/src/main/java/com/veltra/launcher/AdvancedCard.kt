package com.veltra.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veltra.core.VeltraColors

/** Aksi lanjut yang butuh Shizuku/ADB atau root. */
enum class AdvancedAction {
    PROTECT_LIVE, COOLING, RES_DOWN, RES_RESET, FREEZE_BLOAT, TURBO_GAME, RESET_GAME, COMPILE_GAME
}

@Composable
fun AdvancedCard(
    levelLabel: String,
    ready: Boolean,
    lastResult: String,
    onAction: (AdvancedAction) -> Unit,
    onRequestPermission: () -> Unit = {},
    onRefresh: () -> Unit = {},
    gameOptions: List<Pair<String, String>> = emptyList(),   // packageName → nama
    targetPkg: String = "com.dts.freefiremax",
    onTarget: (String) -> Unit = {}
) {
    val accent = when {
        levelLabel.contains("Root", true) -> VeltraColors.NeoMint
        levelLabel.contains("Shizuku", true) -> VeltraColors.IonCyan
        else -> VeltraColors.Amber
    }

    VeltraCard(
        title = "MODE LANJUT",
        subtitle = levelLabel,
        icon = Icons.Rounded.Memory,
        accent = accent,
        action = {
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(.13f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(accent)
                Spacer(Modifier.width(5.dp))
                Text(
                    if (ready) "AKTIF" else "NONAKTIF",
                    color = accent, fontSize = 8.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                )
            }
        }
    ) {
        Text(
            if (ready)
                "Akses aktif. Aksi di bawah mengubah pengaturan sistem — semua bisa dibalik."
            else
                "Tanpa akses tambahan, fitur ini nonaktif. Pasang Shizuku lalu hidupkan lewat " +
                        "Wireless Debugging, atau root, untuk membukanya. Fitur utama VELTRA " +
                        "tetap jalan tanpa ini.",
            color = VeltraColors.Muted, fontSize = 9.5.sp, lineHeight = 13.sp
        )

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VeltraPrimitives.GhostChip("MINTA IZIN", enabled = true,
                modifier = Modifier.weight(1f)) { onRequestPermission() }
            VeltraPrimitives.GhostChip("REFRESH", enabled = true,
                modifier = Modifier.weight(1f)) { onRefresh() }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvButton("🔒 PROTEKSI LIVE", "kunci aplikasi live dari doze & standby",
                ready, Modifier.weight(1f)) { onAction(AdvancedAction.PROTECT_LIVE) }
            AdvButton("❄ HEMAT PANAS", "kurangi sumber panas",
                ready, Modifier.weight(1f)) { onAction(AdvancedAction.COOLING) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvButton("🎮 RES TURUN (FPS↑)", "render lebih sedikit piksel",
                ready, Modifier.weight(1f)) { onAction(AdvancedAction.RES_DOWN) }
            AdvButton("↺ RESET RES", "balikin resolusi pabrik",
                ready, Modifier.weight(1f)) { onAction(AdvancedAction.RES_RESET) }
        }
        Spacer(Modifier.height(8.dp))
        AdvButton("🧹 STOP BLOATWARE", "force-stop aplikasi sampah",
            ready, Modifier.fillMaxWidth()) { onAction(AdvancedAction.FREEZE_BLOAT) }

        if (lastResult.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                lastResult,
                color = if (lastResult.startsWith("Gagal") || lastResult.contains("gagal"))
                    VeltraColors.Amber else VeltraColors.NeoMint,
                fontSize = 9.5.sp, lineHeight = 13.sp
            )
        }

        // ── INJEKSI GAME MODE ────────────────────────────────────
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(VeltraColors.Border))
        Spacer(Modifier.height(11.dp))

        val inj = VeltraColors.IonCyan
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(inj.copy(.13f))
                    .border(1.dp, inj.copy(.38f), RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text("INJEKSI GAME MODE", color = VeltraColors.Titanium, fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "Menyuruh sistem Android menjalankan game ini di mode PERFORMANCE lewat " +
                    "Game Mode API (cmd game mode) — persis cara kerja Game Dashboard/Game Genie " +
                    "bawaan. Turunan resolusi (downscale) + batas FPS juga bisa disetel. " +
                    "Perlu restart game setelah di-inject.",
            color = VeltraColors.Muted, fontSize = 9.5.sp, lineHeight = 13.sp
        )

        if (gameOptions.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            Text("TARGET", color = VeltraColors.Muted, fontSize = 8.5.sp, letterSpacing = 1.6.sp)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                gameOptions.take(8).forEach { (pkg, name) ->
                    val on = pkg == targetPkg
                    Box(
                        Modifier
                            .padding(end = 7.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) VeltraColors.IonCyan.copy(.14f) else Color(0xFF10161F))
                            .border(1.dp, if (on) VeltraColors.IonCyan else VeltraColors.Border,
                                RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .cardClick { onTarget(pkg) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(name, color = if (on) VeltraColors.IonCyan else VeltraColors.Titanium,
                            fontSize = 10.5.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(targetPkg, color = Color(0xFF6B7588), fontSize = 9.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvButton("⚡ INJECT TURBO", "performance + downscale 75%",
                ready, Modifier.weight(1f)) { onAction(AdvancedAction.TURBO_GAME) }
            AdvButton("↺ RESET GAME MODE", "balik ke standar",
                ready, Modifier.weight(1f)) { onAction(AdvancedAction.RESET_GAME) }
        }
        Spacer(Modifier.height(8.dp))
        AdvButton("⚙ KOMPILASI SPEED", "AOT compile: frame lebih konsisten",
            ready, Modifier.fillMaxWidth()) { onAction(AdvancedAction.COMPILE_GAME) }

        Spacer(Modifier.height(10.dp))
        Text(
            "Jujur: VELTRA tidak bisa mendinginkan HP dan tidak bisa menciptakan FPS. " +
                    "Yang dilakukan: mengurangi beban (resolusi, animasi, proses liar) supaya " +
                    "suhu turun, throttle hilang, dan frame-time konsisten. Itu yang kerasa " +
                    "jadi “nggak patah-patah”.",
            color = Color(0xFF6B7588), fontSize = 9.sp, lineHeight = 13.sp
        )
    }
}

@Composable
private fun AdvButton(
    title: String,
    subtitle: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) VeltraColors.Cobalt.copy(0.14f) else Color.White.copy(0.03f)
            )
            .border(
                1.dp,
                if (enabled) VeltraColors.Cobalt.copy(0.45f) else Color.White.copy(0.06f),
                RoundedCornerShape(12.dp)
            )
            .cardClick(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(title, color = VeltraColors.Titanium.copy(alpha), fontSize = 10.sp,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = Color(0xFF6B7588).copy(alpha), fontSize = 8.5.sp)
    }
}
