package com.veltra.launcher

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veltra.core.VeltraColors

/**
 * Settings — panel behaviour, izin, dan transparansi.
 * Semua toggle tersimpan di DataStore (lihat SettingsRepository).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: LauncherViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val autoHide by vm.autoHide.collectAsState()
    val autoStart by vm.autoStart.collectAsState()
    val overlayOk by vm.overlayGranted
    val usageOk by vm.usageGranted

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
                    Text("v1.0.2", color = VeltraColors.Muted, fontSize = 10.sp,
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
private fun SectionTitle(text: String) {
    Text(text, color = VeltraColors.Muted, fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp)
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
