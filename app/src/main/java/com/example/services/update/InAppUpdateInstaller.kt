package com.example.services.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateDownloadProgress(
    val isDownloading: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadedFile: File? = null,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

object InAppUpdateInstaller {
    private const val TAG = "InAppUpdateInstaller"
    private const val DEFAULT_GITHUB_APK_URL =
        "https://github.com/rzayevnicat00-sys/BestGroup.az/releases/latest/download/BestGroup.az.apk"

    private val _downloadState = MutableStateFlow(UpdateDownloadProgress())
    val downloadState: StateFlow<UpdateDownloadProgress> = _downloadState.asStateFlow()

    /**
     * Resolves the best direct APK download URL.
     * If user or server passed a generic GitHub release page, converts to direct APK download asset.
     */
    fun resolveDirectApkUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return DEFAULT_GITHUB_APK_URL

        if (trimmed.endsWith(".apk", ignoreCase = true)) {
            return trimmed
        }

        if (trimmed.contains("github.com/rzayevnicat00-sys/BestGroup.az/releases/latest", ignoreCase = true)) {
            return DEFAULT_GITHUB_APK_URL
        }

        if (trimmed.contains("github.com/rzayevnicat00-sys/BestGroup.az/releases/tag/", ignoreCase = true)) {
            // e.g. https://github.com/rzayevnicat00-sys/BestGroup.az/releases/tag/v1.1.0 -> download/v1.1.0/BestGroup.az.apk
            val tag = trimmed.substringAfter("releases/tag/").substringBefore("/")
            return "https://github.com/rzayevnicat00-sys/BestGroup.az/releases/download/$tag/BestGroup.az.apk"
        }

        return trimmed
    }

    /**
     * Downloads APK directly into app's cache directory with streaming progress.
     */
    suspend fun downloadApk(context: Context, rawUrl: String): Result<File> = withContext(Dispatchers.IO) {
        val targetUrl = resolveDirectApkUrl(rawUrl)
        Log.d(TAG, "Starting APK download from: $targetUrl")

        _downloadState.value = UpdateDownloadProgress(
            isDownloading = true,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = 0L,
            isCompleted = false,
            errorMessage = null
        )

        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outputFile = File(updatesDir, "BestGroup_update.apk")
            if (outputFile.exists()) {
                outputFile.delete()
            }

            var currentUrl = targetUrl
            var connection: HttpURLConnection? = null
            var redirects = 0
            val maxRedirects = 6

            // Follow HTTP/HTTPS redirects (GitHub releases redirect to AWS S3 CDN)
            while (redirects < maxRedirects) {
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "BestGroup-Android/${Build.VERSION.SDK_INT}")
                    setRequestProperty("Accept", "*/*")
                }
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308
                ) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location != null) {
                        currentUrl = location
                        redirects++
                        continue
                    }
                }
                break
            }

            val finalConn = connection ?: throw IllegalStateException("Əlaqə yaradılmadı")
            val responseCode = finalConn.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Server xətası: HTTP $responseCode")
            }

            val contentLength = finalConn.contentLengthLong
            val inputStream = finalConn.inputStream
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L
            var lastEmittedPercent = -1

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val progressFraction = if (contentLength > 0) {
                            (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        val currentPercent = (progressFraction * 100).toInt()
                        if (currentPercent != lastEmittedPercent) {
                            lastEmittedPercent = currentPercent
                            _downloadState.value = UpdateDownloadProgress(
                                isDownloading = true,
                                progress = progressFraction,
                                downloadedBytes = totalRead,
                                totalBytes = contentLength,
                                isCompleted = false
                            )
                        }
                    }
                    output.flush()
                }
            }

            finalConn.disconnect()

            if (!outputFile.exists() || outputFile.length() == 0L) {
                throw IllegalStateException("Yüklənmiş fayl tapılmadı və ya boşdur")
            }

            _downloadState.value = UpdateDownloadProgress(
                isDownloading = false,
                progress = 1f,
                downloadedBytes = totalRead,
                totalBytes = totalRead,
                downloadedFile = outputFile,
                isCompleted = true,
                errorMessage = null
            )

            Log.d(TAG, "APK successfully downloaded to: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            _downloadState.value = UpdateDownloadProgress(
                isDownloading = false,
                errorMessage = e.message ?: "Yükləmə xətası baş verdi"
            )
            Result.failure(e)
        }
    }

    /**
     * Checks if the app has permission to install unknown apps (Android 8.0+).
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens system settings to allow installing apps for BestGroup.az.
     */
    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch package install settings", e)
            }
        }
    }

    /**
     * Installs the downloaded APK directly on top of the currently installed app using FileProvider.
     * The OS package manager compares signatures and versions, upgrading in-place without data loss.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "Cannot install: APK file does not exist")
                return false
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(installIntent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start install intent: ${e.message}", e)
            return false
        }
    }

    fun resetState() {
        _downloadState.value = UpdateDownloadProgress()
    }
}
