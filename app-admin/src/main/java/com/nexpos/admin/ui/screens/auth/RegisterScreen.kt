package com.nexpos.admin.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexpos.admin.ui.viewmodel.AuthViewModel
import com.nexpos.core.ui.components.NexPosButton
import com.nexpos.core.ui.components.NexPosTextField

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess && !hasNavigated) {
            hasNavigated = true
            try {
                onRegisterSuccess()
            } catch (e: Exception) {
                hasNavigated = false
                viewModel.clearError()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Daftar Akun",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Buat akun owner baru",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(40.dp))

        NexPosTextField(
            value = name,
            onValueChange = { name = it },
            label = "Nama Lengkap",
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        NexPosTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        NexPosTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(24.dp))

        state.error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        NexPosButton(
            text = "Daftar",
            onClick = { viewModel.register(email, password, name) },
            isLoading = state.isLoading,
            enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sudah punya akun?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = {
                try { onNavigateToLogin() } catch (_: Exception) { }
            }) { Text("Masuk") }
        }
    }
}
