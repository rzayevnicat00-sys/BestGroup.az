package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localization.Language
import com.example.localization.LocalizationManager
import com.example.model.*
import com.example.repository.BestGroupRepository
import com.example.services.firebase.*
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppDestination {
    SPLASH,
    ONBOARDING,
    AUTH,
    MAIN,
    ORDER_WIZARD,
    ORDER_DETAIL,
    SUPPORT_CENTER,
    ADMIN_DASHBOARD
}

enum class BottomTab {
    HOME,
    ORDERS,
    CHAT,
    NOTIFICATIONS,
    PROFILE
}

enum class OrdersFilter {
    ALL,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

data class OrderWizardUiState(
    val currentStep: Int = 1,
    val selectedService: ServiceType? = null,
    val selectedCatalogService: ServiceCatalogItem? = null,
    val topic: String = "",
    val scopeDescription: String = "",
    val university: String = "Azərbaycan Dövlət Neft və Sənaye Universiteti (ADNSU)",
    val faculty: String = "İnformasiya Texnologiyaları və İdarəetmə",
    val academicLevel: String = "Magistratura",
    val language: String = "Azərbaycan dili",
    val pageCount: String = "50",
    val deadline: String = "20 Noyabr 2026",
    val priority: OrderPriority = OrderPriority.NORMAL,
    val formattingStandard: String = "APA 7th Edition",
    val specialNotes: String = "",
    val attachedFiles: List<OrderAttachedFile> = emptyList(),
    val isSubmitting: Boolean = false,
    val completedOrder: Order? = null,
    val error: String? = null,
    val uploadProgress: Float = 0f,
    val currentUploadingFileName: String? = null
)

class MainViewModel @JvmOverloads constructor(
    application: Application,
    val repository: BestGroupRepository = createDefaultRepository(application)
) : AndroidViewModel(application) {

    private val _currentDestination = MutableStateFlow(AppDestination.SPLASH)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _selectedBottomTab = MutableStateFlow(BottomTab.HOME)
    val selectedBottomTab: StateFlow<BottomTab> = _selectedBottomTab.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>("conv_1")
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _ordersFilter = MutableStateFlow(OrdersFilter.ALL)
    val ordersFilter: StateFlow<OrdersFilter> = _ordersFilter.asStateFlow()

    private val _ordersSearchQuery = MutableStateFlow("")
    val ordersSearchQuery: StateFlow<String> = _ordersSearchQuery.asStateFlow()

    private val _serviceSearchQuery = MutableStateFlow("")
    val serviceSearchQuery: StateFlow<String> = _serviceSearchQuery.asStateFlow()

    private val _wizardState = MutableStateFlow(OrderWizardUiState())
    val wizardState: StateFlow<OrderWizardUiState> = _wizardState.asStateFlow()

    // Auth screen state
    val isLoginMode = MutableStateFlow(true)
    val authEmail = MutableStateFlow("")
    val authPassword = MutableStateFlow("")
    val authFullName = MutableStateFlow("")
    val authPhone = MutableStateFlow("")
    val authUniversity = MutableStateFlow("ADNSU")
    val authFaculty = MutableStateFlow("İnformasiya Texnologiyaları")
    val authEducationLevel = MutableStateFlow("Magistratura")
    val authError = MutableStateFlow<String?>(null)
    val isAuthLoading = repository.isAuthLoading

    // Email verification state
    val showEmailVerificationBanner = MutableStateFlow(false)

    // Data streams from repository
    val currentUser = repository.currentUser
    val themeMode = repository.themeMode
    val catalogServices = repository.catalogServices
    val services = repository.services
    val orders = repository.orders
    val conversations = repository.conversations
    val messages = repository.messages
    val notifications = repository.notifications
    val faqs = repository.faqs
    val adminStats = repository.adminStats
    val currentLanguage = LocalizationManager.currentLanguage

    val isServicesLoading = MutableStateFlow(false)
    val servicesError = MutableStateFlow<String?>(null)

    fun reloadServices() {
        viewModelScope.launch {
            isServicesLoading.value = true
            servicesError.value = null
            try {
                repository.serviceCatalogService?.seedDefaultServicesIfEmpty()
            } catch (e: Exception) {
                servicesError.value = "Xidmətləri yükləmək mümkün olmadı. İnternet bağlantınızı yoxlayın və yenidən cəhd edin."
            } finally {
                isServicesLoading.value = false
            }
        }
    }

    init {
        repository.initFirebaseObservers(viewModelScope)

        viewModelScope.launch {
            delay(1200)
            if (repository.isUserLoggedIn()) {
                showEmailVerificationBanner.value = !repository.isEmailVerified()
                _currentDestination.value = AppDestination.MAIN
            } else {
                _currentDestination.value = AppDestination.AUTH
            }
        }
    }

    fun navigateTo(dest: AppDestination) {
        _currentDestination.value = dest
    }

    fun selectBottomTab(tab: BottomTab) {
        _selectedBottomTab.value = tab
        _currentDestination.value = AppDestination.MAIN
    }

    fun setLanguage(lang: Language) {
        LocalizationManager.setLanguage(lang)
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.setThemeMode(mode)
    }

    fun toggleFavoriteService(serviceId: String) {
        repository.toggleFavoriteService(serviceId)
    }

    fun setOrdersFilter(filter: OrdersFilter) {
        _ordersFilter.value = filter
    }

    fun setOrdersSearch(query: String) {
        _ordersSearchQuery.value = query
    }

    fun setServiceSearch(query: String) {
        _serviceSearchQuery.value = query
    }

    fun viewOrderDetail(order: Order) {
        _selectedOrder.value = order
        _currentDestination.value = AppDestination.ORDER_DETAIL
    }

    var isSendingChatMessage = mutableStateOf(false)
        private set
    var chatErrorMessage = mutableStateOf<String?>(null)
        private set

    fun setActiveConversation(convId: String) {
        _activeConversationId.value = convId
        _selectedBottomTab.value = BottomTab.CHAT
        _currentDestination.value = AppDestination.MAIN
        chatErrorMessage.value = null
        repository.listenToConversationMessages(convId, viewModelScope)
        viewModelScope.launch {
            repository.markConversationAsRead(convId)
        }
    }

    fun openOrderChat(order: Order) {
        viewModelScope.launch {
            try {
                val conv = repository.getOrCreateConversationForOrder(order)
                setActiveConversation(conv.id)
            } catch (e: Exception) {
                chatErrorMessage.value = e.localizedMessage ?: "Söhbətə qoşulmaq mümkün olmadı."
                _selectedBottomTab.value = BottomTab.CHAT
                _currentDestination.value = AppDestination.MAIN
            }
        }
    }

    fun sendChatMessage(text: String, file: String? = null, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val convId = _activeConversationId.value ?: return
        if (isSendingChatMessage.value) return // Duplicate submission protection

        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            chatErrorMessage.value = "Boş mesaj göndərilə bilməz."
            onComplete?.invoke(false, "Boş mesaj göndərilə bilməz.")
            return
        }
        if (trimmed.length > 4000) {
            chatErrorMessage.value = "Mesaj mətni 4000 simvoldan çox ola bilməz."
            onComplete?.invoke(false, "Mesaj mətni 4000 simvoldan çox ola bilməz.")
            return
        }

        isSendingChatMessage.value = true
        chatErrorMessage.value = null

        viewModelScope.launch {
            try {
                repository.sendMessage(convId, trimmed, file)
                onComplete?.invoke(true, null)
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Mesaj göndərilmədi. İnternet bağlantısını yoxlayın."
                chatErrorMessage.value = err
                onComplete?.invoke(false, err)
            } finally {
                isSendingChatMessage.value = false
            }
        }
    }

