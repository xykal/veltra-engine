package com.veltra.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veltra.boost.live.OemGuide
import com.veltra.boost.live.StreamApps
import com.veltra.core.VeltraColors

private val LiveRed = Color(0xFFFF3B5C)

/**
 * LIVE GUARD — kartu khusus buat:
 * "live TikTok mati sendiri pas lagi main game di HP kentang."
 */
@Composable
fun LiveGuardCard(
    vm: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val live by vm.liveAlive.collectAsState()
    val memory by vm.memory.collectAsState()
    val checks by vm.checks.collectAsState()
    val apps = vm.streamApps
    val detected by vm.detectedLive.collectAsState()
    var showOem by remember { mutableStateOf(false) }

    VeltraCard(
        modifier = modifier,
        title = "LIVE GUARD",
        subtitle = memory?.let { m ->
            "RAM ${m.availMb}MB bebas dari ${m.totalMb}MB • target FPS disarankan ${vm.recommendedFps}"
        },
        icon = Icons.Rounded.LiveTv,
        accent = LiveRed,
        action = {
            if (live != null) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            (if (live == true) VeltraColors.NeoMint else VeltraColors.Amber).copy(.13f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusDot(if (live == true) VeltraColors.NeoMint else VeltraColors.Amber)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (live == true) "TERJAGA" else "PUTUS",
                        color = if (live == true) VeltraColors.NeoMint else VeltraColors.Amber,
                        fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                    )
                }
            }
        }
    ) {
        // ── Deteksi live otomatis ────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF101720))
                .border(1.dp, VeltraColors.Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp)) {
                StatusDot(if (detected != null) VeltraColors.NeoMint else VeltraColors.Muted)
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (detected != null) "TERDETEKSI: ${StreamApps.label(detected!!)}"
                    else "Deteksi otomatis: tidak ada siaran",
                    color = if (detected != null) VeltraColors.NeoMint else VeltraColors.Titanium,
                    fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Baca dari notifikasi yang berjalan (tanpa daftar manual). " +
                            "Perlu izin Akses Notifikasi.",
                    color = VeltraColors.Muted, fontSize = 9.sp, lineHeight = 12.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "IZINKAN",
                color = VeltraColors.IonCyan, fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                modifier = Modifier.clickable { vm.openNotificationAccess() }
            )
        }

        // ── Preflight ───────────────────────────────────────────
        if (checks.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            checks.forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (c.ok) "✓" else "!",
                        color = if (c.ok) VeltraColors.NeoMint else VeltraColors.Amber,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(c.title, color = VeltraColors.Titanium, fontSize = 11.sp,
                        modifier = Modifier.weight(1f))
                    if (!c.ok && c.action != null) {
                        Text(
                            "PERBAIKI",
                            color = VeltraColors.IonCyan, fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { vm.fix(c) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        // ── Tombol jaga ─────────────────────────────────────────
        if (apps.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                apps.take(2).forEach { pkg ->
                    val active = live == true
                    VeltraPrimitives.GhostChip(
                        label = if (active) "STOP" else "JAGA ${StreamApps.label(pkg).uppercase()}",
                        selected = !active,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (active) vm.stopLiveGuard() else vm.startLiveGuard(pkg)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            if (showOem) "▾  Tutup panduan ${OemGuide.detect()}"
            else "▸  Kenapa live gue masih putus? (${OemGuide.detect()})",
            color = VeltraColors.IonCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { showOem = !showOem }
        )
        if (showOem) {
            Spacer(Modifier.height(8.dp))
            OemGuide.steps().forEach { step ->
                Text(step.title, color = VeltraColors.Titanium, fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold)
                Text(step.detail, color = VeltraColors.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                Spacer(Modifier.height(8.dp))
            }
            Text(
                "Sumber: dontkillmyapp.com — ROM pabrik sering lebih ganas dari Android standar.",
                color = Color(0xFF6B7588), fontSize = 9.sp
            )
        }
    }
}
