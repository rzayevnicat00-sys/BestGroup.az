package com.example.model

/**
 * BESTGROUP.AZ 2.0 - Ecosystem Data Models
 * Covers: Document Versioning, Academic Tools, Academy, Club/Loyalty,
 * CRM & Staff Workload, and AI Document Analysis.
 */

// --- 1. Document Versioning ---
data class OrderDocumentVersion(
    val id: String = "",
    val orderId: String = "",
    val versionNumber: Int = 1,
    val fileName: String = "",
    val fileUrl: String = "",
    val fileSizeBytes: Long = 0L,
    val fileExtension: String = "docx",
    val uploadedBy: String = "Admin",
    val uploaderRole: String = "admin", // "admin" or "customer"
    val uploadedAt: String = "",
    val uploadedAtEpoch: Long = System.currentTimeMillis(),
    val isLatest: Boolean = true,
    val isArchived: Boolean = false,
    val changeNotes: String = "İlkin sənəd versiyası"
) {
    val sizeFormatted: String
        get() {
            val kb = fileSizeBytes / 1024.0
            return if (kb > 1024) {
                String.format(java.util.Locale.US, "%.1f MB", kb / 1024)
            } else {
                String.format(java.util.Locale.US, "%.0f KB", kb)
            }
        }
}

// --- 2. Live Order Tracking Stages ---
enum class OrderStage(
    val stageIndex: Int,
    val titleAz: String,
    val titleEn: String,
    val titleRu: String,
    val descriptionAz: String
) {
    CREATED(0, "Sifariş yaradıldı", "Order Created", "Заказ создан", "Sifarişiniz sistemdə uğurla qeydiyyata alındı."),
    WAITING_APPROVAL(1, "Təsdiq gözləyir", "Awaiting Approval", "Ожидает подтверждения", "Menecer tərəfindən ilkin təsdiq gözlənilir."),
    ACCEPTED(2, "Qəbul edildi", "Order Accepted", "Принят в работу", "Sifariş təsdiqləndi və komandaya həvalə olundu."),
    IN_PROGRESS(3, "İcra olunur", "In Progress", "Выполняется", "Akademik mütəxəssis tərəfindən yazılma və tədqiqat aparılır."),
    IN_REVIEW(4, "Yoxlanılır", "In Review", "На проверке", "Elmi redaktor və plagiat/AAK yoxlamasından keçirilir."),
    READY(5, "Hazırdır", "Ready", "Готов", "İş tamamlandı, yekun fayllar sistemə yükləndi."),
    COMPLETED(6, "Tamamlandı", "Completed", "Завершен", "Sifariş müştəri tərəfindən təhvil alındı."),
    CANCELLED(7, "Ləğv edildi", "Cancelled", "Отменен", "Sifariş ləğv edildi.")
}

data class OrderLiveStage(
    val stage: OrderStage = OrderStage.CREATED,
    val timestamp: String = "",
    val timestampEpoch: Long = System.currentTimeMillis(),
    val note: String = "",
    val actorName: String = "Sistem"
)

// --- 3. Academic Tools Models ---
enum class CitationFormat(val label: String, val example: String) {
    APA_7TH("APA 7-ci nəşr", "Məmmədov, E. (2024). Azərbaycan iqtisadiyyatının inkişafı. Bakı: Elm nəşriyyatı."),
    MLA_9TH("MLA 9-cu nəşr", "Məmmədov, Elmar. Azərbaycan İqtisadiyyatının İnkişafı. Elm nəşriyyatı, 2024."),
    CHICAGO_17TH("Chicago 17-ci nəşr", "Məmmədov, Elmar. 2024. Azərbaycan iqtisadiyyatının inkişafı. Bakı: Elm nəşriyyatı."),
    HARVARD("Harvard Reference", "Məmmədov, E., 2024. Azərbaycan iqtisadiyyatının inkişafı. Bakı: Elm.")
}

data class CitationInput(
    val author: String = "",
    val title: String = "",
    val publisher: String = "",
    val year: String = "",
    val city: String = "",
    val urlOrDoi: String = ""
)

data class TextAnalysisResult(
    val wordCount: Int = 0,
    val characterCountWithSpaces: Int = 0,
    val characterCountNoSpaces: Int = 0,
    val estimatedStandardPages: Double = 0.0, // Standard 300 words per page
    val estimatedReadingTimeMinutes: Int = 0,
    val paragraphCount: Int = 0,
    val sentenceCount: Int = 0,
    val readabilityScore: String = "Normal"
)

// --- 4. Service Price Calculator ---
data class PriceCalculationEstimate(
    val serviceTitle: String,
    val pageCount: Int,
    val language: String,
    val deadlineDays: Int,
    val isUrgent: Boolean,
    val isAakCheckIncluded: Boolean,
    val basePriceAzn: Double,
    val urgencySurchargeAzn: Double,
    val additionalServicesAzn: Double,
    val clubDiscountAzn: Double,
    val estimatedTotalAzn: Double
)

