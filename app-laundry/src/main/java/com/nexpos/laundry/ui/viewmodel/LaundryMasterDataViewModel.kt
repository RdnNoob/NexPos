package com.nexpos.laundry.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpos.core.data.api.NexPosApi
import com.nexpos.core.data.local.SessionManager
import com.nexpos.core.data.model.*
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

    private suspend fun token(): String? = session.getToken()?.let { "Bearer $it" }

    fun loadServices() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val t = token() ?: run {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                    return@launch
                }
                val res = api.getServices(t, session.getOutletId())
                _state.value = if (res.isSuccessful) {
                    _state.value.copy(isLoading = false, services = res.body()?.services ?: emptyList())
                } else {
                    _state.value.copy(isLoading = false, error = "Gagal memuat layanan (${res.code()})")
                }
            } catch (e: CancellationException) { throw e
            } catch (e: IOException) { _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) { _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message?.take(80)}") }
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
                val t = token() ?: run {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                    return@launch
                }
                val priceValue = price.filter { it.isDigit() }.toIntOrNull() ?: 0
                val res = api.createService(t, CreateServiceRequest(session.getOutletId(), name.trim(), priceValue, unit.trim()))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Layanan berhasil ditambahkan")
                    loadServices()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal menyimpan layanan (${res.code()})")
                }
            } catch (e: CancellationException) { throw e
            } catch (e: IOException) { _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) { _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message?.take(80)}") }
        }
    }

    fun updateService(id: String, name: String, price: String, unit: String) {
        if (name.isBlank() || price.isBlank() || unit.isBlank()) {
            _state.value = _state.value.copy(error = "Nama, harga, dan satuan wajib diisi")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val t = token() ?: run {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                    return@launch
                }
                val priceValue = price.filter { it.isDigit() }.toIntOrNull() ?: 0
                val res = api.updateService(t, id, UpdateServiceRequest(name.trim(), priceValue, unit.trim()))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Layanan berhasil diperbarui")
                    loadServices()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal memperbarui layanan (${res.code()})")
                }
            } catch (e: CancellationException) { throw e
            } catch (e: IOException) { _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) { _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message?.take(80)}") }
        }
    }

    fun deleteService(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val t = token() ?: run {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                    return@launch
                }
                val res = api.deleteService(t, id)
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Layanan dihapus")
                    loadServices()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal menghapus layanan (${res.code()})")
                }
            } catch (e: CancellationException) { throw e
            } catch (e: IOException) { _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) { _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message?.take(80)}") }
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val t = token() ?: run {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                    return@launch
                }
                val res = api.getCustomers(t, session.getOutletId())
                _state.value = if (res.isSuccessful) {
                    _state.value.copy(isLoading = false, customers = res.body()?.customers ?: emptyList())
                } else {
                    _state.value.copy(isLoading = false, error = "Gagal memuat pelanggan (${res.code()})")
                }
            } catch (e: CancellationException) { throw e
            } catch (e: IOException) { _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) { _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message?.take(80)}") }
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
                val t = token() ?: run {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                    return@launch
                }
                val res = api.createCustomer(t, CreateCustomerRequest(session.getOutletId(), name.trim(), phone.trim(), address.trim()))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Pelanggan berhasil ditambahkan")
                    loadCustomers()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal menyimpan pelanggan (${res.code()})")
                }
            } catch (e: CancellationException) { throw e
            } catch (e: IOException) { _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) { _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message?.take(80)}") }
        }
    }

    fun updateCustomer(id: String, name: String, phone: String, address: String) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Nama pelanggan wajib diisi")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val t = token() ?: run {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                    return@launch
                }
                val res = api.updateCustomer(t, id, UpdateCustomerRequest(name.trim(), phone.trim(), address.trim()))
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Pelanggan berhasil diperbarui")
                    loadCustomers()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal memperbarui pelanggan (${res.code()})")
                }
            } catch (e: CancellationException) { throw e
            } catch (e: IOException) { _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) { _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message?.take(80)}") }
        }
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val t = token() ?: run {
                    _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid")
                    return@launch
                }
                val res = api.deleteCustomer(t, id)
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Pelanggan dihapus")
                    loadCustomers()
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Gagal menghapus pelanggan (${res.code()})")
                }
            } catch (e: CancellationException) { throw e
            } catch (e: IOException) { _state.value = _state.value.copy(isLoading = false, error = "Gagal terhubung ke server")
            } catch (e: Exception) { _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message?.take(80)}") }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(error = null, successMessage = null)
    }
}
