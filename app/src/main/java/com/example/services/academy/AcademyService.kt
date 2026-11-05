package com.example.services.academy

import com.example.model.AcademyCategory
import com.example.model.AcademyContentType
import com.example.model.AcademyMaterial
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AcademyService(private val firestore: FirebaseFirestore) {

    private val _materials = MutableStateFlow<List<AcademyMaterial>>(getInitialMaterials())
    val materials: StateFlow<List<AcademyMaterial>> = _materials.asStateFlow()

    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    private val _readMaterialIds = MutableStateFlow<Set<String>>(emptySet())
    val readMaterialIds: StateFlow<Set<String>> = _readMaterialIds.asStateFlow()

    suspend fun loadUserProgress(userId: String) {
        if (userId.isBlank()) return
        try {
            val doc = firestore.collection("users").document(userId)
                .collection("academy_progress").document("user_state").get().await()
            if (doc.exists()) {
                val bookmarks = (doc.get("bookmarks") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
                val readList = (doc.get("read") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
                _bookmarkedIds.value = bookmarks
                _readMaterialIds.value = readList
            }
        } catch (_: Exception) {}
    }

    suspend fun toggleBookmark(userId: String, materialId: String) {
        val current = _bookmarkedIds.value.toMutableSet()
        if (current.contains(materialId)) {
            current.remove(materialId)
        } else {
            current.add(materialId)
        }
        _bookmarkedIds.value = current
        saveUserProgress(userId)
    }

    suspend fun markAsRead(userId: String, materialId: String) {
        val current = _readMaterialIds.value.toMutableSet()
        current.add(materialId)
        _readMaterialIds.value = current
        saveUserProgress(userId)
    }

    private suspend fun saveUserProgress(userId: String) {
        if (userId.isBlank()) return
        try {
            firestore.collection("users").document(userId)
                .collection("academy_progress").document("user_state")
                .set(
                    mapOf(
                        "bookmarks" to _bookmarkedIds.value.toList(),
                        "read" to _readMaterialIds.value.toList(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
        } catch (_: Exception) {}
    }

    companion object {
        fun getInitialMaterials(): List<AcademyMaterial> {
            return listOf(
                AcademyMaterial(
                    id = "acad_1",
                    title = "Akademik Girişin Yazılma Qaydaları və AAK Tələbləri",
                    subtitle = "Dissertasiya və diplom işlərində aktuallıq, məqsəd və vəzifələrin tərtibatı",
                    category = AcademyCategory.ACADEMIC_WRITING,
                    type = AcademyContentType.ARTICLE,
                    contentMarkdown = """
                        # Akademik Giriş Hissəsinin Strukturu
                        
                        Diplom və dissertasiya işlərinin ən vacib hissəsi **Giriş (Introduction)** bölməsidir. Azərbaycan Respublikası Prezidenti yanında Ali Attestasiya Komissiyasının (AAK) təlimatlarına görə, girişdə mütləq aşağıdakı bəndlər əks olunmalıdır:
                        
                        1. **Mövzunun aktuallığı:** Tədqiqatın nə üçün bu gün aparılması vacibdir? Praktiki və nəzəri əhəmiyyəti nədən ibarətdir?
                        2. **Problemin işlənmə dərəcəsi:** Yerli və xarici alimlərdən kimlər bu mövzuda hansı tədqiqatları aparmışdır?
                        3. **Tədqiqatın məqsədi və vəzifələri:** Əsas hədəf və bu hədəfə çatmaq üçün qarşıya qoyulmuş 3-5 konkret vəzifə.
                        4. **Tədqiqatın obyekti və predmeti:** Obyekt ümumi sahəni, predmet isə həmin sahədə öyrənilən konkret proses və qanunauyğunluqları göstərir.
                        5. **Tədqiqat metodları:** Nəzəri analiz, müqayisə, statistik analiz, sorğu, modelləşdirmə və s.
                        6. **Elmi yenilik:** Tədqiqatçı elmə hansı yeni yanaşmanı və ya nəticəni təqdim edir?
                        7. **Praktiki əhəmiyyət:** Əldə olunan nəticələr hansı sahələrdə tətbiq edilə bilər?
                    """.trimIndent(),
                    durationMinutes = 8,
                    author = "BestGroup Elmi Redaksiya",
                    publishedAt = "2024-10-15",
                    viewsCount = 1240
                ),
                AcademyMaterial(
                    id = "acad_2",
                    title = "Magistr Dissertasiyasında Fəsillərin Məntiqi Quruluşu",
                    subtitle = "3 fəsilli standart dissertasiya arxitekturası və fəsillərarası əlaqə",
                    category = AcademyCategory.MASTER_THESIS,
                    type = AcademyContentType.ARTICLE,
                    contentMarkdown = """
                        # Magistr Dissertasiyasının 3 Fəsilli Modeli
                        
                        - **I Fəsil:** Nəzəri-metodoloji əsaslar və ədəbiyyat icmalı (mövcud elmi baxışlar, konsepsiyalar və qanunvericilik bazası).
                        - **II Fəsil:** Mövcud vəziyyətin təhlili və qiymətləndirilməsi (empirik faktlar, statistik göstəricilər, diaqramlar və praktiki problemlər).
                        - **III Fəsil:** İnkişaf istiqamətləri, təkliflər və proqnozlar (müəllifin konkret təklifləri, iqtisadi/elmi səmərəliliyin əsaslandırılması).
                    """.trimIndent(),
                    durationMinutes = 12,
                    author = "Dr. Elmar Məmmədov",
                    publishedAt = "2024-09-28",
                    viewsCount = 980
                ),
                AcademyMaterial(
                    id = "acad_3",
                    title = "Scopus və Web of Science Məqalələri üçün İmpakt Faktor Bələdçisi",
                    subtitle = "Q1-Q4 jurnallarına elmi məqalə göndərərkən nələrə diqqət yetirilməlidir?",
                    category = AcademyCategory.SCIENTIFIC_PAPER,
                    type = AcademyContentType.ARTICLE,
                    contentMarkdown = """
                        # Beynəlxalq İndeksli Jurnallarda Nəşr Strategiyası
                        
                        - IMRAD strukturu (Introduction, Methods, Results, and Discussion).
                        - Q1-Q4 kvartillərinin mənası və seçim qaydaları.
                        - Yırtıcı (Predatory) jurnallardan qorunma üsulları.
                        - Təkrar rəy (Peer-Review) mərhələsində rəyçilərə cavab məktubunun yazılması.
                    """.trimIndent(),
                    durationMinutes = 15,
                    author = "Akademik Şura",
                    publishedAt = "2024-11-01",
                    viewsCount = 1450
                ),
                AcademyMaterial(
                    id = "acad_4",
                    title = "Dissertasiya Müdafiəsi üçün 10 Dəqiqəlik Slayd Çıxışı",
                    subtitle = "Müdafiə komissiyasını inandıran təqdimat texnikası və bədən dili",
                    category = AcademyCategory.PRESENTATION,
                    type = AcademyContentType.ARTICLE,
                    contentMarkdown = """
                        # Uğurlu Müdafiə Çıxışı üçün Qızıl Qaydalar
                        
                        - Hər slayda maksimum 1 dəqiqə: cəmi 10-12 slayd kifayətdir.
                        - Slaydda uzun mətnlər oxunmamalı, yalnız əsas statistik qrafiklər və nəticələr göstərilməlidir.
                        - Şuranın verəcəyi 5 tipik suala əvvəlcədən ehtiyat slaydlar (Backup slides) hazırlamaq.
                    """.trimIndent(),
                    durationMinutes = 10,
                    author = "Təlim Mərkəzi",
                    publishedAt = "2024-10-20",
                    viewsCount = 2100
                ),
                AcademyMaterial(
                    id = "acad_5",
                    title = "APA 7-ci Nəşr İstinad Qaydaları və Nümunələr",
                    subtitle = "Kitab, məqalə, internet mənbələri və dissertasiyalara düzgün istinad forması",
                    category = AcademyCategory.CITATIONS,
                    type = AcademyContentType.ARTICLE,
                    contentMarkdown = """
                        # APA 7 Standartı ilə İstinad Nümunələri
                        
                        - **Mətn daxilində:** (Məmmədov, 2024, s. 45)
                        - **Tək müəllifli kitab:** Məmmədov, E. (2024). *Azərbaycan iqtisadiyyatının inkişafı*. Elm nəşriyyatı.
                        - **İki müəllifli məqalə:** Quliyev, R., & Əliyev, V. (2023). Rəqəmsal transformasiyanın təhlili. *İqtisadi İcmal*, 12(3), 45-58.
                    """.trimIndent(),
                    durationMinutes = 7,
                    author = "BestGroup Standartlar Qrupu",
                    publishedAt = "2024-10-05",
                    viewsCount = 3100
                ),
                AcademyMaterial(
                    id = "acad_6",
                    title = "Microsoft Word-də Avtomatik Mündəricat və Səhifələmə",
                    subtitle = "Başlıq stilləri (Heading 1, 2, 3), bölmələr (Section Break) və cədvəl nömrələmə",
                    category = AcademyCategory.MS_OFFICE,
                    type = AcademyContentType.ARTICLE,
                    contentMarkdown = """
                        # Word-də Peşəkar Akademik Formatlama
                        
                        - Titul vərəqi və mündəricatda nömrənin gizlədilməsi (Section Break -> Link to Previous).
                        - Avtomatik cədvəl və şəkil siyahısının çıxarılması (Insert Caption).
                        - 14 pt Times New Roman, 1.5 sətirarası məsafə və soldan 3 sm, sağdan 1 sm boşluqların tənzimlənməsi.
                    """.trimIndent(),
                    durationMinutes = 9,
                    author = "Formatlama Mütəxəssisi",
                    publishedAt = "2024-09-15",
                    viewsCount = 1890
                )
            )
        }
    }
}
