package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.ChatConversation
import com.example.model.ChatMessage
import com.example.model.ChatMessageType
import com.example.model.MessageDeliveryStatus
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.BottomTab
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState()
    val allMessages by viewModel.messages.collectAsState()
    val activeConvId by viewModel.activeConversationId.collectAsState()

    var showConversationsList by remember { mutableStateOf(false) }
    var inputMessageText by remember { mutableStateOf("") }
    val isSending = viewModel.isSendingChatMessage.value
    val isChatLoading = viewModel.isChatLoading.value
    val chatError = viewModel.chatErrorMessage.value

    val currentMessages = allMessages[activeConvId] ?: emptyList()
    val activeConversation = conversations.find { it.id == activeConvId } ?: conversations.firstOrNull()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(activeConversation?.id) {
        if (activeConversation != null && activeConvId != activeConversation.id) {
            viewModel.setActiveConversation(activeConversation.id)
        }
    }

    LaunchedEffect(currentMessages.size) {
        if (currentMessages.isNotEmpty()) {
            listState.animateScrollToItem(currentMessages.size - 1)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        com.example.ui.components.BestGroupLogoBadge(size = 36)
                        Column {
                            Text(
                                text = activeConversation?.title ?: localizedString(StringKey.CHAT_TITLE),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (activeConversation != null) Color(0xFF10B981) else Color.Gray)
                                )
                                Text(
                                    text = activeConversation?.curatorName ?: if (isChatLoading) localizedString(StringKey.CHAT_LOADING) else "Fəal söhbət yoxdur",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showConversationsList = !showConversationsList },
                        modifier = Modifier.testTag("chat_switch_conversation_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                val totalUnread = conversations.sumOf { it.unreadCount }
                                if (totalUnread > 0) {
                                    Badge(containerColor = Gold500, contentColor = Navy900) {
                                        Text("$totalUnread")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Forum, contentDescription = "Söhbətlər", tint = Gold500)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column {
                    // Chat error feedback banner
                    AnimatedVisibility(
                        visible = chatError != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = chatError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.clearChatError() }
                                ) {
                                    Text(localizedString(StringKey.FILE_ACTION_CANCEL), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Attachment button
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Fayl əlavəsi üçün Sifariş Detalları bölməsindən istifadə edə bilərsiniz.")
                                }
                            },
                            enabled = activeConversation != null,
                            modifier = Modifier.testTag("chat_attachment_btn")
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "Fayl qoş",
                                tint = if (activeConversation != null) Gold500 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        // Message input field
                        val hasActiveConv = activeConversation != null
                        OutlinedTextField(
                            value = inputMessageText,
                            onValueChange = {
                                if (it.length <= 4000) inputMessageText = it
                            },
                            enabled = hasActiveConv && !isChatLoading,
                            placeholder = {
                                Text(
                                    if (hasActiveConv) localizedString(StringKey.CHAT_INPUT_HINT)
                                    else "Fəal söhbət yoxdur"
                                )
                            },
                            singleLine = false,
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_text_field")
                        )

                        // Send Button
                        val canSend = hasActiveConv && inputMessageText.isNotBlank() && !isSending && !isChatLoading
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    val textToSend = inputMessageText.trim()
                                    inputMessageText = ""
                                    viewModel.sendChatMessage(textToSend) { success, _ ->
                                        if (!success) {
                                            inputMessageText = textToSend
                                        }
                                    }
                                }
                            },
                            enabled = canSend,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (canSend) Navy800 else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = Gold500,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("chat_send_button")
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Gold500
                                )
                            } else {
                                Icon(Icons.Default.Send, contentDescription = localizedString(StringKey.CHAT_SEND))
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Messages Feed
            if (isChatLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = Gold500)
                        Text(
                            text = localizedString(StringKey.CHAT_LOADING),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (activeConversation == null) {
                // Clear empty state when accessed directly without an active conversation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Gold500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Gold500,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "Fəal söhbət yoxdur",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Sifarişləriniz bölməsindən kuratorla əlaqəli söhbəti aça bilərsiniz.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.selectBottomTab(BottomTab.ORDERS) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Navy800,
                                contentColor = Gold500
                            )
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(localizedString(StringKey.NAV_ORDERS), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (currentMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Gold500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Gold500,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = localizedString(StringKey.CHAT_EMPTY_MESSAGES),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentMessages, key = { it.id }) { msg ->
                        ChatBubble(message = msg)
                    }
                }
            }

            // Conversations Switcher Drawer/Sheet
            if (showConversationsList) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.5.dp, Gold500, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Söhbətlər", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { showConversationsList = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Bağla")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        if (conversations.isEmpty()) {
                            Text(
                                text = "Aktiv söhbət tapılmadı.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            conversations.forEach { conv ->
                                val isSel = conv.id == activeConvId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                        .clickable {
                                            viewModel.setActiveConversation(conv.id)
                                            showConversationsList = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Gold500)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(conv.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text(conv.curatorName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (conv.unreadCount > 0) {
                                        Badge(containerColor = Gold500, contentColor = Navy900) {
                                            Text("${conv.unreadCount}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    if (message.messageType == ChatMessageType.SYSTEM) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        return
    }

    val isMe = message.isFromUser
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bgColor = if (isMe) Navy800 else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isMe) {
            Text(
                text = message.senderName.ifBlank { "Akademik Kurator" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(bgColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (message.attachedFileName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Gold500, modifier = Modifier.size(18.dp))
                        Text(
                            text = message.attachedFileName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Gold500
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (isMe) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isMe) {
                        val icon = when (message.status) {
                            MessageDeliveryStatus.PENDING -> Icons.Default.Schedule
                            MessageDeliveryStatus.READ -> Icons.Default.DoneAll
                            MessageDeliveryStatus.FAILED -> Icons.Default.ErrorOutline
                            else -> Icons.Default.Done
                        }
                        val tint = when (message.status) {
                            MessageDeliveryStatus.READ -> Gold500
                            MessageDeliveryStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> Color(0xFFCBD5E1)
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
