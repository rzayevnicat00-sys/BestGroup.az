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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Language
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.ServiceCatalogItem
import com.example.model.ServiceType
import com.example.model.UserRole
import com.example.ui.components.BestGroupLogoBadge
import com.example.ui.components.CreateStoryDialog
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.StoryBar
import com.example.ui.components.StoryViewerDialog
import com.example.ui.theme.*
import com.example.viewmodel.AppDestination
import com.example.viewmodel.BottomTab
import com.example.viewmodel.MainViewModel

private data class CircularServiceCategory(
    val title: String,
    val icon: ImageVector,
    val serviceType: ServiceType? = null
)

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val isStaff = user?.role in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.OPERATOR) ||
                  com.example.services.firebase.FirebaseAuthService.isSuperAdminEmail(user?.email)
    val orders by viewModel.orders.collectAsState()
    val catalogServices by viewModel.catalogServices.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val stories by viewModel.stories.collectAsState()

    var selectedStoryIndex by remember { mutableStateOf<Int?>(null) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }

    val unreadChatCount = remember(conversations) { conversations.sumOf { it.unreadCount } }
    val unreadNotifsCount = remember(notifications) { notifications.count { !it.isRead } }

    val activeOrders = remember(orders) {
        orders.filter { it.status == OrderStatus.IN_PROGRESS || it.status == OrderStatus.ACCEPTED || it.status == OrderStatus.PENDING }
    }

    var searchQuery by remember { mutableStateOf("") }

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

    // 8 Required Circular Categories matching user prompt and Birbank circular stories
    val circularCategories = remember {
        listOf(
            CircularServiceCategory("Diplom işi", Icons.Default.School, ServiceType.DIPLOMA),
            CircularServiceCategory("Magistr dissertasiyası", Icons.Default.WorkspacePremium, ServiceType.MASTER),
            CircularServiceCategory("Kurs və layihə işi", Icons.Default.FolderSpecial, ServiceType.COURSE_WORK),
            CircularServiceCategory("Sərbəst iş, referat və esse", Icons.Default.Description, ServiceType.ESSAY),
            CircularServiceCategory("Təqdimat və slayd", Icons.Default.CoPresent, ServiceType.PRESENTATION),
            CircularServiceCategory("Elmi məqalə", Icons.AutoMirrored.Filled.MenuBook, ServiceType.ARTICLE),
            CircularServiceCategory("Statistik təhlil", Icons.Default.Analytics, ServiceType.STATISTICAL_ANALYSIS),
            CircularServiceCategory("Redaktə və formatlaşdırma", Icons.Default.Spellcheck, ServiceType.EDITING)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BirbankBgLight)
            .testTag("home_screen_lazy_column"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // ========================================================
        // 1. TOP HEADER (Birbank Style: Logo, Search pill, Chat, Notif, Profile)
        // ========================================================
        item {
            Surface(
                color = Color.White,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left: Birbank-styled Brand Bar / Emblem
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable {
                                if (isStaff) {
                                    viewModel.navigateTo(AppDestination.ADMIN_DASHBOARD)
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(BirbankRed)
                            )
                            BestGroupLogoBadge(size = 32)
                        }

                        // Center: Search Pill ("Axtar" capsule)
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = BirbankSearchBg,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable {
                                    viewModel.selectBottomTab(BottomTab.SERVICES)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Axtar",
                                    tint = BirbankTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (searchQuery.isBlank()) "Axtar" else searchQuery,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (searchQuery.isBlank()) BirbankTextSecondary else BirbankTextPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Right: Messages, Notifications, Profile Icons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Chat icon with badge
                            IconButton(
                                onClick = { viewModel.selectBottomTab(BottomTab.CHAT) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadChatCount > 0) {
                                            Badge(containerColor = BirbankRed, contentColor = Color.White) {
                                                Text("$unreadChatCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Mesajlar",
                                        tint = BirbankTextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Notification icon with badge
                            IconButton(
                                onClick = { viewModel.selectBottomTab(BottomTab.NOTIFICATIONS) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifsCount > 0) {
                                            Badge(containerColor = BirbankRed, contentColor = Color.White) {
                                                Text("$unreadNotifsCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Bildirişlər",
                                        tint = BirbankTextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Profile Avatar
                            IconButton(
                                onClick = { viewModel.selectBottomTab(BottomTab.PROFILE) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(BirbankRedLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = "Profil",
                                        tint = BirbankRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ========================================================
        // 2. STORIES BAR (Real Stories with unviewed/viewed rings & admin creation)
        // ========================================================
        if (stories.isNotEmpty() || isStaff) {
            item {
                StoryBar(
                    stories = stories,
                    currentUserId = user?.id ?: "",
                    isStaff = isStaff,
                    onStoryClick = { index -> selectedStoryIndex = index },
                    onAddStoryClick = { showCreateStoryDialog = true }
                )
            }
        }

        // ========================================================
        // 3. CIRCULAR SERVICE CATEGORIES (Birbank Circular Services)
        // ========================================================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 14.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(circularCategories) { cat ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(78.dp)
                                .clickable {
                                    val matched = catalogServices.find {
                                        it.serviceId.contains(cat.serviceType?.name?.lowercase() ?: "", ignoreCase = true) ||
                                        it.nameAz.contains(cat.title, ignoreCase = true)
                                    }
                                    viewModel.startNewOrder(preselectedService = cat.serviceType, preselectedCatalogService = matched)
                                }
                        ) {
                            // Circular Ring Border (Red outer ring + inner badge)
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, BirbankRed.copy(alpha = 0.85f), CircleShape)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(BirbankRedLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.title,
                                        tint = BirbankRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = BirbankTextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ========================================================
        // 3. QUICK METRIC / STATUS PILLS (Birbank Balance Chips)
        // ========================================================
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    MetricChip(
                        icon = Icons.Default.VerifiedUser,
                        label = "Turnitin 100%",
                        badgeColor = Color(0xFF10B981)
                    )
                }
                item {
                    MetricChip(
                        icon = Icons.Default.Bolt,
                        label = "24-48 Saat Təhvil",
                        badgeColor = BirbankRed
                    )
                }
                item {
                    MetricChip(
                        icon = Icons.Default.SupportAgent,
                        label = "24/7 Canlı Kurator",
                        badgeColor = Color(0xFF3B82F6)
                    )
                }
                item {
                    MetricChip(
                        icon = Icons.Default.Lock,
                        label = "Tam Məxfilik",
                        badgeColor = Color(0xFF8B5CF6)
                    )
                }
            }
        }

        // ========================================================
        // 4. THREE QUICK ACTION CARDS (Sifarişlərim, Mesajlar, Tarixçə)
        // ========================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Sifarişlərim
                QuickActionCard(
                    title = "Sifarişlərim",
                    icon = Icons.Outlined.Assignment,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectBottomTab(BottomTab.ORDERS) },
                    testTag = "quick_card_orders"
                )

                // Card 2: Mesajlar
                QuickActionCard(
                    title = "Mesajlar",
                    icon = Icons.Outlined.Chat,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectBottomTab(BottomTab.CHAT) },
                    badgeCount = unreadChatCount,
                    testTag = "quick_card_messages"
                )

                // Card 3: Tarixçə
                QuickActionCard(
                    title = "Tarixçə",
                    icon = Icons.Outlined.History,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectBottomTab(BottomTab.ORDERS) },
                    testTag = "quick_card_history"
                )
            }
        }

        // ========================================================
        // 5. IF STAFF: ADMIN DASHBOARD METRICS & SHORTCUTS
        // ========================================================
        if (isStaff) {
            item {
                val stats by viewModel.adminStats.collectAsState()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.navigateTo(AppDestination.ADMIN_DASHBOARD) }
                        .border(1.dp, BirbankRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .testTag("home_hero_admin_dashboard_button"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(BirbankRedLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("İdarəçi Paneli", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BirbankTextPrimary)
                                    Text("Sistem statistikası və sifariş axını", style = MaterialTheme.typography.labelSmall, color = BirbankTextSecondary)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdminMiniMetricCard("Gözləyən", "${stats.pendingOrdersCount}", BirbankRed, Modifier.weight(1f))
                            AdminMiniMetricCard("Hazırlanan", "${stats.inProgressOrdersCount}", Color(0xFF3B82F6), Modifier.weight(1f))
                            AdminMiniMetricCard("Hazır", "${stats.readyOrdersCount}", Color(0xFF10B981), Modifier.weight(1f))
                            AdminMiniMetricCard("Ümumi", "${stats.totalOrdersCount}", BirbankTextPrimary, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ========================================================
        // 6. AKTİV SİFARİŞLƏR ("Kartlar və hesablar" Style Section)
        // Yalnız müştərilər üçün göstərilir. Admin üçün tamamilə çıxarılıb.
        // ========================================================
        if (!isStaff) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aktiv Sifarişlər",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BirbankTextPrimary
                        )
                        TextButton(
                            onClick = { viewModel.navigateTo(AppDestination.ORDER_WIZARD) },
                            modifier = Modifier.testTag("home_create_order_btn")
                        ) {
                            Text(
                                text = "+ Yeni sifariş",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = BirbankRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeOrders.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BirbankRedLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Assignment, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(24.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Aktiv sifarişiniz yoxdur",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BirbankTextPrimary
                                    )
                                    Text(
                                        text = "Dissertasiya, diplom və ya kurs işi üçün dərhal müraciət edin",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BirbankTextSecondary
                                    )
                                }
                                Button(
                                    onClick = { viewModel.navigateTo(AppDestination.ORDER_WIZARD) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Sifariş et", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
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
        }

        // ========================================================
        // 7. AKADEMİK XİDMƏTLƏR KARTLARI (Firestore-dan Yüklənən)
        // ========================================================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Akademik Xidmətlər",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BirbankTextPrimary
                        )
                        Text(
                            text = "Peşəkar elmi və tədqiqat xidmətləri",
                            style = MaterialTheme.typography.labelSmall,
                            color = BirbankTextSecondary
                        )
                    }
                    TextButton(
                        onClick = { viewModel.selectBottomTab(BottomTab.SERVICES) }
                    ) {
                        Text(
                            text = "Hamısına bax",
                            style = MaterialTheme.typography.labelMedium,
                            color = BirbankRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(catalogServices.take(8), key = { it.serviceId }) { cat ->
                        BirbankServiceCard(
                            item = cat,
                            language = currentLanguage,
                            onClick = {
                                viewModel.startNewOrder(preselectedCatalogService = cat)
                            }
                        )
                    }
                }
            }
        }

        // ========================================================
        // 8. İDARƏÇİ ELANLARI VƏ KAMPANİYALAR (Birbank Promo Banner)
        // ========================================================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { viewModel.navigateTo(AppDestination.ORDER_WIZARD) }
                        .shadow(4.dp, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BirbankRedBorder)
                ) {
                    Column {
                        // Top decorative accent strip
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Brush.horizontalGradient(listOf(BirbankRed, BirbankRedDark)))
                        )

                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BirbankRedLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = BirbankRed,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = BirbankRed,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "XÜSUSİ TƏKLİF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Payız Akademik Kampaniyası",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BirbankTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Bütün magistr və dissertasiya işlərinə 15% endirim və rəsmi Turnitin anti-plagiat hesabatı hədiyyə!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BirbankTextSecondary,
                                    lineHeight = 16.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = BirbankRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Story Viewer Dialog ---
    if (selectedStoryIndex != null) {
        StoryViewerDialog(
            stories = stories,
            initialIndex = selectedStoryIndex ?: 0,
            isStaff = isStaff,
            onDismiss = { selectedStoryIndex = null },
            onMarkViewed = { storyId -> viewModel.markStoryViewed(storyId) },
            onDeleteStory = { storyId -> viewModel.deleteStory(storyId) }
        )
    }

    // --- Create Story Dialog (Admin/Staff only) ---
    if (showCreateStoryDialog) {
        CreateStoryDialog(
            onDismiss = { showCreateStoryDialog = false },
            onPublish = { bytes, caption, mimeType ->
                viewModel.publishStory(
                    bytes = bytes,
                    caption = caption,
                    mimeType = mimeType,
                    onSuccess = { showCreateStoryDialog = false },
                    onError = { /* handled */ }
                )
            }
        )
    }
}

@Composable
private fun MetricChip(
    icon: ImageVector,
    label: String,
    badgeColor: Color
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = BirbankTextPrimary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, BirbankBorder, RoundedCornerShape(16.dp))
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (badgeCount > 0) {
                            Badge(containerColor = BirbankRed, contentColor = Color.White) {
                                Text("$badgeCount")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = BirbankRed,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BirbankTextPrimary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BirbankServiceCard(
    item: ServiceCatalogItem,
    language: Language,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, BirbankBorder, RoundedCornerShape(16.dp))
            .testTag("service_mini_card_${item.serviceId}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BirbankRedLight),
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
                    tint = BirbankRed,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.getLocalizedName(language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BirbankTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            val priceFormatted = item.getFormattedPrice(language)
            val priceText = if (priceFormatted.isNotBlank() && item.startingPriceAzn > 0) {
                "$priceFormatted-dən başlayaraq"
            } else {
                "Qiymət işin tələblərinə əsasən müəyyən edilir"
            }

            Text(
                text = priceText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = BirbankRed,
                fontSize = 11.sp,
                maxLines = 2,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.getLocalizedDescription(language),
                style = MaterialTheme.typography.bodySmall,
                color = BirbankTextSecondary,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Ətraflı",
                    style = MaterialTheme.typography.labelSmall,
                    color = BirbankRed,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Ətraflı",
                    tint = BirbankRed,
                    modifier = Modifier.size(14.dp)
                )
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, BirbankBorder, RoundedCornerShape(16.dp))
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    color = BirbankRed
                )
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = order.topic,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BirbankTextPrimary,
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
                    color = BirbankTextSecondary
                )
                Text(
                    text = "${order.progressPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BirbankRed
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { order.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = BirbankRed,
                trackColor = BirbankBorder
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                        tint = BirbankTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = order.deadline,
                        style = MaterialTheme.typography.bodySmall,
                        color = BirbankTextSecondary,
                        fontSize = 11.sp
                    )
                }

                PriorityBadge(priority = order.priority)
            }
        }
    }
}

@Composable
private fun AdminMiniMetricCard(
    title: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = BirbankTextSecondary,
                maxLines = 1
            )
        }
    }
}
