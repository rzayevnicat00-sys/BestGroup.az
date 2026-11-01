package com.example.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * In-App Update model conforming to app_config/update document schema in Firestore.
 */
data class AppUpdateInfo(
    @DocumentId
    val id: String = "update",
    @PropertyName("latestVersionCode")
    val latestVersionCode: Long = 1L,
    @PropertyName("latestVersionName")
    val latestVersionName: String = "1.0.0",
    @PropertyName("apkDownloadUrl")
    val apkDownloadUrl: String = "",
    @PropertyName("releaseNotes")
    val releaseNotes: String = "",
    @PropertyName("isUpdateAvailable")
    val isUpdateAvailable: Boolean = false,
    @PropertyName("isForceUpdate")
    val isForceUpdate: Boolean = false,
    @PropertyName("minimumSupportedVersionCode")
    val minimumSupportedVersionCode: Long = 1L,
    @PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    // Legacy / Admin Compatibility Aliases
    @PropertyName("versionCode")
    val legacyVersionCode: Int? = null,
    @PropertyName("versionName")
    val legacyVersionName: String? = null,
    @PropertyName("releaseNotesAz")
    val releaseNotesAz: String = "",
    @PropertyName("releaseNotesEn")
    val releaseNotesEn: String = "",
    @PropertyName("releaseNotesRu")
    val releaseNotesRu: String = "",
    @PropertyName("downloadUrl")
    val downloadUrl: String = "",
    @PropertyName("githubReleaseUrl")
    val githubReleaseUrl: String = "https://github.com/rzayevnicat00-sys/BestGroup.az/releases",
    @PropertyName("isMandatory")
    val legacyIsMandatory: Boolean? = null,
    @PropertyName("minSupportedVersionCode")
    val legacyMinSupportedVersionCode: Int? = null,
    @PropertyName("publishedAtMillis")
    val publishedAtMillis: Long = System.currentTimeMillis(),
    @PropertyName("publishedBy")
    val publishedBy: String = "",
    @PropertyName("apkSizeMb")
    val apkSizeMb: Double = 31.0
) {
    val versionCode: Int
        get() = legacyVersionCode ?: latestVersionCode.toInt()

    val versionName: String
        get() = legacyVersionName ?: latestVersionName

    val isMandatory: Boolean
        get() = legacyIsMandatory ?: isForceUpdate

    val minSupportedVersionCode: Int
        get() = legacyMinSupportedVersionCode ?: minimumSupportedVersionCode.toInt()

    val effectiveDownloadUrl: String
        get() = apkDownloadUrl.ifBlank { downloadUrl }.ifBlank { "https://github.com/rzayevnicat00-sys/BestGroup.az/releases/latest/download/BestGroup.az.apk" }

    val effectiveReleaseNotes: String
        get() = releaseNotes.ifBlank { releaseNotesAz }.ifBlank { "BestGroup.az 2.0 tətbiqinin yeni versiyası hazırdır." }
}

