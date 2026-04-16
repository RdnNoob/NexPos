package com.nexpos.laundry.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.CreateCustomerRequest
import com.nexpos.core.data.model.CreateServiceRequest
import com.nexpos.core.data.model.CustomerInfo
import com.nexpos.core.data.model.ServiceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class LaundryMasterDataState(
    val isLoading: Boolean = false,
    val services: List<ServiceInfo> = emptyList(),
    val customers: List<CustomerInfo> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class LaundryMasterDataViewModel @Inject constructor(
    private val api: NexPosApi,
    private val session: SessionManager
) : ViewModel() {
    private val _state = MutableStateFlow(LaundryMasterDataState())
    val state: StateFlow<LaundryMasterDataState> = _state

    fun loadServices() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val tokenRaw = session.getToken()
                if (tokenRaw.isNullOrEmpty()) {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                    return@launch
                }
                val res = api.getServices("Bearer $tokenRaw", session.getOutletId())
                _state.value = if (res.isSuccessful) {
                    _state.value.copy(isLoading = false, services = res.body()?.services ?: emptyList())
                } else {
                    _state.value.copy(isLoading = false, error = "Gagal memuat layanan (${res.code()})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal memuat layanan: ${e.message?.take(120) ?: e.javaClass.simpleName}")
            }
        }
    }

    fun createService(name: String, price: String, unit: String) {
        if (name.isBlank() || price.isBlank() || unit.isBlank()) {
            _state.value = _state.value.copy(error = "Nama, harga, dan satuan wajib diisi")
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
                val priceValue = price.filter { it.isDigit() }.toIntOrNull() ?: 0
                val res = api.createService(
                    "Bearer $tokenRaw",
                    CreateServiceRequest(session.getOutletId(), name.trim(), priceValue, unit.trim())
                )
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Layanan berhasil ditambahkan")
                    loadServices()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal menyimpan layanan (${res.code()})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal menyimpan layanan: ${e.message?.take(120) ?: e.javaClass.simpleName}")
            }
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val tokenRaw = session.getToken()
                if (tokenRaw.isNullOrEmpty()) {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                    return@launch
                }
                val res = api.getCustomers("Bearer $tokenRaw", session.getOutletId())
                _state.value = if (res.isSuccessful) {
                    _state.value.copy(isLoading = false, customers = res.body()?.customers ?: emptyList())
                } else {
                    _state.value.copy(isLoading = false, error = "Gagal memuat pelanggan (${res.code()})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal memuat pelanggan: ${e.message?.take(120) ?: e.javaClass.simpleName}")
            }
        }
    }

    fun createCustomer(name: String, phone: String, address: String) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Nama pelanggan wajib diisi")
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
                val res = api.createCustomer(
                    "Bearer $tokenRaw",
                    CreateCustomerRequest(session.getOutletId(), name.trim(), phone.trim(), address.trim())
                )
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Pelanggan berhasil ditambahkan")
                    loadCustomers()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal menyimpan pelanggan (${res.code()})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Gagal menyimpan pelanggan: ${e.message?.take(120) ?: e.javaClass.simpleName}")
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(error = null, successMessage = null)
    }
}