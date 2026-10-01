package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.Order
import com.example.model.OrderPriority
import com.example.model.OrderStatus
import com.example.model.UserRole
import com.example.ui.components.MetricStatCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.AppDestination
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.adminStats.collectAsState()
    val orders by viewModel.orders.collectAsState()

    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var broadcastSentMessage by remember { mutableStateOf(false) }

    var selectedOrderForAdminAction by remember { mutableStateOf<Order?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = localizedString(StringKey.ADMIN_TITLE),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = localizedString(StringKey.ADMIN_BADGE),
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metrics Grid (2x2 pairs)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = localizedString(StringKey.ADMIN_TODAY_ORDERS),
                        value = "${stats.todayOrdersCount}",
                        subtitle = "+3 dünəndən",
                        icon = Icons.Default.Today,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = localizedString(StringKey.ADMIN_ACTIVE_ORDERS),
                        value = "${stats.activeOrdersCount}",
                        subtitle = "İcrada olan",
                        icon = Icons.Default.PendingActions,
                        iconColor = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = localizedString(StringKey.ADMIN_TOTAL_REVENUE),
                        value = "${stats.monthlyRevenueAzn} ₼",
                        subtitle = "Cari ay",
                        icon = Icons.Default.Payments,
                        iconColor = Gold500,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = localizedString(StringKey.ADMIN_NEW_USERS),
                        value = "${stats.newUsersCount}",
                        subtitle = "Bu həftə",
                        icon = Icons.Default.GroupAdd,
                        iconColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Broadcast Action Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showBroadcastDialog = true }
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

            // Orders Management Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sifarişlərin İdarə Edilməsi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${orders.size} sifariş",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold500,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(orders, key = { it.id }) { order ->
                AdminOrderManageCard(
                    order = order,
                    onManageClick = { selectedOrderForAdminAction = order },
                    onViewClick = { viewModel.viewOrderDetail(order) }
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
                            "Push bildiriş bütün aktiv istifadəçilərə uğurla göndərildi!",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it },
                            label = { Text(localizedString(StringKey.ADMIN_PUSH_TITLE)) },
                            placeholder = { Text("Məsələn: Təcili endirim kampaniyası") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = broadcastMessage,
                            onValueChange = { broadcastMessage = it },
                            label = { Text(localizedString(StringKey.ADMIN_PUSH_MSG)) },
                            placeholder = { Text("Bildirişin ətraflı məzmunu...") },
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

                Text("Statusu dəyiş:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                OrderStatus.values().forEach { st ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                viewModel.adminUpdateOrderStatus(o.id, st, "Admin statusu dəyişdi.")
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

@Composable
private fun AdminOrderManageCard(
    order: Order,
    onManageClick: () -> Unit,
    onViewClick: () -> Unit
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

                Button(
                    onClick = onManageClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("İdarə et", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
