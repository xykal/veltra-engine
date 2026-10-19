package com.veltra.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.veltra.core.VeltraColors
import com.veltra.core.VeltraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

/**
 * ANIMASI PAS MAU BUKA GAME.
 *
 * Urutannya kelihatan jelas di layar (bukan cuma nunggu layar hitam):
 *   BERSIHKAN RAM → PASANG PANEL → STABILKAN → buka game → layar ini ilang.
 *
 * Yang dikerjakan di balik layar: `LauncherViewModel.launchDirect()`
 * (trim RAM aman, nyalain monitor & panel overlay, crosshair kalau aktif).
 */
@AndroidEntryPoint
class GameLaunchActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PKG = "pkg"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val pkg = intent?.getStringExtra(EXTRA_PKG).orEmpty()
        val pm = packageManager
        val name = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
        val icon = runCatching { pm.getApplicationIcon(pkg).toBitmap(160, 160).asImageBitmap() }
            .getOrNull()

        setContent {
            VeltraTheme {
                GameLaunchOverlay(
                    gameName = name,
                    gameIcon = icon,
                    onFinished = {
                        // RAM udah dibersihkan & panel udah nyala dari LauncherViewModel.
                        // Sekarang tinggal buka gamenya.
                        if (pkg.isNotBlank()) {
                            runCatching {
                                pm.getLaunchIntentForPackage(pkg)
                                    ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    ?.let { startActivity(it) }
                            }
                        }
                        finish()
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    }
                )
            }
        }
    }
}

@Composable
private fun GameLaunchOverlay(
    gameName: String,
    gameIcon: androidx.compose.ui.graphics.ImageBitmap?,
    onFinished: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "gl")
    val orbit by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "o")
    val pulse by inf.animateFloat(0.86f, 1.12f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "p")

    var step by remember { mutableIntStateOf(0) }
    val steps = listOf("BERSIHKAN RAM", "PASANG PANEL", "STABILKAN", "BUKA GAME")

    LaunchedEffect(Unit) {
        delay(260); step = 1
        delay(300); step = 2
        delay(300); step = 3
        delay(320); step = 4
        delay(300); onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF0F1826), VeltraColors.Obsidian, Color(0xFF05080D)),
                    radius = 1400f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Cincin + ikon game
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val r = size.minDimension / 2 - 10.dp.toPx()
                    drawArc(Color.White.copy(.06f), 0f, 360f, false,
                        style = Stroke(width = 2.dp.toPx()))
                    rotate(orbit, pivot = Offset(cx, cy)) {
                        drawArc(
                            Brush.sweepGradient(
                                listOf(Color.Transparent, VeltraColors.Cobalt,
                                    VeltraColors.IonCyan, Color.Transparent)
                            ),
                            0f, 165f, false,
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    rotate(-orbit * 0.7f, pivot = Offset(cx, cy)) {
                        for (i in 0 until 5) {
                            val a = Math.toRadians((i * 72f).toDouble())
                            drawCircle(
                                VeltraColors.IonCyan.copy(.8f),
                                radius = (2.4f * pulse).dp.toPx(),
                                center = Offset(cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat())
                            )
                        }
                    }
                }

                Box(
                    Modifier
                        .size((86 * pulse).dp.coerceAtMost(110.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(VeltraColors.Cobalt.copy(.35f), Color.Transparent)
                            )
                        )
                )

                Box(
                    Modifier
                        .size(74.dp)
                        .border(1.dp, VeltraColors.IonCyan.copy(.45f), CutCornerShape(cut = 16f))
                        .background(Color(0xFF0C121C), CutCornerShape(cut = 16f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (gameIcon != null) {
                        Image(gameIcon, null, Modifier.size(48.dp))
                    } else {
                        Text(gameName.take(1).uppercase(), color = VeltraColors.IonCyan,
                            fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(gameName, color = VeltraColors.Titanium, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 34.dp))
            Spacer(Modifier.height(4.dp))
            Text("VELTRA MENYIAPKAN SESI", color = VeltraColors.Muted, fontSize = 9.sp,
                letterSpacing = 3.sp)

            Spacer(Modifier.height(26.dp))

            // Langkah-langkah
            Column(horizontalAlignment = Alignment.Start) {
                steps.forEachIndexed { i, label ->
                    val done = step > i
                    val active = step == i
                    val alpha by animateFloatAsState(
                        if (done || active) 1f else 0.25f, tween(220), label = "s$i"
                    )
                    Row(
                        Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(
                                    (if (done) VeltraColors.NeoMint else VeltraColors.IonCyan)
                                        .copy(alpha)
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            label + if (done) "  ✓" else if (active) " …" else "",
                            color = (if (done) VeltraColors.NeoMint else VeltraColors.Titanium).copy(alpha),
                            fontSize = 10.5.sp, letterSpacing = 1.6.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .width(160.dp)
                    .height(2.dp)
                    .background(Color.White.copy(.08f))
            ) {
                val p by animateFloatAsState(step / 4f, tween(260), label = "bar")
                Box(
                    Modifier
                        .fillMaxWidth(p.coerceAtLeast(0.02f))
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(VeltraColors.Cobalt, VeltraColors.IonCyan)
                            )
                        )
                )
            }
        }
    }
}
