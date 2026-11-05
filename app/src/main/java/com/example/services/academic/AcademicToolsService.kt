package com.example.services.academic

import com.example.model.CitationFormat
import com.example.model.CitationInput
import com.example.model.TextAnalysisResult

class AcademicToolsService {

    fun analyzeText(text: String): TextAnalysisResult {
        if (text.isBlank()) return TextAnalysisResult()

        val trimmed = text.trim()
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        val charWithSpaces = text.length
        val charNoSpaces = text.count { !it.isWhitespace() }
        val paragraphs = text.split(Regex("(\r\n|\r|\n){2,}")).filter { it.isNotBlank() }.size.coerceAtLeast(1)
        val sentences = text.split(Regex("[.!?]+\\s*")).filter { it.isNotBlank() }.size.coerceAtLeast(1)

        val pages = wordCount / 300.0
        val readingTime = (wordCount / 180).coerceAtLeast(1) // avg reading speed: 180-200 wpm

        val avgWordLength = if (wordCount > 0) charNoSpaces.toDouble() / wordCount else 0.0
        val avgSentenceLength = if (sentences > 0) wordCount.toDouble() / sentences else 0.0
        val readability = when {
            avgSentenceLength > 25 || avgWordLength > 6.5 -> "Akademik / Yüksək dərəcəli mürəkkəb"
            avgSentenceLength > 15 -> "Orta / Standart elmi dil"
            else -> "Sadə / Asan oxunan"
        }

        return TextAnalysisResult(
            wordCount = wordCount,
            characterCountWithSpaces = charWithSpaces,
            characterCountNoSpaces = charNoSpaces,
            estimatedStandardPages = Math.round(pages * 10.0) / 10.0,
            estimatedReadingTimeMinutes = readingTime,
            paragraphCount = paragraphs,
            sentenceCount = sentences,
            readabilityScore = readability
        )
    }

    fun generateCitation(input: CitationInput, format: CitationFormat): String {
        val author = input.author.trim().ifBlank { "Müəllif" }
        val title = input.title.trim().ifBlank { "Əsərin adı" }
        val publisher = input.publisher.trim().ifBlank { "Nəşriyyat" }
        val year = input.year.trim().ifBlank { "2024" }
        val city = input.city.trim().ifBlank { "Bakı" }
        val urlOrDoi = input.urlOrDoi.trim()

        return when (format) {
            CitationFormat.APA_7TH -> {
                val base = "$author ($year). $title. $city: $publisher."
                if (urlOrDoi.isNotBlank()) "$base https://doi.org/$urlOrDoi" else base
            }
            CitationFormat.MLA_9TH -> {
                val base = "$author. $title. $publisher, $year."
                if (urlOrDoi.isNotBlank()) "$base $urlOrDoi." else base
            }
            CitationFormat.CHICAGO_17TH -> {
                val base = "$author. $year. $title. $city: $publisher."
                if (urlOrDoi.isNotBlank()) "$base $urlOrDoi." else base
            }
            CitationFormat.HARVARD -> {
                val base = "$author, $year. $title. $city: $publisher."
                if (urlOrDoi.isNotBlank()) "$base Available at: <$urlOrDoi>." else base
            }
        }
    }

    fun generatePresentationOutline(topic: String, slideCount: Int = 8): List<Pair<String, List<String>>> {
        val cleanTopic = topic.trim().ifBlank { "Tədqiqat Mövzusu" }
        return listOf(
            "Slayd 1: Giriş və Titul" to listOf(
                "Mövzu: $cleanTopic",
                "Müəllif və elmi rəhbər haqqında məlumat",
                "Tarix və universitet/kafedra"
            ),
            "Slayd 2: Mövzunun Aktuallığı və Problem" to listOf(
                "Niyə bu mövzu bu gün əhəmiyyətlidir?",
                "Mövcud praktiki və elmi boşluqlar",
                "Tədqiqatın zəruriliyi"
            ),
            "Slayd 3: Tədqiqatın Məqsədi və Vəzifələri" to listOf(
                "Əsas elmi məqsəd",
                "Həll edilməli 3-4 əsas vəzifə",
                "Tədqiqatın obyekti və predmeti"
            ),
            "Slayd 4: Tədqiqat Metodologiyası" to listOf(
                "İstifadə olunan elmi yanaşmalar",
                "Məlumatların toplanması və təhlil üsulları",
                "Seçmə və ya emprik baza"
            ),
            "Slayd 5: Əsas Nəzəri və Praktiki Təhlil" to listOf(
                "Mövcud vəziyyətin statistik analizi",
                "Müqayisəli göstəricilər və qrafiklər",
                "Aşkar edilmiş əsas meyllər"
            ),
            "Slayd 6: Tədqiqatın Nəticələri və Yeniliklər" to listOf(
                "Əldə edilmiş əsas nəticələr",
                "Elmi və praktiki yenilik",
                "Model və ya təklif olunan mexanizm"
            ),
            "Slayd 7: Təkliflər və Tövsiyələr" to listOf(
                "Praktiki tətbiq imkanları",
                "Müvafiq sahə üzrə strateji tövsiyələr",
                "Gələcək tədqiqat perspektivləri"
            ),
            "Slayd 8: Nəticə və Təşəkkür" to listOf(
                "Yekun ümumiləşdirmə",
                "Əsas ədəbiyyat mənbələri",
                "Diqqətiniz üçün təşəkkür edirəm! (Sual-cavab)"
            )
        )
    }

    fun getAcademicStructureChecklist(): List<Pair<String, String>> {
        return listOf(
            "Titul vərəqi" to "Ali məktəb, fakültə, kafedra, ixtisas, müəllif və elmi rəhbərin adı tam göstərilməlidir.",
            "Annotasiya və Açar sözlər" to "Azərbaycan və xarici dildə (ingilis/rus) 150-250 söz həcmində, 5-7 açar söz.",
            "Giriş (Aktuallıq və Məqsəd)" to "Mövzunun aktuallığı, elmi yeniliyi, məqsədi, vəzifələri, obyekti və predmeti aydın qeyd edilməlidir.",
            "Fəsillər və Paraqraflar" to "Hər fəsil məntiqi ardıcıllıqla qurulmalı, paraqraf sonlarında ara nəticələr verilməlidir.",
            "Nəticə və Təkliflər" to "Girişdə qoyulmuş vəzifələrin həllinə uyğun elmi və praktiki yekun təkliflər əks olunmalıdır.",
            "İstifadə edilmiş ədəbiyyat" to "Ən azı 30-50 mənbə (son 5-10 ilin elmi nəşrləri, qanunvericilik aktları və xarici jurnallar).",
            "Əlavələr (Qrafiklər, Cədvəllər)" to "Böyük cədvəllər və statistik sənədlər əlavələr bölməsinə keçirilməlidir."
        )
    }
}
