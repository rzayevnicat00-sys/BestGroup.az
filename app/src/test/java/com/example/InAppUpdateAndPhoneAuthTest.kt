package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppUpdateInfo
import com.example.repository.BestGroupRepository
import com.example.services.firebase.FirebaseAuthService
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
    fun testPhoneNumberMatchingNormalization() {
        // Different formats of the same Azerbaijani phone number
        val format1 = "+994 (50) 123-45-67"
        val format2 = "0501234567"
        val format3 = "501234567"
        val format4 = "+994501234567"

        assertTrue("Matching +994 (50)... with 050...", FirebaseAuthService.matchesPhoneNumber(format1, format2))
        assertTrue("Matching 050... with 50...", FirebaseAuthService.matchesPhoneNumber(format2, format3))
        assertTrue("Matching +99450... with +994 (50)...", FirebaseAuthService.matchesPhoneNumber(format4, format1))
        assertFalse("Different phone numbers must not match", FirebaseAuthService.matchesPhoneNumber("+994501234567", "0559998877"))
    }

    @Test
    fun testSuperAdminEmailLookupGeneratesValidSession() = runTest {
        val repo = BestGroupRepository()
        val result = repo.requestPhoneResetOtp("rzayevnicat00@gmail.com")
        assertTrue("Super admin email must generate OTP session successfully", result.isSuccess)
        val otpCode = result.getOrNull()
        assertNotNull(otpCode)
        assertEquals(6, otpCode?.length)

        // Reset password with valid OTP
        val resetResult = repo.completePhonePasswordReset(
            phoneOrEmail = "rzayevnicat00@gmail.com",
            enteredOtp = otpCode!!,
            newPass = "NewSecurePass2026!"
        )
        assertTrue("Reset with valid OTP must succeed", resetResult.isSuccess)
    }
}
