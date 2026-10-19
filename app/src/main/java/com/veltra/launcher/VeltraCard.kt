package com.veltra.launcher

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veltra.core.VeltraColors

/**
 * SATU BENTUK KARTU BUAT SEMUA LAYAR.
 *
 * Gaya ini ngikutin kartu Pembersih yang udah disetujui:
 *  - latar Graphite, border 1dp, sudut 16dp
 *  - kepala: kotak ikon 26dp (sudut 8dp) + judul huruf kapital + aksi di kanan
 *  - isi: jarak 14dp
 *  - catatan jujur: kotak abu gelap dengan judul amber
 *
 * Semua layar (Live Guard, izin, Mode Lanjut, crosshair, sensitivitas, aplikasi)
 * wajib pakai komponen ini biar tampilannya nyambung.
 */
@Composable
fun VeltraCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accent: Color = VeltraColors.IonCyan,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(VeltraColors.Graphite)
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(.13f))
                        .border(1.dp, accent.copy(.38f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = VeltraColors.Titanium, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = VeltraColors.Muted, fontSize = 9.sp, lineHeight = 13.sp)
                }
            }
            if (action != null) {
                Spacer(Modifier.width(8.dp))
                action()
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** Kotak catatan jujur — dipakai di semua layar yang punya batas/keterbatasan. */
@Composable
fun VeltraNote(title: String, lines: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF12181F))
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(13.dp))
            .padding(12.dp)
    ) {
        Text(title, color = VeltraColors.Amber, fontSize = 8.5.sp,
            letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        lines.forEach {
            Text(it, color = VeltraColors.Muted, fontSize = 10.sp, lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 5.dp))
        }
    }
}

/** Baris isi kartu — dipakai daftar aplikasi, daftar sampah, dst. */
@Composable
fun VeltraRow(
    modifier: Modifier = Modifier,
    radius: Int = 13,
    selected: Boolean = false,
    accent: Color = VeltraColors.IonCyan,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.dp))
            .background(if (selected) accent.copy(.10f) else VeltraColors.Graphite)
            .border(1.dp, if (selected) accent.copy(.7f) else VeltraColors.Border,
                RoundedCornerShape(radius.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Ubah jadi bisa diklik tanpa nambah import di tiap file. */
fun Modifier.cardClick(onClick: () -> Unit): Modifier =
    this.then(
        androidx.compose.foundation.clickable(
            indication = null,
            interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource()
        ) { onClick() }
    )

/** Kotak metrik kecil (label + angka) — ukuran seragam, nggak gepeng. */
@Composable
fun VeltraStat(
    label: String,
    value: String,
    color: Color = VeltraColors.Titanium,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101720))
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = VeltraColors.Muted, fontSize = 8.sp, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** Titik status kecil. */
@Composable
fun StatusDot(color: Color) {
    Box(Modifier.size(6.dp).clip(CircleShape).background(color))
}
