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
    val totalOrdersCount: Int = 0,
    val pendingOrdersCount: Int = 0,
    val acceptedOrdersCount: Int = 0,
    val inProgressOrdersCount: Int = 0,
    val readyOrdersCount: Int = 0,
    val cancelledOrdersCount: Int = 0,
    val totalCustomersCount: Int = 0,
    val waitingSupportMessagesCount: Int = 0,
    val waitingOrderMessagesCount: Int = 0,
    val todayOrdersCount: Int = 0,
    val activeOrdersCount: Int = 0,
    val monthlyRevenueAzn: Int = 0,
    val newUsersCount: Int = 0
)
