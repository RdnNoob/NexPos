package com.nexpos.laundry.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.DeviceLoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DeviceAuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val outletName: String = "",
    val error: String? = null
)

@HiltViewModel
class DeviceAuthViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceAuthState())
    val state: StateFlow<DeviceAuthState> = _state

    fun loginWithCode(activationCode: String) {
        viewModelScope.launch {
            _state.value = DeviceAuthState(isLoading = true)
            try {
                val deviceId = getOrCreateDeviceId()
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                val response = api.loginDevice(
                    DeviceLoginRequest(
                        activationCode = activationCode.trim().uppercase(),
                        deviceName = deviceName,
                        deviceId = deviceId
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        session.saveDeviceSession(body.token, body.outlet, body.device, deviceId)
                        _state.value = DeviceAuthState(isSuccess = true, outletName = body.outlet?.name ?: "")
                    } else {
                        _state.value = DeviceAuthState(error = "Respons server tidak valid")
                    }
                } else {
                    val msg = when (response.code()) {
                        401 -> "Kode aktivasi tidak valid"
                        403 -> "Batas maksimal device tercapai (5 device)"
                        else -> "Login gagal, coba lagi"
                    }
                    _state.value = DeviceAuthState(error = msg)
                }
            } catch (e: Exception) {
                _state.value = DeviceAuthState(error = "Gagal terhubung ke server: ${e.message}")
            }
        }
    }

    private fun getOrCreateDeviceId(): String {
        return UUID.randomUUID().toString()
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
