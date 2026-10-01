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
    val senderId: String = "",
    val senderRole: String = "customer", // customer, staff, system
    val senderName: String = "",
    val text: String,
    val messageType: ChatMessageType = ChatMessageType.TEXT,
    val attachedFileName: String? = null,
    val fileStoragePath: String? = null,
    val isFromUser: Boolean = (senderRole == "customer"),
    val timestamp: String = "",
    val isRead: Boolean = false,
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
    val customerId: String = "",
    val assignedStaffId: String? = null,
    val orderId: String? = null,
    val title: String,
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
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val conversationId: String get() = id
    val lastMessageText: String get() = lastMessage
    val lastMessageTime: String get() = lastMessageAt.ifBlank { "İndicə" }
    val unreadCount: Int get() = unreadForCustomer
}
