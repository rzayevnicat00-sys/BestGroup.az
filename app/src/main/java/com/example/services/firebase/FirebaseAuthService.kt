package com.example.services.firebase

import com.example.model.User
import com.example.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val rawException: Exception? = null) : AuthResult<Nothing>()
}

class FirebaseAuthService(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified == true

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { currentAuth ->
            trySend(currentAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
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
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val firebaseUser = authResult.user ?: throw Exception("İstifadəçi yaradıla bilmədi.")

            // Send Email Verification
            try {
                firebaseUser.sendEmailVerification().await()
            } catch (_: Exception) {
                // Non-blocking if email verification send encounters issue
            }

            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

            val userProfile = User(
                id = firebaseUser.uid,
                fullName = fullName.trim(),
                email = email.trim(),
                phone = phone.trim(),
                university = university.trim(),
                faculty = faculty.trim(),
                degreeLevel = educationLevel.trim(),
                role = UserRole.CUSTOMER
            )

            val profileMap = hashMapOf(
                "uid" to firebaseUser.uid,
                "fullName" to userProfile.fullName,
                "email" to userProfile.email,
                "phone" to userProfile.phone,
                "university" to userProfile.university,
                "faculty" to userProfile.faculty,
                "educationLevel" to userProfile.degreeLevel,
                "profilePhoto" to "",
                "role" to "customer",
                "preferredLanguage" to "az",
                "theme" to "system",
                "createdAt" to now,
                "updatedAt" to now
            )

            // Save to Firestore under users/{userId}
            firestore.collection("users").document(firebaseUser.uid).set(profileMap).await()

            AuthResult.Success(userProfile)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e), e)
        }
    }

    suspend fun signIn(email: String, pass: String): AuthResult<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val firebaseUser = authResult.user ?: throw Exception("Giriş uğursuz oldu.")

            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            val roleStr = userDoc.getString("role") ?: "customer"
            val role = when (roleStr.lowercase()) {
                "admin" -> UserRole.ADMIN
                "manager" -> UserRole.MANAGER
                "operator" -> UserRole.OPERATOR
                else -> UserRole.CUSTOMER
            }

            val user = User(
                id = firebaseUser.uid,
                fullName = userDoc.getString("fullName") ?: (firebaseUser.displayName ?: "İstifadəçi"),
                email = userDoc.getString("email") ?: (firebaseUser.email ?: email),
                phone = userDoc.getString("phone") ?: "",
                university = userDoc.getString("university") ?: "",
                faculty = userDoc.getString("faculty") ?: "",
                degreeLevel = userDoc.getString("educationLevel") ?: "Bakalavriat",
                role = role
            )

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e), e)
        }
    }

    suspend fun fetchUserProfile(userId: String): User? {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val roleStr = doc.getString("role") ?: "customer"
                val role = when (roleStr.lowercase()) {
                    "admin" -> UserRole.ADMIN
                    "manager" -> UserRole.MANAGER
                    "operator" -> UserRole.OPERATOR
                    else -> UserRole.CUSTOMER
                }
                User(
                    id = userId,
                    fullName = doc.getString("fullName") ?: "",
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    university = doc.getString("university") ?: "",
                    faculty = doc.getString("faculty") ?: "",
                    degreeLevel = doc.getString("educationLevel") ?: "Bakalavriat",
                    role = role
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun reloadUser(): Boolean {
        return try {
            auth.currentUser?.reload()?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkEmailVerification(): Boolean {
        reloadUser()
        return isEmailVerified
    }

    suspend fun sendPasswordReset(email: String): AuthResult<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e), e)
        }
    }

    suspend fun resendVerificationEmail(): AuthResult<Unit> {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e), e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    companion object {
        fun mapFirebaseError(e: Exception): String {
            val msg = e.message ?: ""
            return when {
                e is FirebaseAuthException -> when (e.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> "Bu e-mail ilə qeydiyyatdan keçmiş istifadəçi tapılmadı."
                    "ERROR_WRONG_PASSWORD" -> "Daxil edilən şifrə yanlışdır."
                    "ERROR_INVALID_EMAIL" -> "Daxil edilən e-mail ünvanının formatı düzgün deyil."
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Bu e-mail ünvanı ilə artıq başqa bir hesab qeydiyyatdan keçib."
                    "ERROR_WEAK_PASSWORD" -> "Şifrə çox zəifdir. Ən azı 6 simvoldan ibarət olmalıdır."
                    "ERROR_USER_DISABLED" -> "Bu istifadəçi hesabı sistem tərəfindən deaktiv edilib."
                    "ERROR_TOO_MANY_REQUESTS" -> "Çox sayda uğursuz cəhd edildi. Zəhmət olmasa bir qədər sonra yenidən cəhd edin."
                    "ERROR_OPERATION_NOT_ALLOWED" -> "E-mail/şifrə ilə giriş Firebase Console-da aktivləşdirilməyib."
                    "ERROR_NETWORK_REQUEST_FAILED" -> "İnternet bağlantısı yoxdur. Şəbəkə əlaqənizi yoxlayın."
                    else -> "Autentifikasiya xətası: ${e.localizedMessage ?: e.errorCode}"
                }
                msg.contains("The email address is already in use", ignoreCase = true) ->
                    "Bu e-mail ünvanı ilə artıq başqa bir hesab qeydiyyatdan keçib."
                msg.contains("The email address is badly formatted", ignoreCase = true) ||
                msg.contains("invalid email", ignoreCase = true) ->
                    "Daxil edilən e-mail ünvanının formatı düzgün deyil."
                msg.contains("There is no user record", ignoreCase = true) ->
                    "Bu e-mail ilə qeydiyyatdan keçmiş istifadəçi tapılmadı."
                msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                msg.contains("wrong password", ignoreCase = true) ||
                msg.contains("invalid credential", ignoreCase = true) ->
                    "Daxil edilən e-mail və ya şifrə yanlışdır."
                msg.contains("Password should be at least 6 characters", ignoreCase = true) ||
                msg.contains("weak-password", ignoreCase = true) ->
                    "Şifrə ən azı 6 simvoldan ibarət olmalıdır."
                msg.contains("network error", ignoreCase = true) ||
                msg.contains("network-request-failed", ignoreCase = true) ->
                    "İnternet bağlantısı yoxdur. Şəbəkə bağlantınızı yoxlayın."
                msg.contains("too many requests", ignoreCase = true) ||
                msg.contains("too-many-requests", ignoreCase = true) ->
                    "Çox sayda uğursuz cəhd edildi. Zəhmət olmasa bir qədər sonra yenidən cəhd edin."
                msg.contains("user-disabled", ignoreCase = true) ->
                    "Bu istifadəçi hesabı sistem tərəfindən deaktiv edilib."
                else -> e.localizedMessage ?: "Əməliyyat zamanı xəta baş verdi. Zəhmət olmasa yenidən cəhd edin."
            }
        }
    }
}
