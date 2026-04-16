package com.nexpos.admin.ui.screens.dashboard

import androidx.compose.foundation.clickable
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
import com.nexpos.admin.ui.viewmodel.DashboardViewModel
import com.nexpos.core.ui.components.LoadingScreen
import com.nexpos.core.ui.components.StatCard
import com.nexpos.core.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToOutlets: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSuperAdmin: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var logoTapCount by remember { mutableStateOf(0) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Keluar") },
            text = { Text("Yakin ingin keluar dari akun ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Keluar") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "NexPos Owners",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                logoTapCount += 1
                                if (logoTapCount >= 5) {
                                    logoTapCount = 0
                                    onNavigateToSuperAdmin()
                                }
                            }
                        )
                        Text(
                            "Selamat datang, ${state.userName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, "Notifikasi", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onNavigateToAccount) {
                        Icon(Icons.Default.AccountCircle, "Akun", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, "Keluar", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LoadingScreen("Memuat dashboard...")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total Outlet",
                            value = "${state.outlets.size}",
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        StatCard(
                            title = "Device Online",
                            value = "${state.devices.count { it.status == "online" }}",
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.PhoneAndroid, null, tint = MaterialTheme.colorScheme.secondary) }
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total Transaksi",
                            value = "${state.transactions.size}",
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.tertiary) }
                        )
                        StatCard(
                            title = "Total Pendapatan",
                            value = "Rp ${"%,.0f".format(state.transactions.sumOf { it.amount })}",
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.secondary) }
                        )
                    }
                }
                item {
                    Text("Menu Utama", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MenuCard(
                            title = "Outlet",
                            subtitle = "${state.outlets.size} outlet aktif",
                            icon = Icons.Default.Store,
                            onClick = onNavigateToOutlets,
                            modifier = Modifier.weight(1f)
                        )
                        MenuCard(
                            title = "Device",
                            subtitle = "${state.devices.size} device terdaftar",
                            icon = Icons.Default.PhoneAndroid,
                            onClick = onNavigateToDevices,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToTransactions) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Semua Transaksi", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${state.transactions.size} transaksi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
                if (state.transactions.isNotEmpty()) {
                    item {
                        Text("Transaksi Terbaru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(state.transactions.take(5)) { tx ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.customer, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        tx.outletName ?: "Outlet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Rp ${"%,.0f".format(tx.amount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                StatusChip(status = tx.status)
                            }
                        }
                    }
                }
                state.error?.let { error ->
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(error, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
