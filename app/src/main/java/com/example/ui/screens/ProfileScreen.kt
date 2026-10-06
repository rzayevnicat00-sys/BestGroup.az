package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Language
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.UserRole
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.AppDestination
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val currentTheme by viewModel.themeMode.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        com.example.ui.components.BestGroupLogoBadge(size = 32)
                        Text(
                            text = localizedString(StringKey.PROFILE_TITLE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(Navy800)
                                .border(2.dp, Gold500, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Gold500,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = user?.fullName ?: "İstifadəçi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = user?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Role and University Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Navy800
                        ) {
                            Text(
                                text = "${user?.role?.title} • ${user?.university?.take(28)}...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold500,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Navigation into Admin Dashboard for authorized staff only
            if (user?.role == UserRole.ADMIN || user?.role == UserRole.MANAGER) {
                item {
                    Button(
                        onClick = { viewModel.navigateTo(AppDestination.ADMIN_DASHBOARD) },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("open_admin_dashboard_btn")
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(localizedString(StringKey.ADMIN_TITLE), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Menu Items List
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Default.Translate,
                            title = localizedString(StringKey.PROFILE_LANGUAGE),
                            subtitle = "${currentLang.flag} ${currentLang.nativeName}",
                            onClick = { showLanguageDialog = true },
                            tag = "profile_menu_language"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ProfileMenuItem(
                            icon = Icons.Default.DarkMode,
                            title = localizedString(StringKey.PROFILE_THEME),
                            subtitle = when (currentTheme) {
                                ThemeMode.SYSTEM -> localizedString(StringKey.THEME_SYSTEM)
                                ThemeMode.LIGHT -> localizedString(StringKey.THEME_LIGHT)
                                ThemeMode.DARK -> localizedString(StringKey.THEME_DARK)
                            },
                            onClick = { showThemeDialog = true },
                            tag = "profile_menu_theme"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ProfileMenuItem(
                            icon = Icons.Default.HelpOutline,
                            title = localizedString(StringKey.PROFILE_HELP_SUPPORT),
                            subtitle = "FAQ və Dəstək",
                            onClick = { viewModel.navigateTo(AppDestination.SUPPORT_CENTER) },
                            tag = "profile_menu_support"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ProfileMenuItem(
                            icon = Icons.Default.Description,
                            title = localizedString(StringKey.PROFILE_TERMS),
                            onClick = { showTermsDialog = true },
                            tag = "profile_menu_terms"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ProfileMenuItem(
                            icon = Icons.Default.PrivacyTip,
                            title = localizedString(StringKey.PROFILE_PRIVACY),
                            onClick = { showPrivacyDialog = true },
                            tag = "profile_menu_privacy"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ProfileMenuItem(
                            icon = Icons.Default.Logout,
                            title = localizedString(StringKey.BTN_LOGOUT),
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { showLogoutDialog = true },
                            tag = "profile_menu_logout"
                        )
                    }
                }
            }
        }
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(localizedString(StringKey.PROFILE_LANGUAGE), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Language.values().forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(lang.flag, fontSize = 22.sp)
                            Text(
                                text = lang.nativeName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (lang == currentLang) FontWeight.Bold else FontWeight.Normal,
                                color = if (lang == currentLang) Gold500 else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(localizedString(StringKey.BTN_CLOSE))
                }
            }
        )
    }

    // Theme Selector Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(localizedString(StringKey.PROFILE_THEME), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = mode == currentTheme,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> localizedString(StringKey.THEME_SYSTEM)
                                    ThemeMode.LIGHT -> localizedString(StringKey.THEME_LIGHT)
                                    ThemeMode.DARK -> localizedString(StringKey.THEME_DARK)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (mode == currentTheme) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(localizedString(StringKey.BTN_CLOSE))
                }
            }
        )
    }

    // Terms Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text(localizedString(StringKey.PROFILE_TERMS), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "BestGroup.az xidmətlərindən istifadə zamanı bütün akademik və hüquqi etik qaydalar qorunur. Hazırlanan elmi və analitik materiallar istinad və metodiki baza məqsədilə təqdim olunur. Şirkət orijinal məzmun, plagiat yoxlanışı və vaxtında təhvil zəmanəti verir.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500)
                ) {
                    Text(localizedString(StringKey.BTN_CLOSE))
                }
            }
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(localizedString(StringKey.PROFILE_PRIVACY), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Məxfilik siyasətimiz istifadəçilərin şəxsi məlumatlarının, sifariş fayllarının və rabitə kanallarının 100% qorunmasını təmin edir. Məlumatlar heç bir halda üçüncü şəxslərə və ya təşkilatlara ötürülmür.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500)
                ) {
                    Text(localizedString(StringKey.BTN_CLOSE))
                }
            }
        )
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(localizedString(StringKey.BTN_LOGOUT), fontWeight = FontWeight.Bold) },
            text = { Text(localizedString(StringKey.PROFILE_LOGOUT_CONFIRM)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(localizedString(StringKey.BTN_LOGOUT), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(localizedString(StringKey.BTN_CANCEL))
                }
            }
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor != Color.Unspecified) titleColor else Gold500,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}
