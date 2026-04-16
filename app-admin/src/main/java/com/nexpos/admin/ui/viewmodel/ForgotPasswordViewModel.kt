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
import org.json.JSONObject
import javax.inject.Inject

data class ForgotPasswordState(
    val step: ForgotPasswordStep = ForgotPasswordStep.EMAIL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val resendCountdown: Int = 0,
    val email: String = "",
    val resetToken: String = ""
)

enum class ForgotPasswordStep {
    EMAIL, OTP, NEW_PASSWORD, DONE, CONTACT_CS
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
                when {
                    response.isSuccessful -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                step = ForgotPasswordStep.OTP,
                                email = email.trim().lowercase(),
                                error = null
                            )
                        }
                        startResendCountdown(60)
                    }
                    response.code() == 429 -> {
                        val errBody = parseErrorBody(response.errorBody()?.string())
                        if (errBody.contactCs) {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    step = ForgotPasswordStep.CONTACT_CS,
                                    email = email.trim().lowercase(),
                                    error = null
                                )
                            }
                        } else {
                            val cooldown = errBody.cooldown
                            if (cooldown > 0) startResendCountdown(cooldown)
                            _state.update { it.copy(isLoading = false, error = errBody.message) }
                        }
                    }
                    else -> {
                        val msg = parseError(response.errorBody()?.string())
                        _state.update { it.copy(isLoading = false, error = msg) }
                    }
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
                when {
                    response.isSuccessful -> {
                        _state.update { it.copy(isLoading = false, successMessage = "Kode OTP baru telah dikirim ke email", error = null) }
                        startResendCountdown(60)
                    }
                    response.code() == 429 -> {
                        val errBody = parseErrorBody(response.errorBody()?.string())
                        if (errBody.contactCs) {
                            _state.update {
                                it.copy(isLoading = false, step = ForgotPasswordStep.CONTACT_CS, error = null)
                            }
                        } else {
                            val cooldown = errBody.cooldown
                            if (cooldown > 0) startResendCountdown(cooldown)
                            _state.update { it.copy(isLoading = false, error = errBody.message) }
                        }
                    }
                    else -> {
                        val msg = parseError(response.errorBody()?.string())
                        _state.update { it.copy(isLoading = false, error = msg) }
                    }
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

    private fun startResendCountdown(seconds: Int) {
        countdownJob?.cancel()
        _state.update { it.copy(resendCountdown = seconds) }
        countdownJob = viewModelScope.launch {
            repeat(seconds) {
                delay(1_000)
                _state.update { it.copy(resendCountdown = it.resendCountdown - 1) }
            }
        }
    }

    private data class ErrorBody(val message: String, val contactCs: Boolean, val cooldown: Int)

    private fun parseErrorBody(raw: String?): ErrorBody {
        if (raw == null) return ErrorBody("Terjadi kesalahan server", false, 0)
        return try {
            val json = JSONObject(raw)
            ErrorBody(
                message = json.optString("message", "Terjadi kesalahan"),
                contactCs = json.optBoolean("contact_cs", false),
                cooldown = json.optInt("cooldown", 0)
            )
        } catch (_: Exception) {
            ErrorBody("Terjadi kesalahan", false, 0)
        }
    }

    private fun parseError(raw: String?): String = parseErrorBody(raw).message

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
