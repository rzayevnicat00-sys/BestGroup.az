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
    val avatarUrl: String? = null
)
