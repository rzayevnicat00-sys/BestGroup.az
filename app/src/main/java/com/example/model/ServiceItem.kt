package com.example.model

import com.example.localization.StringKey

enum class ServiceType(
    val nameKey: StringKey,
    val descKey: StringKey,
    val startingPriceAzn: Int,
    val standardDays: Int,
    val iconName: String
) {
    DIPLOMA(
        StringKey.SRV_DIPLOMA_NAME,
        StringKey.SRV_DIPLOMA_DESC,
        400,
        20,
        "school"
    ),
    MASTER(
        StringKey.SRV_MASTER_NAME,
        StringKey.SRV_MASTER_DESC,
        700,
        35,
        "psychology"
    ),
    COURSE_WORK(
        StringKey.SRV_COURSE_WORK_NAME,
        StringKey.SRV_COURSE_WORK_DESC,
        30,
        10,
        "menu_book"
    ),
    PROJECT_WORK(
        StringKey.SRV_COURSE_WORK_NAME,
        StringKey.SRV_COURSE_WORK_DESC,
        30,
        12,
        "work"
    ),
    INDEPENDENT_WORK(
        StringKey.SRV_INDEPENDENT_WORK_NAME,
        StringKey.SRV_INDEPENDENT_WORK_DESC,
        1,
        5,
        "assignment"
    ),
    REPORT(
        StringKey.SRV_REPORT_NAME,
        StringKey.SRV_REPORT_DESC,
        1,
        4,
        "description"
    ),
    ESSAY(
        StringKey.SRV_ESSAY_NAME,
        StringKey.SRV_ESSAY_DESC,
        1,
        3,
        "edit_note"
    ),
    PRESENTATION(
        StringKey.SRV_PRESENTATION_NAME,
        StringKey.SRV_PRESENTATION_DESC,
        20,
        4,
        "slideshow"
    ),
    ARTICLE(
        StringKey.SRV_ARTICLE_NAME,
        StringKey.SRV_ARTICLE_DESC,
        25,
        15,
        "article"
    ),
    STATISTICAL_ANALYSIS(
        StringKey.SRV_STATISTICAL_ANALYSIS_NAME,
        StringKey.SRV_STATISTICAL_ANALYSIS_DESC,
        15,
        7,
        "analytics"
    ),
    EDITING(
        StringKey.SRV_EDITING_NAME,
        StringKey.SRV_EDITING_DESC,
        15,
        5,
        "spellcheck"
    ),
    FORMATTING(
        StringKey.SRV_FORMATTING_NAME,
        StringKey.SRV_FORMATTING_DESC,
        15,
        3,
        "format_shapes"
    ),
    OTHER(
        StringKey.SRV_OTHER_NAME,
        StringKey.SRV_OTHER_DESC,
        0,
        7,
        "more_horiz"
    )
}

data class ServiceItem(
    val id: String,
    val type: ServiceType,
    val isFavorite: Boolean = false
)

data class ServiceCatalogItem(
    val serviceId: String = "",
    val nameAz: String = "",
    val nameEn: String = "",
    val nameRu: String = "",
    val descriptionAz: String = "",
    val descriptionEn: String = "",
    val descriptionRu: String = "",
    val startingPriceAzn: Double = 0.0,
    val priceType: String = "starting_from", // "starting_from", "fixed", "quote"
    val unit: String = "service", // "page", "project", "service", "hour", "quote"
    val maxPriceAzn: Double = 0.0,
    val currency: String = "AZN",
    val basePrice: Double = 0.0,
    val priceUnit: String = "service",
    val estimatedDuration: String = "",
    val active: Boolean = true,
    val icon: String = "school",
    val sortOrder: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun getLocalizedName(language: com.example.localization.Language): String {
        return when (language) {
            com.example.localization.Language.AZ -> nameAz.ifBlank { nameEn.ifBlank { nameRu } }
            com.example.localization.Language.EN -> nameEn.ifBlank { nameAz.ifBlank { nameRu } }
            com.example.localization.Language.RU -> nameRu.ifBlank { nameAz.ifBlank { nameEn } }
        }
    }

    fun getLocalizedDescription(language: com.example.localization.Language): String {
        return when (language) {
            com.example.localization.Language.AZ -> descriptionAz.ifBlank { descriptionEn.ifBlank { descriptionRu } }
            com.example.localization.Language.EN -> descriptionEn.ifBlank { descriptionAz.ifBlank { descriptionRu } }
            com.example.localization.Language.RU -> descriptionRu.ifBlank { descriptionAz.ifBlank { descriptionEn } }
        }
    }

    fun getFormattedPrice(language: com.example.localization.Language): String {
        return when {
            priceType.equals("quote", ignoreCase = true) -> when (language) {
                com.example.localization.Language.AZ -> "Qiymət fərdi hesablanır"
                com.example.localization.Language.EN -> "Custom quote"
                com.example.localization.Language.RU -> "Индивидуальный расчет"
            }
            priceType.equals("range", ignoreCase = true) || maxPriceAzn > startingPriceAzn -> {
                val minInt = startingPriceAzn.toInt()
                val maxInt = if (maxPriceAzn > 0) maxPriceAzn.toInt() else minInt
                "$minInt–$maxInt AZN"
            }
            unit.equals("page", ignoreCase = true) || priceUnit.equals("page", ignoreCase = true) -> {
                val formattedBase = if (startingPriceAzn == 0.5) "0.50" else String.format(java.util.Locale.US, "%.2f", startingPriceAzn)
                when (language) {
                    com.example.localization.Language.AZ -> "$formattedBase AZN / səhifədən"
                    com.example.localization.Language.EN -> "From $formattedBase AZN / page"
                    com.example.localization.Language.RU -> "От $formattedBase AZN / стр."
                }
            }
            priceType.equals("fixed", ignoreCase = true) -> "${startingPriceAzn.toInt()} AZN"
            else -> {
                val priceInt = startingPriceAzn.toInt()
                when (language) {
                    com.example.localization.Language.AZ -> "${priceInt} AZN-dən"
                    com.example.localization.Language.EN -> "From ${priceInt} AZN"
                    com.example.localization.Language.RU -> "От ${priceInt} AZN"
                }
            }
        }
    }
}

