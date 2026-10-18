# VELTRA — Contributing & Build Guide

## Setup lokal
```bash
# 1. JDK 17 wajib (AGP 8.5)
java -version   # harus 17

# 2. Android SDK 35 wajib
export ANDROID_HOME=$HOME/Android/Sdk

# 3. Build
./gradlew assembleDebug
```

## Kolaborasi GitHub
1. **Owner** buat repo private `veltra-engine`
2. **Settings → Collaborators → Add people** → masukkan username kolaborator
3. Kolaborator: `git clone` → `git checkout -b feat/nama-fitur`
4. PR ke `dev`, review, merge ke `main` → Actions jalan otomatis

## Branch
| Branch | Fungsi |
|---|---|
| `main` | Rilis. Push ke sini → Actions build **signed AAB** |
| `dev`  | Development. Push → Actions build **debug APK** |
| `feat/*` | Fitur |

## GitHub Secrets yang dibutuhkan
| Secret | Isi |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 veltra.jks` |
| `KEYSTORE_PASSWORD` | password keystore |
| `KEY_ALIAS` | alias key |
| `KEY_PASSWORD` | password key |

Tambahkan di **Settings → Secrets and variables → Actions**.
Jangan pernah commit file `.jks` atau `keystore.properties`.

## Konvensi
- Kotlin: `ktlint` style, 4 spasi (default IntelliJ)
- Compose: `@Composable` tanpa side-effect langsung — efek lewat `LaunchedEffect`
- Semua aksi boost harus lewat `BoostEngine` — tidak ada `Runtime.exec` di UI layer
