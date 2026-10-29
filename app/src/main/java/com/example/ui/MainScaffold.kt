package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.ui.screens.*
import com.example.ui.theme.BestGroupTheme
import com.example.ui.theme.BirbankBorder
import com.example.ui.theme.BirbankRed
import com.example.ui.theme.BirbankRedLight
import com.example.ui.theme.BirbankTextPrimary
import com.example.ui.theme.BirbankTextSecondary
import com.example.viewmodel.AppDestination
import com.example.viewmodel.BottomTab
import com.example.viewmodel.MainViewModel

@Composable
fun MainApp(viewModel: MainViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val destination by viewModel.currentDestination.collectAsState()
    val selectedTab by viewModel.selectedBottomTab.collectAsState()
    val selectedOrder by viewModel.selectedOrder.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotifsCount = remember(notifications) { notifications.count { !it.isRead } }
    val currentUser by viewModel.currentUser.collectAsState()
    val isUserLoggedIn = currentUser != null || viewModel.repository.isUserLoggedIn()

    BestGroupTheme(themeMode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (destination != AppDestination.SPLASH &&
                destination != AppDestination.AUTH &&
                destination != AppDestination.ONBOARDING &&
                !isUserLoggedIn
            ) {
                // Strict Protected Route Enforcement: redirect to AuthScreen
                AuthScreen(
                    viewModel = viewModel,
                    onAuthSuccess = { viewModel.navigateTo(AppDestination.MAIN) }
                )
            } else {
                when (destination) {
                    AppDestination.SPLASH -> {
                        SplashScreen()
                    }

                AppDestination.ONBOARDING -> {
                    OnboardingScreen(
                        onFinish = { viewModel.completeOnboarding() }
                    )
                }

                AppDestination.AUTH -> {
                    AuthScreen(
                        viewModel = viewModel,
                        onAuthSuccess = { viewModel.navigateTo(AppDestination.MAIN) }
                    )
                }

                AppDestination.ORDER_WIZARD -> {
                    BackHandler {
                        viewModel.navigateTo(AppDestination.MAIN)
                    }
                    OrderWizardScreen(viewModel = viewModel)
                }

                AppDestination.ORDER_DETAIL -> {
                    BackHandler {
                        viewModel.navigateTo(AppDestination.MAIN)
                    }
                    selectedOrder?.let { order ->
                        OrderDetailScreen(order = order, viewModel = viewModel)
                    } ?: run {
                        viewModel.navigateTo(AppDestination.MAIN)
                    }
                }

                AppDestination.SUPPORT_CENTER -> {
                    BackHandler {
                        viewModel.navigateTo(AppDestination.MAIN)
                    }
                    SupportScreen(viewModel = viewModel)
                }

                AppDestination.ADMIN_DASHBOARD -> {
                    BackHandler {
                        viewModel.navigateTo(AppDestination.MAIN)
                    }
                    val isStaff = currentUser?.role in listOf(
                        com.example.model.UserRole.ADMIN,
                        com.example.model.UserRole.MANAGER,
                        com.example.model.UserRole.OPERATOR
                    ) || com.example.services.firebase.FirebaseAuthService.isSuperAdminEmail(currentUser?.email)
                    if (isStaff) {
                        AdminDashboardScreen(viewModel = viewModel)
                    } else {
                        // Customer cannot view or open Admin Dashboard: redirect to Home
                        LaunchedEffect(Unit) {
                            viewModel.navigateTo(AppDestination.MAIN)
                        }
                    }
                }

                AppDestination.MAIN -> {
                    Scaffold(
                        bottomBar = {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                            ) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.testTag("main_bottom_nav_bar")
                                ) {
                                    // 1. Ana səhifə
                                    NavigationBarItem(
                                        selected = selectedTab == BottomTab.HOME,
                                        onClick = { viewModel.selectBottomTab(BottomTab.HOME) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selectedTab == BottomTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                                contentDescription = localizedString(StringKey.NAV_HOME)
                                            )
                                        },
                                        label = { Text(localizedString(StringKey.NAV_HOME), fontSize = 11.sp, fontWeight = if (selectedTab == BottomTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = com.example.ui.theme.BirbankRed,
                                            selectedTextColor = com.example.ui.theme.BirbankRed,
                                            indicatorColor = com.example.ui.theme.BirbankRedLight,
                                            unselectedIconColor = com.example.ui.theme.BirbankTextSecondary,
                                            unselectedTextColor = com.example.ui.theme.BirbankTextSecondary
                                        ),
                                        modifier = Modifier.testTag("nav_item_home")
                                    )

                                    // 2. Xidmətlər
                                    NavigationBarItem(
                                        selected = selectedTab == BottomTab.SERVICES,
                                        onClick = { viewModel.selectBottomTab(BottomTab.SERVICES) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selectedTab == BottomTab.SERVICES) Icons.Filled.GridView else Icons.Outlined.GridView,
                                                contentDescription = localizedString(StringKey.NAV_SERVICES)
                                            )
                                        },
                                        label = { Text(localizedString(StringKey.NAV_SERVICES), fontSize = 11.sp, fontWeight = if (selectedTab == BottomTab.SERVICES) FontWeight.Bold else FontWeight.Normal) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = com.example.ui.theme.BirbankRed,
                                            selectedTextColor = com.example.ui.theme.BirbankRed,
                                            indicatorColor = com.example.ui.theme.BirbankRedLight,
                                            unselectedIconColor = com.example.ui.theme.BirbankTextSecondary,
                                            unselectedTextColor = com.example.ui.theme.BirbankTextSecondary
                                        ),
                                        modifier = Modifier.testTag("nav_item_services")
                                    )

                                    // 3. Sifarişlərim
                                    NavigationBarItem(
                                        selected = selectedTab == BottomTab.ORDERS,
                                        onClick = { viewModel.selectBottomTab(BottomTab.ORDERS) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selectedTab == BottomTab.ORDERS) Icons.Filled.Assignment else Icons.Outlined.Assignment,
                                                contentDescription = localizedString(StringKey.NAV_ORDERS)
                                            )
                                        },
                                        label = { Text(localizedString(StringKey.NAV_ORDERS), fontSize = 11.sp, fontWeight = if (selectedTab == BottomTab.ORDERS) FontWeight.Bold else FontWeight.Normal) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = com.example.ui.theme.BirbankRed,
                                            selectedTextColor = com.example.ui.theme.BirbankRed,
                                            indicatorColor = com.example.ui.theme.BirbankRedLight,
                                            unselectedIconColor = com.example.ui.theme.BirbankTextSecondary,
                                            unselectedTextColor = com.example.ui.theme.BirbankTextSecondary
                                        ),
                                        modifier = Modifier.testTag("nav_item_orders")
                                    )

                                    // 4. Mesajlar
                                    NavigationBarItem(
                                        selected = selectedTab == BottomTab.CHAT,
                                        onClick = { viewModel.selectBottomTab(BottomTab.CHAT) },
                                        icon = {
                                            val conversations by viewModel.conversations.collectAsState()
                                            val unreadChatCount = remember(conversations) { conversations.sumOf { it.unreadCount } }
                                            BadgedBox(
                                                badge = {
                                                    if (unreadChatCount > 0) {
                                                        Badge(containerColor = com.example.ui.theme.BirbankRed, contentColor = Color.White) {
                                                            Text("$unreadChatCount")
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (selectedTab == BottomTab.CHAT) Icons.Filled.Chat else Icons.Outlined.Chat,
                                                    contentDescription = localizedString(StringKey.NAV_CHAT)
                                                )
                                            }
                                        },
                                        label = { Text(localizedString(StringKey.NAV_CHAT), fontSize = 11.sp, fontWeight = if (selectedTab == BottomTab.CHAT) FontWeight.Bold else FontWeight.Normal) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = com.example.ui.theme.BirbankRed,
                                            selectedTextColor = com.example.ui.theme.BirbankRed,
                                            indicatorColor = com.example.ui.theme.BirbankRedLight,
                                            unselectedIconColor = com.example.ui.theme.BirbankTextSecondary,
                                            unselectedTextColor = com.example.ui.theme.BirbankTextSecondary
                                        ),
                                        modifier = Modifier.testTag("nav_item_chat")
                                    )

                                    // 5. Profil
                                    NavigationBarItem(
                                        selected = selectedTab == BottomTab.PROFILE,
                                        onClick = { viewModel.selectBottomTab(BottomTab.PROFILE) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selectedTab == BottomTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                                contentDescription = localizedString(StringKey.NAV_PROFILE)
                                            )
                                        },
                                        label = { Text(localizedString(StringKey.NAV_PROFILE), fontSize = 11.sp, fontWeight = if (selectedTab == BottomTab.PROFILE) FontWeight.Bold else FontWeight.Normal) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = com.example.ui.theme.BirbankRed,
                                            selectedTextColor = com.example.ui.theme.BirbankRed,
                                            indicatorColor = com.example.ui.theme.BirbankRedLight,
                                            unselectedIconColor = com.example.ui.theme.BirbankTextSecondary,
                                            unselectedTextColor = com.example.ui.theme.BirbankTextSecondary
                                        ),
                                        modifier = Modifier.testTag("nav_item_profile")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (selectedTab) {
                                BottomTab.HOME -> HomeScreen(viewModel = viewModel)
                                BottomTab.SERVICES -> ServicesScreen(viewModel = viewModel)
                                BottomTab.ORDERS -> OrdersScreen(viewModel = viewModel)
                                BottomTab.CHAT -> {
                                    val isStaffUser = currentUser?.role in listOf(
                                        com.example.model.UserRole.ADMIN,
                                        com.example.model.UserRole.MANAGER,
                                        com.example.model.UserRole.OPERATOR
                                    ) || com.example.services.firebase.FirebaseAuthService.isSuperAdminEmail(currentUser?.email)
                                    if (isStaffUser) {
                                        AdminChatScreen(viewModel = viewModel)
                                    } else {
                                        ChatScreen(viewModel = viewModel)
                                    }
                                }
                                BottomTab.NOTIFICATIONS -> NotificationsScreen(viewModel = viewModel)
                                BottomTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }

        // Birbank-style App Update Dialog (In-Place Auto Updater)
        val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsState()
        val latestAppUpdate by viewModel.latestAppUpdate.collectAsState()
        val isMandatoryUpdate by viewModel.isMandatoryUpdate.collectAsState()
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val downloadProgress by com.example.services.update.InAppUpdateInstaller.downloadState.collectAsState()

        if (isUpdateAvailable && latestAppUpdate != null) {
            val update = latestAppUpdate!!
            AlertDialog(
                onDismissRequest = {
                    if (!isMandatoryUpdate && !downloadProgress.isDownloading) {
                        viewModel.dismissUpdate()
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(BirbankRedLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = "Yeniləmə",
                            tint = BirbankRed,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Yeni versiya hazırdır",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BirbankTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "v${update.versionName} • Build ${update.versionCode}",
                            style = MaterialTheme.typography.labelMedium,
                            color = BirbankRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val notes = update.releaseNotesAz.ifBlank {
                            "BestGroup.az tətbiqinin yeni versiyası daha sürətli işləmə, dizayn təkmilləşdirmələri və yeni imkanlarla təqdim olunur."
                        }
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BirbankTextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        if (update.apkSizeMb > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Həcm: ${String.format("%.1f", update.apkSizeMb)} MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = BirbankTextSecondary
                            )
                        }

                        // Seamless upgrade badge
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mövcud tətbiqin üzərinə yenilənir. Silməyə ehtiyac yoxdur.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Download progress display
                        if (downloadProgress.isDownloading) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = BirbankRed,
                                trackColor = BirbankRedLight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Yüklənir: ${(downloadProgress.progress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BirbankRed
                                )
                                val downloadedMb = downloadProgress.downloadedBytes / (1024.0 * 1024.0)
                                val totalMb = if (downloadProgress.totalBytes > 0) {
                                    downloadProgress.totalBytes / (1024.0 * 1024.0)
                                } else {
                                    update.apkSizeMb
                                }
                                Text(
                                    text = "${String.format("%.1f", downloadedMb)} / ${String.format("%.1f", totalMb)} MB",
                                    fontSize = 11.sp,
                                    color = BirbankTextSecondary
                                )
                            }
                        } else if (downloadProgress.isCompleted && downloadProgress.downloadedFile != null) {
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Paket hazırdır! Aşağıdakı düyməyə toxunaraq quraşdırın.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1D4ED8),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else if (downloadProgress.errorMessage != null) {
                            Surface(
                                color = BirbankRedLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Yükləmə xətası: ${downloadProgress.errorMessage}",
                                    fontSize = 11.sp,
                                    color = BirbankRed,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        if (isMandatoryUpdate) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = BirbankRedLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Bu yeniləmə tətbiqdən istifadə üçün vacibdir.",
                                    color = BirbankRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when {
                            downloadProgress.isDownloading -> {
                                Button(
                                    onClick = { /* In progress */ },
                                    enabled = false,
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = BirbankRedLight,
                                        disabledContentColor = BirbankRed
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = BirbankRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Yüklənir...", fontWeight = FontWeight.Bold)
                                }
                            }
                            downloadProgress.isCompleted && downloadProgress.downloadedFile != null -> {
                                Button(
                                    onClick = {
                                        val file = downloadProgress.downloadedFile!!
                                        if (!com.example.services.update.InAppUpdateInstaller.canInstallPackages(context)) {
                                            com.example.services.update.InAppUpdateInstaller.requestInstallPermission(context)
                                        } else {
                                            com.example.services.update.InAppUpdateInstaller.installApk(context, file)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BirbankRed,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("İndi quraşdır", fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> {
                                Button(
                                    onClick = {
                                        val directUrl = update.downloadUrl.ifBlank { update.githubReleaseUrl }
                                        coroutineScope.launch {
                                            val result = com.example.services.update.InAppUpdateInstaller.downloadApk(
                                                context = context,
                                                rawUrl = directUrl
                                            )
                                            result.onSuccess { apkFile ->
                                                if (com.example.services.update.InAppUpdateInstaller.canInstallPackages(context)) {
                                                    com.example.services.update.InAppUpdateInstaller.installApk(context, apkFile)
                                                } else {
                                                    com.example.services.update.InAppUpdateInstaller.requestInstallPermission(context)
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BirbankRed,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Birbaşa yenilə", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                dismissButton = if (!isMandatoryUpdate && !downloadProgress.isDownloading) {
                    {
                        TextButton(
                            onClick = { viewModel.dismissUpdate() },
                            colors = ButtonDefaults.textButtonColors(contentColor = BirbankTextSecondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Daha sonra", fontWeight = FontWeight.Medium)
                        }
                    }
                } else null
            )
        }
    }
}
}
