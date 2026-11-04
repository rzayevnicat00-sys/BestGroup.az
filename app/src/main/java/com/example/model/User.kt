package com.example.model

enum class UserRole(val title: String) {
    CUSTOMER("Müştəri"),
    OPERATOR("Operator"),
    MANAGER("Menecer"),
    ADMIN("Admin")
}

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val university: String = "BestGroup.az",
    val faculty: String = "Akademik",
    val degreeLevel: String = "Bakalavriat",
    val role: UserRole = UserRole.CUSTOMER,
    val avatarUrl: String? = null,
    val createdAt: String = "",
    val orderCount: Int = 0,
    val phoneVerified: Boolean = false,
    val isOnline: Boolean = false,
    val lastActiveAtMillis: Long = 0L,
    val isTwoFactorEnabled: Boolean = false,
    val twoFactorMethod: String = "SMS",
    val isBiometricEnabled: Boolean = false,
    val isPinSet: Boolean = false,
    val autoLockTimeoutSeconds: Long = 60L,
    val isScreenSecurityEnabled: Boolean = false
) {
    companion object {
        const val ONLINE_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes inactivity timeout
    }

    /**
     * Determines whether the user is authentically online:
     * Requires both `isOnline == true` AND an activity heartbeat recorded within the last 5 minutes.
     */
    val isCurrentlyOnline: Boolean
        get() = isOnline && lastActiveAtMillis > 0 && (System.currentTimeMillis() - lastActiveAtMillis < ONLINE_TIMEOUT_MS)
}
