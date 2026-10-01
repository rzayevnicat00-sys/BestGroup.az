package com.example.model

enum class FaqCategory(val titleAz: String, val titleEn: String, val titleRu: String) {
    GENERAL("Ümumi", "General", "Общие"),
    ORDERING("Sifariş Prosesi", "Ordering Process", "Процесс заказа"),
    PAYMENT("Ödəniş və Qiymət", "Payment & Pricing", "Оплата и цены"),
    QUALITY("Zəmanət və Orijinallıq", "Guarantees & Quality", "Гарантии и качество"),
    PRIVACY("Məxfilik və Təhlükəsizlik", "Privacy & Security", "Конфиденциальность")
}

data class FaqItem(
    val id: String,
    val category: FaqCategory,
    val questionAz: String,
    val questionEn: String,
    val questionRu: String,
    val answerAz: String,
    val answerEn: String,
    val answerRu: String
)

data class AdminDashboardStats(
    val todayOrdersCount: Int,
    val activeOrdersCount: Int,
    val pendingApprovalCount: Int,
    val readyOrdersCount: Int,
    val overdueOrdersCount: Int,
    val monthlyRevenueAzn: Int,
    val newUsersCount: Int,
    val completionRate: Int
)
