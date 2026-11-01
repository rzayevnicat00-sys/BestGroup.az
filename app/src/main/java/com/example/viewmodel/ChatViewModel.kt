package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.pnv.FirebasePhoneNumberVerification
import kotlinx.coroutines.tasks.await

/**
 * ChatViewModel handles chat-related operations and Firebase Phone Number Verification (PNV).
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

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
}
