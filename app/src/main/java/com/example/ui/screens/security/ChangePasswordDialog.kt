package com.example.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BirbankRed
import com.example.ui.theme.BirbankRedLight
import com.example.ui.theme.BirbankTextPrimary
import com.example.ui.theme.BirbankTextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun ChangePasswordDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
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
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = BirbankRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Icon(Icons.Default.Close, contentDescription = "Bağla")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Şifrəni Dəyişmək",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BirbankTextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Hesabınızın təhlükəsizliyi üçün cari şifrənizi və yeni etibarlı şifrəni daxil edin:",
                    style = MaterialTheme.typography.bodySmall,
                    color = BirbankTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = {
                        oldPassword = it
                        errorMessage = null
                    },
                    label = { Text("Cari şifrə") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("input_change_old_pass"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BirbankRed,
                        cursorColor = BirbankRed
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = null
                    },
                    label = { Text("Yeni şifrə (min. 6 simvol)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("input_change_new_pass"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BirbankRed,
                        cursorColor = BirbankRed
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Yeni şifrəni təsdiqləyin") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("input_change_confirm_pass"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BirbankRed,
                        cursorColor = BirbankRed
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (oldPassword.isBlank()) {
                            errorMessage = "Cari şifrəni daxil edin."
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            errorMessage = "Yeni şifrə ən azı 6 simvoldan ibarət olmalıdır."
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            errorMessage = "Yeni şifrələr uyğun gəlmir."
                            return@Button
                        }
                        isSubmitting = true
                        errorMessage = null
                        viewModel.changePassword(
                            oldPass = oldPassword,
                            newPass = newPassword,
                            onSuccess = {
                                isSubmitting = false
                                onSuccess()
                            },
                            onError = { err ->
                                isSubmitting = false
                                errorMessage = err
                            }
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_change_password_submit")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Şifrəni Yenilə", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
