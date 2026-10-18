package com.veltra.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veltra.core.VeltraColors
import com.veltra.overlay.crosshair.CrosshairConfig
import com.veltra.overlay.crosshair.CrosshairStyle
import com.veltra.overlay.crosshair.VeltraCrosshair

/** Warna crosshair yang kepakai di segala map. */
private val Swatches = listOf(
    "MINT" to 0xFF00E6A0,
    "CYAN" to 0xFF00D1FF,
    "PUTIH" to 0xFFFFFFFF,
    "KUNING" to 0xFFFFD400,
    "MERAH" to 0xFFFF3B30,
    "UNGU" to 0xFFB45CFF
)

@Composable
fun CrosshairCard(
    config: CrosshairConfig,
    hasOverlayPermission: Boolean,
    onChange: (CrosshairConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VeltraColors.Graphite),
        border = androidx.compose.foundation.BorderStroke(1.dp, VeltraColors.Border)
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(VeltraColors.NeoMint.copy(.12f))
                        .border(1.dp, VeltraColors.NeoMint.copy(.35f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CenterFocusStrong, null, tint = VeltraColors.NeoMint,
                        modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("CROSSHAIR PRESISI", color = VeltraColors.Titanium, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                    Text("Bantu bidik • sentuhan tetap tembus ke game",
                        color = VeltraColors.Muted, fontSize = 9.sp)
                }
                androidx.compose.material3.Switch(
                    checked = config.enabled && hasOverlayPermission,
                    enabled = hasOverlayPermission,
                    onCheckedChange = { onChange(config.copy(enabled = it)) },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = VeltraColors.Obsidian,
                        checkedTrackColor = VeltraColors.NeoMint,
                        checkedBorderColor = VeltraColors.NeoMint,
                        uncheckedThumbColor = VeltraColors.Muted,
                        uncheckedTrackColor = Color(0xFF1C2331),
                        uncheckedBorderColor = VeltraColors.Border
                    )
                )
            }

            if (!hasOverlayPermission) {
                Spacer(Modifier.height(8.dp))
                Text("Butuh izin “Tampilkan di atas aplikasi lain” dulu.",
                    color = VeltraColors.Amber, fontSize = 9.5.sp)
            }

            AnimatedVisibility(
                visible = config.enabled,
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    CrosshairPreview(config)
                    Spacer(Modifier.height(12.dp))

                    Text("BENTUK", color = VeltraColors.Muted, fontSize = 8.5.sp,
                        letterSpacing = 1.8.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        CrosshairStyle.entries.forEach { st ->
                            val on = st == config.style
                            Box(
                                Modifier
                                    .padding(end = 7.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (on) VeltraColors.NeoMint.copy(.14f) else Color(0xFF10161F))
                                    .border(1.dp, if (on) VeltraColors.NeoMint else VeltraColors.Border,
                                        RoundedCornerShape(10.dp))
                                    .clickable { onChange(config.copy(style = st)) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(st.label, color = if (on) VeltraColors.NeoMint else VeltraColors.Titanium,
                                    fontSize = 10.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("WARNA", color = VeltraColors.Muted, fontSize = 8.5.sp,
                        letterSpacing = 1.8.sp)
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Swatches.forEach { (name, argb) ->
                            val on = argb == config.colorArgb
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(2.dp, if (on) Color.White else Color.Transparent, CircleShape)
                                    .clickable { onChange(config.copy(colorArgb = argb)) }
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    ChSlider("Ukuran", config.sizeDp.toFloat(), 8f, 60f, "dp") {
                        onChange(config.copy(sizeDp = it.toInt()))
                    }
                    ChSlider("Tebal", config.thicknessDp.toFloat(), 1f, 8f, "dp") {
                        onChange(config.copy(thicknessDp = it.toInt()))
                    }
                    ChSlider("Celah tengah", config.gapDp.toFloat(), 0f, 20f, "dp") {
                        onChange(config.copy(gapDp = it.toInt()))
                    }
                    ChSlider("Jelas", config.alpha, 0.25f, 1f, "%") {
                        onChange(config.copy(alpha = it))
                    }
                    ChSlider("Geser X", config.offsetXDp.toFloat(), -160f, 160f, "dp") {
                        onChange(config.copy(offsetXDp = it.toInt()))
                    }
                    ChSlider("Geser Y", config.offsetYDp.toFloat(), -240f, 240f, "dp") {
                        onChange(config.copy(offsetYDp = it.toInt()))
                    }

                    Spacer(Modifier.height(8.dp))
                    VeltraPrimitives.SwitchRow(
                        title = "Sembunyikan di luar game",
                        subtitle = "Butuh izin Akses Penggunaan. Kalau mati, crosshair selalu tampil.",
                        checked = config.onlyInGame
                    ) { onChange(config.copy(onlyInGame = it)) }

                    Spacer(Modifier.height(10.dp))
                    CrosshairHonestNote()
                }
            }
        }
    }
}

@Composable
private fun CrosshairPreview(config: CrosshairConfig) {
    val inf = rememberInfiniteTransition(label = "chPrev")
    val sweep by inf.animateFloat(-0.3f, 1.4f,
        infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "s")
    Box(
        Modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A2436), Color(0xFF0B1018))
                )
            )
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(13.dp))
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(.05f), Color.Transparent),
                        start = Offset(size.width * sweep - 120f, 0f),
                        end = Offset(size.width * sweep, size.height)
                    ), blendMode = BlendMode.Screen
                )
            }
    ) {
        VeltraCrosshair(config = config, preview = true)
        Text("PRATINJAU", color = VeltraColors.Muted, fontSize = 8.sp, letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}

@Composable
private fun ChSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    unit: String,
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = VeltraColors.Titanium, fontSize = 10.5.sp,
                modifier = Modifier.weight(1f))
            Text(
                if (unit == "%") "${(value * 100).toInt()}%" else "${value.toInt()} $unit",
                color = VeltraColors.NeoMint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value, onValueChange = onChange, valueRange = min..max,
            modifier = Modifier.fillMaxWidth().height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = VeltraColors.NeoMint,
                activeTrackColor = VeltraColors.NeoMint,
                inactiveTrackColor = VeltraColors.Border
            )
        )
    }
}

@Composable
private fun CrosshairHonestNote() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF12181F))
            .border(1.dp, VeltraColors.Border, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("YANG HARUS LU TAHU", color = VeltraColors.Amber, fontSize = 8.5.sp,
            letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        listOf(
            "• Ini murni gambar di atas layar (izin “tampilkan di atas aplikasi lain”), persis kayak chat head atau perekam layar. VELTRA tidak membaca memori game, tidak mengubah nilai aim, dan tidak ada auto-aim sedikit pun.",
            "• Window-nya dibuat tidak bisa disentuh, jadi jari lu tembus 100% ke game — nggak ada area mati atau ghost touch.",
            "• Posisi digeser pakai Geser X / Geser Y di atas. Semua setting langsung kelihatan di pratinjau.",
            "• Risikonya bukan teknis tapi kebijakan: sebagian game/turnamen melarang overlay pihak ketiga walau cuma gambar. Pakai dengan risiko lu sendiri; VELTRA tidak pernah menyentuh data game."
        ).forEach {
            Text(it, color = VeltraColors.Muted, fontSize = 10.sp, lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 5.dp))
        }
    }
}
