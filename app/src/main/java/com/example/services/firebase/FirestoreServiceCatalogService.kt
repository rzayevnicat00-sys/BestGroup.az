package com.example.services.firebase

import com.example.model.ServiceCatalogItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreServiceCatalogService(
    private val firestore: FirebaseFirestore
) {
    private val servicesCollection = firestore.collection("services")

    fun getServicesFlow(onlyActive: Boolean = true): Flow<List<ServiceCatalogItem>> = callbackFlow {
        val query: Query = if (onlyActive) {
            servicesCollection.whereEqualTo("active", true).orderBy("sortOrder", Query.Direction.ASCENDING)
        } else {
            servicesCollection.orderBy("sortOrder", Query.Direction.ASCENDING)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // If index is building or offline without local cache yet, attempt unsorted fallback
                servicesCollection.whereEqualTo("active", true).get().addOnSuccessListener { fallbackSnap ->
                    val fallbackList = fallbackSnap.documents.mapNotNull { doc ->
                        mapDocToService(doc.id, doc.data ?: emptyMap())
                    }.sortedBy { it.sortOrder }
                    trySend(fallbackList)
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    mapDocToService(doc.id, doc.data ?: emptyMap())
                }.sortedBy { it.sortOrder }
                trySend(list)
            }
        }

        awaitClose { registration.remove() }
    }

    suspend fun getServiceById(serviceId: String): ServiceCatalogItem? {
        val doc = servicesCollection.document(serviceId).get().await()
        return if (doc.exists()) {
            mapDocToService(doc.id, doc.data ?: emptyMap())
        } else null
    }

    suspend fun seedDefaultServicesIfEmpty(): Boolean {
        return try {
            val existing = servicesCollection.limit(1).get().await()
            if (existing.isEmpty) {
                val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                val batch = firestore.batch()

                val defaultServices = listOf(
                    ServiceCatalogItem(
                        serviceId = "diploma",
                        nameAz = "Diplom işi",
                        nameEn = "Bachelor Diploma Thesis",
                        nameRu = "Дипломная работа",
                        descriptionAz = "Bakalavr pilləsi üzrə metodiki göstərişlərə uyğun, 0% plagiatlıq və yüksək elmi əsaslandırma ilə diplom işinin yazılması.",
                        descriptionEn = "Comprehensive Bachelor's thesis adhering to university standards with guaranteed originality and defense preparation.",
                        descriptionRu = "Написание дипломной работы для бакалавриата по стандартам вуза с гарантией оригинальности.",
                        startingPriceAzn = 250.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "15-25 gün",
                        active = true,
                        icon = "school",
                        sortOrder = 1,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "master",
                        nameAz = "Magistr dissertasiyası",
                        nameEn = "Master's Dissertation",
                        nameRu = "Магистерская диссертация",
                        descriptionAz = "Dərin elmi tədqiqat, beynəlxalq ədəbiyyat icmalı və empirik təhlillərə əsaslanan dissertasiya işi.",
                        descriptionEn = "Rigorous scientific research, extensive literature review, empirical methodology and analysis.",
                        descriptionRu = "Глубокое научное исследование, анализ литературы и эмпирические расчеты.",
                        startingPriceAzn = 450.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "25-40 gün",
                        active = true,
                        icon = "psychology",
                        sortOrder = 2,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "course_work",
                        nameAz = "Kurs işi",
                        nameEn = "Coursework",
                        nameRu = "Курсовая работа",
                        descriptionAz = "İxtisas fənləri üzrə nəzəri və praktiki hissələrdən ibarət akademik kurs işlərinin hazırlanması.",
                        descriptionEn = "Theoretical and applied academic coursework fulfilling course curriculum standards.",
                        descriptionRu = "Подготовка теоретической и практической частей курсового проекта.",
                        startingPriceAzn = 90.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "7-12 gün",
                        active = true,
                        icon = "menu_book",
                        sortOrder = 3,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "project_work",
                        nameAz = "Layihə işi",
                        nameEn = "Project Work",
                        nameRu = "Проектная работа",
                        descriptionAz = "Biznes, mühəndislik və İT sahələrində tətbiqi layihə və keys analizlərinin işlənməsi.",
                        descriptionEn = "Practical projects and case studies for engineering, IT, economics and business.",
                        descriptionRu = "Разработка прикладных проектов и кейс-стади в инженерии и бизнесе.",
                        startingPriceAzn = 100.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "10-15 gün",
                        active = true,
                        icon = "work",
                        sortOrder = 4,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "independent_work",
                        nameAz = "Sərbəst iş",
                        nameEn = "Independent Assignment",
                        nameRu = "Самостоятельная работа",
                        descriptionAz = "Semestr ərzində tələb olunan fərdi tapşırıq və hesabatların vaxtında hazırlanması.",
                        descriptionEn = "Timely preparation of semester independent tasks and individual academic reports.",
                        descriptionRu = "Качественное и своевременное выполнение индивидуальных заданий семестра.",
                        startingPriceAzn = 40.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "3-5 gün",
                        active = true,
                        icon = "assignment",
                        sortOrder = 5,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "report",
                        nameAz = "Referat",
                        nameEn = "Academic Report",
                        nameRu = "Реферат",
                        descriptionAz = "Müasir elmi mənbələr və istinadlar əsasında zəngin məzmunlu referatların tərtibi.",
                        descriptionEn = "Concise and well-cited academic reports based on verified scientific literature.",
                        descriptionRu = "Составление рефератов на основе современных научных публикаций и первоисточников.",
                        startingPriceAzn = 35.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "2-4 gün",
                        active = true,
                        icon = "description",
                        sortOrder = 6,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "essay",
                        nameAz = "Esse",
                        nameEn = "Essay",
                        nameRu = "Эссе",
                        descriptionAz = "Tənqidi təfəkkür, arqumentasiya və akademik yazı üslubunda esselərin qələmə alınması.",
                        descriptionEn = "Persuasive and critical academic essays written in structured academic prose.",
                        descriptionRu = "Академические и критические эссе с четкой аргументацией и безупречным стилем.",
                        startingPriceAzn = 30.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "2-3 gün",
                        active = true,
                        icon = "edit_note",
                        sortOrder = 7,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "presentation",
                        nameAz = "Təqdimat / Slayd",
                        nameEn = "Presentation Slides",
                        nameRu = "Презентация / Слайды",
                        descriptionAz = "PowerPoint / Canva ilə peşəkar dizayn, infoqrafika və anlaşıqlı strukturda slaydlar.",
                        descriptionEn = "Professionally structured PowerPoint/Canva presentations with modern visuals.",
                        descriptionRu = "Профессиональные слайды в PowerPoint/Canva с наглядной инфографикой.",
                        startingPriceAzn = 45.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "2-4 gün",
                        active = true,
                        icon = "slideshow",
                        sortOrder = 8,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "article",
                        nameAz = "Elmi məqalə",
                        nameEn = "Scientific Article",
                        nameRu = "Научная статья",
                        descriptionAz = "Scopus, Web of Science və AAK indeksli jurnalların tələblərinə uyğun elmi məqalələr.",
                        descriptionEn = "High-impact scientific articles formatted for peer-reviewed indexed journals.",
                        descriptionRu = "Научные статьи для публикации в индексируемых и рецензируемых журналах.",
                        startingPriceAzn = 180.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "10-20 gün",
                        active = true,
                        icon = "article",
                        sortOrder = 9,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "statistical_analysis",
                        nameAz = "Statistik analiz",
                        nameEn = "Statistical Analysis",
                        nameRu = "Статистический анализ",
                        descriptionAz = "SPSS, R, Python və Excel vasitəsilə korrelyasiya, reqressiya və hipotez testləri.",
                        descriptionEn = "Empirical data analysis, hypothesis testing and regression using SPSS, R, Python.",
                        descriptionRu = "Обработка и анализ данных с помощью SPSS, R, Python и интерпретация результатов.",
                        startingPriceAzn = 120.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "5-10 gün",
                        active = true,
                        icon = "analytics",
                        sortOrder = 10,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "editing",
                        nameAz = "Redaktə / Korrektə",
                        nameEn = "Proofreading & Editing",
                        nameRu = "Редактура / Корректура",
                        descriptionAz = "Qrammatik, üslub və orfoqrafik xətaların aradan qaldırılması və axıcılığın təmin edilməsi.",
                        descriptionEn = "Thorough proofreading for grammar, syntax, clarity, flow and vocabulary precision.",
                        descriptionRu = "Проверка орфографии, пунктуации, академического стиля и логики изложения.",
                        startingPriceAzn = 50.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "3-5 gün",
                        active = true,
                        icon = "spellcheck",
                        sortOrder = 11,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "formatting",
                        nameAz = "Formatlaşdırma",
                        nameEn = "Formatting & Standards",
                        nameRu = "Форматирование",
                        descriptionAz = "APA, MLA, Harvard, IEEE və GOST standartlarına uyğun səhifələnmə və ədəbiyyat tərtibatı.",
                        descriptionEn = "Strict alignment with APA, MLA, Harvard, IEEE and GOST citation and margin rules.",
                        descriptionRu = "Оформление по стандартам APA, MLA, Harvard, IEEE, ГОСТ и требованиям кафедры.",
                        startingPriceAzn = 40.0,
                        priceType = "starting_from",
                        unit = "project",
                        estimatedDuration = "2-4 gün",
                        active = true,
                        icon = "format_shapes",
                        sortOrder = 12,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "other",
                        nameAz = "Digər",
                        nameEn = "Other Academic Service",
                        nameRu = "Другие услуги",
                        descriptionAz = "Kataloqda qeyd olunmayan digər bütün akademik və tədqiqat sorğuları üçün fərdi qiymətləndirmə.",
                        descriptionEn = "Individual assessment for customized academic and professional service requests.",
                        descriptionRu = "Индивидуальный расчет для нестандартных академических и профессиональных задач.",
                        startingPriceAzn = 0.0,
                        priceType = "quote",
                        unit = "quote",
                        estimatedDuration = "Fərdi",
                        active = true,
                        icon = "more_horiz",
                        sortOrder = 13,
                        createdAt = now,
                        updatedAt = now
                    )
                )

                for (srv in defaultServices) {
                    val docRef = servicesCollection.document(srv.serviceId)
                    val map = hashMapOf<String, Any>(
                        "serviceId" to srv.serviceId,
                        "nameAz" to srv.nameAz,
                        "nameEn" to srv.nameEn,
                        "nameRu" to srv.nameRu,
                        "descriptionAz" to srv.descriptionAz,
                        "descriptionEn" to srv.descriptionEn,
                        "descriptionRu" to srv.descriptionRu,
                        "startingPriceAzn" to srv.startingPriceAzn,
                        "priceType" to srv.priceType,
                        "unit" to srv.unit,
                        "estimatedDuration" to srv.estimatedDuration,
                        "active" to srv.active,
                        "icon" to srv.icon,
                        "sortOrder" to srv.sortOrder,
                        "createdAt" to srv.createdAt,
                        "updatedAt" to srv.updatedAt
                    )
                    batch.set(docRef, map)
                }

                batch.commit().await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Admin Architecture Operations
    suspend fun createService(service: ServiceCatalogItem) {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val docRef = if (service.serviceId.isNotBlank()) {
            servicesCollection.document(service.serviceId)
        } else {
            servicesCollection.document()
        }
        val sId = docRef.id
        val map = hashMapOf<String, Any>(
            "serviceId" to sId,
            "nameAz" to service.nameAz,
            "nameEn" to service.nameEn,
            "nameRu" to service.nameRu,
            "descriptionAz" to service.descriptionAz,
            "descriptionEn" to service.descriptionEn,
            "descriptionRu" to service.descriptionRu,
            "startingPriceAzn" to service.startingPriceAzn,
            "priceType" to service.priceType,
            "unit" to service.unit,
            "estimatedDuration" to service.estimatedDuration,
            "active" to service.active,
            "icon" to service.icon,
            "sortOrder" to service.sortOrder,
            "createdAt" to now,
            "updatedAt" to now
        )
        docRef.set(map).await()
    }

    suspend fun updateService(serviceId: String, updates: Map<String, Any>) {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val fullUpdates = updates + mapOf("updatedAt" to now)
        servicesCollection.document(serviceId).update(fullUpdates).await()
    }

    suspend fun toggleServiceActive(serviceId: String, active: Boolean) {
        updateService(serviceId, mapOf("active" to active))
    }

    suspend fun deleteService(serviceId: String) {
        servicesCollection.document(serviceId).delete().await()
    }

    private fun mapDocToService(id: String, data: Map<String, Any>): ServiceCatalogItem {
        return ServiceCatalogItem(
            serviceId = id,
            nameAz = data["nameAz"] as? String ?: "",
            nameEn = data["nameEn"] as? String ?: "",
            nameRu = data["nameRu"] as? String ?: "",
            descriptionAz = data["descriptionAz"] as? String ?: "",
            descriptionEn = data["descriptionEn"] as? String ?: "",
            descriptionRu = data["descriptionRu"] as? String ?: "",
            startingPriceAzn = (data["startingPriceAzn"] as? Number)?.toDouble() ?: 0.0,
            priceType = data["priceType"] as? String ?: "starting_from",
            unit = data["unit"] as? String ?: "service",
            estimatedDuration = data["estimatedDuration"] as? String ?: "",
            active = data["active"] as? Boolean ?: true,
            icon = data["icon"] as? String ?: "school",
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            createdAt = data["createdAt"] as? String ?: "",
            updatedAt = data["updatedAt"] as? String ?: ""
        )
    }
}
