package com.veltra.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Games
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veltra.core.VeltraColors
import com.veltra.BuildConfig

/**
 * Settings — panel behaviour, izin, dan transparansi.
 * Semua toggle tersimpan di DataStore (lihat SettingsRepository).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onShizuku: () -> Unit = {},
    onProfiles: () -> Unit = {},
    vm: LauncherViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val autoHide by vm.autoHide.collectAsState()
    val autoStart by vm.autoStart.collectAsState()
    val overlayOk by vm.overlayGranted
    val usageOk by vm.usageGranted
    val mode by vm.panelMode.collectAsState()
    val side by vm.panelSide.collectAsState()
    val advLabel by vm.advancedLabel
    val advResult by vm.advancedResult
    val advTarget by vm.advancedTarget
    val games by vm.games.collectAsState()
    val crosshair by vm.crosshair.collectAsState()
    val sensitivity by vm.sensitivity.collectAsState()
    val lockedHz by vm.lockedHz.collectAsState()
    val profiles by vm.gameProfiles.collectAsState()
    val targetName = games.firstOrNull { it.packageName == advTarget }?.name ?: advTarget

    LaunchedEffect(Unit) { vm.refreshAdvanced() }

    Column(
        Modifier
            .fillMaxSize()
            .background(VeltraColors.Obsidian)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(VeltraColors.Graphite)
                    .border(1.dp, VeltraColors.Border, RoundedCornerShape(11.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Text("‹", color = VeltraColors.Titanium, fontSize = 19.sp) }
            Spacer(Modifier.width(14.dp))
            Text("SETTINGS", color = VeltraColors.Titanium, fontSize = 17.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        }
        Spacer(Modifier.height(24.dp))

        SectionTitle("PANEL")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VeltraColors.Graphite),
            border = androidx.compose.foundation.BorderStroke(1.dp, VeltraColors.Border)
        ) {
            Column(Modifier.padding(4.dp)) {
                ToggleRow(
                    "Sembunyikan otomatis di luar game",
                    if (usageOk) "Aktif — butuh izin akses penggunaan"
                    else "Butuh izin akses penggunaan dulu",
                    autoHide && usageOk,
                    enabled = usageOk
                ) { vm.setAutoHide(it) }

                ToggleRow(
                    "Nyalakan panel saat game dibuka",
                    "Panel muncul otomatis tanpa buka VELTRA dulu",
                    autoStart && overlayOk,
                    enabled = overlayOk
                ) { vm.setAutoStart(it) }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("GAYA PANEL")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VeltraColors.Graphite),
            border = androidx.compose.foundation.BorderStroke(1.dp, VeltraColors.Border)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Bentuk panel", color = VeltraColors.Muted, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StyleChip("HUD MENGAMBANG", "kapsul → panel 300×468, bisa digeser",
                        selected = mode == "HUD", Modifier.weight(1f)) { vm.setPanelMode("HUD") }
                    StyleChip("SIDEBAR ROG", "handle di tepi → panel geser 232dp",
                        selected = mode == "SIDEBAR", Modifier.weight(1f)) { vm.setPanelMode("SIDEBAR") }
                }
                Spacer(Modifier.height(14.dp))
                Text("Posisi sidebar", color = VeltraColors.Muted, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StyleChip("KIRI", "nempel di tepi kiri", selected = side == "LEFT",
                        Modifier.weight(1f)) { vm.setPanelSide("LEFT") }
                    StyleChip("KANAN", "nempel di tepi kanan", selected = side == "RIGHT",
                        Modifier.weight(1f)) { vm.setPanelSide("RIGHT") }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Sidebar: ketuk handle buat buka/tutup, geser handle buat naik-turun. " +
                            "HUD: ketuk kapsul buat expand.",
                    color = Color(0xFF6B7588), fontSize = 9.sp, lineHeight = 13.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("IZIN")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VeltraColors.Graphite),
            border = androidx.compose.foundation.BorderStroke(1.dp, VeltraColors.Border)
        ) {
            Column {
                PermItem("Tampilkan di atas aplikasi lain", overlayOk) { vm.requestOverlay() }
                PermItem("Akses penggunaan", usageOk) { vm.requestUsage() }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("MODE LANJUT  (SHIZUKU / ROOT)")
        AdvancedCard(
            levelLabel = advLabel,
            ready = vm.advancedReady,
            lastResult = advResult,
            onAction = { vm.runAdvanced(it) },
            onRequestPermission = { vm.requestAdvancedPermission() },
            onRefresh = { vm.refreshAdvanced() },
            gameOptions = games.map { it.packageName to it.name },
            targetPkg = advTarget,
            onTarget = { vm.setAdvancedTarget(it) }
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("PROFIL GAME", "Pengaturan per-game yang dipakai otomatis saat game dibuka.")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VeltraColors.Graphite),
            border = androidx.compose.foundation.BorderStroke(1.dp, VeltraColors.Border)
        ) {
            Column {
                val custom = profiles.count { it.value.isCustom }
                NavItem(
                    icon = Icons.Rounded.Games,
                    title = "Profil per-game",
                    subtitle = if (custom == 0) "Semua game masih pakai pengaturan bawaan"
                    else "$custom game punya profil sendiri",
                    onClick = onProfiles
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("CROSSHAIR", "Gambar bantu bidik. Tidak menyentuh game sama sekali.")
        CrosshairCard(
            config = crosshair,
            hasOverlayPermission = overlayOk,
            onChange = { vm.setCrosshair(it) }
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("LAYAR, FPS & SENSITIVITAS",
            "Yang bisa dikunci dan yang tidak bisa — dibedakan jujur.")
        SensitivityCard(
            lockedHz = lockedHz,
            advancedReady = vm.advancedReady,
            statusText = advResult,
            targetPkg = advTarget,
            targetName = targetName,
            sensitivity = sensitivity,
            onLockHz = { vm.lockHz(it) },
            onUnlockHz = { vm.unlockHz() },
            onReadHz = { vm.readHz() },
            onLockFps = { vm.lockFps(it) },
            onUnlockFps = { vm.unlockFps() },
            onSaveSensitivity = { vm.saveSensitivity(advTarget, it) }
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("TENTANG")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VeltraColors.Graphite),
            border = androidx.compose.foundation.BorderStroke(1.dp, VeltraColors.Border)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(VeltraColors.Cobalt, VeltraColors.IonCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text("V", color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("VELTRA", color = VeltraColors.Titanium, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("Game Performance System", color = VeltraColors.Muted, fontSize = 10.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("v${BuildConfig.VERSION_NAME}", color = VeltraColors.Muted, fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "VELTRA tidak mengirim data apa pun ke server. Semua metrik performa, " +
                            "daftar game, dan status live diproses hanya di perangkat ini. " +
                            "Tidak ada hook, injeksi, atau modifikasi berkas game.",
                    color = VeltraColors.Muted, fontSize = 10.sp, lineHeight = 15.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Catatan jujur: Android tidak mengizinkan aplikasi pihak ketiga memaksa " +
                            "mode performa sistem atau mencegah sistem menutup aplikasi " +
                            "background saat memori habis. VELTRA hanya mengurangi risikonya.",
                    color = Color(0xFF6B7588), fontSize = 9.5.sp, lineHeight = 14.sp
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String, hint: String? = null) {
    Text(text, color = VeltraColors.Muted, fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp)
    if (hint != null) {
        Spacer(Modifier.height(3.dp))
        Text(hint, color = VeltraColors.Muted, fontSize = 9.sp, lineHeight = 13.sp)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) VeltraColors.Titanium else Color(0xFF6B7588),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = VeltraColors.Muted, fontSize = 9.5.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VeltraColors.Cobalt,
                checkedTrackColor = VeltraColors.Cobalt.copy(0.35f),
                uncheckedThumbColor = Color(0xFF6B7588),
                uncheckedTrackColor = Color.White.copy(0.08f)
            )
        )
    }
}

@Composable
private fun StyleChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) VeltraColors.Cobalt.copy(0.16f) else Color.White.copy(0.03f))
            .border(
                1.dp,
                if (selected) VeltraColors.IonCyan.copy(0.6f) else Color.White.copy(0.06f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(7.dp).clip(CircleShape)
                    .background(if (selected) VeltraColors.IonCyan else Color(0xFF6B7588))
            )
            Spacer(Modifier.width(7.dp))
            Text(title, color = if (selected) VeltraColors.Titanium else VeltraColors.Muted,
                fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = Color(0xFF6B7588), fontSize = 8.5.sp, lineHeight = 12.sp)
    }
}

/** Baris menu yang membuka layar lain. */
@Composable
private fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(VeltraColors.IonCyan.copy(.12f))
                .border(1.dp, VeltraColors.IonCyan.copy(.35f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = VeltraColors.IonCyan, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = VeltraColors.Titanium, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = VeltraColors.Muted, fontSize = 9.5.sp, lineHeight = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.KeyboardArrowRight, null, tint = VeltraColors.Muted,
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PermItem(title: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { if (!granted) onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape)
            .background(if (granted) VeltraColors.NeoMint else VeltraColors.Amber))
        Spacer(Modifier.width(12.dp))
        Text(title, color = VeltraColors.Titanium, fontSize = 12.sp,
            modifier = Modifier.weight(1f))
        Text(
            if (granted) "AKTIF" else "BERI IZIN",
            color = if (granted) VeltraColors.NeoMint else VeltraColors.IonCyan,
            fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
        )
    }
}
