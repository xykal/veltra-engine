package com.veltra.launcher.ocr

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veltra.core.VeltraColors
import com.veltra.launcher.VeltraPrimitives

/**
 * LAYAR PEMBACA TEKS — baca teks di layar HP tanpa mengunggah apa pun.
 * Semua diproses offline di perangkat (ML Kit OCR).
 */
@Composable
fun OcrScreen(onBack: () -> Unit, vm: OcrViewModel = hiltViewModel()) {
    val st by vm.state.collectAsState()
    val ctx = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res -> vm.onPermissionResult(res.resultCode == android.app.Activity.RESULT_OK, res.data) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.readUri(it) } }

    Box(Modifier.fillMaxSize().background(VeltraColors.Obsidian)) {

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(top = 46.dp, start = 8.dp, end = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = VeltraColors.Titanium)
                }
                Column(Modifier.weight(1f)) {
                    Text("PEMBACA TEKS", color = VeltraColors.Titanium, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 2.4.sp)
                    Text("Ambil teks dari layar • 100% offline", color = VeltraColors.Muted, fontSize = 10.sp)
                }
                if (st.phase != OcrPhase.IDLE) {
                    IconButton(onClick = { vm.reset() }) {
                        Icon(Icons.Rounded.Refresh, null, tint = VeltraColors.IonCyan)
                    }
                }
            }

            // ── Tombol utama dengan animasi ───────────────────────────────
            Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                CaptureOrb(
                    phase = st.phase,
                    onClick = { permissionLauncher.launch(vm.permissionIntent()) }
                )
            }

            Text(
                when (st.phase) {
                    OcrPhase.CAPTURING -> "Arahkan ke layar yang mau dibaca…"
                    OcrPhase.READING -> "Membaca teks di perangkat…"
                    OcrPhase.DONE -> "Selesai — ketuk baris untuk memilih, lalu salin."
                    OcrPhase.EMPTY -> "Tidak ada teks terbaca."
                    OcrPhase.ERROR -> st.message
                    else -> "Sistem akan meminta izin tangkap layar. Teks dibaca di HP, tidak diunggah."
                },
                color = if (st.phase == OcrPhase.ERROR || st.phase == OcrPhase.EMPTY) VeltraColors.Amber
                else VeltraColors.Muted,
                fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ── Aksi sekunder ─────────────────────────────────────────────
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
                VeltraPrimitives.GhostChip("AMBIL ULANG", Icons.Rounded.Refresh) {
                    // kalau sesi masih hidup, cukup ambil frame baru;
                    // kalau sudah mati, minta izin tangkap layar lagi
                    if (!vm.captureAgain()) permissionLauncher.launch(vm.permissionIntent())
                }
                Spacer(Modifier.width(8.dp))
                VeltraPrimitives.GhostChip("DARI GAMBAR", Icons.Rounded.Image) {
                    pickImage.launch("image/*")
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Preview tangkapan ─────────────────────────────────────────
            st.bitmap?.let { bmp ->
                Text("TANGKAPAN", color = VeltraColors.Muted, fontSize = 9.sp,
                    letterSpacing = 2.sp, modifier = Modifier.padding(start = 22.dp, bottom = 6.dp))
                Box(Modifier.padding(horizontal = 22.dp)) {
                    ScanPreview(bmp, scanning = st.phase == OcrPhase.READING)
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Hasil ─────────────────────────────────────────────────────
            AnimatedContent(
                targetState = st.result,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "result"
            ) { res ->
                if (res == null || res.fullText.isBlank()) {
                    EmptyPlate()
                } else {
                    Column {
                        SmartChips(res, onPick = { vm.toggle(it) }, selected = st.selected)
                        Spacer(Modifier.height(12.dp))
                        ResultPlate(res.lines, st.selected, onToggle = { vm.toggle(it) })
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Footer aksi ───────────────────────────────────────────────
            if (!st.result?.fullText.isNullOrBlank()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
                    VeltraPrimitives.CobaltButton(
                        "SALIN TEKS", Icons.Rounded.ContentCopy, Modifier.weight(1f)
                    ) { vm.copy() }
                    Spacer(Modifier.width(8.dp))
                    VeltraPrimitives.GhostChip("BAGIKAN", Icons.Rounded.Share) {
                        vm.shareIntent()?.let { ctx.startActivity(it) }
                    }
                }
            }

            HonestNote()
            Spacer(Modifier.height(30.dp))
        }
    }
}

/* ─────────────────────────── ORB UTAMA ─────────────────────────── */

@Composable
private fun CaptureOrb(phase: OcrPhase, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "orb")
    val spin by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "spin")
    val breathe by inf.animateFloat(0.86f, 1.10f,
        infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "breathe")
    val active = phase == OcrPhase.CAPTURING || phase == OcrPhase.READING

    Box(contentAlignment = Alignment.Center) {
        // halo
        Box(
            Modifier
                .size(208.dp)
                .graphicsLayer { scaleX = breathe; scaleY = breathe }
                .background(
                    Brush.radialGradient(
                        listOf(VeltraColors.Cobalt.copy(if (active) 0.32f else 0.18f), Color.Transparent)
                    )
                )
        )
        // ring orbit
        CanvasRing(Modifier.size(170.dp), spin, active)
        CanvasRing(Modifier.size(170.dp), -spin * 0.6f, active, color = VeltraColors.IonCyan)

        Box(
            Modifier
                .size(112.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1B2540), Color(0xFF101828))
                    )
                )
                .border(
                    1.4.dp,
                    Brush.linearGradient(listOf(VeltraColors.IonCyan.copy(.75f), VeltraColors.Cobalt.copy(.35f))),
                    androidx.compose.foundation.shape.CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.Screenshot,
                    null, tint = VeltraColors.IonCyan, modifier = Modifier.size(30.dp)
                )
                Spacer(Modifier.height(5.dp))
                Text(if (active) "MINDER" else "BACA", color = VeltraColors.Titanium,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun CanvasRing(modifier: Modifier, angle: Float, active: Boolean,
                       color: Color = VeltraColors.Cobalt) {
    val w = if (active) 3.5f else 2f
    androidx.compose.foundation.Canvas(modifier) {
        rotate(angle, pivot = Offset(size.width / 2, size.height / 2)) {
            drawArc(
                brush = Brush.sweepGradient(listOf(Color.Transparent, color, Color.Transparent)),
                startAngle = 0f, sweepAngle = 190f, useCenter = false,
                style = Stroke(width = w.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}

/* ─────────────────────────── PREVIEW ─────────────────────────── */

@Composable
private fun ScanPreview(bmp: Bitmap, scanning: Boolean) {
    val inf = rememberInfiniteTransition(label = "scan")
    val sweep by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "sweep")

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(16.dp))
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    if (scanning) {
                        val y = size.height * sweep
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, VeltraColors.IonCyan.copy(.55f), Color.Transparent),
                                startY = y - 42f, endY = y + 42f
                            ),
                            blendMode = BlendMode.Screen
                        )
                    }
                }
            }
    ) {
        Image(
            bitmap = bmp.asImageBitmap(), contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
        )
    }
}

