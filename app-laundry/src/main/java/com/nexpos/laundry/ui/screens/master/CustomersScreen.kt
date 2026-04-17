package com.nexpos.laundry.ui.screens.master

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexpos.core.data.model.CustomerInfo
import com.nexpos.laundry.ui.viewmodel.LaundryMasterDataViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    onNavigateBack: () -> Unit,
    viewModel: LaundryMasterDataViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var editingCustomer by remember { mutableStateOf<CustomerInfo?>(null) }
    var deleteConfirmCustomer by remember { mutableStateOf<CustomerInfo?>(null) }

    LaunchedEffect(Unit) { viewModel.loadCustomers() }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) viewModel.clearMessage()
    }

    if (editingCustomer != null) {
        val cust = editingCustomer!!
        var editName by remember(cust.id) { mutableStateOf(cust.name) }
        var editPhone by remember(cust.id) { mutableStateOf(cust.phone ?: "") }
        var editAddress by remember(cust.id) { mutableStateOf(cust.address ?: "") }

        AlertDialog(
            onDismissRequest = { editingCustomer = null },
            title = { Text("Edit Pelanggan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama pelanggan") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it.filter { c -> c.isDigit() || c == '+' } },
                        label = { Text("Nomor HP") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Alamat") },
                        leadingIcon = { Icon(Icons.Default.Home, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCustomer(cust.id, editName, editPhone, editAddress)
                        editingCustomer = null
                    },
                    enabled = editName.isNotBlank()
                ) { Text("Simpan") }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingCustomer = null }) { Text("Batal") }
            }
        )
    }

    if (deleteConfirmCustomer != null) {
        val cust = deleteConfirmCustomer!!
        AlertDialog(
            onDismissRequest = { deleteConfirmCustomer = null },
            title = { Text("Hapus Pelanggan") },
            text = { Text("Yakin ingin menghapus pelanggan \"${cust.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(cust.id)
                        deleteConfirmCustomer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteConfirmCustomer = null }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Pelanggan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Kembali") }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCustomers() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onPrimary)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tambah Pelanggan Baru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama pelanggan") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                        label = { Text("Nomor HP") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat") },
                        leadingIcon = { Icon(Icons.Default.Home, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            viewModel.createCustomer(name, phone, address)
                            name = ""; phone = ""; address = ""
                        },
                        enabled = !state.isLoading && name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isLoading) "Menyimpan..." else "Tambah Pelanggan")
                    }
                }
            }

            state.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            state.successMessage?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }

            Text("Daftar Pelanggan (${state.customers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (state.customers.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Belum ada pelanggan. Tambahkan di atas.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                state.customers.forEach { customer ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customer.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    customer.phone ?: "-",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!customer.address.isNullOrBlank()) {
                                    Text(
                                        customer.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { editingCustomer = customer }) {
                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { deleteConfirmCustomer = customer }) {
                                Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
