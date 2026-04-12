package com.nexpos.laundry.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.CreateTransactionRequest
import com.nexpos.core.data.model.TransactionInfo
import com.nexpos.core.data.model.UpdateStatusRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
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
                val tokenRaw = session.getToken()
                if (tokenRaw.isNullOrEmpty()) {
                    _state.value = LaundryTxState(error = "Sesi tidak valid, silakan login ulang")
                    return@launch
                }
                val token = "Bearer $tokenRaw"
                val outletId = session.getOutletId()
                val res = api.getTransactions(token, outletId)
                if (res.isSuccessful) {
                    _state.value = LaundryTxState(transactions = res.body()?.transactions ?: emptyList())
                } else {
                    _state.value = LaundryTxState(error = "Gagal memuat transaksi (${res.code()})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server. Periksa koneksi internet.")
            } catch (e: Exception) {
                val detail = e.message?.take(120) ?: e.javaClass.simpleName
                _state.value = _state.value.copy(isLoading = false, error = "Gagal memuat: $detail")
            }
        }
    }

    fun createTransaction(customer: String, service: String, amount: String) {
        if (customer.isBlank() || service.isBlank() || amount.isBlank()) {
            _state.value = _state.value.copy(error = "Semua field wajib diisi")
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
                val amountVal = amount.replace(",", "").replace(".", "").toDoubleOrNull()
                if (amountVal == null || amountVal <= 0) {
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
                        successMessage = "Transaksi untuk ${customer.trim()} berhasil dibuat!"
                    )
                } else {
                    val msg = when (res.code()) {
                        401 -> "Sesi tidak valid, silakan login ulang"
                        403 -> "Akses ditolak"
                        else -> "Gagal membuat transaksi (${res.code()})"
                    }
                    _state.value = _state.value.copy(isLoading = false, error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) {
                val detail = e.message?.take(120) ?: e.javaClass.simpleName
                _state.value = _state.value.copy(isLoading = false, error = "Gagal membuat transaksi: $detail")
            }
        }
    }

    fun updateStatus(transactionId: Int, newStatus: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)
            try {
                val tokenRaw = session.getToken()
                if (tokenRaw.isNullOrEmpty()) {
                    _state.value = _state.value.copy(error = "Sesi tidak valid")
                    return@launch
                }
                val token = "Bearer $tokenRaw"
                val res = api.updateTransactionStatus(token, UpdateStatusRequest(transactionId, newStatus))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(successMessage = "Status diperbarui ke: ${newStatus.replaceFirstChar { it.uppercase() }}")
                    loadTransactions()
                } else {
                    val msg = when (res.code()) {
                        404 -> "Transaksi tidak ditemukan"
                        400 -> "Status tidak valid"
                        else -> "Gagal memperbarui status (${res.code()})"
                    }
                    _state.value = _state.value.copy(error = msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(error = "Gagal terhubung ke server")
            } catch (e: Exception) {
                val detail = e.message?.take(120) ?: e.javaClass.simpleName
                _state.value = _state.value.copy(error = "Gagal update status: $detail")
            }
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(successMessage = null, error = null) }
}
