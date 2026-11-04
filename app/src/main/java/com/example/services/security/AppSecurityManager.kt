package com.example.services.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.model.AutoLockTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

sealed class PinValidationResult {
    object Success : PinValidationResult()
    data class WrongPin(val attemptsLeft: Int) : PinValidationResult()
    data class LockedOut(val secondsRemaining: Long) : PinValidationResult()
}

class AppSecurityManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("bestgroup_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_PIN_HASH = "sec_pin_hash"
        private const val PREF_PIN_SALT = "sec_pin_salt"
        private const val PREF_FAILED_ATTEMPTS = "sec_failed_attempts"
        private const val PREF_LOCKOUT_UNTIL = "sec_lockout_until"
        private const val PREF_BIOMETRIC_ENABLED = "sec_biometric_enabled"
        private const val PREF_PIN_ENABLED = "sec_pin_enabled"
        private const val PREF_AUTO_LOCK_SECONDS = "sec_auto_lock_seconds"
        private const val PREF_SCREEN_SECURITY = "sec_screen_security"
        private const val PREF_DEVICE_SESSION_ID = "sec_device_session_id"
        private const val MAX_FREE_ATTEMPTS = 5
        private const val INITIAL_LOCKOUT_SECONDS = 30L
    }

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(PREF_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(prefs.getBoolean(PREF_PIN_ENABLED, false) && isPinConfigured())
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _autoLockTimeout = MutableStateFlow(
        AutoLockTimeout.fromSeconds(prefs.getLong(PREF_AUTO_LOCK_SECONDS, 60L))
    )
    val autoLockTimeout: StateFlow<AutoLockTimeout> = _autoLockTimeout.asStateFlow()

    private val _isScreenSecurityEnabled = MutableStateFlow(prefs.getBoolean(PREF_SCREEN_SECURITY, false))
    val isScreenSecurityEnabled: StateFlow<Boolean> = _isScreenSecurityEnabled.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L

    init {
        // Ensure device session ID exists
        if (prefs.getString(PREF_DEVICE_SESSION_ID, null).isNullOrBlank()) {
            val newSessionId = UUID.randomUUID().toString()
            prefs.edit().putString(PREF_DEVICE_SESSION_ID, newSessionId).apply()
        }
    }

    fun getDeviceSessionId(): String {
        var id = prefs.getString(PREF_DEVICE_SESSION_ID, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(PREF_DEVICE_SESSION_ID, id).apply()
        }
        return id
    }

    // -------------------------------------------------------------
    // 4-Digit PIN Code Storage & Keystore/Crypto Logic
    // -------------------------------------------------------------

    private fun getOrCreateSalt(): String {
        var salt = prefs.getString(PREF_PIN_SALT, null)
        if (salt.isNullOrBlank()) {
            val random = SecureRandom()
            val saltBytes = ByteArray(16)
            random.nextBytes(saltBytes)
            salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
            prefs.edit().putString(PREF_PIN_SALT, salt).apply()
        }
        return salt
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val hashedBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }

    fun isPinConfigured(): Boolean {
        return !prefs.getString(PREF_PIN_HASH, null).isNullOrBlank()
    }

    fun setPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        val salt = getOrCreateSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(PREF_PIN_HASH, hash)
            .putBoolean(PREF_PIN_ENABLED, true)
            .putInt(PREF_FAILED_ATTEMPTS, 0)
            .putLong(PREF_LOCKOUT_UNTIL, 0L)
            .apply()
        _isPinEnabled.value = true
        return true
    }

    fun removePin(): Boolean {
        prefs.edit()
            .remove(PREF_PIN_HASH)
            .remove(PREF_PIN_SALT)
            .putBoolean(PREF_PIN_ENABLED, false)
            .putInt(PREF_FAILED_ATTEMPTS, 0)
            .putLong(PREF_LOCKOUT_UNTIL, 0L)
            .apply()
        _isPinEnabled.value = false
        return true
    }

    fun validatePin(pin: String): PinValidationResult {
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs.getLong(PREF_LOCKOUT_UNTIL, 0L)

        if (lockoutUntil > now) {
            val secondsRemaining = ((lockoutUntil - now) / 1000L).coerceAtLeast(1L)
            return PinValidationResult.LockedOut(secondsRemaining)
        }

        val storedHash = prefs.getString(PREF_PIN_HASH, null)
        val salt = prefs.getString(PREF_PIN_SALT, null)

        if (storedHash.isNullOrBlank() || salt.isNullOrBlank()) {
            // No PIN is set; grant access
            unlockApp()
            return PinValidationResult.Success
        }

        val inputHash = hashPin(pin, salt)
        return if (storedHash == inputHash) {
            // Correct PIN
            prefs.edit()
                .putInt(PREF_FAILED_ATTEMPTS, 0)
                .putLong(PREF_LOCKOUT_UNTIL, 0L)
                .apply()
            unlockApp()
            PinValidationResult.Success
        } else {
            // Wrong PIN
            val failed = prefs.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
            if (failed >= MAX_FREE_ATTEMPTS) {
                // Exponential backoff: 30s, 60s, 120s... max 10 minutes
                val exponent = (failed - MAX_FREE_ATTEMPTS).coerceAtMost(5)
                val durationSec = (INITIAL_LOCKOUT_SECONDS * (1 shl exponent)).coerceAtMost(600L)
                val lockUntil = now + (durationSec * 1000L)
                prefs.edit()
                    .putInt(PREF_FAILED_ATTEMPTS, failed)
                    .putLong(PREF_LOCKOUT_UNTIL, lockUntil)
                    .apply()
                PinValidationResult.LockedOut(durationSec)
            } else {
                prefs.edit()
                    .putInt(PREF_FAILED_ATTEMPTS, failed)
                    .apply()
                PinValidationResult.WrongPin(MAX_FREE_ATTEMPTS - failed)
            }
        }
    }

    fun resetLockout() {
        prefs.edit()
            .putInt(PREF_FAILED_ATTEMPTS, 0)
            .putLong(PREF_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    // -------------------------------------------------------------
    // Biometric Authentication Helper
    // -------------------------------------------------------------

    fun canAuthenticateBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricSupported(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        val status = biometricManager.canAuthenticate(authenticators)
        return status != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
               status != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "BestGroup.az Təhlükəsiz Giriş",
        subtitle: String = "Tətbiqə daxil olmaq üçün barmaq izinizi təsdiqləyin",
        negativeButtonText: String = "PIN kod ilə daxil ol",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlockApp()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If user clicked negative button ("PIN kod ilə daxil ol"), pass clear message
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        onError("PIN_FALLBACK")
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Barmaq izi tanınmadı. Yenidən cəhd edin.")
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Biometrik autentifikasiya başladıla bilmədi.")
        }
    }

    // -------------------------------------------------------------
    // Auto-Lock Lifecycle Management
    // -------------------------------------------------------------

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        prefs.edit().putLong(PREF_AUTO_LOCK_SECONDS, timeout.seconds).apply()
        _autoLockTimeout.value = timeout
    }

    fun setScreenSecurityEnabled(enabled: Boolean, activity: FragmentActivity? = null) {
        prefs.edit().putBoolean(PREF_SCREEN_SECURITY, enabled).apply()
        _isScreenSecurityEnabled.value = enabled
        applyScreenSecurity(activity)
    }

    fun applyScreenSecurity(activity: FragmentActivity?) {
        if (activity == null) return
        val enabled = _isScreenSecurityEnabled.value
        try {
            if (enabled) {
                activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        } catch (_: Exception) {}
    }

    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
        val timeout = _autoLockTimeout.value
        if (timeout == AutoLockTimeout.IMMEDIATE && (_isPinEnabled.value || _isBiometricEnabled.value)) {
            _isAppLocked.value = true
        }
    }

    fun onAppForegrounded() {
        val timeout = _autoLockTimeout.value
        if (timeout == AutoLockTimeout.DISABLED) return

        val now = System.currentTimeMillis()
        if (lastBackgroundTimestamp > 0 && (_isPinEnabled.value || _isBiometricEnabled.value)) {
            val elapsedSeconds = (now - lastBackgroundTimestamp) / 1000L
            if (elapsedSeconds >= timeout.seconds) {
                _isAppLocked.value = true
            }
        }
    }

    fun lockApp() {
        if (_isPinEnabled.value || _isBiometricEnabled.value) {
            _isAppLocked.value = true
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
        lastBackgroundTimestamp = System.currentTimeMillis()
    }
}
