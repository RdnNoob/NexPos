package com.nexpos.admin.ui.screens.outlets

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexpos.admin.ui.viewmodel.OutletViewModel
import com.nexpos.core.ui.components.NexPosButton
import com.nexpos.core.ui.components.NexPosTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOutletScreen(
    onNavigateBack: () -> Unit,
    onOutletCreated: () -> Unit,
    viewModel: OutletViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var outletName by remember { mutableStateOf("") }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) onOutletCreated()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buat Outlet Baru") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Nama Outlet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            NexPosTextField(
                value = outletName,
                onValueChange = { outletName = it },
                label = "Contoh: Laundry Bersih Jaya",
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) }
            )

            state.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    "Setelah outlet dibuat, kode aktivasi akan otomatis dibuat. Bagikan kode tersebut ke kasir untuk login di Aplikasi Laundry.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            NexPosButton(
                text = "Buat Outlet",
                onClick = { viewModel.createOutlet(outletName) },
                isLoading = state.isLoading,
                enabled = outletName.isNotBlank()
            )
        }
    }
}
