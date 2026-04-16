package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.model.BanUserRequest
import com.nexpos.core.data.model.OtpRequestInfo
import com.nexpos.core.data.model.SuperAdminLoginRequest
import com.nexpos.core.data.model.SuperAdminStatsResponse
import com.nexpos.core.data.model.SuperAdminUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuperAdminUiState(
    val isLoading: Boolean = false,
    val token: String = "",
    val stats: SuperAdminStatsResponse? = null,
    val users: List<SuperAdminUser> = emptyList(),
    val otpRequests: List<OtpRequestInfo> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class SuperAdminViewModel @Inject constructor(
    private val api: NexPosApi
) : ViewModel() {
    private val _state = MutableStateFlow(SuperAdminUiState())
    val state: StateFlow<SuperAdminUiState> = _state

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val res = api.superAdminLogin(SuperAdminLoginRequest(username.trim(), password))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, token = res.body()?.token.orEmpty())
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = parseError(res.errorBody()?.string()))
                }
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Tidak dapat terhubung ke server")
            }
        }
    }

    fun load(token: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, token = token)
            val bearer = "Bearer $token"
            try {
                val stats = api.getSuperAdminStats(bearer)
                val users = api.getSuperAdminUsers(bearer)
                val otp = api.getSuperAdminOtpRequests(bearer)
                _state.value = _state.value.copy(
                    isLoading = false,
                    stats = stats.body(),
                    users = users.body()?.users ?: emptyList(),
                    otpRequests = otp.body()?.requests ?: emptyList(),
                    error = if (!stats.isSuccessful || !users.isSuccessful || !otp.isSuccessful) "Sebagian data gagal dimuat" else null
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal memuat dashboard super admin")
            }
        }
    }

    fun ban(userId: String, reason: String = "Banned permanen oleh super admin") = action {
        api.banSuperAdminUser("Bearer ${_state.value.token}", userId, BanUserRequest(reason, true))
        load(_state.value.token)
    }

    fun unban(userId: String) = action {
        api.unbanSuperAdminUser("Bearer ${_state.value.token}", userId)
        load(_state.value.token)
    }

    fun deleteUser(userId: String) = action {
        api.deleteSuperAdminUser("Bearer ${_state.value.token}", userId)
        load(_state.value.token)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (_: Exception) {
                _state.value = _state.value.copy(error = "Aksi gagal dijalankan")
            }
        }
    }

    private fun parseError(raw: String?): String {
        if (raw == null) return "Terjadi kesalahan"
        val marker = "\"message\":\""
        val start = raw.indexOf(marker)
        if (start == -1) return "Terjadi kesalahan"
        val valueStart = start + marker.length
        val valueEnd = raw.indexOf("\"", valueStart)
        return if (valueEnd == -1) "Terjadi kesalahan" else raw.substring(valueStart, valueEnd)
    }
}