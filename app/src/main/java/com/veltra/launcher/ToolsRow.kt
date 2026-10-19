package com.veltra.launcher

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veltra.core.VeltraColors

/** Sudut terpotong khas VELTRA — bukan kotak biasa, bukan juga segi enam norak. */
class CutCornerShape(private val cut: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { cut.dp.toPx() }
        return Outline.Generic(Path().apply {
            moveTo(0f, c)
            lineTo(c, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - c)
            lineTo(size.width - c, size.height)
            lineTo(0f, size.height)
            close()
        })
    }
}

/** Empat alat utama — kotak 2×2 biar ukurannya lega, nggak sesak kayak 3 sebaris. */
@Composable
fun ToolsRow(
    onOcr: () -> Unit,
    onJunk: () -> Unit,
    onApps: () -> Unit,
    onDevice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text("ALAT", color = VeltraColors.Muted, fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            ToolCard(
                title = "TEKS LAYAR", sub = "baca teks offline",
                icon = Icons.Rounded.TextFields, tint = VeltraColors.IonCyan,
                modifier = Modifier.weight(1f), onClick = onOcr
            )
            Spacer(Modifier.width(10.dp))
            ToolCard(
                title = "PEMBERSIH", sub = "sampah & cache",
                icon = Icons.Rounded.AutoDelete, tint = VeltraColors.NeoMint,
                modifier = Modifier.weight(1f), onClick = onJunk
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            ToolCard(
                title = "APLIKASI", sub = "semua & detail",
                icon = Icons.Rounded.Apps, tint = VeltraColors.Cobalt,
                modifier = Modifier.weight(1f), onClick = onApps
            )
            Spacer(Modifier.width(10.dp))
            ToolCard(
                title = "PERANGKAT", sub = "info, latensi, tangkap layar",
                icon = Icons.Rounded.Devices, tint = VeltraColors.Amber,
                modifier = Modifier.weight(1f), onClick = onDevice
            )
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    sub: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(110), label = "s")
    val inf = rememberInfiniteTransition(label = "toolSweep")
    val slide by inf.animateFloat(-0.6f, 1.6f,
        infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "sw")

    val shape = remember { CutCornerShape(cut = 14f) }

    Column(
        modifier
            .scale(scale)
            .clip(shape)
            .background(VeltraColors.Graphite)
            .border(1.dp, VeltraColors.Border, shape)
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, tint.copy(0.075f), Color.Transparent),
                        start = Offset(size.width * slide - size.width * 0.5f, 0f),
                        end = Offset(size.width * slide, size.height)
                    ), blendMode = BlendMode.Screen
                )
            }
            .clickable(interactionSource = src, indication = null) { onClick() }
            .padding(vertical = 13.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(shape)
                .background(tint.copy(.13f))
                .border(1.dp, tint.copy(.45f), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(9.dp))
        Text(title, color = VeltraColors.Titanium, fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(2.dp))
        Text(sub, color = VeltraColors.Muted, fontSize = 9.sp)
    }
}
