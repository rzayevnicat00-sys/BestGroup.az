package com.example.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.example.services.security.PinValidationResult
import com.example.ui.theme.BirbankRed
import com.example.ui.theme.BirbankRedLight
import com.example.ui.theme.BirbankTextPrimary
import com.example.ui.theme.BirbankTextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun AdminReauthDialog(
    actionTitle: String,
    actionDescription: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onAuthorized: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()

    var passwordInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    // If biometric is enabled, auto-prompt for fast curator convenience
    LaunchedEffect(Unit) {
        if (isBiometricEnabled && activity != null) {
            viewModel.authenticateBiometric(
                activity = activity,
                onSuccess = { onAuthorized() },
                onError = { /* fallback to dialog */ }
            )
        }
    }

    Dialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(BirbankRedLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = BirbankRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isVerifying) {
                        Icon(Icons.Default.Close, contentDescription = "Bağla")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Təhlükəsizlik Təsdiqi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BirbankTextPrimary,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = actionTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = actionDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }

                if (isPinConfigured) {
                    Text(
                        text = "Əməliyyatı təsdiqləmək üçün 4 rəqəmli PIN kodunuzu daxil edin:",
                        style = MaterialTheme.typography.bodySmall,
                        color = BirbankTextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pinInput = it
                                errorMessage = null
                                if (it.length == 4) {
                                    val res = viewModel.validatePin(it)
                                    if (res is PinValidationResult.Success) {
                                        onAuthorized()
                                    } else {
                                        errorMessage = "PIN kod yanlışdır."
                                    }
                                }
                            }
                        },
                        label = { Text("PIN kod") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_admin_reauth_pin"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BirbankRed,
                            cursorColor = BirbankRed
                        )
                    )
                } else {
                    Text(
                        text = "Əməliyyatı təsdiqləmək üçün hesab şifrənizi daxil edin:",
                        style = MaterialTheme.typography.bodySmall,
                        color = BirbankTextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            errorMessage = null
                        },
                        label = { Text("Hesab şifrəsi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_admin_reauth_password"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BirbankRed,
                            cursorColor = BirbankRed
                        )
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isBiometricEnabled && activity != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.authenticateBiometric(
                                    activity = activity,
                                    onSuccess = { onAuthorized() },
                                    onError = { err -> if (err != "PIN_FALLBACK") errorMessage = err }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = BirbankRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Barmaq izi", color = BirbankRed, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (isPinConfigured) {
                                if (pinInput.length == 4) {
                                    val res = viewModel.validatePin(pinInput)
                                    if (res is PinValidationResult.Success) {
                                        onAuthorized()
                                    } else {
                                        errorMessage = "PIN kod yanlışdır."
                                    }
                                } else {
                                    errorMessage = "PIN kod 4 rəqəmdən ibarət olmalıdır."
                                }
                            } else {
                                if (passwordInput.isNotBlank()) {
                                    isVerifying = true
                                    viewModel.verifyPasswordAndResetPin(
                                        password = passwordInput,
                                        onSuccess = {
                                            isVerifying = false
                                            onAuthorized()
                                        },
                                        onError = { err ->
                                            isVerifying = false
                                            errorMessage = err
                                        }
                                    )
                                } else {
                                    errorMessage = "Şifrə daxil edilməlidir."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("btn_admin_reauth_confirm")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Təsdiqlə", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
