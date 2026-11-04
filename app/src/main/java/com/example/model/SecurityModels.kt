package com.example.model

enum class AutoLockTimeout(val seconds: Long, val titleAz: String, val titleEn: String, val titleRu: String) {
    IMMEDIATE(0L, "Dərhal", "Immediately", "Сразу"),
    ONE_MINUTE(60L, "1 dəqiqə", "1 minute", "1 минута"),
    FIVE_MINUTES(300L, "5 dəqiqə", "5 minutes", "5 минут"),
    FIFTEEN_MINUTES(900L, "15 dəqiqə", "15 minutes", "15 минут"),
    DISABLED(-1L, "Deaktiv", "Disabled", "Отключено");

    companion object {
        fun fromSeconds(sec: Long): AutoLockTimeout {
            return values().find { it.seconds == sec } ?: ONE_MINUTE
        }
    }
}

enum class TwoFactorMethod(val titleAz: String, val titleEn: String, val titleRu: String) {
    SMS("SMS OTP (Mobil nömrəyə)", "SMS OTP (To mobile number)", "SMS OTP (На номер)"),
    EMAIL("E-poçt təsdiqləməsi", "Email verification", "Подтверждение по email")
}

data class SecuritySettings(
    val isBiometricEnabled: Boolean = false,
    val isPinEnabled: Boolean = false,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.ONE_MINUTE,
    val isScreenSecurityEnabled: Boolean = false,
    val isTwoFactorEnabled: Boolean = false,
    val twoFactorMethod: TwoFactorMethod = TwoFactorMethod.SMS
)

data class DeviceSession(
    val id: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val deviceModel: String = "",
    val osVersion: String = "",
    val appVersion: String = "2.0.0",
    val ipAddress: String = "127.0.0.1",
    val location: String = "Bakı, Azərbaycan",
    val loginTimestamp: Long = System.currentTimeMillis(),
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val isCurrentDevice: Boolean = false,
    val isRevoked: Boolean = false
)

data class LoginHistoryItem(
    val id: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Uğurlu giriş",
    val ipOrNetwork: String = "Wi-Fi / Mobil şəbəkə",
    val isSuspicious: Boolean = false,
    val eventType: String = "LOGIN" // LOGIN, PIN_FAILED, REAUTH, 2FA_SUCCESS, LOGOUT_ALL
)

data class AdminAuditLog(
    val id: String = "",
    val adminId: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetUserId: String? = null,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
