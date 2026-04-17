package com.nexpos.laundry.ui.screens.transaction

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexpos.core.data.model.TransactionInfo
import com.nexpos.core.ui.components.LoadingScreen
import com.nexpos.core.ui.components.StatusChip
import com.nexpos.laundry.ui.viewmodel.LaundryTransactionViewModel

val STATUS_FLOW = listOf("diterima", "dicuci", "disetrika", "selesai")
private val STATUS_FILTER_OPTIONS = listOf(
    null to "Semua",
    "diterima" to "Diterima",
    "dicuci" to "Dicuci",
    "disetrika" to "Disetrika",
    "selesai" to "Selesai"
)

private fun buildReceiptText(tx: TransactionInfo): String = buildString {
    appendLine("================================")
    appendLine("        NexPos Laundry          ")
    appendLine("================================")
    appendLine()
    appendLine("No. Transaksi : #${tx.id}")
    appendLine("Pelanggan     : ${tx.customer}")
    appendLine("Layanan       : ${tx.service ?: "-"}")
    appendLine("Harga         : Rp ${"%,.0f".format(tx.amount)}")
    appendLine("Status        : ${tx.status.replaceFirstChar { it.uppercase() }}")
    appendLine("Tanggal       : ${tx.createdAt.take(10)}")
    appendLine()
    appendLine("--------------------------------")
    appendLine("   Terima kasih sudah menjadi   ")
    appendLine("       pelanggan kami!           ")
    appendLine("================================")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onNavigateBack: () -> Unit,
    viewModel: LaundryTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedTx by remember { mutableStateOf<TransactionInfo?>(null) }
    var receiptTx by remember { mutableStateOf<TransactionInfo?>(null) }
    var txToCancel by remember { mutableStateOf<TransactionInfo?>(null) }
    var filterStatus by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val displayedTransactions = remember(state.filteredTransactions, filterStatus) {
        if (filterStatus == null) state.filteredTransactions
        else state.filteredTransactions.filter { it.status.lowercase() == filterStatus }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    selectedTx?.let { tx ->
        val currentIdx = STATUS_FLOW.indexOf(tx.status.lowercase())
        val nextStatus = if (currentIdx >= 0 && currentIdx < STATUS_FLOW.size - 1) STATUS_FLOW[currentIdx + 1] else null

        AlertDialog(
            onDismissRequest = { selectedTx = null },
            title = { Text("Update Status Laundry") },
            text = {
                Column {
                    Text("Pelanggan: ${tx.customer}", fontWeight = FontWeight.Bold)
                    Text("Layanan: ${tx.service ?: "-"}")
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

    receiptTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { receiptTx = null },
            title = { Text("Struk Transaksi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("================================", style = MaterialTheme.typography.bodySmall)
                    Text("        NexPos Laundry        ", fontWeight = FontWeight.Bold)
                    Text("================================", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("No. Transaksi : #${tx.id}")
                    Text("Pelanggan     : ${tx.customer}")
                    Text("Layanan       : ${tx.service ?: "-"}")
                    Text("Harga         : Rp ${"%,.0f".format(tx.amount)}")
                    Text("Status        : ${tx.status.replaceFirstChar { it.uppercase() }}")
                    Text("Tanggal       : ${tx.createdAt.take(10)}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("--------------------------------", style = MaterialTheme.typography.bodySmall)
                    Text("   Terima kasih sudah menjadi", style = MaterialTheme.typography.bodySmall)
                    Text("       pelanggan kami!        ", style = MaterialTheme.typography.bodySmall)
                    Text("================================", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val struk = buildReceiptText(tx)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, struk)
                    }
                    context.startActivity(Intent.createChooser(intent, "Bagikan Struk"))
                }) { Text("Bagikan") }
            },
            dismissButton = {
                TextButton(onClick = { receiptTx = null }) { Text("Tutup") }
            }
        )
    }

    txToCancel?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToCancel = null },
            icon = { Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Batalkan Transaksi") },
            text = {
                Text("Batalkan transaksi laundry pelanggan \"${tx.customer}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelTransaction(tx.id)
                        txToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Batalkan") }
            },
            dismissButton = {
                TextButton(onClick = { txToCancel = null }) { Text("Tutup") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LoadingScreen("Memuat transaksi...")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Cari pelanggan atau layanan...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (state.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, null)
                                    }
                                }
                            },
                            singleLine = true
                        )
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(STATUS_FILTER_OPTIONS) { (status, label) ->
                            FilterChip(
                                selected = filterStatus == status,
                                onClick = { filterStatus = if (filterStatus == status) null else status },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (displayedTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Receipt, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(if (state.searchQuery.isNotBlank() || filterStatus != null) "Tidak ada hasil yang cocok" else "Belum ada transaksi")
                                if (state.searchQuery.isBlank() && filterStatus == null) {
                                    Text("Tambahkan transaksi baru", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                } else {
                    items(displayedTransactions) { tx ->
                        TransactionCard(
                            tx = tx,
                            onUpdateStatus = { if (tx.status != "selesai" && tx.status != "dibatalkan") selectedTx = tx },
                            onShowReceipt = { receiptTx = tx },
                            onCancel = { if (tx.status != "selesai" && tx.status != "dibatalkan") txToCancel = tx }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionCard(
    tx: TransactionInfo,
    onUpdateStatus: () -> Unit,
    onShowReceipt: () -> Unit,
    onCancel: () -> Unit
) {
    val isDone = tx.status == "selesai" || tx.status == "dibatalkan"

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        onClick = onUpdateStatus
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Rp ${"%,.0f".format(tx.amount)}",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!isDone) {
                    Text(
                        "Tap untuk update status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onShowReceipt,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Receipt, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lihat & Bagikan Struk", style = MaterialTheme.typography.labelSmall)
                }
                if (!isDone) {
                    TextButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Batalkan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
