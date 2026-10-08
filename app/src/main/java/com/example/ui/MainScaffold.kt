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
                    AdminDashboardScreen(viewModel = viewModel)
                }

                AppDestination.MAIN -> {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 6.dp,
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .testTag("main_bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == BottomTab.HOME,
                                    onClick = { viewModel.selectBottomTab(BottomTab.HOME) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == BottomTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = localizedString(StringKey.NAV_HOME)
                                        )
                                    },
                                    label = { Text(localizedString(StringKey.NAV_HOME), fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Navy900,
                                        indicatorColor = Gold500,
                                        selectedTextColor = Gold500
                                    ),
                                    modifier = Modifier.testTag("nav_item_home")
                                )

                                NavigationBarItem(
                                    selected = selectedTab == BottomTab.ORDERS,
                                    onClick = { viewModel.selectBottomTab(BottomTab.ORDERS) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == BottomTab.ORDERS) Icons.Filled.Assignment else Icons.Outlined.Assignment,
                                            contentDescription = localizedString(StringKey.NAV_ORDERS)
                                        )
                                    },
                                    label = { Text(localizedString(StringKey.NAV_ORDERS), fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Navy900,
                                        indicatorColor = Gold500,
                                        selectedTextColor = Gold500
                                    ),
                                    modifier = Modifier.testTag("nav_item_orders")
                                )

                                NavigationBarItem(
                                    selected = selectedTab == BottomTab.CHAT,
                                    onClick = { viewModel.selectBottomTab(BottomTab.CHAT) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == BottomTab.CHAT) Icons.Filled.Chat else Icons.Outlined.Chat,
                                            contentDescription = localizedString(StringKey.NAV_CHAT)
                                        )
                                    },
                                    label = { Text(localizedString(StringKey.NAV_CHAT), fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Navy900,
                                        indicatorColor = Gold500,
                                        selectedTextColor = Gold500
                                    ),
                                    modifier = Modifier.testTag("nav_item_chat")
                                )

                                NavigationBarItem(
                                    selected = selectedTab == BottomTab.NOTIFICATIONS,
                                    onClick = { viewModel.selectBottomTab(BottomTab.NOTIFICATIONS) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (unreadNotifsCount > 0) {
                                                    Badge(containerColor = Gold500, contentColor = Navy900) {
                                                        Text("$unreadNotifsCount")
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selectedTab == BottomTab.NOTIFICATIONS) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                                contentDescription = localizedString(StringKey.NAV_NOTIFICATIONS)
                                            )
                                        }
                                    },
                                    label = { Text(localizedString(StringKey.NAV_NOTIFICATIONS), fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Navy900,
                                        indicatorColor = Gold500,
                                        selectedTextColor = Gold500
                                    ),
                                    modifier = Modifier.testTag("nav_item_notifications")
                                )

                                NavigationBarItem(
                                    selected = selectedTab == BottomTab.PROFILE,
                                    onClick = { viewModel.selectBottomTab(BottomTab.PROFILE) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == BottomTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                            contentDescription = localizedString(StringKey.NAV_PROFILE)
                                        )
                                    },
                                    label = { Text(localizedString(StringKey.NAV_PROFILE), fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Navy900,
                                        indicatorColor = Gold500,
                                        selectedTextColor = Gold500
                                    ),
                                    modifier = Modifier.testTag("nav_item_profile")
                                )
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
                                BottomTab.ORDERS -> OrdersScreen(viewModel = viewModel)
                                BottomTab.CHAT -> ChatScreen(viewModel = viewModel)
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
