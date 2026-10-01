package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizationManager
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.ui.components.BestGroupLogoBadge
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLogin by remember { mutableStateOf(true) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("rzayevnicat00@gmail.com") }
    var phone by remember { mutableStateOf("+994 (50) 412-38-90") }
    var password by remember { mutableStateOf("bestgroup2026") }
    var confirmPassword by remember { mutableStateOf("bestgroup2026") }
    var passwordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    val isLoading by viewModel.isAuthLoading.collectAsState()

    val scrollState = rememberScrollState()

    fun performRegistration() {
        viewModel.register(
            email = email,
            pass = password,
            fullName = fullName,
            phone = phone,
            university = "ADNSU",
            faculty = "İnformasiya Texnologiyaları",
            educationLevel = "Magistratura",
            onSuccess = onAuthSuccess,
            onError = { err -> errorMessage = err }
        )
    }

    fun validateAndProceed() {
        errorMessage = null
        if (!isLogin && fullName.isBlank()) {
            errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_NAME_REQUIRED)
            return
        }
        if (!email.contains("@") || !email.contains(".")) {
            errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_EMAIL_INVALID)
            return
        }
        if (!isLogin && phone.isBlank()) {
            errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_PHONE_REQUIRED)
            return
        }
        if (password.length < 6) {
            errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_PASSWORD_SHORT)
            return
        }
        if (!isLogin && password != confirmPassword) {
            errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_PASSWORDS_DONT_MATCH)
            return
        }

        if (!isLogin) {
            performRegistration()
        } else {
            viewModel.login(
                email = email,
                pass = password,
                onSuccess = onAuthSuccess,
                onError = { err -> errorMessage = err }
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BestGroupLogoBadge(size = 32)
                        Text(
                            text = "BestGroup.az",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = localizedString(if (isLogin) StringKey.AUTH_LOGIN_TITLE else StringKey.AUTH_REGISTER_TITLE),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = localizedString(StringKey.APP_SLOGAN),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login / Register Tabs
            TabRow(
                selectedTabIndex = if (isLogin) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = Gold500,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = isLogin,
                    onClick = { isLogin = true; errorMessage = null },
                    text = {
                        Text(
                            text = localizedString(StringKey.BTN_LOGIN),
                            fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = !isLogin,
                    onClick = { isLogin = false; errorMessage = null },
                    text = {
                        Text(
                            text = localizedString(StringKey.BTN_REGISTER),
                            fontWeight = if (!isLogin) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error display
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Name (if registering)
            if (!isLogin) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(localizedString(StringKey.LABEL_FULL_NAME)) },
                    placeholder = { Text(localizedString(StringKey.HINT_FULL_NAME)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_input_fullname")
                )
            }

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(localizedString(StringKey.LABEL_EMAIL)) },
                placeholder = { Text(localizedString(StringKey.HINT_EMAIL)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .testTag("auth_input_email")
            )

            // Phone (if registering)
            if (!isLogin) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(localizedString(StringKey.LABEL_PHONE)) },
                    placeholder = { Text(localizedString(StringKey.HINT_PHONE)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_input_phone")
                )
            }

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(localizedString(StringKey.LABEL_PASSWORD)) },
                placeholder = { Text(localizedString(StringKey.HINT_PASSWORD)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (!isLogin) 14.dp else 4.dp)
                    .testTag("auth_input_password")
            )

            // Confirm Password (if registering)
            if (!isLogin) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(localizedString(StringKey.LABEL_CONFIRM_PASSWORD)) },
                    placeholder = { Text(localizedString(StringKey.HINT_CONFIRM_PASSWORD)) },
                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_input_confirm_password")
                )
            }

            // Forgot password link (if login)
            if (isLogin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showForgotPasswordDialog = true }) {
                        Text(
                            text = localizedString(StringKey.AUTH_FORGOT_PASSWORD_LINK),
                            style = MaterialTheme.typography.bodySmall,
                            color = Gold500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Action Button
            Button(
                onClick = { validateAndProceed() },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy800,
                    contentColor = Gold500
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Gold500,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = localizedString(if (isLogin) StringKey.BTN_LOGIN else StringKey.BTN_REGISTER),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        var resetSent by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text(localizedString(StringKey.AUTH_FORGOT_PASSWORD_TITLE), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (resetSent) {
                        Text(
                            "Şifrə sıfırlama təlimatı $resetEmail ünvanına göndərildi.",
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text("Qeydiyyatdan keçdiyiniz e-mail ünvanını daxil edin:")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("E-mail") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!resetSent) {
                            if (resetEmail.isNotBlank()) {
                                viewModel.sendPasswordReset(
                                    email = resetEmail,
                                    onSuccess = { resetSent = true },
                                    onError = { err -> errorMessage = err; showForgotPasswordDialog = false }
                                )
                            }
                        } else {
                            showForgotPasswordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500)
                ) {
                    Text(if (resetSent) "Bağla" else "Kodu göndər")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text(localizedString(StringKey.BTN_CANCEL))
                }
            }
        )
    }
}


