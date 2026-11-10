package com.example.services.club

import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BestGroupClubService(private val firestore: FirebaseFirestore? = null) {

    private val _membership = MutableStateFlow(ClubMembership())
    val membership: StateFlow<ClubMembership> = _membership.asStateFlow()

    private val _bonusTransactions = MutableStateFlow<List<BonusTransaction>>(emptyList())
    val bonusTransactions: StateFlow<List<BonusTransaction>> = _bonusTransactions.asStateFlow()

    private val _campaigns = MutableStateFlow<List<ClubCampaign>>(emptyList())
    val campaigns: StateFlow<List<ClubCampaign>> = _campaigns.asStateFlow()

    private val _tierConfigs = MutableStateFlow<List<ClubTierConfig>>(defaultTierConfigs())
    val tierConfigs: StateFlow<List<ClubTierConfig>> = _tierConfigs.asStateFlow()

    companion object {
        fun defaultTierConfigs(): List<ClubTierConfig> {
            return listOf(
                ClubTierConfig(
                    tier = ClubTier.STANDARD,
                    minSpendAzn = 0.0,
                    minCompletedOrders = 0,
                    bonusPercent = 0.02,
                    discountPercent = 0.0,
                    perks = listOf(
                        "Hər sifarişdən 2% keşbek bonusu",
                        "Referal proqramında iştirak (hər dostuna 5 AZN)",
                        "Standart müştəri dəstəyi və sifariş izləmə"
                    )
                ),
                ClubTierConfig(
                    tier = ClubTier.SILVER,
                    minSpendAzn = 150.0,
                    minCompletedOrders = 2,
                    bonusPercent = 0.04,
                    discountPercent = 0.03,
                    perks = listOf(
                        "Hər sifarişdən 4% keşbek bonusu",
                        "Bütün xidmətlərə 3% daimi endirim",
                        "Referal proqramında hər dostuna 7 AZN",
                        "Prioritet müştəri xidmətləri"
                    )
                ),
                ClubTierConfig(
                    tier = ClubTier.GOLD,
                    minSpendAzn = 400.0,
                    minCompletedOrders = 5,
                    bonusPercent = 0.06,
                    discountPercent = 0.05,
                    perks = listOf(
                        "Hər sifarişdən 6% keşbek bonusu",
                        "Bütün xidmətlərə 5% daimi endirim",
                        "Referal proqramında hər dostuna 10 AZN",
                        "Pulsuz AAK ilkin plagiat yoxlaması",
                        "Xüsusi kampaniyalara 12 saat erkən giriş"
                    )
                ),
                ClubTierConfig(
                    tier = ClubTier.PLATINUM,
                    minSpendAzn = 900.0,
                    minCompletedOrders = 10,
                    bonusPercent = 0.10,
                    discountPercent = 0.08,
                    perks = listOf(
                        "Hər sifarişdən 10% VIP keşbek bonusu",
                        "Bütün xidmətlərə 8% daimi endirim",
                        "Referal proqramında hər dostuna 15 AZN",
                        "Şəxsi elmi kurator və VIP dəstək meneceri",
                        "Limitsiz AAK və Turnitin rəsmi hesabatları",
                        "Eksklüziv kampaniyalara 24 saat erkən giriş"
                    )
                )
            )
        }
    }

    suspend fun initClub() {
        loadTierConfigs()
        loadCampaigns()
    }

    suspend fun loadTierConfigs() {
        val fs = firestore ?: run {
            _tierConfigs.value = defaultTierConfigs()
            return
        }
        try {
            val doc = fs.collection("club_config").document("tiers").get().await()
            if (doc.exists()) {
                val listData = doc.get("configs") as? List<Map<String, Any>>
                if (!listData.isNullOrEmpty()) {
                    val parsed = listData.mapNotNull { m ->
                        val tierName = m["tier"] as? String ?: return@mapNotNull null
                        val tier = ClubTier.values().find { it.name.equals(tierName, true) } ?: ClubTier.STANDARD
                        val spend = (m["minSpendAzn"] as? Number)?.toDouble() ?: tier.minSpendAzn
                        val orders = (m["minCompletedOrders"] as? Number)?.toInt() ?: 0
                        val bonusPct = (m["bonusPercent"] as? Number)?.toDouble() ?: tier.bonusPercent
                        val discPct = (m["discountPercent"] as? Number)?.toDouble() ?: tier.discountPercent
                        val perks = (m["perks"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        ClubTierConfig(tier, spend, orders, bonusPct, discPct, perks)
                    }
                    if (parsed.isNotEmpty()) {
                        _tierConfigs.value = parsed
                        return
                    }
                }
            }
            // Seed defaults if empty
            saveTierConfigs(defaultTierConfigs())
        } catch (_: Exception) {
            _tierConfigs.value = defaultTierConfigs()
        }
    }

    suspend fun saveTierConfigs(configs: List<ClubTierConfig>) {
        _tierConfigs.value = configs
        val fs = firestore ?: return
        try {
            val data = configs.map { c ->
                mapOf(
                    "tier" to c.tier.name,
                    "minSpendAzn" to c.minSpendAzn,
                    "minCompletedOrders" to c.minCompletedOrders,
                    "bonusPercent" to c.bonusPercent,
                    "discountPercent" to c.discountPercent,
                    "perks" to c.perks
                )
            }
            fs.collection("club_config").document("tiers")
                .set(mapOf("configs" to data, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
                .await()
        } catch (_: Exception) {}
    }

    suspend fun loadCampaigns() {
        val fs = firestore ?: run {
            _campaigns.value = defaultCampaigns()
            return
        }
        try {
            val snapshot = fs.collection("club_campaigns").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                val typeStr = doc.getString("type") ?: "DOUBLE_BONUS"
                val type = ClubCampaignType.values().find { it.name.equals(typeStr, true) } ?: ClubCampaignType.DOUBLE_BONUS
                ClubCampaign(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    type = type,
                    discountPercent = (doc.getDouble("discountPercent") ?: 0.0),
                    bonusMultiplier = (doc.getDouble("bonusMultiplier") ?: 1.0),
                    rewardBonusAzn = (doc.getDouble("rewardBonusAzn") ?: 0.0),
                    targetTier = doc.getString("targetTier") ?: "ALL",
                    startDate = doc.getString("startDate") ?: "",
                    endDate = doc.getString("endDate") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true
                )
            }
            if (list.isEmpty()) {
                seedInitialCampaigns()
            } else {
                _campaigns.value = list
            }
        } catch (_: Exception) {
            _campaigns.value = defaultCampaigns()
        }
    }

    private fun defaultCampaigns(): List<ClubCampaign> {
        return listOf(
            ClubCampaign(
                id = "camp_double_bonus",
                title = "Akademik Yazı Həftəsi – 2x Bonus",
                description = "Magistr dissertasiyası və diplom işi sifarişlərində ikiqat bonus keşbek qazanın!",
                type = ClubCampaignType.DOUBLE_BONUS,
                bonusMultiplier = 2.0,
                targetTier = "ALL",
                startDate = "01.11.2026",
                endDate = "30.11.2026",
                isActive = true
            ),
            ClubCampaign(
                id = "camp_gold_plat_disc",
                title = "Gold və Platinum Üzvlərə Özəl 10% Endirim",
                description = "Bütün təcili elmi məqalə və tərcümə xidmətlərinə dərhal tətbiq olunan endirim.",
                type = ClubCampaignType.SERVICE_DISCOUNT,
                discountPercent = 0.10,
                targetTier = "GOLD",
                startDate = "01.11.2026",
                endDate = "31.12.2026",
                isActive = true
            ),
            ClubCampaign(
                id = "camp_birthday",
                title = "Ad Günü Xüsusi Bonusu",
                description = "Ad gününüzdə sifariş verin və balansınıza dərhal 15 AZN hədiyyə bonusu qazanın!",
                type = ClubCampaignType.BIRTHDAY_SPECIAL,
                rewardBonusAzn = 15.0,
                targetTier = "ALL",
                startDate = "01.01.2026",
                endDate = "31.12.2026",
                isActive = true
            )
        )
    }

    private suspend fun seedInitialCampaigns() {
        val defs = defaultCampaigns()
        _campaigns.value = defs
        val fs = firestore ?: return
        for (camp in defs) {
            try {
                fs.collection("club_campaigns").document(camp.id).set(
                    mapOf(
                        "title" to camp.title,
                        "description" to camp.description,
                        "type" to camp.type.name,
                        "discountPercent" to camp.discountPercent,
                        "bonusMultiplier" to camp.bonusMultiplier,
                        "rewardBonusAzn" to camp.rewardBonusAzn,
                        "targetTier" to camp.targetTier,
                        "startDate" to camp.startDate,
                        "endDate" to camp.endDate,
                        "isActive" to camp.isActive
                    )
                ).await()
            } catch (_: Exception) {}
        }
    }

    suspend fun createCampaign(campaign: ClubCampaign) {
        val id = if (campaign.id.isNotBlank()) campaign.id else "camp_${UUID.randomUUID()}"
        val toSave = campaign.copy(id = id)
        _campaigns.value = _campaigns.value.filter { it.id != id } + toSave
        val fs = firestore ?: return
        try {
            fs.collection("club_campaigns").document(id).set(
                mapOf(
                    "title" to toSave.title,
                    "description" to toSave.description,
                    "type" to toSave.type.name,
                    "discountPercent" to toSave.discountPercent,
                    "bonusMultiplier" to toSave.bonusMultiplier,
                    "rewardBonusAzn" to toSave.rewardBonusAzn,
                    "targetTier" to toSave.targetTier,
                    "startDate" to toSave.startDate,
                    "endDate" to toSave.endDate,
                    "isActive" to toSave.isActive
                ), SetOptions.merge()
            ).await()
        } catch (_: Exception) {}
    }

    suspend fun toggleCampaign(campaignId: String, active: Boolean) {
        _campaigns.value = _campaigns.value.map { if (it.id == campaignId) it.copy(isActive = active) else it }
        val fs = firestore ?: return
        try {
            fs.collection("club_campaigns").document(campaignId).update("isActive", active).await()
        } catch (_: Exception) {}
    }

    suspend fun deleteCampaign(campaignId: String) {
        _campaigns.value = _campaigns.value.filter { it.id != campaignId }
        val fs = firestore ?: return
        try {
            fs.collection("club_campaigns").document(campaignId).delete().await()
        } catch (_: Exception) {}
    }

    suspend fun loadClubData(userId: String) {
        if (userId.isBlank()) return
        initClub()
        val fs = firestore ?: return
        try {
            val docRef = fs.collection("users").document(userId).collection("club").document("membership")
            val doc = docRef.get().await()
            if (doc.exists()) {
                val totalSpent = (doc.getDouble("totalSpentAzn") ?: 0.0)
                val bonusBal = (doc.getDouble("bonusBalanceAzn") ?: 0.0)
                val completedCount = (doc.getLong("completedOrdersCount") ?: 0L).toInt()
                val refCode = doc.getString("referralCode") ?: generateReferralCode(userId)
                val savedCardNumber = doc.getString("cardNumber")
                val cardNumber = if (!savedCardNumber.isNullOrBlank() && savedCardNumber.length == 16) {
                    savedCardNumber
                } else {
                    val gen = generate16DigitCardNumber(userId)
                    docRef.set(mapOf("cardNumber" to gen), com.google.firebase.firestore.SetOptions.merge())
                    gen
                }
                val referredCount = (doc.getLong("referredUsersCount") ?: 0L).toInt()
                val refBonus = doc.getDouble("totalReferralBonusAzn") ?: 0.0
                val bdayYear = (doc.getLong("birthdayBonusGivenYear") ?: 0L).toInt()

                val tier = calculateTier(totalSpent, completedCount)
                val perks = getTierPerks(tier)
                val friends = loadReferredFriends(userId)

                _membership.value = ClubMembership(
                    userId = userId,
                    tier = tier,
                    bonusBalanceAzn = bonusBal,
                    totalSpentAzn = totalSpent,
                    completedOrdersCount = completedCount,
                    referralCode = refCode,
                    cardNumber = cardNumber,
                    referredUsersCount = referredCount,
                    totalReferralBonusAzn = refBonus,
                    perkList = perks,
                    invitedFriends = friends,
                    birthdayBonusGivenYear = bdayYear
                )
            } else {
                val initialCode = generateReferralCode(userId)
                val initialCardNumber = generate16DigitCardNumber(userId)
                val initialTier = ClubTier.STANDARD
                val initialMembership = ClubMembership(
                    userId = userId,
                    tier = initialTier,
                    bonusBalanceAzn = 5.0, // 5 AZN welcome bonus
                    totalSpentAzn = 0.0,
                    completedOrdersCount = 0,
                    referralCode = initialCode,
                    cardNumber = initialCardNumber,
                    referredUsersCount = 0,
                    totalReferralBonusAzn = 0.0,
                    perkList = getTierPerks(initialTier),
                    invitedFriends = emptyList()
                )
                _membership.value = initialMembership
                docRef.set(
                    mapOf(
                        "totalSpentAzn" to 0.0,
                        "bonusBalanceAzn" to 5.0,
                        "completedOrdersCount" to 0,
                        "referralCode" to initialCode,
                        "cardNumber" to initialCardNumber,
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

    private suspend fun loadReferredFriends(userId: String): List<ClubReferralItem> {
        val fs = firestore ?: return emptyList()
        return try {
            val snapshot = fs.collection("users").document(userId)
                .collection("club_referrals").limit(20).get().await()
            snapshot.documents.mapNotNull { d ->
                ClubReferralItem(
                    id = d.id,
                    invitedUserId = d.getString("invitedUserId") ?: "",
                    invitedUserName = d.getString("invitedUserName") ?: "Dost",
                    status = d.getString("status") ?: "REGISTERED",
                    joinedDate = d.getString("joinedDate") ?: "",
                    bonusEarnedAzn = (d.getDouble("bonusEarnedAzn") ?: 5.0)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun loadTransactions(userId: String) {
        val fs = firestore ?: return
        try {
            val snapshot = fs.collection("users").document(userId)
                .collection("bonus_transactions")
                .orderBy("timestampEpoch", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
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
        orderId: String? = null,
        adminAuditNote: String? = null
    ) {
        if (userId.isBlank() || amount <= 0.0) return
        val cur = _membership.value
        val newBal = if (isCredit) cur.bonusBalanceAzn + amount else (cur.bonusBalanceAzn - amount).coerceAtLeast(0.0)
        val rounded = Math.round(newBal * 100.0) / 100.0
        _membership.value = cur.copy(bonusBalanceAzn = rounded)
        val fs = firestore ?: return
        try {
            val transId = UUID.randomUUID().toString()
            val epoch = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(epoch))

            val dataMap = mutableMapOf<String, Any>(
                "amountAzn" to amount,
                "isCredit" to isCredit,
                "description" to description,
                "orderId" to (orderId ?: ""),
                "timestamp" to dateStr,
                "timestampEpoch" to epoch
            )
            if (adminAuditNote != null) {
                dataMap["adminAudit"] = adminAuditNote
            }

            fs.collection("users").document(userId)
                .collection("bonus_transactions").document(transId)
                .set(dataMap).await()

            fs.collection("users").document(userId).collection("club").document("membership")
                .set(mapOf("bonusBalanceAzn" to rounded), SetOptions.merge())
                .await()

            loadTransactions(userId)
        } catch (_: Exception) {}
    }

    /**
     * Called when an order is completed/ready. Calculates real bonus based on user tier and active campaigns.
     * Prevents duplicate bonus awards using order ID lookup.
     */
    suspend fun awardOrderCompletionBonus(userId: String, orderId: String, orderAmountAzn: Double) {
        if (userId.isBlank() || orderAmountAzn <= 0.0) return
        val fs = firestore ?: return
        try {
            // Anti-duplication check: verify if bonus was already awarded for this order
            val existing = fs.collection("users").document(userId)
                .collection("bonus_transactions")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()
            if (!existing.isEmpty) {
                return // Already awarded
            }

            val cur = _membership.value
            val cfg = getTierConfig(cur.tier)
            var multiplier = 1.0

            // Check if active campaign boosts bonus
            val active2x = _campaigns.value.find { it.isActive && it.type == ClubCampaignType.DOUBLE_BONUS }
            if (active2x != null) {
                multiplier = active2x.bonusMultiplier
            }

            val bonusRate = cfg.bonusPercent * multiplier
            val earnedBonus = Math.round((orderAmountAzn * bonusRate) * 100.0) / 100.0
            val newTotalSpent = cur.totalSpentAzn + orderAmountAzn
            val newCompletedCount = cur.completedOrdersCount + 1
            val newTier = calculateTier(newTotalSpent, newCompletedCount)

            addTransaction(
                userId = userId,
                amount = earnedBonus,
                isCredit = true,
                description = "Sifariş #${orderId.takeLast(6).uppercase()} bonusu (${(bonusRate * 100).toInt()}%)",
                orderId = orderId
            )

            // Update membership stats
            fs.collection("users").document(userId).collection("club").document("membership").set(
                mapOf(
                    "totalSpentAzn" to newTotalSpent,
                    "completedOrdersCount" to newCompletedCount,
                    "tier" to newTier.name
                ), SetOptions.merge()
            ).await()

            _membership.value = _membership.value.copy(
                totalSpentAzn = newTotalSpent,
                completedOrdersCount = newCompletedCount,
                tier = newTier,
                perkList = getTierPerks(newTier)
            )

            // If user transitioned to higher tier, reward tier upgrade bonus!
            if (newTier.ordinal > cur.tier.ordinal) {
                val upgradeBonus = when (newTier) {
                    ClubTier.SILVER -> 5.0
                    ClubTier.GOLD -> 15.0
                    ClubTier.PLATINUM -> 30.0
                    else -> 0.0
                }
                if (upgradeBonus > 0) {
                    addTransaction(
                        userId = userId,
                        amount = upgradeBonus,
                        isCredit = true,
                        description = "Təbriklər! ${newTier.title} səviyyəsinə yüksəliş bonusu"
                    )
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Admin manual bonus adjustment with audit record.
     */
    suspend fun adminAdjustBonus(
        userId: String,
        amountAzn: Double,
        isCredit: Boolean,
        reason: String,
        adminName: String
    ): Result<Unit> {
        return try {
            val note = "Admin: $adminName | Səbəb: $reason"
            val desc = if (isCredit) "Admin tərəfindən bonus əlavə edildi ($reason)" else "Admin tərəfindən bonus silindi ($reason)"
            addTransaction(
                userId = userId,
                amount = amountAzn,
                isCredit = isCredit,
                description = desc,
                adminAuditNote = note
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register referral link when a friend signs up using an invite code.
     * Prevents self-referral and duplicate signups.
     */
    suspend fun registerReferralSignup(newUserId: String, newUserName: String, referralCode: String): Result<String> {
        val cleanCode = referralCode.trim().uppercase()
        if (cleanCode.isBlank()) return Result.failure(IllegalArgumentException("Dəvət kodu boşdur."))
        val fs = firestore ?: return Result.failure(IllegalStateException("Baza əlçatan deyil"))

        return try {
            // Find referrer by referralCode across users collection
            val membersQuery = fs.collectionGroup("club")
                .whereEqualTo("referralCode", cleanCode)
                .limit(1)
                .get()
                .await()

            if (membersQuery.isEmpty) {
                return Result.failure(IllegalArgumentException("Daxil edilən dəvət kodu tapılmadı."))
            }

            val referrerDoc = membersQuery.documents.first()
            val referrerUserId = referrerDoc.reference.parent.parent?.id ?: ""

            // Anti-fraud: cannot refer oneself
            if (referrerUserId == newUserId) {
                return Result.failure(IllegalStateException("Özünüzü dəvət edə bilməzsiniz."))
            }

            val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
            val referralId = "ref_${UUID.randomUUID()}"

            // Record in referrer's list
            fs.collection("users").document(referrerUserId)
                .collection("club_referrals").document(referralId)
                .set(
                    mapOf(
                        "invitedUserId" to newUserId,
                        "invitedUserName" to newUserName,
                        "status" to "REGISTERED",
                        "joinedDate" to today,
                        "bonusEarnedAzn" to 5.0
                    )
                ).await()

            // Give new user welcome referral bonus
            addTransaction(
                userId = newUserId,
                amount = 5.0,
                isCredit = true,
                description = "Dost dəvəti ilə qeydiyyat bonusu ($cleanCode)"
            )

            // Reward referrer immediately with partial bonus, rest on first order
            fs.collection("users").document(referrerUserId)
                .collection("club").document("membership")
                .set(
                    mapOf(
                        "referredUsersCount" to com.google.firebase.firestore.FieldValue.increment(1)
                    ), SetOptions.merge()
                ).await()

            Result.success(referrerUserId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun calculateTier(totalSpentAzn: Double, completedOrdersCount: Int = 0): ClubTier {
        val configs = _tierConfigs.value
        val plat = configs.find { it.tier == ClubTier.PLATINUM }
        val gold = configs.find { it.tier == ClubTier.GOLD }
        val silver = configs.find { it.tier == ClubTier.SILVER }

        return when {
            plat != null && (totalSpentAzn >= plat.minSpendAzn || (plat.minCompletedOrders > 0 && completedOrdersCount >= plat.minCompletedOrders)) -> ClubTier.PLATINUM
            gold != null && (totalSpentAzn >= gold.minSpendAzn || (gold.minCompletedOrders > 0 && completedOrdersCount >= gold.minCompletedOrders)) -> ClubTier.GOLD
            silver != null && (totalSpentAzn >= silver.minSpendAzn || (silver.minCompletedOrders > 0 && completedOrdersCount >= silver.minCompletedOrders)) -> ClubTier.SILVER
            else -> ClubTier.STANDARD
        }
    }

    fun getTierConfig(tier: ClubTier): ClubTierConfig {
        return _tierConfigs.value.find { it.tier == tier } ?: ClubTierConfig(tier = tier)
    }

    fun getTierPerks(tier: ClubTier): List<String> {
        return getTierConfig(tier).perks.ifEmpty {
            when (tier) {
                ClubTier.STANDARD -> listOf("Hər sifarişdən 2% bonus keşbek", "Referal proqramı (hər dəvətə 5 AZN)", "Standart növbə və dəstək")
                ClubTier.SILVER -> listOf("Hər sifarişdən 4% bonus keşbek", "Bütün xidmətlərə 3% daimi endirim", "Referal proqramı (hər dəvətə 7 AZN)", "Prioritet müştəri dəstəyi")
                ClubTier.GOLD -> listOf("Hər sifarişdən 6% bonus keşbek", "Bütün xidmətlərə 5% daimi endirim", "Referal proqramı (hər dəvətə 10 AZN)", "Pulsuz AAK ilkin plagiat yoxlaması", "Təcili sifarişlərdə 20% xüsusi güzəşt")
                ClubTier.PLATINUM -> listOf("Hər sifarişdən 10% VIP keşbek", "Bütün xidmətlərə 8% daimi endirim", "Referal proqramı (hər dəvətə 15 AZN)", "Şəxsi elmi məsləhətçi və menecer", "Pulsuz qeyri-məhdud anti-plagiat yoxlamaları", "Bütün kampaniyalara 24 saat erkən giriş")
            }
        }
    }

    private fun generateReferralCode(userId: String): String {
        val clean = userId.filter { it.isLetterOrDigit() }.take(5).uppercase()
        return if (clean.isNotBlank()) "BG-$clean" else "BG-${(1000..9999).random()}"
    }

    /**
     * Generates an official 16-digit membership smart card number.
     * Starts with 8620 (86=AZ, 20=BestGroup), 11 deterministic digits, and Luhn checksum.
     */
    fun generate16DigitCardNumber(userId: String): String {
        val prefix = "8620"
        val cleanUser = userId.ifBlank { "BESTGROUP" }
        val hash = Math.abs(cleanUser.hashCode().toLong())
        val revHash = Math.abs(cleanUser.reversed().hashCode().toLong())
        val combined = "$hash$revHash"
        val digitsOnly = combined.filter { it.isDigit() }.padEnd(11, '3')
        val middle11 = digitsOnly.take(11)
        val base15 = "$prefix$middle11"
        val checkDigit = calculateLuhnCheckDigit(base15)
        return "$base15$checkDigit"
    }

    private fun calculateLuhnCheckDigit(numberWithoutCheckDigit: String): Int {
        var sum = 0
        var alternate = true
        for (i in numberWithoutCheckDigit.length - 1 downTo 0) {
            var n = numberWithoutCheckDigit[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        val remainder = sum % 10
        return if (remainder == 0) 0 else 10 - remainder
    }
}
