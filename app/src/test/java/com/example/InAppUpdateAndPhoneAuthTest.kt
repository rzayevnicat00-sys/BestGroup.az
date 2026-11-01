package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppUpdateInfo
import com.example.repository.BestGroupRepository
import com.example.services.update.InAppUpdateInstaller
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InAppUpdateAndPhoneAuthTest {

    @Test
    fun testAppUpdateInfoMapping() {
        val update = AppUpdateInfo(
            latestVersionCode = 2,
            latestVersionName = "2.0.0",
            apkDownloadUrl = "https://github.com/rzayevnicat00-sys/BestGroup.az/releases/latest/download/BestGroup.az.apk",
            releaseNotes = "Yeni tətbiqdaxili yenilənmə sistemi və SMS OTP ilə şifrə bərpası",
            isUpdateAvailable = true,
            isForceUpdate = false,
            minimumSupportedVersionCode = 1,
            updatedAt = System.currentTimeMillis()
        )

        assertEquals(2, update.versionCode)
        assertEquals("2.0.0", update.versionName)
        assertEquals("https://github.com/rzayevnicat00-sys/BestGroup.az/releases/latest/download/BestGroup.az.apk", update.effectiveDownloadUrl)
        assertTrue(update.effectiveReleaseNotes.contains("SMS OTP"))
        assertTrue(update.isUpdateAvailable)
        assertFalse(update.isForceUpdate)
        assertEquals(1, update.minimumSupportedVersionCode)
    }

    @Test
    fun testAppUpdateInfoLegacyFallback() {
        val legacy = AppUpdateInfo(
            latestVersionCode = 3L,
            latestVersionName = "1.3.0",
            downloadUrl = "https://example.com/legacy.apk",
            releaseNotes = "Köhnə versiya təsviri"
        )

        assertEquals(3, legacy.versionCode)
        assertEquals("1.3.0", legacy.versionName)
        assertEquals("https://example.com/legacy.apk", legacy.effectiveDownloadUrl)
        assertEquals("Köhnə versiya təsviri", legacy.effectiveReleaseNotes)
    }

    @Test
    fun testVerifyDownloadedApkWithNonExistentFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyFile = File(context.cacheDir, "missing_test_update.apk")
        val result = InAppUpdateInstaller.verifyDownloadedApk(context, dummyFile)
        assertTrue("Non existent APK file must be reported as failure", result.isFailure)
        assertNotNull(result.exceptionOrNull()?.message)
    }

    @Test
    fun testPhoneResetSessionValidation() = runTest {
        val repo = BestGroupRepository()
        // Format with phone number
        val result = repo.requestPhoneResetOtp("+994509998877")
        assertNotNull(result)

        // Invalid OTP code attempt without session
        val completeResult = repo.completePhonePasswordReset(
            phone = "+994509998877",
            enteredOtp = "123456",
            newPass = "Password123!"
        )
        assertTrue("Invalid or expired session must be rejected", completeResult.isFailure)
    }
}
