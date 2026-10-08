package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.*
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MetricStatCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.AppDestination
import com.example.viewmodel.MainViewModel

enum class AdminTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    SUPPORT("Müştəri xidmətləri", Icons.Default.SupportAgent),
    ORDERS("Sifarişlər", Icons.Default.Assignment),
    CUSTOMERS("Müştərilər", Icons.Default.People),
    ORDER_CHATS("Sifariş çatları", Icons.Default.Chat),
    NOTIFICATIONS("Bildirişlər", Icons.Default.Notifications),
    PROFILE("Profil", Icons.Default.AccountCircle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.adminStats.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val customers by viewModel.adminCustomers.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var currentTab by remember { mutableStateOf(AdminTab.DASHBOARD) }

    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var broadcastSentMessage by remember { mutableStateOf(false) }

    var selectedOrderForAdminAction by remember { mutableStateOf<Order?>(null) }
    var selectedCustomerForDetail by remember { mutableStateOf<User?>(null) }
    var orderFilterStatus by remember { mutableStateOf<OrderStatus?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTab.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentUser?.role?.title ?: "Admin"} • BestGroup.az İdarəetmə",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppDestination.MAIN) },
                        modifier = Modifier.testTag("admin_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showBroadcastDialog = true },
                        modifier = Modifier.testTag("admin_broadcast_btn")
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = "Push Bildiriş", tint = Gold500)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            ScrollableTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = Gold500,
                edgePadding = 12.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("admin_tabs_row")
            ) {
                AdminTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = { Text(tab.title, fontSize = 12.sp, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(18.dp)) },
                        selectedContentColor = Gold500,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            AdminTab.DASHBOARD -> {
                AdminDashboardContent(
                    stats = stats,
                    orders = orders,
                    modifier = modifier.padding(innerPadding),
                    onBroadcastClick = { showBroadcastDialog = true },
                    onManageOrder = { selectedOrderForAdminAction = it },
                    onViewOrder = { viewModel.viewOrderDetail(it) },
                    onNavigateToTab = { currentTab = it }
                )
            }
            AdminTab.SUPPORT -> {
                AdminSupportContent(
                    conversations = conversations.filter { it.orderId == null },
                    modifier = modifier.padding(innerPadding),
                    onOpenChat = { conv ->
                        viewModel.setActiveConversation(conv.id)
                        viewModel.navigateTo(AppDestination.MAIN)
                        viewModel.selectBottomTab(com.example.viewmodel.BottomTab.CHAT)
                    }
                )
            }
            AdminTab.ORDERS -> {
                AdminOrdersContent(
                    orders = if (orderFilterStatus != null) orders.filter { it.status == orderFilterStatus } else orders,
                    currentFilter = orderFilterStatus,
                    onFilterChange = { orderFilterStatus = it },
                    modifier = modifier.padding(innerPadding),
                    onManageOrder = { selectedOrderForAdminAction = it },
                    onViewOrder = { viewModel.viewOrderDetail(it) },
                    onOpenOrderChat = { viewModel.openOrderChat(it.id) }
                )
            }
            AdminTab.CUSTOMERS -> {
                AdminCustomersContent(
                    customers = customers,
                    modifier = modifier.padding(innerPadding),
                    onCustomerClick = { selectedCustomerForDetail = it },
                    onOpenSupportChat = { customer ->
                        viewModel.openSupportChatForCustomer(customer)
                    }
                )
            }
            AdminTab.ORDER_CHATS -> {
                AdminOrderChatsContent(
                    conversations = conversations.filter { it.orderId != null },
                    modifier = modifier.padding(innerPadding),
                    onOpenChat = { conv ->
                        viewModel.setActiveConversation(conv.id)
                        viewModel.navigateTo(AppDestination.MAIN)
                        viewModel.selectBottomTab(com.example.viewmodel.BottomTab.CHAT)
                    }
                )
            }
            AdminTab.NOTIFICATIONS -> {
                AdminNotificationsContent(
                    notifications = notifications,
                    modifier = modifier.padding(innerPadding),
                    onBroadcastClick = { showBroadcastDialog = true },
                    onNotificationClick = { notif ->
                        if (notif.targetOrderId != null) {
                            val matched = orders.find { it.id == notif.targetOrderId }
                            if (matched != null) viewModel.viewOrderDetail(matched)
                        } else if (notif.targetChatId != null) {
                            viewModel.setActiveConversation(notif.targetChatId)
                            viewModel.navigateTo(AppDestination.MAIN)
                            viewModel.selectBottomTab(com.example.viewmodel.BottomTab.CHAT)
                        }
                    }
                )
            }
            AdminTab.PROFILE -> {
                AdminProfileContent(
                    user = currentUser,
                    stats = stats,
                    modifier = modifier.padding(innerPadding),
                    onLogout = { viewModel.logout() }
                )
            }
        }
    }

    // Broadcast Push Notification Dialog
    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text(localizedString(StringKey.ADMIN_PUSH_BROADCAST), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (broadcastSentMessage) {
                        Text(
                            "Push bildiriş bütün aktiv müştərilərə uğurla göndərildi!",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it },
                            label = { Text(localizedString(StringKey.ADMIN_PUSH_TITLE)) },
                            placeholder = { Text("Məsələn: Yeni imtiyazlı xidmət təklifi") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = broadcastMessage,
                            onValueChange = { broadcastMessage = it },
                            label = { Text(localizedString(StringKey.ADMIN_PUSH_MSG)) },
                            placeholder = { Text("Bildirişin ətraflı mətni...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!broadcastSentMessage) {
                            if (broadcastTitle.isNotBlank()) {
                                viewModel.adminSendPushBroadcast(broadcastTitle, broadcastMessage)
                                broadcastSentMessage = true
                            }
                        } else {
                            showBroadcastDialog = false
                            broadcastSentMessage = false
                            broadcastTitle = ""
                            broadcastMessage = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500)
                ) {
                    Text(if (broadcastSentMessage) "Bağla" else "Göndər")
                }
            },
            dismissButton = {
                if (!broadcastSentMessage) {
                    TextButton(onClick = { showBroadcastDialog = false }) {
                        Text(localizedString(StringKey.BTN_CANCEL))
                    }
                }
            }
        )
    }

    // Customer Detail Dialog
    if (selectedCustomerForDetail != null) {
        val cust = selectedCustomerForDetail!!
        AlertDialog(
            onDismissRequest = { selectedCustomerForDetail = null },
            title = { Text(cust.fullName, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Email: ${cust.email}")
                    if (cust.phone.isNotBlank()) Text("Telefon: ${cust.phone}")
                    if (cust.university.isNotBlank()) Text("Universitet: ${cust.university}")
                    if (cust.faculty.isNotBlank()) Text("Fakültə: ${cust.faculty}")
                    if (cust.degreeLevel.isNotBlank()) Text("Təhsil səviyyəsi: ${cust.degreeLevel}")
                    if (cust.createdAt.isNotBlank()) Text("Qeydiyyat: ${cust.createdAt}")
                    Text("Sifariş sayı: ${cust.orderCount}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = cust
                        selectedCustomerForDetail = null
                        viewModel.openSupportChatForCustomer(c)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500)
                ) {
                    Text("Çat aç")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCustomerForDetail = null }) {
                    Text("Bağla")
                }
            }
        )
    }

    // Manage Single Order Modal Sheet
    if (selectedOrderForAdminAction != null) {
        val o = selectedOrderForAdminAction!!
        ModalBottomSheet(
            onDismissRequest = { selectedOrderForAdminAction = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Sifariş #${o.orderNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = o.topic,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Statusu dəyiş (Real Firestore):", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                OrderStatus.values().forEach { st ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                viewModel.adminUpdateOrderStatus(o.id, st, "Admin statusu '${st.name}' olaraq dəyişdi.")
                                selectedOrderForAdminAction = null
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(localizedString(st.labelKey), fontWeight = FontWeight.Bold)
                            StatusBadge(status = st)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Prioriteti dəyiş:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OrderPriority.values().forEach { pr ->
                        Button(
                            onClick = {
                                viewModel.adminUpdatePriority(o.id, pr)
                                selectedOrderForAdminAction = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (o.priority == pr) Gold500 else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (o.priority == pr) Navy900 else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(localizedString(pr.labelKey), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 1: DASHBOARD
// ----------------------------------------------------
@Composable
private fun AdminDashboardContent(
    stats: AdminDashboardStats,
    orders: List<Order>,
    modifier: Modifier = Modifier,
    onBroadcastClick: () -> Unit,
    onManageOrder: (Order) -> Unit,
    onViewOrder: (Order) -> Unit,
    onNavigateToTab: (AdminTab) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real Metrics Row 1: Ümumi sifarişlər & Gözləyən
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Ümumi Sifariş",
                    value = "${stats.totalOrdersCount}",
                    subtitle = "Bütün dövr üzrə",
                    icon = Icons.Default.Assignment,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Gözləyən",
                    value = "${stats.pendingOrdersCount}",
                    subtitle = "Təsdiq gözləyir",
                    icon = Icons.Default.HourglassEmpty,
                    iconColor = Gold500,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Real Metrics Row 2: Qəbul edilmiş & Hazırlanan
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Qəbul Edildi",
                    value = "${stats.acceptedOrdersCount}",
                    subtitle = "Kurator təyin edildi",
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Hazırlanır",
                    value = "${stats.inProgressOrdersCount}",
                    subtitle = "İcrada olan",
                    icon = Icons.Default.PendingActions,
                    iconColor = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Real Metrics Row 3: Hazır & Ləğv edilmiş
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Hazır",
                    value = "${stats.readyOrdersCount}",
                    subtitle = "Təhvil verilmiş",
                    icon = Icons.Default.TaskAlt,
                    iconColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Ləğv Edildi",
                    value = "${stats.cancelledOrdersCount}",
                    subtitle = "Ləğv olunmuş",
                    icon = Icons.Default.Cancel,
                    iconColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Real Metrics Row 4: Müştəri Sayı & Cavab Gözləyən Mesajlar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Müştəri Sayı",
                    value = "${stats.totalCustomersCount}",
                    subtitle = "Qeydiyyatdan keçmiş",
                    icon = Icons.Default.Group,
                    iconColor = Gold500,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Cavab Gözləyən",
                    value = "${stats.waitingSupportMessagesCount + stats.waitingOrderMessagesCount}",
                    subtitle = "Dəstək: ${stats.waitingSupportMessagesCount} | Sifariş: ${stats.waitingOrderMessagesCount}",
                    icon = Icons.Default.MarkUnreadChatAlt,
                    iconColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Broadcast Push Action Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onBroadcastClick() }
                    .border(1.dp, Gold500.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.SendToMobile, contentDescription = null, tint = Gold500)
                        Column {
                            Text(
                                text = localizedString(StringKey.ADMIN_PUSH_BROADCAST),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Bütün istifadəçilərə qlobal bildiriş göndər",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Gold500)
                }
            }
        }

        // Orders List Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Son Sifarişlər",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { onNavigateToTab(AdminTab.ORDERS) }) {
                    Text("Hamısına bax (${orders.size})", color = Gold500, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(orders.take(5), key = { it.id }) { order ->
            AdminOrderManageCard(
                order = order,
                onManageClick = { onManageOrder(order) },
                onViewClick = { onViewOrder(order) }
            )
        }
    }
}

// ----------------------------------------------------
// TAB 2: MÜŞTƏRİ XİDMƏTLƏRİ (Support Chat)
// ----------------------------------------------------
@Composable
private fun AdminSupportContent(
    conversations: List<ChatConversation>,
    modifier: Modifier = Modifier,
    onOpenChat: (ChatConversation) -> Unit
) {
    if (conversations.isEmpty()) {
        EmptyStateView(
            titleKey = StringKey.CHAT_EMPTY_MESSAGES,
            descKey = StringKey.EMPTY_ORDERS_DESC,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conversations, key = { it.id }) { conv ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenChat(conv) }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                .background(Navy800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Gold500)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = conv.title.ifBlank { "Müştəri Söhbəti" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = conv.lastMessageTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = conv.lastMessageText.ifBlank { "Yeni söhbət başlandı" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        if (conv.unreadCount > 0) {
                            Badge(containerColor = Gold500, contentColor = Navy900) {
                                Text("${conv.unreadCount}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 3: SİFARİŞLƏR
// ----------------------------------------------------
@Composable
private fun AdminOrdersContent(
    orders: List<Order>,
    currentFilter: OrderStatus?,
    onFilterChange: (OrderStatus?) -> Unit,
    modifier: Modifier = Modifier,
    onManageOrder: (Order) -> Unit,
    onViewOrder: (Order) -> Unit,
    onOpenOrderChat: (Order) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = currentFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("Hamısı", fontSize = 11.sp) }
            )
            OrderStatus.values().forEach { st ->
                FilterChip(
                    selected = currentFilter == st,
                    onClick = { onFilterChange(if (currentFilter == st) null else st) },
                    label = { Text(localizedString(st.labelKey), fontSize = 11.sp) }
                )
            }
        }

        if (orders.isEmpty()) {
            EmptyStateView(
                titleKey = StringKey.EMPTY_ORDERS_TITLE,
                descKey = StringKey.EMPTY_ORDERS_DESC,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    AdminOrderManageCard(
                        order = order,
                        onManageClick = { onManageOrder(order) },
                        onViewClick = { onViewOrder(order) },
                        onChatClick = { onOpenOrderChat(order) }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 4: MÜŞTƏRİLƏR
// ----------------------------------------------------
@Composable
private fun AdminCustomersContent(
    customers: List<User>,
    modifier: Modifier = Modifier,
    onCustomerClick: (User) -> Unit,
    onOpenSupportChat: (User) -> Unit
) {
    if (customers.isEmpty()) {
        EmptyStateView(
            titleKey = StringKey.EMPTY_ORDERS_TITLE,
            descKey = StringKey.EMPTY_ORDERS_DESC,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(customers, key = { it.id }) { cust ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCustomerClick(cust) }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cust.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Navy800
                            ) {
                                Text(
                                    text = cust.role.title,
                                    color = Gold500,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cust.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (cust.phone.isNotBlank()) {
                            Text(
                                text = "Tel: ${cust.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (cust.university.isNotBlank()) {
                            Text(
                                text = "${cust.university} • ${cust.degreeLevel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold500
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onCustomerClick(cust) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Profil", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onOpenSupportChat(cust) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Çat aç", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 5: SİFARİŞ ÇATLARI (Order Chats)
// ----------------------------------------------------
@Composable
private fun AdminOrderChatsContent(
    conversations: List<ChatConversation>,
    modifier: Modifier = Modifier,
    onOpenChat: (ChatConversation) -> Unit
) {
    if (conversations.isEmpty()) {
        EmptyStateView(
            titleKey = StringKey.CHAT_EMPTY_MESSAGES,
            descKey = StringKey.EMPTY_ORDERS_DESC,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conversations, key = { it.id }) { conv ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenChat(conv) }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                .background(Navy800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = Gold500)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = conv.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = conv.lastMessageTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = conv.lastMessageText.ifBlank { "Sifariş üzrə yeni mesaj" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        if (conv.unreadCount > 0) {
                            Badge(containerColor = Gold500, contentColor = Navy900) {
                                Text("${conv.unreadCount}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 6: BİLDİRİŞLƏR (Admin Notification Center)
// ----------------------------------------------------
@Composable
private fun AdminNotificationsContent(
    notifications: List<NotificationItem>,
    modifier: Modifier = Modifier,
    onBroadcastClick: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onBroadcastClick,
                colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Qlobal Push Bildiriş Göndər", fontWeight = FontWeight.Bold)
            }
        }

        items(notifications, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNotificationClick(item) }
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Navy800),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (item.category) {
                            NotificationCategory.ORDERS -> Icons.Default.Assignment
                            NotificationCategory.MESSAGES -> Icons.Default.Chat
                            NotificationCategory.CAMPAIGNS -> Icons.Default.Campaign
                            else -> Icons.Default.Notifications
                        }
                        Icon(icon, contentDescription = null, tint = Gold500, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.timestamp,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 7: PROFİL/HESAB
// ----------------------------------------------------
@Composable
private fun AdminProfileContent(
    user: User?,
    stats: AdminDashboardStats,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(user?.fullName ?: "Admin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Navy800
                ) {
                    Text(
                        text = "İcazə: ${user?.role?.title ?: "Admin"} (Firebase Custom Claim)",
                        color = Gold500,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sistem İcmalı", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("Ümumi Sifarişlər: ${stats.totalOrdersCount}", style = MaterialTheme.typography.bodyMedium)
                Text("Aktiv Sifarişlər: ${stats.activeOrdersCount}", style = MaterialTheme.typography.bodyMedium)
                Text("Qeydiyyatlı Müştərilər: ${stats.totalCustomersCount}", style = MaterialTheme.typography.bodyMedium)
                Text("FCM Bildiriş Sistemi: Aktiv", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Çıxış et", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminOrderManageCard(
    order: Order,
    onManageClick: () -> Unit,
    onViewClick: () -> Unit,
    onChatClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .testTag("admin_order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = order.topic,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${order.university} • ${order.deadline}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Bax", style = MaterialTheme.typography.labelSmall)
                }

                if (onChatClick != null) {
                    OutlinedButton(
                        onClick = onChatClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Çat", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Button(
                    onClick = onManageClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("İdarə et", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