// --- 5. BestGroup Academy ---
enum class AcademyCategory(val id: String, val titleAz: String, val iconName: String) {
    ACADEMIC_WRITING("writing", "Akademik yazı qaydaları", "EditNote"),
    MASTER_THESIS("master", "Magistr dissertasiyası", "School"),
    PHD_RESEARCH("phd", "Doktorantura və elmi tədqiqat", "Psychology"),
    SCIENTIFIC_PAPER("paper", "Elmi məqalə yazılması", "Article"),
    PRESENTATION("presentation", "Təqdimat və slaydlar", "CoPresent"),
    METHODOLOGY("methodology", "Tədqiqat metodologiyası", "AccountTree"),
    STATISTICS("statistics", "Statistik analizə giriş (SPSS, R)", "QueryStats"),
    CITATIONS("citations", "İstinad və mənbə qaydaları", "FormatQuote"),
    MS_OFFICE("office", "Microsoft Word və Excel", "GridOn"),
    CAREER("career", "Peşəkar inkişaf", "TrendingUp")
}

enum class AcademyContentType {
    ARTICLE,
    VIDEO,
    PDF_MATERIAL
}

data class AcademyMaterial(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val category: AcademyCategory = AcademyCategory.ACADEMIC_WRITING,
    val type: AcademyContentType = AcademyContentType.ARTICLE,
    val contentMarkdown: String = "",
    val videoUrl: String = "",
    val pdfDownloadUrl: String = "",
    val durationMinutes: Int = 10,
    val author: String = "BestGroup Elmi Heyət",
    val publishedAt: String = "",
    val viewsCount: Int = 0,
    val isBookmarked: Boolean = false,
    val isRead: Boolean = false
)

// --- 6. Personal Academic Cabinet ---
data class AcademicNote(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val relatedOrderId: String? = null,
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

data class SavedDocumentItem(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val fileUrl: String = "",
    val fileSizeFormatted: String = "",
    val category: String = "Diplom / Dissertasiya",
    val uploadedAt: String = "",
    val notes: String = ""
)

// --- 7. BestGroup Club (Loyalty & Bonuses) ---
enum class ClubTier(
    val title: String,
    val minSpendAzn: Double,
    val bonusPercent: Double,
    val discountPercent: Double,
    val badgeColorHex: Long
) {
    STANDARD("Standard", 0.0, 0.02, 0.0, 0xFF64748B),
    SILVER("Silver", 150.0, 0.04, 0.03, 0xFF94A3B8),
    GOLD("Gold", 400.0, 0.06, 0.05, 0xFFEAB308),
    PLATINUM("Platinum", 900.0, 0.10, 0.08, 0xFF7C3AED)
}

data class BonusTransaction(
    val id: String = "",
    val userId: String = "",
    val amountAzn: Double = 0.0,
    val isCredit: Boolean = true, // true = gained bonus, false = spent bonus
    val description: String = "",
    val orderId: String? = null,
    val timestamp: String = "",
    val timestampEpoch: Long = System.currentTimeMillis()
)

data class ClubMembership(
    val userId: String = "",
    val tier: ClubTier = ClubTier.STANDARD,
    val bonusBalanceAzn: Double = 0.0,
    val totalSpentAzn: Double = 0.0,
    val completedOrdersCount: Int = 0,
    val referralCode: String = "",
    val referredUsersCount: Int = 0,
    val totalReferralBonusAzn: Double = 0.0,
    val perkList: List<String> = emptyList()
)

// --- 8. CRM & Staff Workload ---
data class OrderTask(
    val id: String = "",
    val orderId: String = "",
    val title: String = "",
    val assignedStaffId: String = "",
    val assignedStaffName: String = "",
    val deadline: String = "",
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0
)

data class StaffMember(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val roleTitle: String = "Elmi Mütəxəssis",
    val activeTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val workloadScore: Int = 0 // 0 to 100%
)

data class CannedResponseTemplate(
    val id: String = "",
    val title: String = "",
    val category: String = "Sifariş",
    val responseText: String = ""
)

// --- 9. AI Assistant & Document Assistant Models ---
data class AiChatMessage(
    val id: String = "",
    val text: String = "",
    val isFromUser: Boolean = false,
    val timestamp: String = "",
    val suggestedAction: String? = null
)

data class AiDocumentFinding(
    val category: String, // "Orfoqrafiya", "Qrammatika", "Formatlama", "Struktur", "İstinad"
    val originalSnippet: String,
    val suggestedSnippet: String,
    val explanation: String,
    val isAccepted: Boolean = false
)

data class AiDocumentReport(
    val id: String = "",
    val documentTitle: String = "",
    val overallScorePercent: Int = 85,
    val findings: List<AiDocumentFinding> = emptyList(),
    val structureAssessment: String = "",
    val citationAssessment: String = "",
    val generalAdvice: List<String> = emptyList()
)
