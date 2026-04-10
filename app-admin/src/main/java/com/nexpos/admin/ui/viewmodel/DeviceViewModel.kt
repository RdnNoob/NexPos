package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.DeviceInfo
import com.nexpos.core.data.model.ForceLogoutRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceUiState(
    val isLoading: Boolean = false,
    val devices: List<DeviceInfo> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceUiState())
    val state: StateFlow<DeviceUiState> = _state

    init { loadDevices() }

    fun loadDevices() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${session.getToken() ?: ""}"
                val res = api.getDevices(token)
                if (res.isSuccessful) {
                    _state.value = DeviceUiState(devices = res.body()?.devices ?: emptyList())
                } else {
                    _state.value = DeviceUiState(error = "Gagal memuat daftar device")
                }
            } catch (e: Exception) {
                _state.value = DeviceUiState(error = "Error: ${e.message}")
            }
        }
    }

    fun forceLogout(deviceId: Int) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${session.getToken() ?: ""}"
                val res = api.forceLogout(token, ForceLogoutRequest(deviceId))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(message = "Device berhasil di-force logout")
                    loadDevices()
                } else {
                    _state.value = _state.value.copy(error = "Gagal force logout device")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Error: ${e.message}")
            }
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null, error = null) }
}
