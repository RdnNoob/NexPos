package com.nexpos.admin.ui.screens.superadmin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexpos.admin.ui.viewmodel.SuperAdminViewModel
import com.nexpos.core.ui.components.NexPosButton
import com.nexpos.core.ui.components.NexPosTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminLoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    viewModel: SuperAdminViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var navigated by remember { mutableStateOf(false) }

    LaunchedEffect(state.token) {
        if (state.token.isNotBlank() && !navigated) {
            navigated = true
            onLoginSuccess(state.token)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Super Admin") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Kembali") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Panel Tersembunyi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Masuk untuk kontrol semua owner dan OTP request.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(32.dp))
            NexPosTextField(
                value = username,
                onValueChange = { username = it },
                label = "ID Super Admin",
                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            NexPosTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true,
                leadingIcon = { Icon(Icons.Default.Lock, null) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            state.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            NexPosButton(
                text = "Masuk Super Admin",
                onClick = { viewModel.login(username, password) },
                isLoading = state.isLoading,
                enabled = username.isNotBlank() && password.isNotBlank()
            )
        }
    }
}