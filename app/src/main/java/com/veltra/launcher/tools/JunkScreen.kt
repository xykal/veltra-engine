package com.veltra.launcher.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veltra.core.VeltraColors
import com.veltra.data.JunkCategory
import com.veltra.data.formatBytes
import com.veltra.launcher.VeltraPrimitives
import kotlin.math.roundToInt

@Composable
fun JunkScreen(onBack: () -> Unit, vm: JunkViewModel = hiltViewModel()) {
    val st by vm.state.collectAsState()
    val sizeAnim by animateFloatAsState(
        targetValue = st.totalBytes.coerceAtLeast(1).toFloat(),
        animationSpec = tween(700), label = "size"
    )

    Box(Modifier.fillMaxSize().background(VeltraColors.Obsidian)) {
        Column(Modifier.fillMaxSize()) {

            Row(
                Modifier.fillMaxWidth().padding(top = 46.dp, start = 8.dp, end = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = VeltraColors.Titanium)
                }
                Column(Modifier.weight(1f)) {
                    Text("PEMBERSIH", color = VeltraColors.Titanium, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 2.4.sp)
                    Text("Buang sampah, bukan berkas penting", color = VeltraColors.Muted, fontSize = 10.sp)
                }
                IconButton(onClick = { vm.scan() }) {
                    Icon(Icons.Rounded.Refresh, null, tint = VeltraColors.IonCyan)
                }
            }

            // ── Gauge besarnya sampah ──────────────────────────────────
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                JunkGauge(
                    bytes = sizeAnim.toLong(),
                    scanning = st.scanning,
                    freed = st.freedBytes
                )
            }

            AnimatedVisibility(
                visible = st.freedBytes > 0,
                enter = fadeIn() + slideInVertically()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    VeltraPrimitives.SuccessBurst(true, "Bebas ${formatBytes(st.freedBytes)}")
                }
            }

            if (st.note.isNotBlank()) {
                Text(st.note, color = VeltraColors.Muted, fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 16.sp)
            }

            // ── Aksi cepat ────────────────────────────────────────────
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) {
                VeltraPrimitives.GhostChip("PILIH SEMUA", Icons.Rounded.CheckCircle) {
                    vm.selectAll(st.selected.size < st.items.size)
                }
                Spacer(Modifier.width(8.dp))
                VeltraPrimitives.GhostChip("SCAN ULANG", Icons.Rounded.CleaningServices) { vm.scan() }
            }

            // ── Daftar ────────────────────────────────────────────────
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(st.items, key = { it.path }) { item ->
                    JunkRow(item, item.path in st.selected) { vm.toggle(item.path) }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            VeltraPrimitives.CobaltButton(
                label = if (st.selected.isEmpty()) "PILIH DULU" else "BUANG ${st.selected.size} ITEM",
                icon = Icons.Rounded.AutoDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                enabled = st.selected.isNotEmpty(),
                loading = st.cleaning
            ) { vm.clean() }

            JunkHonestNote()
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun JunkGauge(bytes: Long, scanning: Boolean, freed: Long) {
    val inf = rememberInfiniteTransition(label = "jg")
    val spin by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "s")
    val ratio = (bytes / (1024f * 1024f * 1024f)).coerceIn(0f, 1f)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(178.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val w = 9.dp.toPx()
            drawArc(Color.White.copy(.06f), 0f, 360f, false, style = Stroke(w))
            rotate(spin, pivot = center) {
                drawArc(
                    Brush.sweepGradient(
                        listOf(Color.Transparent, VeltraColors.IonCyan, VeltraColors.NeoMint, Color.Transparent)
                    ),
                    0f, (280f * ratio).coerceAtLeast(18f), false,
                    style = Stroke(w, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (scanning) "MEMINDAI…" else formatBytes(bytes),
                color = VeltraColors.Titanium, fontSize = if (scanning) 13.sp else 24.sp,
                fontWeight = FontWeight.Bold, letterSpacing = if (scanning) 2.sp else 0.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(if (scanning) "membaca penyimpanan" else "bisa dibebaskan",
                color = VeltraColors.Muted, fontSize = 10.sp)
            if (freed > 0) {
                Spacer(Modifier.height(6.dp))
                Text("+ ${formatBytes(freed)} dibuang", color = VeltraColors.NeoMint,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun JunkRow(item: com.veltra.data.JunkItem, selected: Boolean, onToggle: () -> Unit) {
    val bg = if (selected) VeltraColors.IonCyan.copy(.10f) else VeltraColors.Graphite
    val bd = if (selected) VeltraColors.IonCyan.copy(.7f) else VeltraColors.Border
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .border(1.dp, bd, RoundedCornerShape(13.dp))
            .clickable { onToggle() }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            null,
            tint = if (selected) VeltraColors.IonCyan else VeltraColors.Muted,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, color = VeltraColors.Titanium, fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(item.category.label, color = VeltraColors.Muted, fontSize = 9.5.sp)
        }
        if (item.size > 0) {
            Text(formatBytes(item.size), color = if (selected) VeltraColors.IonCyan else VeltraColors.Muted,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun JunkHonestNote() {
    Column(
        Modifier
            .padding(horizontal = 22.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF12181F))
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(13.dp))
            .padding(13.dp)
    ) {
        Text("BATASNYA", color = VeltraColors.Amber, fontSize = 9.sp,
            letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Sejak Android 11, folder Android/data & Android/obb milik aplikasi lain " +
            "dikunci oleh sistem. Aplikasi pembersih mana pun tidak bisa menyentuhnya tanpa " +
            "izin khusus yang ditolak Play Store. Jadi yang bisa VELTRA bersihkan: sisa berkas " +
            "pemasang (.apk), berkas sementara, folder kosong, dan berkas besar di folder publik. " +
            "Kalau HP kamu pakai Shizuku/root, VELTRA bisa sekalian menjalankan pemangkasan cache " +
            "sistem — itu yang paling terasa efeknya.",
            color = VeltraColors.Muted, fontSize = 10.5.sp, lineHeight = 16.sp
        )
    }
}
