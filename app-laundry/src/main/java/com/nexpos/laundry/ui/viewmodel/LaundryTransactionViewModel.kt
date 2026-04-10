package com.nexpos.laundry.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.CreateTransactionRequest
import com.nexpos.core.data.model.TransactionInfo
import com.nexpos.core.data.model.UpdateStatusRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LaundryTxState(
    val isLoading: Boolean = false,
    val transactions: List<TransactionInfo> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class LaundryTransactionViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(LaundryTxState())
    val state: StateFlow<LaundryTxState> = _state

    fun loadTransactions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${session.getToken() ?: ""}"
                val outletId = session.getOutletId()
                val res = api.getTransactions(token, outletId)
                if (res.isSuccessful) {
                    _state.value = LaundryTxState(transactions = res.body()?.transactions ?: emptyList())
                } else {
                    _state.value = LaundryTxState(error = "Gagal memuat transaksi")
                }
            } catch (e: Exception) {
                _state.value = LaundryTxState(error = "Error: ${e.message}")
            }
        }
    }

    fun createTransaction(customer: String, service: String, amount: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${session.getToken() ?: ""}"
                val amountVal = amount.replace(",", "").replace(".", "").toDoubleOrNull()
                if (amountVal == null) {
                    _state.value = _state.value.copy(isLoading = false, error = "Harga tidak valid")
                    return@launch
                }
                val res = api.createTransaction(
                    token,
                    CreateTransactionRequest(customer.trim(), service.trim(), amountVal)
                )
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "Transaksi untuk ${customer} berhasil dibuat!"
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal membuat transaksi")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message}")
            }
        }
    }

    fun updateStatus(transactionId: Int, newStatus: String) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${session.getToken() ?: ""}"
                val res = api.updateTransactionStatus(token, UpdateStatusRequest(transactionId, newStatus))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(successMessage = "Status diperbarui ke: $newStatus")
                    loadTransactions()
                } else {
                    _state.value = _state.value.copy(error = "Gagal memperbarui status")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Error: ${e.message}")
            }
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(successMessage = null, error = null) }
}
