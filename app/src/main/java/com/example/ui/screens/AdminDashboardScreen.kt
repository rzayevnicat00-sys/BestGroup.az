package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import com.example.ui.theme.ThemeMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.*
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MetricStatCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import coil.compose.AsyncImage
import com.example.ui.theme.BirbankBorder
import com.example.ui.theme.BirbankBgLight
import com.example.ui.theme.BirbankRed
import com.example.ui.theme.BirbankRedLight
import com.example.ui.theme.BirbankTextPrimary
import com.example.ui.theme.BirbankTextSecondary
import com.example.viewmodel.AppDestination
import com.example.viewmodel.MainViewModel

enum class AdminTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    SUPPORT("Müştəri xidmətləri", Icons.Default.SupportAgent),
    ORDERS("Sifarişlər", Icons.Default.Assignment),
    CUSTOMERS("Müştərilər", Icons.Default.People),
    ORDER_CHATS("Sifariş çatları", Icons.Default.Chat),
    UPDATES("Yeniləmələr", Icons.Default.SystemUpdate),
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
    val notifications by viewModel.adminNotifications.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var currentTab by remember { mutableStateOf(AdminTab.DASHBOARD) }
    var selectedSupportConversation by remember { mutableStateOf<ChatConversation?>(null) }
    var contactTargetCustomer by remember { mutableStateOf<Triple<String, String, String?>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.ensureAdminDataObserved()
    }

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
                            color = BirbankRed
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
                        Icon(Icons.Default.Campaign, contentDescription = "Push Bildiriş", tint = BirbankRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            ScrollableTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = Color.White,
                contentColor = BirbankRed,
                edgePadding = 12.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .border(BorderStroke(1.dp, BirbankBorder))
                    .testTag("admin_tabs_row")
            ) {
                AdminTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = {
                            currentTab = tab
                            selectedSupportConversation = null
                        },
                        text = { Text(tab.title, fontSize = 12.sp, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(18.dp)) },
                        selectedContentColor = BirbankRed,
                        unselectedContentColor = BirbankTextSecondary
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
                    customers = customers,
                    modifier = modifier.padding(innerPadding),
                    onBroadcastClick = { showBroadcastDialog = true },
                    onManageOrder = { selectedOrderForAdminAction = it },
                    onViewOrder = { viewModel.viewOrderDetail(it) },
                    onContactOrder = { ord ->
                        val matchingCust = customers.find { it.id == ord.userId }
                        val cPhone = ord.customerPhone.ifBlank { matchingCust?.phone ?: "" }
                        val cName = ord.customerName.ifBlank { matchingCust?.fullName ?: "Müştəri" }
                        contactTargetCustomer = Triple(cName, cPhone, ord.orderNumber)
                    },
                    onNavigateToTab = { currentTab = it }
                )
            }
            AdminTab.SUPPORT -> {
                if (selectedSupportConversation != null) {
                    AdminSupportChatDetailView(
                        conversation = selectedSupportConversation!!,
                        viewModel = viewModel,
                        customers = customers,
                        modifier = modifier.padding(innerPadding),
                        onBack = { selectedSupportConversation = null }
                    )
                } else {
                    AdminSupportContent(
                        conversations = conversations,
                        customers = customers,
                        modifier = modifier.padding(innerPadding),
                        onOpenChat = { conv ->
                            selectedSupportConversation = conv
                            viewModel.setActiveConversation(conv.id, navigateToCustomerChat = false)
                        }
                    )
                }
            }
            AdminTab.ORDERS -> {
                val sortedOrders = remember(orders) {
                    orders.sortedByDescending { it.createdAt }
                }
                val filteredOrders = remember(sortedOrders, orderFilterStatus) {
                    if (orderFilterStatus != null) sortedOrders.filter { it.status == orderFilterStatus }
                    else sortedOrders
                }
                AdminOrdersContent(
                    orders = filteredOrders,
                    allOrders = sortedOrders,
                    customers = customers,
                    currentFilter = orderFilterStatus,
                    onFilterChange = { orderFilterStatus = it },
                    modifier = modifier.padding(innerPadding),
                    onManageOrder = { selectedOrderForAdminAction = it },
                    onViewOrder = { viewModel.viewOrderDetail(it) },
                    onOpenOrderChat = { ord ->
                        val matchedConv = conversations.find { it.orderId == ord.id }
                            ?: conversations.find { it.id == "order_${ord.id}" }
                        if (matchedConv != null) {
                            selectedSupportConversation = matchedConv
                            currentTab = AdminTab.SUPPORT
                        } else {
                            viewModel.openOrderChat(ord.id)
                        }
                    },
                    onContactOrder = { ord ->
                        val matchingCust = customers.find { it.id == ord.userId }
                        val cPhone = ord.customerPhone.ifBlank { matchingCust?.phone ?: "" }
                        val cName = ord.customerName.ifBlank { matchingCust?.fullName ?: "Müştəri" }
                        contactTargetCustomer = Triple(cName, cPhone, ord.orderNumber)
                    }
                )
            }
            AdminTab.CUSTOMERS -> {
                AdminCustomersContent(
                    customers = customers,
                    modifier = modifier.padding(innerPadding),
                    onCustomerClick = { selectedCustomerForDetail = it },
                    onOpenSupportChat = { customer ->
                        val supportConv = conversations.find { it.customerId == customer.id && it.orderId == null }
                        if (supportConv != null) {
                            selectedSupportConversation = supportConv
                            currentTab = AdminTab.SUPPORT
                        } else {
                            coroutineScope.launch {
                                try {
                                    val conv = viewModel.repository.getOrCreateGeneralSupportConversation(
                                        targetCustomerId = customer.id,
                                        targetCustomerName = customer.fullName
                                    )
                                    selectedSupportConversation = conv
                                    currentTab = AdminTab.SUPPORT
                                } catch (_: Exception) {
                                    viewModel.openSupportChatForCustomer(customer)
                                }
                            }
                        }
                    },
                    onContactCustomer = { customer ->
                        contactTargetCustomer = Triple(customer.fullName, customer.phone, null)
                    }
                )
            }
            AdminTab.ORDER_CHATS -> {
                if (selectedSupportConversation != null) {
                    AdminSupportChatDetailView(
                        conversation = selectedSupportConversation!!,
                        viewModel = viewModel,
                        customers = customers,
                        modifier = modifier.padding(innerPadding),
                        onBack = { selectedSupportConversation = null }
                    )
                } else {
                    AdminOrderChatsContent(
                        conversations = conversations.filter { it.orderId != null },
                        modifier = modifier.padding(innerPadding),
                        onOpenChat = { conv ->
                            selectedSupportConversation = conv
                            viewModel.setActiveConversation(conv.id, navigateToCustomerChat = false)
                        }
                    )
                }
            }
            AdminTab.UPDATES -> {
                AdminUpdatesContent(
                    viewModel = viewModel,
                    modifier = modifier.padding(innerPadding)
                )
            }
            AdminTab.NOTIFICATIONS -> {
                AdminNotificationsContent(
                    notifications = notifications,
                    modifier = modifier.padding(innerPadding),
                    onBroadcastClick = { showBroadcastDialog = true },
                    onMarkAllAsRead = { viewModel.markNotificationsAsRead() },
                    onNotificationClick = { notif ->
                        viewModel.markNotificationAsRead(notif.id)
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
                    currentThemeMode = themeMode,
                    onThemeChange = { viewModel.setThemeMode(it) },
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
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White)
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

                    val custOrders = orders.filter { it.userId == cust.id }
                    if (custOrders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Son Sifarişləri:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        custOrders.take(3).forEach { ord ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ord.orderNumber} - ${ord.topic.take(18)}...",
                                    fontSize = 12.sp,
                                    color = BirbankTextSecondary
                                )
                                StatusBadge(status = ord.status)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val c = cust
                            selectedCustomerForDetail = null
                            contactTargetCustomer = Triple(c.fullName, c.phone, null)
                        }
                    ) {
                        Icon(Icons.Default.SendToMobile, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS / WA")
                    }
                    Button(
                        onClick = {
                            val c = cust
                            selectedCustomerForDetail = null
                            coroutineScope.launch {
                                try {
                                    val conv = viewModel.repository.getOrCreateGeneralSupportConversation(
                                        targetCustomerId = c.id,
                                        targetCustomerName = c.fullName
                                    )
                                    selectedSupportConversation = conv
                                    currentTab = AdminTab.SUPPORT
                                } catch (_: Exception) {
                                    viewModel.openSupportChatForCustomer(c)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Çat aç")
                    }
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

                Button(
                    onClick = {
                        val matchingCustomer = customers.find { it.id == o.userId }
                        val custPhone = o.customerPhone.ifBlank { matchingCustomer?.phone ?: "" }
                        val custName = o.customerName.ifBlank { matchingCustomer?.fullName ?: "Müştəri" }
                        val ordNum = o.orderNumber
                        selectedOrderForAdminAction = null
                        contactTargetCustomer = Triple(custName, custPhone, ordNum)
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.SendToMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Müştəriyə SMS / WhatsApp ilə Mesaj Yaz", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                                containerColor = if (o.priority == pr) BirbankRed else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (o.priority == pr) Color.White else MaterialTheme.colorScheme.onSurface
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

    if (contactTargetCustomer != null) {
        val (cName, cPhone, cOrderNum) = contactTargetCustomer!!
        ContactCustomerBottomSheet(
            customerName = cName,
            initialPhone = cPhone,
            orderNumber = cOrderNum,
            onDismiss = { contactTargetCustomer = null }
        )
    }
}

// ----------------------------------------------------
// TAB 1: DASHBOARD
// ----------------------------------------------------
@Composable
private fun AdminDashboardContent(
    stats: AdminDashboardStats,
    orders: List<Order>,
    customers: List<User> = emptyList(),
    modifier: Modifier = Modifier,
    onBroadcastClick: () -> Unit,
    onManageOrder: (Order) -> Unit,
    onViewOrder: (Order) -> Unit,
    onContactOrder: (Order) -> Unit,
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
                    iconColor = BirbankRed,
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
                    iconColor = BirbankRed,
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
                    .border(1.dp, BirbankBorder, RoundedCornerShape(14.dp)),
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
                        Icon(Icons.Default.SendToMobile, contentDescription = null, tint = BirbankRed)
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
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BirbankRed)
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
                    Text("Hamısına bax (${orders.size})", color = BirbankRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Hələlik heç bir sifariş daxil olmayıb",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Müştərilər sifariş verdikdə burada dərhal canlı əks olunacaq.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(orders.take(5), key = { it.id }) { order ->
                val matchingCust = customers.find { it.id == order.userId }
                val cName = order.customerName.ifBlank { matchingCust?.fullName ?: "Müştəri" }
                val cPhone = order.customerPhone.ifBlank { matchingCust?.phone ?: "" }
                AdminOrderManageCard(
                    order = order,
                    customerName = cName,
                    customerPhone = cPhone,
                    onManageClick = { onManageOrder(order) },
                    onViewClick = { onViewOrder(order) },
                    onContactClick = { onContactOrder(order) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// TAB 2: MÜŞTƏRİ XİDMƏTLƏRİ (Support Chat)
// ----------------------------------------------------
@Composable
private fun AdminSupportContent(
    conversations: List<ChatConversation>,
    customers: List<User> = emptyList(),
    modifier: Modifier = Modifier,
    onOpenChat: (ChatConversation) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = BirbankRed)
                Column {
                    Text(
                        text = "Müştəri Xidmətləri Müraciətləri",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Yalnız müştərilər tərəfindən göndərilən canlı sorğular və suallar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(BirbankRedLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MarkUnreadChatAlt, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(30.dp))
                        }
                        Text(
                            text = "Aktiv müştəri müraciəti yoxdur",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Müştərilər 'Müştəri Xidmətləri' bölməsindən sual və ya müraciət göndərdikdə burada canlı əks olunacaq və birbaşa cavab yaza biləcəksiniz.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    val matchingUser = customers.find { it.id == conv.customerId }
                    val displayTitle = conv.title.ifBlank { matchingUser?.fullName ?: "Müştəri" }
                    val subInfo = matchingUser?.phone?.takeIf { it.isNotBlank() }
                        ?: matchingUser?.university?.takeIf { it.isNotBlank() }
                        ?: "Canlı Müştəri Sorğusu"

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
                                    .background(BirbankRedLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = BirbankRed)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = conv.lastMessageTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = subInfo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BirbankRed
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = conv.lastMessageText.ifBlank { "Yeni söhbət başlandı" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (conv.unreadCount > 0) {
                                Badge(containerColor = BirbankRed, contentColor = Color.White) {
                                    Text("${conv.unreadCount}", fontWeight = FontWeight.Bold)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BirbankRed)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSupportChatDetailView(
    conversation: ChatConversation,
    viewModel: MainViewModel,
    customers: List<User> = emptyList(),
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    androidx.activity.compose.BackHandler {
        onBack()
    }

    LaunchedEffect(conversation.id) {
        viewModel.setActiveConversation(conversation.id, navigateToCustomerChat = false)
        viewModel.repository.markConversationAsRead(conversation.id)
    }

    val matchingUser = remember(conversation, customers) {
        customers.find { it.id == conversation.customerId }
    }
    val customerDisplayName = remember(conversation, matchingUser) {
        matchingUser?.fullName?.takeIf { it.isNotBlank() } ?: conversation.title.ifBlank { "Müştəri" }
    }
    val customerSubInfo = remember(matchingUser) {
        val phone = matchingUser?.phone?.takeIf { it.isNotBlank() }
        val uni = matchingUser?.university?.takeIf { it.isNotBlank() }
        when {
            phone != null && uni != null -> "$phone • $uni"
            phone != null -> phone
            uni != null -> uni
            else -> "Müştəri Xidmətləri Sorğusu"
        }
    }

    val allMessages by viewModel.messages.collectAsState()
    val chatMessages = allMessages[conversation.id] ?: emptyList()
    val listState = rememberLazyListState()
    var replyText by remember { mutableStateOf("") }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Chat Header with Back button
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = BirbankRed
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BirbankRedLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = BirbankRed)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Müştəri ilə yazışın ($customerDisplayName)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = customerSubInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Messages List
        if (chatMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = BirbankRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Bu müştəri ilə hələ ki mesaj yoxdur",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aşağıdakı sahədən müştəriyə dərhal cavab yaza bilərsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    AdminChatBubbleItem(message = msg)
                }
            }
        }

        // Reply Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Müştəriyə cavab yazın...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )
                IconButton(
                    onClick = {
                        val text = replyText.trim()
                        if (text.isNotBlank()) {
                            viewModel.sendChatMessage(text, targetConvId = conversation.id)
                            replyText = ""
                        }
                    },
                    enabled = replyText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (replyText.isNotBlank()) BirbankRed else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Göndər",
                        tint = if (replyText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminChatBubbleItem(message: ChatMessage) {
    val isAdmin = message.senderRole.equals("staff", ignoreCase = true) ||
                  message.senderRole.equals("admin", ignoreCase = true) ||
                  !message.isFromUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isAdmin) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isAdmin) BirbankRed else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isAdmin) 16.dp else 2.dp,
                bottomEnd = if (isAdmin) 2.dp else 16.dp
            ),
            border = if (!isAdmin) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isAdmin) "Siz (Baş Admin)" else message.senderName.ifBlank { "Müştəri" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAdmin) Color.White.copy(alpha = 0.9f) else BirbankRed,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isAdmin) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAdmin) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    if (isAdmin) {
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (message.isRead) "Oxundu" else "Göndərildi",
                            tint = if (message.isRead) Color(0xFF67E8F9) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
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
    allOrders: List<Order> = orders,
    customers: List<User> = emptyList(),
    currentFilter: OrderStatus?,
    onFilterChange: (OrderStatus?) -> Unit,
    modifier: Modifier = Modifier,
    onManageOrder: (Order) -> Unit,
    onViewOrder: (Order) -> Unit,
    onOpenOrderChat: (Order) -> Unit,
    onContactOrder: (Order) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Status Filter Chips (Horizontally Scrollable with live counts)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = currentFilter == null,
                onClick = { onFilterChange(null) },
                label = {
                    Text(
                        text = "Hamısı (${allOrders.size})",
                        fontSize = 12.sp,
                        fontWeight = if (currentFilter == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BirbankRed,
                    selectedLabelColor = Color.White
                )
            )
            OrderStatus.values().forEach { st ->
                val count = allOrders.count { it.status == st }
                FilterChip(
                    selected = currentFilter == st,
                    onClick = { onFilterChange(if (currentFilter == st) null else st) },
                    label = {
                        Text(
                            text = "${localizedString(st.labelKey)} ($count)",
                            fontSize = 12.sp,
                            fontWeight = if (currentFilter == st) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BirbankRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Live Header Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "Canlı Sifarişlər • ${orders.size} göstərilir",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Ən yeni sifarişlər üstdə",
                style = MaterialTheme.typography.labelSmall,
                color = BirbankRed,
                fontSize = 11.sp
            )
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
                    val matchingCust = customers.find { it.id == order.userId }
                    val cName = order.customerName.ifBlank { matchingCust?.fullName ?: "Müştəri" }
                    val cPhone = order.customerPhone.ifBlank { matchingCust?.phone ?: "" }
                    AdminOrderManageCard(
                        order = order,
                        customerName = cName,
                        customerPhone = cPhone,
                        onManageClick = { onManageOrder(order) },
                        onViewClick = { onViewOrder(order) },
                        onChatClick = { onOpenOrderChat(order) },
                        onContactClick = { onContactOrder(order) }
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
    onOpenSupportChat: (User) -> Unit,
    onContactCustomer: (User) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "online", "offline"

    val totalCount = customers.size
    val onlineCount = remember(customers) { customers.count { it.isOnline } }
    val offlineCount = totalCount - onlineCount

    val filteredCustomers = remember(customers, searchQuery, selectedFilter) {
        customers.filter { cust ->
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isBlank() ||
                cust.fullName.lowercase().contains(q) ||
                cust.email.lowercase().contains(q) ||
                cust.phone.lowercase().contains(q) ||
                cust.university.lowercase().contains(q)

            val matchesFilter = when (selectedFilter) {
                "online" -> cust.isOnline
                "offline" -> !cust.isOnline
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Filter Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Müştəri axtar (ad, email, nömrə)...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Axtar", tint = BirbankTextSecondary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Təmizlə", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BirbankRed,
                        cursorColor = BirbankRed
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" },
                        label = { Text("Hamısı ($totalCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BirbankRed,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    FilterChip(
                        selected = selectedFilter == "online",
                        onClick = { selectedFilter = "online" },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF12B76A))
                            )
                        },
                        label = { Text("Onlayn ($onlineCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF12B76A),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    FilterChip(
                        selected = selectedFilter == "offline",
                        onClick = { selectedFilter = "offline" },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF98A2B3))
                            )
                        },
                        label = { Text("Oflayn ($offlineCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF475467),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // List
        if (filteredCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = BirbankTextSecondary,
                        modifier = Modifier.size(52.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "Axtarışa uyğun müştəri tapılmadı" else "Qeydiyyatdan keçmiş müştəri yoxdur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BirbankTextPrimary
                    )
                    Text(
                        text = "Qeydiyyatdan keçən bütün müştərilər real vaxt rejimində burada görünəcək.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BirbankTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCustomers, key = { it.id }) { cust ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onCustomerClick(cust) }
                            .border(1.dp, BirbankBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar with presence indicator dot
                                Box(modifier = Modifier.size(54.dp)) {
                                    if (!cust.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = cust.avatarUrl,
                                            contentDescription = cust.fullName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(BirbankRedLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cust.fullName.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = BirbankRed
                                            )
                                        }
                                    }

                                    // Online/Offline status dot
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(if (cust.isOnline) Color(0xFF12B76A) else Color(0xFF98A2B3))
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cust.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BirbankTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Presence status text badge
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (cust.isOnline) Color(0xFFECFDF3) else BirbankBgLight
                                        ) {
                                            Text(
                                                text = if (cust.isOnline) "Onlayn" else formatRelativeActiveTime(cust.lastActiveAtMillis),
                                                color = if (cust.isOnline) Color(0xFF027A48) else BirbankTextSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = cust.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BirbankTextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (cust.phone.isNotBlank()) {
                                        Text(
                                            text = "Tel: ${cust.phone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BirbankTextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (cust.university.isNotBlank()) {
                                        Text(
                                            text = "${cust.university} • ${cust.degreeLevel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BirbankRed,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Order count badge & Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BirbankRedLight,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = "${cust.orderCount} sifariş",
                                        color = BirbankRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onCustomerClick(cust) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("Detallar", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { onContactCustomer(cust) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BirbankRed),
                                    modifier = Modifier.weight(1.1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.SendToMobile, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("SMS / WA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onOpenSupportChat(cust) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Çat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelativeActiveTime(millis: Long): String {
    if (millis <= 0L) return "Oflayn"
    val diff = System.currentTimeMillis() - millis
    val minutes = diff / (60 * 1000)
    return when {
        minutes < 2 -> "İndicə onlayn"
        minutes < 60 -> "$minutes dəq əvvəl"
        minutes < 1440 -> "${minutes / 60} saat əvvəl"
        else -> "${minutes / 1440} gün əvvəl"
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(36.dp))
                    Text(
                        text = "Aktiv sifariş çatı yoxdur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Müştərilər sifarişləri üzrə sual və ya müraciət yazdıqda bütün canlı söhbətlər burada real vaxt rejimində əks olunacaq.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
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
                                .background(BirbankRedLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = BirbankRed)
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
                            Badge(containerColor = BirbankRed, contentColor = Color.White) {
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
    onMarkAllAsRead: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onBroadcastClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Push Göndər", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onMarkAllAsRead,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hamısı Oxundu", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        items(notifications, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNotificationClick(item) }
                    .border(
                        1.dp,
                        if (!item.isRead) BirbankRed.copy(alpha = 0.6f) else BirbankBorder,
                        RoundedCornerShape(14.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (item.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
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
                            .background(if (item.isRead) BirbankRedLight.copy(alpha = 0.5f) else BirbankRedLight),
                        contentAlignment = Alignment.Center
                    ) {
                        val (icon, tint) = when (item.category) {
                            NotificationCategory.ORDERS -> Icons.Default.Assignment to BirbankRed
                            NotificationCategory.MESSAGES -> Icons.Default.Chat to Color(0xFF3B82F6)
                            NotificationCategory.CAMPAIGNS -> Icons.Default.Campaign to Color(0xFF10B981)
                            else -> Icons.Default.Notifications to BirbankRed
                        }
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (item.isRead) tint.copy(alpha = 0.6f) else tint,
                            modifier = Modifier.size(20.dp)
                        )
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
                                fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Normal,
                                color = if (!item.isRead) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (!item.isRead) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(BirbankRed)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (item.isRead) 0.8f else 1f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (item.isRead) "${item.timestamp} • Oxunub" else item.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!item.isRead) BirbankRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
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
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
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
                    color = BirbankRed
                ) {
                    Text(
                        text = "İcazə: ${user?.role?.title ?: "Admin"} (Firebase Custom Claim)",
                        color = BirbankRed,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Appearance / Theme Card (Persisted across app restarts)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Görünüş Teması", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Icon(Icons.Default.Palette, contentDescription = null, tint = BirbankRed)
                }
                Text(
                    text = "Seçdiyiniz tema tətbiqdən çıxıb yenidən daxil olduqda da yaddaşda saxlanılır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.values().forEach { mode ->
                        val isSelected = currentThemeMode == mode
                        val (label, icon) = when (mode) {
                            ThemeMode.SYSTEM -> "Sistem" to Icons.Default.BrightnessAuto
                            ThemeMode.LIGHT -> "Açıq" to Icons.Default.LightMode
                            ThemeMode.DARK -> "Tünd" to Icons.Default.DarkMode
                        }
                        Button(
                            onClick = { onThemeChange(mode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) BirbankRed else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
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
    customerName: String = "",
    customerPhone: String = "",
    onManageClick: () -> Unit,
    onViewClick: () -> Unit,
    onChatClick: (() -> Unit)? = null,
    onContactClick: (() -> Unit)? = null
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
                    color = BirbankRed
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

            if (customerName.isNotBlank() || customerPhone.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(13.dp))
                    Text(
                        text = "$customerName ${if (customerPhone.isNotBlank()) "• $customerPhone" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${order.university} • ${order.deadline}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onViewClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Text("Bax", style = MaterialTheme.typography.labelSmall)
                }

                if (onContactClick != null) {
                    OutlinedButton(
                        onClick = onContactClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BirbankRed),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.SendToMobile, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("SMS / WA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (onChatClick != null) {
                    OutlinedButton(
                        onClick = onChatClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Çat", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Button(
                    onClick = onManageClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("İdarə", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactCustomerBottomSheet(
    customerName: String,
    initialPhone: String,
    orderNumber: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf(initialPhone) }
    
    val defaultText = if (orderNumber != null) {
        "Hörmətli $customerName, #$orderNumber nömrəli sifarişinizlə bağlı BestGroup.az komandasından yazırıq."
    } else {
        "Hörmətli $customerName, BestGroup.az müştəri xidmətlərindən müraciətinizlə bağlı yazırıq."
    }
    var messageText by remember { mutableStateOf(defaultText) }

    val templates = listOf(
        "Təsdiq" to if (orderNumber != null) "Hörmətli $customerName, #$orderNumber nömrəli sifarişiniz qeydə alındı və icraya başlanıldı." else "Hörmətli $customerName, müraciətiniz qeydə alındı.",
        "Məlumat" to if (orderNumber != null) "Hörmətli $customerName, #$orderNumber nömrəli sifarişinizlə bağlı əlavə məlumata ehtiyac var. Zəhmət olmasa bizimlə əlaqə saxlayın." else "Hörmətli $customerName, əlavə məlumata ehtiyac var.",
        "Hazırdır" to if (orderNumber != null) "Hörmətli $customerName, şad xəbər! #$orderNumber nömrəli sifarişiniz tam hazırdır. Tətbiqdən təhvil ala bilərsiniz." else "Hörmətli $customerName, işiniz hazırdır."
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Müştəri ilə Əlaqə",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$customerName ${if (orderNumber != null) "(#$orderNumber)" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BirbankRed
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Bağla")
                }
            }

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Telefon nömrəsi") },
                placeholder = { Text("+994 50 123 45 67") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BirbankRed) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                text = "Sürətli Şablonlar:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                templates.forEach { (label, text) ->
                    SuggestionChip(
                        onClick = { messageText = text },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Mesaj mətni") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // SMS Button
                Button(
                    onClick = {
                        val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
                        if (cleanPhone.isBlank()) {
                            Toast.makeText(context, "Telefon nömrəsi daxil edilməyib", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        try {
                            val smsUri = Uri.parse("smsto:$cleanPhone")
                            val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                                putExtra("sms_body", messageText)
                            }
                            context.startActivity(intent)
                            onDismiss()
                        } catch (e: Exception) {
                            Toast.makeText(context, "SMS tətbiqi açıla bilmədi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SMS ilə aç", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // WhatsApp Button
                Button(
                    onClick = {
                        val rawPhone = phoneNumber.filter { it.isDigit() }
                        if (rawPhone.isBlank()) {
                            Toast.makeText(context, "Telefon nömrəsi daxil edilməyib", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val formattedPhone = when {
                            rawPhone.startsWith("994") -> rawPhone
                            rawPhone.startsWith("0") -> "994" + rawPhone.substring(1)
                            else -> rawPhone
                        }
                        try {
                            val encodedText = URLEncoder.encode(messageText, "UTF-8")
                            val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedText")
                            val intent = Intent(Intent.ACTION_VIEW, waUri)
                            context.startActivity(intent)
                            onDismiss()
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp açıla bilmədi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun AdminUpdatesContent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allUpdates by viewModel.allAppUpdates.collectAsState()
    val context = LocalContext.current
    var showPublishDialog by remember { mutableStateOf(false) }
    var newVersionCode by remember { mutableStateOf("") }
    var newVersionName by remember { mutableStateOf("") }
    var newReleaseNotesAz by remember { mutableStateOf("") }
    var newDownloadUrl by remember { mutableStateOf("") }
    var newApkSizeMb by remember { mutableStateOf("") }
    var isMandatory by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BirbankBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(BirbankRedLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = BirbankRed)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Tətbiq Yeniləmələri", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = BirbankTextPrimary)
                                Text("GitHub Releases & OTA Dağıtımı", style = MaterialTheme.typography.bodySmall, color = BirbankTextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Müştərilərə yeni versiya haqqında bildiriş və in-app yeniləmə dialoqu göndərmək üçün yeni versiya elan edin və ya GitHub Releases səhifəsinə keçin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BirbankTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPublishDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Yeni versiya elan et", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rzayevnicat00-sys/BestGroup.az/releases"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            border = BorderStroke(1.dp, BirbankBorder),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BirbankTextPrimary)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = BirbankRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GitHub", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Yayımlanmış versiyalar (${allUpdates.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = BirbankTextPrimary
            )
        }

        if (allUpdates.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BirbankBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = BirbankTextSecondary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Hələ yayımlanmış versiya elanı yoxdur", fontWeight = FontWeight.SemiBold, color = BirbankTextPrimary)
                        Text("Yeni versiya yaratmaq üçün yuxarıdakı düymədən istifadə edin.", style = MaterialTheme.typography.bodySmall, color = BirbankTextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(allUpdates) { update ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BirbankBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BirbankRedLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("v${update.versionName}", fontWeight = FontWeight.Bold, color = BirbankTextPrimary)
                                    Text("Build: ${update.versionCode} • ${String.format("%.1f", update.apkSizeMb)} MB", style = MaterialTheme.typography.labelSmall, color = BirbankTextSecondary)
                                }
                            }
                            if (update.isMandatory) {
                                Surface(
                                    color = BirbankRedLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Məcburi", color = BirbankRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }

                        if (update.releaseNotesAz.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(update.releaseNotesAz, style = MaterialTheme.typography.bodySmall, color = BirbankTextSecondary)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (update.downloadUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl))
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        try { context.startActivity(intent) } catch (_: Exception) {}
                                    },
                                    border = BorderStroke(1.dp, BirbankBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp), tint = BirbankRed)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("APK Yüklə", fontSize = 11.sp, color = BirbankTextPrimary)
                                }
                            }
                            if (update.githubReleaseUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.githubReleaseUrl))
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        try { context.startActivity(intent) } catch (_: Exception) {}
                                    },
                                    border = BorderStroke(1.dp, BirbankBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp), tint = BirbankRed)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GitHub", fontSize = 11.sp, color = BirbankTextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPublishDialog) {
        AlertDialog(
            onDismissRequest = { showPublishDialog = false },
            title = { Text("Yeni Versiya Elan Et", fontWeight = FontWeight.Bold, color = BirbankTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newVersionCode,
                        onValueChange = { newVersionCode = it },
                        label = { Text("Version Code (məsələn: 2)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = newVersionName,
                        onValueChange = { newVersionName = it },
                        label = { Text("Version Name (məsələn: 1.1.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = newReleaseNotesAz,
                        onValueChange = { newReleaseNotesAz = it },
                        label = { Text("Dəyişikliklər (Qeydlər)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = newDownloadUrl,
                        onValueChange = { newDownloadUrl = it },
                        label = { Text("APK Yükləmə URL-i (GitHub və ya birbaşa)") },
                        placeholder = { Text("https://github.com/rzayevnicat00-sys/BestGroup.az/releases/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = newApkSizeMb,
                        onValueChange = { newApkSizeMb = it },
                        label = { Text("APK Həcmi MB (məsələn: 28.5)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { isMandatory = !isMandatory }
                    ) {
                        Checkbox(checked = isMandatory, onCheckedChange = { isMandatory = it }, colors = CheckboxDefaults.colors(checkedColor = BirbankRed))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bu yeniləmə məcburidir", fontSize = 13.sp, color = BirbankTextPrimary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = newVersionCode.trim().toIntOrNull() ?: 2
                        val name = newVersionName.trim().ifBlank { "1.1.0" }
                        val url = newDownloadUrl.trim().ifBlank { "https://github.com/rzayevnicat00-sys/BestGroup.az/releases/latest" }
                        val size = newApkSizeMb.trim().toDoubleOrNull() ?: 25.0
                        viewModel.adminPublishUpdate(
                            versionCode = code,
                            versionName = name,
                            releaseNotesAz = newReleaseNotesAz.trim().ifBlank { "BestGroup.az tətbiqinin yeni versiyası yayımlandı." },
                            downloadUrl = url,
                            githubReleaseUrl = "https://github.com/rzayevnicat00-sys/BestGroup.az/releases",
                            isMandatory = isMandatory,
                            minSupportedVersionCode = if (isMandatory) code else 1,
                            apkSizeMb = size
                        )
                        showPublishDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Elan et və Yayımla", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishDialog = false }) {
                    Text("Ləğv et", color = BirbankTextSecondary)
                }
            }
        )
    }
}
