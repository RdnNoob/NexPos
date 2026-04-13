package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.CreateOutletRequest
import com.nexpos.core.data.model.OutletInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class OutletUiState(
    val isLoading: Boolean = false,
    val outlets: List<OutletInfo> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val deletingOutletId: Int? = null
)

@HiltViewModel
class OutletViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(OutletUiState())
    val state: StateFlow<OutletUiState> = _state

    init { loadOutlets() }

    fun loadOutlets() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val tokenRaw = session.getToken()
                if (tokenRaw.isNullOrEmpty()) {
                    _state.value = OutletUiState(error = "Sesi tidak valid")
                    return@launch
                }
                val token = "Bearer $tokenRaw"
                val res = api.getOutlets(token)
                if (res.isSuccessful) {
                    _state.value = OutletUiState(outlets = res.body()?.outlets ?: emptyList())
                } else {
                    _state.value = OutletUiState(error = "Gagal memuat outlet (${res.code()})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server. Periksa koneksi internet.")
            } catch (e: Exception) {
                val detail = e.message?.take(120) ?: e.javaClass.simpleName
                _state.value = _state.value.copy(isLoading = false, error = "Gagal memuat outlet: $detail")
            }
        }
    }

    fun createOutlet(name: String) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Nama outlet tidak boleh kosong")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val tokenRaw = session.getToken()
                if (tokenRaw.isNullOrEmpty()) {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                    return@launch
                }
                val token = "Bearer $tokenRaw"
                val res = api.createOutlet(token, CreateOutletRequest(name.trim()))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "Outlet '${name.trim()}' berhasil dibuat!"
                    )
                    loadOutlets()
                } else {
                    val msg = when (res.code()) {
                        403 -> "Batas maksimal 5 outlet tercapai"
                        else -> "Gagal membuat outlet (${res.code()})"
                    }
                    _state.value = _state.value.copy(isLoading = false, error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server. Periksa koneksi internet.")
            } catch (e: Exception) {
                val detail = e.message?.take(120) ?: e.javaClass.simpleName
                _state.value = _state.value.copy(isLoading = false, error = "Gagal membuat outlet: $detail")
            }
        }
    }

    fun deleteOutlet(outletId: Int, outletName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(deletingOutletId = outletId, error = null)
            try {
                val tokenRaw = session.getToken()
                if (tokenRaw.isNullOrEmpty()) {
                    _state.value = _state.value.copy(deletingOutletId = null, error = "Sesi tidak valid")
                    return@launch
                }
                val token = "Bearer $tokenRaw"
                val res = api.deleteOutlet(token, outletId)
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(
                        deletingOutletId = null,
                        successMessage = "Outlet '$outletName' berhasil dihapus"
                    )
                    loadOutlets()
                } else {
                    val msg = when (res.code()) {
                        404 -> "Outlet tidak ditemukan"
                        else -> "Gagal menghapus outlet (${res.code()})"
                    }
                    _state.value = _state.value.copy(deletingOutletId = null, error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(deletingOutletId = null, error = "Gagal terhubung ke server. Periksa koneksi internet.")
            } catch (e: Exception) {
                val detail = e.message?.take(120) ?: e.javaClass.simpleName
                _state.value = _state.value.copy(deletingOutletId = null, error = "Gagal menghapus outlet: $detail")
            }
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(successMessage = null, error = null) }
}