    fun clearChatError() {
        chatErrorMessage.value = null
    }

    fun submitRating(orderId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.submitOrderReview(orderId, rating, comment)
            _selectedOrder.value = repository.orders.value.find { it.id == orderId }
        }
    }

    fun markNotificationsAsRead() {
        repository.markAllNotificationsAsRead()
    }

    // --- Authentication Actions ---

    fun login(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.signIn(email, pass)
            when (result) {
                is AuthResult.Success -> {
                    authError.value = null
                    showEmailVerificationBanner.value = !repository.isEmailVerified()
                    _currentDestination.value = AppDestination.MAIN
                    onSuccess()
                }
                is AuthResult.Error -> {
                    authError.value = result.message
                    onError(result.message)
                }
            }
        }
    }

    fun register(
        email: String,
        pass: String,
        fullName: String,
        phone: String,
        university: String,
        faculty: String,
        educationLevel: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.signUp(email, pass, fullName, phone, university, faculty, educationLevel)
            when (result) {
                is AuthResult.Success -> {
                    authError.value = null
                    showEmailVerificationBanner.value = true
                    _currentDestination.value = AppDestination.MAIN
                    onSuccess()
                }
                is AuthResult.Error -> {
                    authError.value = result.message
                    onError(result.message)
                }
            }
        }
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.sendPasswordReset(email)
            when (result) {
                is AuthResult.Success -> onSuccess()
                is AuthResult.Error -> onError(result.message)
            }
        }
    }

    fun resendVerificationEmail(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.resendVerificationEmail()
            when (result) {
                is AuthResult.Success -> onSuccess()
                is AuthResult.Error -> onError(result.message)
            }
        }
    }

    fun checkEmailVerificationStatus() {
        viewModelScope.launch {
            val verified = repository.checkEmailVerified()
            showEmailVerificationBanner.value = !verified
        }
    }

    fun logout() {
        repository.signOut()
        _currentDestination.value = AppDestination.AUTH
    }

    // --- Wizard Management ---

    fun startNewOrder(preselectedService: ServiceType? = null, preselectedCatalogService: ServiceCatalogItem? = null) {
        val resolvedCatalog = preselectedCatalogService
            ?: preselectedService?.let { st -> repository.catalogServices.value.find { it.serviceId.equals(st.name, ignoreCase = true) || it.serviceId.equals(st.name.replace("_", ""), ignoreCase = true) } }
        val resolvedService = preselectedService
            ?: preselectedCatalogService?.let { cat -> ServiceType.values().find { it.name.equals(cat.serviceId, ignoreCase = true) || it.name.replace("_", "").equals(cat.serviceId.replace("_", ""), ignoreCase = true) } }
            ?: ServiceType.DIPLOMA

        _wizardState.value = OrderWizardUiState(
            currentStep = if (preselectedService != null || preselectedCatalogService != null) 2 else 1,
            selectedService = resolvedService,
            selectedCatalogService = resolvedCatalog
        )
        _currentDestination.value = AppDestination.ORDER_WIZARD
    }

    fun updateWizardState(transform: (OrderWizardUiState) -> OrderWizardUiState) {
        _wizardState.value = transform(_wizardState.value)
    }

    fun wizardNextStep() {
        val current = _wizardState.value
        if (current.currentStep < 8) {
            _wizardState.value = current.copy(currentStep = current.currentStep + 1, error = null)
        } else {
            submitWizardOrder()
        }
    }

    fun wizardPreviousStep() {
        val current = _wizardState.value
        if (current.currentStep > 1) {
            _wizardState.value = current.copy(currentStep = current.currentStep - 1, error = null)
        }
    }

    fun addWizardSampleFile() {
        val current = _wizardState.value
        if (current.attachedFiles.size >= FirebaseStorageService.MAX_FILES_PER_ORDER) {
            _wizardState.value = current.copy(error = "Maksimum 10 fayl əlavə edilə bilər.")
            return
        }
        val sampleNames = listOf("Metodika_Tələblər.pdf", "Ədəbiyyat_Siyahısı.docx", "Giriş_Strukturu.docx", "Kafedra_Təlimatı.pdf")
        val sampleExts = listOf("pdf", "docx", "docx", "pdf")
        val sampleMimes = listOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/pdf")
        val randomIdx = (sampleNames.indices).random()
        val newFile = OrderAttachedFile(
            id = "f_${UUID.randomUUID()}",
            name = sampleNames[randomIdx],
            originalFileName = sampleNames[randomIdx],
            sizeBytes = (500000L..4500000L).random(),
            extension = sampleExts[randomIdx],
            progress = 1.0f,
            state = FileUploadState.UPLOADED,
            mimeType = sampleMimes[randomIdx]
        )
        _wizardState.value = current.copy(attachedFiles = current.attachedFiles + newFile, error = null)
    }

    fun addWizardPickedFile(name: String, sizeBytes: Long, mimeType: String, uriString: String): String? {
        val current = _wizardState.value
        if (current.attachedFiles.size >= FirebaseStorageService.MAX_FILES_PER_ORDER) {
            val err = "Maksimum 10 fayl əlavə edilə bilər."
            _wizardState.value = current.copy(error = err)
            return err
        }
        try {
            FirebaseStorageService.validateFile(name, sizeBytes, mimeType)
        } catch (e: IllegalArgumentException) {
            val err = e.message ?: "Fayl yoxlanışından keçmədi."
            _wizardState.value = current.copy(error = err)
            return err
        }
        // Duplicate detection
        if (current.attachedFiles.any { it.name.equals(name, ignoreCase = true) && it.sizeBytes == sizeBytes }) {
            val err = "Bu fayl artıq əlavə edilib."
            _wizardState.value = current.copy(error = err)
            return err
        }

        val ext = FirebaseStorageService.extractExtension(name).ifBlank { "pdf" }
        val newFile = OrderAttachedFile(
            id = "f_${UUID.randomUUID()}",
            name = name,
            originalFileName = name,
            sizeBytes = sizeBytes,
            extension = ext,
            progress = 0f,
            state = FileUploadState.PENDING,
            mimeType = mimeType,
            localUri = uriString
        )
        _wizardState.value = current.copy(attachedFiles = current.attachedFiles + newFile, error = null)
        return null
    }

    fun removeWizardFile(fileId: String) {
        val current = _wizardState.value
        _wizardState.value = current.copy(attachedFiles = current.attachedFiles.filter { it.id != fileId })
    }

    fun retryOrderFileUpload(orderId: String, fileId: String) {
        viewModelScope.launch {
            repository.retryFileUpload(orderId, fileId)
            val updated = repository.orders.value.find { it.id == orderId }
            if (updated != null) {
                _selectedOrder.value = updated
                _wizardState.value = _wizardState.value.copy(
                    completedOrder = updated,
                    attachedFiles = updated.files
                )
            }
        }
    }

    fun cancelFileUpload(fileId: String) {
        repository.cancelFileUpload(fileId)
    }

    fun openOrDownloadOrderFile(context: android.content.Context, file: OrderAttachedFile, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (file.storagePath.isNotBlank()) {
                val downloadUrl = repository.getFileDownloadUrl(file.storagePath)
                if (downloadUrl != null) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse(downloadUrl)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        onResult(true, "Fayl açılır...")
                    } catch (_: Exception) {
                        onResult(false, "Faylı açmaq üçün uyğun tətbiq tapılmadı.")
                    }
                } else {
                    onResult(false, "Fayl artıq mövcud deyil və ya ona giriş mümkün deyil.")
                }
            } else if (file.localUri != null) {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(file.localUri)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(intent)
                    onResult(true, "Fayl açılır...")
                } catch (_: Exception) {
                    onResult(false, "Faylı açmaq üçün uyğun tətbiq tapılmadı.")
                }
            } else {
                onResult(false, "Fayl artıq mövcud deyil və ya ona giriş mümkün deyil.")
            }
        }
    }

    private fun submitWizardOrder() {
        val state = _wizardState.value
        // Duplicate submit protection
        if (state.isSubmitting) return

        if (state.selectedCatalogService == null && state.selectedService == null) {
            _wizardState.value = state.copy(error = "Zəhmət olmasa xidmət növünü seçin.")
            return
        }
        if (state.topic.isBlank()) {
            _wizardState.value = state.copy(error = "Zəhmət olmasa mövzu sahəsini doldurun.")
            return
        }
        if (state.university.isBlank()) {
            _wizardState.value = state.copy(error = "Zəhmət olmasa ali təhsil müəssisəsini qeyd edin.")
            return
        }
        if (state.language.isBlank()) {
            _wizardState.value = state.copy(error = "Zəhmət olmasa tədris dilini seçin.")
            return
        }

        // Validate deadline not in past
        val deadlineError = checkDeadlinePast(state.deadline)
        if (deadlineError != null) {
            _wizardState.value = state.copy(error = deadlineError)
            return
        }

        val service = state.selectedService ?: ServiceType.DIPLOMA
        val catService = state.selectedCatalogService
            ?: repository.catalogServices.value.find { it.serviceId.equals(service.name, ignoreCase = true) || it.serviceId.equals(service.name.replace("_", ""), ignoreCase = true) }

        if (catService != null && !catService.active) {
            _wizardState.value = state.copy(error = "Seçilmiş xidmət hazırda aktiv deyil.")
            return
        }

        _wizardState.value = state.copy(isSubmitting = true, uploadProgress = 0f, error = null)

        viewModelScope.launch {
            try {
                // 1. Create order in Firestore
                val created = repository.createOrder(
                    serviceType = service,
                    topic = state.topic,
                    scopeDescription = state.scopeDescription.ifBlank { "Tələblər kuratorla fərdi müzakirə olunacaq." },
                    university = state.university,
                    faculty = state.faculty,
                    academicLevel = state.academicLevel,
                    language = state.language,
                    pageCount = state.pageCount.toIntOrNull() ?: 50,
                    deadline = state.deadline,
                    priority = state.priority,
                    formattingStandard = state.formattingStandard,
                    specialNotes = state.specialNotes,
                    files = state.attachedFiles,
                    serviceId = catService?.serviceId ?: service.name.lowercase(),
                    serviceName = catService?.nameAz ?: service.name,
                    serviceCatalogItem = catService
                )

                // 2. Upload files to Firebase Storage if any pending files exist
                var finalFiles = state.attachedFiles
                val pendingFiles = state.attachedFiles.filter { it.localUri != null }
                if (pendingFiles.isNotEmpty()) {
                    finalFiles = repository.uploadOrderFiles(
                        orderId = created.id,
                        files = state.attachedFiles,
                        onProgress = { fileId, progress ->
                            _wizardState.value = _wizardState.value.copy(
                                uploadProgress = progress,
                                attachedFiles = _wizardState.value.attachedFiles.map { f ->
                                    if (f.id == fileId) f.copy(progress = progress, state = FileUploadState.UPLOADING) else f
                                }
                            )
                        }
                    )
                }

                val hasFailures = finalFiles.any { it.isFailed }
                _wizardState.value = state.copy(
                    isSubmitting = false,
                    completedOrder = created.copy(files = finalFiles),
                    attachedFiles = finalFiles,
                    error = if (hasFailures) "Faylların bir hissəsini yükləmək mümkün olmadı. Yenidən cəhd edin." else null
                )
            } catch (e: Exception) {
                _wizardState.value = state.copy(
                    isSubmitting = false,
                    error = e.localizedMessage ?: "Sifariş göndərilərkən xəta baş verdi. Zəhmət olmasa yenidən cəhd edin."
                )
            }
        }
    }

    private fun checkDeadlinePast(deadlineStr: String): String? {
        if (deadlineStr.isBlank()) return "Son tarixi qeyd edin."
        val formats = listOf(
            java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("az")),
            java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.US),
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US),
            java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.US),
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
        )
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val today = cal.time

        for (fmt in formats) {
            try {
                val d = fmt.parse(deadlineStr.trim())
                if (d != null) {
                    if (d.before(today)) {
                        return "Son tarix keçmiş tarix ola bilməz."
                    }
                    return null
                }
            } catch (_: Exception) {}
        }
        return null
    }

    // --- Admin Management ---

    fun adminUpdateOrderStatus(orderId: String, status: OrderStatus, note: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status, note, "Admin (Sistem Meneceri)")
            _selectedOrder.value = repository.orders.value.find { it.id == orderId }
        }
    }

    fun adminUpdatePriority(orderId: String, priority: OrderPriority) {
        viewModelScope.launch {
            repository.updateOrderPriority(orderId, priority)
            _selectedOrder.value = repository.orders.value.find { it.id == orderId }
        }
    }

    fun adminSendPushBroadcast(title: String, msg: String) {
        repository.addNotification(
            NotificationItem(
                id = "broadcast_${UUID.randomUUID()}",
                title = title,
                body = msg,
                category = NotificationCategory.CAMPAIGNS,
                timestamp = "İndicə"
            )
        )
    }

    companion object {
        fun createDefaultRepository(application: Application): BestGroupRepository {
            return try {
                val fbService = FirebaseService(application.applicationContext)
                val authService = FirebaseAuthService(fbService.auth, fbService.firestore)
                val orderService = FirestoreOrderService(fbService.firestore)
                val chatService = FirestoreChatService(fbService.firestore)
                val storageService = FirebaseStorageService(fbService.storage)
                val catalogService = FirestoreServiceCatalogService(fbService.firestore)
                BestGroupRepository(authService, orderService, chatService, storageService, catalogService)
            } catch (e: Throwable) {
                BestGroupRepository()
            }
        }
    }
}
