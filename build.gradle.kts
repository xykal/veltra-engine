// Top-level build file — VELTRA
//
// PENTING: semua plugin yang dipakai modul WAJIB dideklarasi di sini dengan `apply false`.
// Kalau tidak, Gradle error: "already on the classpath with an unknown version".
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
}
