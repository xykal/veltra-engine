package com.veltra.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veltra.boost.profile.GameProfile
import com.veltra.core.VeltraColors

/**
 * PROFIL GAME — pengaturan per-game yang dipakai otomatis.
 *
 * Jujur dari awal:
 *  - "Target FPS" adalah **hint** ke scheduler (ADPF), bukan perintah. Game dengan
 *    batas FPS sendiri tetap menang.
 *  - "Kunci FPS" dan "Kunci Hz" baru jalan kalau Shizuku/ADB atau root aktif.
 *  - "Jaga live" menyalakan Live Guard otomatis, tapi live TETAP harus lu mulai
 *    sendiri sebelum game dibuka — Android 14+ melarang aplikasi menyalakan
 *    kamera dari background.
 */
@Composable
fun ProfilesScreen(
    onBack: () -> Unit,
    vm: LauncherViewModel = hiltViewModel()
) {
    val games by vm.games.collectAsState()
    val profiles by vm.gameProfiles.collectAsState()
    var expandedPkg by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(VeltraColors.Obsidian)
    ) {
        // ── Kepala ───────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 26.dp, start = 8.dp, end = 20.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = VeltraColors.Titanium)
            }
            Column(Modifier.weight(1f)) {
                Text("PROFIL GAME", color = VeltraColors.Titanium, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.5.sp)
                Spacer(Modifier.height(3.dp))
                Text("Diterapkan otomatis tiap game dibuka",
                    color = VeltraColors.Muted, fontSize = 10.sp)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            VeltraNote(
                title = "YANG BISA DAN YANG TIDAK",
                lines = listOf(
                    "• Target FPS = hint ke sistem (ADPF). Game yang mengunci FPS-nya sendiri tetap menang — VELTRA tidak membongkar batas itu.",
                    "• Kunci FPS & kunci refresh rate butuh Shizuku/ADB atau root. Tanpa itu, tombolnya tetap bisa ditekan tapi hasilnya ditolak sistem.",
                    "• Panel 60Hz tetap 60Hz. Tidak ada aplikasi yang bisa mengubah kemampuan layar fisik."
                )
            )

            Spacer(Modifier.height(14.dp))

            val custom = profiles.count { it.value.isCustom }
            Text(
                "${games.size} game terpasang  •  $custom profil disetel",
                color = VeltraColors.Muted, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp
            )
            Spacer(Modifier.height(12.dp))

            games.forEach { g ->
                val profile = profiles[g.packageName] ?: GameProfile.defaultFor(g.packageName)
                val open = expandedPkg == g.packageName
                ProfileCard(
                    name = g.name,
                    pkg = g.packageName,
                    profile = profile,
                    open = open,
                    onToggle = { expandedPkg = if (open) null else g.packageName },
                    onChange = { vm.saveProfile(it) },
                    onReset = { vm.removeProfile(g.packageName) }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileCard(
    name: String,
    pkg: String,
    profile: GameProfile,
    open: Boolean,
    onToggle: () -> Unit,
    onChange: (GameProfile) -> Unit,
    onReset: () -> Unit
) {
    var draft by remember(pkg) { mutableStateOf(profile) }
    LaunchedEffect(profile) { if (!open) draft = profile }

    val accent = if (draft.isCustom) VeltraColors.NeoMint else VeltraColors.IonCyan

    VeltraCard(
        title = name.ifBlank { pkg },
        subtitle = pkg,
        icon = Icons.Rounded.Games,
        accent = accent,
        action = {
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(.13f))
                    .border(1.dp, accent.copy(.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (draft.isCustom) Icons.Rounded.Check else Icons.Rounded.Tune,
                    null, tint = accent, modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (draft.isCustom) "DISETEL" else "BAWAAN",
                    color = accent, fontSize = 8.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                )
            }
        }
    ) {
        // Ringkasan saat ditutup
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { onToggle() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${draft.targetFps} FPS • ${draft.boost.label}" +
                            (if (draft.fpsLock > 0) " • kunci ${draft.fpsLock}" else "") +
                            (if (draft.hzLock > 0) " • ${draft.hzLock}Hz" else "") +
                            (if (draft.crosshair) " • crosshair" else "") +
                            (if (draft.quiet) " • senyap" else "") +
                            (if (draft.liveGuard) " • jaga live" else ""),
                    color = VeltraColors.Muted, fontSize = 9.5.sp, lineHeight = 13.sp
                )
            }
            Icon(
                if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null, tint = VeltraColors.Muted, modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = open,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(VeltraColors.Border))
                Spacer(Modifier.height(12.dp))

                GroupLabel("TARGET FPS", "hint ke sistem, bukan paksaan")
                ChipRow(GameProfile.DEFAULT_FPS_CHOICES.map { it.toString() },
                    GameProfile.DEFAULT_FPS_CHOICES.map { it == draft.targetFps }) {
                    draft = draft.copy(targetFps = it.toInt())
                }

                Spacer(Modifier.height(12.dp))
                GroupLabel("KUNCI FPS GAME", "butuh Shizuku/root • restart game")
                ChipRow(listOf("OFF", "30", "45", "60", "90"),
                    GameProfile.LOCK_FPS_CHOICES.map { it == draft.fpsLock }) {
                    draft = draft.copy(fpsLock = if (it == "OFF") 0 else it.toInt())
                }

                Spacer(Modifier.height(12.dp))
                GroupLabel("KUNCI REFRESH RATE", "butuh Shizuku/root")
                ChipRow(listOf("OFF", "60", "90", "120"),
                    GameProfile.HZ_CHOICES.map { it == draft.hzLock }) {
                    draft = draft.copy(hzLock = if (it == "OFF") 0 else it.toInt())
                }

                Spacer(Modifier.height(12.dp))
                GroupLabel("LEVEL PEMBERSIHAN")
                GameProfile.BoostLevel.values().forEach { lvl ->
                    Spacer(Modifier.height(6.dp))
                    VeltraRow(
                        selected = draft.boost == lvl,
                        accent = VeltraColors.IonCyan,
                        onClick = { draft = draft.copy(boost = lvl) }
                    ) {
                        Icon(Icons.Rounded.CleaningServices, null,
                            tint = if (draft.boost == lvl) VeltraColors.IonCyan else VeltraColors.Muted,
                            modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(lvl.label, color = VeltraColors.Titanium, fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold)
                            Text(lvl.hint, color = VeltraColors.Muted, fontSize = 9.sp,
                                lineHeight = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                GroupLabel("OTOMATIS SAAT GAME DIBUKA")
                Spacer(Modifier.height(6.dp))
                VeltraPrimitives.SwitchRow(
                    title = "Crosshair menyala",
                    subtitle = "Gambar bantu bidik overlay VELTRA",
                    checked = draft.crosshair
                ) { draft = draft.copy(crosshair = it) }
                VeltraPrimitives.SwitchRow(
                    title = "Mode senyap",
                    subtitle = "DND kalau izin ada; tanpa izin, dering dimatikan",
                    checked = draft.quiet
                ) { draft = draft.copy(quiet = it) }
                VeltraPrimitives.SwitchRow(
                    title = "Jaga live otomatis",
                    subtitle = "Live Guard menyala sendiri (live tetap harus lu mulai dulu)",
                    checked = draft.liveGuard
                ) { draft = draft.copy(liveGuard = it) }

                Spacer(Modifier.height(12.dp))
                GroupLabel("CATATAN", "mis. angka sensitivitas — murni catatan lu")
                Spacer(Modifier.height(6.dp))
                TextField(
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) },
                    placeholder = {
                        Text("mis. General 95 • Red Dot 90 • Scope 85",
                            color = VeltraColors.Muted, fontSize = 11.sp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, VeltraColors.Border, RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF101720),
                        unfocusedContainerColor = Color(0xFF101720),
                        disabledContainerColor = Color(0xFF101720),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = VeltraColors.Titanium,
                        unfocusedTextColor = VeltraColors.Titanium,
                        cursorColor = VeltraColors.IonCyan
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
                    maxLines = 4
                )

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VeltraPrimitives.CobaltButton(
                        "SIMPAN", Icons.Rounded.Check, Modifier.weight(1f)
                    ) { onChange(draft) }
                    VeltraPrimitives.GhostChip(
                        "RESET", Icons.Rounded.RestartAlt, Modifier.weight(1f)
                    ) {
                        draft = GameProfile.defaultFor(pkg)
                        onReset()
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String, hint: String? = null) {
    Text(text, color = VeltraColors.Muted, fontSize = 8.5.sp,
        letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
    if (hint != null) {
        Spacer(Modifier.height(2.dp))
        Text(hint, color = Color(0xFF6B7588), fontSize = 9.sp, lineHeight = 12.sp)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ChipRow(
    labels: List<String>,
    selected: List<Boolean>,
    onPick: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, label ->
            VeltraPrimitives.GhostChip(
                label = label,
                selected = selected.getOrElse(i) { false },
                modifier = Modifier.weight(1f)
            ) { onPick(label) }
            if (i != labels.lastIndex) Spacer(Modifier.width(6.dp))
        }
    }
}
