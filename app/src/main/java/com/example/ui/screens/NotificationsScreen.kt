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
import com.example.model.NotificationCategory
import com.example.model.NotificationItem
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    val orders by viewModel.orders.collectAsState()
    var selectedCategory by remember { mutableStateOf(NotificationCategory.ALL) }

    val filteredList = remember(notifications, selectedCategory) {
        if (selectedCategory == NotificationCategory.ALL) {
            notifications
        } else {
            notifications.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = localizedString(StringKey.NOTIF_TITLE),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.markNotificationsAsRead() },
                        modifier = Modifier.testTag("notif_mark_all_read_btn")
                    ) {
                        Text(
                            text = localizedString(StringKey.NOTIF_MARK_ALL_READ),
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold500,
                            fontWeight = FontWeight.SemiBold
                        )
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
            // Category Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotifCategoryChip(
                    titleKey = StringKey.NOTIF_CAT_ALL,
                    isSelected = selectedCategory == NotificationCategory.ALL,
                    onClick = { selectedCategory = NotificationCategory.ALL }
                )
                NotifCategoryChip(
                    titleKey = StringKey.NOTIF_CAT_ORDERS,
                    isSelected = selectedCategory == NotificationCategory.ORDERS,
                    onClick = { selectedCategory = NotificationCategory.ORDERS }
                )
                NotifCategoryChip(
                    titleKey = StringKey.NOTIF_CAT_MESSAGES,
                    isSelected = selectedCategory == NotificationCategory.MESSAGES,
                    onClick = { selectedCategory = NotificationCategory.MESSAGES }
                )
                NotifCategoryChip(
                    titleKey = StringKey.NOTIF_CAT_CAMPAIGNS,
                    isSelected = selectedCategory == NotificationCategory.CAMPAIGNS,
                    onClick = { selectedCategory = NotificationCategory.CAMPAIGNS }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredList.isEmpty()) {
                EmptyStateView(
                    titleKey = StringKey.EMPTY_NOTIF_TITLE,
                    descKey = StringKey.EMPTY_NOTIF_DESC,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notifications_list")
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            onClick = {
                                if (item.targetOrderId != null) {
                                    val matchedOrder = orders.find { it.id == item.targetOrderId }
                                    if (matchedOrder != null) {
                                        viewModel.viewOrderDetail(matchedOrder)
                                    }
                                } else if (item.targetChatId != null) {
                                    viewModel.setActiveConversation(item.targetChatId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotifCategoryChip(
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
private fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit
) {
    val (icon, tint) = when (item.category) {
        NotificationCategory.ORDERS -> Icons.Default.Assignment to Gold500
        NotificationCategory.MESSAGES -> Icons.Default.Chat to Color(0xFF3B82F6)
        NotificationCategory.CAMPAIGNS -> Icons.Default.Campaign to Color(0xFF10B981)
        NotificationCategory.SYSTEM, NotificationCategory.ALL -> Icons.Default.Notifications to Gold500
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (!item.isRead) Gold500.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .testTag("notif_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Navy800),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
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
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Gold500)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = Gold500
                )
            }
        }
    }
}
