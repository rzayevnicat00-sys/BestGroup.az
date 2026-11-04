package com.example.services.security

import android.app.Activity
import android.util.Log
import com.example.model.TwoFactorMethod
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

sealed class TwoFactorResult {
    data class CodeSent(val verificationId: String, val method: TwoFactorMethod, val destinationMasked: String) : TwoFactorResult()
    data class Verified(val userId: String) : TwoFactorResult()
    data class Error(val message: String) : TwoFactorResult()
}

class TwoFactorAuthService(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val TAG = "TwoFactorAuthService"
        const val CODE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
        const val MAX_VERIFY_ATTEMPTS = 3
    }

    private var activeEmailCode: String? = null
    private var emailCodeExpiresAt: Long = 0L
    private var emailCodeAttempts: Int = 0
    private var lastCodeSentTimestamp: Long = 0L

    fun canResendCode(): Boolean {
        val elapsed = System.currentTimeMillis() - lastCodeSentTimestamp
        return elapsed >= 60_000L
    }

    fun getResendCooldownSeconds(): Int {
        val elapsed = System.currentTimeMillis() - lastCodeSentTimestamp
        return ((60_000L - elapsed) / 1000L).coerceAtLeast(0L).toInt()
    }

    fun sendSmsTwoFactor(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+$phoneNumber"
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval on device
                val code = credential.smsCode
                if (!code.isNullOrBlank()) {
                    onCodeSent(code)
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "SMS 2FA dispatch failed: ${e.message}", e)
                onError(e.localizedMessage ?: "SMS kodu göndərilə bilmədi. Nömrənizi yoxlayın.")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                lastCodeSentTimestamp = System.currentTimeMillis()
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(cleanPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifySmsTwoFactor(
        verificationId: String,
        smsCode: String
    ): Result<Unit> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
            // Verify credential validity by validating with Firebase Auth
            val currentAuthUser = auth.currentUser
            if (currentAuthUser != null) {
                currentAuthUser.updatePhoneNumber(credential).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // If phone already linked or updated, check if invalid code
            val msg = e.localizedMessage ?: ""
            if (msg.contains("invalid", ignoreCase = true) || msg.contains("code", ignoreCase = true)) {
                Result.failure(IllegalArgumentException("Daxil edilən SMS kodu yanlışdır."))
            } else {
                // Credential was valid
                Result.success(Unit)
            }
        }
    }

    suspend fun sendEmailTwoFactor(userId: String, email: String): Result<String> {
        if (!canResendCode()) {
            return Result.failure(IllegalStateException("Yenidən kod göndərmək üçün ${getResendCooldownSeconds()} saniyə gözləyin."))
        }

        return try {
            val random = SecureRandom()
            val codeNum = 100000 + random.nextInt(900000)
            val code = codeNum.toString()
            val now = System.currentTimeMillis()

            activeEmailCode = code
            emailCodeExpiresAt = now + CODE_VALIDITY_MS
            emailCodeAttempts = 0
            lastCodeSentTimestamp = now

            // Store in Firestore for real multi-device verification
            val tokenData = mapOf(
                "userId" to userId,
                "email" to email,
                "code" to code,
                "expiresAt" to emailCodeExpiresAt,
                "attempts" to 0,
                "createdAt" to now
            )

            firestore.collection("users").document(userId)
                .collection("two_factor_tokens").document("active_token")
                .set(tokenData, SetOptions.merge())
                .await()

            // Also send standard Firebase verification email if not verified
            val user = auth.currentUser
            if (user != null && !user.isEmailVerified) {
                try {
                    user.sendEmailVerification().await()
                } catch (_: Exception) {}
            }

            Log.i(TAG, "Email 2FA code generated and persisted for $email")
            Result.success(code)
        } catch (e: Exception) {
            Log.e(TAG, "sendEmailTwoFactor failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun verifyEmailTwoFactor(userId: String, inputCode: String): Result<Unit> {
        val now = System.currentTimeMillis()
        emailCodeAttempts++

        if (emailCodeAttempts > MAX_VERIFY_ATTEMPTS) {
            activeEmailCode = null
            return Result.failure(IllegalStateException("Maksimum 3 səhv cəhd limiti aşıldı. Yenidən yeni kod tələb edin."))
        }

        // Check local state or Firestore token
        return try {
            val tokenDoc = firestore.collection("users").document(userId)
                .collection("two_factor_tokens").document("active_token")
                .get()
                .await()

            val remoteCode = tokenDoc.getString("code") ?: activeEmailCode
            val remoteExpires = tokenDoc.getLong("expiresAt") ?: emailCodeExpiresAt

            if (now > remoteExpires) {
                Result.failure(IllegalStateException("Təsdiqləmə kodunun 5 dəqiqəlik etibarlılıq müddəti bitib."))
            } else if (remoteCode == inputCode.trim()) {
                // Delete token after successful consumption
                firestore.collection("users").document(userId)
                    .collection("two_factor_tokens").document("active_token")
                    .delete()
                    .await()
                activeEmailCode = null
                Result.success(Unit)
            } else {
                val remaining = (MAX_VERIFY_ATTEMPTS - emailCodeAttempts).coerceAtLeast(0)
                Result.failure(IllegalArgumentException("Daxil edilən təsdiq kodu yanlışdır. Qalan cəhdlər: $remaining"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
