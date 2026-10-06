package com.example.services.firebase

import com.example.model.ChatConversation
import com.example.model.ChatMessage
import com.example.model.ChatMessageType
import com.example.model.MessageDeliveryStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FirestoreChatService(
    private val firestore: FirebaseFirestore
) {

    private val convCollection = firestore.collection("conversations")

    companion object {
        const val MAX_MESSAGE_LENGTH = 4000
        const val DEFAULT_MESSAGES_LIMIT = 50L

        fun deterministicOrderConversationId(orderId: String): String = "order_$orderId"
    }

    fun getConversationsFlow(userId: String, isStaff: Boolean): Flow<List<ChatConversation>> = callbackFlow {
        val query = if (isStaff) {
            convCollection.orderBy("updatedAt", Query.Direction.DESCENDING)
        } else {
            convCollection.whereEqualTo("customerId", userId)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Return gracefully without crashing, snapshot listener will retry on reconnect
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    mapDocumentToConversation(doc.id, doc.data ?: emptyMap())
                }
                trySend(list)
            }
        }
        awaitClose { registration.remove() }
    }

    fun getMessagesFlow(conversationId: String, limit: Long = DEFAULT_MESSAGES_LIMIT): Flow<List<ChatMessage>> = callbackFlow {
        if (conversationId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = convCollection.document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(limit)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // If orderBy('createdAt') fails because some older/pending local message lacks it,
                // we gracefully handle it
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val msgs = snapshot.documents.mapNotNull { doc ->
                    mapDocumentToMessage(doc.id, conversationId, doc.data ?: emptyMap())
                }
                trySend(msgs)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun getOrCreateConversationForOrder(
        orderId: String,
        orderNumber: String,
        topic: String,
        customerId: String,
        customerName: String = "Müştəri"
    ): ChatConversation {
        require(customerId.isNotBlank()) { "İstifadəçi təsdiqlənməyib." }
        require(orderId.isNotBlank()) { "Sifariş ID boş ola bilməz." }

        val convId = deterministicOrderConversationId(orderId)
        val docRef = convCollection.document(convId)
        val docSnap = docRef.get().await()

        if (docSnap.exists()) {
            return mapDocumentToConversation(convId, docSnap.data ?: emptyMap())
        }

        val nowFormatted = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val title = "$orderNumber Kuratorluğu"
        val greetingText = "Salam! #$orderNumber nömrəli '$topic' mövzulu sifarişiniz qəbul edildi. Təyin olunmuş akademik kuratorunuz buradan sizinlə əlaqədə olacaq."

        val convData = hashMapOf(
            "conversationId" to convId,
            "customerId" to customerId,
            "assignedStaffId" to null,
            "orderId" to orderId,
            "title" to title,
            "lastMessage" to "",
            "lastMessageText" to "",
            "lastMessageAt" to nowFormatted,
            "lastMessageSenderId" to null,
            "unreadForCustomer" to 0,
            "unreadForStaff" to 0,
            "status" to "active",
            "curatorName" to "Akademik Şura Kuratoru",
            "curatorRole" to "Elmi Məsləhətçi",
            "isOnline" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "createdIso" to isoNow
        )

        docRef.set(convData, SetOptions.merge()).await()

        return mapDocumentToConversation(convId, convData)
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        senderRole: String,
        text: String,
        messageType: ChatMessageType = ChatMessageType.TEXT,
        attachedFileName: String? = null,
        fileStoragePath: String? = null
    ): ChatMessage {
        val trimmed = text.trim()
        if (senderId.isBlank()) {
            throw IllegalStateException("Mesaj göndərmək üçün sistemə daxil olun.")
        }
        if (conversationId.isBlank()) {
            throw IllegalArgumentException("Söhbət tapılmadı.")
        }
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Boş mesaj göndərilə bilməz.")
        }
        if (trimmed.length > MAX_MESSAGE_LENGTH) {
            throw IllegalArgumentException("Mesaj mətni 4000 simvoldan çox ola bilməz.")
        }

        val dateFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val nowFormatted = dateFmt.format(Date())
        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val messageId = "msg_${UUID.randomUUID()}"

        val actualSenderId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: senderId

        // Ensure parent conversation exists in Firestore before creating a subcollection message
        val convRef = convCollection.document(conversationId)
        val convSnap = convRef.get().await()
        if (!convSnap.exists()) {
            val dateFmtInit = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
            val derivedOrderId = if (conversationId.startsWith("order_")) conversationId.removePrefix("order_") else null
            val newConvData = hashMapOf<String, Any?>(
                "conversationId" to conversationId,
                "customerId" to actualSenderId,
                "assignedStaffId" to null,
                "orderId" to derivedOrderId,
                "title" to "Akademik Dəstək",
                "lastMessage" to trimmed,
                "lastMessageText" to trimmed,
                "lastMessageAt" to dateFmtInit,
                "lastMessageSenderId" to actualSenderId,
                "unreadForCustomer" to 0,
                "unreadForStaff" to 1,
                "status" to "active",
                "curatorName" to "Akademik Şura Kuratoru",
                "curatorRole" to "Elmi Məsləhətçi",
                "isOnline" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "createdIso" to isoNow
            )
            convRef.set(newConvData).await()
        }

        val isFromCustomer = senderRole.equals("customer", ignoreCase = true)
        val targetRole = if (isFromCustomer) "customer" else "staff"

        val msgDoc = hashMapOf(
            "messageId" to messageId,
            "conversationId" to conversationId,
            "senderId" to actualSenderId,
            "senderRole" to targetRole,
            "senderName" to senderName,
            "text" to trimmed,
            "messageType" to messageType.name,
            "status" to "sent",
            "isRead" to false,
            "readAt" to null,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "timeFormatted" to nowFormatted,
            "attachedFileName" to attachedFileName,
            "fileStoragePath" to fileStoragePath
        )

        // 1. Write message to messages subcollection
        convCollection.document(conversationId).collection("messages").document(messageId).set(msgDoc).await()

        // 2. Update conversation header
        val convUpdate = hashMapOf<String, Any>(
            "lastMessage" to trimmed,
            "lastMessageText" to trimmed,
            "lastMessageAt" to nowFormatted,
            "lastMessageSenderId" to actualSenderId,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!isFromCustomer) {
            convUpdate["unreadForCustomer"] = FieldValue.increment(1)
        }

        convCollection.document(conversationId).update(convUpdate).await()

        return ChatMessage(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            senderRole = targetRole,
            senderName = senderName,
            text = trimmed,
            messageType = messageType,
            attachedFileName = attachedFileName,
            fileStoragePath = fileStoragePath,
            isFromUser = isFromCustomer,
            timestamp = nowFormatted,
            isRead = false,
            status = MessageDeliveryStatus.SENT,
            createdAt = isoNow,
            updatedAt = isoNow
        )
    }

    suspend fun markConversationAsRead(conversationId: String, isStaff: Boolean) {
        if (conversationId.isBlank()) return
        try {
            val convRef = convCollection.document(conversationId)
            val updates = hashMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isStaff) {
                updates["unreadForStaff"] = 0
            } else {
                updates["unreadForCustomer"] = 0
            }
            convRef.update(updates).await()

            // Update readAt on messages
            val unreadRole = if (isStaff) "customer" else "staff"
            val unreadDocs = convRef.collection("messages")
                .whereEqualTo("senderRole", unreadRole)
                .whereEqualTo("isRead", false)
                .limit(20)
                .get()
                .await()

            for (doc in unreadDocs.documents) {
                doc.reference.update(
                    mapOf(
                        "isRead" to true,
                        "readAt" to FieldValue.serverTimestamp(),
                        "status" to "read"
                    )
                ).await()
            }
        } catch (_: Exception) {}
    }

    private fun mapDocumentToConversation(id: String, data: Map<String, Any?>): ChatConversation {
        val title = data["title"] as? String ?: "Söhbət"
        val orderId = data["orderId"] as? String
        val customerId = data["customerId"] as? String ?: (data["userId"] as? String ?: "")
        val assignedStaffId = data["assignedStaffId"] as? String
        val lastMsg = data["lastMessage"] as? String ?: (data["lastMessageText"] as? String ?: "")
        val lastMsgTime = data["lastMessageAt"] as? String ?: (data["lastMessageTime"] as? String ?: "İndicə")
        val lastMsgSenderId = data["lastMessageSenderId"] as? String ?: ""
        val unreadCust = (data["unreadForCustomer"] as? Number)?.toInt()
            ?: ((data["unreadCount"] as? Number)?.toInt() ?: 0)
        val unreadStf = (data["unreadForStaff"] as? Number)?.toInt() ?: 0
        val status = data["status"] as? String ?: "active"
        val curatorName = data["curatorName"] as? String ?: "Akademik Kurator"
        val curatorRole = data["curatorRole"] as? String ?: "Elmi Məsləhətçi"
        val isOnline = data["isOnline"] as? Boolean ?: true

        return ChatConversation(
            id = id,
            customerId = customerId,
            assignedStaffId = assignedStaffId,
            orderId = orderId,
            title = title,
            lastMessage = lastMsg,
            lastMessageAt = lastMsgTime,
            lastMessageSenderId = lastMsgSenderId,
            unreadForCustomer = unreadCust,
            unreadForStaff = unreadStf,
            status = status,
            curatorName = curatorName,
            curatorRole = curatorRole,
            isOnline = isOnline
        )
    }

    private fun mapDocumentToMessage(id: String, conversationId: String, data: Map<String, Any?>): ChatMessage {
        val senderId = data["senderId"] as? String ?: ""
        val senderRole = data["senderRole"] as? String ?: (if (data["isFromUser"] == true) "customer" else "staff")
        val senderName = data["senderName"] as? String ?: ""
        val text = data["text"] as? String ?: ""
        val isFromUser = senderRole.equals("customer", ignoreCase = true)
        val timeFormatted = data["timeFormatted"] as? String ?: (data["timestamp"] as? String ?: "")
        val isRead = data["isRead"] as? Boolean ?: false
        val attachedFileName = data["attachedFileName"] as? String
        val fileStoragePath = data["fileStoragePath"] as? String
        val msgTypeStr = data["messageType"] as? String ?: "TEXT"
        val messageType = try {
            ChatMessageType.valueOf(msgTypeStr.uppercase())
        } catch (_: Exception) {
            ChatMessageType.TEXT
        }
        val statusStr = data["status"] as? String ?: (if (isRead) "read" else "sent")
        val status = when (statusStr.lowercase()) {
            "pending" -> MessageDeliveryStatus.PENDING
            "read" -> MessageDeliveryStatus.READ
            "failed" -> MessageDeliveryStatus.FAILED
            else -> MessageDeliveryStatus.SENT
        }

        return ChatMessage(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            senderRole = senderRole,
            senderName = senderName,
            text = text,
            messageType = messageType,
            attachedFileName = attachedFileName,
            fileStoragePath = fileStoragePath,
            isFromUser = isFromUser,
            timestamp = timeFormatted,
            isRead = isRead,
            status = status
        )
    }
}
