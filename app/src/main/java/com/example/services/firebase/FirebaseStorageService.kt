package com.example.services.firebase

import android.net.Uri
import com.example.model.FileUploadState
import com.example.model.OrderAttachedFile
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseStorageService(
    private val storage: FirebaseStorage
) {

    private val activeUploads = ConcurrentHashMap<String, UploadTask>()

    companion object {
        const val MAX_FILE_SIZE_BYTES: Long = 20L * 1024L * 1024L // 20 MB
        const val MAX_FILES_PER_ORDER: Int = 10

        val SUPPORTED_EXTENSIONS = setOf(
            "pdf", "doc", "docx", "ppt", "pptx", "jpg", "jpeg", "png"
        )

        val SUPPORTED_MIME_TYPES = setOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "image/jpeg",
            "image/png"
        )

        fun sanitizeFileName(fileName: String): String {
            val base = fileName.substringAfterLast('/').substringAfterLast('\\')
            val sanitized = base.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            return if (sanitized.isBlank()) "file_${System.currentTimeMillis()}" else sanitized
        }

        fun extractExtension(fileName: String): String {
            return fileName.substringAfterLast('.', "").lowercase()
        }

        fun validateFile(fileName: String, sizeBytes: Long, mimeType: String? = null) {
            if (sizeBytes > MAX_FILE_SIZE_BYTES) {
                throw IllegalArgumentException("Fayl ölçüsü 20 MB-dan çox ola bilməz.")
            }
            if (sizeBytes <= 0) {
                throw IllegalArgumentException("Fayl boş ola bilməz.")
            }

            val ext = extractExtension(fileName)
            if (ext.isBlank() || !SUPPORTED_EXTENSIONS.contains(ext)) {
                throw IllegalArgumentException("Dəstəklənməyən fayl formatı. Yalnız PDF, DOC, DOCX, PPT, PPTX, JPG, PNG qəbul edilir.")
            }

            if (!mimeType.isNullOrBlank() && !mimeType.equals("application/octet-stream", ignoreCase = true)) {
                val normalizedMime = mimeType.trim().lowercase()
                if (!SUPPORTED_MIME_TYPES.contains(normalizedMime)) {
                    throw IllegalArgumentException("Dəstəklənməyən MIME type: $normalizedMime")
                }
            }
        }

        fun mapStorageError(e: Throwable): String {
            val msg = e.message ?: ""
            return when {
                e is StorageException -> when (e.errorCode) {
                    StorageException.ERROR_NOT_AUTHENTICATED,
                    StorageException.ERROR_NOT_AUTHORIZED -> "Bu fayla giriş hüququnuz yoxdur."
                    StorageException.ERROR_OBJECT_NOT_FOUND -> "Fayl artıq mövcud deyil və ya ona giriş mümkün deyil."
                    StorageException.ERROR_CANCELED -> "Yükləmə ləğv edildi."
                    StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> "Faylı yükləmək mümkün olmadı. İnternet bağlantınızı yoxlayın və yenidən cəhd edin."
                    else -> "Fayl yüklənərkən xəta baş verdi. Zəhmət olmasa yenidən cəhd edin."
                }
                msg.contains("network", ignoreCase = true) ||
                msg.contains("timeout", ignoreCase = true) ||
                msg.contains("connection", ignoreCase = true) ->
                    "Faylı yükləmək mümkün olmadı. İnternet bağlantınızı yoxlayın və yenidən cəhd edin."
                else -> e.message ?: "Fayl yüklənərkən xəta baş verdi."
            }
        }
    }

    fun cancelUpload(fileId: String): Boolean {
        val task = activeUploads.remove(fileId)
        return if (task != null) {
            task.cancel()
            true
        } else false
    }

    suspend fun getFileDownloadUrl(storagePath: String): String {
        return storage.reference.child(storagePath).downloadUrl.await().toString()
    }

    suspend fun deleteOrderFile(storagePath: String) {
        try {
            storage.reference.child(storagePath).delete().await()
        } catch (_: Exception) {}
    }

    suspend fun uploadOrderFileWithUri(
        userId: String,
        orderId: String,
        uri: Uri,
        originalFileName: String,
        sizeBytes: Long,
        mimeType: String,
        fileId: String = UUID.randomUUID().toString(),
        onProgress: ((Float) -> Unit)? = null
    ): OrderAttachedFile {
        validateFile(originalFileName, sizeBytes, mimeType)

        val ext = extractExtension(originalFileName).ifBlank { "pdf" }
        val safeStoredName = "${fileId}.$ext"
        val storagePath = "users/$userId/orders/$orderId/files/$safeStoredName"
        val storageRef = storage.reference.child(storagePath)

        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType.ifBlank { "application/octet-stream" })
            .setCustomMetadata("originalFileName", sanitizeFileName(originalFileName))
            .setCustomMetadata("uploadedBy", userId)
            .setCustomMetadata("orderId", orderId)
            .setCustomMetadata("uploadedAt", isoNow)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val uploadTask = storageRef.putFile(uri, metadata)
            activeUploads[fileId] = uploadTask

            continuation.invokeOnCancellation {
                uploadTask.cancel()
                activeUploads.remove(fileId)
            }

            uploadTask.addOnProgressListener { snapshot ->
                if (snapshot.totalByteCount > 0) {
                    val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
                    onProgress?.invoke(progress.coerceIn(0f, 1f))
                }
            }

            uploadTask.addOnSuccessListener {
                activeUploads.remove(fileId)
                val uploadedFile = OrderAttachedFile(
                    id = fileId,
                    name = originalFileName,
                    originalFileName = originalFileName,
                    sizeBytes = sizeBytes,
                    extension = ext,
                    progress = 1.0f,
                    state = FileUploadState.UPLOADED,
                    storagePath = storagePath,
                    mimeType = mimeType,
                    uploadedAt = isoNow,
                    uploadedBy = userId
                )
                if (continuation.isActive) {
                    continuation.resume(uploadedFile)
                }
            }

            uploadTask.addOnFailureListener { exception ->
                activeUploads.remove(fileId)
                if (continuation.isActive) {
                    val isCanceled = uploadTask.isCanceled
                    val failedFile = OrderAttachedFile(
                        id = fileId,
                        name = originalFileName,
                        originalFileName = originalFileName,
                        sizeBytes = sizeBytes,
                        extension = ext,
                        progress = 0f,
                        state = if (isCanceled) FileUploadState.CANCELLED else FileUploadState.FAILED,
                        storagePath = storagePath,
                        mimeType = mimeType,
                        uploadedAt = isoNow,
                        uploadedBy = userId,
                        localUri = uri.toString(),
                        errorMessage = mapStorageError(exception)
                    )
                    continuation.resume(failedFile)
                }
            }
        }
    }

    suspend fun uploadOrderFileBytes(
        userId: String,
        orderId: String,
        fileBytes: ByteArray,
        originalFileName: String,
        mimeType: String,
        fileId: String = UUID.randomUUID().toString(),
        onProgress: ((Float) -> Unit)? = null
    ): OrderAttachedFile {
        validateFile(originalFileName, fileBytes.size.toLong(), mimeType)

        val ext = extractExtension(originalFileName).ifBlank { "pdf" }
        val safeStoredName = "${fileId}.$ext"
        val storagePath = "users/$userId/orders/$orderId/files/$safeStoredName"
        val storageRef = storage.reference.child(storagePath)

        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType.ifBlank { "application/octet-stream" })
            .setCustomMetadata("originalFileName", sanitizeFileName(originalFileName))
            .setCustomMetadata("uploadedBy", userId)
            .setCustomMetadata("orderId", orderId)
            .setCustomMetadata("uploadedAt", isoNow)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val uploadTask = storageRef.putBytes(fileBytes, metadata)
            activeUploads[fileId] = uploadTask

            continuation.invokeOnCancellation {
                uploadTask.cancel()
                activeUploads.remove(fileId)
            }

            uploadTask.addOnProgressListener { snapshot ->
                if (snapshot.totalByteCount > 0) {
                    val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
                    onProgress?.invoke(progress.coerceIn(0f, 1f))
                }
            }

            uploadTask.addOnSuccessListener {
                activeUploads.remove(fileId)
                val uploadedFile = OrderAttachedFile(
                    id = fileId,
                    name = originalFileName,
                    originalFileName = originalFileName,
                    sizeBytes = fileBytes.size.toLong(),
                    extension = ext,
                    progress = 1.0f,
                    state = FileUploadState.UPLOADED,
                    storagePath = storagePath,
                    mimeType = mimeType,
                    uploadedAt = isoNow,
                    uploadedBy = userId
                )
                if (continuation.isActive) {
                    continuation.resume(uploadedFile)
                }
            }

            uploadTask.addOnFailureListener { exception ->
                activeUploads.remove(fileId)
                if (continuation.isActive) {
                    val isCanceled = uploadTask.isCanceled
                    val failedFile = OrderAttachedFile(
                        id = fileId,
                        name = originalFileName,
                        originalFileName = originalFileName,
                        sizeBytes = fileBytes.size.toLong(),
                        extension = ext,
                        progress = 0f,
                        state = if (isCanceled) FileUploadState.CANCELLED else FileUploadState.FAILED,
                        storagePath = storagePath,
                        mimeType = mimeType,
                        uploadedAt = isoNow,
                        uploadedBy = userId,
                        errorMessage = mapStorageError(exception)
                    )
                    continuation.resume(failedFile)
                }
            }
        }
    }

    suspend fun uploadOrderFile(
        userId: String,
        orderId: String,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String
    ): OrderAttachedFile {
        return uploadOrderFileBytes(
            userId = userId,
            orderId = orderId,
            fileBytes = fileBytes,
            originalFileName = fileName,
            mimeType = mimeType
        )
    }
}
