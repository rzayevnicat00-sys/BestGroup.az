package com.example.model

import com.example.localization.StringKey

enum class OrderStatus(val labelKey: StringKey) {
    PENDING(StringKey.STATUS_PENDING),
    ACCEPTED(StringKey.STATUS_ACCEPTED),
    IN_PROGRESS(StringKey.STATUS_IN_PROGRESS),
    READY(StringKey.STATUS_READY),
    CANCELLED(StringKey.STATUS_CANCELLED)
}

enum class OrderPriority(val labelKey: StringKey) {
    NORMAL(StringKey.WIZARD_PRIORITY_NORMAL),
    HIGH(StringKey.WIZARD_PRIORITY_HIGH),
    URGENT(StringKey.WIZARD_PRIORITY_URGENT)
}

data class OrderStatusHistoryItem(
    val status: OrderStatus,
    val timestamp: String,
    val note: String,
    val actor: String = "Sistem"
)

enum class FileUploadState {
    PENDING,
    UPLOADING,
    SUCCESS,
    UPLOADED,
    FAILED,
    ERROR,
    CANCELLED
}

data class OrderAttachedFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val extension: String,
    val progress: Float = 1.0f,
    val state: FileUploadState = FileUploadState.UPLOADED,
    val originalFileName: String = name,
    val storagePath: String = "",
    val downloadUrl: String? = null,
    val mimeType: String = "",
    val uploadedAt: String = "",
    val uploadedBy: String = "",
    val localUri: String? = null,
    val errorMessage: String? = null
) {
    val fileId: String get() = id
    val status: String get() = when (state) {
        FileUploadState.PENDING -> "pending"
        FileUploadState.UPLOADING -> "uploading"
        FileUploadState.SUCCESS, FileUploadState.UPLOADED -> "uploaded"
        FileUploadState.FAILED, FileUploadState.ERROR -> "failed"
        FileUploadState.CANCELLED -> "cancelled"
    }

    val isSuccess: Boolean get() = state == FileUploadState.SUCCESS || state == FileUploadState.UPLOADED
    val isFailed: Boolean get() = state == FileUploadState.FAILED || state == FileUploadState.ERROR
    val isUploading: Boolean get() = state == FileUploadState.UPLOADING
    val isPending: Boolean get() = state == FileUploadState.PENDING

    val sizeFormatted: String
        get() {
            val kb = sizeBytes / 1024.0
            return if (kb > 1024) {
                String.format(java.util.Locale.US, "%.1f MB", kb / 1024)
            } else {
                String.format(java.util.Locale.US, "%.0f KB", kb)
            }
        }
}

data class OrderReview(
    val rating: Int, // 1 to 5
    val comment: String,
    val submittedAt: String
)

data class Order(
    val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val serviceId: String = "diploma",
    val serviceName: String = "Diplom işi",
    val serviceType: ServiceType = ServiceType.DIPLOMA,
    val topic: String = "",
    val scopeDescription: String = "",
    val university: String = "",
    val faculty: String = "",
    val academicLevel: String = "",
    val language: String = "Azərbaycan dili",
    val pageCount: Int = 0,
    val deadline: String = "",
    val priority: OrderPriority = OrderPriority.NORMAL,
    val formattingStandard: String = "APA 7th",
    val specialNotes: String = "",
    val additionalNotes: String = specialNotes,
    val status: OrderStatus = OrderStatus.PENDING,
    val progressPercent: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val files: List<OrderAttachedFile> = emptyList(),
    val statusHistory: List<OrderStatusHistoryItem> = emptyList(),
    val review: OrderReview? = null,
    val estimatedPriceAzn: Int = 0
) {
    val orderId: String get() = id
    val subject: String get() = topic
    val educationLevel: String get() = academicLevel
}
