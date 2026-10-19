package com.veltra.launcher

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veltra.core.VeltraColors

/**
 * SHIZUKU HUB — fitur utama, bukan lagi menu karang di Settings.
 *
 * Semua aksi yang butuh hak akses lebih (setara perintah ADB) dikumpulkan di sini:
 * injeksi Game Mode, kunci refresh rate, proteksi live, pangkas cache, dan lain-lain.
 * Tanpa Shizuku/root semua tombol nonaktif — VELTRA tetap jalan buat fitur utamanya.
 */
@Composable
fun ShizukuHubScreen(
    onBack: () -> Unit,
    vm: LauncherViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val level by vm.advancedLabel
    val ready = vm.advancedReady
    val result by vm.advancedResult
    val target by vm.advancedTarget
    val games by vm.games.collectAsState()
    val lockedHz by vm.lockedHz.collectAsState()
    val crosshair by vm.crosshair.collectAsState()
    val targetName = games.firstOrNull { it.packageName == target }?.name ?: target

    val accent = when {
        level.contains("Root", true) -> VeltraColors.NeoMint
        level.contains("Shizuku", true) -> VeltraColors.IonCyan
        else -> VeltraColors.Amber
    }

    Box(Modifier.fillMaxSize().background(VeltraColors.Obsidian)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Kepala ────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(top = 46.dp, start = 8.dp, end = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = VeltraColors.Titanium)
                }
                Column(Modifier.weight(1f)) {
                    Text("MODE LANJUT", color = VeltraColors.Titanium, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 2.4.sp)
                    Text("Shizuku / root • aksi setara perintah ADB",
                        color = VeltraColors.Muted, fontSize = 10.sp)
                }
                IconButton(onClick = { vm.refreshAdvanced() }) {
                    Icon(Icons.Rounded.Refresh, null, tint = accent)
                }
            }

            // ── Kartu status besar ────────────────────────────────────
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(VeltraColors.Graphite)
                    .border(1.dp, if (ready) accent.copy(.5f) else VeltraColors.Border,
                        RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(accent.copy(.13f))
                            .border(1.dp, accent.copy(.45f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (ready) Icons.Rounded.Verified else Icons.Rounded.Lock,
                            null, tint = accent, modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(level, color = VeltraColors.Titanium, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (ready) "Akses aktif. Semua aksi di bawah bisa dipakai."
                            else "Belum ada akses. Ikuti 3 langkah di bawah.",
                            color = VeltraColors.Muted, fontSize = 10.sp, lineHeight = 14.sp
                        )
                    }
                }

                AnimatedVisibility(visible = !ready, enter = fadeIn() + expandVertically()) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(VeltraColors.Border))
                        Spacer(Modifier.height(12.dp))
                        StepCard(1, "Pasang Shizuku",
                            "Aplikasi gratis di Play Store. VELTRA memakainya buat menjalankan perintah ADB tanpa kabel.")
                        StepCard(2, "Pasangkan lewat Wireless Debugging",
                            "Di Pengembang → Wireless debugging → “Pair device with pairing code”. Masukkan kodenya ke Shizuku.")
                        StepCard(3, "Balik ke VELTRA, tekan MINTA IZIN",
                            "Setiap kali HP restart, pairing perlu diulang (kecuali HP di-root).")

                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth()) {
                            VeltraPrimitives.CobaltButton(
                                "PASANG SHIZUKU", Icons.Rounded.Shop, Modifier.weight(1f)
                            ) {
                                runCatching {
                                    ctx.startActivity(
                                        Intent(Intent.ACTION_VIEW,
                                            Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            VeltraPrimitives.GhostChip("BUKA SHIZUKU", Icons.Rounded.OpenInNew,
                                Modifier.weight(1f)) {
                                runCatching {
                                    val i = ctx.packageManager
                                        .getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                    if (i != null) ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        VeltraPrimitives.CobaltButton(
                            "MINTA IZIN", Icons.Rounded.Key, Modifier.fillMaxWidth()
                        ) { vm.requestAdvancedPermission() }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Target game ───────────────────────────────────────────
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("GAME TARGET", color = VeltraColors.Muted, fontSize = 9.sp,
                    letterSpacing = 2.sp)
                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    games.take(10).forEach { g ->
                        val on = g.packageName == target
                        Box(
                            Modifier
                                .padding(end = 7.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (on) accent.copy(.14f) else VeltraColors.Graphite)
                                .border(1.dp, if (on) accent else VeltraColors.Border,
                                    RoundedCornerShape(11.dp))
                                .clickableCard { vm.setAdvancedTarget(g.packageName) }
                                .padding(horizontal = 11.dp, vertical = 8.dp)
                        ) {
                            Text(g.name, color = if (on) accent else VeltraColors.Titanium,
                                fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("$targetName  •  $target", color = VeltraColors.Muted, fontSize = 9.sp)
            }

            Spacer(Modifier.height(16.dp))

            // ── Aksi ──────────────────────────────────────────────────
            Column(Modifier.padding(horizontal = 20.dp)) {

                HubSection("PERFORMA GAME", "Jalur resmi Android — sama seperti Game Genie bawaan.") {
                    HubRow {
                        HubButton("INJECT TURBO", Icons.Rounded.Bolt, accent, ready,
                            Modifier.weight(1f)) { vm.runAdvanced(AdvancedAction.TURBO_GAME) }
                        HubButton("KOMPILASI SPEED", Icons.Rounded.Memory, accent, ready,
                            Modifier.weight(1f)) { vm.runAdvanced(AdvancedAction.COMPILE_GAME) }
                    }
                    HubRow {
                        HubButton("RESET GAME MODE", Icons.Rounded.RestartAlt, accent, ready,
                            Modifier.weight(1f)) { vm.runAdvanced(AdvancedAction.RESET_GAME) }
                        HubButton("RES TURUN 85%", Icons.Rounded.Screenshot, accent, ready,
                            Modifier.weight(1f)) { vm.runAdvanced(AdvancedAction.RES_DOWN) }
                    }
                    HubButton("RESET RESOLUSI", Icons.Rounded.RestartAlt, accent, ready,
                        Modifier.fillMaxWidth()) { vm.runAdvanced(AdvancedAction.RES_RESET) }
                }

                HubSection("LAYAR & FPS",
                    "Kunci refresh rate butuh akses shell. Panel 60Hz tetap 60Hz.") {
                    Row(Modifier.fillMaxWidth()) {
                        listOf(60, 90, 120, 144).forEach { hz ->
                            VeltraPrimitives.GhostChip(
                                label = "${hz}Hz", selected = lockedHz == hz,
                                enabled = ready, modifier = Modifier.weight(1f)
                            ) { vm.lockHz(hz) }
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    HubRow {
                        HubButton("LEPAS KUNCI Hz", Icons.Rounded.LockOpen, accent, ready,
                            Modifier.weight(1f)) { vm.unlockHz() }
                        HubButton("BACA STATUS", Icons.Rounded.Info, accent, ready,
                            Modifier.weight(1f)) { vm.readHz() }
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.fillMaxWidth()) {
                        listOf(30, 45, 60, 90).forEach { fps ->
                            VeltraPrimitives.GhostChip(
                                label = "${fps}fps", enabled = ready,
                                modifier = Modifier.weight(1f)
                            ) { vm.lockFps(fps) }
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    HubButton("LEPAS BATAS FPS GAME", Icons.Rounded.LockOpen, accent, ready,
                        Modifier.fillMaxWidth()) { vm.unlockFps() }
                }

                HubSection("LIVE TETEP JALAN", "Kunci aplikasi live dari doze & standby.") {
                    HubRow {
                        HubButton("PROTEKSI LIVE", Icons.Rounded.LiveTv, accent, ready,
                            Modifier.weight(1f)) { vm.runAdvanced(AdvancedAction.PROTECT_LIVE) }
                        HubButton("LEPAS PROTEKSI", Icons.Rounded.LockOpen, accent, ready,
                            Modifier.weight(1f)) { vm.unprotectLive() }
                    }
                }

                HubSection("PANAS & BATERAI", "Kurangi sumber panas — bukan mendinginkan HP.") {
                    HubRow {
                        HubButton("HEMAT PANAS", Icons.Rounded.AcUnit, accent, ready,
                            Modifier.weight(1f)) { vm.runAdvanced(AdvancedAction.COOLING) }
                        HubButton("BACA SUHU", Icons.Rounded.DeviceThermostat, accent, ready,
                            Modifier.weight(1f)) { vm.readTemp() }
                    }
                }

                HubSection("BERSIH-BERSIH", "") {
                    HubRow {
                        HubButton("PANGKAS CACHE", Icons.Rounded.CleaningServices, accent, ready,
                            Modifier.weight(1f)) { vm.trimCaches() }
                        HubButton("STOP BLOATWARE", Icons.Rounded.Block, accent, ready,
                            Modifier.weight(1f)) { vm.runAdvanced(AdvancedAction.FREEZE_BLOAT) }
                    }
                }

                HubSection("SISTEM", "") {
                    HubRow {
                        HubButton("ANIMASI CEPAT", Icons.Rounded.Animation, accent, ready,
                            Modifier.weight(1f)) { vm.fastAnimations() }
                        HubButton("MODE SENYAP", Icons.Rounded.DoNotDisturbOn, accent, ready,
                            Modifier.weight(1f)) { vm.toggleDnd() }
                    }
                }

                // Crosshair cepat
                Spacer(Modifier.height(6.dp))
                VeltraCard(
                    title = "CROSSHAIR CEPAT",
                    subtitle = "Nyalakan/matikan dari sini tanpa buka Settings",
                    icon = Icons.Rounded.CenterFocusStrong,
                    accent = VeltraColors.NeoMint
                ) {
                    VeltraPrimitives.SwitchRow(
                        title = "Crosshair menyala",
                        subtitle = "Atur bentuk & warnanya di Settings → CROSSHAIR",
                        checked = crosshair.enabled
                    ) { vm.setCrosshair(crosshair.copy(enabled = it)) }
                }
            }

            // ── Hasil perintah ────────────────────────────────────────
            if (result.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0D131C))
                        .border(1.dp, VeltraColors.Border, RoundedCornerShape(14.dp))
                        .padding(13.dp)
                ) {
                    Text("HASIL", color = VeltraColors.Muted, fontSize = 8.5.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(result, color = if (result.startsWith("Gagal")) VeltraColors.Amber
                    else VeltraColors.NeoMint, fontSize = 10.5.sp, lineHeight = 15.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            VeltraNote(
                "BATAS YANG JUJUR",
                listOf(
                    "• Shizuku bukan root. Dia menjalankan perintah dengan hak shell (ADB) lewat aplikasi yang lu pasang sendiri. Aman, bisa dicabut kapan saja, dan nggak mengubah sistem.",
                    "• Tanpa root, beberapa perintah tetap ditolak Android (mis. mengubah berkas sistem). Kalau ditolak, VELTRA menampilkan pesannya — bukan berpura-pura berhasil.",
                    "• Pairing Wireless Debugging perlu diulang setiap HP restart. Kalau HP di-root, Shizuku bisa jalan otomatis tanpa pairing ulang.",
                    "• VELTRA tidak pernah mengubah berkas game, tidak menyuntik apa pun ke proses game, dan tidak menyentuh data game. Semua aksi di sini adalah pengaturan sistem yang bisa dibalik."
                ),
                Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

/* ─────────────── KOMPONEN HUB ─────────────── */

@Composable
private fun StepCard(no: Int, title: String, body: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(VeltraColors.Cobalt.copy(.16f))
                .border(1.dp, VeltraColors.Cobalt.copy(.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$no", color = VeltraColors.IonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = VeltraColors.Titanium, fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(body, color = VeltraColors.Muted, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun HubSection(
    title: String,
    hint: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VeltraColors.Graphite)
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(16.dp))
            .padding(13.dp)
    ) {
        Text(title, color = VeltraColors.Titanium, fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        if (hint.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(hint, color = VeltraColors.Muted, fontSize = 9.5.sp, lineHeight = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun HubRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun HubButton(
    label: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    VeltraPrimitives.GhostChip(
        label = label, icon = icon, modifier = modifier, enabled = enabled,
        onClick = onClick
    )
}

private fun Modifier.clickableCard(onClick: () -> Unit): Modifier = cardClick(onClick)
