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
    val totalIncome: Double = 0.0,
    val totalTransactions: Int = 0
)

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
                    _state.value = TransactionUiState(
                        transactions = txList,
                        totalIncome = total,
                        totalTransactions = txList.size
                    )
                } else {
                    _state.value = TransactionUiState(error = "Gagal memuat transaksi (${res.code()})")
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
}
