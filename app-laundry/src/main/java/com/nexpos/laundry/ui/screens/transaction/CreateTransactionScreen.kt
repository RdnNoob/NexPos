package com.nexpos.laundry.ui.screens.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexpos.core.ui.components.NexPosButton
import com.nexpos.laundry.ui.viewmodel.LaundryMasterDataViewModel
import com.nexpos.laundry.ui.viewmodel.LaundryTransactionViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTransactionScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    txViewModel: LaundryTransactionViewModel = hiltViewModel(),
    masterViewModel: LaundryMasterDataViewModel = hiltViewModel()
) {
    val txState by txViewModel.state.collectAsState()
    val masterState by masterViewModel.state.collectAsState()

    var selectedCustomer by remember { mutableStateOf<com.nexpos.core.data.model.CustomerInfo?>(null) }
    var selectedService by remember { mutableStateOf<com.nexpos.core.data.model.ServiceInfo?>(null) }
    var quantity by remember { mutableStateOf("1") }

    var expandedCustomer by remember { mutableStateOf(false) }
    var expandedService by remember { mutableStateOf(false) }

    val unitPrice = selectedService?.price ?: 0
    val qty = quantity.toIntOrNull() ?: 0
    val totalPrice = unitPrice * qty

    val rupiah = NumberFormat.getNumberInstance(Locale("id", "ID"))

    LaunchedEffect(Unit) {
        masterViewModel.loadCustomers()
        masterViewModel.loadServices()
    }

    LaunchedEffect(txState.successMessage) {
        if (txState.successMessage != null) {
            txViewModel.clearMessage()
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaksi Baru") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Pilih Pelanggan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ExposedDropdownMenuBox(
                expanded = expandedCustomer,
                onExpandedChange = { expandedCustomer = it }
            ) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pelanggan") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomer) },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    shape = MaterialTheme.shapes.medium,
                    placeholder = { Text("Pilih pelanggan...") }
                )
                ExposedDropdownMenu(
                    expanded = expandedCustomer,
                    onDismissRequest = { expandedCustomer = false }
                ) {
                    if (masterState.customers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Belum ada pelanggan", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { expandedCustomer = false }
                        )
                    }
                    masterState.customers.forEach { customer ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(customer.name, fontWeight = FontWeight.Medium)
                                    if (!customer.phone.isNullOrBlank()) {
                                        Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            onClick = {
                                selectedCustomer = customer
                                expandedCustomer = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()
            Text("Pilih Layanan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ExposedDropdownMenuBox(
                expanded = expandedService,
                onExpandedChange = { expandedService = it }
            ) {
                OutlinedTextField(
                    value = selectedService?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jenis Layanan") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedService) },
                    leadingIcon = { Icon(Icons.Default.LocalLaundryService, null) },
                    shape = MaterialTheme.shapes.medium,
                    placeholder = { Text("Pilih layanan...") }
                )
                ExposedDropdownMenu(
                    expanded = expandedService,
                    onDismissRequest = { expandedService = false }
                ) {
                    if (masterState.services.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Belum ada layanan", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { expandedService = false }
                        )
                    }
                    masterState.services.forEach { service ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(service.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            "per ${service.unit ?: "kg"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "Rp ${rupiah.format(service.price)}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                selectedService = service
                                expandedService = false
                            }
                        )
                    }
                }
            }

            if (selectedService != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Harga satuan:", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "Rp ${rupiah.format(unitPrice)} / ${selectedService?.unit ?: "kg"}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            HorizontalDivider()
            Text("Jumlah", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter { c -> c.isDigit() }.ifEmpty { "1" } },
                label = { Text("Kuantitas (${selectedService?.unit ?: "kg"})") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Scale, null) },
                shape = MaterialTheme.shapes.medium,
                suffix = { Text(selectedService?.unit ?: "kg") }
            )

            if (selectedService != null && qty > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "TOTAL:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Rp ${rupiah.format(totalPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Text(
                        "${rupiah.format(unitPrice)} × $qty ${selectedService?.unit ?: "kg"} = Rp ${rupiah.format(totalPrice)}",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            txState.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "Status awal transaksi akan otomatis: Diterima",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            NexPosButton(
                text = "Simpan Transaksi",
                onClick = {
                    val customer = selectedCustomer?.name ?: ""
                    val service = selectedService?.name ?: ""
                    txViewModel.createTransaction(customer, service, totalPrice.toString())
                },
                isLoading = txState.isLoading,
                enabled = selectedCustomer != null && selectedService != null && qty > 0
            )
        }
    }
}
