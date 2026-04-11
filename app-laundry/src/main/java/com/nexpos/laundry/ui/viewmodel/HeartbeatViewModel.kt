package com.nexpos.laundry.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.HeartbeatRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HeartbeatViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    private var heartbeatStarted = false

    fun startHeartbeat() {
        if (heartbeatStarted) return
        heartbeatStarted = true
        viewModelScope.launch {
            while (isActive) {
                try {
                    val tokenRaw = session.getToken()
                    val deviceId = session.getDeviceId()
                    if (!tokenRaw.isNullOrEmpty() && !deviceId.isNullOrEmpty()) {
                        val token = "Bearer $tokenRaw"
                        val res = api.sendHeartbeat(token, HeartbeatRequest(deviceId))
                        _isOnline.value = res.isSuccessful
                    } else {
                        _isOnline.value = false
                    }
                } catch (e: Exception) {
                    _isOnline.value = false
                }
                delay(20_000L)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { session.clearSession() } catch (_: Exception) { }
        }
    }
}
