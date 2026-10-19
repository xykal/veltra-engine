package com.veltra.launcher

/** Penanda sederhana: aplikasi lagi di depan atau enggak (dipakai buat hemat kerja background). */
object ForegroundBus {
    @Volatile var foreground: Boolean = false
}
