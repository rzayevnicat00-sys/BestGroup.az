package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatConversation
import com.example.ui.components.BestGroupLogoBadge
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.MainViewModel

private enum class AdminChatCategory(val label: String) {
    ALL("Hamısı"),
    SUPPORT("Müştəri Xidmətləri"),
    ORDERS("Sifariş Çatları")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState()
    val customers by viewModel.adminCustomers.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val activeConvId by viewModel.activeConversationId.collectAsState()

    var selectedConversation by remember { mutableStateOf<ChatConversation?>(null) }
    var selectedCategory by remember { mutableStateOf(AdminChatCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    // If active conversation ID is set in ViewModel, sync with selectedConversation
    LaunchedEffect(activeConvId, conversations) {
        if (activeConvId != null && selectedConversation?.id != activeConvId) {
            val matched = conversations.find { it.id == activeConvId }
            if (matched != null) {
                selectedConversation = matched
            }
        }
    }

    if (selectedConversation != null) {
        // Direct Live Chat view inside Admin profile
        AdminSupportChatDetailView(
            conversation = selectedConversation!!,
            viewModel = viewModel,
            customers = customers,
            modifier = modifier.fillMaxSize(),
            onBack = {
                selectedConversation = null
                viewModel.setActiveConversation("", navigateToCustomerChat = false)
            }
        )
    } else {
        val filteredConversations = remember(conversations, selectedCategory, searchQuery, customers, orders) {
            conversations.filter { conv ->
                val matchesCategory = when (selectedCategory) {
                    AdminChatCategory.ALL -> true
                    AdminChatCategory.SUPPORT -> conv.orderId == null
                    AdminChatCategory.ORDERS -> conv.orderId != null
                }
                val matchingUser = customers.find { it.id == conv.customerId }
                val matchingOrder = orders.find { it.id == conv.orderId }
                val searchLower = searchQuery.trim().lowercase()
                val matchesSearch = searchLower.isEmpty() ||
                        conv.title.lowercase().contains(searchLower) ||
                        conv.lastMessageText.lowercase().contains(searchLower) ||
                        matchingOrder?.orderNumber?.lowercase()?.contains(searchLower) == true ||
                        (matchingUser?.fullName?.lowercase()?.contains(searchLower) == true) ||
                        (matchingUser?.phone?.contains(searchLower) == true)

                matchesCategory && matchesSearch
            }.sortedByDescending { it.unreadCount > 0 }
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
                            BestGroupLogoBadge(size = 34)
                            Column {
                                Text(
                                    text = "Müştəri Çatları (Admin)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bütün canlı müraciətlər və sifariş söhbətləri",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Müştəri adı, mövzu və ya nömrə axtar...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Gold500) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Təmizlə")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .testTag("admin_chat_search_field")
                )

                // Category chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminChatCategory.values().forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val count = when (cat) {
                            AdminChatCategory.ALL -> conversations.size
                            AdminChatCategory.SUPPORT -> conversations.count { it.orderId == null }
                            AdminChatCategory.ORDERS -> conversations.count { it.orderId != null }
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.label} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold500,
                                selectedLabelColor = Navy900,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (filteredConversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Navy800),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Forum, contentDescription = null, tint = Gold500, modifier = Modifier.size(30.dp))
                                }
                                Text(
                                    text = "Aktiv müştəri söhbəti yoxdur",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "Müştərilər xidmətlər və ya sifarişləri ilə bağlı sual yazdıqda bütün canlı söhbətlər burada real vaxt rejimində əks olunacaq.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredConversations, key = { it.id }) { conv ->
                            val matchingUser = customers.find { it.id == conv.customerId }
                            val displayTitle = conv.title.ifBlank { matchingUser?.fullName ?: "Müştəri" }
                            val isOrderChat = conv.orderId != null
                            val matchingOrder = if (isOrderChat) orders.find { it.id == conv.orderId } else null
                            val subInfo = when {
                                isOrderChat && matchingOrder != null -> "${matchingOrder.orderNumber} • Sifariş Çatı"
                                matchingUser?.phone?.isNotBlank() == true -> "${matchingUser.phone} • Müştəri Xidmətləri"
                                else -> "Canlı Müştəri Sorğusu"
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedConversation = conv
                                        viewModel.setActiveConversation(conv.id, navigateToCustomerChat = false)
                                    }
                                    .border(
                                        1.dp,
                                        if (conv.unreadCount > 0) Gold500 else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        RoundedCornerShape(14.dp)
                                    ),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(if (isOrderChat) Gold500.copy(alpha = 0.2f) else Navy800),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isOrderChat) Icons.Default.Assignment else Icons.Default.SupportAgent,
                                            contentDescription = null,
                                            tint = Gold500
                                        )
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
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
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
                                            color = Gold500
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
                                        Badge(containerColor = Gold500, contentColor = Navy900) {
                                            Text("${conv.unreadCount}", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Aç",
                                        tint = Gold500
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
