package com.veltra.launcher.tools

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdk: Int,
    val soc: String,
    val board: String,
    val width: Int,
    val height: Int,
    val refreshHz: Float,
    val totalRam: Long,
    val availRam: Long,
    val freeStorage: Long,
    val batteryPct: Int,
    val batteryC: Float
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    app: Application
) : AndroidViewModel(app) {

    private val _info = MutableStateFlow<DeviceInfo?>(null)
    val info: StateFlow<DeviceInfo?> = _info

    private val _latency = MutableStateFlow<Int?>(null)
    val latency: StateFlow<Int?> = _latency

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _info.value = withContext(Dispatchers.IO) { readInfo(getApplication()) }
    }

    /** Ukur latensi TCP 3x ke 1.1.1.1:443, ambil rata-rata yang berhasil. */
    fun testLatency() = viewModelScope.launch {
        _testing.value = true
        val results = withContext(Dispatchers.IO) {
            val out = mutableListOf<Int>()
            repeat(3) {
                val t = measureTcp()
                if (t > 0) out += t
            }
            out
        }
        _latency.value = if (results.isEmpty()) null else results.average().toInt()
        _testing.value = false
    }

    private fun measureTcp(): Int = runCatching {
        val start = System.currentTimeMillis()
        Socket().use { s ->
            s.connect(InetSocketAddress("1.1.1.1", 443), 2_500)
        }
        (System.currentTimeMillis() - start).toInt()
    }.getOrDefault(-1)

    private fun readInfo(ctx: Context): DeviceInfo {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        runCatching { am.getMemoryInfo(mi) }

        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        runCatching { wm.defaultDisplay.getRealMetrics(dm) }
        @Suppress("DEPRECATION")
        val hz = runCatching { wm.defaultDisplay.refreshRate }.getOrDefault(60f)

        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val tempC = runCatching {
            val i = ctx.registerReceiver(
                null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            )
            val t = i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            if (t > 0) t / 10f else -1f
        }.getOrDefault(-1f)

        val free = runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        }.getOrDefault(0L)

        val soc = when {
            Build.VERSION.SDK_INT >= 31 && Build.SOC_MODEL.isNotBlank() -> "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}".trim()
            Build.HARDWARE.isNotBlank() -> Build.HARDWARE
            else -> "tidak diketahui"
        }

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT,
            soc = soc,
            board = Build.BOARD,
            width = dm.widthPixels,
            height = dm.heightPixels,
            refreshHz = hz,
            totalRam = mi.totalMem,
            availRam = mi.availMem,
            freeStorage = free,
            batteryPct = pct,
            batteryC = tempC
        )
    }
}
