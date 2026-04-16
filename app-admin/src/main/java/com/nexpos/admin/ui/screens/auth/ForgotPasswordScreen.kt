package com.nexpos.admin.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.net.Uri
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import com.nexpos.admin.ui.viewmodel.ForgotPasswordStep
import com.nexpos.admin.ui.viewmodel.ForgotPasswordViewModel
import com.nexpos.core.ui.components.NexPosButton
import com.nexpos.core.ui.components.NexPosTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onResetSuccess: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    fun openEmailCs(targetEmail: String) {
        val subject = Uri.encode("Permintaan OTP Reset Password NexPos")
        val body = Uri.encode("Halo Customer Service NexPos, saya ingin meminta kode OTP reset password untuk email: ${targetEmail.trim().lowercase()}")
        uriHandler.openUri("mailto:raden.muhammad.ichsan@gmail.com?subject=$subject&body=$body")
    }

    fun openWhatsAppCs(targetEmail: String) {
        val text = Uri.encode("Halo Customer Service NexPos, saya ingin meminta kode OTP reset password untuk email: ${targetEmail.trim().lowercase()}")
        uriHandler.openUri("https://wa.me/6289606056767?text=$text")
    }

    // Navigate to login once done
    LaunchedEffect(state.step) {
        if (state.step == ForgotPasswordStep.DONE) {
            // stay on screen to show success message, user presses button to go back
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lupa Password") },
                navigationIcon = {
                    IconButton(onClick = {
                        try { onNavigateBack() } catch (_: Exception) { }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Step indicator
            StepIndicator(currentStep = state.step)

            Spacer(modifier = Modifier.height(32.dp))

            when (state.step) {
                ForgotPasswordStep.EMAIL -> EmailStep(
                    email = email,
                    onEmailChange = { email = it; viewModel.clearError() },
                    isLoading = state.isLoading,
                    error = state.error,
                    onSendOtp = { viewModel.sendOtp(email) },
                    onContactEmail = { openEmailCs(email) },
                    onContactWhatsApp = { openWhatsAppCs(email) }
                )

                ForgotPasswordStep.OTP -> OtpStep(
                    email = state.email,
                    otp = otp,
                    onOtpChange = { otp = it; viewModel.clearError() },
                    isLoading = state.isLoading,
                    error = state.error,
                    successMessage = state.successMessage,
                    resendCountdown = state.resendCountdown,
                    onVerifyOtp = { viewModel.verifyOtp(otp) },
                    onResendOtp = { viewModel.resendOtp() },
                    onClearSuccess = { viewModel.clearSuccessMessage() },
                    onContactEmail = { openEmailCs(state.email) },
                    onContactWhatsApp = { openWhatsAppCs(state.email) }
                )

                ForgotPasswordStep.CONTACT_CS -> ContactCsStep(
                    email = state.email,
                    onContactEmail = { openEmailCs(state.email) },
                    onContactWhatsApp = { openWhatsAppCs(state.email) },
                    onNavigateBack = { try { onNavigateBack() } catch (_: Exception) { } }
                )

                ForgotPasswordStep.NEW_PASSWORD -> NewPasswordStep(
                    newPassword = newPassword,
                    confirmPassword = confirmPassword,
                    onNewPasswordChange = { newPassword = it; viewModel.clearError() },
                    onConfirmPasswordChange = { confirmPassword = it; viewModel.clearError() },
                    isLoading = state.isLoading,
                    error = state.error,
                    onResetPassword = { viewModel.resetPassword(newPassword, confirmPassword) }
                )

                ForgotPasswordStep.DONE -> DoneStep(
                    message = state.successMessage ?: "Password berhasil diperbarui",
                    onGoToLogin = {
                        try { onResetSuccess() } catch (_: Exception) { }
                    }
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: ForgotPasswordStep) {
    val steps = listOf("Kirim OTP", "Verifikasi OTP", "Password Baru")
    val currentIndex = when (currentStep) {
        ForgotPasswordStep.EMAIL -> 0
        ForgotPasswordStep.CONTACT_CS -> 0
        ForgotPasswordStep.OTP -> 1
        ForgotPasswordStep.NEW_PASSWORD -> 2
        ForgotPasswordStep.DONE -> 2
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { index, label ->
            val isActive = index == currentIndex
            val isCompleted = index < currentIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(72.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isActive || isCompleted)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (index + 1).toString(),
                        color = if (isActive || isCompleted)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (index < steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                color = if (index < currentIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailStep(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onSendOtp: () -> Unit,
    onContactEmail: () -> Unit,
    onContactWhatsApp: () -> Unit
) {
    Text(
        text = "Masukkan email terdaftar. Kode OTP akan masuk ke panel Customer Service, lalu CS mengirimkannya ke kamu.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))

    NexPosTextField(
        value = email,
        onValueChange = onEmailChange,
        label = "Alamat Email",
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        )
    )
    Spacer(modifier = Modifier.height(16.dp))

    error?.let { ErrorCard(it) }
    if (error != null) Spacer(modifier = Modifier.height(16.dp))

    NexPosButton(
        text = "Buat Permintaan OTP",
        onClick = onSendOtp,
        isLoading = isLoading,
        enabled = email.isNotBlank()
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = onContactEmail,
        modifier = Modifier.fillMaxWidth(),
        enabled = email.isNotBlank()
    ) {
        Text("Hubungi CS via Gmail")
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onContactWhatsApp,
        modifier = Modifier.fillMaxWidth(),
        enabled = email.isNotBlank()
    ) {
        Text("Hubungi CS via WhatsApp")
    }
}

@Composable
private fun OtpStep(
    email: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    successMessage: String?,
    resendCountdown: Int,
    onVerifyOtp: () -> Unit,
    onResendOtp: () -> Unit,
    onClearSuccess: () -> Unit,
    onContactEmail: () -> Unit,
    onContactWhatsApp: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) { }
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3_000)
            onClearSuccess()
        }
    }

    Text(
        text = "Permintaan OTP untuk:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = email,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(24.dp))

    // 6-digit OTP input
    OtpInputField(
        otp = otp,
        onOtpChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) onOtpChange(it) },
        modifier = Modifier.focusRequester(focusRequester)
    )
    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Hubungi CS melalui tombol di bawah. Setelah menerima kode, masukkan 6 digit OTP di sini.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))

    successMessage?.let {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text(
                text = it,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    error?.let { ErrorCard(it) }
    if (error != null) Spacer(modifier = Modifier.height(16.dp))

    NexPosButton(
        text = "Verifikasi OTP",
        onClick = onVerifyOtp,
        isLoading = isLoading,
        enabled = otp.length == 6
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(onClick = onContactEmail, modifier = Modifier.fillMaxWidth()) {
        Text("Hubungi CS via Gmail")
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onContactWhatsApp, modifier = Modifier.fillMaxWidth()) {
        Text("Hubungi CS via WhatsApp")
    }
    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Perlu buat ulang kode?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        if (resendCountdown > 0) {
            Text(
                text = "Minta ulang (${resendCountdown}s)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            TextButton(
                onClick = onResendOtp,
                enabled = !isLoading
            ) { Text("Minta Ulang") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtpInputField(
    otp: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Display 6 boxes, backed by a single invisible TextField
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Invisible backing text field
        OutlinedTextField(
            value = otp,
            onValueChange = onOtpChange,
            modifier = modifier
                .fillMaxWidth()
                .height(1.dp)
                .border(0.dp, MaterialTheme.colorScheme.surface),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.surface,
                unfocusedTextColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.surface
            )
        )
        // Visual boxes
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) { index ->
                val char = otp.getOrNull(index)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .border(
                            width = 2.dp,
                            color = if (index == otp.length) MaterialTheme.colorScheme.primary
                            else if (index < otp.length) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.small
                        )
                        .background(
                            color = if (index < otp.length) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char?.toString() ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun NewPasswordStep(
    newPassword: String,
    confirmPassword: String,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onResetPassword: () -> Unit
) {
    Text(
        text = "Buat password baru yang kuat",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))

    NexPosTextField(
        value = newPassword,
        onValueChange = onNewPasswordChange,
        label = "Password Baru",
        isPassword = true,
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    NexPosTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = "Konfirmasi Password Baru",
        isPassword = true,
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Password minimal 6 karakter",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))

    error?.let { ErrorCard(it) }
    if (error != null) Spacer(modifier = Modifier.height(16.dp))

    NexPosButton(
        text = "Perbarui Password",
        onClick = onResetPassword,
        isLoading = isLoading,
        enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank()
    )
}

@Composable
private fun DoneStep(
    message: String,
    onGoToLogin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Password Berhasil Diperbarui!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        NexPosButton(
            text = "Masuk Sekarang",
            onClick = onGoToLogin
        )
    }
}

@Composable
private fun ContactCsStep(
    email: String,
    onContactEmail: () -> Unit,
    onContactWhatsApp: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Batas Permintaan OTP Habis",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Kamu sudah mencapai batas maksimal 3x permintaan OTP dalam 24 jam.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hubungi Customer Service kami untuk membantu reset password akun:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (email.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        NexPosButton(
            text = "Hubungi CS via WhatsApp",
            onClick = onContactWhatsApp
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onContactEmail,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hubungi CS via Email")
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNavigateBack) {
            Text("Kembali ke Login")
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
