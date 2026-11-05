package com.example.services.academic

import com.example.model.ClubTier
import com.example.model.PriceCalculationEstimate
import com.example.model.ServiceType

class PriceCalculatorService {

    fun calculateEstimate(
        serviceType: ServiceType,
        pageCount: Int,
        language: String,
        deadlineDays: Int,
        isUrgent: Boolean,
        isAakCheck: Boolean,
        userTier: ClubTier = ClubTier.STANDARD
    ): PriceCalculationEstimate {
        val safePages = pageCount.coerceAtLeast(1)

        val (basePageRate, serviceName) = when (serviceType) {
            ServiceType.DIPLOMA -> 6.0 to "Diplom işi"
            ServiceType.MASTER -> 9.0 to "Magistr dissertasiyası"
            ServiceType.COURSE_WORK -> 4.0 to "Kurs işi"
            ServiceType.PROJECT_WORK -> 4.5 to "Layihə işi"
            ServiceType.INDEPENDENT_WORK -> 2.5 to "Sərbəst iş"
            ServiceType.REPORT -> 3.5 to "Hesabat"
            ServiceType.ESSAY -> 4.5 to "Esse"
            ServiceType.PRESENTATION -> 3.0 to "Təqdimat (slayd)"
            ServiceType.ARTICLE -> 8.0 to "Elmi məqalə"
            ServiceType.STATISTICAL_ANALYSIS -> 12.0 to "Statistik analiz"
            ServiceType.EDITING -> 2.0 to "Korrektə və Redaktə"
            ServiceType.FORMATTING -> 1.5 to "Formatlama (AAK / APA)"
            ServiceType.OTHER -> 4.0 to "Digər akademik xidmət"
        }

        val langMultiplier = when {
            language.contains("ingilis", ignoreCase = true) || language.contains("english", ignoreCase = true) -> 1.5
            language.contains("rus", ignoreCase = true) || language.contains("russian", ignoreCase = true) -> 1.25
            language.contains("alman", ignoreCase = true) || language.contains("türk", ignoreCase = true) -> 1.4
            else -> 1.0
        }

        val rawBase = safePages * basePageRate * langMultiplier

        val urgencySurcharge = when {
            isUrgent || deadlineDays <= 3 -> rawBase * 0.40
            deadlineDays <= 6 -> rawBase * 0.20
            else -> 0.0
        }

        val aakFee = if (isAakCheck) 35.0 else 0.0

        val subtotal = rawBase + urgencySurcharge + aakFee
        val discount = subtotal * userTier.discountPercent
        val total = (subtotal - discount).coerceAtLeast(10.0)

        return PriceCalculationEstimate(
            serviceTitle = serviceName,
            pageCount = safePages,
            language = language,
            deadlineDays = deadlineDays,
            isUrgent = isUrgent || deadlineDays <= 3,
            isAakCheckIncluded = isAakCheck,
            basePriceAzn = Math.round(rawBase * 100.0) / 100.0,
            urgencySurchargeAzn = Math.round(urgencySurcharge * 100.0) / 100.0,
            additionalServicesAzn = aakFee,
            clubDiscountAzn = Math.round(discount * 100.0) / 100.0,
            estimatedTotalAzn = Math.round(total * 100.0) / 100.0
        )
    }
}
