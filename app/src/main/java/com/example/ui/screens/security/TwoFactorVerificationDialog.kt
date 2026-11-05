package com.example.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.TwoFactorMethod
import com.example.ui.theme.BirbankRed
import com.example.ui.theme.BirbankRedLight
import com.example.ui.theme.BirbankTextPrimary
import com.example.ui.theme.BirbankTextSecondary
import kotlinx.coroutines.delay

@Composable
fun TwoFactorVerificationDialog(
    method: TwoFactorMethod,
    destinationMasked: String,
    onDismiss: () -> Unit,
    onVerify: (code: String, onError: (String) -> Unit) -> Unit,
    onResend: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    var resendCooldown by remember { mutableIntStateOf(60) }
    var totalTimerSeconds by remember { mutableIntStateOf(300) } // 5 minutes

    LaunchedEffect(Unit) {
        while (totalTimerSeconds > 0) {
            delay(1000L)
            totalTimerSeconds -= 1
            if (resendCooldown > 0) {
                resendCooldown -= 1
            }
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

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "İki Mərhələli Təsdiqləmə (2FA)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BirbankTextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (method == TwoFactorMethod.SMS) {
                        "Təhlükəsizlik kodu $destinationMasked nömrəsinə real SMS vasitəsilə göndərildi."
                    } else {
                        "Təhlükəsizlik kodu $destinationMasked ünvanına göndərildi."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = BirbankTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            codeInput = it
                            errorMessage = null
                        }
                    },
                    label = { Text("6 rəqəmli təsdiq kodu") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_2fa_code"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BirbankRed,
                        cursorColor = BirbankRed
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val minutes = totalTimerSeconds / 60
                    val seconds = totalTimerSeconds % 60
                    Text(
                        text = "Kodun vaxtı: ${String.format("%02d:%02d", minutes, seconds)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (totalTimerSeconds < 60) MaterialTheme.colorScheme.error else BirbankTextSecondary
                    )

                    if (resendCooldown > 0) {
                        Text(
                            text = "Yenidən göndər ($resendCooldown s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = BirbankTextSecondary
                        )
                    } else {
                        TextButton(
                            onClick = {
                                resendCooldown = 60
                                onResend()
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                text = "Yenidən göndər",
                                color = BirbankRed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (codeInput.length < 6) {
                            errorMessage = "Zəhmət olmasa 6 rəqəmli kodu tam daxil edin."
                            return@Button
                        }
                        isVerifying = true
                        errorMessage = null
                        onVerify(codeInput) { err ->
                            isVerifying = false
                            errorMessage = err
                        }
                    },
                    enabled = !isVerifying && codeInput.length == 6 && totalTimerSeconds > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BirbankRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_verify_2fa_submit")
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Təsdiq et", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
