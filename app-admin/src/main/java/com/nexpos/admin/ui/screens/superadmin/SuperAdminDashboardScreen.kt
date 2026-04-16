package com.nexpos.admin.ui.screens.superadmin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexpos.admin.ui.viewmodel.SuperAdminViewModel
import com.nexpos.core.ui.components.LoadingScreen
import com.nexpos.core.ui.components.StatCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
    token: String,
    onLogout: () -> Unit,
    viewModel: SuperAdminViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(token) { viewModel.load(token) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Super Admin NexPos") },
                actions = {
                    IconButton(onClick = { viewModel.load(token) }) { Icon(Icons.Default.Refresh, "Refresh") }
                    TextButton(onClick = onLogout) { Text("Keluar") }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingScreen("Memuat super admin...") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            state.error?.let { error ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(error, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            state.stats?.let { stats ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("User", stats.totalUsers.toString(), Modifier.weight(1f))
                        StatCard("Outlet", stats.totalOutlets.toString(), Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("Device", stats.totalDevices.toString(), Modifier.weight(1f))
                        StatCard("OTP", stats.totalOtpRequests.toString(), Modifier.weight(1f))
                    }
                }
            }
            item { Text("Permintaan OTP Terbaru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (state.otpRequests.isEmpty()) {
                item { Text("Belum ada permintaan OTP.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.otpRequests.take(10)) { req ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(req.name ?: req.email, fontWeight = FontWeight.SemiBold)
                            Text(req.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("OTP: ${req.otpCode}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            req.createdAt?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
            item { Text("Semua User Owner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(state.users) { user ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(user.name, fontWeight = FontWeight.SemiBold)
                                Text(user.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${user.outletCount} outlet • ${user.deviceCount} device • ${user.accountStatus}")
                                user.penaltyReason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (user.accountStatus == "banned") {
                                OutlinedButton(onClick = { viewModel.unban(user.id) }) {
                                    Icon(Icons.Default.LockOpen, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Unban")
                                }
                            } else {
                                OutlinedButton(onClick = { viewModel.ban(user.id) }) {
                                    Icon(Icons.Default.Block, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Ban")
                                }
                            }
                            Button(
                                onClick = { viewModel.deleteUser(user.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Hapus")
                            }
                        }
                    }
                }
            }
            }
        }
    }
}