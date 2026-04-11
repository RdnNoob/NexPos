package com.nexpos.admin.ui.screens.devices

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
import com.nexpos.admin.ui.viewmodel.DeviceViewModel
import com.nexpos.core.data.model.DeviceInfo
import com.nexpos.core.ui.components.LoadingScreen
import com.nexpos.core.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeviceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var deviceToLogout by remember { mutableStateOf<DeviceInfo?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    LaunchedEffect(state.message) {
        if (state.message != null) viewModel.clearMessage()
    }

    deviceToLogout?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceToLogout = null },
            title = { Text("Force Logout Device") },
            text = { Text("Yakin ingin paksa logout device '${device.deviceName}'? Device akan otomatis keluar saat request berikutnya.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.forceLogout(device.id)
                        deviceToLogout = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Force Logout") }
            },
            dismissButton = {
                TextButton(onClick = { deviceToLogout = null }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitor Device") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadDevices() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LoadingScreen("Memuat device...")
            }
        } else if (state.devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Belum ada device terdaftar")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val online = state.devices.count { it.status == "online" }
                        val offline = state.devices.count { it.status != "online" }
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text("Online: $online", modifier = Modifier.padding(8.dp, 4.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelMedium)
                        }
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.errorContainer) {
                            Text("Offline: $offline", modifier = Modifier.padding(8.dp, 4.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                items(state.devices) { device ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                null,
                                tint = if (device.status == "online") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.deviceName, fontWeight = FontWeight.Bold)
                                Text(
                                    device.outletName ?: "Outlet tidak diketahui",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                device.lastSeen?.let {
                                    Text("Terakhir: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusChip(status = device.status)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (device.status == "online") {
                                    OutlinedButton(
                                        onClick = { deviceToLogout = device },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Logout, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Force Logout", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
