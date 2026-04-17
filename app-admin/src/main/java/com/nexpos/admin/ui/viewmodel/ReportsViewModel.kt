package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.DailySummary
import com.nexpos.core.data.model.ReportSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class ReportsState(
    val isLoading: Boolean = false,
    val summary: ReportSummary? = null,
    val dailySummaries: List<DailySummary> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state

    init { loadReports() }

    fun loadReports(outletId: Int? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val tokenRaw = session.getToken()
                if (tokenRaw.isNullOrEmpty()) {
                    _state.value = ReportsState(error = "Sesi tidak valid")
                    return@launch
                }
                val token = "Bearer $tokenRaw"

                val summaryRes = api.getReportSummary(token, outletId)
                val dailyRes = api.getReportDaily(token, outletId)

                if (summaryRes.isSuccessful && dailyRes.isSuccessful) {
                    _state.value = ReportsState(
                        summary = summaryRes.body(),
                        dailySummaries = dailyRes.body()?.days ?: emptyList()
                    )
                } else {
                    _state.value = ReportsState(error = "Gagal memuat laporan (${summaryRes.code()})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = ReportsState(error = "Gagal terhubung ke server. Periksa koneksi internet.")
            } catch (e: Exception) {
                _state.value = ReportsState(error = "Gagal memuat laporan: ${e.message?.take(100)}")
            }
        }
    }
}
