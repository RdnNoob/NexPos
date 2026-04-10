package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.LoginRequest
import com.nexpos.core.data.model.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            try {
                val response = api.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    session.saveAdminSession(body.token, body.user!!)
                    _state.value = AuthState(isSuccess = true)
                } else {
                    _state.value = AuthState(error = "Email atau password salah")
                }
            } catch (e: Exception) {
                _state.value = AuthState(error = "Gagal terhubung ke server: ${e.message}")
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            try {
                val response = api.register(RegisterRequest(email.trim(), password, name.trim()))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    session.saveAdminSession(body.token, body.user!!)
                    _state.value = AuthState(isSuccess = true)
                } else {
                    _state.value = AuthState(error = "Pendaftaran gagal, coba lagi")
                }
            } catch (e: Exception) {
                _state.value = AuthState(error = "Gagal terhubung ke server: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch { session.clearSession() }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
