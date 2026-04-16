package com.nexpos.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.NotificationInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationInfo> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val tokenRaw = session.getToken()
            if (tokenRaw.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                return@launch
            }
            try {
                val res = api.getNotifications("Bearer $tokenRaw")
                _state.value = if (res.isSuccessful) {
                    _state.value.copy(isLoading = false, notifications = res.body()?.notifications ?: emptyList())
                } else {
                    _state.value.copy(isLoading = false, error = "Gagal memuat notifikasi")
                }
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Tidak dapat terhubung ke server")
            }
        }
    }
}