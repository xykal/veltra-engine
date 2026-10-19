package com.veltra.launcher.tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veltra.core.VeltraColors
import com.veltra.data.formatBytes
import com.veltra.launcher.VeltraCard
import com.veltra.launcher.VeltraNote
import com.veltra.launcher.VeltraPrimitives
import com.veltra.launcher.ocr.OcrCaptureService

/**
 * PERANGKAT — info HP, tes latensi jaringan, dan tangkap layar.
 *
 * Jujur:
 *  - Info yang ditampilkan cuma yang **diizinkan Android** dibaca tanpa root.
 *    Nama GPU/SoC sering disembunyikan pabrikan — VELTRA menampilkan apa adanya.
 *  - "Latensi" di sini = waktu jabat tangan TCP ke server DNS publik, BUKAN
 *    ping ICMP (Android tidak mengizinkan ICMP tanpa root). Angkanya biasanya
 *    sedikit lebih tinggi dari ping game, tapi cukup buat ngecek "net normal / jelek".
 *  - Tangkap layar memakai izin tangkap layar sistem (sama seperti pembaca teks).
 */
@Composable
fun DeviceScreen(onBack: () -> Unit, vm: DeviceViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val info by vm.info.collectAsState()
    val latency by vm.latency.collectAsState()
    val testing by vm.testing.collectAsState()

    val shotLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            ctx.startForegroundService(
                OcrCaptureService.intent(ctx, OcrCaptureService.ACTION_SHOT,
                    rc = Activity.RESULT_OK, data = res.data)
            )
        }
    }

    Box(Modifier.fillMaxSize().background(VeltraColors.Obsidian)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 46.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-8).dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = VeltraColors.Titanium)
                }
                Column(Modifier.weight(1f)) {
                    Text("PERANGKAT", color = VeltraColors.Titanium, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 2.4.sp)
                    Text("Info HP • latensi • tangkap layar",
                        color = VeltraColors.Muted, fontSize = 10.sp)
                }
            }

            // ── Info perangkat ───────────────────────────────────────
            VeltraCard(
                title = "INFO PERANGKAT",
                subtitle = "Yang bisa dibaca tanpa root — apa adanya",
                icon = Icons.Rounded.Devices,
                accent = VeltraColors.Amber
            ) {
                info?.let { d ->
                    InfoRow("Pabrikan / tipe", "${d.manufacturer} ${d.model}")
                    InfoRow("Android", "${d.androidVersion} (SDK ${d.sdk})")
                    InfoRow("Chipset", d.soc)
                    InfoRow("Papan", d.board)
                    InfoRow("Layar", "${d.width}×${d.height} @ ${"%.0f".format(d.refreshHz)}Hz")
                    InfoRow("RAM total", formatBytes(d.totalRam))
                    InfoRow("RAM bebas", formatBytes(d.availRam))
                    InfoRow("Penyimpanan bebas", formatBytes(d.freeStorage))
                    InfoRow("Baterai", "${d.batteryPct}% • ${"%.1f".format(d.batteryC)}°C")
                } ?: Text("Membaca info…", color = VeltraColors.Muted, fontSize = 11.sp)
            }

            Spacer(Modifier.height(14.dp))

            // ── Tes latensi ──────────────────────────────────────────
            VeltraCard(
                title = "LATENSI JARINGAN",
                subtitle = "Jabat tangan TCP ke DNS publik — bukan ping ICMP",
                icon = Icons.Rounded.NetworkPing,
                accent = VeltraColors.IonCyan
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        latency?.let { "${it}ms" } ?: "--",
                        color = when {
                            latency == null -> VeltraColors.Muted
                            latency!! <= 60 -> VeltraColors.NeoMint
                            latency!! <= 140 -> VeltraColors.Amber
                            else -> VeltraColors.Amber
                        },
                        fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                latency == null -> "Belum diukur"
                                latency!! <= 60 -> "Enak buat main"
                                latency!! <= 140 -> "Agak ngaret"
                                else -> "Jelek — coba ganti jaringan"
                            },
                            color = VeltraColors.Titanium, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("3x percobaan, diambil rata-rata",
                            color = VeltraColors.Muted, fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                VeltraPrimitives.CobaltButton(
                    label = "UKUR LATENSI",
                    icon = Icons.Rounded.NetworkPing,
                    modifier = Modifier.fillMaxWidth(),
                    loading = testing
                ) { vm.testLatency() }
            }

            Spacer(Modifier.height(14.dp))

            // ── Tangkap layar ────────────────────────────────────────
            VeltraCard(
                title = "TANGKAP LAYAR",
                subtitle = "Simpan ke Galeri → Pictures/VELTRA",
                icon = Icons.Rounded.Screenshot,
                accent = VeltraColors.NeoMint
            ) {
                VeltraPrimitives.CobaltButton(
                    label = "AMBIL TANGKAPAN",
                    icon = Icons.Rounded.Screenshot,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val mgr = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                            as android.media.projection.MediaProjectionManager
                    shotLauncher.launch(mgr.createScreenCaptureIntent())
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Sistem akan minta izin tangkap layar dulu (dialog + ikon siaran di " +
                            "status bar). Layar yang diamankan sistem (bank, pembayaran) akan " +
                            "tersimpan hitam — itu aturan Android, bukan bug.",
                    color = VeltraColors.Muted, fontSize = 10.sp, lineHeight = 15.sp
                )
            }

            Spacer(Modifier.height(14.dp))
            VeltraNote(
                "BATAS YANG JUJUR",
                listOf(
                    "• Android tidak mengizinkan aplikasi mengirim ping ICMP tanpa root, jadi yang diukur di sini adalah waktu sambung TCP ke 1.1.1.1 — cukup buat tahu jaringan lu sehat atau nggak.",
                    "• Nama chipset sering disembunyikan pabrikan di HP murah. Kalau tertulis “tidak diketahui”, itu keterbatasan perangkatnya, bukan VELTRA-nya.",
                    "• VELTRA tidak bisa mengubah kemampuan layar (Hz), jumlah RAM, atau kekuatan sinyal. Yang bisa diubah cuma kebiasaan sistem yang membebani performa."
                )
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = VeltraColors.Muted, fontSize = 10.5.sp,
            modifier = Modifier.weight(1f))
        Text(value, color = VeltraColors.Titanium, fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f))
    }
}
