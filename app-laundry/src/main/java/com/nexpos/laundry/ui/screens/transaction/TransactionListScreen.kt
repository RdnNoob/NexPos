package com.nexpos.laundry.ui.screens.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexpos.core.data.model.TransactionInfo
import com.nexpos.core.ui.components.LoadingScreen
import com.nexpos.core.ui.components.StatusChip
import com.nexpos.laundry.ui.viewmodel.LaundryTransactionViewModel

val STATUS_FLOW = listOf("diterima", "dicuci", "disetrika", "selesai")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onNavigateBack: () -> Unit,
    viewModel: LaundryTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTx by remember { mutableStateOf<TransactionInfo?>(null) }

    LaunchedEffect(Unit) { viewModel.loadTransactions() }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) viewModel.clearMessage()
    }

    if (selectedTx != null) {
        val tx = selectedTx!!
        val currentIdx = STATUS_FLOW.indexOf(tx.status.lowercase())
        val nextStatus = if (currentIdx < STATUS_FLOW.size - 1) STATUS_FLOW[currentIdx + 1] else null

        AlertDialog(
            onDismissRequest = { selectedTx = null },
            title = { Text("Update Status Laundry") },
            text = {
                Column {
                    Text("Pelanggan: ${tx.customer}", fontWeight = FontWeight.Bold)
                    Text("Layanan: ${tx.service}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Status saat ini:")
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusChip(status = tx.status)
                    if (nextStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Update ke: ${nextStatus.replaceFirstChar { it.uppercase() }}?")
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Laundry sudah selesai!", color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Pilih status manual:")
                    STATUS_FLOW.forEach { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = tx.status.lowercase() == status,
                                onClick = {
                                    viewModel.updateStatus(tx.id, status)
                                    selectedTx = null
                                }
                            )
                            Text(status.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {
                if (nextStatus != null) {
                    Button(onClick = {
                        viewModel.updateStatus(tx.id, nextStatus)
                        selectedTx = null
                    }) { Text("Lanjut ke ${nextStatus.replaceFirstChar { it.uppercase() }}") }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTx = null }) { Text("Tutup") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Transaksi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadTransactions() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingScreen("Memuat transaksi...")
        } else if (state.transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Belum ada transaksi")
                    Text("Tambahkan transaksi baru", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.transactions) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { if (tx.status != "selesai") selectedTx = tx }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.customer, fontWeight = FontWeight.Bold)
                                    Text(
                                        tx.service ?: "-",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusChip(status = tx.status)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Rp ${"%,.0f".format(tx.amount)}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (tx.status != "selesai") {
                                    Text(
                                        "Tap untuk update status",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
