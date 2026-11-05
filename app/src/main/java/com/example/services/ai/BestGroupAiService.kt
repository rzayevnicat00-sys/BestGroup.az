package com.example.services.ai

import com.example.model.AiChatMessage
import com.example.model.AiDocumentFinding
import com.example.model.AiDocumentReport
import com.example.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BestGroupAiService {

    private val _chatMessages = MutableStateFlow<List<AiChatMessage>>(listOf(
        AiChatMessage(
            id = "ai_welcome",
            text = "Salam! Mən BestGroup AI köməkçinizəm. Sizə diplom, dissertasiya, elmi məqalə, xidmət seçimi və ya mövcud sifarişlərinizin vəziyyəti barədə necə kömək edə bilərəm?",
            isFromUser = false,
            timestamp = "İndi",
            suggestedAction = "service_help"
        )
    ))
    val chatMessages: StateFlow<List<AiChatMessage>> = _chatMessages.asStateFlow()

    fun sendMessage(
        userPrompt: String,
        userOrders: List<Order>,
        onResponse: (AiChatMessage) -> Unit
    ) {
        val clean = userPrompt.trim()
        if (clean.isBlank()) return

        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            text = clean,
            isFromUser = true,
            timestamp = "İndi"
        )
        _chatMessages.value = _chatMessages.value + userMsg

        val aiReply = generateAiResponse(clean, userOrders)
        _chatMessages.value = _chatMessages.value + aiReply
        onResponse(aiReply)
    }

    private fun generateAiResponse(prompt: String, userOrders: List<Order>): AiChatMessage {
        val lower = prompt.lowercase()

        // 1. Order Status Queries (Privacy Isolated: ONLY checks the user's own orders)
        if (lower.contains("sifariş") || lower.contains("status") || lower.contains("mərhələ") || lower.contains("vəziyyət")) {
            return if (userOrders.isEmpty()) {
                AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Hazırda sizin adınıza qeydiyyatda olan aktiv sifariş tapılmadı. Yeni bir akademik sifariş formalaşdırmaq istərdinizmi?",
                    isFromUser = false,
                    timestamp = "İndi",
                    suggestedAction = "new_order"
                )
            } else {
                val latest = userOrders.first()
                val stageText = latest.currentStage.titleAz
                AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Sonuncu sifarişiniz: №${latest.orderNumber} - '${latest.serviceName}' (${latest.topic.ifBlank { "Mövzu qeyd olunmayıb" }}).\n\n" +
                            "Cari mərhələ: $stageText.\n" +
                            "Təhvil tarixi: ${latest.deadline.ifBlank { "Təyin olunur" }}.\n\n" +
                            "Ətraflı baxmaq və sənədləri yükləmək üçün 'Sifarişlərim' bölməsinə keçə bilərsiniz.",
                    isFromUser = false,
                    timestamp = "İndi",
                    suggestedAction = "view_orders"
                )
            }
        }

        // 2. Pricing & Calculator
        if (lower.contains("qiymət") || lower.contains("neçəyə") || lower.contains("qiyməti") || lower.contains("kalkulyator")) {
            return AiChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Xidmətlərimizin ilkin qiyməti mövzu növü, səhifə sayı, dil və son tarixdən asılı olaraq hesablanır:\n\n" +
                        "• Diplom işi: 6 AZN/səhifədən başlayaraq\n" +
                        "• Magistr dissertasiyası: 9 AZN/səhifədən başlayaraq\n" +
                        "• Kurs işi: 4 AZN/səhifə\n" +
                        "• Elmi məqalə: 8 AZN/səhifə\n\n" +
                        "Dəqiq hesablama üçün tətbiqin 'Qiymət Kalkulyatoru' alətindən birbaşa istifadə edə bilərsiniz.",
                isFromUser = false,
                timestamp = "İndi",
                suggestedAction = "open_calculator"
            )
        }

        // 3. AAK (Ali Attestasiya Komissiyası) Plagiarism & Requirements
        if (lower.contains("aak") || lower.contains("plagiat") || lower.contains("anti-plagiat") || lower.contains("yoxlama")) {
            return AiChatMessage(
                id = UUID.randomUUID().toString(),
                text = "BestGroup.az yalnız Diplom işləri və Dissertasiyalar üçün rəsmi AAK standartlarına tam uyğunluq və anti-plagiat yoxlaması təqdim edir. Bütün işlərimiz unikal tədqiqat əsasında yazılır və AAK təlimatına uyğun 70-80%+ unikallıq sertifikatı ilə təmin olunur.",
                isFromUser = false,
                timestamp = "İndi",
                suggestedAction = "aak_info"
            )
        }

        // 4. Citation formats (APA, MLA, Harvard)
        if (lower.contains("apa") || lower.contains("mla") || lower.contains("istinad") || lower.contains("ədəbiyyat")) {
            return AiChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Akademik işlərinizdə istinadların düzgün formatlanması üçün tətbiqdə 'Akademik Alətlər -> İstinad Generatoru' mövcuddur. APA 7th, MLA 9th və Chicago 17th formatlarında dərhal hazır biblioqrafik qeyd əldə edə bilərsiniz.",
                isFromUser = false,
                timestamp = "İndi",
                suggestedAction = "citation_tool"
            )
        }

        // 5. Human Support escalation
        if (lower.contains("menecer") || lower.contains("operator") || lower.contains("zəng") || lower.contains("əlaqə") || lower.contains("canlı dəstək")) {
            return AiChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Sizi dərhal canlı müştəri dəstəyinə və ya təyin olunmuş akademik menecerə yönləndirirəm. 'Dəstək və Çat' bölməsindən birbaşa yazışa və ya +994 50 123 45 67 qaynar xəttinə zəng edə bilərsiniz.",
                isFromUser = false,
                timestamp = "İndi",
                suggestedAction = "open_support"
            )
        }

        // Default intelligent assistance
        return AiChatMessage(
            id = UUID.randomUUID().toString(),
            text = "Sualınızı qeyd etdiyiniz üçün təşəkkürlər. BestGroup.az olaraq diplom işləri, magistr dissertasiyaları, elmi məqalələr, təqdimatlar və statistik analiz sahəsində peşəkar akademik dəstək göstəririk. Sizə konkret hansı xidmət və ya mövzu üzrə məlumat lazımdır?",
            isFromUser = false,
            timestamp = "İndi",
            suggestedAction = null
        )
    }

    fun analyzeDocument(documentTitle: String, content: String): AiDocumentReport {
        val trimmed = content.trim()
        val findings = mutableListOf<AiDocumentFinding>()

        // 1. Structure check: Title, Introduction, Chapters, Conclusion, References
        val hasIntro = trimmed.contains("giriş", ignoreCase = true) || trimmed.contains("introduction", ignoreCase = true)
        val hasConclusion = trimmed.contains("nəticə", ignoreCase = true) || trimmed.contains("conclusion", ignoreCase = true)
        val hasReferences = trimmed.contains("ədəbiyyat", ignoreCase = true) || trimmed.contains("references", ignoreCase = true)

        if (!hasIntro) {
            findings.add(
                AiDocumentFinding(
                    category = "Struktur",
                    originalSnippet = "Sənədin əvvəli",
                    suggestedSnippet = "1. GİRİŞ (Aktuallıq, Məqsəd, Vəzifələr)",
                    explanation = "Akademik sənədlərdə 'Giriş' bölməsinin olması mütləqdir."
                )
            )
        }

        if (!hasConclusion) {
            findings.add(
                AiDocumentFinding(
                    category = "Struktur",
                    originalSnippet = "Sənədin sonu",
                    suggestedSnippet = "NƏTİCƏ VƏ TƏKLİFLƏR",
                    explanation = "Tədqiqatın yekun elmi və praktiki nəticələrini ümumiləşdirən nəticə bölməsi əlavə edilməlidir."
                )
            )
        }

        if (!hasReferences) {
            findings.add(
                AiDocumentFinding(
                    category = "İstinad",
                    originalSnippet = "Mənbə siyahısı",
                    suggestedSnippet = "İSTİFADƏ EDİLMİŞ ƏDƏBİYYAT SİYAHISI (APA 7th standartı)",
                    explanation = "Ən azı 20-30 elmi mənbədən ibarət ədəbiyyat siyahısı tələb olunur."
                )
            )
        }

        // 2. Spell & Linguistic checks
        if (trimmed.contains("ve ya", ignoreCase = true)) {
            findings.add(
                AiDocumentFinding(
                    category = "Orfoqrafiya",
                    originalSnippet = "ve ya",
                    suggestedSnippet = "və ya",
                    explanation = "Azərbaycan əlifbasının 'ə' hərfi bərpa edilməlidir."
                )
            )
        }
        if (trimmed.contains("belelikle", ignoreCase = true)) {
            findings.add(
                AiDocumentFinding(
                    category = "Orfoqrafiya",
                    originalSnippet = "belelikle",
                    suggestedSnippet = "Beləliklə,",
                    explanation = "Mətnin başında ara söz kimi işləndikdə 'Beləliklə,' şəklində vergüllə ayrılmalıdır."
                )
            )
        }

        // 3. Sentence Length Analysis
        val sentences = trimmed.split(Regex("[.!?]+\\s*")).filter { it.isNotBlank() }
        val veryLongSentences = sentences.filter { it.split(Regex("\\s+")).size > 35 }
        if (veryLongSentences.isNotEmpty()) {
            findings.add(
                AiDocumentFinding(
                    category = "Formatlama",
                    originalSnippet = veryLongSentences.first().take(80) + "...",
                    suggestedSnippet = "Cümləni 2 müstəqil fikrə ayırın",
                    explanation = "Cümlə 35-dən artıq sözdən ibarətdir. Akademik dildə aydınlıq üçün uzun cümlələri bölmək tövsiyə edilir."
                )
            )
        }

        val score = (100 - findings.size * 6).coerceIn(60, 98)

        return AiDocumentReport(
            id = UUID.randomUUID().toString(),
            documentTitle = documentTitle.ifBlank { "Sənəd analizi" },
            overallScorePercent = score,
            findings = findings,
            structureAssessment = if (hasIntro && hasConclusion && hasReferences) "AAK standartlarına tam uyğundur." else "Strukturda çatışmayan bölmələr mövcuddur.",
            citationAssessment = if (hasReferences) "İstinadlar aşkar edildi, APA 7-ci nəşr formatına uyğunlaşdırılması tövsiyə olunur." else "Ədəbiyyat siyahısı tapılmadı.",
            generalAdvice = listOf(
                "Times New Roman 14 pt, 1.5 sətir intervalı və abzas boşluqlarına riayət edin.",
                "Cümlə başlanğıclarında elmi üsluba uyğun keçid sözlərindən istifadə edin ('Aparılan təhlil göstərir ki...', 'Nəticə etibarilə...').",
                "AAK anti-plagiat yoxlamasından əvvəl yekun redaktə üçün BestGroup redaktoruna göndərin."
            )
        )
    }
}
