package com.example.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.model.AutoLockTimeout
import com.example.model.TwoFactorMethod
import com.example.model.UserRole
import com.example.services.firebase.FirebaseAuthService
import com.example.ui.theme.BirbankRed
import com.example.ui.theme.BirbankRedLight
import com.example.ui.theme.BirbankTextPrimary
import com.example.ui.theme.BirbankTextSecondary
import com.example.viewmodel.AppDestination
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val currentUser by viewModel.currentUser.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()
    val autoLockTimeout by viewModel.autoLockTimeout.collectAsState()
    val isScreenSecurityEnabled by viewModel.isScreenSecurityEnabled.collectAsState()
    val isTwoFactorEnabled by viewModel.isTwoFactorEnabled.collectAsState()
    val twoFactorMethod by viewModel.twoFactorMethod.collectAsState()
    val activeSessions by viewModel.activeDeviceSessions.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var show2FaMethodDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutAllDialog by remember { mutableStateOf(false) }
    var showRemovePinConfirmDialog by remember { mutableStateOf(false) }

    val isStaff = currentUser?.role in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.OPERATOR) ||
                  FirebaseAuthService.isSuperAdminEmail(currentUser?.email)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Təhlükəsizlik və Giriş",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Security Status Card
            item {
                Surface(
                    color = if (isPinConfigured || isBiometricEnabled) Color(0xFFF0FDF4) else BirbankRedLight,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isPinConfigured || isBiometricEnabled) Color(0xFFDCFCE7) else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPinConfigured || isBiometricEnabled) Icons.Default.Shield else Icons.Default.SecurityUpdateWarning,
                                contentDescription = null,
                                tint = if (isPinConfigured || isBiometricEnabled) Color(0xFF16A34A) else BirbankRed,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPinConfigured || isBiometricEnabled) "Hesabınız Yüksək Səviyyədə Qorunur" else "Təhlükəsizliyi Gücləndirin",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPinConfigured || isBiometricEnabled) Color(0xFF15803D) else BirbankRed
                            )
                            Text(
                                text = if (isPinConfigured || isBiometricEnabled) {
                                    "Biometrika və ya PIN kodu aktivdir. Ekran kilidi tətbiq olunur."
                                } else {
                                    "Cihazınızda 4 rəqəmli PIN kod və ya barmaq izi ilə girişi aktiv edin."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPinConfigured || isBiometricEnabled) Color(0xFF166534) else BirbankTextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Section 1: Biometrika və PIN Kod
            item {
                SecuritySectionCard(title = "BİOMETRİKA VƏ CİHAZ KİLİDİ") {
                    // 1. Barmaq izi ilə giriş
                    SecurityToggleItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Barmaq izi ilə giriş",
                        subtitle = if (viewModel.isBiometricSupported()) {
                            "Tətbiqi barmaq izinizi toxunduraraq açın"
                        } else {
                            "Bu cihazda biometrik sensor dəstəklənmir"
                        },
                        checked = isBiometricEnabled,
                        enabled = viewModel.isBiometricSupported(),
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (activity != null) {
                                    viewModel.authenticateBiometric(
                                        activity = activity,
                                        title = "Barmaq izini aktivləşdir",
                                        subtitle = "Aktivləşdirməni təsdiqləmək üçün barmağınızı toxundurun",
                                        onSuccess = {
                                            viewModel.setBiometricEnabled(true)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Barmaq izi ilə giriş aktivləşdirildi.")
                                            }
                                        },
                                        onError = { err ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(if (err == "PIN_FALLBACK") "Ləğv edildi" else err)
                                            }
                                        }
                                    )
                                }
                            } else {
                                viewModel.setBiometricEnabled(false)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Barmaq izi ilə giriş deaktiv edildi.")
                                }
                            }
                        },
                        tag = "toggle_biometric"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // 2. 4 rəqəmli PIN kod
                    SecurityNavigationItem(
                        icon = Icons.Default.Pin,
                        title = "4 rəqəmli PIN kod",
                        subtitle = if (isPinConfigured) "Aktivdir • Dəyişmək və ya silmək üçün toxunun" else "Təyin edilməyib • Daxil olmaq üçün təyin edin",
                        badge = if (isPinConfigured) "Aktiv" else "Qur",
                        badgeColor = if (isPinConfigured) Color(0xFF16A34A) else BirbankRed,
                        onClick = {
                            if (isPinConfigured) {
                                showRemovePinConfirmDialog = true
                            } else {
                                showPinSetupDialog = true
                            }
                        },
                        tag = "menu_pin_code"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // 3. Avtomatik ekran kilidi
                    SecurityNavigationItem(
                        icon = Icons.Default.LockClock,
                        title = "Avtomatik ekran kilidi",
                        subtitle = "Tətbiq arxa plana keçdikdə kilidlənmə vaxtı",
                        badge = autoLockTimeout.titleAz,
                        badgeColor = BirbankRed,
                        onClick = { showAutoLockDialog = true },
                        tag = "menu_auto_lock"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Ekran məxfiliyi (FLAG_SECURE)
                    SecurityToggleItem(
                        icon = Icons.Default.VisibilityOff,
                        title = "Ekran görüntüsünün qorunması",
                        subtitle = "Son tətbiqlər siyahısında ekran görüntüsünü gizlət",
                        checked = isScreenSecurityEnabled,
                        onCheckedChange = { enable ->
                            viewModel.setScreenSecurityEnabled(enable, activity)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (enable) "Ekran məxfiliyi aktivləşdirildi (FLAG_SECURE)." else "Ekran məxfiliyi deaktiv edildi."
                                )
                            }
                        },
                        tag = "toggle_screen_security"
                    )
                }
            }

            // Section 2: İki Mərhələli Təsdiqləmə (2FA)
            item {
                SecuritySectionCard(title = "İKİ MƏRHƏLƏLİ TƏSDİQLƏMƏ (2FA)") {
                    SecurityToggleItem(
                        icon = Icons.Default.Security,
                        title = "İki mərhələli təsdiqləmə (2FA)",
                        subtitle = if (isStaff) {
                            "Admin və kurator heyəti üçün 2FA məcburidir"
                        } else {
                            "Yeni cihazdan daxil olduqda SMS və ya Email ilə təsdiq tələb et"
                        },
                        checked = if (isStaff) true else isTwoFactorEnabled,
                        enabled = !isStaff,
                        onCheckedChange = { enable ->
                            viewModel.setTwoFactorEnabled(enable)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (enable) "2FA aktivləşdirildi." else "2FA deaktiv edildi."
                                )
                            }
                        },
                        tag = "toggle_2fa"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    SecurityNavigationItem(
                        icon = Icons.Default.Message,
                        title = "2FA Təsdiqləmə üsulu",
                        subtitle = if (twoFactorMethod == TwoFactorMethod.SMS) "Mobil nömrəyə real SMS OTP kodu ilə" else "E-poçt ünvanına təsdiq kodu ilə",
                        badge = twoFactorMethod.name,
                        badgeColor = BirbankRed,
                        onClick = { show2FaMethodDialog = true },
                        tag = "menu_2fa_method"
                    )
                }
            }

            // Section 3: Cihazlar və Giriş Tarixçəsi
            item {
                SecuritySectionCard(title = "CİHAZLAR VƏ SESSİYALAR") {
                    SecurityNavigationItem(
                        icon = Icons.Default.Devices,
                        title = "Aktiv cihazlar",
                        subtitle = "Hesabınıza qoşulmuş sessiyaları yoxlayın",
                        badge = "${activeSessions.size} cihaz",
                        badgeColor = if (activeSessions.size > 1) Color(0xFFEAB308) else Color(0xFF16A34A),
                        onClick = { viewModel.navigateTo(AppDestination.ACTIVE_DEVICES) },
                        tag = "menu_active_devices"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    SecurityNavigationItem(
                        icon = Icons.Default.History,
                        title = "Giriş tarixçəsi",
                        subtitle = "Son autentifikasiya hadisələri və təhlükəsizlik bildirişləri",
                        onClick = { viewModel.navigateTo(AppDestination.LOGIN_HISTORY) },
                        tag = "menu_login_history"
                    )
                }
            }

            // Section 4: Hesab Əməliyyatları
            item {
                SecuritySectionCard(title = "HESAB TƏHLÜKƏSİZLİYİ") {
                    SecurityNavigationItem(
                        icon = Icons.Default.Key,
                        title = "Şifrəni dəyişmək",
                        subtitle = "Mövcud hesab şifrənizi yeniləyin",
                        onClick = { showChangePasswordDialog = true },
                        tag = "menu_change_password"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    SecurityNavigationItem(
                        icon = Icons.Default.PowerSettingsNew,
                        title = "Bütün cihazlardan çıxış",
                        subtitle = "Bütün aktiv sessiyaları etibarsız et və yenidən daxil ol",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showLogoutAllDialog = true },
                        tag = "menu_logout_all"
                    )
                }
            }
        }
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onPinConfigured = { pin ->
                showPinSetupDialog = false
                val success = viewModel.setPin(pin)
                coroutineScope.launch {
                    if (success) {
                        snackbarHostState.showSnackbar("4 rəqəmli PIN kod uğurla aktivləşdirildi.")
                    } else {
                        snackbarHostState.showSnackbar("PIN kod yadda saxlanılmadı.")
                    }
                }
            }
        )
    }

    // Remove PIN Confirmation Dialog
    if (showRemovePinConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePinConfirmDialog = false },
            title = { Text("PIN Kodu İdarəetmə", fontWeight = FontWeight.Bold) },
            text = {
                Text("Mövcud PIN kodunuz aktivdir. PIN kodu dəyişmək, yoxsa tamamilə silmək istəyirsiniz?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemovePinConfirmDialog = false
                        showPinSetupDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White)
                ) {
                    Text("Yeni PIN Təyin Et")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemovePinConfirmDialog = false
                        viewModel.removePin()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("PIN kod ləğv edildi.")
                        }
                    }
                ) {
                    Text("PIN-i Sil", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Auto-Lock Timeout Dialog
    if (showAutoLockDialog) {
        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            title = { Text("Avtomatik Ekran Kilidi", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AutoLockTimeout.values().forEach { timeout ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setAutoLockTimeout(timeout)
                                    showAutoLockDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = timeout.titleAz,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (timeout == autoLockTimeout) FontWeight.Bold else FontWeight.Normal,
                                color = if (timeout == autoLockTimeout) BirbankRed else BirbankTextPrimary
                            )
                            if (timeout == autoLockTimeout) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BirbankRed)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoLockDialog = false }) {
                    Text("Bağla")
                }
            }
        )
    }

    // 2FA Method Selector Dialog
    if (show2FaMethodDialog) {
        AlertDialog(
            onDismissRequest = { show2FaMethodDialog = false },
            title = { Text("2FA Təsdiqləmə Metodu", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TwoFactorMethod.values().forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setTwoFactorMethod(method)
                                    show2FaMethodDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = method.titleAz,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (method == twoFactorMethod) FontWeight.Bold else FontWeight.Normal,
                                    color = if (method == twoFactorMethod) BirbankRed else BirbankTextPrimary
                                )
                                Text(
                                    text = if (method == TwoFactorMethod.SMS) "Firebase Phone Auth vasitəsilə" else "Hesabınızın e-poçt ünvanına",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BirbankTextSecondary
                                )
                            }
                            if (method == twoFactorMethod) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BirbankRed)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { show2FaMethodDialog = false }) {
                    Text("Bağla")
                }
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            viewModel = viewModel,
            onDismiss = { showChangePasswordDialog = false },
            onSuccess = {
                showChangePasswordDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Şifrəniz uğurla yeniləndi.")
                }
            }
        )
    }

    // Logout All Devices Dialog
    if (showLogoutAllDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutAllDialog = false },
            title = { Text("Bütün Cihazlardan Çıxış", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Bütün digər cihazlardakı aktiv sessiyalarınız ləğv ediləcək, saxlanılmış PIN/biometrik girişlər təmizlənəcək və bu cihazdan da çıxış ediləcək. Davam edilsin?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutAllDialog = false
                        viewModel.logoutAllDevices()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Bütün Cihazlardan Çıx", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutAllDialog = false }) {
                    Text("Ləğv et")
                }
            }
        )
    }
}

@Composable
fun SecuritySectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BirbankTextSecondary,
            modifier = Modifier.padding(start = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SecurityNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color = BirbankRed,
    titleColor: Color = BirbankTextPrimary,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(BirbankRedLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (titleColor == MaterialTheme.colorScheme.error) MaterialTheme.colorScheme.error else BirbankRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BirbankTextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (badge != null) {
                Surface(
                    color = badgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = BirbankTextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun SecurityToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(BirbankRedLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BirbankRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = BirbankTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BirbankTextSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BirbankRed,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
