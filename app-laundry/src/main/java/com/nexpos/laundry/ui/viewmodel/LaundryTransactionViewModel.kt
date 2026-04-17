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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class LaundryTxState(
    val isLoading: Boolean = false,
    val transactions: List<TransactionInfo> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = ""
) {
    val filteredTransactions: List<TransactionInfo>
        get() = if (searchQuery.isBlank()) transactions
        else {
            val q = searchQuery.lowercase()
            transactions.filter {
                it.customer.lowercase().contains(q) ||
                (it.service?.lowercase()?.contains(q) == true)
            }
        }
}

@HiltViewModel
class LaundryTransactionViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(LaundryTxState())
    val state: StateFlow<LaundryTxState> = _state

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                loadTransactions(silent = _state.value.transactions.isNotEmpty())
                delay(30_000L)
            }
        }
    }

    fun loadTransactions(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _state.value = _state.value.copy(isLoading = true, error = null)
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
                    _state.value = _state.value.copy(
                        isLoading = false,
                        transactions = res.body()?.transactions ?: emptyList(),
                        error = null
                    )
                } else {
                    if (!silent) _state.value = LaundryTxState(error = "Gagal memuat transaksi (${res.code()})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                if (!silent) _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server. Periksa koneksi internet.")
            } catch (e: Exception) {
                val detail = e.message?.take(120) ?: e.javaClass.simpleName
                if (!silent) _state.value = _state.value.copy(isLoading = false, error = "Gagal memuat: $detail")
            }
        }
    }

    fun createTransaction(customerId: String, serviceId: String, quantity: Int) {
        if (customerId.isBlank() || serviceId.isBlank() || quantity <= 0) {
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
                val outletId = session.getOutletId()
                if (outletId == null) {
                    _state.value = _state.value.copy(isLoading = false, error = "Outlet tidak ditemukan, silakan login ulang")
                    return@launch
                }
                val res = api.createTransaction(
                    token,
                    CreateTransactionRequest(outletId, customerId, serviceId, quantity)
                )
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Transaksi berhasil dibuat!")
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
                    loadTransactions(silent = true)
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

    fun cancelTransaction(transactionId: Int) {
        updateStatus(transactionId, "dibatalkan")
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun clearMessage() { _state.value = _state.value.copy(successMessage = null, error = null) }
}
