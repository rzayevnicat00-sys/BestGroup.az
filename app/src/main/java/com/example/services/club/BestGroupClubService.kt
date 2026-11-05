package com.example.services.club

import com.example.model.BonusTransaction
import com.example.model.ClubMembership
import com.example.model.ClubTier
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BestGroupClubService(private val firestore: FirebaseFirestore) {

    private val _membership = MutableStateFlow(ClubMembership())
    val membership: StateFlow<ClubMembership> = _membership.asStateFlow()

    private val _bonusTransactions = MutableStateFlow<List<BonusTransaction>>(emptyList())
    val bonusTransactions: StateFlow<List<BonusTransaction>> = _bonusTransactions.asStateFlow()

    suspend fun loadClubData(userId: String) {
        if (userId.isBlank()) return
        try {
            val docRef = firestore.collection("users").document(userId).collection("club").document("membership")
            val doc = docRef.get().await()
            if (doc.exists()) {
                val totalSpent = (doc.getDouble("totalSpentAzn") ?: 0.0)
                val bonusBal = (doc.getDouble("bonusBalanceAzn") ?: 0.0)
                val completedCount = (doc.getLong("completedOrdersCount") ?: 0L).toInt()
                val refCode = doc.getString("referralCode") ?: generateReferralCode(userId)
                val referredCount = (doc.getLong("referredUsersCount") ?: 0L).toInt()
                val refBonus = doc.getDouble("totalReferralBonusAzn") ?: 0.0

                val tier = calculateTier(totalSpent)
                _membership.value = ClubMembership(
                    userId = userId,
                    tier = tier,
                    bonusBalanceAzn = bonusBal,
                    totalSpentAzn = totalSpent,
                    completedOrdersCount = completedCount,
                    referralCode = refCode,
                    referredUsersCount = referredCount,
                    totalReferralBonusAzn = refBonus,
                    perkList = getTierPerks(tier)
                )
            } else {
                val initialCode = generateReferralCode(userId)
                val initialMembership = ClubMembership(
                    userId = userId,
                    tier = ClubTier.STANDARD,
                    bonusBalanceAzn = 5.0, // 5 AZN welcome bonus
                    totalSpentAzn = 0.0,
                    completedOrdersCount = 0,
                    referralCode = initialCode,
                    referredUsersCount = 0,
                    totalReferralBonusAzn = 0.0,
                    perkList = getTierPerks(ClubTier.STANDARD)
                )
                _membership.value = initialMembership
                docRef.set(
                    mapOf(
                        "totalSpentAzn" to 0.0,
                        "bonusBalanceAzn" to 5.0,
                        "completedOrdersCount" to 0,
                        "referralCode" to initialCode,
                        "referredUsersCount" to 0,
                        "totalReferralBonusAzn" to 0.0,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()

                addTransaction(
                    userId = userId,
                    amount = 5.0,
                    isCredit = true,
                    description = "Qoşulma bonusu (Xoş gəldiniz!)"
                )
            }

            loadTransactions(userId)
        } catch (_: Exception) {}
    }

    private suspend fun loadTransactions(userId: String) {
        try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("bonus_transactions")
                .orderBy("timestampEpoch", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { doc ->
                val amount = doc.getDouble("amountAzn") ?: 0.0
                val isCredit = doc.getBoolean("isCredit") ?: true
                val desc = doc.getString("description") ?: ""
                val ts = doc.getString("timestamp") ?: ""
                val epoch = doc.getLong("timestampEpoch") ?: 0L
                val oId = doc.getString("orderId")
                BonusTransaction(
                    id = doc.id,
                    userId = userId,
                    amountAzn = amount,
                    isCredit = isCredit,
                    description = desc,
                    orderId = oId,
                    timestamp = ts,
                    timestampEpoch = epoch
                )
            }
            _bonusTransactions.value = list
        } catch (_: Exception) {}
    }

    suspend fun addTransaction(
        userId: String,
        amount: Double,
        isCredit: Boolean,
        description: String,
        orderId: String? = null
    ) {
        if (userId.isBlank() || amount <= 0.0) return
        try {
            val transId = UUID.randomUUID().toString()
            val epoch = System.currentTimeMillis()
            val dateStr = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(epoch))

            firestore.collection("users").document(userId)
                .collection("bonus_transactions").document(transId)
                .set(
                    mapOf(
                        "amountAzn" to amount,
                        "isCredit" to isCredit,
                        "description" to description,
                        "orderId" to (orderId ?: ""),
                        "timestamp" to dateStr,
                        "timestampEpoch" to epoch
                    )
                ).await()

            val cur = _membership.value
            val newBal = if (isCredit) cur.bonusBalanceAzn + amount else (cur.bonusBalanceAzn - amount).coerceAtLeast(0.0)
            _membership.value = cur.copy(bonusBalanceAzn = Math.round(newBal * 100.0) / 100.0)

            firestore.collection("users").document(userId).collection("club").document("membership")
                .update("bonusBalanceAzn", _membership.value.bonusBalanceAzn)
                .await()

            loadTransactions(userId)
        } catch (_: Exception) {}
    }

    private fun calculateTier(totalSpentAzn: Double): ClubTier {
        return when {
            totalSpentAzn >= ClubTier.PLATINUM.minSpendAzn -> ClubTier.PLATINUM
            totalSpentAzn >= ClubTier.GOLD.minSpendAzn -> ClubTier.GOLD
            totalSpentAzn >= ClubTier.SILVER.minSpendAzn -> ClubTier.SILVER
            else -> ClubTier.STANDARD
        }
    }

    private fun getTierPerks(tier: ClubTier): List<String> {
        return when (tier) {
            ClubTier.STANDARD -> listOf(
                "Hər sifarişdən 2% bonus keşbek",
                "Referal proqramı (hər dəvətə 5 AZN)",
                "Standart növbə və dəstək"
            )
            ClubTier.SILVER -> listOf(
                "Hər sifarişdən 4% bonus keşbek",
                "Bütün xidmətlərə 3% daimi endirim",
                "Referal proqramı (hər dəvətə 7 AZN)",
                "Prioritet müştəri dəstəyi"
            )
            ClubTier.GOLD -> listOf(
                "Hər sifarişdən 6% bonus keşbek",
                "Bütün xidmətlərə 5% daimi endirim",
                "Referal proqramı (hər dəvətə 10 AZN)",
                "Pulsuz AAK ilkin plagiat yoxlaması",
                "Təcili sifarişlərdə 20% xüsusi güzəşt"
            )
            ClubTier.PLATINUM -> listOf(
                "Hər sifarişdən 10% VIP keşbek",
                "Bütün xidmətlərə 8% daimi endirim",
                "Referal proqramı (hər dəvətə 15 AZN)",
                "Şəxsi elmi məsləhətçi və menecer",
                "Pulsuz qeyri-məhdud anti-plagiat yoxlamaları",
                "Bütün kampaniyalara 24 saat erkən giriş"
            )
        }
    }

    private fun generateReferralCode(userId: String): String {
        val clean = userId.filter { it.isLetterOrDigit() }.take(5).uppercase()
        return "BG-$clean"
    }
}
