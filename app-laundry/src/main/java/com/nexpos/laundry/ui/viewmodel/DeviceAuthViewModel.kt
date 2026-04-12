package com.nexpos.laundry.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.DeviceLoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
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
        if (activationCode.isBlank()) {
            _state.value = DeviceAuthState(error = "Kode aktivasi tidak boleh kosong")
            return
        }
        viewModelScope.launch {
            _state.value = DeviceAuthState(isLoading = true)
            try {
                val deviceId = session.getOrCreateDeviceId()
                val deviceName = try {
                    "${Build.MANUFACTURER} ${Build.MODEL}"
                } catch (_: Exception) {
                    "Unknown Device"
                }
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
                        400 -> "Data tidak lengkap"
                        401 -> "Kode aktivasi tidak valid"
                        403 -> "Batas maksimal device tercapai (5 device)"
                        500 -> "Terjadi kesalahan server, coba lagi nanti"
                        else -> "Login gagal (${response.code()})"
                    }
                    _state.value = DeviceAuthState(error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = DeviceAuthState(error = "Tidak bisa menjangkau server NexPos. Pastikan koneksi internet aktif lalu coba lagi.")
            } catch (e: Exception) {
                val detail = e.message?.take(120) ?: e.javaClass.simpleName
                _state.value = DeviceAuthState(error = "Login gagal: $detail")
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
