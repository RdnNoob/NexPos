package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.TransactionInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class TransactionUiState(
    val isLoading: Boolean = false,
    val transactions: List<TransactionInfo> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val totalIncome: Double = 0.0,
    val totalTransactions: Int = 0,
    val searchQuery: String = "",
    val filterStatus: String? = null
) {
    val filteredTransactions: List<TransactionInfo>
        get() {
            var list = transactions
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase()
                list = list.filter {
                    it.customer.lowercase().contains(q) ||
                    (it.service?.lowercase()?.contains(q) == true) ||
                    (it.outletName?.lowercase()?.contains(q) == true)
                }
            }
            if (filterStatus != null) {
                list = list.filter { it.status.lowercase() == filterStatus.lowercase() }
            }
            return list
        }
}

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionUiState())
    val state: StateFlow<TransactionUiState> = _state

    fun loadTransactions(outletId: Int? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val tokenRaw = session.getToken()
                if (tokenRaw.isNullOrEmpty()) {
                    _state.value = TransactionUiState(error = "Sesi tidak valid")
                    return@launch
                }
                val token = "Bearer $tokenRaw"
                val res = api.getTransactions(token, outletId)
                if (res.isSuccessful) {
                    val txList = res.body()?.transactions ?: emptyList()
                    val total = txList.sumOf { it.amount }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        transactions = txList,
                        totalIncome = total,
                        totalTransactions = txList.size,
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal memuat transaksi (${res.code()})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server. Periksa koneksi internet.")
            } catch (e: Exception) {
                val detail = e.message?.take(120) ?: e.javaClass.simpleName
                _state.value = _state.value.copy(isLoading = false, error = "Gagal memuat transaksi: $detail")
            }
        }
    }

    fun deleteTransaction(transactionId: Int) {
        viewModelScope.launch {
            try {
                val tokenRaw = session.getToken()
                if (tokenRaw.isNullOrEmpty()) {
                    _state.value = _state.value.copy(error = "Sesi tidak valid")
                    return@launch
                }
                val token = "Bearer $tokenRaw"
                val res = api.deleteTransaction(token, transactionId)
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(
                        transactions = _state.value.transactions.filter { it.id != transactionId },
                        successMessage = "Transaksi berhasil dihapus"
                    )
                } else {
                    _state.value = _state.value.copy(error = "Gagal menghapus transaksi (${res.code()})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(error = "Gagal terhubung ke server")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Gagal: ${e.message?.take(100)}")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun setFilterStatus(status: String?) {
        _state.value = _state.value.copy(filterStatus = status)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(successMessage = null, error = null)
    }

    fun exportCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("ID,Pelanggan,Layanan,Outlet,Jumlah,Status,Tanggal")
        _state.value.transactions.forEach { tx ->
            sb.appendLine("${tx.id},\"${tx.customer}\",\"${tx.service ?: "-"}\",\"${tx.outletName ?: "-"}\",${tx.amount},${tx.status},${tx.createdAt.take(10)}")
        }
        return sb.toString()
    }
}
