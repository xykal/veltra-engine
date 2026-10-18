# VELTRA — Game Performance System

**Rilis terbaru:** [v1.0.2 — UI Disamakan dengan Referensi](https://github.com/xykal/veltra-engine/releases/tag/v1.0.2) · APK 1.9 MB · AAB 3.6 MB

[![Build](https://github.com/xykal/veltra-engine/actions/workflows/build.yml/badge.svg)](https://github.com/xykal/veltra-engine/actions/workflows/build.yml)
✅ **Build status: GREEN** — `assembleDebug` sukses (APK 12 MB)
> **All Device • All Games • Native Android (Kotlin + Compose)**

**Status:** PRD v1.0 LOCKED — Ready to Build  
**Brand:** VELTRA — Game Launcher & FPS Stabilizer  
**Dashboard:** LOCKED (obsidian premium) — `veltra-dashboard-mockup.png`  
**Panel:** V2 Premium HUD — `veltra-panel-v2-premium-hud.png`

### Download & Install
1. Buka [Releases](https://github.com/xykal/veltra-engine/releases)
2. Unduh `veltra-release.apk` (1.7 MB)
3. Install → buka → **izinkan Overlay** saat diminta

### Quick Start
```bash
./gradlew assembleDebug  # APK di app/build/outputs/apk/debug/
./gradlew bundleRelease  # AAB untuk Play Store
```

### Struktur Modular
```
veltra/
├── app/            → Launcher UI (Compose) + Billing + Navigation
├── core-boost/     → Engine: trimMemory, DNS switch, DND, Performance Hint API
├── overlay/        → WindowManager + Floating Panel HUD (60fps)
├── data/           → Room + DataStore Encrypted + Repositories
├── lottie/         → Launch animation JSON (<400KB)
└── .github/workflows/build.yml → CI auto build APK/AAB
```

### Flow utama (v1.0.1+)
Tap game di VELTRA → bersihkan RAM → **panel overlay nyala** → game kebuka.
Panel otomatis **sembunyi** saat keluar game (butuh izin Akses Penggunaan).

## LIVE GUARD — Live TikTok nggak boleh mati pas main game

Masalah HP kentang: **game makan RAM → sistem bunuh TikTok → live putus.**

| Yang VELTRA lakukan | Cara |
|---|---|
| Bersihkan RAM **sebelum** live, bukan saat live jalan | `BoostEngine.runLiveBoost()` |
| TikTok/Lite/IG/YT/Twitch **whitelist permanen** | `StreamApps.ALL` — nggak pernah dibunuh |
| **LiveWatchdog** cek nyawa tiap 5 detik (scan `/proc`) | `LiveWatchdog.start()` → notif "LIVE PUTUS" |
| Turunkan **target FPS game** biar encoder live kebagian jatah | `HintManager.setTargetFps(24/30/40/60)` |
| **Panduan per-ROM** (MIUI, One UI, ColorOS, Funtouch, HiOS) | `OemGuide.steps()` |
| Chip **LIVE TERJAGA / LIVE PUTUS** di panel overlay | `VeltraHud → LiveChip()` |

**Yang TIDAK bisa dilakukan (dan nggak ada app pihak ketiga yang bisa):**
cekal Low Memory Killer sistem, paksa split-screen, atau nyambungin live yang udah putus.

Lihat PRD §09 untuk root cause lengkap + aturan emas streaming di HP kentang.

### DOU (MVP) Checklist
- [ ] Auto-scan game (PackageManager)
- [ ] Launcher grid 2 kolom + STABILIZE
- [ ] Boost engine (whitelist per brand)
- [ ] Overlay permission onboarding
- [ ] Floating V 42dp → Panel 300x420 HUD
- [ ] FPS est. (Choreographer) + Ping + Temp
- [ ] DND + Launch Lottie 1.4s
- [ ] R8 + Baseline Profile + Play Integrity

### GitHub → Play Store (repo PUBLIC)
1. `export GITHUB_TOKEN=ghp_xxx` (Fine-grained PAT, scope: Contents R/W, 1 repo)
2. `./push_github.sh <username> veltra-engine` → repo kebentuk + langsung push
3. **Settings → Secrets → Actions**, isi:
   `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
4. Push ke `main` → Actions otomatis build **signed AAB**
5. Upload AAB ke Play Console

> ⚠️ Repo public = kode bisa dilihat siapa pun. **Jangan pernah** commit `.jks`,
> `keystore.properties`, atau API key. Semua sudah di-`.gitignore`.

### License
Source-available untuk referensi & kontribusi. Brand **VELTRA**, nama produk,
dan aset visual (logo, HUD, mockup) tetap milik VELTRA Labs.
Detail lisensi menyusul — jangan republish sebagai produk sendiri.

### Tech Stack (LOCKED Native)
Kotlin 2.x • Compose + M3 • Hilt • Room • DataStore • ForegroundService • WindowManager • GameMode API • Billing 6.1

---
© 2026 VELTRA Labs — *Make every device feel flagship.*
