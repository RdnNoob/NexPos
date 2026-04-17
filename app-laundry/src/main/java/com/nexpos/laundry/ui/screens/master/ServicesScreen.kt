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
import com.nexpos.core.data.model.ServiceInfo
import com.nexpos.laundry.ui.viewmodel.LaundryMasterDataViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    onNavigateBack: () -> Unit,
    viewModel: LaundryMasterDataViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }

    var editingService by remember { mutableStateOf<ServiceInfo?>(null) }
    var deleteConfirmService by remember { mutableStateOf<ServiceInfo?>(null) }

    val rupiah = NumberFormat.getNumberInstance(Locale("id", "ID"))

    LaunchedEffect(Unit) { viewModel.loadServices() }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) viewModel.clearMessage()
    }

    if (editingService != null) {
        val svc = editingService!!
        var editName by remember(svc.id) { mutableStateOf(svc.name) }
        var editPrice by remember(svc.id) { mutableStateOf(svc.price.toString()) }
        var editUnit by remember(svc.id) { mutableStateOf(svc.unit ?: "kg") }

        AlertDialog(
            onDismissRequest = { editingService = null },
            title = { Text("Edit Layanan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama layanan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editUnit,
                        onValueChange = { editUnit = it },
                        label = { Text("Satuan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateService(svc.id, editName, editPrice, editUnit)
                        editingService = null
                    },
                    enabled = editName.isNotBlank() && editPrice.isNotBlank() && editUnit.isNotBlank()
                ) { Text("Simpan") }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingService = null }) { Text("Batal") }
            }
        )
    }

    if (deleteConfirmService != null) {
        val svc = deleteConfirmService!!
        AlertDialog(
            onDismissRequest = { deleteConfirmService = null },
            title = { Text("Hapus Layanan") },
            text = { Text("Yakin ingin menghapus layanan \"${svc.name}\"? Tindakan ini tidak bisa dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteService(svc.id)
                        deleteConfirmService = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteConfirmService = null }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Layanan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Kembali") }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadServices() }) {
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
                    Text("Tambah Layanan Baru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama layanan") },
                        leadingIcon = { Icon(Icons.Default.LocalLaundryService, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga") },
                        prefix = { Text("Rp ") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan (contoh: kg, pcs, pasang)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            viewModel.createService(name, price, unit)
                            name = ""; price = ""; unit = "kg"
                        },
                        enabled = !state.isLoading && name.isNotBlank() && price.isNotBlank() && unit.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isLoading) "Menyimpan..." else "Tambah Layanan")
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

            Text("Daftar Layanan (${state.services.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (state.services.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Belum ada layanan. Tambahkan layanan di atas.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                state.services.forEach { service ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalLaundryService, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(service.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Rp ${rupiah.format(service.price)} / ${service.unit ?: "kg"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = { editingService = service }) {
                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { deleteConfirmService = service }) {
                                Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
