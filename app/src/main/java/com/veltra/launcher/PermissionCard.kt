package com.veltra.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veltra.core.VeltraColors

/**
 * Onboarding izin — ini yang bikin VELTRA kepake atau nggak.
 *
 * Dua izin yang diminta, dan keduanya dijelaskan dulu sebelum dibuka ke Settings:
 *  1. **Overlay** (WAJIB) — supaya panel bisa muncul di atas game.
 *  2. **Akses Penggunaan** (OPSIONAL) — supaya panel tahu kapan kita di dalam game
 *     dan bisa sembunyi sendiri saat kita keluar. Tanpa ini panel nggak bisa auto-hide.
 */
@Composable
fun PermissionCard(vm: LauncherViewModel) {
    val overlayOk by vm.overlayGranted
    val usageOk by vm.usageGranted
    val panelOn by vm.panelRunning

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VeltraColors.Graphite),
        border = androidx.compose.foundation.BorderStroke(1.dp, VeltraColors.Border)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "IZIN & PANEL",
                color = VeltraColors.Titanium,
                fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(10.dp))

            PermRow(
                title = "Tampilkan di atas aplikasi lain",
                desc = if (overlayOk) "Sudah aktif — panel bisa muncul di dalam game."
                else "Wajib. Tanpa ini panel VELTRA tidak bisa muncul di atas game.",
                ok = overlayOk,
                cta = "BERI IZIN",
                onClick = { vm.requestOverlay() }
            )
            Spacer(Modifier.height(10.dp))

            PermRow(
                title = "Akses penggunaan",
                desc = if (usageOk) "Sudah aktif — panel otomatis sembunyi di luar game."
                else "Opsional. Dipakai untuk mendeteksi game dan menyembunyikan panel otomatis. " +
                        "Data hanya diproses di perangkat, tidak dikirim ke mana pun.",
                ok = usageOk,
                cta = "BUKA",
                onClick = { vm.requestUsage() }
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (panelOn) vm.stopPanel() else vm.startPanel() },
                    enabled = overlayOk,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (panelOn) VeltraColors.Graphite else VeltraColors.Cobalt
                    ),
                    border = if (panelOn)
                        androidx.compose.foundation.BorderStroke(1.dp, VeltraColors.Border)
                    else null
                ) {
                    Text(
                        if (panelOn) "MATIKAN PANEL" else "UJI PANEL",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                }
            }
            if (!overlayOk) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Belum bisa diuji: izin overlay belum aktif.",
                    color = VeltraColors.Amber, fontSize = 9.5.sp
                )
            }
        }
    }
}

@Composable
private fun PermRow(
    title: String,
    desc: String,
    ok: Boolean,
    cta: String,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    if (ok) VeltraColors.NeoMint else VeltraColors.Amber,
                    androidx.compose.foundation.shape.CircleShape
                )
                .offset(y = 4.dp)
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = VeltraColors.Titanium, fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(desc, color = VeltraColors.Muted, fontSize = 10.sp)
        }
        if (!ok) {
            Spacer(Modifier.width(8.dp))
            Text(
                cta,
                color = VeltraColors.IonCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onClick() }.padding(top = 2.dp)
            )
        }
    }
}

/** Helper buka layar izin sistem. */
object PermissionIntents {
    fun overlay(ctx: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${ctx.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun usage(ctx: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
