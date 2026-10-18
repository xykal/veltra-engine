package com.veltra.launcher

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veltra.core.VeltraColors

/**
 * Komponen dasar VELTRA — bentuk, ikon, dan gerak yang seragam di semua layar.
 * Prinsipnya: garis tipis, sudut 16dp, cahaya (glow) halus, gerak lambat & mahal.
 * Tidak ada pelangi, tidak ada pantulan neon norak.
 */
object VeltraPrimitives {

    /* ───────── KARTU ───────── */

    /**
     * Kartu premium: sudut membulat, border tipis, dan cahaya yang berputar
     * pelan di balik border saat kartu ini "aktif".
     */
    @Composable
    fun PremiumCard(
        modifier: Modifier = Modifier,
        glowing: Boolean = false,
        glowColor: Color = VeltraColors.IonCyan,
        corner: Dp = 18.dp,
        content: @Composable ColumnScope.() -> Unit
    ) {
        val inf = rememberInfiniteTransition(label = "cardGlow")
        val spin by inf.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(if (glowing) 4200 else 14000, easing = LinearEasing)),
            label = "spin"
        )
        val alpha = if (glowing) 0.55f else 0.16f
        val shape = RoundedCornerShape(corner)

        Box(modifier) {
            // lapisan cahaya luar
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .drawBehind {
                        rotate(spin, pivot = center) {
                            drawRect(
                                Brush.sweepGradient(
                                    listOf(
                                        Color.Transparent,
                                        glowColor.copy(alpha * 0.55f),
                                        Color.Transparent,
                                        glowColor.copy(alpha * 0.20f),
                                        Color.Transparent
                                    ),
                                    center = center
                                ), blendMode = BlendMode.Screen
                            )
                        }
                    }
            )
            Column(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(VeltraColors.Graphite)
                    .border(1.dp, VeltraColors.Border, shape),
                content = content
            )
        }
    }

    /* ───────── TOMBOL ───────── */

    @Composable
    fun CobaltButton(
        label: String,
        icon: ImageVector? = null,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        loading: Boolean = false,
        onClick: () -> Unit
    ) {
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed) 0.965f else 1f, tween(120), label = "p")
        val shimmer = rememberInfiniteTransition(label = "sh")
        val slide by shimmer.animateFloat(
            -1f, 2f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "s"
        )

        val enabledNow = enabled && !loading
        Box(
            modifier
                .scale(scale)
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        if (enabledNow) listOf(VeltraColors.Cobalt, Color(0xFF1B4FD8))
                        else listOf(Color(0xFF1A2130), Color(0xFF151C28))
                    )
                )
                .border(
                    BorderStroke(1.dp, if (enabledNow) VeltraColors.IonCyan.copy(.35f) else VeltraColors.Border),
                    RoundedCornerShape(14.dp)
                )
                .drawWithContent {
                    drawContent()
                    if (enabledNow && !loading) {
                        val w = size.width * 0.42f
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(.16f), Color.Transparent),
                                start = Offset(size.width * slide - w, 0f),
                                end = Offset(size.width * slide, size.height)
                            ), blendMode = BlendMode.Screen
                        )
                    }
                }
                .clickable(enabledNow, interactionSource = src, indication = null) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (loading) "MEMPROSES…" else label,
                    color = if (enabledNow) Color.White else VeltraColors.Muted,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp
                )
            }
        }
    }

    @Composable
    fun GhostChip(
        label: String,
        icon: ImageVector? = null,
        modifier: Modifier = Modifier,
        selected: Boolean = false,
        onClick: () -> Unit
    ) {
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(110), label = "g")
        val bg = if (selected) VeltraColors.IonCyan.copy(.13f) else VeltraColors.Graphite
        val bd = if (selected) VeltraColors.IonCyan.copy(.8f) else VeltraColors.Border
        Row(
            modifier
                .scale(scale)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .border(1.dp, bd, RoundedCornerShape(12.dp))
                .clickable(indication = null, interactionSource = src) { onClick() }
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = if (selected) VeltraColors.IonCyan else VeltraColors.Muted,
                    modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
            }
            Text(label, color = if (selected) VeltraColors.IonCyan else VeltraColors.Titanium,
                fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
        }
    }

    /* ───────── TEKS ───────── */

    @Composable
    fun SectionTitle(text: String, hint: String? = null) {
        Column(Modifier.padding(bottom = 10.dp)) {
            Text(text, color = VeltraColors.Titanium, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
            if (hint != null) {
                Spacer(Modifier.height(4.dp))
                Text(hint, color = VeltraColors.Muted, fontSize = 10.5.sp, lineHeight = 15.sp)
            }
        }
    }

    @Composable
    fun SwitchRow(
        title: String,
        subtitle: String? = null,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    onCheckedChange(!checked)
                }
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = VeltraColors.Titanium, fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = VeltraColors.Muted, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = checked, onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VeltraColors.Obsidian,
                    checkedTrackColor = VeltraColors.IonCyan,
                    checkedBorderColor = VeltraColors.IonCyan,
                    uncheckedThumbColor = VeltraColors.Muted,
                    uncheckedTrackColor = Color(0xFF1C2331),
                    uncheckedBorderColor = VeltraColors.Border
                )
            )
        }
    }

    /* ───────── IKON STATUS ───────── */

    /** Lingkaran centang yang menggambar dirinya sendiri — dipakai di hasil aksi. */
    @Composable
    fun SuccessBurst(visible: Boolean, label: String, modifier: Modifier = Modifier) {
        val scale by animateFloatAsState(if (visible) 1f else 0.6f, tween(320), label = "b")
        val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(260), label = "a")
        if (alpha > 0.01f) {
            Row(
                modifier.graphicsLayer {
                    this.alpha = alpha; scaleX = scale; scaleY = scale
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(VeltraColors.NeoMint.copy(.16f))
                        .border(1.dp, VeltraColors.NeoMint, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, null, tint = VeltraColors.NeoMint,
                        modifier = Modifier.size(13.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(label, color = VeltraColors.NeoMint, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }

    /** Garis miring halus buat latar — biar nggak polos, tapi tetap kalem. */
    @Composable
    fun DiagonalWash(alpha: Float = 0.05f): Brush =
        Brush.linearGradient(
            listOf(Color.Transparent, Color.White.copy(alpha), Color.Transparent),
            start = Offset(0f, 0f), end = Offset(700f, 700f),
            tileMode = TileMode.Clamp
        )
}

/** Modifier helper: latar gradasi khas VELTRA. */
fun Modifier.veltraBackdrop(): Modifier = this.background(
    Brush.verticalGradient(
        listOf(Color(0xFF0C121C), VeltraColors.Obsidian, Color(0xFF080B11))
    )
)
