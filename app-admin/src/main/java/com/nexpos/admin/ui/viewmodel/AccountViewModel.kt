package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.ChangePasswordRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class AccountState(
    val userName: String = "",
    val userEmail: String = "",
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                userName = session.getUserName() ?: "",
                userEmail = session.getUserEmail() ?: ""
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            _state.value = _state.value.copy(error = "Semua field wajib diisi")
            return
        }
        if (newPassword.length < 6) {
            _state.value = _state.value.copy(error = "Password baru minimal 6 karakter")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, successMessage = null)
            try {
                val tokenRaw = session.getToken() ?: run {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid, silakan login ulang")
                    return@launch
                }
                val token = "Bearer $tokenRaw"
                val res = api.changePassword(token, ChangePasswordRequest(currentPassword, newPassword))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Password berhasil diperbarui!")
                } else {
                    val msg = when (res.code()) {
                        401 -> "Password lama tidak sesuai"
                        400 -> "Data tidak valid"
                        else -> "Gagal mengubah password (${res.code()})"
                    }
                    _state.value = _state.value.copy(isLoading = false, error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Tidak bisa menjangkau server")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal: ${e.message?.take(100)}")
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val token = session.getToken() ?: run {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid, silakan login ulang")
                    return@launch
                }
                val response = api.deleteAccount("Bearer $token")
                if (response.isSuccessful) {
                    session.clearSession()
                    _state.value = _state.value.copy(isLoading = false, isDeleted = true)
                } else {
                    val msg = when (response.code()) {
                        401 -> "Sesi tidak valid, silakan login ulang"
                        404 -> "Akun tidak ditemukan"
                        500 -> "Terjadi kesalahan server, coba lagi nanti"
                        else -> "Hapus akun gagal (${response.code()})"
                    }
                    _state.value = _state.value.copy(isLoading = false, error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Tidak bisa menjangkau server")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal menghapus akun: ${e.message?.take(100)}")
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(successMessage = null, error = null)
    }
}
