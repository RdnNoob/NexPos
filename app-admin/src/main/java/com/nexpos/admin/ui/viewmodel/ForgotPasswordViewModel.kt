package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.model.ForgotPasswordRequest
import com.nexpos.core.data.model.ResetPasswordRequest
import com.nexpos.core.data.model.VerifyOtpRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordState(
    val step: ForgotPasswordStep = ForgotPasswordStep.EMAIL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val resendCountdown: Int = 0,
    // internal
    val email: String = "",
    val resetToken: String = ""
)

enum class ForgotPasswordStep {
    EMAIL, OTP, NEW_PASSWORD, DONE
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val api: NexPosApi
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    fun sendOtp(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(error = "Email wajib diisi") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.forgotPassword(ForgotPasswordRequest(email.trim().lowercase()))
                if (response.isSuccessful) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            step = ForgotPasswordStep.OTP,
                            email = email.trim().lowercase(),
                            error = null
                        )
                    }
                    startResendCountdown()
                } else {
                    val msg = parseError(response.errorBody()?.string())
                    _state.update { it.copy(isLoading = false, error = msg) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Tidak dapat terhubung ke server") }
            }
        }
    }

    fun resendOtp() {
        val email = _state.value.email
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.forgotPassword(ForgotPasswordRequest(email))
                if (response.isSuccessful) {
                    _state.update { it.copy(isLoading = false, successMessage = "Kode OTP baru telah dikirim", error = null) }
                    startResendCountdown()
                } else {
                    val msg = parseError(response.errorBody()?.string())
                    _state.update { it.copy(isLoading = false, error = msg) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Tidak dapat terhubung ke server") }
            }
        }
    }

    fun verifyOtp(otp: String) {
        if (otp.length != 6) {
            _state.update { it.copy(error = "Masukkan 6 digit kode OTP") }
            return
        }
        val email = _state.value.email
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            try {
                val response = api.verifyOtp(VerifyOtpRequest(email, otp.trim()))
                if (response.isSuccessful) {
                    val token = response.body()?.resetToken ?: ""
                    _state.update {
                        it.copy(
                            isLoading = false,
                            step = ForgotPasswordStep.NEW_PASSWORD,
                            resetToken = token,
                            error = null
                        )
                    }
                } else {
                    val msg = parseError(response.errorBody()?.string())
                    _state.update { it.copy(isLoading = false, error = msg) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Tidak dapat terhubung ke server") }
            }
        }
    }

    fun resetPassword(newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _state.update { it.copy(error = "Password minimal 6 karakter") }
            return
        }
        if (newPassword != confirmPassword) {
            _state.update { it.copy(error = "Konfirmasi password tidak cocok") }
            return
        }
        val email = _state.value.email
        val token = _state.value.resetToken
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.resetPassword(ResetPasswordRequest(email, token, newPassword))
                if (response.isSuccessful) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            step = ForgotPasswordStep.DONE,
                            successMessage = response.body()?.message ?: "Password berhasil diperbarui",
                            error = null
                        )
                    }
                } else {
                    val msg = parseError(response.errorBody()?.string())
                    _state.update { it.copy(isLoading = false, error = msg) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Tidak dapat terhubung ke server") }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
    fun clearSuccessMessage() = _state.update { it.copy(successMessage = null) }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        _state.update { it.copy(resendCountdown = 60) }
        countdownJob = viewModelScope.launch {
            repeat(60) {
                delay(1_000)
                _state.update { it.copy(resendCountdown = it.resendCountdown - 1) }
            }
        }
    }

    private fun parseError(raw: String?): String {
        if (raw == null) return "Terjadi kesalahan server"
        return try {
            val start = raw.indexOf("\"message\":\"")
            if (start == -1) "Terjadi kesalahan"
            else {
                val valueStart = start + 11
                val valueEnd = raw.indexOf("\"", valueStart)
                if (valueEnd == -1) "Terjadi kesalahan" else raw.substring(valueStart, valueEnd)
            }
        } catch (_: Exception) {
            "Terjadi kesalahan"
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
