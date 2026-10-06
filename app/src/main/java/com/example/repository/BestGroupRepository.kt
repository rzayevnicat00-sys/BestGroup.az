package com.example.repository

import com.example.localization.Language
import com.example.localization.LocalizationManager
import com.example.model.*
import com.example.services.firebase.*
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BestGroupRepository(
    val authService: FirebaseAuthService? = null,
    val orderService: FirestoreOrderService? = null,
    val chatService: FirestoreChatService? = null,
    val storageService: FirebaseStorageService? = null,
    val serviceCatalogService: FirestoreServiceCatalogService? = null
) {

    // Current User Session
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Auth state loading
    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // App Preferences
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    // Role switcher (useful to inspect customer experience vs admin dashboard)
    fun switchUserRole(newRole: UserRole) {
        _currentUser.value = _currentUser.value?.copy(role = newRole)
    }

    fun updateUserProfile(fullName: String, phone: String, university: String, faculty: String, degreeLevel: String) {
        _currentUser.value = _currentUser.value?.copy(
            fullName = fullName,
            phone = phone,
            university = university,
            faculty = faculty,
            degreeLevel = degreeLevel
        )
    }

    // Real Firestore Catalog Services
    private val _catalogServices = MutableStateFlow<List<ServiceCatalogItem>>(getDefaultCatalogServices())
    val catalogServices: StateFlow<List<ServiceCatalogItem>> = _catalogServices.asStateFlow()

    // Legacy Services Catalog (for backwards compatibility)
    private val _services = MutableStateFlow(
        ServiceType.values().map { type ->
            ServiceItem(
                id = "srv_${type.name.lowercase()}",
                type = type,
                isFavorite = type in listOf(ServiceType.DIPLOMA, ServiceType.MASTER, ServiceType.ARTICLE)
            )
        }
    )
    val services: StateFlow<List<ServiceItem>> = _services.asStateFlow()

    fun toggleFavoriteService(serviceId: String) {
        _services.value = _services.value.map { item ->
            if (item.id == serviceId) item.copy(isFavorite = !item.isFavorite) else item
        }
    }

    // Orders Database
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    // Chat Conversations & Messages
    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(
        listOf(
            NotificationItem(
                id = "n_1",
                title = "BestGroup.az platformasına xoş gəlmisiniz",
                body = "Firebase bulud bazası və təhlükəsizlik qaydaları aktivləşdirildi.",
                category = NotificationCategory.SYSTEM,
                timestamp = "İndicə",
                isRead = false
            ),
            NotificationItem(
                id = "n_2",
                title = "Payız Kampaniyası: Magistraturaya 15% Endirim",
                body = "Bütün dissertasiya və elmi məqalə sifarişlərinə 15% güzəşt tətbiq olunur.",
                category = NotificationCategory.CAMPAIGNS,
                timestamp = "Dünən",
                isRead = true
            )
        )
    )
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // FAQ items
    private val _faqs = MutableStateFlow(
        listOf(
            FaqItem(
                id = "faq_1",
                category = FaqCategory.QUALITY,
                questionAz = "Hazırlanan elmi işlərdə anti-plagiat yoxlanışı necə aparılır?",
                questionEn = "How is anti-plagiarism verification carried out?",
                questionRu = "Как проводится проверка на антиплагиат?",
                answerAz = "Bütün akademik işlər təhvil verilməzdən əvvəl beynəlxalq 'Turnitin' və rəsmi Azərbaycan AAK anti-plagiat sistemlərində yoxlanılır. Sifarişlə birlikdə rəsmi faiz hesabatı təqdim edilir.",
                answerEn = "All academic works are verified using Turnitin and official databases before delivery. An official plagiarism certificate is provided with the final order.",
                answerRu = "Все академические работы проверяются через Turnitin и официальные системы ВАК перед сдачей. Вместе с работой выдается официальный отчет об оригинальности."
            ),
            FaqItem(
                id = "faq_2",
                category = FaqCategory.ORDERING,
                questionAz = "Müəllim və ya kafedra tərəfindən düzəliş tələb olunarsa, bu pulsuzdurmu?",
                questionEn = "Are revisions free of charge if requested by my professor?",
                questionRu = "Бесплатны ли доработки по замечаниям научного руководителя?",
                answerAz = "Bəli! İlkin razılaşdırılmış plana və metodikaya uyğun olaraq elmi rəhbərin bütün tövsiyə və qeydləri üzrə düzəlişlər 100% pulsuz və operativ şəkildə həyata keçirilir.",
                answerEn = "Yes! All revisions aligned with the initial scope and supervisor guidelines are executed 100% free of charge and with highest priority.",
                answerRu = "Да! Все доработки по замечаниям научного руководителя в рамках согласованного плана выполняются абсолютно бесплатно и в кратчайшие сроки."
            ),
            FaqItem(
                id = "faq_3",
                category = FaqCategory.PAYMENT,
                questionAz = "Ödəniş prosesi necə həyata keçirilir?",
                questionEn = "How is the payment processed?",
                questionRu = "Как происходит процесс оплаты?",
                answerAz = "Ödəniş 2 mərhələdə həyata keçirilir: 50% ilkin beh sifariş təsdiqləndikdən sonra, qalan 50% isə işin yekun qaralamasını görüb razı qaldıqdan sonra ödənilir.",
                answerEn = "Payment is divided into two phases: 50% advance upon order approval, and the remaining 50% only after reviewing the draft and final approval.",
                answerRu = "Оплата разделена на 2 этапа: 50% предоплата после подтверждения заказа и остальные 50% после ознакомления с готовой работой."
            ),
            FaqItem(
                id = "faq_4",
                category = FaqCategory.PRIVACY,
                questionAz = "Şəxsi məlumatlarım və sifariş etdiyim mövzu məxfi saxlanılırmı?",
                questionEn = "Are my personal details and project confidential?",
                questionRu = "Остаются ли мои персональные данные и тема конфиденциальными?",
                answerAz = "BestGroup.az 100% məxfilik müqaviləsi ilə fəaliyyət göstərir. Sizin adınız, əlaqə vasitələriniz və universitetiniz heç vaxt üçüncü tərəflərə ötürülmür.",
                answerEn = "BestGroup operates under strict non-disclosure terms. Your identity, contact numbers, and university data are strictly protected and never shared.",
                answerRu = "BestGroup гарантирует 100% конфиденциальность. Ваши контактные данные, имя и название вуза строго защищены и никогда не передаются третьим лицам."
            )
        )
    )
    val faqs: StateFlow<List<FaqItem>> = _faqs.asStateFlow()

    // Admin Dashboard Statistics
    val adminStats: StateFlow<AdminDashboardStats> = MutableStateFlow(
        AdminDashboardStats(
            todayOrdersCount = 8,
            activeOrdersCount = 24,
            pendingApprovalCount = 5,
            readyOrdersCount = 142,
            overdueOrdersCount = 0,
            monthlyRevenueAzn = 18450,
            newUsersCount = 67,
            completionRate = 98
        )
    ).asStateFlow()

    // --- Firebase Initialization & Synchronization ---

    fun initFirebaseObservers(scope: CoroutineScope) {
        if (authService == null) {
            initSampleDataIfEmpty()
            return
        }

        scope.launch {
            authService.authStateFlow().collect { fbUser ->
                if (fbUser != null) {
                    val profile = authService.fetchUserProfile(fbUser.uid)
                    if (profile != null) {
                        _currentUser.value = profile
                    } else {
                        _currentUser.value = User(
                            id = fbUser.uid,
                            fullName = fbUser.displayName ?: "İstifadəçi",
                            email = fbUser.email ?: "",
                            phone = "",
                            university = "",
                            faculty = "",
                            degreeLevel = "Bakalavriat",
                            role = UserRole.CUSTOMER
                        )
                    }
                    observeFirestoreData(scope, fbUser.uid)
                } else {
                    _currentUser.value = null
                    _orders.value = emptyList()
                    _conversations.value = emptyList()
                }
            }
        }
    }

    private fun observeFirestoreData(scope: CoroutineScope, userId: String) {
        val isStaff = _currentUser.value?.role in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.OPERATOR)

        if (serviceCatalogService != null) {
            scope.launch {
                try {
                    serviceCatalogService.seedDefaultServicesIfEmpty()
                } catch (_: Exception) {}
                serviceCatalogService.getServicesFlow(onlyActive = !isStaff).collect { remoteServices ->
                    if (remoteServices.isNotEmpty()) {
                        _catalogServices.value = remoteServices
                    }
                }
            }
        }

        if (orderService != null) {
            scope.launch {
                orderService.getOrdersFlow(userId, isStaff).collect { remoteOrders ->
                    _orders.value = remoteOrders
                }
            }
        }

        if (chatService != null) {
            scope.launch {
                chatService.getConversationsFlow(userId, isStaff).collect { remoteConvs ->
                    if (remoteConvs.isNotEmpty()) {
                        _conversations.value = remoteConvs
                    }
                }
            }
        }
    }

    private fun initSampleDataIfEmpty() {
        if (_orders.value.isEmpty()) {
            _orders.value = listOf(
                Order(
                    id = "ord_101",
                    orderNumber = "BG-2026-8492",
                    serviceType = ServiceType.MASTER,
                    topic = "Süni İntellekt və Böyük Verilənlərin Maliyyə Risklərinin Modelləşdirilməsində Tətbiqi",
                    scopeDescription = "Bank sektorunda kredit risklərinin təhlili, maşın öyrənməsi alqoritmləri ilə skorinq modellərinin qurulması və empirik nəticələr.",
                    university = "Azərbaycan Dövlət İqtisad Universiteti (UNEC)",
                    faculty = "Maliyyə və Mühasibat",
                    academicLevel = "Magistratura",
                    language = "Azərbaycan dili",
                    pageCount = 85,
                    deadline = "15 Oktyabr 2026",
                    priority = OrderPriority.HIGH,
                    formattingStandard = "APA 7th Edition",
                    specialNotes = "Fəsil 2-də Python ilə reqressiya analizi qrafikləri və interpretasiyaları ətraflı göstərilməlidir.",
                    status = OrderStatus.IN_PROGRESS,
                    progressPercent = 65,
                    createdAt = "12 Sentyabr 2026",
                    updatedAt = "27 Sentyabr 2026",
                    files = listOf(
                        OrderAttachedFile("f1", "Metodik_Gosteris_UNEC_2026.pdf", 2450000, "pdf"),
                        OrderAttachedFile("f2", "Data_Bank_Risks_Sample.xlsx", 890000, "xlsx"),
                        OrderAttachedFile("f3", "Dissertasiya_Plan_Layihəsi.docx", 450000, "docx")
                    ),
                    statusHistory = listOf(
                        OrderStatusHistoryItem(OrderStatus.PENDING, "12 Sentyabr 2026, 14:30", "Sifariş müştəri tərəfindən qeydə alındı."),
                        OrderStatusHistoryItem(OrderStatus.ACCEPTED, "13 Sentyabr 2026, 10:15", "Akademik şura tərəfindən təsdiqləndi və ixtisas kuratoru təyin olundu.", "M. İsmayılov (Aparıcı Mütəxəssis)"),
                        OrderStatusHistoryItem(OrderStatus.IN_PROGRESS, "18 Sentyabr 2026, 16:40", "1-ci və 2-ci fəsillər yazılır, məlumat bazası strukturlaşdırıldı.", "A. Qasımov (Elmi Kurator)")
                    ),
                    estimatedPriceAzn = 480
                )
            )
        }

        if (_conversations.value.isEmpty()) {
            _conversations.value = listOf(
                ChatConversation(
                    id = "conv_1",
                    title = "BG-2026-8492 Kuratorluğu",
                    orderId = "ord_101",
                    curatorName = "Məmməd İsmayılov",
                    curatorRole = "Böyük Elmi Kurator",
                    lastMessageText = "Fəsil 2-nin ilkin qaralamasını nəzərdən keçirə bilərsiniz.",
                    lastMessageTime = "14:20",
                    unreadCount = 1,
                    isOnline = true
                ),
                ChatConversation(
                    id = "conv_2",
                    title = "Ümumi Akademik Dəstək",
                    orderId = null,
                    curatorName = "BestGroup Müştəri Xidmətləri",
                    curatorRole = "Dəstək Operatoru",
                    lastMessageText = "Salam, sizə necə kömək edə bilərik?",
                    lastMessageTime = "Dünən",
                    unreadCount = 0,
                    isOnline = true
                )
            )

            _messages.value = mapOf(
                "conv_1" to listOf(
                    ChatMessage("m1", "conv_1", "Məmməd İsmayılov", false, "Salam! Mövzu və metodik göstərişləriniz təsdiqləndi.", "10:15"),
                    ChatMessage("m2", "conv_1", "İstifadəçi", true, "Salam Məmməd müəllim, çox sağ olun. Reqressiya analizi üçün bazanı da əlavə etmişdim.", "10:20"),
                    ChatMessage("m3", "conv_1", "Məmməd İsmayılov", false, "Bəli, məlumat bazası yoxlanıldı, STATA modelləşdirməsi tam qaydasındadır.", "10:25"),
                    ChatMessage("m4", "conv_1", "Məmməd İsmayılov", false, "Fəsil 2-nin ilkin qaralamasını nəzərdən keçirə bilərsiniz.", "14:20")
                ),
                "conv_2" to listOf(
                    ChatMessage("m5", "conv_2", "BestGroup Müştəri Xidmətləri", false, "Salam! BestGroup.az-a xoş gəlmisiniz. 24/7 dəstək xidməti aktivdir.", "09:00"),
                    ChatMessage("m6", "conv_2", "BestGroup Müştəri Xidmətləri", false, "Salam, sizə necə kömək edə bilərik?", "Dünən")
                )
            )
        }
    }

    // --- Authentication Operations ---

    suspend fun signIn(email: String, pass: String): AuthResult<User> {
        _isAuthLoading.value = true
        return try {
            if (authService != null) {
                val res = authService.signIn(email, pass)
                if (res is AuthResult.Success) {
                    _currentUser.value = res.data
                }
                res
            } else {
                val fallbackUser = User(
                    id = "usr_${UUID.randomUUID()}",
                    fullName = "Nicat Rzayev",
                    email = email,
                    phone = "+994 (50) 412-38-90",
                    university = "ADNSU",
                    faculty = "İTİ",
                    degreeLevel = "Magistratura",
                    role = UserRole.CUSTOMER
                )
                _currentUser.value = fallbackUser
                AuthResult.Success(fallbackUser)
            }
        } finally {
            _isAuthLoading.value = false
        }
    }

    suspend fun signUp(
        email: String,
        pass: String,
        fullName: String,
        phone: String,
        university: String,
        faculty: String,
        educationLevel: String
    ): AuthResult<User> {
        _isAuthLoading.value = true
        return try {
            if (authService != null) {
                val res = authService.signUp(email, pass, fullName, phone, university, faculty, educationLevel)
                if (res is AuthResult.Success) {
                    _currentUser.value = res.data
                }
                res
            } else {
                val fallbackUser = User(
                    id = "usr_${UUID.randomUUID()}",
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    university = university,
                    faculty = faculty,
                    degreeLevel = educationLevel,
                    role = UserRole.CUSTOMER
                )
                _currentUser.value = fallbackUser
                AuthResult.Success(fallbackUser)
            }
        } finally {
            _isAuthLoading.value = false
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult<Unit> {
        return authService?.sendPasswordReset(email) ?: AuthResult.Success(Unit)
    }

    suspend fun resendVerificationEmail(): AuthResult<Unit> {
        return authService?.resendVerificationEmail() ?: AuthResult.Success(Unit)
    }

    suspend fun checkEmailVerified(): Boolean {
        return authService?.checkEmailVerification() ?: true
    }

    fun isEmailVerified(): Boolean {
        return authService?.isEmailVerified ?: true
    }

    fun isUserLoggedIn(): Boolean {
        return authService?.isUserLoggedIn ?: (_currentUser.value != null)
    }

    val currentUserId: String
        get() = _currentUser.value?.id ?: authService?.currentUserId ?: ""

    fun signOut() {
        authService?.signOut()
        _currentUser.value = null
        _orders.value = emptyList()
        _conversations.value = emptyList()
    }

    // --- Order Operations ---

    suspend fun createOrder(
        serviceType: ServiceType,
        topic: String,
        scopeDescription: String,
        university: String,
        faculty: String,
        academicLevel: String,
        language: String,
        pageCount: Int,
        deadline: String,
        priority: OrderPriority,
        formattingStandard: String,
        specialNotes: String,
        files: List<OrderAttachedFile> = emptyList(),
        serviceId: String = "",
        serviceName: String = "",
        serviceCatalogItem: ServiceCatalogItem? = null
    ): Order {
        val currentUid = _currentUser.value?.id ?: authService?.currentUserId ?: ""
        if (currentUid.isBlank()) {
            throw IllegalStateException("Sifariş yaratmaq üçün zəhmət olmasa daxil olun.")
        }

        com.example.services.firebase.FirestoreOrderService.validateDeadlineNotInPast(deadline)

        val resolvedServiceItem = serviceCatalogItem
            ?: _catalogServices.value.find { it.serviceId == serviceId }
            ?: _catalogServices.value.find { it.serviceId.equals(serviceType.name, ignoreCase = true) }

        if (resolvedServiceItem != null && !resolvedServiceItem.active) {
            throw IllegalStateException("Seçilmiş xidmət hazırda aktiv deyil.")
        }

        val createdOrder = if (orderService != null) {
            orderService.createOrder(
                userId = currentUid,
                serviceId = serviceId.ifBlank { resolvedServiceItem?.serviceId ?: serviceType.name.lowercase() },
                serviceName = serviceName.ifBlank { resolvedServiceItem?.nameAz ?: serviceType.name },
                serviceType = serviceType,
                topic = topic,
                scopeDescription = scopeDescription,
                university = university,
                faculty = faculty,
                academicLevel = academicLevel,
                language = language,
                pageCount = pageCount,
                deadline = deadline,
                priority = priority,
                formattingStandard = formattingStandard,
                specialNotes = specialNotes,
                files = files,
                serviceCatalogItem = resolvedServiceItem
            )
        } else {
            val dateFmt = SimpleDateFormat("dd MMMM yyyy", Locale("az"))
            val now = dateFmt.format(Date())
            val orderNum = "#BG-${SimpleDateFormat("yyyy", Locale.US).format(Date())}-${(1000..9999).random()}"
            val calculatedPrice = when {
                resolvedServiceItem != null -> {
                    when {
                        resolvedServiceItem.priceType.equals("quote", ignoreCase = true) -> 0
                        resolvedServiceItem.unit.equals("page", ignoreCase = true) || resolvedServiceItem.priceUnit.equals("page", ignoreCase = true) -> {
                            val total = resolvedServiceItem.startingPriceAzn * (if (pageCount > 0) pageCount else 1)
                            Math.max(1, Math.round(total).toInt())
                        }
                        else -> resolvedServiceItem.startingPriceAzn.toInt()
                    }
                }
                serviceType == ServiceType.INDEPENDENT_WORK || serviceType == ServiceType.REPORT || serviceType == ServiceType.ESSAY -> {
                    Math.max(1, Math.round(0.50 * (if (pageCount > 0) pageCount else 1)).toInt())
                }
                else -> serviceType.startingPriceAzn
            }
            Order(
                id = "ord_${UUID.randomUUID()}",
                orderNumber = orderNum,
                userId = currentUid,
                serviceId = serviceId.ifBlank { resolvedServiceItem?.serviceId ?: serviceType.name.lowercase() },
                serviceName = serviceName.ifBlank { resolvedServiceItem?.nameAz ?: serviceType.name },
                serviceType = serviceType,
                topic = topic,
                scopeDescription = scopeDescription,
                university = university,
                faculty = faculty,
                academicLevel = academicLevel,
                language = language,
                pageCount = pageCount,
                deadline = deadline,
                priority = priority,
                formattingStandard = formattingStandard,
                specialNotes = specialNotes,
                additionalNotes = specialNotes,
                status = OrderStatus.PENDING,
                progressPercent = 15,
                createdAt = now,
                updatedAt = now,
                files = files,
                statusHistory = listOf(
                    OrderStatusHistoryItem(OrderStatus.PENDING, "$now, 10:00", "Sifariş qəbul edildi və nəzərdən keçirilməyə göndərildi.", "Sistem")
                ),
                estimatedPriceAzn = calculatedPrice
            )
        }

        _orders.value = listOf(createdOrder) + _orders.value.filter { it.id != createdOrder.id }

        addNotification(
            NotificationItem(
                id = "notif_${UUID.randomUUID()}",
                title = "Sifarişiniz qeydə alındı (${createdOrder.orderNumber})",
                body = "'${createdOrder.topic}' mövzulu sifarişiniz qəbul edildi. Tezliklə kurator sizinlə əlaqə quracaq.",
                category = NotificationCategory.ORDERS,
                timestamp = "İndicə",
                targetOrderId = createdOrder.id
            )
        )

        return createdOrder
    }

    suspend fun uploadOrderFiles(
        orderId: String,
        files: List<OrderAttachedFile>,
        onProgress: ((fileId: String, progress: Float) -> Unit)? = null
    ): List<OrderAttachedFile> {
        val currentUid = _currentUser.value?.id ?: authService?.currentUserId ?: "guest"
        val updatedFiles = mutableListOf<OrderAttachedFile>()

        for (file in files) {
            if (file.localUri != null && storageService != null) {
                try {
                    val uploaded = storageService.uploadOrderFileWithUri(
                        userId = currentUid,
                        orderId = orderId,
                        uri = android.net.Uri.parse(file.localUri),
                        originalFileName = file.name,
                        sizeBytes = file.sizeBytes,
                        mimeType = file.mimeType,
                        fileId = file.id,
                        onProgress = { p -> onProgress?.invoke(file.id, p) }
                    )
                    updatedFiles.add(uploaded)
                } catch (e: Exception) {
                    updatedFiles.add(
                        file.copy(
                            state = FileUploadState.FAILED,
                            progress = 0f,
                            errorMessage = FirebaseStorageService.mapStorageError(e)
                        )
                    )
                }
            } else if (file.localUri != null && storageService == null) {
                val ext = file.extension.ifBlank { FirebaseStorageService.extractExtension(file.name) }
                updatedFiles.add(
                    file.copy(
                        state = FileUploadState.UPLOADED,
                        progress = 1.0f,
                        storagePath = "users/$currentUid/orders/$orderId/files/${file.id}.$ext"
                    )
                )
            } else {
                updatedFiles.add(file)
            }
        }

        if (orderService != null) {
            try {
                orderService.updateOrderFiles(orderId, updatedFiles)
            } catch (_: Exception) {}
        }

        _orders.value = _orders.value.map { order ->
            if (order.id == orderId) {
                order.copy(files = updatedFiles)
            } else order
        }

        return updatedFiles
    }

    suspend fun retryFileUpload(
        orderId: String,
        fileId: String,
        onProgress: ((progress: Float) -> Unit)? = null
    ): OrderAttachedFile? {
        val currentUid = _currentUser.value?.id ?: authService?.currentUserId ?: "guest"
        val order = _orders.value.find { it.id == orderId } ?: return null
        val targetFile = order.files.find { it.id == fileId } ?: return null

        val updatedFile = if (targetFile.localUri != null && storageService != null) {
            try {
                storageService.uploadOrderFileWithUri(
                    userId = currentUid,
                    orderId = orderId,
                    uri = android.net.Uri.parse(targetFile.localUri),
                    originalFileName = targetFile.name,
                    sizeBytes = targetFile.sizeBytes,
                    mimeType = targetFile.mimeType,
                    fileId = targetFile.id,
                    onProgress = onProgress
                )
            } catch (e: Exception) {
                targetFile.copy(
                    state = FileUploadState.FAILED,
                    errorMessage = FirebaseStorageService.mapStorageError(e)
                )
            }
        } else {
            targetFile.copy(state = FileUploadState.UPLOADED, progress = 1f)
        }

        val newFiles = order.files.map { if (it.id == fileId) updatedFile else it }
        if (orderService != null) {
            try {
                orderService.updateOrderFiles(orderId, newFiles)
            } catch (_: Exception) {}
        }
        _orders.value = _orders.value.map { if (it.id == orderId) it.copy(files = newFiles) else it }
        return updatedFile
    }

    fun cancelFileUpload(fileId: String): Boolean {
        return storageService?.cancelUpload(fileId) ?: false
    }

    suspend fun getFileDownloadUrl(storagePath: String): String? {
        return try {
            storageService?.getFileDownloadUrl(storagePath)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun createCatalogService(service: ServiceCatalogItem) {
        serviceCatalogService?.createService(service)
    }

    suspend fun updateCatalogService(serviceId: String, updates: Map<String, Any>) {
        serviceCatalogService?.updateService(serviceId, updates)
    }

    suspend fun toggleServiceActive(serviceId: String, active: Boolean) {
        serviceCatalogService?.toggleServiceActive(serviceId, active)
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus, note: String, actor: String = "Admin") {
        if (orderService != null) {
            orderService.updateOrderStatus(orderId, newStatus, note, actor)
        }
        val dateFmt = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("az"))
        val now = dateFmt.format(Date())
        _orders.value = _orders.value.map { order ->
            if (order.id == orderId) {
                val newHistory = order.statusHistory + OrderStatusHistoryItem(newStatus, now, note, actor)
                val newProgress = when (newStatus) {
                    OrderStatus.PENDING -> 15
                    OrderStatus.ACCEPTED -> 35
                    OrderStatus.IN_PROGRESS -> 70
                    OrderStatus.READY -> 100
                    OrderStatus.CANCELLED -> 0
                }
                order.copy(
                    status = newStatus,
                    progressPercent = newProgress,
                    statusHistory = newHistory,
                    updatedAt = now
                )
            } else order
        }
    }

    suspend fun updateOrderPriority(orderId: String, newPriority: OrderPriority) {
        if (orderService != null) {
            orderService.updateOrderPriority(orderId, newPriority)
        }
        _orders.value = _orders.value.map { order ->
            if (order.id == orderId) order.copy(priority = newPriority) else order
        }
    }

    suspend fun submitOrderReview(orderId: String, rating: Int, comment: String) {
        if (orderService != null) {
            orderService.submitOrderReview(orderId, rating, comment)
        }
        val dateFmt = SimpleDateFormat("dd MMMM yyyy", Locale("az"))
        val now = dateFmt.format(Date())
        _orders.value = _orders.value.map { order ->
            if (order.id == orderId) {
                order.copy(review = OrderReview(rating, comment, now))
            } else order
        }
    }

    private var activeChatJob: kotlinx.coroutines.Job? = null

    suspend fun getOrCreateConversationForOrder(order: Order): ChatConversation {
        val currentUid = authService?.currentUserId ?: _currentUser.value?.id ?: ""
        if (currentUid.isBlank()) {
            throw IllegalStateException("Söhbətə daxil olmaq üçün zəhmət olmasa sistemə daxil olun.")
        }
        if (order.userId.isNotBlank() && currentUid.isNotBlank() && order.userId != currentUid) {
            throw IllegalStateException("Bu sifarişə və onun söhbətinə giriş icazəniz yoxdur.")
        }

        val convId = FirestoreChatService.deterministicOrderConversationId(order.id)

        if (chatService != null) {
            try {
                val conv = chatService.getOrCreateConversationForOrder(
                    orderId = order.id,
                    orderNumber = order.orderNumber,
                    topic = order.topic,
                    customerId = currentUid,
                    customerName = _currentUser.value?.fullName ?: "Müştəri"
                )
                val existing = _conversations.value.find { it.id == conv.id }
                if (existing == null) {
                    _conversations.value = listOf(conv) + _conversations.value
                } else {
                    _conversations.value = _conversations.value.map { if (it.id == conv.id) conv else it }
                }
                return conv
            } catch (_: Exception) {
                // fall through to local fallback on network issue
            }
        }

        val existing = _conversations.value.find { it.id == convId }
        if (existing != null) return existing

        val newConv = ChatConversation(
            id = convId,
            customerId = currentUid,
            orderId = order.id,
            title = "${order.orderNumber} Kuratorluğu",
            lastMessage = "",
            lastMessageAt = "İndicə",
            unreadForCustomer = 0,
            unreadForStaff = 0,
            curatorName = "Akademik Şura Kuratoru",
            curatorRole = "Elmi Məsləhətçi",
            isOnline = true
        )
        _conversations.value = listOf(newConv) + _conversations.value
        return newConv
    }

    suspend fun getOrCreateGeneralSupportConversation(): ChatConversation {
        val currentUid = authService?.currentUserId ?: _currentUser.value?.id ?: ""
        if (currentUid.isBlank()) {
            throw IllegalStateException("Müştəri xidmətləri ilə əlaqə saxlamaq üçün zəhmət olmasa sistemə daxil olun.")
        }

        val convId = FirestoreChatService.deterministicSupportConversationId(currentUid)

        if (chatService != null) {
            try {
                val conv = chatService.getOrCreateSupportConversation(
                    customerId = currentUid,
                    customerName = _currentUser.value?.fullName ?: "Müştəri"
                )
                val existing = _conversations.value.find { it.id == conv.id }
                if (existing == null) {
                    _conversations.value = listOf(conv) + _conversations.value
                } else {
                    _conversations.value = _conversations.value.map { if (it.id == conv.id) conv else it }
                }
                return conv
            } catch (_: Exception) {
                // fall through to local fallback on network issue
            }
        }

        val existing = _conversations.value.find { it.id == convId }
        if (existing != null) return existing

        val newConv = ChatConversation(
            id = convId,
            customerId = currentUid,
            orderId = null,
            title = "Müştəri xidmətləri",
            lastMessage = "",
            lastMessageAt = "İndicə",
            unreadForCustomer = 0,
            unreadForStaff = 0,
            curatorName = "Müştəri Xidmətləri Meneceri",
            curatorRole = "Akademik Dəstək",
            isOnline = true
        )
        _conversations.value = listOf(newConv) + _conversations.value
        return newConv
    }

    fun listenToConversationMessages(conversationId: String, scope: CoroutineScope) {
        activeChatJob?.cancel()
        if (conversationId.isBlank()) return

        if (chatService != null) {
            activeChatJob = scope.launch {
                chatService.getMessagesFlow(conversationId).collect { remoteMsgs ->
                    val updated = _messages.value.toMutableMap()
                    updated[conversationId] = remoteMsgs
                    _messages.value = updated
                }
            }
        }
    }

    private fun createChatConversationForOrder(order: Order) {
        val currentUid = authService?.currentUserId ?: _currentUser.value?.id ?: ""
        val convId = FirestoreChatService.deterministicOrderConversationId(order.id)
        val existing = _conversations.value.find { it.id == convId }
        if (existing != null) return

        val newConv = ChatConversation(
            id = convId,
            customerId = currentUid,
            orderId = order.id,
            title = "${order.orderNumber} Kuratorluğu",
            curatorName = "Akademik Şura Kuratoru",
            curatorRole = "Elmi Məsləhətçi",
            lastMessage = "",
            lastMessageAt = "İndicə",
            unreadForCustomer = 0,
            unreadForStaff = 0,
            isOnline = true
        )
        _conversations.value = listOf(newConv) + _conversations.value
    }

    suspend fun sendMessage(conversationId: String, text: String, attachedFileName: String? = null): ChatMessage {
        val currentUid = _currentUser.value?.id ?: authService?.currentUserId ?: ""
        if (currentUid.isBlank()) {
            throw IllegalStateException("Mesaj göndərmək üçün sistemə daxil olun.")
        }
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Boş mesaj göndərilə bilməz.")
        }
        if (trimmed.length > FirestoreChatService.MAX_MESSAGE_LENGTH) {
            throw IllegalArgumentException("Mesaj mətni 4000 simvoldan çox ola bilməz.")
        }

        val sender = _currentUser.value?.fullName ?: "İstifadəçi"
        val isStaff = _currentUser.value?.role in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.OPERATOR)
        val role = if (isStaff) "staff" else "customer"

        val sentMessage = if (chatService != null) {
            chatService.sendMessage(
                conversationId = conversationId,
                senderId = currentUid,
                senderName = sender,
                senderRole = role,
                text = trimmed,
                attachedFileName = attachedFileName
            )
        } else {
            val dateFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = dateFmt.format(Date())
            ChatMessage(
                id = "m_${UUID.randomUUID()}",
                conversationId = conversationId,
                senderId = currentUid,
                senderRole = role,
                senderName = sender,
                text = trimmed,
                timestamp = now,
                isRead = false,
                status = MessageDeliveryStatus.SENT,
                attachedFileName = attachedFileName
            )
        }

        val updatedMap = _messages.value.toMutableMap()
        val currentList = updatedMap[conversationId] ?: emptyList()
        if (currentList.none { it.id == sentMessage.id }) {
            updatedMap[conversationId] = currentList + sentMessage
            _messages.value = updatedMap
        }

        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                conv.copy(lastMessage = trimmed, lastMessageAt = sentMessage.timestamp)
            } else conv
        }

        return sentMessage
    }

    suspend fun markConversationAsRead(conversationId: String) {
        val isStaff = _currentUser.value?.role in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.OPERATOR)
        if (chatService != null) {
            try {
                chatService.markConversationAsRead(conversationId, isStaff)
            } catch (_: Exception) {}
        }
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                if (isStaff) conv.copy(unreadForStaff = 0) else conv.copy(unreadForCustomer = 0)
            } else conv
        }
    }

    fun markAllNotificationsAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun addNotification(item: NotificationItem) {
        _notifications.value = listOf(item) + _notifications.value
    }

    companion object {
        fun getDefaultCatalogServices(): List<ServiceCatalogItem> {
            return listOf(
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
                    sortOrder = 1
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
                    sortOrder = 2
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
                    sortOrder = 3
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
                    sortOrder = 4
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
                    sortOrder = 5
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
                    sortOrder = 6
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
                    sortOrder = 7
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
                    sortOrder = 8
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
                    sortOrder = 9
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
                    sortOrder = 10
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
                    sortOrder = 11
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
                    sortOrder = 12
                )
            )
        }
    }
}
