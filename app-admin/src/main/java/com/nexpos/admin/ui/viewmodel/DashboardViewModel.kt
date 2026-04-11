package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.DeviceInfo
import com.nexpos.core.data.model.OutletInfo
import com.nexpos.core.data.model.TransactionInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val outlets: List<OutletInfo> = emptyList(),
    val devices: List<DeviceInfo> = emptyList(),
    val transactions: List<TransactionInfo> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${session.getToken() ?: ""}"
                val name = session.userNameFlow.first() ?: ""
                val outletsRes = api.getOutlets(token)
                val devicesRes = api.getDevices(token)
                val txRes = api.getTransactions(token)

                val outlets = if (outletsRes.isSuccessful) outletsRes.body()?.outlets ?: emptyList() else emptyList()
                val devices = if (devicesRes.isSuccessful) devicesRes.body()?.devices ?: emptyList() else emptyList()
                val transactions = if (txRes.isSuccessful) txRes.body()?.transactions ?: emptyList() else emptyList()

                _state.value = DashboardUiState(
                    isLoading = false,
                    userName = name,
                    outlets = outlets,
                    devices = devices,
                    transactions = transactions
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal memuat data: ${e.message}")
            }
        }
    }

    // FIX: Clear session on logout so user doesn't stay logged in
    fun logout() {
        viewModelScope.launch { session.clearSession() }
    }
}
