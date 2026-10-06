package com.example.services.firebase

import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class FirestoreOrderService(
    private val firestore: FirebaseFirestore
) {

    private val ordersCollection = firestore.collection("orders")

    fun getOrdersFlow(userId: String, isStaff: Boolean): Flow<List<Order>> = callbackFlow {
        val query = if (isStaff) {
            ordersCollection
        } else {
            ordersCollection.whereEqualTo("userId", userId)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val ordersList = snapshot.documents.mapNotNull { doc ->
                    mapDocumentToOrder(doc.id, doc.data ?: emptyMap())
                }.sortedByDescending { it.createdAt }
                trySend(ordersList)
            }
        }

        awaitClose { registration.remove() }
    }

    suspend fun getOrderById(orderId: String): Order? {
        val doc = ordersCollection.document(orderId).get().await()
        return if (doc.exists()) {
            mapDocumentToOrder(doc.id, doc.data ?: emptyMap())
        } else null
    }

    suspend fun createOrder(
        userId: String,
        serviceId: String,
        serviceName: String,
        serviceType: ServiceType,
        topic: String,
        scopeDescription: String,
        university: String,
        faculty: String,
        academicLevel: String,
        language: String,
        pageCount: Int,
        deadline: String,
        priority: OrderPriority,
        formattingStandard: String,
        specialNotes: String,
        files: List<OrderAttachedFile>,
        serviceCatalogItem: ServiceCatalogItem? = null
    ): Order {
        if (userId.isBlank()) {
            throw IllegalStateException("İstifadəçi daxil olmayıb. Sifariş yaratmaq üçün giriş edin.")
        }
        if (topic.isBlank()) {
            throw IllegalArgumentException("Mövzu sahəsi məcburidir.")
        }
        if (university.isBlank()) {
            throw IllegalArgumentException("Universitet sahəsi məcburidir.")
        }
        if (language.isBlank()) {
            throw IllegalArgumentException("Tədris dili sahəsi məcburidir.")
        }

        // Validate deadline is not in the past
        validateDeadlineNotInPast(deadline)

        // Validate service is active
        if (serviceCatalogItem != null && !serviceCatalogItem.active) {
            throw IllegalStateException("Seçilmiş xidmət hazırda aktiv deyil.")
        }

        val dateFmt = SimpleDateFormat("dd MMMM yyyy", Locale("az"))
        val now = dateFmt.format(Date())
        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val orderNum = "#BG-${SimpleDateFormat("yyyy", Locale.US).format(Date())}-${(1000..9999).random()}"
        val docRef = ordersCollection.document()
        val orderId = docRef.id

        val filesData = files.map { file ->
            val ext = file.extension.ifBlank { FirebaseStorageService.extractExtension(file.name) }
            val safeStoragePath = file.storagePath.ifBlank {
                "users/$userId/orders/$orderId/files/${file.id}.$ext"
            }
            mapOf(
                "id" to file.id,
                "name" to file.name,
                "originalFileName" to file.originalFileName,
                "sizeBytes" to file.sizeBytes,
                "extension" to ext,
                "storagePath" to safeStoragePath,
                "downloadUrl" to (file.downloadUrl ?: ""),
                "mimeType" to file.mimeType,
                "uploadedAt" to file.uploadedAt.ifBlank { isoNow },
                "uploadedBy" to file.uploadedBy.ifBlank { userId },
                "status" to file.status
            )
        }

        val initialHistory = listOf(
            mapOf(
                "status" to "waiting",
                "timestamp" to "$now, 10:00",
                "note" to "Sifariş qəbul edildi və nəzərdən keçirilməyə göndərildi.",
                "actor" to "Sistem",
                "changedBy" to userId,
                "changedAt" to isoNow
            )
        )

        // Secure server-side pricing logic: client cannot inject custom price
        val calculatedEstimatedPrice = when {
            serviceCatalogItem != null -> {
                when {
                    serviceCatalogItem.priceType.equals("quote", ignoreCase = true) -> 0
                    serviceCatalogItem.unit.equals("page", ignoreCase = true) || serviceCatalogItem.priceUnit.equals("page", ignoreCase = true) -> {
                        // Sərbəst iş / Referat / Esse: 0.50 AZN per page (minimum 1 AZN if pageCount >= 2)
                        val total = serviceCatalogItem.startingPriceAzn * (if (pageCount > 0) pageCount else 1)
                        Math.max(1, Math.round(total).toInt())
                    }
                    else -> serviceCatalogItem.startingPriceAzn.toInt()
                }
            }
            serviceType == ServiceType.INDEPENDENT_WORK || serviceType == ServiceType.REPORT || serviceType == ServiceType.ESSAY -> {
                Math.max(1, Math.round(0.50 * (if (pageCount > 0) pageCount else 1)).toInt())
            }
            else -> serviceType.startingPriceAzn
        }

        val resolvedServiceName = serviceCatalogItem?.nameAz?.ifBlank { serviceName } ?: serviceName

        val orderDoc = hashMapOf(
            "orderId" to orderId,
            "orderNumber" to orderNum,
            "userId" to userId,
            "serviceId" to serviceId.ifBlank { serviceType.name.lowercase() },
            "serviceName" to resolvedServiceName,
            "subject" to topic,
            "scopeDescription" to scopeDescription,
            "university" to university,
            "faculty" to faculty,
            "educationLevel" to academicLevel,
            "language" to language,
            "pageCount" to pageCount,
            "deadline" to deadline,
            "priority" to priority.name.lowercase(),
            "formattingStandard" to formattingStandard,
            "additionalNotes" to specialNotes,
            "notes" to specialNotes,
            "status" to "waiting",
            "progressPercent" to 15,
            "estimatedPriceAzn" to calculatedEstimatedPrice,
            "files" to filesData,
            "statusHistory" to initialHistory,
            "createdAt" to isoNow,
            "updatedAt" to isoNow
        )

        docRef.set(orderDoc).await()

        return Order(
            id = orderId,
            orderNumber = orderNum,
            userId = userId,
            serviceId = serviceId.ifBlank { serviceType.name.lowercase() },
            serviceName = resolvedServiceName,
            serviceType = serviceType,
            topic = topic,
            scopeDescription = scopeDescription,
            university = university,
            faculty = faculty,
            academicLevel = academicLevel,
            language = language,
            pageCount = pageCount,
            deadline = deadline,
            priority = priority,
            formattingStandard = formattingStandard,
            specialNotes = specialNotes,
            additionalNotes = specialNotes,
            status = OrderStatus.PENDING,
            progressPercent = 15,
            createdAt = now,
            updatedAt = now,
            files = files,
            statusHistory = listOf(
                OrderStatusHistoryItem(
                    status = OrderStatus.PENDING,
                    timestamp = "$now, 10:00",
                    note = "Sifariş qəbul edildi və nəzərdən keçirilməyə göndərildi.",
                    actor = "Sistem"
                )
            ),
            estimatedPriceAzn = calculatedEstimatedPrice
        )
    }

    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        note: String,
        actor: String
    ) {
        val dateFmt = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("az"))
        val now = dateFmt.format(Date())
        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val doc = ordersCollection.document(orderId).get().await()
        if (doc.exists()) {
            val statusString = when (newStatus) {
                OrderStatus.PENDING -> "waiting"
                OrderStatus.ACCEPTED -> "accepted"
                OrderStatus.IN_PROGRESS -> "in_progress"
                OrderStatus.READY -> "ready"
                OrderStatus.CANCELLED -> "cancelled"
            }
            val newProgress = when (newStatus) {
                OrderStatus.PENDING -> 15
                OrderStatus.ACCEPTED -> 35
                OrderStatus.IN_PROGRESS -> 70
                OrderStatus.READY -> 100
                OrderStatus.CANCELLED -> 0
            }

            @Suppress("UNCHECKED_CAST")
            val currentHistory = (doc.data?.get("statusHistory") as? List<Map<String, Any>>) ?: emptyList()
            val newHistoryItem = mapOf(
                "status" to statusString,
                "timestamp" to now,
                "note" to note,
                "actor" to actor,
                "changedAt" to isoNow
            )

            ordersCollection.document(orderId).update(
                mapOf(
                    "status" to statusString,
                    "progressPercent" to newProgress,
                    "statusHistory" to currentHistory + newHistoryItem,
                    "updatedAt" to isoNow
                )
            ).await()
        }
    }

    suspend fun updateOrderPriority(orderId: String, newPriority: OrderPriority) {
        ordersCollection.document(orderId).update("priority", newPriority.name.lowercase()).await()
    }

    suspend fun updateOrderFiles(orderId: String, files: List<OrderAttachedFile>) {
        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val filesData = files.map { file ->
            mapOf(
                "id" to file.id,
                "name" to file.name,
                "originalFileName" to file.originalFileName,
                "sizeBytes" to file.sizeBytes,
                "extension" to file.extension,
                "storagePath" to file.storagePath,
                "downloadUrl" to (file.downloadUrl ?: ""),
                "mimeType" to file.mimeType,
                "uploadedAt" to file.uploadedAt.ifBlank { isoNow },
                "uploadedBy" to file.uploadedBy,
                "status" to file.status
            )
        }
        ordersCollection.document(orderId).update(
            mapOf(
                "files" to filesData,
                "updatedAt" to isoNow
            )
        ).await()
    }

    suspend fun submitOrderReview(orderId: String, rating: Int, comment: String) {
        val dateFmt = SimpleDateFormat("dd MMMM yyyy", Locale("az"))
        val now = dateFmt.format(Date())
        val reviewMap = mapOf(
            "rating" to rating,
            "comment" to comment,
            "submittedAt" to now
        )
        ordersCollection.document(orderId).update("review", reviewMap).await()
    }

    companion object {
        fun validateDeadlineNotInPast(deadlineStr: String) {
            if (deadlineStr.isBlank()) return
            val formats = listOf(
                SimpleDateFormat("dd MMMM yyyy", Locale("az")),
                SimpleDateFormat("dd MMMM yyyy", Locale.US),
                SimpleDateFormat("yyyy-MM-dd", Locale.US),
                SimpleDateFormat("dd.MM.yyyy", Locale.US),
                SimpleDateFormat("dd/MM/yyyy", Locale.US)
            )
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val today = calendar.time

            for (fmt in formats) {
                try {
                    val parsed = fmt.parse(deadlineStr.trim())
                    if (parsed != null) {
                        if (parsed.before(today)) {
                            throw IllegalArgumentException("Son tarix keçmiş tarix ola bilməz.")
                        }
                        return
                    }
                } catch (e: IllegalArgumentException) {
                    throw e
                } catch (_: Exception) {}
            }
        }
    }

    private fun mapDocumentToOrder(docId: String, data: Map<String, Any>): Order? {
        return try {
            val serviceIdStr = data["serviceId"] as? String ?: "diploma"
            val serviceType = ServiceType.values().find {
                it.name.equals(serviceIdStr, ignoreCase = true) || it.name.replace("_", "").equals(serviceIdStr.replace("_", ""), ignoreCase = true)
            } ?: ServiceType.DIPLOMA

            val statusStr = data["status"] as? String ?: "waiting"
            val status = when (statusStr.lowercase()) {
                "accepted" -> OrderStatus.ACCEPTED
                "in_progress" -> OrderStatus.IN_PROGRESS
                "ready" -> OrderStatus.READY
                "cancelled" -> OrderStatus.CANCELLED
                else -> OrderStatus.PENDING
            }

            val priorityStr = data["priority"] as? String ?: "normal"
            val priority = when (priorityStr.lowercase()) {
                "high" -> OrderPriority.HIGH
                "urgent" -> OrderPriority.URGENT
                else -> OrderPriority.NORMAL
            }

            @Suppress("UNCHECKED_CAST")
            val filesListRaw = (data["files"] as? List<Map<String, Any>>) ?: emptyList()
            val files = filesListRaw.map { f ->
                val fStatusStr = f["status"] as? String ?: "uploaded"
                val fState = when (fStatusStr.lowercase()) {
                    "pending" -> FileUploadState.PENDING
                    "uploading" -> FileUploadState.UPLOADING
                    "failed", "error" -> FileUploadState.FAILED
                    "cancelled" -> FileUploadState.CANCELLED
                    else -> FileUploadState.UPLOADED
                }
                val rawName = f["name"] as? String ?: (f["originalFileName"] as? String ?: "fayl")
                val origName = f["originalFileName"] as? String ?: rawName
                val ext = f["extension"] as? String ?: FirebaseStorageService.extractExtension(origName).ifBlank { "pdf" }
                val downloadUrlRaw = (f["downloadUrl"] as? String)?.takeIf { it.isNotBlank() }
                OrderAttachedFile(
                    id = f["id"] as? String ?: UUID.randomUUID().toString(),
                    name = rawName,
                    originalFileName = origName,
                    sizeBytes = (f["sizeBytes"] as? Number)?.toLong() ?: 1024000L,
                    extension = ext,
                    progress = if (fState == FileUploadState.UPLOADED) 1.0f else 0.0f,
                    state = fState,
                    storagePath = f["storagePath"] as? String ?: "",
                    downloadUrl = downloadUrlRaw,
                    mimeType = f["mimeType"] as? String ?: "",
                    uploadedAt = f["uploadedAt"] as? String ?: "",
                    uploadedBy = f["uploadedBy"] as? String ?: ""
                )
            }

            @Suppress("UNCHECKED_CAST")
            val historyRaw = (data["statusHistory"] as? List<Map<String, Any>>) ?: emptyList()
            val history = historyRaw.map { h ->
                val hStatus = when ((h["status"] as? String)?.lowercase()) {
                    "accepted" -> OrderStatus.ACCEPTED
                    "in_progress" -> OrderStatus.IN_PROGRESS
                    "ready" -> OrderStatus.READY
                    "cancelled" -> OrderStatus.CANCELLED
                    else -> OrderStatus.PENDING
                }
                OrderStatusHistoryItem(
                    status = hStatus,
                    timestamp = h["timestamp"] as? String ?: (h["changedAt"] as? String ?: ""),
                    note = h["note"] as? String ?: "",
                    actor = h["actor"] as? String ?: "Sistem"
                )
            }

            @Suppress("UNCHECKED_CAST")
            val reviewRaw = data["review"] as? Map<String, Any>
            val review = if (reviewRaw != null) {
                OrderReview(
                    rating = (reviewRaw["rating"] as? Number)?.toInt() ?: 5,
                    comment = reviewRaw["comment"] as? String ?: "",
                    submittedAt = reviewRaw["submittedAt"] as? String ?: ""
                )
            } else null

            Order(
                id = docId,
                orderNumber = data["orderNumber"] as? String ?: "#BG-2026",
                userId = data["userId"] as? String ?: "",
                serviceId = serviceIdStr,
                serviceName = data["serviceName"] as? String ?: "Diplom işi",
                serviceType = serviceType,
                topic = data["subject"] as? String ?: "",
                scopeDescription = data["scopeDescription"] as? String ?: "",
                university = data["university"] as? String ?: "",
                faculty = data["faculty"] as? String ?: "",
                academicLevel = data["educationLevel"] as? String ?: "",
                language = data["language"] as? String ?: "Azərbaycan dili",
                pageCount = (data["pageCount"] as? Number)?.toInt() ?: 50,
                deadline = data["deadline"] as? String ?: "",
                priority = priority,
                formattingStandard = data["formattingStandard"] as? String ?: "APA 7th",
                specialNotes = data["additionalNotes"] as? String ?: (data["notes"] as? String ?: ""),
                additionalNotes = data["additionalNotes"] as? String ?: (data["notes"] as? String ?: ""),
                status = status,
                progressPercent = (data["progressPercent"] as? Number)?.toInt() ?: 15,
                createdAt = (data["createdAt"] as? String)?.take(10) ?: "",
                updatedAt = (data["updatedAt"] as? String)?.take(10) ?: "",
                files = files,
                statusHistory = history,
                review = review,
                estimatedPriceAzn = (data["estimatedPriceAzn"] as? Number)?.toInt() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }
}
