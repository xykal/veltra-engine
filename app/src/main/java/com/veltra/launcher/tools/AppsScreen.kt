package com.veltra.launcher.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.veltra.core.VeltraColors
import com.veltra.data.AppEntry
import com.veltra.data.formatBytes
import com.veltra.launcher.VeltraPrimitives
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DAFTAR APLIKASI LENGKAP — ikon asli, versi, ukuran, tanggal pasang, dan
 * detail teknis tiap aplikasi.
 */
@Composable
fun AppsScreen(onBack: () -> Unit, vm: AppsViewModel = hiltViewModel()) {
    val st by vm.state.collectAsState()
    val visible = remember(st.all, st.filter, st.query) { vm.visible() }
    var expanded by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(VeltraColors.Obsidian)) {
        Column(Modifier.fillMaxSize()) {

            Row(
                Modifier.fillMaxWidth().padding(top = 46.dp, start = 8.dp, end = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = VeltraColors.Titanium)
                }
                Column(Modifier.weight(1f)) {
                    Text("SEMUA APLIKASI", color = VeltraColors.Titanium, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 2.4.sp)
                    Text(st.note, color = VeltraColors.Muted, fontSize = 10.sp)
                }
                Text("${visible.size}", color = VeltraColors.IonCyan, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
            }

            // ── Cari ──────────────────────────────────────────────────
            Box(Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) {
                TextField(
                    value = st.query,
                    onValueChange = { vm.query(it) },
                    placeholder = { Text("Cari nama atau paket…", color = VeltraColors.Muted, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = VeltraColors.Muted,
                        modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, VeltraColors.Border, RoundedCornerShape(14.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VeltraColors.Graphite,
                        unfocusedContainerColor = VeltraColors.Graphite,
                        disabledContainerColor = VeltraColors.Graphite,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = VeltraColors.Titanium,
                        unfocusedTextColor = VeltraColors.Titanium,
                        cursorColor = VeltraColors.IonCyan
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )
            }

            // ── Filter ────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 8.dp)
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                AppFilter.entries.forEach { f ->
                    VeltraPrimitives.GhostChip(
                        label = when (f) {
                            AppFilter.GAMES -> "GAME"
                            AppFilter.USER -> "APLIKASI"
                            AppFilter.SYSTEM -> "SISTEM"
                            AppFilter.ALL -> "SEMUA"
                        },
                        icon = when (f) {
                            AppFilter.GAMES -> Icons.Rounded.SportsEsports
                            AppFilter.SYSTEM -> Icons.Rounded.Android
                            else -> null
                        },
                        selected = st.filter == f
                    ) { vm.filter(f) }
                    Spacer(Modifier.width(7.dp))
                }
            }

            // ── Daftar ────────────────────────────────────────────────
            if (st.loading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Membaca daftar aplikasi…", color = VeltraColors.Muted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visible, key = { it.packageName }) { app ->
                        val open = expanded == app.packageName
                        AppRow(
                            app = app,
                            icon = remember(app.packageName) {
                                runCatching {
                                    vm.icon(app.packageName)?.toBitmap(96, 96)?.asImageBitmap()
                                }.getOrNull()
                            },
                            expanded = open,
                            onOpen = { expanded = if (open) null else app.packageName },
                            onLaunch = { vm.launch(app.packageName) },
                            onDetails = { vm.openDetails(app.packageName) },
                            onExtract = { vm.extract(app) }
                        )
                    }
                    item { Spacer(Modifier.height(14.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppEntry,
    icon: androidx.compose.ui.graphics.ImageBitmap?,
    expanded: Boolean,
    onOpen: () -> Unit,
    onLaunch: () -> Unit,
    onDetails: () -> Unit,
    onExtract: () -> Unit
) {
    val cardAlpha by animateFloatAsState(if (expanded) 1f else 0f, tween(220), label = "a")
    val border = if (expanded) VeltraColors.IonCyan.copy(.55f) else VeltraColors.Border

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(if (expanded) VeltraColors.Graphite.copy(0.98f) else VeltraColors.Graphite)
            .border(1.dp, border, RoundedCornerShape(15.dp))
            .clickable { onOpen() }
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // ikon asli aplikasi
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0E1420))
                    .border(1.dp, VeltraColors.Border, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(icon, null, Modifier.size(34.dp))
                } else {
                    Text(app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = VeltraColors.IonCyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, color = VeltraColors.Titanium, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(if (app.isGame) "GAME" else if (app.isSystem) "SISTEM" else "APLIKASI",
                        if (app.isGame) VeltraColors.NeoMint else VeltraColors.Muted)
                    Spacer(Modifier.width(6.dp))
                    Text(app.versionName ?: "v${app.versionCode}", color = VeltraColors.Muted,
                        fontSize = 10.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatBytes(app.appSize.coerceAtLeast(app.apkSize)),
                    color = VeltraColors.Titanium, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                if (app.cacheSize > 0) {
                    Text("cache ${formatBytes(app.cacheSize)}", color = VeltraColors.Amber, fontSize = 9.sp)
                }
            }
        }

        AnimatedVisibility(visible = expanded, enter = fadeIn() + slideInVertically()) {
            Column(Modifier.padding(top = 12.dp)) {
                Divider()
                Spacer(Modifier.height(10.dp))
                Text(app.packageName, color = VeltraColors.IonCyan, fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth()) {
                    Detail("Versi", "${app.versionName ?: "-"} (${app.versionCode})", Modifier.weight(1f))
                    Detail("Target SDK", "${app.targetSdk}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth()) {
                    Detail("Dipasang", date(app.firstInstall), Modifier.weight(1f))
                    Detail("Diperbarui", date(app.lastUpdate), Modifier.weight(1f))
                }
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth()) {
                    Detail("APK", formatBytes(app.apkSize), Modifier.weight(1f))
                    Detail("Sumber", app.installer ?: "tak diketahui", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    VeltraPrimitives.CobaltButton("BUKA", Icons.Rounded.PlayArrow, Modifier.weight(1f)) { onLaunch() }
                }
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth()) {
                    VeltraPrimitives.GhostChip("DETAIL SISTEM", Icons.Rounded.Info, Modifier.weight(1f)) { onDetails() }
                    Spacer(Modifier.width(7.dp))
                    VeltraPrimitives.GhostChip("SALIN APK", Icons.Rounded.Archive, Modifier.weight(1f)) { onExtract() }
                }
                if (app.isGame) {
                    Spacer(Modifier.height(9.dp))
                    Text("Game ini bisa di-boost dari layar utama VELTRA.",
                        color = VeltraColors.Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(.14f))
            .border(1.dp, color.copy(.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun Detail(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), color = VeltraColors.Muted, fontSize = 8.5.sp, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = VeltraColors.Titanium, fontSize = 11.5.sp, maxLines = 1)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(VeltraColors.Border))
}

private val fmt = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
private fun date(ms: Long): String = runCatching { fmt.format(Date(ms)) }.getOrDefault("-")
