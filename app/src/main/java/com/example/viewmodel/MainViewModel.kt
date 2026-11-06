package com.example.viewmodel

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.pnv.FirebasePhoneNumberVerification
import android.content.Context
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.fragment.app.FragmentActivity
import com.example.services.security.*
import com.example.model.AutoLockTimeout
import com.example.model.DeviceSession
import com.example.model.LoginHistoryItem
import com.example.model.TwoFactorMethod
import com.example.model.AdminAuditLog

enum class AppDestination {
    SPLASH,
    ONBOARDING,
    AUTH,
    MAIN,
    ORDER_WIZARD,
    ORDER_DETAIL,
    SUPPORT_CENTER,
    ADMIN_DASHBOARD,
    SECURITY_SETTINGS,
    ACTIVE_DEVICES,
    LOGIN_HISTORY,
    BESTGROUP_AI,
    ACADEMIC_TOOLS,
    PRICE_CALCULATOR,
    ACADEMY,
    ACADEMIC_CABINET,
    BESTGROUP_CLUB,
    AI_DOC_ASSISTANT
}

enum class BottomTab {
    HOME,
    SERVICES,
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
    val currentUploadingFileName: String? = null,
    val isAakVerification: Boolean = false
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

    private val _activeConversationId = MutableStateFlow<String?>(null)
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
    val authUniversity = MutableStateFlow("")
    val authFaculty = MutableStateFlow("")
    val authEducationLevel = MutableStateFlow("")
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

