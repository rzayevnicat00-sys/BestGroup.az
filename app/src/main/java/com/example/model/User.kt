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
    val university: String,
    val faculty: String,
    val degreeLevel: String,
    val role: UserRole = UserRole.CUSTOMER,
    val avatarUrl: String? = null,
    val createdAt: String = "",
    val orderCount: Int = 0,
    val phoneVerified: Boolean = false,
    val isOnline: Boolean = false,
    val lastActiveAtMillis: Long = 0L
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
