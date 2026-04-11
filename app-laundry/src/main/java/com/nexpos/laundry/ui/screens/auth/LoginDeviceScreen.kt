package com.nexpos.laundry.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexpos.core.ui.components.NexPosButton
import com.nexpos.core.ui.components.NexPosTextField
import com.nexpos.laundry.ui.viewmodel.DeviceAuthViewModel

@Composable
fun LoginDeviceScreen(
    onLoginSuccess: () -> Unit,
    viewModel: DeviceAuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var activationCode by remember { mutableStateOf("") }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "NexPos Laundry",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Aplikasi Kasir Outlet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Masukkan kode aktivasi outlet yang diberikan oleh owner",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))

        NexPosTextField(
            value = activationCode,
            onValueChange = { activationCode = it.uppercase() },
            label = "Kode Aktivasi",
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(24.dp))

        state.error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        NexPosButton(
            text = "Masuk ke Outlet",
            onClick = { viewModel.loginWithCode(activationCode) },
            isLoading = state.isLoading,
            enabled = activationCode.length >= 4
        )

        Spacer(modifier = Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text(
                "Kode aktivasi didapat dari owner melalui aplikasi NexPos Admin",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