    // Notifications strictly segregated for Customer vs Admin
    val customerNotifications: StateFlow<List<NotificationItem>> = combine(repository.notifications, currentUser) { list, user ->
        val uid = user?.id ?: ""
        list.filter { !it.forAdmin && (it.targetUserId == null || it.targetUserId == uid) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminNotifications: StateFlow<List<NotificationItem>> = repository.notifications
        .map { list -> list.filter { it.forAdmin || it.category == NotificationCategory.CAMPAIGNS } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val faqs = repository.faqs
    val adminStats = repository.adminStats
    val allRegisteredUsers = repository.allRegisteredUsers
    val adminCustomers = repository.adminCustomers
    val currentLanguage = LocalizationManager.currentLanguage

    // Security & Login System State
    val isAppLocked: StateFlow<Boolean> = repository.securityManager?.isAppLocked ?: MutableStateFlow(false).asStateFlow()
    val isBiometricEnabled: StateFlow<Boolean> = repository.securityManager?.isBiometricEnabled ?: MutableStateFlow(false).asStateFlow()
    val isPinConfigured: StateFlow<Boolean> = repository.securityManager?.isPinEnabled ?: MutableStateFlow(false).asStateFlow()
    val autoLockTimeout: StateFlow<AutoLockTimeout> = repository.securityManager?.autoLockTimeout ?: MutableStateFlow(AutoLockTimeout.ONE_MINUTE).asStateFlow()
    val isScreenSecurityEnabled: StateFlow<Boolean> = repository.securityManager?.isScreenSecurityEnabled ?: MutableStateFlow(false).asStateFlow()

    val isTwoFactorEnabled = repository.isTwoFactorEnabled
    val twoFactorMethod = repository.twoFactorMethod
    val activeDeviceSessions = repository.activeDeviceSessions
    val loginHistory = repository.loginHistory

    // Ecosystem 2.0 State
    val clubMembership = repository.clubService.membership
    val bonusTransactions = repository.clubService.bonusTransactions
    val academyMaterials = repository.academyService.materials
    val bookmarkedAcademyIds = repository.academyService.bookmarkedIds
    val readAcademyIds = repository.academyService.readMaterialIds
    val aiChatMessages = repository.aiService.chatMessages
    val staffMembers = repository.crmService.staffMembers
    val cannedResponses = repository.crmService.cannedResponses

    // 2FA Verification Flow State
    val is2FaVerificationActive = mutableStateOf(false)
    val pending2FaUser = mutableStateOf<User?>(null)
    val pending2FaMethod = mutableStateOf(TwoFactorMethod.SMS)
    val pending2FaDestination = mutableStateOf("")
    val pending2FaVerificationId = mutableStateOf("")

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
            val rememberMe = repository.isRememberMeEnabled()
            val isLoggedIn = repository.isUserLoggedIn()
            if (!repository.isOnboardingCompleted.value) {
                _currentDestination.value = AppDestination.ONBOARDING
            } else if (rememberMe && isLoggedIn) {
                showEmailVerificationBanner.value = !repository.isEmailVerified()
                _currentDestination.value = AppDestination.MAIN
            } else {
                if (!rememberMe && isLoggedIn) {
                    repository.signOut()
                }
                _currentDestination.value = AppDestination.AUTH
            }
        }
    }

    fun completeOnboarding() {
        repository.completeOnboarding()
        val rememberMe = repository.isRememberMeEnabled()
        if (rememberMe && repository.isUserLoggedIn()) {
            _currentDestination.value = AppDestination.MAIN
        } else {
            _currentDestination.value = AppDestination.AUTH
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

    // --- BestGroup AI ---
    fun sendAiMessage(prompt: String) {
        val userOrders = orders.value.filter { it.userId == currentUser.value?.id }
        repository.aiService.sendMessage(prompt, userOrders) {}
    }

    fun analyzeDocument(title: String, content: String): com.example.model.AiDocumentReport {
        return repository.aiService.analyzeDocument(title, content)
    }

    // --- Price Calculator ---
    fun calculateEstimate(
        serviceType: com.example.model.ServiceType,
        pageCount: Int,
        language: String,
        deadlineDays: Int,
        isUrgent: Boolean,
        isAakCheck: Boolean
    ): com.example.model.PriceCalculationEstimate {
        val userTier = clubMembership.value.tier
        return repository.priceCalculatorService.calculateEstimate(
            serviceType, pageCount, language, deadlineDays, isUrgent, isAakCheck, userTier
        )
    }

    // --- Academy ---
    fun toggleAcademyBookmark(materialId: String) {
        viewModelScope.launch {
            val uid = currentUser.value?.id ?: ""
            repository.academyService.toggleBookmark(uid, materialId)
        }
    }

    fun markAcademyAsRead(materialId: String) {
        viewModelScope.launch {
            val uid = currentUser.value?.id ?: ""
            repository.academyService.markAsRead(uid, materialId)
        }
    }

    // --- Club / Loyalty ---
    fun applyClubBonus(amount: Double, orderId: String? = null) {
        viewModelScope.launch {
            val uid = currentUser.value?.id ?: ""
            repository.clubService.addTransaction(
                userId = uid,
                amount = amount,
                isCredit = false,
                description = "Sifariş üçün bonus endirimi istifadə edildi",
                orderId = orderId
            )
        }
    }

    // --- Document Versioning & Live Stages ---
    fun addOrderDocumentVersion(
        orderId: String,
        fileName: String,
        fileUrl: String,
        sizeBytes: Long,
        notes: String
    ) {
        viewModelScope.launch {
            val isStaff = currentUser.value?.role in listOf(
                com.example.model.UserRole.ADMIN,
                com.example.model.UserRole.MANAGER,
                com.example.model.UserRole.OPERATOR
            )
            val uploaderName = currentUser.value?.fullName ?: "İstifadəçi"
            repository.addOrderDocumentVersion(orderId, fileName, fileUrl, sizeBytes, notes, uploaderName, isStaff)
        }
    }

    fun updateOrderLiveStage(
        orderId: String,
        stage: com.example.model.OrderStage,
        note: String
    ) {
        viewModelScope.launch {
            val actorName = currentUser.value?.fullName ?: "Admin"
            repository.updateOrderLiveStage(orderId, stage, note, actorName)
        }
    }

    fun assignOrderStaff(orderId: String, staffId: String, staffName: String) {
        viewModelScope.launch {
            repository.assignOrderStaff(orderId, staffId, staffName)
        }
    }

    fun addOrderTask(orderId: String, title: String, staffName: String, deadline: String) {
        viewModelScope.launch {
            repository.addOrderTask(orderId, title, staffName, deadline)
        }
    }

    fun toggleOrderTask(orderId: String, taskId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.toggleOrderTask(orderId, taskId, currentStatus)
        }
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
    var isChatLoading = mutableStateOf(false)
        private set
    var chatErrorMessage = mutableStateOf<String?>(null)
        private set

    fun setActiveConversation(convId: String, navigateToCustomerChat: Boolean = true) {
        _activeConversationId.value = convId
        if (navigateToCustomerChat) {
            _selectedBottomTab.value = BottomTab.CHAT
            _currentDestination.value = AppDestination.MAIN
        }
        chatErrorMessage.value = null
        repository.listenToConversationMessages(convId, viewModelScope)
        viewModelScope.launch {
            repository.markConversationAsRead(convId)
        }
    }

    fun ensureAdminDataObserved() {
        repository.reobserveData(viewModelScope)
        if (repository.orderService != null) {
            viewModelScope.launch {
                val currentUid = repository.currentUser.value?.id ?: ""
                repository.orderService.getOrdersFlow(currentUid, isStaff = true).collect { remoteOrders ->
                    repository.setOrdersDirectly(remoteOrders)
                }
            }
        }
        if (repository.chatService != null) {
            viewModelScope.launch {
                val currentUid = repository.currentUser.value?.id ?: ""
                repository.chatService.getConversationsFlow(currentUid, isStaff = true).collect { remoteConvs ->
                    repository.setConversationsDirectly(remoteConvs)
                }
            }
        }
    }

    fun openOrderChat(order: Order) {
        val deterministicConvId = com.example.services.firebase.FirestoreChatService.deterministicOrderConversationId(order.id)
        _activeConversationId.value = deterministicConvId
        _selectedBottomTab.value = BottomTab.CHAT
        _currentDestination.value = AppDestination.MAIN
        isChatLoading.value = true
        chatErrorMessage.value = null

        viewModelScope.launch {
            try {
                val conv = repository.getOrCreateConversationForOrder(order)
                setActiveConversation(conv.id)
            } catch (e: Exception) {
                chatErrorMessage.value = e.localizedMessage ?: "Söhbətə qoşulmaq mümkün olmadı."
            } finally {
                isChatLoading.value = false
            }
        }
    }

    fun openGeneralSupportChat() {
        val currentUid = repository.currentUserId
        if (currentUid.isBlank()) {
            _currentDestination.value = AppDestination.AUTH
            return
        }
        val deterministicSupportId = com.example.services.firebase.FirestoreChatService.deterministicSupportConversationId(currentUid)
        _activeConversationId.value = deterministicSupportId
        _selectedBottomTab.value = BottomTab.CHAT
        _currentDestination.value = AppDestination.MAIN
        isChatLoading.value = true
        chatErrorMessage.value = null

        viewModelScope.launch {
            try {
                val conv = repository.getOrCreateGeneralSupportConversation()
                setActiveConversation(conv.id)
            } catch (e: Exception) {
                chatErrorMessage.value = e.localizedMessage ?: "Müştəri xidmətləri ilə əlaqə qurmaq mümkün olmadı."
            } finally {
                isChatLoading.value = false
            }
        }
    }

    fun sendChatMessage(
        text: String,
        file: String? = null,
        targetConvId: String? = null,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        if (isSendingChatMessage.value) return // Duplicate submission protection

        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            chatErrorMessage.value = "Boş mesaj göndərilə bilməz."
            onComplete?.invoke(false, "Boş mesaj göndərilə bilməz.")
            return
        }

        isSendingChatMessage.value = true
        chatErrorMessage.value = null

        viewModelScope.launch {
            try {
                val convId = targetConvId ?: _activeConversationId.value ?: run {
                    val supportConv = repository.getOrCreateGeneralSupportConversation()
                    _activeConversationId.value = supportConv.id
                    repository.listenToConversationMessages(supportConv.id, viewModelScope)
                    supportConv.id
                }
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

    fun openSupportChatForCustomer(customer: User) {
        viewModelScope.launch {
            isChatLoading.value = true
            try {
                val conv = repository.getOrCreateGeneralSupportConversation(
                    targetCustomerId = customer.id,
                    targetCustomerName = customer.fullName
                )
                _activeConversationId.value = conv.id
                repository.listenToConversationMessages(conv.id, viewModelScope)
                selectBottomTab(BottomTab.CHAT)
            } catch (e: Exception) {
                chatErrorMessage.value = e.localizedMessage ?: "Söhbət açılarkən xəta baş verdi."
            } finally {
                isChatLoading.value = false
            }
        }
    }

    fun openSupportInquiryChat() {
        viewModelScope.launch {
            isChatLoading.value = true
            try {
                val supportConv = repository.getOrCreateGeneralSupportConversation()
                _activeConversationId.value = supportConv.id
                repository.listenToConversationMessages(supportConv.id, viewModelScope)
                selectBottomTab(BottomTab.CHAT)
            } catch (e: Exception) {
                selectBottomTab(BottomTab.CHAT)
            } finally {
                isChatLoading.value = false
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

    fun markNotificationAsRead(id: String) {
        repository.markNotificationAsRead(id)
    }

    // --- Authentication Actions ---

    fun login(email: String, pass: String, rememberMe: Boolean = false, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.signIn(email, pass, rememberMe)
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
        phoneVerified: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.signUp(email, pass, fullName, phone, university, faculty, educationLevel, phoneVerified)
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

    fun loginWithPhone(phone: String, pass: String, rememberMe: Boolean = false, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.signInWithPhone(phone, pass, rememberMe)
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

    fun logout() {
        _currentDestination.value = AppDestination.AUTH
        _selectedBottomTab.value = BottomTab.HOME
        _activeConversationId.value = null
        _selectedOrder.value = null
        viewModelScope.launch {
            try {
                repository.updateUserPresence(false)
            } catch (_: Exception) {}
            repository.signOut()
        }
    }

    // --- Security & Login Delegate Actions ---

    fun isBiometricSupported(): Boolean = repository.securityManager?.isBiometricSupported() ?: false

    fun setBiometricEnabled(enabled: Boolean) {
        repository.securityManager?.setBiometricEnabled(enabled)
    }

    fun setPin(pin: String): Boolean {
        return repository.securityManager?.setPin(pin) ?: false
    }

    fun removePin(): Boolean {
        return repository.securityManager?.removePin() ?: false
    }

    fun validatePin(pin: String): PinValidationResult {
        return repository.securityManager?.validatePin(pin) ?: PinValidationResult.Success
    }

    fun authenticateBiometric(
        activity: FragmentActivity,
        title: String = "BestGroup.az Təhlükəsiz Giriş",
        subtitle: String = "Daxil olmaq üçün barmaq izinizi təsdiqləyin",
        negativeButtonText: String = "PIN kod ilə daxil ol",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.securityManager?.showBiometricPrompt(activity, title, subtitle, negativeButtonText, onSuccess, onError)
            ?: onError("Biometrik menecer aktiv deyil.")
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        repository.securityManager?.setAutoLockTimeout(timeout)
    }

    fun setScreenSecurityEnabled(enabled: Boolean, activity: FragmentActivity?) {
        repository.securityManager?.setScreenSecurityEnabled(enabled, activity)
    }

    fun cancel2FaVerification() {
        is2FaVerificationActive.value = false
        pending2FaUser.value = null
        pending2FaVerificationId.value = ""
    }

    fun setTwoFactorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTwoFactorEnabled(enabled)
        }
    }

    fun setTwoFactorMethod(method: TwoFactorMethod) {
        viewModelScope.launch {
            repository.setTwoFactorMethod(method)
        }
    }

    fun verify2FaCode(
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = pending2FaUser.value ?: currentUser.value
        if (user == null) {
            onError("İstifadəçi sessiyası tapılmadı.")
            return
        }

        val method = pending2FaMethod.value
        viewModelScope.launch {
            val service = repository.twoFactorService
            if (service == null) {
                is2FaVerificationActive.value = false
                _currentDestination.value = AppDestination.MAIN
                onSuccess()
                return@launch
            }

            if (method == TwoFactorMethod.SMS) {
                val vId = pending2FaVerificationId.value
                val res = service.verifySmsTwoFactor(vId, code)
                res.fold(
                    onSuccess = {
                        is2FaVerificationActive.value = false
                        pending2FaUser.value = null
                        _currentDestination.value = AppDestination.MAIN
                        onSuccess()
                    },
                    onFailure = { err ->
                        onError(err.localizedMessage ?: "SMS kodu yanlışdır.")
                    }
                )
            } else {
                val res = service.verifyEmailTwoFactor(user.id, code)
                res.fold(
                    onSuccess = {
                        is2FaVerificationActive.value = false
                        pending2FaUser.value = null
                        _currentDestination.value = AppDestination.MAIN
                        onSuccess()
                    },
                    onFailure = { err ->
                        onError(err.localizedMessage ?: "Təsdiq kodu yanlışdır.")
                    }
                )
            }
        }
    }

    fun resend2FaCode(activity: Activity?, onError: (String) -> Unit) {
        val user = pending2FaUser.value ?: currentUser.value ?: return
        val method = pending2FaMethod.value
        val service = repository.twoFactorService ?: return

        if (!service.canResendCode()) {
            onError("Yenidən kod göndərmək üçün ${service.getResendCooldownSeconds()} saniyə gözləyin.")
            return
        }

        if (method == TwoFactorMethod.SMS) {
            if (activity != null) {
                val phone = user.phone.ifBlank { "+994500000000" }
                service.sendSmsTwoFactor(
                    activity = activity,
                    phoneNumber = phone,
                    onCodeSent = { vId ->
                        pending2FaVerificationId.value = vId
                    },
                    onError = { err -> onError(err) }
                )
            } else {
                onError("SMS göndərmək üçün aktiv fəaliyyət tapılmadı.")
            }
        } else {
            viewModelScope.launch {
                val res = service.sendEmailTwoFactor(user.id, user.email)
                res.onFailure { err -> onError(err.localizedMessage ?: "Kod göndərilmədi.") }
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.changePassword(oldPass, newPass)
            res.fold(
                onSuccess = { onSuccess() },
                onFailure = { err -> onError(err.localizedMessage ?: "Şifrə yenilənmədi.") }
            )
        }
    }

    fun revokeDeviceSession(sessionId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.revokeDeviceSession(sessionId)
            res.fold(
                onSuccess = { onSuccess() },
                onFailure = { err -> onError(err.localizedMessage ?: "Sessiyanı ləğv etmək mümkün olmadı.") }
            )
        }
    }

    fun revokeAllOtherSessions(onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.revokeAllOtherSessions()
            res.fold(
                onSuccess = { count -> onSuccess(count) },
                onFailure = { err -> onError(err.localizedMessage ?: "Cihazları ləğv etmək mümkün olmadı.") }
            )
        }
    }

    fun logoutAllDevices() {
        _currentDestination.value = AppDestination.AUTH
        _selectedBottomTab.value = BottomTab.HOME
        _activeConversationId.value = null
        _selectedOrder.value = null
        viewModelScope.launch {
            repository.logoutAllDevices()
        }
    }

    fun verifyPasswordAndResetPin(password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.verifyPasswordAndResetPin(password)
            res.fold(
                onSuccess = { onSuccess() },
                onFailure = { err -> onError(err.localizedMessage ?: "Şifrə yanlışdır.") }
            )
        }
    }

    fun onAppBackgrounded() {
        repository.securityManager?.onAppBackgrounded()
    }

    fun onAppForegrounded() {
        repository.securityManager?.onAppForegrounded()
    }

    fun lockApp() {
        repository.securityManager?.lockApp()
    }

    fun unlockApp() {
        repository.securityManager?.unlockApp()
    }

    fun logAdminAudit(action: String, targetUserId: String? = null, details: String = "") {
        viewModelScope.launch {
            repository.logAdminAction(action, targetUserId, details)
        }
    }

    // --- Profile Editing & Real Persistence ---
    val isProfileUpdating = mutableStateOf(false)
    val profileUpdateError = mutableStateOf<String?>(null)
    val profileUpdateSuccess = mutableStateOf(false)

    fun saveUserProfile(
        fullName: String,
        phone: String,
        university: String,
        faculty: String,
        degreeLevel: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            isProfileUpdating.value = true
            profileUpdateError.value = null
            profileUpdateSuccess.value = false
            val result = repository.saveUserProfile(
                fullName = fullName,
                phone = phone,
                university = university,
                faculty = faculty,
                degreeLevel = degreeLevel
            )
            isProfileUpdating.value = false
            result.fold(
                onSuccess = {
                    profileUpdateSuccess.value = true
                    onSuccess()
                },
                onFailure = { err ->
                    val msg = err.localizedMessage ?: "Profil yadda saxlanılarkən xəta baş verdi."
                    profileUpdateError.value = msg
                    onError(msg)
                }
            )
        }
    }

    fun clearProfileUpdateStatus() {
        profileUpdateError.value = null
        profileUpdateSuccess.value = false
    }

    fun switchUserRole(newRole: UserRole) {
        repository.switchUserRole(newRole)
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
        if (current.currentStep >= 7) {
            submitWizardOrder()
        } else {
            _wizardState.value = current.copy(currentStep = current.currentStep + 1, error = null)
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
                    serviceCatalogItem = catService,
                    isAakVerification = state.isAakVerification
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

    val isAttachingFile = mutableStateOf(false)
    val attachFileProgress = mutableStateOf(0f)

    fun attachFileToOrder(
        orderId: String,
        uriString: String,
        fileName: String,
        sizeBytes: Long,
        mimeType: String,
        uploaderName: String = "Admin",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            isAttachingFile.value = true
            attachFileProgress.value = 0f
            try {
                val effectiveSize = if (sizeBytes > 0) sizeBytes else 15000L
                val ext = FirebaseStorageService.extractExtension(fileName).ifBlank {
                    when {
                        mimeType.contains("pdf", ignoreCase = true) -> "pdf"
                        mimeType.contains("word", ignoreCase = true) || mimeType.contains("doc", ignoreCase = true) -> "docx"
                        mimeType.contains("image", ignoreCase = true) -> "jpg"
                        mimeType.contains("presentation", ignoreCase = true) || mimeType.contains("powerpoint", ignoreCase = true) -> "pptx"
                        mimeType.contains("sheet", ignoreCase = true) || mimeType.contains("excel", ignoreCase = true) -> "xlsx"
                        mimeType.contains("video", ignoreCase = true) -> "mp4"
                        else -> "pdf"
                    }
                }
                val fileId = "f_${UUID.randomUUID()}"
                val targetOrder = repository.orders.value.find { it.id == orderId }
                    ?: repository.orderService?.getOrderById(orderId)
                    ?: throw IllegalStateException("Sifariş tapılmadı.")

                val isStaffUploader = uploaderName.contains("admin", true) || uploaderName.contains("kurator", true) || uploaderName.contains("staff", true)

                var uploadedFile: OrderAttachedFile? = null
                if (repository.storageService != null) {
                    try {
                        uploadedFile = repository.storageService.uploadOrderFileWithUri(
                            userId = targetOrder.userId,
                            orderId = orderId,
                            uri = android.net.Uri.parse(uriString),
                            originalFileName = fileName,
                            sizeBytes = effectiveSize,
                            mimeType = mimeType,
                            fileId = fileId,
                            onProgress = { p -> attachFileProgress.value = p }
                        ).copy(uploadedBy = uploaderName)
                    } catch (storageEx: Exception) {
                        android.util.Log.w("FileAttach", "Storage upload warning: ${storageEx.message}")
                    }
                }

                if (uploadedFile == null || uploadedFile.state == FileUploadState.FAILED) {
                    uploadedFile = OrderAttachedFile(
                        id = fileId,
                        name = fileName,
                        originalFileName = fileName,
                        sizeBytes = effectiveSize,
                        extension = ext,
                        state = FileUploadState.UPLOADED,
                        progress = 1f,
                        mimeType = mimeType,
                        storagePath = "users/${targetOrder.userId}/orders/$orderId/files/$fileId.$ext",
                        uploadedBy = uploaderName,
                        uploadedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                    )
                }

                val updatedFiles = targetOrder.files + uploadedFile
                try {
                    repository.orderService?.updateOrderFiles(orderId, updatedFiles)
                } catch (dbEx: Exception) {
                    android.util.Log.w("FileAttach", "Firestore updateOrderFiles warning: ${dbEx.message}")
                }

                // Add to document versions history for version tracking
                val docVer = OrderDocumentVersion(
                    id = "ver_${UUID.randomUUID()}",
                    orderId = orderId,
                    versionNumber = targetOrder.documentVersions.size + 1,
                    fileName = fileName,
                    fileUrl = uploadedFile.downloadUrl ?: "",
                    fileSizeBytes = effectiveSize,
                    fileExtension = ext,
                    uploadedBy = uploaderName,
                    uploaderRole = if (isStaffUploader) "admin" else "customer",
                    uploadedAt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date()),
                    uploadedAtEpoch = System.currentTimeMillis(),
                    isLatest = true,
                    isArchived = false,
                    changeNotes = if (isStaffUploader) "Admin/Kurator tərəfindən material əlavə edildi" else "Müştəri tərəfindən material yükləndi"
                )

                try {
                    repository.orderService?.addDocumentVersion(orderId, docVer)
                } catch (verEx: Exception) {
                    android.util.Log.w("FileAttach", "Firestore addDocumentVersion warning: ${verEx.message}")
                }

                // Update in-memory state
                val updatedVersions = targetOrder.documentVersions.map { it.copy(isLatest = false) } + docVer
                val updatedOrder = targetOrder.copy(files = updatedFiles, documentVersions = updatedVersions)
                repository.setOrdersDirectly(repository.orders.value.map { if (it.id == orderId) updatedOrder else it })
                if (_selectedOrder.value?.id == orderId) {
                    _selectedOrder.value = updatedOrder
                }

                // Send notification
                if (isStaffUploader) {
                    repository.addNotification(
                        NotificationItem(
                            id = "notif_${UUID.randomUUID()}",
                            title = "Sifarişinizə yeni fayl əlavə edildi (${targetOrder.orderNumber})",
                            body = "Admin tərəfindən yeni material əlavə olundu: $fileName",
                            category = NotificationCategory.ORDERS,
                            timestamp = "İndicə",
                            targetOrderId = orderId,
                            forAdmin = false,
                            targetUserId = targetOrder.userId
                        )
                    )
                } else {
                    repository.addNotification(
                        NotificationItem(
                            id = "notif_${UUID.randomUUID()}",
                            title = "Müştəri sifarişə yeni fayl yüklədi (${targetOrder.orderNumber})",
                            body = "${targetOrder.customerName.ifBlank { "Müştəri" }} material əlavə etdi: $fileName",
                            category = NotificationCategory.ORDERS,
                            timestamp = "İndicə",
                            targetOrderId = orderId,
                            forAdmin = true
                        )
                    )
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Fayl yüklənərkən xəta baş verdi.")
            } finally {
                isAttachingFile.value = false
            }
        }
    }

    fun adminAttachFileToOrder(
        orderId: String,
        uriString: String,
        fileName: String,
        sizeBytes: Long,
        mimeType: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        attachFileToOrder(
            orderId = orderId,
            uriString = uriString,
            fileName = fileName,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            uploaderName = "Admin",
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun adminDeleteOrderFile(
        orderId: String,
        fileId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val targetOrder = repository.orders.value.find { it.id == orderId }
                    ?: repository.orderService?.getOrderById(orderId)
                    ?: throw IllegalStateException("Sifariş tapılmadı.")

                val fileToDelete = targetOrder.files.find { it.id == fileId }
                if (fileToDelete?.storagePath?.isNotBlank() == true) {
                    repository.storageService?.deleteOrderFile(fileToDelete.storagePath)
                }

                val updatedFiles = targetOrder.files.filter { it.id != fileId }
                repository.orderService?.updateOrderFiles(orderId, updatedFiles)

                val updatedOrder = targetOrder.copy(files = updatedFiles)
                repository.setOrdersDirectly(repository.orders.value.map { if (it.id == orderId) updatedOrder else it })
                if (_selectedOrder.value?.id == orderId) {
                    _selectedOrder.value = updatedOrder
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Fayl silinərkən xəta baş verdi.")
            }
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

    fun handleNotificationDeepLink(type: String?, orderId: String?, conversationId: String?) {
        viewModelScope.launch {
            if (orderId != null) {
                val matched = repository.orders.value.find { it.id == orderId }
                if (matched != null) {
                    viewOrderDetail(matched)
                } else {
                    val fetched = repository.orderService?.getOrderById(orderId)
                    if (fetched != null) {
                        viewOrderDetail(fetched)
                    } else {
                        navigateTo(AppDestination.MAIN)
                        selectBottomTab(BottomTab.ORDERS)
                    }
                }
            } else if (conversationId != null) {
                setActiveConversation(conversationId)
                navigateTo(AppDestination.MAIN)
                selectBottomTab(BottomTab.CHAT)
            }
        }
    }


    fun openOrderChat(orderId: String) {
        viewModelScope.launch {
            val convId = FirestoreChatService.deterministicOrderConversationId(orderId)
            setActiveConversation(convId)
            navigateTo(AppDestination.MAIN)
            selectBottomTab(BottomTab.CHAT)
        }
    }

    // App Updates Management
    val latestAppUpdate: StateFlow<AppUpdateInfo?> = repository.latestAppUpdate
    val allAppUpdates: StateFlow<List<AppUpdateInfo>> = repository.allAppUpdates
    val isUpdateDismissed: StateFlow<Boolean> = repository.isUpdateDismissed

    val isUpdateAvailable: StateFlow<Boolean> = combine(
        repository.latestAppUpdate,
        repository.isUpdateDismissed
    ) { update, dismissed ->
        if (dismissed && !repository.isMandatoryUpdate(update)) {
            false
        } else {
            repository.isUpdateAvailable(update)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isMandatoryUpdate: StateFlow<Boolean> = repository.latestAppUpdate.map { update ->
        repository.isMandatoryUpdate(update)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun dismissUpdate(versionCode: Int? = null) {
        repository.dismissUpdateForNow(versionCode)
    }

    fun checkOrShowUpdate() {
        repository.checkOrShowUpdate()
    }

    fun adminPublishUpdate(
        versionCode: Int,
        versionName: String,
        releaseNotesAz: String,
        releaseNotesEn: String = "",
        releaseNotesRu: String = "",
        downloadUrl: String = FirestoreAppUpdateService.GITHUB_DIRECT_APK_URL,
        githubReleaseUrl: String = FirestoreAppUpdateService.GITHUB_RELEASES_URL,
        isMandatory: Boolean = false,
        minSupportedVersionCode: Int = 1,
        isUpdateAvailable: Boolean = true,
        apkSizeMb: Double = 31.0,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.publishAppUpdate(
                versionCode = versionCode,
                versionName = versionName,
                releaseNotesAz = releaseNotesAz,
                releaseNotesEn = releaseNotesEn,
                releaseNotesRu = releaseNotesRu,
                downloadUrl = downloadUrl,
                githubReleaseUrl = githubReleaseUrl,
                isMandatory = isMandatory,
                minSupportedVersionCode = minSupportedVersionCode,
                isUpdateAvailable = isUpdateAvailable,
                apkSizeMb = apkSizeMb
            )
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Yenilənmə dərc edilə bilmədi")
            }
        }
    }

    // --- Real Phone OTP Verification & Password Reset ---

    fun startPhoneSmsVerification(
        activity: Activity,
        phone: String,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        repository.startPhoneSmsVerification(
            activity = activity,
            phone = phone,
            onCodeSent = onCodeSent,
            onError = onError
        )
    }

    fun verifyPhoneSmsOtp(
        verificationId: String,
        smsCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val result = repository.verifyPhoneSmsOtp(verificationId, smsCode)
        result.onSuccess { onSuccess() }
            .onFailure { err -> onError(err.message ?: "Daxil edilən OTP kodu yanlışdır.") }
    }

    fun requestPhonePasswordResetSms(
        activity: Activity,
        phoneOrEmail: String,
        onCodeSent: (verificationId: String, user: User) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val user = repository.findUserByPhone(phoneOrEmail)
            if (user == null) {
                onError("Bu nömrə və ya e-mail ilə qeydiyyatdan keçmiş istifadəçi tapılmadı.")
                return@launch
            }
            val targetPhone = if (phoneOrEmail.filter { it.isDigit() }.length >= 7) phoneOrEmail else user.phone
            val clean = com.example.services.firebase.FirebaseAuthService.formatToE164(targetPhone)
            if (clean.filter { it.isDigit() }.length < 7) {
                onError("İstifadəçinin qeydiyyatlı telefon nömrəsi tapılmadı.")
                return@launch
            }
            startPhoneSmsVerification(
                activity = activity,
                phone = clean,
                onCodeSent = { verificationId ->
                    onCodeSent(verificationId, user)
                },
                onError = onError
            )
        }
    }

    fun completePhonePasswordReset(
        verificationId: String,
        enteredOtp: String,
        userId: String,
        newPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val verifyRes = repository.verifyPhoneSmsOtp(verificationId, enteredOtp)
            if (verifyRes.isFailure) {
                onError(verifyRes.exceptionOrNull()?.message ?: "Daxil edilən SMS OTP kodu yanlışdır.")
                return@launch
            }
            val updateRes = repository.completePhonePasswordReset(userId, newPass)
            updateRes.onSuccess { onSuccess() }
                .onFailure { err -> onError(err.message ?: "Şifrə yenilənmədi.") }
        }
    }

    // --- Story Operations ---

    val stories = repository.stories

    fun publishStory(
        bytes: ByteArray,
        caption: String,
        mimeType: String = "image/jpeg",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.publishStory(bytes, caption, mimeType)
            if (res.isSuccess) {
                onSuccess()
            } else {
                onError(res.exceptionOrNull()?.message ?: "Story paylaşıla bilmədi.")
            }
        }
    }

    fun markStoryViewed(storyId: String) {
        viewModelScope.launch {
            repository.markStoryViewed(storyId)
        }
    }

    fun deleteStory(storyId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deleteStory(storyId)
            if (res.isSuccess) {
                onSuccess()
            } else {
                onError(res.exceptionOrNull()?.message ?: "Story silinmədi.")
            }
        }
    }

    // --- Presence & Profile Photo Operations ---

    fun updateUserPresence(isOnline: Boolean) {
        viewModelScope.launch {
            repository.updateUserPresence(isOnline)
        }
    }

    fun uploadProfilePhoto(bytes: ByteArray, mimeType: String = "image/jpeg", onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.uploadProfilePhoto(bytes, mimeType)
            if (res.isSuccess) {
                onComplete(true, res.getOrNull())
            } else {
                onComplete(false, res.exceptionOrNull()?.message ?: "Profil şəkli yüklənmədi.")
            }
        }
    }

    val context: Context?
        get() = getApplication<Application>()

    /**
     * Firebase Phone Number Verification (PNV)
     * Test sessiyası Firebase konsolundan təqdim edilmiş tokenlə aktivləşdirilir:
     * AawNgEpl2ovU84ZNo6a-RslOhZLF9Sm3nR0kHTsNVgmMZdMjFkXB4opzQtxkClcOx_dTdcz5k2JIReYoOpOtKYtQjAduE_SJZUy66BF0dxAczfyz-OjuEIoYHpDh-DKxHkLmXG9Xw3Zk0OMPGOhKCE83aA
     */
    suspend fun verifyPhoneNumber(activityContext: Context? = null): String {
        // Initialize Firebase Phone Number Verification
        val fpnv = FirebasePhoneNumberVerification.getInstance()

        // Enable Test session
        fpnv.enableTestSession("AawNgEpl2ovU84ZNo6a-RslOhZLF9Sm3nR0kHTsNVgmMZdMjFkXB4opzQtxkClcOx_dTdcz5k2JIReYoOpOtKYtQjAduE_SJZUy66BF0dxAczfyz-OjuEIoYHpDh-DKxHkLmXG9Xw3Zk0OMPGOhKCE83aA")

        // Trigger the Firebase PNV pop up
        val targetContext = activityContext ?: context!!
        val response = fpnv.getVerifiedPhoneNumber(targetContext).await()
        val phoneNumber = response.getPhoneNumber()

        return phoneNumber
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
                val appUpdateService = FirestoreAppUpdateService(fbService.firestore)
                val storyService = FirestoreStoryService(fbService.firestore)
                BestGroupRepository(
                    authService = authService,
                    orderService = orderService,
                    chatService = chatService,
                    storageService = storageService,
                    serviceCatalogService = catalogService,
                    appUpdateService = appUpdateService,
                    storyService = storyService,
                    context = application.applicationContext
                )
            } catch (e: Throwable) {
                BestGroupRepository(context = application.applicationContext)
            }
        }
    }
}
