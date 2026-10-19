package com.veltra.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    VeltraCard(
        title = "IZIN & PANEL",
        subtitle = when {
            overlayOk && usageOk -> "Semua izin siap — panel bisa muncul di dalam game."
            overlayOk -> "Overlay siap. Akses penggunaan belum ada (panel tidak bisa auto-hide)."
            else -> "Izin overlay belum ada — panel belum bisa muncul di atas game."
        },
        icon = Icons.Rounded.Shield,
        accent = if (overlayOk && usageOk) VeltraColors.NeoMint else VeltraColors.Amber,
        action = {
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        (if (overlayOk) VeltraColors.NeoMint else VeltraColors.Amber).copy(.13f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(if (overlayOk) VeltraColors.NeoMint else VeltraColors.Amber)
                Spacer(Modifier.width(5.dp))
                Text(
                    if (overlayOk) "SIAP" else "BELUM",
                    color = if (overlayOk) VeltraColors.NeoMint else VeltraColors.Amber,
                    fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                )
            }
        }
    ) {
        PermRow(
            title = "Tampilkan di atas aplikasi lain",
            desc = if (overlayOk) "Sudah aktif — panel bisa muncul di dalam game."
            else "Wajib. Tanpa ini panel VELTRA tidak bisa muncul di atas game.",
            ok = overlayOk,
            cta = "BERI IZIN",
            onClick = { vm.requestOverlay() }
        )

        Spacer(Modifier.height(9.dp))

        PermRow(
            title = "Akses penggunaan",
            desc = if (usageOk) "Sudah aktif — panel otomatis sembunyi di luar game."
            else "Opsional. Dipakai untuk mendeteksi game dan menyembunyikan panel otomatis. " +
                    "Data hanya diproses di perangkat, tidak dikirim ke mana pun.",
            ok = usageOk,
            cta = "BUKA",
            onClick = { vm.requestUsage() }
        )

        Spacer(Modifier.height(12.dp))

        if (overlayOk) {
            VeltraPrimitives.CobaltButton(
                label = if (panelOn) "MATIKAN PANEL" else "UJI PANEL",
                icon = Icons.Rounded.LockOpen,
                modifier = Modifier.fillMaxWidth()
            ) { if (panelOn) vm.stopPanel() else vm.startPanel() }
        } else {
            VeltraNote(
                title = "BELUM BISA DIUJI",
                lines = listOf(
                    "Panel hanya bisa dicoba setelah izin overlay aktif. " +
                            "Android menganggap ini izin berisiko tinggi, jadi harus lu berikan sendiri lewat layar pengaturan sistem."
                )
            )
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
        Box(Modifier.size(8.dp).offset(y = 4.dp)) {
            StatusDot(if (ok) VeltraColors.NeoMint else VeltraColors.Amber)
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = VeltraColors.Titanium, fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(desc, color = VeltraColors.Muted, fontSize = 10.sp, lineHeight = 14.sp)
        }
        if (!ok) {
            Spacer(Modifier.width(8.dp))
            Text(
                cta,
                color = VeltraColors.IonCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .clickable { onClick() }
                    .padding(top = 2.dp)
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
