package com.example.model

enum class ChatMessageType {
    TEXT,
    SYSTEM,
    FILE
}

enum class MessageDeliveryStatus {
    PENDING,
    SENT,
    READ,
    FAILED
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderName: String = "",
    val isFromUser: Boolean = true,
    val text: String = "",
    val timestamp: String = "",
    val isRead: Boolean = false,
    val attachedFileName: String? = null,
    val fileStoragePath: String? = null,
    val senderId: String = "",
    val senderRole: String = if (isFromUser) "customer" else "staff",
    val messageType: ChatMessageType = ChatMessageType.TEXT,
    val status: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
    val createdAt: String = "",
    val updatedAt: String = "",
    val readAt: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val readAtMillis: Long? = null
) {
    val messageId: String get() = id
}

data class ChatConversation(
    val id: String,
    val title: String,
    val orderId: String? = null,
    val customerId: String = "",
    val assignedStaffId: String? = null,
    val lastMessage: String = "",
    val lastMessageAt: String = "",
    val lastMessageSenderId: String = "",
    val unreadForCustomer: Int = 0,
    val unreadForStaff: Int = 0,
    val status: String = "active",
    val curatorName: String = "Akademik Kurator",
    val curatorRole: String = "Elmi Məsləhətçi",
    val isOnline: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val lastMessageText: String = lastMessage,
    val lastMessageTime: String = if (lastMessageAt.isNotBlank()) lastMessageAt else "İndicə",
    val unreadCount: Int = unreadForCustomer
) {
    val conversationId: String get() = id
}
