package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.ServiceType
import com.example.model.UserRole
import com.example.ui.components.BestGroupLogoBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.viewmodel.AppDestination
import com.example.viewmodel.BottomTab
import com.example.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val catalogServices by viewModel.catalogServices.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val activeOrders = remember(orders) {
        orders.filter { it.status == OrderStatus.IN_PROGRESS || it.status == OrderStatus.ACCEPTED || it.status == OrderStatus.PENDING }
    }

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_lazy_column"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Top Header & Greeting
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Navy900, Navy800)
                        )
                    )
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BestGroupLogoBadge(size = 44)
                        Column {
                            Text(
                                text = "BestGroup.az",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = localizedString(StringKey.APP_SLOGAN),
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold400,
                                maxLines = 1
                            )
                        }
                    }

                    // Role indicator badge (authoritative from Firebase account, non-switchable)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("home_role_badge"),
                        color = if (user?.role == UserRole.ADMIN) Gold500 else Navy700
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (user?.role == UserRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (user?.role == UserRole.ADMIN) Navy900 else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = user?.role?.title ?: "Müştəri",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (user?.role == UserRole.ADMIN) Navy900 else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Personalized Greeting
                Text(
                    text = "${localizedString(StringKey.HOME_GREETING_PREFIX)}, ${user?.fullName?.split(" ")?.firstOrNull() ?: "İstifadəçi"} 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = localizedString(StringKey.HOME_SUBTITLE),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Email verification notice banner if not verified
                val showVerifyBanner by viewModel.showEmailVerificationBanner.collectAsState()
                if (showVerifyBanner) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Navy700),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gold500),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.MarkEmailUnread, contentDescription = null, tint = Gold500)
                                Text(
                                    text = localizedString(StringKey.AUTH_VERIFY_EMAIL_TITLE),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hesabınızın təhlükəsizliyi üçün göndərilmiş e-mail təsdiq linkinə keçid edin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resendVerificationEmail({}, {}) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = localizedString(StringKey.AUTH_RESEND_CODE),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Gold500
                                    )
                                }
                                Button(
                                    onClick = { viewModel.checkEmailVerificationStatus() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Yoxla",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Big Primary Hero Button: “Yeni sifariş”
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.startNewOrder() }
                        .border(
                            1.5.dp,
                            Brush.horizontalGradient(listOf(Gold500, Color(0xFFF3E5AB), Gold500)),
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("home_hero_new_order_button"),
                    colors = CardDefaults.cardColors(containerColor = Navy700)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Gold500),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Navy900,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = localizedString(StringKey.BTN_NEW_ORDER),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "8 addımlı rahat sifariş sistemi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gold400
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Gold500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // If user is Staff (Admin, Manager, Operator), show direct access button to Admin Panel
                if (user?.role == UserRole.ADMIN || user?.role == UserRole.MANAGER || user?.role == UserRole.OPERATOR) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.navigateTo(AppDestination.ADMIN_DASHBOARD) }
                            .border(1.dp, Gold500, RoundedCornerShape(14.dp))
                            .testTag("home_admin_panel_shortcut"),
                        colors = CardDefaults.cardColors(containerColor = Navy800)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Gold500,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Admin İdarəetmə Paneli",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Gold500
                                    )
                                    Text(
                                        text = "Sifarişlər, müştərilər, çat və statistikalar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Gold500)
                        }
                    }
                }
            }
        }

        // 2. Active Orders Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedString(StringKey.HOME_ACTIVE_ORDERS_TITLE),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = { viewModel.selectBottomTab(BottomTab.ORDERS) },
                        modifier = Modifier.testTag("home_view_all_orders_button")
                    ) {
                        Text(
                            text = localizedString(StringKey.BTN_VIEW_ALL),
                            style = MaterialTheme.typography.labelMedium,
                            color = Gold500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeOrders.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Gold500
                            )
                            Text(
                                text = localizedString(StringKey.EMPTY_ORDERS_TITLE),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    activeOrders.take(2).forEach { order ->
                        HomeActiveOrderCard(
                            order = order,
                            onClick = { viewModel.viewOrderDetail(order) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // 3. Service Categories Grid/Carousel
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedString(StringKey.HOME_SERVICES_TITLE),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = { viewModel.navigateTo(AppDestination.ORDER_WIZARD) }
                    ) {
                        Text(
                            text = localizedString(StringKey.BTN_VIEW_ALL),
                            style = MaterialTheme.typography.labelMedium,
                            color = Gold500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(catalogServices.take(8), key = { it.serviceId }) { cat ->
                        ServiceMiniCard(
                            item = cat,
                            language = currentLanguage,
                            onClick = { viewModel.startNewOrder(preselectedCatalogService = cat) }
                        )
                    }
                }
            }
        }

        // 4. Featured Campaign / Announcement Banner
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Navy800, TechBlue600))
                        )
                        .border(
                            1.dp,
                            Gold500.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Gold500),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = Navy900,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Payız Akademik Kampaniyası",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Magistr və doktorantura işlərinə 15% endirim və rəsmi Turnitin anti-plagiat hesabatı hədiyyə!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // 5. BestGroup Trust & Quality Guarantees
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = localizedString(StringKey.HOME_GUARANTEE_TITLE),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TrustBadgeItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "100% Orijinallıq",
                        subtitle = "Turnitin hesabatı",
                        modifier = Modifier.weight(1f)
                    )
                    TrustBadgeItem(
                        icon = Icons.Default.Lock,
                        title = "Tam Məxfilik",
                        subtitle = "Şəxsi məlumatlar qorunur",
                        modifier = Modifier.weight(1f)
                    )
                    TrustBadgeItem(
                        icon = Icons.Default.SupportAgent,
                        title = "24/7 Dəstək",
                        subtitle = "Canlı kurator əlaqəsi",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeActiveOrderCard(
    order: Order,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderNumber,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold500
                )
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = order.topic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "İcra faizi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${order.progressPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { order.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Gold500,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = order.deadline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PriorityBadge(priority = order.priority)
            }
        }
    }
}

@Composable
private fun ServiceMiniCard(
    item: com.example.model.ServiceCatalogItem,
    language: com.example.localization.Language,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .testTag("service_mini_card_${item.serviceId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Navy800),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.icon.lowercase()) {
                        "school" -> Icons.Default.School
                        "psychology" -> Icons.Default.Psychology
                        "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
                        "work" -> Icons.Default.Work
                        "assignment" -> Icons.Default.Assignment
                        "description" -> Icons.Default.Description
                        "edit_note" -> Icons.Default.EditNote
                        "slideshow" -> Icons.Default.Slideshow
                        "article" -> Icons.Default.Article
                        "analytics" -> Icons.Default.Analytics
                        "spellcheck" -> Icons.Default.Spellcheck
                        "format_shapes" -> Icons.Default.FormatShapes
                        else -> Icons.Default.MoreHoriz
                    },
                    contentDescription = null,
                    tint = Gold500,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.getLocalizedName(language),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.getFormattedPrice(language),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Gold500,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TrustBadgeItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Gold500,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
