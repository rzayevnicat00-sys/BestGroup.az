package com.example.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class AppUpdateInfo(
    @DocumentId
    val id: String = "",
    @PropertyName("versionCode")
    val versionCode: Int = 1,
    @PropertyName("versionName")
    val versionName: String = "1.0.0",
    @PropertyName("releaseNotesAz")
    val releaseNotesAz: String = "",
    @PropertyName("releaseNotesEn")
    val releaseNotesEn: String = "",
    @PropertyName("releaseNotesRu")
    val releaseNotesRu: String = "",
    @PropertyName("downloadUrl")
    val downloadUrl: String = "https://github.com/rzayevnicat00-sys/BestGroup.az/releases/latest",
    @PropertyName("githubReleaseUrl")
    val githubReleaseUrl: String = "https://github.com/rzayevnicat00-sys/BestGroup.az/releases",
    @PropertyName("isMandatory")
    val isMandatory: Boolean = false,
    @PropertyName("minSupportedVersionCode")
    val minSupportedVersionCode: Int = 1,
    @PropertyName("publishedAtMillis")
    val publishedAtMillis: Long = System.currentTimeMillis(),
    @PropertyName("publishedBy")
    val publishedBy: String = "",
    @PropertyName("apkSizeMb")
    val apkSizeMb: Double = 0.0
)
