# VELTRA — Game Performance System

**Rilis terbaru:** [v1.0.3 — Crosshair • Pembaca Teks • Pembersih • Game Mode](https://github.com/xykal/veltra-engine/releases/tag/v1.0.3) · APK 2.3 MB · AAB 5.0 MB

[![Build](https://github.com/xykal/veltra-engine/actions/workflows/build.yml/badge.svg)](https://github.com/xykal/veltra-engine/actions/workflows/build.yml)
✅ **Build status: GREEN** — `assembleDebug` sukses (APK 12 MB)
> **All Device • All Games • Native Android (Kotlin + Compose)**

**Status:** PRD v1.0 LOCKED — Ready to Build  
**Brand:** VELTRA — Game Launcher & FPS Stabilizer  
**Dashboard:** LOCKED (obsidian premium) — `veltra-dashboard-mockup.png`  
**Panel:** V2 Premium HUD — `veltra-panel-v2-premium-hud.png`

### Download & Install
1. Buka [Releases](https://github.com/xykal/veltra-engine/releases)
2. Unduh `veltra-release.apk` (2.3 MB)
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


## v1.0.3 — fitur baru

### 🎯 Crosshair presisi
Gambar bantu bidik di atas game. 7 bentuk (titik, silang, silang+titik, lingkar+titik,
silang+lingkar, huruf T, presisi), atur ukuran/tebal/celah/warna/jelas/geser X-Y,
pratinjau langsung. Window-nya `FLAG_NOT_TOUCHABLE` — **sentuhan tembus 100% ke game**.
Bisa sembunyi otomatis di luar game.
> Jujur: ini cuma gambar di atas layar. VELTRA tidak membaca memori game, tidak mengubah
> nilai aim, dan tidak ada auto-aim. Sebagian game/turnamen melarang overlay pihak ketiga.

### 📖 Pembaca teks layar (OCR offline)
Ambil teks dari layar tanpa mengunggah apa pun — OCR jalan di perangkat (ML Kit).
Baris bisa diketuk, deteksi otomatis nomor HP / email / link / kode, salin atau bagikan.
Jalan lewat **izin tangkap layar sistem** (dialog konfirmasi + ikon siaran di status bar),
karena Android tidak mengizinkan aplikasi membaca layar aplikasi lain secara diam-diam.
Layar berflag SECURE (bank/pembayaran) tertangkap hitam → VELTRA bilang jujur.
Model OCR diunduh sekali lewat Layanan Google Play (APK tetap ±2 MB).

### 🧹 Pembersih sampah
Sisa .apk, berkas sementara, folder kosong, berkas >100 MB. Dengan Shizuku/root: `pm trim-caches`.
> `Android/data` & `Android/obb` aplikasi lain dikunci sistem sejak Android 11 — tidak ada
> pembersih pihak ketiga yang bisa menyentuhnya tanpa akses khusus.

### 📦 Semua aplikasi
Daftar lengkap dengan ikon asli, versi, ukuran, tanggal pasang/perbarui, minSdk/targetSdk,
sumber instal, dan **salin APK** ke `Download/VELTRA/APK`.

### ⚡ Injeksi Game Mode + kunci layar/FPS
`cmd game mode performance`, `--downscale`, `--fps`, `device_config put game_overlay`,
`cmd package compile -m speed -f` — persis jalur Game Dashboard/Game Genie bawaan.
Kunci refresh rate 60/90/120/144 Hz (`settings put system peak/min_refresh_rate`, butuh
Shizuku/root) dan target FPS game lewat Game Mode intervention.
> Jujur: VELTRA **tidak bisa** mengubah sensitivitas dalam game (itu data game = risiko
> banned). Yang disediakan: **catatan sensitivitas** per game. Panel 60Hz tetap 60Hz —
> nggak ada aplikasi yang bisa mengubah kemampuan layar.

### 🐛 Perbaikan dari pemakaian nyata
- Panel tidak lagi hilang sendiri di dalam game (auto-hide butuh 3x konfirmasi + grace 20s).
- Ghost touch & area mati hilang: `FLAG_NOT_TOUCH_MODAL` + window benar-benar dicabut saat disembunyikan.

### 🎨 Animasi & bentuk baru
Splash V2 (heksagon orbit, logo menggambar dirinya sendiri, baris boot), komponen dasar
seragam (`VeltraPrimitives`), sudut terpotong khas VELTRA (`CutCornerShape`), kartu game
pakai ikon asli game.

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