/* ─────────────────────────── HASIL ─────────────────────────── */

@Composable
private fun SmartChips(res: ReadResult, selected: Set<String>, onPick: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 22.dp)) {
        Text("TERDETEKSI", color = VeltraColors.Muted, fontSize = 9.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            res.phones.forEach { SmartChip("HP", it, selected, onPick) }
            res.emails.forEach { SmartChip("EMAIL", it, selected, onPick) }
            res.links.forEach { SmartChip("LINK", it, selected, onPick) }
            res.codes.take(4).forEach { SmartChip("KODE", it, selected, onPick) }
        }
    }
}

@Composable
private fun SmartChip(kind: String, value: String, selected: Set<String>, onPick: (String) -> Unit) {
    val on = value in selected
    Box(
        Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (on) VeltraColors.IonCyan.copy(.14f) else VeltraColors.Graphite)
            .border(1.dp, if (on) VeltraColors.IonCyan else VeltraColors.Border, RoundedCornerShape(11.dp))
            .clickable { onPick(value) }
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Column {
            Text(kind, color = VeltraColors.Muted, fontSize = 8.sp, letterSpacing = 1.6.sp)
            Text(value.take(28), color = if (on) VeltraColors.IonCyan else VeltraColors.Titanium,
                fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ResultPlate(lines: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 22.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VeltraColors.Graphite)
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        lines.filter { it.isNotBlank() }.forEach { line ->
            val on = line in selected
            Text(
                line,
                color = if (on) VeltraColors.IonCyan else VeltraColors.Titanium,
                fontSize = 13.sp, lineHeight = 20.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) VeltraColors.IonCyan.copy(.10f) else Color.Transparent)
                    .clickable { onToggle(line) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun EmptyPlate() {
    Column(
        Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(16.dp))
            .background(VeltraColors.Graphite.copy(.55f))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Belum ada teks", color = VeltraColors.Titanium, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Tekan BACA, izinkan tangkapan layar, lalu buka halaman/obrolan yang mau disalin teksnya.",
            color = VeltraColors.Muted, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 17.sp)
    }
}

@Composable
private fun HonestNote() {
    Column(
        Modifier
            .padding(horizontal = 22.dp, vertical = 18.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF12181F))
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("BACA INI SEBELUM PAKAI", color = VeltraColors.Amber, fontSize = 9.sp,
            letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        listOf(
            "• Model OCR tidak dibawa di dalam APK (biar ukurannya tetap ±2 MB, bukan 44 MB). Model diunduh sekali saat instal lewat Layanan Google Play — setelah itu pembacaan murni offline. HP tanpa Layanan Google Play tidak bisa pakai fitur ini.",
            "• Android tidak mengizinkan satu aplikasi membaca layar aplikasi lain secara diam-diam. Jalan satu-satunya yang sah adalah izin tangkap layar sistem — layar akan muncul dialog konfirmasi dan ada ikon siaran di status bar.",
            "• Yang diterima VELTRA cuma GAMBAR layar. Teksnya dibaca oleh OCR offline (ML Kit) di HP kamu. Tidak ada gambar atau teks yang dikirim ke server mana pun.",
            "• Layar yang dikunci sistem (aplikasi bank, pembayaran, DRM, beberapa obrolan) akan tertangkap hitam — itu aturan Android, bukan bug. VELTRA akan bilang jujur kalau layar tidak bisa dibaca.",
            "• Hasil tangkapan hanya hidup di memori dan hilang saat kamu menutup layar ini. Tidak ada yang disimpan ke galeri."
        ).forEach {
            Text(it, color = VeltraColors.Muted, fontSize = 11.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}
