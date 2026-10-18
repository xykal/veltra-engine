# VELTRA — Security & Anti-Piracy Foundation (Phase 2)

Dokumen ini fondasi keamanan untuk fitur **Premium**. Semua keputusan di sini diambil
sebelum satu baris kode premium ditulis — supaya tidak perlu bongkar arsitektur nanti.

## Prinsip
1. **Tidak pernah menyimpan status premium di SharedPreferences biasa.**
2. **Selalu verifikasi server-side / Play-side** — keputusan premium bukan milik klien.
3. **Grace period, bukan hard block** — user offline tetap bisa pakai 72 jam.
4. **Tidak ada root detection agresif** — VELTRA tetap jalan di semua device.

## Lapisan Keamanan

| Layer | Teknologi | Catatan |
|---|---|---|
| Integritas app | **Play Integrity API** (`MEETS_DEVICE_INTEGRITY`) | Wajib untuk cek premium, bukan untuk blokir total |
| Kepemilikan | **Google Play Billing 6.x** + `acknowledgePurchase()` | Verifikasi `purchaseToken` via backend / Play Developer API |
| Penyimpanan lokal | **Encrypted DataStore** (AES-256-GCM via Tink/Security Crypto) | Bukan plain SharedPreferences |
| Obfuskasi | **R8 full mode** + string obfuscation + `native-lib` stub | Class `BillingRepository` di-rename otomatis |
| Deteksi tamper | Cek `PackageManager.getInstallerPackageName()` + signature hash | Hanya untuk analytics, bukan hard block |
| Komunikasi | TLS 1.3 + certificate pinning (hanya kalau ada backend) | Jika murni on-device → skip |

## Aturan Kode
- ❌ Dilarang: `if (BuildConfig.DEBUG) grantPremium()` di rilis.
- ❌ Dilarang: simpan `isPremium` boolean tanpa expiry + HMAC.
- ✅ Wajib: `LicenseState` = `{ entitled, expiry, source }` dengan signature.
- ✅ Wajib: semua keputusan entitlement lewat satu `EntitlementRepository`.

## Roadmap
- [ ] P0 — R8 rules + Baseline Profile (sebelum rilis DOU)
- [ ] P1 — Encrypted DataStore untuk flag lokal
- [ ] P1 — Play Integrity wiring + fallback offline 72 jam
- [ ] P2 — Billing 6.1 + server token verification
- [ ] P2 — Anti-tamper telemetry (anonymous)

## Catatan Penting
Fitur premium VELTRA **tidak boleh** mengaktivasi performa yang sebelumnya dimatikan.
Semua fitur teknis (boost, stabilizer, monitoring) **gratis & penuh**.
Premium hanya: HUD skin, profil per-game tak terbatas, auto-schedule, statistik 30 hari.
Ini membuat VELTRA **tahan review Play Store** dan **tidak terasa seperti jebakan**.
