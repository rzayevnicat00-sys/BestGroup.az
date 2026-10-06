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
                        serviceId = "independent_work",
                        nameAz = "Sərbəst iş / Referat / Esse",
                        nameEn = "Independent Work / Report / Essay",
                        nameRu = "Самостоятельная работа / Реферат / Эссе",
                        descriptionAz = "Tələbə və magistrantlar üçün mövzu üzrə fərdi tapşırıq, referat və esselərin peşəkar standartlara uyğun yazılması.",
                        descriptionEn = "Individual academic assignments, reports and essays written according to strict university standards.",
                        descriptionRu = "Подготовка самостоятельных работ, рефератов и эссе по стандартам высших учебных заведений.",
                        startingPriceAzn = 0.50,
                        maxPriceAzn = 0.0,
                        basePrice = 0.50,
                        priceType = "starting_from",
                        unit = "page",
                        priceUnit = "page",
                        currency = "AZN",
                        estimatedDuration = "1-3 gün",
                        active = true,
                        icon = "assignment",
                        sortOrder = 1,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "presentation",
                        nameAz = "Təqdimat / Slayd",
                        nameEn = "Presentation / Slides",
                        nameRu = "Презентация / Слайды",
                        descriptionAz = "PowerPoint və Canva ilə müasir vizual dizayn, infoqrafika və aydın struktura malik akademik və biznes təqdimatları.",
                        descriptionEn = "High-impact visual presentations designed with infographics and structured delivery in PowerPoint/Canva.",
                        descriptionRu = "Профессионально оформленные слайды и презентации в PowerPoint/Canva с инфографикой.",
                        startingPriceAzn = 20.0,
                        maxPriceAzn = 0.0,
                        basePrice = 20.0,
                        priceType = "starting_from",
                        unit = "project",
                        priceUnit = "project",
                        currency = "AZN",
                        estimatedDuration = "1-2 gün",
                        active = true,
                        icon = "slideshow",
                        sortOrder = 2,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "course_work",
                        nameAz = "Kurs işi",
                        nameEn = "Coursework",
                        nameRu = "Курсовая работа",
                        descriptionAz = "İxtisas fənləri üzrə nəzəri, təhlili və praktiki bölmələrdən ibarət yüksək keyfiyyətli akademik kurs işləri.",
                        descriptionEn = "Comprehensive academic coursework covering theoretical frameworks, applied research and literature review.",
                        descriptionRu = "Курсовые проекты по специальности с теоретической, практической и расчетной частями.",
                        startingPriceAzn = 30.0,
                        maxPriceAzn = 0.0,
                        basePrice = 30.0,
                        priceType = "starting_from",
                        unit = "project",
                        priceUnit = "project",
                        currency = "AZN",
                        estimatedDuration = "5-10 gün",
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
                        descriptionAz = "Mühəndislik, biznes, İT və iqtisadiyyat sahələrində tətbiqi layihələrin və keys araşdırmalarının hazırlanması.",
                        descriptionEn = "Practical projects, technical reports and case studies for engineering, IT, management and economics.",
                        descriptionRu = "Прикладные учебные и инженерные проекты, кейс-стади и практические отчеты.",
                        startingPriceAzn = 30.0,
                        maxPriceAzn = 0.0,
                        basePrice = 30.0,
                        priceType = "starting_from",
                        unit = "project",
                        priceUnit = "project",
                        currency = "AZN",
                        estimatedDuration = "5-12 gün",
                        active = true,
                        icon = "work",
                        sortOrder = 4,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "article",
                        nameAz = "Elmi məqalə",
                        nameEn = "Scientific Article",
                        nameRu = "Научная статья",
                        descriptionAz = "Yerli və beynəlxalq indeksli jurnalların (Scopus, Web of Science, AAK) tələblərinə uyğun elmi məqalələrin tərtibi.",
                        descriptionEn = "Academic articles structured and referenced for indexed peer-reviewed scientific journals.",
                        descriptionRu = "Научные статьи для публикации в рецензируемых и индексируемых журналах ВАК и Scopus.",
                        startingPriceAzn = 25.0,
                        maxPriceAzn = 0.0,
                        basePrice = 25.0,
                        priceType = "starting_from",
                        unit = "project",
                        priceUnit = "project",
                        currency = "AZN",
                        estimatedDuration = "7-15 gün",
                        active = true,
                        icon = "article",
                        sortOrder = 5,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "statistical_analysis",
                        nameAz = "Statistik analiz",
                        nameEn = "Statistical Analysis",
                        nameRu = "Статистический анализ",
                        descriptionAz = "SPSS, R, Python və Excel vasitəsilə empirik məlumatların emalı, hipotez sınaqları və reqressiya analizləri.",
                        descriptionEn = "Empirical data processing, correlation/regression modeling and hypothesis testing in SPSS, R, Python.",
                        descriptionRu = "Обработка статистических данных, корреляционный и регрессионный анализ в SPSS, R, Python.",
                        startingPriceAzn = 15.0,
                        maxPriceAzn = 0.0,
                        basePrice = 15.0,
                        priceType = "starting_from",
                        unit = "project",
                        priceUnit = "project",
                        currency = "AZN",
                        estimatedDuration = "3-7 gün",
                        active = true,
                        icon = "analytics",
                        sortOrder = 6,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "editing",
                        nameAz = "Redaktə / Korrektə",
                        nameEn = "Editing / Proofreading",
                        nameRu = "Редактирование / Корректура",
                        descriptionAz = "Qrammatik, üslub, leksik və durğu işarələri xətalarının düzəldilməsi və mətnin akademik səliqəyə salınması.",
                        descriptionEn = "Professional copyediting and proofreading for academic style, grammar, syntax and punctuation.",
                        descriptionRu = "Вычитка и исправление грамматических, пунктуационных и стилистических ошибок.",
                        startingPriceAzn = 15.0,
                        maxPriceAzn = 0.0,
                        basePrice = 15.0,
                        priceType = "starting_from",
                        unit = "project",
                        priceUnit = "project",
                        currency = "AZN",
                        estimatedDuration = "1-3 gün",
                        active = true,
                        icon = "spellcheck",
                        sortOrder = 7,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "formatting",
                        nameAz = "Formatlaşdırma",
                        nameEn = "Formatting",
                        nameRu = "Форматирование",
                        descriptionAz = "APA, MLA, Harvard, IEEE və GOST standartlarına əsasən şrift, paraqraf, cədvəl və ədəbiyyat siyahısı tərtibatı.",
                        descriptionEn = "Standard formatting for citations, bibliography, typography and margins in APA, MLA, Harvard, IEEE.",
                        descriptionRu = "Оформление списков литературы, таблиц и структуры текста по стандартам APA, MLA, ГОСТ.",
                        startingPriceAzn = 15.0,
                        maxPriceAzn = 0.0,
                        basePrice = 15.0,
                        priceType = "starting_from",
                        unit = "project",
                        priceUnit = "project",
                        currency = "AZN",
                        estimatedDuration = "1-2 gün",
                        active = true,
                        icon = "format_shapes",
                        sortOrder = 8,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "diploma",
                        nameAz = "Diplom işi",
                        nameEn = "Bachelor's Thesis (Diploma)",
                        nameRu = "Дипломная работа (Бакалавриат)",
                        descriptionAz = "Bakalavr pilləsi üzrə metodiki göstərişlərə uyğun, 0% plagiat zəmanəti və elmi əsaslandırma ilə diplom işi.",
                        descriptionEn = "Bachelor's degree thesis compliant with university guidelines and complete originality guarantee.",
                        descriptionRu = "Написание бакалаврской дипломной работы по методическим указаниям с защитой от плагиата.",
                        startingPriceAzn = 400.0,
                        maxPriceAzn = 500.0,
                        basePrice = 400.0,
                        priceType = "range",
                        unit = "project",
                        priceUnit = "project",
                        currency = "AZN",
                        estimatedDuration = "15-25 gün",
                        active = true,
                        icon = "school",
                        sortOrder = 9,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "master",
                        nameAz = "Magistr dissertasiyası",
                        nameEn = "Master's Dissertation",
                        nameRu = "Магистерская диссертация",
                        descriptionAz = "Dərin elmi tədqiqat, beynəlxalq ədəbiyyat icmalı və empirik metodologiyaya əsaslanan dissertasiya işi.",
                        descriptionEn = "Rigorous master's dissertation with in-depth literature review, empirical research and defense support.",
                        descriptionRu = "Магистерская диссертация с фундаментальным анализом литературы и практической базой.",
                        startingPriceAzn = 700.0,
                        maxPriceAzn = 800.0,
                        basePrice = 700.0,
                        priceType = "range",
                        unit = "project",
                        priceUnit = "project",
                        currency = "AZN",
                        estimatedDuration = "25-40 gün",
                        active = true,
                        icon = "psychology",
                        sortOrder = 10,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ServiceCatalogItem(
                        serviceId = "phd_dissertation",
                        nameAz = "Doktorantura dissertasiyası",
                        nameEn = "PhD Dissertation",
                        nameRu = "Докторская диссертация",
                        descriptionAz = "Doktorantura və dissertantura pillələri üçün fəlsəfə doktoru və elmlər doktoru dissertasiyaları üzrə akademik konsultasiya.",
                        descriptionEn = "Academic consulting and methodology support for PhD doctoral dissertations and research.",
                        descriptionRu = "Научно-консультационная поддержка диссертаций на соискание ученой степени доктора философии (PhD).",
                        startingPriceAzn = 0.0,
                        maxPriceAzn = 0.0,
                        basePrice = 0.0,
                        priceType = "quote",
                        unit = "quote",
                        priceUnit = "quote",
                        currency = "AZN",
                        estimatedDuration = "Fərdi",
                        active = true,
                        icon = "school",
                        sortOrder = 11,
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
                        maxPriceAzn = 0.0,
                        basePrice = 0.0,
                        priceType = "quote",
                        unit = "quote",
                        priceUnit = "quote",
                        currency = "AZN",
                        estimatedDuration = "Fərdi",
                        active = true,
                        icon = "more_horiz",
                        sortOrder = 12,
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
                        "maxPriceAzn" to srv.maxPriceAzn,
                        "basePrice" to srv.basePrice,
                        "priceType" to srv.priceType,
                        "unit" to srv.unit,
                        "priceUnit" to srv.priceUnit,
                        "currency" to srv.currency,
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
            "maxPriceAzn" to service.maxPriceAzn,
            "basePrice" to service.basePrice,
            "priceType" to service.priceType,
            "unit" to service.unit,
            "priceUnit" to service.priceUnit,
            "currency" to service.currency,
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
            maxPriceAzn = (data["maxPriceAzn"] as? Number)?.toDouble() ?: 0.0,
            basePrice = (data["basePrice"] as? Number)?.toDouble() ?: ((data["startingPriceAzn"] as? Number)?.toDouble() ?: 0.0),
            priceType = data["priceType"] as? String ?: "starting_from",
            unit = data["unit"] as? String ?: "service",
            priceUnit = data["priceUnit"] as? String ?: (data["unit"] as? String ?: "service"),
            currency = data["currency"] as? String ?: "AZN",
            estimatedDuration = data["estimatedDuration"] as? String ?: "",
            active = data["active"] as? Boolean ?: true,
            icon = data["icon"] as? String ?: "school",
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            createdAt = data["createdAt"] as? String ?: "",
            updatedAt = data["updatedAt"] as? String ?: ""
        )
    }
}
