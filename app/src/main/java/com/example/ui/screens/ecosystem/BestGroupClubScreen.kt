package com.example.ui.screens.ecosystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import com.example.services.club.GoogleWalletPassService
import com.example.services.club.Barcode128Encoder
import com.example.services.club.BestGroupClubService
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClubCampaign
import com.example.model.ClubCampaignType
import com.example.model.ClubTier
import com.example.ui.theme.*
import com.example.viewmodel.AppDestination
import com.example.viewmodel.MainViewModel

private val ClubBrandNavy = Color(0xFF1B4F72)
private val ClubScreenBg = Color(0xFFF8F9FA)
private val ClubWarningBg = Color(0xFFFFFBEB)
private val ClubWarningBorder = Color(0xFFFDE68A)
private val ClubWarningText = Color(0xFF92400E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestGroupClubScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val membership by viewModel.clubMembership.collectAsState()
    val transactions by viewModel.bonusTransactions.collectAsState()
    val campaigns by viewModel.clubCampaigns.collectAsState()
    val tierConfigs by viewModel.clubTierConfigs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showRulesDialog by remember { mutableStateOf(false) }
    var showEnterReferralDialog by remember { mutableStateOf(false) }
    var enteredReferralCode by remember { mutableStateOf("") }
    var referralSubmitLoading by remember { mutableStateOf(false) }
    var selectedTierForDetails by remember { mutableStateOf<ClubTier?>(null) }
    var showWalletModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshClubData()
    }

    val currentTier = membership.tier
    val nextTier = when (currentTier) {
        ClubTier.STANDARD -> ClubTier.SILVER
        ClubTier.SILVER -> ClubTier.GOLD
        ClubTier.GOLD -> ClubTier.PLATINUM
        ClubTier.PLATINUM -> null
    }

    val nextTierConfig = nextTier?.let { nt -> tierConfigs.find { it.tier == nt } }
    val targetSpend = nextTierConfig?.minSpendAzn ?: nextTier?.minSpendAzn ?: 1500.0
    val targetOrders = nextTierConfig?.minCompletedOrders ?: 0

    val spendRemaining = (targetSpend - membership.totalSpentAzn).coerceAtLeast(0.0)
    val ordersRemaining = (targetOrders - membership.completedOrdersCount).coerceAtLeast(0)

    val progressFraction = if (targetSpend > 0.0) {
        (membership.totalSpentAzn / targetSpend).toFloat().coerceIn(0f, 1f)
    } else 1.0f

    val memberNameDisplay = currentUser?.fullName?.ifBlank { "TƏLƏBƏ ADI" } ?: "TƏLƏBƏ ADI"
    val memberJoinedDisplay = "Üzv: İyul 2026-dan"

    Scaffold(
        containerColor = ClubScreenBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BestGroup.az CLUB",
                            fontWeight = FontWeight.Bold,
                            color = ClubBrandNavy,
                            fontSize = 17.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFEBF5FB),
                            border = BorderStroke(1.dp, Color(0xFFD4E6F1))
                        ) {
                            Text(
                                text = "${currentTier.title} Üzv",
                                color = ClubBrandNavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("club_screen_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = ClubBrandNavy
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showRulesDialog = true }) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "Qaydalar",
                            tint = ClubBrandNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. Rəqəmsal Klub Kartı (Digital Membership Card)
            item {
                val effectiveCardNumber = membership.cardNumber.ifBlank {
                    BestGroupClubService.generate16DigitCardNumber(currentUser?.uid ?: "")
                }
                DigitalClubCard(
                    tier = currentTier,
                    memberName = memberNameDisplay,
                    joinedText = memberJoinedDisplay,
                    cardNumber = effectiveCardNumber,
                    referralCode = membership.referralCode,
                    progressFraction = progressFraction
                )
            }

            // 2. İrəliləyiş və Balans Məlumatı (Progress & Balance Info)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${nextTier?.title ?: "Platinum"} səviyyəsinə qaldı",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClubBrandNavy
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${String.format(Locale.US, "%.0f", membership.totalSpentAzn)} AZN / ${String.format(Locale.US, "%.0f", targetSpend)} AZN",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Amber Warning Alert Banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ClubWarningBg,
                            border = BorderStroke(1.dp, ClubWarningBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "120 gün ərzində aktiv sifariş verilməzsə, səviyyə «Bronza»ya enə bilər. Səviyyəni qorumaq və ya yüksəltmək üçün aktiv sifarişlərinizə davam edin!",
                                    fontSize = 12.sp,
                                    color = ClubWarningText,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Google Wallet / Apple Wallet Button
                        OutlinedButton(
                            onClick = { showWalletModal = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFF8FAFC),
                                contentColor = ClubBrandNavy
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Google Wallet / Apple Wallet-ə əlavə et",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 3. Cari İmtiyazlarınız (Current Tier Privileges)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "✦ Cari imtiyazlarınız",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ClubBrandNavy
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val privileges = listOf(
                            "Artırılmış keşbek Coins (Bonuslar) – ${(currentTier.bonusPercent * 100).toInt()}%",
                            "Sifarişlərdə ${(currentTier.discountPercent * 100).toInt()}% birbaşa xidmət endirimi",
                            "Çatda prioritet dəstək və sürətli cavab",
                            "Pulsuz AAK ilkin plagiat yoxlaması",
                            if (currentTier == ClubTier.GOLD || currentTier == ClubTier.PLATINUM)
                                "Fərdi VIP menecer və yeni xidmətlərə erkən giriş"
                            else
                                "Xüsusi mövsümi və ad günü bonus kampaniyaları"
                        )

                        privileges.forEach { priv ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFFE0F2FE),
                                    shape = CircleShape,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF0369A1),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = priv,
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 4. Bütün Səviyyələr (All Tiers Metallic Cards)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bütün Səviyyələr",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "İmtiyazları görmək üçün səviyyənin üzərinə toxunun",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Tier 1: Bronza
                    TierMetallicCard(
                        tierName = "Bronza",
                        subtitle = "Pulsuz qeydiyyat / Başlanğıc status",
                        gradient = Brush.horizontalGradient(
                            listOf(Color(0xFF8D4F28), Color(0xFF5A2C10))
                        ),
                        isActive = currentTier == ClubTier.STANDARD,
                        onClick = { selectedTierForDetails = ClubTier.STANDARD }
                    )

                    // Tier 2: Silver
                    TierMetallicCard(
                        tierName = "Silver",
                        subtitle = "Standart endirimlər və bonuslar",
                        gradient = Brush.horizontalGradient(
                            listOf(Color(0xFF5A6B7C), Color(0xFF384654))
                        ),
                        isActive = currentTier == ClubTier.SILVER,
                        onClick = { selectedTierForDetails = ClubTier.SILVER }
                    )

                    // Tier 3: Gold
                    TierMetallicCard(
                        tierName = "Gold",
                        subtitle = "Daha yüksək endirimlər və özəl təkliflər",
                        gradient = Brush.horizontalGradient(
                            listOf(Color(0xFFD4AF37), Color(0xFFA67C1E))
                        ),
                        isActive = currentTier == ClubTier.GOLD,
                        onClick = { selectedTierForDetails = ClubTier.GOLD }
                    )

                    // Tier 4: Platinum
                    TierMetallicCard(
                        tierName = "Platinum",
                        subtitle = "Maksimum üstünlüklər və prioritet xidmət",
                        gradient = Brush.horizontalGradient(
                            listOf(Color(0xFF2C343E), Color(0xFF141A22))
                        ),
                        isActive = currentTier == ClubTier.PLATINUM,
                        onClick = { selectedTierForDetails = ClubTier.PLATINUM }
                    )
                }
            }

            // 5. BestGroup Club Coins & Balance
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.MonetizationOn,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "BestGroup Coins (Bonus)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        "1 Bonus = 1 AZN",
                                        fontSize = 11.sp,
                                        color = Color(0xFF16A34A),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = "${String.format(Locale.US, "%.2f", membership.bonusBalanceAzn)} ₼",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = ClubBrandNavy
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Toplanan bonusları yeni sifarişlərinizdə 50%-dək ödəniş kimi istifadə edə bilərsiniz.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.navigateTo(AppDestination.ORDER_WIZARD) },
                            colors = ButtonDefaults.buttonColors(containerColor = ClubBrandNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sifarişdə bonus istifadə et", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 6. Referral & Invite Friend
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = ClubBrandNavy, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dostunu Dəvət Et – Bonus Qazan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            "Şəxsi dəvət kodunuzu dostlarınızla paylaşın. Onlar qeydiyyatdan keçib ilk sifarişi tamamladıqda hər ikiniz bonus əldə edirsiniz!",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Şəxsi Dəvət Kodunuz", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = membership.referralCode,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = ClubBrandNavy,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = {
                                        val clip = ClipData.newPlainText("BestGroup Referral", membership.referralCode)
                                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                        Toast.makeText(context, "Dəvət kodu kopyalandı!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = ClubBrandNavy)
                                    }

                                    // WhatsApp Share
                                    IconButton(onClick = {
                                        val shareText = "BestGroup.az platformasında akademik və peşəkar xidmətlər üçün mənim '${membership.referralCode}' dəvət kodumu istifadə et və dərhal xoş gəldin bonusu qazan! Ətraflı: https://bestgroup.az"
                                        try {
                                            val url = "https://api.whatsapp.com/send?text=" + Uri.encode(shareText)
                                            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(i)
                                        } catch (_: Exception) {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "WhatsApp ilə paylaş"))
                                        }
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "Paylaş", tint = Color(0xFF25D366))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dəvət etdiyiniz dostlar: ${membership.referredUsersCount} nəfər",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )

                            TextButton(onClick = { showEnterReferralDialog = true }) {
                                Text("Dost kodu daxil et", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ClubBrandNavy)
                            }
                        }
                    }
                }
            }

            // 7. Active Campaigns (if any)
            val activeCampaigns = campaigns.filter { it.isActive }
            if (activeCampaigns.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Aktiv Club Kampaniyaları",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        activeCampaigns.forEach { camp ->
                            ClubCampaignCard(campaign = camp)
                        }
                    }
                }
            }

            // 8. Bonus Transactions History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bonus Tarixçəsi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Əməliyyatlar",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Hələ ki heç bir bonus əməliyyatı aparılmayıb.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(transactions) { trans ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (trans.isCredit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (trans.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (trans.isCredit) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(trans.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(trans.timestamp, fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                            }

                            Text(
                                text = "${if (trans.isCredit) "+" else "-"}${String.format(Locale.US, "%.2f", trans.amountAzn)} ₼",
                                fontWeight = FontWeight.Bold,
                                color = if (trans.isCredit) Color(0xFF16A34A) else Color(0xFFDC2626),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }

    // Detail Bottom Sheet for any tapped Tier
    selectedTierForDetails?.let { tier ->
        val cfg = tierConfigs.find { it.tier == tier }
        val spendReq = cfg?.minSpendAzn ?: tier.minSpendAzn
        val ordersReq = cfg?.minCompletedOrders ?: 0
        val bonusPct = cfg?.bonusPercent ?: tier.bonusPercent
        val discPct = cfg?.discountPercent ?: tier.discountPercent

        ModalBottomSheet(
            onDismissRequest = { selectedTierForDetails = null },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(tier.badgeColorHex).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(tier.badgeColorHex),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${tier.title} Səviyyəsi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = if (tier == currentTier) "Hazırkı cari səviyyəniz" else "Üzvlük statusu",
                                fontSize = 12.sp,
                                color = if (tier == currentTier) Color(0xFF16A34A) else Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = { selectedTierForDetails = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Bağla")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Stats Cards (Cashback & Discount)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Keşbek Bonus", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("${(bonusPct * 100).toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ClubBrandNavy)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Xidmət Endirimi", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("${(discPct * 100).toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Keçid şərtləri:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (spendReq == 0.0) "Qeydiyyatdan keçən hər müştəriyə dərhal təqdim olunur."
                    else "Ən azı ${String.format(Locale.US, "%.0f", spendReq)} AZN tamamlanmış sifariş həcmi" +
                            if (ordersReq > 0) " və ya $ordersReq uğurlu sifariş." else ".",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Bu səviyyənin imtiyazları:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(8.dp))

                val tierPerks = cfg?.perks?.ifEmpty { null } ?: when (tier) {
                    ClubTier.STANDARD -> listOf("Bonus proqramında iştirak", "Ümumi kampaniyalar", "Şəxsi bonus balansını izləmək")
                    ClubTier.SILVER -> listOf("Standart xidmət endirimləri", "Artırılmış keşbek bonusları", "Çatda prioritet dəstək", "AAK ilkin plagiat yoxlaması")
                    ClubTier.GOLD -> listOf("Daha yüksək keşbek imkanları", "Xüsusi kampaniyalara erkən giriş", "Seçilmiş xidmətlərdə əlavə güzəştlər", "Prioritet müştəri dəstəyi")
                    ClubTier.PLATINUM -> listOf("Maksimum 10% keşbek və 8% endirim", "VIP fərdi menecer", "Eksklüziv kampaniyalara ilk giriş", "Premium ekspres xidmət")
                }

                tierPerks.forEach { p ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(p, fontSize = 12.sp, color = Color(0xFF334155))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { selectedTierForDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ClubBrandNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bağla", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Rules & Conditions Dialog
    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFD97706))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BestGroup Club Qaydaları", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "• 1 Bonus Xalı = 1 AZN pul ekvivalenti daşıyır.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Bonuslar yalnız tamamlanmış və təhvil verilmiş sifarişlərdən sonra hesablanır.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Növbəti sifarişlərinizdə məbləğin 50%-dək hissəsini bonuslarla ödəyə bilərsiniz.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Səviyyə yüksəldikcə keşbek faizi 2%-dən 10%-dək, xidmət endirimləri isə 8%-dək artır.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Öz-özünü dəvət etmə və saxta referral cəhdləri server tərəfində avtomatik ləğv edilir.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRulesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ClubBrandNavy)
                ) {
                    Text("Oldu", color = Color.White)
                }
            }
        )
    }

    // Enter Referral Code Dialog
    if (showEnterReferralDialog) {
        AlertDialog(
            onDismissRequest = { if (!referralSubmitLoading) showEnterReferralDialog = false },
            title = { Text("Dəvət Kodu Daxil Et", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Dostunuzun verdiyi dəvət kodunu yazın və dərhal 5 ₼ xoş gəldin bonusu əldə edin:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = enteredReferralCode,
                        onValueChange = { enteredReferralCode = it.uppercase() },
                        label = { Text("Dəvət Kodu (məs: BG-XXXX)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredReferralCode.isNotBlank()) {
                            referralSubmitLoading = true
                            viewModel.applyReferralCode(
                                code = enteredReferralCode.trim(),
                                onSuccess = {
                                    referralSubmitLoading = false
                                    showEnterReferralDialog = false
                                    Toast.makeText(context, "Təbriklər! 5 ₼ bonus balansınıza əlavə edildi.", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    referralSubmitLoading = false
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClubBrandNavy),
                    enabled = !referralSubmitLoading && enteredReferralCode.isNotBlank()
                ) {
                    if (referralSubmitLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Təsdiq et", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnterReferralDialog = false }, enabled = !referralSubmitLoading) {
                    Text("Ləğv et")
                }
            }
        )
    }

    if (showWalletModal) {
        val effectiveCardNumber = membership.cardNumber.ifBlank {
            BestGroupClubService.generate16DigitCardNumber(currentUser?.uid ?: "")
        }
        GoogleWalletModalBottomSheet(
            userName = memberNameDisplay,
            referralCode = membership.referralCode,
            cardNumber = effectiveCardNumber,
            tier = currentTier,
            bonusBalanceAzn = membership.bonusBalanceAzn,
            totalSpentAzn = membership.totalSpentAzn,
            onDismiss = { showWalletModal = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleWalletModalBottomSheet(
    userName: String,
    referralCode: String,
    cardNumber: String,
    tier: ClubTier,
    bonusBalanceAzn: Double,
    totalSpentAzn: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isInstalled = remember { GoogleWalletPassService.isGoogleWalletInstalled(context) }
    val cleanCardNumber = cardNumber.filter { it.isDigit() }.let {
        if (it.length == 16) it else it.padEnd(16, '7')
    }.ifEmpty { "8620202611100000" }
    val formattedCardNumber = Barcode128Encoder.format16DigitCardNumber(cleanCardNumber)
    val cleanRef = if (referralCode.startsWith("BG-", ignoreCase = true)) referralCode else "BG-${referralCode.ifBlank { "2026" }}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEBF5FB),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = ClubBrandNavy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Google Wallet Rəqəmsal Kartı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "BestGroup VIP Loyallıq Kartı",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Bağla")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wallet App Status Indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isInstalled) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, if (isInstalled) Color(0xFF86EFAC) else Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isInstalled) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isInstalled) Color(0xFF16A34A) else Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isInstalled) "Google Wallet tətbiqi cihazınızda mövcuddur"
                               else "Google Wallet quraşdırılmayıb. Kart veb cüzdanda açılacaq və ya Play Store-dan quraşdırıla bilər.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isInstalled) Color(0xFF15803D) else Color(0xFF92400E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Digital Pass Preview Card (Google Wallet Pass Style)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ClubBrandNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Pass Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BESTGROUP CLUB",
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.5.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${tier.title} Üzv",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = formattedCardNumber,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.6.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${userName.uppercase()}  •  Dəvət: $cleanRef",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mövcud Coins", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(
                                "${String.format(Locale.US, "%.2f", bonusBalanceAzn)} ₼",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Status", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("Daimi Aktiv", color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // White Barcode Surface for scanning with Real Code 128
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ClubBarcodeCanvas(
                                code = cleanCardNumber,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formattedCardNumber,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Instructions on how to add loyalty pass to Google Wallet
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = ClubBrandNavy,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Google Wallet-ə Kart Əlavə Etmə Təlimatı:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. 'Kartı Qalereyaya Saxla' düyməsinə toxunaraq rəsmi VIP kartınızı və barkodunuzu telefonun yaddaşına yazın.\n2. 'Google Wallet-i Aç' düyməsinə basıb, Google Wallet tətbiqində '+' (Əlavə et) -> 'Şəkil/Sənəd və ya Loyallıq' seçərək kartı dərhal cüzdanınıza daxil edin.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var cardSavedToGallery by remember { mutableStateOf(false) }

            // Action 1: Save Digital Card to Device Gallery (PNG with Barcode)
            Button(
                onClick = {
                    val result = GoogleWalletPassService.savePassCardToGallery(
                        context = context,
                        userName = userName,
                        referralCode = referralCode,
                        cardNumber = cleanCardNumber,
                        tierTitle = tier.title,
                        bonusBalanceAzn = bonusBalanceAzn,
                        totalSpentAzn = totalSpentAzn
                    )
                    if (result.isSuccess) {
                        cardSavedToGallery = true
                        Toast.makeText(
                            context,
                            "✅ Rəqəmsal VIP kart Qalereyaya saxlanıldı! İndi Google Wallet-də '+' basaraq əlavə edə bilərsiniz.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Xəta: ${result.exceptionOrNull()?.message ?: "Saxlanıla bilmədi"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (cardSavedToGallery) Color(0xFF16A34A) else Color(0xFF1B4F72)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    if (cardSavedToGallery) Icons.Default.CheckCircle else Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (cardSavedToGallery) "Kart Qalereyaya Saxlanıldı (Hazırdır)" else "1. Rəqəmsal Kartı Qalereyaya Saxla (PNG)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action 2: Open Real Google Wallet App
            OutlinedButton(
                onClick = {
                    val opened = GoogleWalletPassService.openGoogleWalletApp(context)
                    if (opened) {
                        Toast.makeText(
                            context,
                            "Google Wallet açılır. '+' düyməsinə toxunub kartı əlavə edin.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(context, "Google Wallet açıla bilmədi.", Toast.LENGTH_SHORT).show()
                    }
                },
                border = BorderStroke(1.5.dp, Color(0xFF1B4F72)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color(0xFF1B4F72),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "2. Google Wallet Tətbiqini Aç (+ İlə Əlavə Et)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1B4F72)
                )
            }

            // If not installed, provide direct Play Store install button
            if (!isInstalled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        GoogleWalletPassService.openGoogleWalletOnPlayStore(context)
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        Icons.Default.InstallMobile,
                        contentDescription = null,
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Google Wallet Tətbiqini Play Store-dan Quraşdır",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action 3: Share Pass Image & Details
            TextButton(
                onClick = {
                    GoogleWalletPassService.sharePassCard(
                        context = context,
                        userName = userName,
                        referralCode = referralCode,
                        cardNumber = cleanCardNumber,
                        tierTitle = tier.title,
                        bonusBalanceAzn = bonusBalanceAzn,
                        totalSpentAzn = totalSpentAzn
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Kart Şəklini və Məlumatlarını Paylaş",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Draws a real, scannable ISO/IEC 15417 Code 128 barcode on Canvas.
 */
@Composable
fun ClubBarcodeCanvas(
    code: String,
    modifier: Modifier = Modifier
) {
    val modules = remember(code) {
        Barcode128Encoder.encodeCode128C(code, quietZoneModules = 8)
    }

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val barHeight = size.height
        val moduleWidth = totalWidth / modules.size.toFloat()

        for (i in modules.indices) {
            if (modules[i]) {
                val x = i * moduleWidth
                drawRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(moduleWidth, barHeight)
                )
            }
        }
    }
}

/**
 * Exact replica of the Digital Club Card from React code and reference screenshots.
 */
@Composable
fun DigitalClubCard(
    tier: ClubTier,
    memberName: String,
    joinedText: String,
    cardNumber: String,
    referralCode: String,
    progressFraction: Float
) {
    val cleanCardNumber = cardNumber.filter { it.isDigit() }.let {
        if (it.length == 16) it else it.padEnd(16, '7')
    }.ifEmpty { "8620202611100000" }
    val formattedCardNumber = Barcode128Encoder.format16DigitCardNumber(cleanCardNumber)
    val cleanRef = if (referralCode.startsWith("BG-", ignoreCase = true)) referralCode else "BG-${referralCode.ifBlank { "2026" }}"

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ClubBrandNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .shadow(10.dp, RoundedCornerShape(26.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Brand & Tier Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BESTGROUP CLUB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.8.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = tier.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Middle: 16-Digit Card Number, Name, Joined Info & Referral Code
            Column {
                Text(
                    text = formattedCardNumber,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = memberName.uppercase(),
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$joinedText  •  Dəvət: $cleanRef",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }

            // Bottom Progress Bar inside card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

/**
 * 4 Metallic gradient cards matching the user's React code and reference screenshots.
 */
@Composable
fun TierMetallicCard(
    tierName: String,
    subtitle: String,
    gradient: Brush,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = if (isActive) BorderStroke(2.dp, Color.White) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 6.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(gradient)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tierName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )

                    if (isActive) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF1E293B),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Cari səviyyə",
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Ətraflı",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ClubCampaignCard(campaign: ClubCampaign) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (campaign.type) {
                        ClubCampaignType.DOUBLE_BONUS -> Color(0xFFFEF3C7)
                        ClubCampaignType.SERVICE_DISCOUNT -> Color(0xFFDCFCE7)
                        ClubCampaignType.BIRTHDAY_SPECIAL -> Color(0xFFFCE7F3)
                        else -> Color(0xFFEFF6FF)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = campaign.type.labelAz,
                        color = when (campaign.type) {
                            ClubCampaignType.DOUBLE_BONUS -> Color(0xFFD97706)
                            ClubCampaignType.SERVICE_DISCOUNT -> Color(0xFF15803D)
                            ClubCampaignType.BIRTHDAY_SPECIAL -> Color(0xFFBE185D)
                            else -> ClubBrandNavy
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (campaign.endDate.isNotBlank()) {
                    Text(
                        text = "Bitmə: ${campaign.endDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = campaign.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = campaign.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
        }
    }
}
