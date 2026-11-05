package com.example.ui.screens.security

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.model.User
import com.example.services.security.PinValidationResult
import com.example.ui.components.BestGroupLogoBadge
import com.example.ui.theme.BirbankRed
import com.example.ui.theme.BirbankRedLight
import com.example.ui.theme.BirbankTextPrimary
import com.example.ui.theme.BirbankTextSecondary
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun AppLockScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val currentUser by viewModel.currentUser.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableLongStateOf(0L) }
    var showForgotPinDialog by remember { mutableStateOf(false) }

    // Lockout countdown timer
    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            while (lockoutSeconds > 0) {
                delay(1000L)
                lockoutSeconds -= 1
            }
            errorMessage = null
        }
    }

    // Auto-trigger biometric prompt on first display if enabled
    LaunchedEffect(isBiometricEnabled, activity) {
        if (isBiometricEnabled && activity != null && lockoutSeconds == 0L) {
            viewModel.authenticateBiometric(
                activity = activity,
                onSuccess = {},
                onError = { /* fallback silently to PIN keypad */ }
            )
        }
    }

    fun onDigit(digit: String) {
        if (lockoutSeconds > 0) return
        errorMessage = null
        if (enteredPin.length < 4) {
            val updated = enteredPin + digit
            enteredPin = updated
            if (updated.length == 4) {
                // Validate PIN
                val result = viewModel.validatePin(updated)
                when (result) {
                    is PinValidationResult.Success -> {
                        enteredPin = ""
                        errorMessage = null
                    }
                    is PinValidationResult.WrongPin -> {
                        enteredPin = ""
                        errorMessage = "Yanlış PIN kod! Qalan cəhd sayı: ${result.attemptsLeft}"
                    }
                    is PinValidationResult.LockedOut -> {
                        enteredPin = ""
                        lockoutSeconds = result.secondsRemaining
                        errorMessage = "Həddindən artıq yanlış cəhd! Gözləmə müddəti: ${result.secondsRemaining} saniyə"
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    fun onBiometricTap() {
        if (activity != null && lockoutSeconds == 0L) {
            viewModel.authenticateBiometric(
                activity = activity,
                onSuccess = {},
                onError = { err ->
                    if (err != "PIN_FALLBACK") {
                        errorMessage = err
                    }
                }
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Logo, User greeting & instruction
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                BestGroupLogoBadge(size = 56)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "BestGroup.az Təhlükəsizlik",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BirbankTextPrimary
                )
                Text(
                    text = currentUser?.fullName?.takeIf { it.isNotBlank() } ?: "Xoş gəlmisiniz",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BirbankTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    color = BirbankRedLight,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BirbankRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (lockoutSeconds > 0) "Müvəqqəti bloklanıb" else "Tətbiqi açmaq üçün PIN daxil edin",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BirbankRed
                        )
                    }
                }

                // PIN dots indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) BirbankRed else Color.Transparent)
                                .border(2.dp, if (isFilled) BirbankRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                        )
                    }
                }

                if (lockoutSeconds > 0) {
                    Text(
                        text = "Təhlükəsizlik bloklaması: $lockoutSeconds saniyə",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            // Numeric Keypad
            PinKeypad(
                onDigit = { onDigit(it) },
                onBackspace = { onBackspace() },
                showBiometric = isBiometricEnabled,
                onBiometricClick = { onBiometricTap() }
            )

            // Forgot PIN button
            TextButton(
                onClick = { showForgotPinDialog = true },
                modifier = Modifier.testTag("btn_forgot_pin")
            ) {
                Text(
                    text = "PIN kodumu unutmuşam",
                    color = BirbankRed,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showForgotPinDialog) {
        ForgotPinDialog(
            user = currentUser,
            viewModel = viewModel,
            onDismiss = { showForgotPinDialog = false },
            onPinResetSuccess = {
                showForgotPinDialog = false
                enteredPin = ""
                errorMessage = null
                lockoutSeconds = 0L
            }
        )
    }
}

@Composable
fun ForgotPinDialog(
    user: User?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onPinResetSuccess: () -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "PIN Kodu Sıfırlamaq",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PIN kodu ləğv etmək üçün mövcud hesabınızın (${user?.email ?: "hesabınız"}) şifrəsini daxil edin:",
                    style = MaterialTheme.typography.bodySmall,
                    color = BirbankTextSecondary
                )
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        resetError = null
                    },
                    label = { Text("Hesab şifrəsi") },
                    singleLine = true,
                    isError = resetError != null,
                    modifier = Modifier.fillMaxWidth().testTag("input_reset_pin_pass"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BirbankRed,
                        cursorColor = BirbankRed
                    )
                )
                if (resetError != null) {
                    Text(
                        text = resetError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passwordInput.isBlank()) {
                        resetError = "Şifrə boş ola bilməz."
                        return@Button
                    }
                    isVerifying = true
                    resetError = null
                    viewModel.verifyPasswordAndResetPin(
                        password = passwordInput,
                        onSuccess = {
                            isVerifying = false
                            onPinResetSuccess()
                        },
                        onError = { err ->
                            isVerifying = false
                            resetError = err
                        }
                    )
                },
                enabled = !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Təsdiq et və Sıfırla")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isVerifying) {
                Text("Ləğv et")
            }
        }
    )
}
