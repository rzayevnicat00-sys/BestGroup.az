package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Gold500
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.OrdersFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.orders.collectAsState()
    val activeFilter by viewModel.ordersFilter.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredOrders = remember(orders, activeFilter, searchQuery) {
        orders.filter { order ->
            val matchesFilter = when (activeFilter) {
                OrdersFilter.ALL -> true
                OrdersFilter.ACTIVE -> order.status in listOf(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS)
                OrdersFilter.COMPLETED -> order.status == OrderStatus.READY
                OrdersFilter.CANCELLED -> order.status == OrderStatus.CANCELLED
            }
            val matchesSearch = searchQuery.isBlank() ||
                    order.orderNumber.contains(searchQuery, ignoreCase = true) ||
                    order.topic.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

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
                            text = localizedString(StringKey.ORDERS_TITLE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startNewOrder() },
                        modifier = Modifier.testTag("orders_add_new_order_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Yeni Sifariş", tint = Gold500)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(localizedString(StringKey.ORDERS_SEARCH_HINT)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Gold500) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("orders_search_field")
            )

            // Filter Tabs (Bütün, Aktiv, Tamamlanmış, Ləğv edilmiş)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTabChip(
                    titleKey = StringKey.ORDERS_FILTER_ALL,
                    isSelected = activeFilter == OrdersFilter.ALL,
                    onClick = { viewModel.setOrdersFilter(OrdersFilter.ALL) }
                )
                FilterTabChip(
                    titleKey = StringKey.ORDERS_FILTER_ACTIVE,
                    isSelected = activeFilter == OrdersFilter.ACTIVE,
                    onClick = { viewModel.setOrdersFilter(OrdersFilter.ACTIVE) }
                )
                FilterTabChip(
                    titleKey = StringKey.ORDERS_FILTER_COMPLETED,
                    isSelected = activeFilter == OrdersFilter.COMPLETED,
                    onClick = { viewModel.setOrdersFilter(OrdersFilter.COMPLETED) }
                )
                FilterTabChip(
                    titleKey = StringKey.ORDERS_FILTER_CANCELLED,
                    isSelected = activeFilter == OrdersFilter.CANCELLED,
                    onClick = { viewModel.setOrdersFilter(OrdersFilter.CANCELLED) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Orders List or Empty State
            if (filteredOrders.isEmpty()) {
                EmptyStateView(
                    titleKey = StringKey.EMPTY_ORDERS_TITLE,
                    descKey = StringKey.EMPTY_ORDERS_DESC,
                    actionButtonTextKey = StringKey.BTN_NEW_ORDER,
                    onActionClick = { viewModel.startNewOrder() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("orders_list")
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        OrderItemCard(
                            order = order,
                            onClick = { viewModel.viewOrderDetail(order) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabChip(
    titleKey: StringKey,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = localizedString(titleKey),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    )
}

@Composable
private fun OrderItemCard(
    order: Order,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("order_item_${order.id}"),
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = order.topic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = localizedString(order.serviceType.nameKey),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { order.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
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
                        text = "${localizedString(StringKey.ORDER_DEADLINE)}: ${order.deadline}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PriorityBadge(priority = order.priority)
            }
        }
    }
}
