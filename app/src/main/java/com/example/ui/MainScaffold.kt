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
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.ui.screens.*
import com.example.ui.theme.BestGroupTheme
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
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
    }
}
}
