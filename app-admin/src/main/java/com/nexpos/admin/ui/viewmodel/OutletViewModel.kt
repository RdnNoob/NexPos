package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.CreateOutletRequest
import com.nexpos.core.data.model.OutletInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OutletUiState(
    val isLoading: Boolean = false,
    val outlets: List<OutletInfo> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
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
                val token = "Bearer ${session.getToken() ?: ""}"
                val res = api.getOutlets(token)
                if (res.isSuccessful) {
                    _state.value = OutletUiState(outlets = res.body()?.outlets ?: emptyList())
                } else {
                    _state.value = OutletUiState(error = "Gagal memuat outlet")
                }
            } catch (e: Exception) {
                _state.value = OutletUiState(error = "Error: ${e.message}")
            }
        }
    }

    fun createOutlet(name: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${session.getToken() ?: ""}"
                val res = api.createOutlet(token, CreateOutletRequest(name.trim()))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "Outlet '${name}' berhasil dibuat!"
                    )
                    loadOutlets()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal membuat outlet")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message}")
            }
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(successMessage = null, error = null) }
}
