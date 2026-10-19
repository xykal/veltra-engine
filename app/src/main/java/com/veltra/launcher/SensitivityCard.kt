package com.veltra.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veltra.core.VeltraColors

/**
 * KUNCI LAYAR & FPS + profil sensitivitas.
 *
 * Jujur dari awal:
 *  - **Sensitivitas game tidak bisa diubah VELTRA.** Nilainya disimpan di data
 *    game; menulis ke sana = modifikasi game = risiko banned. VELTRA cuma
 *    menyimpan catatan angka lu biar gampang dipindah antar HP.
 *  - Yang BISA dikunci: **refresh rate layar** (60/90/120/144Hz, butuh
 *    Shizuku/root) dan **target FPS game** lewat Game Mode intervention.
 *  - Kalau panel HP lu cuma 60Hz, nggak ada aplikasi yang bisa bikin jadi 120Hz.
 */
@Composable
fun SensitivityCard(
    lockedHz: Int,
    advancedReady: Boolean,
    statusText: String,
    targetPkg: String,
    targetName: String,
    sensitivity: Map<String, String>,
    onLockHz: (Int) -> Unit,
    onUnlockHz: () -> Unit,
    onReadHz: () -> Unit,
    onLockFps: (Int) -> Unit,
    onUnlockFps: () -> Unit,
    onSaveSensitivity: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var note by remember(targetPkg) { mutableStateOf(sensitivity[targetPkg].orEmpty()) }

    VeltraCard(
        modifier = modifier,
        title = "KUNCI LAYAR & FPS",
        subtitle = if (lockedHz > 0) "Refresh rate terkunci ${lockedHz}Hz" else "Mengikuti sistem",
        icon = Icons.Rounded.Speed,
        accent = VeltraColors.Cobalt,
        action = {
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        (if (lockedHz > 0) VeltraColors.NeoMint else VeltraColors.Muted).copy(.13f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(if (lockedHz > 0) VeltraColors.NeoMint else VeltraColors.Muted)
                Spacer(Modifier.width(5.dp))
                Text(
                    if (lockedHz > 0) "${lockedHz}HZ" else "AUTO",
                    color = if (lockedHz > 0) VeltraColors.NeoMint else VeltraColors.Muted,
                    fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                )
            }
        }
    ) {
        GroupLabel("REFRESH RATE LAYAR")
        Row(Modifier.fillMaxWidth()) {
            listOf(60, 90, 120, 144).forEach { hz ->
                VeltraPrimitives.GhostChip(
                    label = "$hz",
                    selected = lockedHz == hz,
                    enabled = advancedReady,
                    modifier = Modifier.weight(1f)
                ) { onLockHz(hz) }
                Spacer(Modifier.width(6.dp))
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth()) {
            VeltraPrimitives.GhostChip("LEPAS KUNCI", enabled = advancedReady,
                modifier = Modifier.weight(1f)) { onUnlockHz() }
            Spacer(Modifier.width(6.dp))
            VeltraPrimitives.GhostChip("BACA STATUS", enabled = advancedReady,
                modifier = Modifier.weight(1f)) { onReadHz() }
        }

        Spacer(Modifier.height(12.dp))
        GroupLabel("TARGET FPS GAME", targetName.ifBlank { targetPkg })
        Row(Modifier.fillMaxWidth()) {
            listOf(30, 45, 60, 90).forEach { fps ->
                VeltraPrimitives.GhostChip(
                    label = "$fps",
                    enabled = advancedReady,
                    modifier = Modifier.weight(1f)
                ) { onLockFps(fps) }
                Spacer(Modifier.width(6.dp))
            }
        }
        Spacer(Modifier.height(7.dp))
        VeltraPrimitives.GhostChip("LEPAS BATAS FPS GAME", enabled = advancedReady,
            modifier = Modifier.fillMaxWidth()) { onUnlockFps() }

        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(VeltraColors.Border))
        Spacer(Modifier.height(11.dp))

        GroupLabel("CATATAN SENSITIVITAS")
        TextField(
            value = note,
            onValueChange = { note = it },
            placeholder = {
                Text("mis. General 95 • Red Dot 90 • Scope 85 • Sensi HP 42",
                    color = VeltraColors.Muted, fontSize = 11.sp)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, VeltraColors.Border, RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF101720),
                unfocusedContainerColor = Color(0xFF101720),
                disabledContainerColor = Color(0xFF101720),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = VeltraColors.Titanium,
                unfocusedTextColor = VeltraColors.Titanium,
                cursorColor = VeltraColors.IonCyan
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            maxLines = 4
        )
        Spacer(Modifier.height(8.dp))
        VeltraPrimitives.CobaltButton(
            "SIMPAN CATATAN", null, Modifier.fillMaxWidth()
        ) { onSaveSensitivity(note) }

        if (statusText.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(statusText, color = VeltraColors.Muted, fontSize = 9.5.sp, lineHeight = 14.sp)
        }

        Spacer(Modifier.height(10.dp))
        VeltraNote(
            title = "BATASNYA",
            lines = listOf(
                "• VELTRA TIDAK BISA mengubah sensitivitas dalam game. Angkanya tersimpan di data game; menulis ke situ = modifikasi game = risiko banned. Yang VELTRA simpan di sini cuma catatan lu sendiri.",
                "• Kunci refresh rate butuh Shizuku/ADB atau root, dan cuma permintaan ke sistem. Panel 60Hz tetap 60Hz. ROM dengan menu refresh rate sendiri sering menimpa nilai ini.",
                "• Kunci FPS game jalan lewat Game Mode API (sama kayak Game Genie bawaan) — efeknya nyata di game yang mendukung, dan butuh restart game."
            )
        )
    }
}

@Composable
private fun GroupLabel(text: String, hint: String? = null) {
    Text(text, color = VeltraColors.Muted, fontSize = 8.5.sp,
        letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
    if (hint != null) {
        Spacer(Modifier.height(3.dp))
        Text(hint, color = VeltraColors.IonCyan, fontSize = 10.sp)
    }
    Spacer(Modifier.height(6.dp))
}
