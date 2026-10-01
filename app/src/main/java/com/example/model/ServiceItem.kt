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
        250,
        20,
        "school"
    ),
    MASTER(
        StringKey.SRV_MASTER_NAME,
        StringKey.SRV_MASTER_DESC,
        450,
        35,
        "psychology"
    ),
    COURSE_WORK(
        StringKey.SRV_COURSE_WORK_NAME,
        StringKey.SRV_COURSE_WORK_DESC,
        90,
        10,
        "menu_book"
    ),
    PROJECT_WORK(
        StringKey.SRV_COURSE_WORK_NAME,
        StringKey.SRV_COURSE_WORK_DESC,
        100,
        12,
        "work"
    ),
    INDEPENDENT_WORK(
        StringKey.SRV_INDEPENDENT_WORK_NAME,
        StringKey.SRV_INDEPENDENT_WORK_DESC,
        40,
        5,
        "assignment"
    ),
    REPORT(
        StringKey.SRV_REPORT_NAME,
        StringKey.SRV_REPORT_DESC,
        35,
        4,
        "description"
    ),
    ESSAY(
        StringKey.SRV_ESSAY_NAME,
        StringKey.SRV_ESSAY_DESC,
        30,
        3,
        "edit_note"
    ),
    PRESENTATION(
        StringKey.SRV_PRESENTATION_NAME,
        StringKey.SRV_PRESENTATION_DESC,
        45,
        4,
        "slideshow"
    ),
    ARTICLE(
        StringKey.SRV_ARTICLE_NAME,
        StringKey.SRV_ARTICLE_DESC,
        180,
        15,
        "article"
    ),
    STATISTICAL_ANALYSIS(
        StringKey.SRV_STATISTICAL_ANALYSIS_NAME,
        StringKey.SRV_STATISTICAL_ANALYSIS_DESC,
        120,
        7,
        "analytics"
    ),
    EDITING(
        StringKey.SRV_EDITING_NAME,
        StringKey.SRV_EDITING_DESC,
        50,
        5,
        "spellcheck"
    ),
    FORMATTING(
        StringKey.SRV_FORMATTING_NAME,
        StringKey.SRV_FORMATTING_DESC,
        40,
        3,
        "format_shapes"
    ),
    OTHER(
        StringKey.SRV_OTHER_NAME,
        StringKey.SRV_OTHER_DESC,
        50,
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
        return when (priceType.lowercase()) {
            "quote" -> when (language) {
                com.example.localization.Language.AZ -> "Qiymət fərdi hesablanır"
                com.example.localization.Language.EN -> "Custom quote"
                com.example.localization.Language.RU -> "Индивидуальный расчет"
            }
            "fixed" -> "${startingPriceAzn.toInt()} AZN"
            else -> when (language) {
                com.example.localization.Language.AZ -> "${startingPriceAzn.toInt()} AZN-dən başlayır"
                com.example.localization.Language.EN -> "Starts from ${startingPriceAzn.toInt()} AZN"
                com.example.localization.Language.RU -> "От ${startingPriceAzn.toInt()} AZN"
            }
        }
    }
}

