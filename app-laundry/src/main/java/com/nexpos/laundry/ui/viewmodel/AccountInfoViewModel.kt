package com.nexpos.laundry.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountInfoState(
    val outletName: String = "",
    val deviceName: String = "",
    val activationCode: String = ""
)

@HiltViewModel
class AccountInfoViewModel @Inject constructor(
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(AccountInfoState())
    val state: StateFlow<AccountInfoState> = _state

    init {
        viewModelScope.launch {
            _state.value = AccountInfoState(
                outletName = session.getOutletName() ?: "",
                deviceName = session.getDeviceName() ?: "",
                activationCode = session.getOutletCode() ?: ""
            )
        }
    }
}
