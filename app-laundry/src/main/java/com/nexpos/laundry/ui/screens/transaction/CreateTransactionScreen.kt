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
import com.nexpos.core.ui.components.NexPosTextField
import com.nexpos.laundry.ui.viewmodel.LaundryTransactionViewModel

val LAYANAN_LIST = listOf(
    "Cuci Reguler (3 Hari)",
    "Cuci Express (1 Hari)",
    "Cuci Setrika Reguler",
    "Cuci Setrika Express",
    "Setrika Saja",
    "Dry Cleaning",
    "Laundry Sepatu",
    "Laundry Selimut/Bed Cover"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTransactionScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: LaundryTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var customer by remember { mutableStateOf("") }
    var service by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var expandedService by remember { mutableStateOf(false) }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            viewModel.clearMessage()
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
            Text("Data Pelanggan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            NexPosTextField(
                value = customer,
                onValueChange = { customer = it },
                label = "Nama Pelanggan",
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            ExposedDropdownMenuBox(
                expanded = expandedService,
                onExpandedChange = { expandedService = it }
            ) {
                OutlinedTextField(
                    value = service,
                    onValueChange = { service = it },
                    label = { Text("Jenis Layanan") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedService) },
                    leadingIcon = { Icon(Icons.Default.LocalLaundryService, null) },
                    shape = MaterialTheme.shapes.medium
                )
                ExposedDropdownMenu(
                    expanded = expandedService,
                    onDismissRequest = { expandedService = false }
                ) {
                    LAYANAN_LIST.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                service = item
                                expandedService = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() } },
                label = { Text("Harga (Rp)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                shape = MaterialTheme.shapes.medium,
                prefix = { Text("Rp ") }
            )

            state.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    "Status awal transaksi akan otomatis: Diterima",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            NexPosButton(
                text = "Simpan Transaksi",
                onClick = { viewModel.createTransaction(customer, service, amount) },
                isLoading = state.isLoading,
                enabled = customer.isNotBlank() && service.isNotBlank() && amount.isNotBlank()
            )
        }
    }
}
