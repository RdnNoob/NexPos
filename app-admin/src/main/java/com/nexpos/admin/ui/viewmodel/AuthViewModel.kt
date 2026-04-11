package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.LoginRequest
import com.nexpos.core.data.model.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState(error = "Email dan password tidak boleh kosong")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            try {
                val response = api.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val body = response.body()
                    val user = body?.user
                    if (body != null && user != null) {
                        session.saveAdminSession(body.token, user)
                        _state.value = AuthState(isSuccess = true)
                    } else {
                        _state.value = AuthState(error = "Respons server tidak lengkap, coba lagi")
                    }
                } else {
                    val serverMsg = tryParseErrorBody(response.errorBody()?.string())
                    val msg = serverMsg ?: when (response.code()) {
                        400 -> "Email dan password wajib diisi"
                        401 -> "Email atau password salah"
                        500 -> "Terjadi kesalahan server, coba lagi nanti"
                        else -> "Login gagal (${response.code()})"
                    }
                    _state.value = AuthState(error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = AuthState(error = "Tidak bisa menjangkau server. Pastikan koneksi internet aktif.")
            } catch (e: Exception) {
                val detail = e.message?.take(150) ?: e.javaClass.simpleName
                _state.value = AuthState(error = "Login gagal: $detail")
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _state.value = AuthState(error = "Semua field wajib diisi")
            return
        }
        if (password.length < 6) {
            _state.value = AuthState(error = "Password minimal 6 karakter")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            try {
                val response = api.register(RegisterRequest(email.trim(), password, name.trim()))
                if (response.isSuccessful) {
                    val body = response.body()
                    val user = body?.user
                    if (body != null && user != null) {
                        session.saveAdminSession(body.token, user)
                        _state.value = AuthState(isSuccess = true)
                    } else {
                        _state.value = AuthState(error = "Respons server tidak lengkap, coba lagi")
                    }
                } else {
                    val serverMsg = tryParseErrorBody(response.errorBody()?.string())
                    val msg = serverMsg ?: when (response.code()) {
                        400 -> "Data tidak lengkap"
                        409 -> "Email sudah terdaftar"
                        500 -> "Terjadi kesalahan server, coba lagi nanti"
                        else -> "Pendaftaran gagal (${response.code()})"
                    }
                    _state.value = AuthState(error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = AuthState(error = "Tidak bisa menjangkau server. Pastikan koneksi internet aktif.")
            } catch (e: Exception) {
                val detail = e.message?.take(150) ?: e.javaClass.simpleName
                _state.value = AuthState(error = "Pendaftaran gagal: $detail")
            }
        }
    }

    private fun tryParseErrorBody(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val regex = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
            regex.find(errorBody)?.groupValues?.get(1)
        } catch (_: Exception) { null }
    }

    fun logout() {
        viewModelScope.launch {
            try { session.clearSession() } catch (_: Exception) { }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
