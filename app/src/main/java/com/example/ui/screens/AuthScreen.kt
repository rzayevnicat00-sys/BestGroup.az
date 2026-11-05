package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.example.model.User
import com.example.localization.LocalizationManager
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.ui.components.BestGroupLogoBadge
import com.example.model.TwoFactorMethod
import com.example.ui.screens.security.TwoFactorVerificationDialog
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

private fun Context.findActivity(): Activity? {
    var cur = this
    while (cur is ContextWrapper) {
        if (cur is Activity) return cur
        cur = cur.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLogin by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Login tab state - strictly initialized to empty
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    // Register tab state - strictly initialized to empty
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regUniversity by remember { mutableStateOf("") }
    var regFaculty by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }

    // Real Phone OTP Verification State (Firebase Phone Auth)
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    var otpTimerSeconds by remember { mutableIntStateOf(60) }
    var otpAttempts by remember { mutableIntStateOf(0) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var phoneVerificationId by remember { mutableStateOf("") }
    var isSendingSms by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    val isLoading by viewModel.isAuthLoading.collectAsState()

    val scrollState = rememberScrollState()

    LaunchedEffect(showOtpDialog) {
        if (showOtpDialog) {
            otpTimerSeconds = 60
            while (otpTimerSeconds > 0) {
                delay(1000L)
                otpTimerSeconds -= 1
            }
        }
    }

    fun performRegistration(phoneVerified: Boolean) {
        val cleanPhone = regPhone.trim().replace(" ", "").replace("-", "")
        viewModel.register(
            email = regEmail.trim(),
            pass = regPassword,
            fullName = regFullName.trim(),
            phone = cleanPhone,
            university = regUniversity.trim().ifBlank { "Təyin edilməyib" },
            faculty = regFaculty.trim().ifBlank { "Təyin edilməyib" },
            educationLevel = "Bakalavriat",
            phoneVerified = phoneVerified,
            onSuccess = onAuthSuccess,
            onError = { err -> errorMessage = err }
        )
    }

    fun startRegisterSmsVerification() {
        val cleanPhone = com.example.services.firebase.FirebaseAuthService.formatToE164(regPhone)
        val activity = context.findActivity()
        if (activity == null) {
            performRegistration(phoneVerified = false)
            return
        }

        isSendingSms = true
        errorMessage = null
        viewModel.startPhoneSmsVerification(
            activity = activity,
            phone = cleanPhone,
            onCodeSent = { vId ->
                isSendingSms = false
                phoneVerificationId = vId
                otpInput = ""
                otpError = null
                otpAttempts = 0
                otpTimerSeconds = 60
                showOtpDialog = true
            },
            onError = { err ->
                isSendingSms = false
                errorMessage = err
            }
        )
    }

    fun validateAndProceed() {
        errorMessage = null
        if (isLogin) {
            val inputClean = loginEmail.trim()
            if (loginPassword.length < 6) {
                errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_PASSWORD_SHORT)
                return
            }
            if (inputClean.contains("@")) {
                viewModel.login(
                    email = inputClean,
                    pass = loginPassword,
                    rememberMe = rememberMe,
                    onSuccess = onAuthSuccess,
                    onError = { err -> errorMessage = err }
                )
            } else {
                viewModel.loginWithPhone(
                    phone = inputClean,
                    pass = loginPassword,
                    rememberMe = rememberMe,
                    onSuccess = onAuthSuccess,
                    onError = { err -> errorMessage = err }
                )
            }
        } else {
            if (regFullName.isBlank()) {
                errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_NAME_REQUIRED)
                return
            }
            val emailClean = regEmail.trim()
            if (!emailClean.contains("@") || !emailClean.contains(".")) {
                errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_EMAIL_INVALID)
                return
            }
            val rawPhone = regPhone.trim().replace(" ", "").replace("-", "")
            if (rawPhone.isBlank()) {
                errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_PHONE_REQUIRED)
                return
            }
            val cleanPhone = if (rawPhone.startsWith("+")) rawPhone else "+$rawPhone"
            val digitsOnly = cleanPhone.filter { it.isDigit() }
            if (digitsOnly.length < 9) {
                errorMessage = "Telefon nömrəsi beynəlxalq formatda olmalıdır (məsələn: +994501234567)"
                return
            }
            if (regPassword.length < 6) {
                errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_PASSWORD_SHORT)
                return
            }
            if (regPassword != regConfirmPassword) {
                errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_PASSWORDS_DONT_MATCH)
                return
            }

            // Real Firebase Phone Auth SMS dispatch
            startRegisterSmsVerification()
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
                containerColor = Color.White,
                contentColor = BirbankRed,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[if (isLogin) 0 else 1]),
                        color = BirbankRed
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BirbankBorder, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = isLogin,
                    onClick = {
                        isLogin = true
                        errorMessage = null
                    },
                    selectedContentColor = BirbankRed,
                    unselectedContentColor = BirbankTextSecondary,
                    text = {
                        Text(
                            text = localizedString(StringKey.BTN_LOGIN),
                            fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
                Tab(
                    selected = !isLogin,
                    onClick = {
                        isLogin = false
                        errorMessage = null
                    },
                    selectedContentColor = BirbankRed,
                    unselectedContentColor = BirbankTextSecondary,
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

            if (isLogin) {
                // --- LOGIN FORM ---
                // Email
                OutlinedTextField(
                    value = loginEmail,
                    onValueChange = { loginEmail = it },
                    label = { Text(localizedString(StringKey.LABEL_EMAIL)) },
                    placeholder = { Text(localizedString(StringKey.HINT_EMAIL)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_input_email")
                )

                // Password
                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it },
                    label = { Text(localizedString(StringKey.LABEL_PASSWORD)) },
                    placeholder = { Text(localizedString(StringKey.HINT_PASSWORD)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                            Icon(
                                imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("auth_input_password")
                )

                // "Yadda saxla" checkbox & "Şifrəni unutmusunuz?"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { rememberMe = !rememberMe }
                            .testTag("auth_remember_me_row")
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BirbankRed,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.testTag("auth_remember_me_checkbox")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = localizedString(StringKey.AUTH_REMEMBER_ME),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    TextButton(
                        onClick = { showForgotPasswordDialog = true },
                        modifier = Modifier.testTag("auth_forgot_password_button")
                    ) {
                        Text(
                            text = localizedString(StringKey.AUTH_FORGOT_PASSWORD_LINK),
                            style = MaterialTheme.typography.bodySmall,
                            color = BirbankRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                // --- REGISTRATION FORM ---
                // Full Name
                OutlinedTextField(
                    value = regFullName,
                    onValueChange = { regFullName = it },
                    label = { Text(localizedString(StringKey.LABEL_FULL_NAME)) },
                    placeholder = { Text(localizedString(StringKey.HINT_FULL_NAME)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_input_fullname")
                )

                // Email
                OutlinedTextField(
                    value = regEmail,
                    onValueChange = { regEmail = it },
                    label = { Text(localizedString(StringKey.LABEL_EMAIL)) },
                    placeholder = { Text(localizedString(StringKey.HINT_EMAIL)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_input_email")
                )

                // Phone
                OutlinedTextField(
                    value = regPhone,
                    onValueChange = { regPhone = it },
                    label = { Text(localizedString(StringKey.LABEL_PHONE)) },
                    placeholder = { Text(localizedString(StringKey.HINT_PHONE)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_input_phone")
                )

                // University
                OutlinedTextField(
                    value = regUniversity,
                    onValueChange = { regUniversity = it },
                    label = { Text(localizedString(StringKey.LABEL_UNIVERSITY)) },
                    placeholder = { Text(localizedString(StringKey.HINT_UNIVERSITY)) },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_input_university")
                )

                // Faculty
                OutlinedTextField(
                    value = regFaculty,
                    onValueChange = { regFaculty = it },
                    label = { Text(localizedString(StringKey.LABEL_FACULTY)) },
                    placeholder = { Text(localizedString(StringKey.HINT_FACULTY)) },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_input_faculty")
                )

                // Password
                OutlinedTextField(
                    value = regPassword,
                    onValueChange = { regPassword = it },
                    label = { Text(localizedString(StringKey.LABEL_PASSWORD)) },
                    placeholder = { Text(localizedString(StringKey.HINT_PASSWORD)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                            Icon(
                                imageVector = if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_input_password")
                )

                // Confirm Password
                OutlinedTextField(
                    value = regConfirmPassword,
                    onValueChange = { regConfirmPassword = it },
                    label = { Text(localizedString(StringKey.LABEL_CONFIRM_PASSWORD)) },
                    placeholder = { Text(localizedString(StringKey.HINT_CONFIRM_PASSWORD)) },
                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_input_confirm_password")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Action Button
            Button(
                onClick = { validateAndProceed() },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BirbankRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
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

    // Forgot Password Dialog with Email & SMS OTP Options
    if (showForgotPasswordDialog) {
        var resetMode by remember { mutableStateOf(0) } // 0 = Email, 1 = SMS OTP
        var resetEmail by remember { mutableStateOf(loginEmail) }
        var resetPhone by remember { mutableStateOf("") }
        var emailResetSent by remember { mutableStateOf(false) }

        var phoneOtpSent by remember { mutableStateOf(false) }
        var phoneOtpInput by remember { mutableStateOf("") }
        var phoneVerificationId by remember { mutableStateOf("") }
        var phoneResetUser by remember { mutableStateOf<User?>(null) }
        var phoneNewPassword by remember { mutableStateOf("") }
        var phoneNewPasswordConfirm by remember { mutableStateOf("") }
        var phoneResetSuccess by remember { mutableStateOf(false) }
        var phoneResetError by remember { mutableStateOf<String?>(null) }
        var isResetSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isResetSubmitting) showForgotPasswordDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = BirbankRed)
                    Text(localizedString(StringKey.AUTH_FORGOT_PASSWORD_TITLE), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Mode selector: Email vs SMS OTP
                    if (!emailResetSent && !phoneResetSuccess) {
                        TabRow(
                            selectedTabIndex = resetMode,
                            containerColor = BirbankRedLight,
                            contentColor = BirbankRed,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        ) {
                            Tab(
                                selected = resetMode == 0,
                                onClick = { resetMode = 0; phoneResetError = null },
                                text = { Text("E-mail ilə", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                            )
                            Tab(
                                selected = resetMode == 1,
                                onClick = { resetMode = 1; phoneResetError = null },
                                text = { Text("SMS ilə kod göndər", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                            )
                        }
                    }

                    if (phoneResetError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = phoneResetError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    if (resetMode == 0) {
                        // --- EMAIL RESET ---
                        if (emailResetSent) {
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("✓ Təlimat Göndərildi", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Şifrə sıfırlama təlimatı $resetEmail ünvanına göndərildi. E-poçt qutunuzu yoxlayın.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BirbankTextPrimary
                                    )
                                }
                            }
                        } else {
                            Text("Qeydiyyatdan keçdiyiniz e-mail ünvanını daxil edin:", style = MaterialTheme.typography.bodySmall, color = BirbankTextSecondary)
                            OutlinedTextField(
                                value = resetEmail,
                                onValueChange = { resetEmail = it; phoneResetError = null },
                                label = { Text("E-mail") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // --- REAL SMS OTP RESET ---
                        if (phoneResetSuccess) {
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("✓ Şifrə Yeniləndi!", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Şifrəniz uğurla yeniləndi! İndi yeni şifrənizlə daxil ola bilərsiniz.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BirbankTextPrimary
                                    )
                                }
                            }
                        } else if (!phoneOtpSent) {
                            Text(
                                "Qeydiyyatdan keçdiyiniz mobil nömrənizi daxil edin. Mobil telefonunuza həqiqi SMS OTP kodu göndəriləcək:",
                                style = MaterialTheme.typography.bodySmall,
                                color = BirbankTextSecondary
                            )
                            OutlinedTextField(
                                value = resetPhone,
                                onValueChange = { resetPhone = it; phoneResetError = null },
                                label = { Text("Mobil nömrə") },
                                placeholder = { Text("+994 (50) 123-45-67") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BirbankRed) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // OTP is sent via SMS to phone - enter code and new password
                            Text(
                                "6 rəqəmli OTP təsdiq kodu ${phoneResetUser?.phone?.ifBlank { resetPhone }} nömrəsinə SMS vasitəsilə göndərildi. Zəhmət olmasa daxil edin:",
                                style = MaterialTheme.typography.bodySmall,
                                color = BirbankTextSecondary
                            )
                            OutlinedTextField(
                                value = phoneOtpInput,
                                onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) phoneOtpInput = it },
                                label = { Text("6 rəqəmli OTP kodu") },
                                placeholder = { Text("SMS kodunu daxil edin") },
                                leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null, tint = BirbankRed) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phoneNewPassword,
                                onValueChange = { phoneNewPassword = it },
                                label = { Text("Yeni şifrə (min 6 simvol)") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phoneNewPasswordConfirm,
                                onValueChange = { phoneNewPasswordConfirm = it },
                                label = { Text("Yeni şifrənin təkrarı") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetMode == 0) {
                            if (!emailResetSent) {
                                if (resetEmail.isNotBlank()) {
                                    isResetSubmitting = true
                                    viewModel.sendPasswordReset(
                                        email = resetEmail.trim(),
                                        onSuccess = { emailResetSent = true; isResetSubmitting = false },
                                        onError = { err -> phoneResetError = err; isResetSubmitting = false }
                                    )
                                }
                            } else {
                                showForgotPasswordDialog = false
                            }
                        } else {
                            if (phoneResetSuccess) {
                                showForgotPasswordDialog = false
                                if (phoneResetUser != null) {
                                    loginEmail = phoneResetUser?.email?.ifBlank { phoneResetUser?.phone } ?: resetPhone
                                }
                                isLogin = true
                            } else if (!phoneOtpSent) {
                                val rawInput = resetPhone.trim()
                                val digits = rawInput.filter { it.isDigit() }
                                if (digits.length < 7) {
                                    phoneResetError = "Mobil nömrənizi düzgün daxil edin (məsələn: +994501234567)."
                                    return@Button
                                }
                                val activity = context.findActivity()
                                if (activity == null) {
                                    phoneResetError = "Tətbiq aktiv pəncərəsi tapılmadı."
                                    return@Button
                                }
                                isResetSubmitting = true
                                phoneResetError = null
                                viewModel.requestPhonePasswordResetSms(
                                    activity = activity,
                                    phoneOrEmail = rawInput,
                                    onCodeSent = { vId, foundUser ->
                                        phoneVerificationId = vId
                                        phoneResetUser = foundUser
                                        phoneOtpSent = true
                                        phoneOtpInput = ""
                                        isResetSubmitting = false
                                    },
                                    onError = { err ->
                                        phoneResetError = err
                                        isResetSubmitting = false
                                    }
                                )
                            } else {
                                if (phoneOtpInput.length < 6) {
                                    phoneResetError = "6 rəqəmli SMS OTP kodunu daxil edin."
                                    return@Button
                                }
                                if (phoneNewPassword.length < 6) {
                                    phoneResetError = "Yeni şifrə ən azı 6 simvoldan ibarət olmalıdır."
                                    return@Button
                                }
                                if (phoneNewPassword != phoneNewPasswordConfirm) {
                                    phoneResetError = "Daxil edilən yeni şifrələr bir-biri ilə eyni deyil."
                                    return@Button
                                }
                                isResetSubmitting = true
                                phoneResetError = null
                                viewModel.completePhonePasswordReset(
                                    verificationId = phoneVerificationId,
                                    enteredOtp = phoneOtpInput.trim(),
                                    userId = phoneResetUser?.id ?: "",
                                    newPass = phoneNewPassword,
                                    onSuccess = {
                                        phoneResetSuccess = true
                                        isResetSubmitting = false
                                        loginPassword = phoneNewPassword
                                    },
                                    onError = { err ->
                                        phoneResetError = err
                                        isResetSubmitting = false
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isResetSubmitting
                ) {
                    if (isResetSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        val btnText = when {
                            resetMode == 0 && emailResetSent -> "Bağla"
                            resetMode == 0 -> "E-mail ilə bərpa et"
                            phoneResetSuccess -> "Girişə qayıt"
                            !phoneOtpSent -> "SMS ilə kod göndər"
                            else -> "Şifrəni yenilə"
                        }
                        Text(btnText, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!emailResetSent && !phoneResetSuccess) {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text(localizedString(StringKey.BTN_CANCEL), color = BirbankTextSecondary)
                    }
                }
            }
        )
    }

    // Real Phone Verification (OTP) Modal for Registration
    if (showOtpDialog) {
        val rawPhone = regPhone.trim().replace(" ", "").replace("-", "")
        val cleanPhone = if (rawPhone.startsWith("+")) rawPhone else "+$rawPhone"
        AlertDialog(
            onDismissRequest = { /* Modal: require completion or explicit cancel */ },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = BirbankRed)
                    Text("Telefon nömrəsinin təsdiqi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "6 rəqəmli OTP təsdiq kodu $cleanPhone nömrəsinə SMS vasitəsilə göndərildi. Zəhmət olmasa daxil edin:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (otpError != null) {
                        Text(
                            text = otpError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                otpInput = it
                                otpError = null
                            }
                        },
                        label = { Text("6 rəqəmli OTP kodu") },
                        placeholder = { Text("SMS kodunu daxil edin") },
                        leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null, tint = BirbankRed) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("auth_otp_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (otpTimerSeconds > 0) "Yenidən göndər ($otpTimerSeconds saniyə)" else "Kodu yenidən ala bilərsiniz",
                            style = MaterialTheme.typography.labelSmall,
                            color = BirbankTextSecondary
                        )
                        TextButton(
                            onClick = {
                                if (otpTimerSeconds == 0) {
                                    startRegisterSmsVerification()
                                }
                            },
                            enabled = otpTimerSeconds == 0 && !isSendingSms
                        ) {
                            Text(
                                if (isSendingSms) "Göndərilir..." else "Yenidən göndər",
                                color = if (otpTimerSeconds == 0) BirbankRed else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (otpInput.length < 6) {
                            otpError = "6 rəqəmli SMS OTP kodunu tam daxil edin."
                            return@Button
                        }
                        if (otpAttempts >= 5) {
                            otpError = "Çox sayda yanlış cəhd edildi. Zəhmət olmasa yenidən kod tələb edin."
                            return@Button
                        }

                        isVerifyingOtp = true
                        otpError = null
                        viewModel.verifyPhoneSmsOtp(
                            verificationId = phoneVerificationId,
                            smsCode = otpInput.trim(),
                            onSuccess = {
                                isVerifyingOtp = false
                                showOtpDialog = false
                                performRegistration(phoneVerified = true)
                            },
                            onError = { err ->
                                isVerifyingOtp = false
                                otpAttempts += 1
                                otpError = "$err Qalan cəhdlər: ${5 - otpAttempts}"
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                    enabled = !isVerifyingOtp,
                    modifier = Modifier.testTag("auth_otp_verify_btn")
                ) {
                    if (isVerifyingOtp) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Təsdiq et və Davam et")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("Ləğv et", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }

    val is2FaVerificationActive = viewModel.is2FaVerificationActive.value
    val pending2FaUser = viewModel.pending2FaUser.value
    val pending2FaMethod = viewModel.pending2FaMethod.value

    if (is2FaVerificationActive && pending2FaUser != null) {
        val destinationMasked = if (pending2FaMethod == TwoFactorMethod.SMS) {
            val phone = pending2FaUser.phone.ifBlank { "+994500000000" }
            if (phone.length > 6) {
                "${phone.take(4)}***${phone.takeLast(2)}"
            } else phone
        } else {
            val email = pending2FaUser.email
            val parts = email.split("@")
            if (parts.size == 2 && parts[0].length > 2) {
                "${parts[0].take(2)}***@${parts[1]}"
            } else email
        }

        TwoFactorVerificationDialog(
            method = pending2FaMethod,
            destinationMasked = destinationMasked,
            onDismiss = {
                viewModel.cancel2FaVerification()
            },
            onVerify = { code, onError ->
                viewModel.verify2FaCode(
                    code = code,
                    onSuccess = {
                        onAuthSuccess()
                    },
                    onError = { err ->
                        onError(err)
                    }
                )
            },
            onResend = {
                val act = context.findActivity()
                viewModel.resend2FaCode(
                    activity = act,
                    onError = { /* handled */ }
                )
            }
        )
    }
}
