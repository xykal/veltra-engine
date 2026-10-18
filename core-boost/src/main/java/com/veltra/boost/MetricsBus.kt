package com.veltra.boost

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Bus metrik sederhana — overlay & UI berlangganan di sini. */
object MetricsBus {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _fps = MutableStateFlow(60)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _ping = MutableStateFlow(24)
    val ping: StateFlow<Int> = _ping.asStateFlow()

    private val _temp = MutableStateFlow(38)
    val temp: StateFlow<Int> = _temp.asStateFlow()

    private val _status = MutableStateFlow("STABLE")
    val status: StateFlow<String> = _status.asStateFlow()

    /** true = aplikasi live masih hidup. null = mode live tidak aktif. */
    private val _live = MutableStateFlow<Boolean?>(null)
    val live: StateFlow<Boolean?> = _live.asStateFlow()

    fun publishLive(alive: Boolean?) { _live.value = alive }

    fun publishFps(v: Int) { _fps.value = v }
    fun publishPing(v: Int) { _ping.value = v }
    fun publishTemp(v: Int) { _temp.value = v }

    init {
        scope.launch {
            combine(fps, ping) { f, p ->
                when {
                    p < 0 || p > 120 -> "NETWORK"
                    f < 30 -> "LAGGY"
                    else -> "STABLE"
                }
            }.collect { _status.value = it }
        }
    }
}
