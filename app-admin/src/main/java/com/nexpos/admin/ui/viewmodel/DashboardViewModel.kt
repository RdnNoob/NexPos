package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.DeviceInfo
import com.nexpos.core.data.model.OutletInfo
import com.nexpos.core.data.model.TransactionInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
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
                val tokenRaw = session.getToken()
                if (tokenRaw.isNullOrEmpty()) {
                    _state.value = DashboardUiState(error = "Sesi tidak valid, silakan login ulang")
                    return@launch
                }
                val token = "Bearer $tokenRaw"
                val name = try { session.userNameFlow.first() ?: "" } catch (_: Exception) { "" }

                val outlets = try {
                    val res = api.getOutlets(token)
                    if (res.isSuccessful) res.body()?.outlets ?: emptyList() else emptyList()
                } catch (_: Exception) { emptyList() }

                val devices = try {
                    val res = api.getDevices(token)
                    if (res.isSuccessful) res.body()?.devices ?: emptyList() else emptyList()
                } catch (_: Exception) { emptyList() }

                val transactions = try {
                    val res = api.getTransactions(token)
                    if (res.isSuccessful) res.body()?.transactions ?: emptyList() else emptyList()
                } catch (_: Exception) { emptyList() }

                _state.value = DashboardUiState(
                    isLoading = false,
                    userName = name,
                    outlets = outlets,
                    devices = devices,
                    transactions = transactions
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server. Periksa koneksi internet Anda.")
            } catch (e: Exception) {
                val detail = e.message?.take(120) ?: e.javaClass.simpleName
                _state.value = _state.value.copy(isLoading = false, error = "Gagal memuat data: $detail")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { session.clearSession() } catch (_: Exception) { }
        }
    }
}
