package com.veltra.launcher

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veltra.boost.live.LiveGuard
import com.veltra.boost.live.OemGuide
import com.veltra.boost.live.StreamApps
import com.veltra.core.VeltraColors

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
    var showOem by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VeltraColors.Graphite),
        border = androidx.compose.foundation.BorderStroke(1.dp, VeltraColors.Border)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF3B5C)))
                Spacer(Modifier.width(8.dp))
                Text(
                    "LIVE GUARD",
                    color = VeltraColors.Titanium,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                live?.let { alive ->
                    Text(
                        if (alive) "TERJAGA" else "PUTUS",
                        color = if (alive) VeltraColors.NeoMint else VeltraColors.Amber,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            memory?.let { m ->
                Text(
                    "RAM ${m.availMb}MB bebas dari ${m.totalMb}MB • " +
                            "target FPS game disarankan ${vm.recommendedFps}",
                    color = VeltraColors.Muted, fontSize = 10.sp
                )
            }

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

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                apps.take(2).forEach { pkg ->
                    val active = live == true
                    Button(
                        onClick = { if (active) vm.stopLiveGuard() else vm.startLiveGuard(pkg) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) VeltraColors.Graphite else Color(0xFFFF3B5C),
                            contentColor = Color.White
                        ),
                        border = if (active)
                            androidx.compose.foundation.BorderStroke(1.dp, VeltraColors.Border)
                        else null
                    ) {
                        Text(
                            if (active) "STOP" else "JAGA ${StreamApps.label(pkg).uppercase()}",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                if (showOem) "▾  Tutup panduan ${OemGuide.detect()}" else "▸  Kenapa live gue masih putus? (${OemGuide.detect()})",
                color = VeltraColors.IonCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { showOem = !showOem }
            )
            if (showOem) {
                Spacer(Modifier.height(8.dp))
                OemGuide.steps().forEach { step ->
                    Text(step.title, color = VeltraColors.Titanium, fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text(step.detail, color = VeltraColors.Muted, fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "Sumber: dontkillmyapp.com — ROM pabrik sering lebih ganas dari Android standar.",
                    color = Color(0xFF6B7588), fontSize = 9.sp
                )
            }
        }
    }
}
