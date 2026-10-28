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
import com.example.localization.LocalizationManager
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.ui.components.BestGroupLogoBadge
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLogin by remember { mutableStateOf(true) }

    // Login tab state - strictly initialized to empty
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    // Register tab state - strictly initialized to empty
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regUniversity by remember { mutableStateOf("") }
    var regFaculty by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }

    // Phone OTP Verification State
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    var otpTimerSeconds by remember { mutableIntStateOf(60) }
    var otpAttempts by remember { mutableIntStateOf(0) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var generatedOtpCode by remember { mutableStateOf("") }
    var otpGeneratedAt by remember { mutableLongStateOf(0L) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    val isLoading by viewModel.isAuthLoading.collectAsState()

    val scrollState = rememberScrollState()

    LaunchedEffect(showOtpDialog, otpGeneratedAt) {
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

    fun validateAndProceed() {
        errorMessage = null
        if (isLogin) {
            val emailClean = loginEmail.trim()
            if (!emailClean.contains("@") || !emailClean.contains(".")) {
                errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_EMAIL_INVALID)
                return
            }
            if (loginPassword.length < 6) {
                errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_PASSWORD_SHORT)
                return
            }
            viewModel.login(
                email = emailClean,
                pass = loginPassword,
                rememberMe = rememberMe,
                onSuccess = onAuthSuccess,
                onError = { err -> errorMessage = err }
            )
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
            val cleanPhone = regPhone.trim().replace(" ", "").replace("-", "")
            if (cleanPhone.isBlank()) {
                errorMessage = LocalizationManager.getString(StringKey.AUTH_ERR_PHONE_REQUIRED)
                return
            }
            // Strict international phone format check (+994XXXXXXXXX or +[country_code][number])
            val phoneRegex = Regex("^\\+[1-9]\\d{8,14}$")
            if (!phoneRegex.matches(cleanPhone)) {
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

            // Launch 6-digit OTP verification dialog
            val code = (100000..999999).random().toString()
            generatedOtpCode = code
            otpGeneratedAt = System.currentTimeMillis()
            otpInput = ""
            otpError = null
            otpAttempts = 0
            showOtpDialog = true
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

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(loginEmail) }
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                                    email = resetEmail.trim(),
                                    onSuccess = { resetSent = true },
                                    onError = { err -> errorMessage = err; showForgotPasswordDialog = false }
                                )
                            }
                        } else {
                            showForgotPasswordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White)
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

    // Phone Verification (OTP) Modal
    if (showOtpDialog) {
        val cleanPhone = regPhone.trim().replace(" ", "").replace("-", "")
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
                        text = "6 rəqəmli OTP təsdiq kodu $cleanPhone nömrəsinə göndərildi.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Information box regarding real SMS vs demo/sandbox environment
                    Surface(
                        color = BirbankRedLight,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🔒 Təhlükəsizlik Sistemi",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = BirbankRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Firebase Console-da SMS kvotası və Phone Auth aktivləşdirilənədək sandbox təhlükəsizlik kodu: $generatedOtpCode",
                                style = MaterialTheme.typography.labelSmall,
                                color = BirbankTextPrimary
                            )
                        }
                    }

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
                        placeholder = { Text("Məs: 123456") },
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
                                    val code = (100000..999999).random().toString()
                                    generatedOtpCode = code
                                    otpGeneratedAt = System.currentTimeMillis()
                                    otpTimerSeconds = 60
                                    otpError = null
                                }
                            },
                            enabled = otpTimerSeconds == 0
                        ) {
                            Text(
                                "Yenidən göndər",
                                color = if (otpTimerSeconds == 0) BirbankRed else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - otpGeneratedAt > 120_000L) {
                            otpError = "OTP kodunun etibarlılıq vaxtı (2 dəq) bitib. Yenidən göndərin."
                            return@Button
                        }
                        if (otpAttempts >= 5) {
                            otpError = "Çox sayda yanlış cəhd edildi. Zəhmət olmasa bir qədər sonra cəhd edin."
                            return@Button
                        }
                        if (otpInput.trim() != generatedOtpCode) {
                            otpAttempts += 1
                            otpError = "Daxil edilən 6 rəqəmli kod yanlışdır. Qalan cəhdlər: ${5 - otpAttempts}"
                            return@Button
                        }

                        // OTP Verified successfully!
                        showOtpDialog = false
                        performRegistration(phoneVerified = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                    modifier = Modifier.testTag("auth_otp_verify_btn")
                ) {
                    Text("Təsdiq et və Davam et")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("Ləğv et", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}
