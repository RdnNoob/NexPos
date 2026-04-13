package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.model.ForgotPasswordRequest
import com.nexpos.core.data.model.ResetPasswordRequest
import com.nexpos.core.data.model.VerifyOtpRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class ForgotPasswordState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val otpSent: Boolean = false,
    val otpVerified: Boolean = false,
    val resetDone: Boolean = false,
    val resetToken: String = ""
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val api: NexPosApi
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state

    fun requestOtp(email: String) {
        if (email.isBlank()) {
            _state.value = _state.value.copy(error = "Email wajib diisi")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _state.value = _state.value.copy(error = "Format email tidak valid")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val res = api.forgotPassword(ForgotPasswordRequest(email.trim().lowercase()))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        otpSent = true,
                        successMessage = res.body()?.message ?: "Kode OTP telah dikirim ke email kamu"
                    )
                } else {
                    val msg = when (res.code()) {
                        503 -> "Layanan email belum dikonfigurasi di server"
                        500 -> "Gagal mengirim email. Coba beberapa saat lagi."
                        else -> "Gagal memproses permintaan (${res.code()})"
                    }
                    _state.value = _state.value.copy(isLoading = false, error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server. Periksa koneksi internet.")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Terjadi kesalahan. Coba lagi.")
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        if (otp.length != 6) {
            _state.value = _state.value.copy(error = "Masukkan 6 digit kode OTP")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val res = api.verifyOtp(VerifyOtpRequest(email.trim().lowercase(), otp.trim()))
                if (res.isSuccessful) {
                    val body = res.body()
                    if (body?.resetToken.isNullOrBlank()) {
                        _state.value = _state.value.copy(isLoading = false, error = "Respons server tidak valid")
                        return@launch
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        otpVerified = true,
                        resetToken = body!!.resetToken
                    )
                } else {
                    val msg = when (res.code()) {
                        400 -> "Kode OTP salah atau sudah kadaluarsa"
                        else -> "Verifikasi gagal (${res.code()})"
                    }
                    _state.value = _state.value.copy(isLoading = false, error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server. Periksa koneksi internet.")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Terjadi kesalahan. Coba lagi.")
            }
        }
    }

    fun resetPassword(newPassword: String, confirmPassword: String) {
        if (newPassword.isBlank()) {
            _state.value = _state.value.copy(error = "Password baru wajib diisi")
            return
        }
        if (newPassword.length < 6) {
            _state.value = _state.value.copy(error = "Password minimal 6 karakter")
            return
        }
        if (newPassword != confirmPassword) {
            _state.value = _state.value.copy(error = "Konfirmasi password tidak cocok")
            return
        }
        val token = _state.value.resetToken
        if (token.isBlank()) {
            _state.value = _state.value.copy(error = "Token tidak valid. Ulangi proses dari awal.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val res = api.resetPassword(ResetPasswordRequest(token, newPassword))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        resetDone = true,
                        successMessage = "Password berhasil diubah! Silakan login dengan password baru."
                    )
                } else {
                    val msg = when (res.code()) {
                        400 -> "Token kadaluarsa. Ulangi proses dari awal."
                        else -> "Gagal mengubah password (${res.code()})"
                    }
                    _state.value = _state.value.copy(isLoading = false, error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server. Periksa koneksi internet.")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Terjadi kesalahan. Coba lagi.")
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun clearMessage() { _state.value = _state.value.copy(successMessage = null) }
}
