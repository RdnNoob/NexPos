package com.nexpos.laundry.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LaundrySplashViewModel @Inject constructor(
    private val session: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn

    private val _lastCrash = MutableStateFlow<String?>(null)
    val lastCrash: StateFlow<String?> = _lastCrash

    init {
        val prefs = context.getSharedPreferences("crash_log", Context.MODE_PRIVATE)
        val crash = prefs.getString("last_crash", null)
        if (!crash.isNullOrBlank()) {
            _lastCrash.value = crash
            prefs.edit().remove("last_crash").apply()
        }

        viewModelScope.launch {
            try {
                _isLoggedIn.value = session.isLoggedIn()
            } catch (e: Exception) {
                _isLoggedIn.value = false
            }
        }
    }

    fun clearCrash() {
        _lastCrash.value = null
    }
}
