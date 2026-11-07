package com.example.ui.screens.ecosystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.ClubCampaign
import com.example.model.ClubCampaignType
import com.example.model.ClubTier
import com.example.ui.theme.*
import com.example.viewmodel.AppDestination
import com.example.viewmodel.MainViewModel

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
    val targetSpend = nextTierConfig?.minSpendAzn ?: nextTier?.minSpendAzn ?: 0.0
    val targetOrders = nextTierConfig?.minCompletedOrders ?: 0

    val spendRemaining = (targetSpend - membership.totalSpentAzn).coerceAtLeast(0.0)
    val ordersRemaining = (targetOrders - membership.completedOrdersCount).coerceAtLeast(0)

    val progressFraction = if (targetSpend > 0.0) {
        (membership.totalSpentAzn / targetSpend).toFloat().coerceIn(0f, 1f)
    } else 1.0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "BestGroup Club",
                                fontWeight = FontWeight.Bold,
                                color = BirbankTextPrimary,
                                fontSize = 17.sp
                            )
                            Text(
                                "Premium Loyallıq və Üstünlüklər",
                                fontSize = 11.sp,
                                color = BirbankTextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("club_screen_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showRulesDialog = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Qaydalar", tint = BirbankRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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

            // 1. Premium Banking Card Design by Tier
            item {
                PremiumMembershipCard(
                    tier = currentTier,
                    bonusBalance = membership.bonusBalanceAzn,
                    memberName = currentUser?.fullName?.ifBlank { "Hörmətli Müştəri" } ?: "Hörmətli Müştəri",
                    referralCode = membership.referralCode,
                    onUseBonusClick = {
                        viewModel.navigateTo(AppDestination.ORDER_WIZARD)
                    }
                )
            }

            // 2. Next Tier Progress Card
            if (nextTier != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, BirbankBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = BirbankRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Növbəti səviyyə: ${nextTier.title}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = "${(progressFraction * 100).toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    color = BirbankRed,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = BirbankRed,
                                trackColor = BirbankBorder
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val reqText = buildString {
                                append("Qalan tələb: ")
                                if (spendRemaining > 0) {
                                    append("${String.format(Locale.US, "%.1f", spendRemaining)} ₼")
                                }
                                if (ordersRemaining > 0) {
                                    if (spendRemaining > 0) append(" və ya ")
                                    append("$ordersRemaining sifariş")
                                }
                            }

                            Text(
                                text = reqText,
                                style = MaterialTheme.typography.bodySmall,
                                color = BirbankTextSecondary
                            )
                        }
                    }
                }
            }

            // 3. Referral Program & Invite Friends (WhatsApp / Telegram)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, BirbankBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = BirbankRed)
                            Text("Dostunu Dəvət Et – Bonus Qazan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            "Dəvət kodunuzu dostlarınızla paylaşın. Onlar qeydiyyatdan keçib ilk sifarişlərini tamamladıqda hər ikiniz 5–15 ₼ bonus qazanın!",
                            style = MaterialTheme.typography.bodySmall,
                            color = BirbankTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BirbankBgLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Şəxsi Dəvət Kodunuz", fontSize = 11.sp, color = BirbankTextSecondary)
                                    Text(
                                        text = membership.referralCode,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = BirbankRed,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = {
                                        val clip = ClipData.newPlainText("BestGroup Referral", membership.referralCode)
                                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                        Toast.makeText(context, "Dəvət kodu kopyalandı!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = BirbankRed)
                                    }

                                    // Direct WhatsApp Share
                                    IconButton(onClick = {
                                        val shareText = "BestGroup.az platformasında akademik və peşəkar xidmətlər üçün mənim '${membership.referralCode}' dəvət kodumu istifadə et və dərhal 5 ₼ xoş gəldin bonusu qazan! Ətraflı: https://bestgroup.az"
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
                                color = BirbankTextSecondary,
                                fontWeight = FontWeight.Medium
                            )

                            TextButton(onClick = { showEnterReferralDialog = true }) {
                                Text("Dost kodu daxil et", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BirbankRed)
                            }
                        }

                        // Referred Friends List
                        if (membership.invitedFriends.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            membership.invitedFriends.forEach { friend ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = BirbankTextSecondary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(friend.invitedUserName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Text(
                                            text = "+${String.format(Locale.US, "%.0f", friend.bonusEarnedAzn)} ₼ bonus",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Active Club Campaigns
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
                                color = BirbankTextPrimary
                            )
                        }

                        activeCampaigns.forEach { camp ->
                            ClubCampaignCard(campaign = camp)
                        }
                    }
                }
            }

            // 5. Current Tier Privileges
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, BirbankBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${currentTier.title} Səviyyəsinin Üstünlükləri",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        membership.perkList.forEach { perk ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(perk, style = MaterialTheme.typography.bodySmall, color = BirbankTextPrimary)
                            }
                        }
                    }
                }
            }

            // 6. Bonus Transactions History
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
                        color = BirbankTextPrimary
                    )
                    Text(
                        text = "1 Bonus = 1 AZN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF16A34A)
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, BirbankBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Hələ ki heç bir bonus əməliyyatı aparılmayıb.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BirbankTextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(transactions) { trans ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BirbankBorder),
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
                                        tint = if (trans.isCredit) Color(0xFF16A34A) else BirbankRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(trans.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(trans.timestamp, fontSize = 11.sp, color = BirbankTextSecondary)
                                }
                            }

                            Text(
                                text = "${if (trans.isCredit) "+" else "-"}${String.format(Locale.US, "%.2f", trans.amountAzn)} ₼",
                                fontWeight = FontWeight.Bold,
                                color = if (trans.isCredit) Color(0xFF16A34A) else BirbankRed,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
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
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed)
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
                        color = BirbankTextSecondary
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
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed),
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
}

@Composable
fun PremiumMembershipCard(
    tier: ClubTier,
    bonusBalance: Double,
    memberName: String,
    referralCode: String,
    onUseBonusClick: () -> Unit
) {
    // 4 Distinct Luxury Banking Card Gradients
    val cardBrush = when (tier) {
        ClubTier.STANDARD -> Brush.linearGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
        )
        ClubTier.SILVER -> Brush.linearGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF475569), Color(0xFF94A3B8))
        )
        ClubTier.GOLD -> Brush.linearGradient(
            colors = listOf(Color(0xFF451A03), Color(0xFF92400E), Color(0xFFD97706), Color(0xFFF59E0B))
        )
        ClubTier.PLATINUM -> Brush.linearGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF3B0764), Color(0xFF6B21A8), Color(0xFF7C3AED))
        )
    }

    val chipColor = when (tier) {
        ClubTier.GOLD -> Color(0xFFFDE68A)
        ClubTier.PLATINUM -> Color(0xFFE9D5FF)
        ClubTier.SILVER -> Color(0xFFE2E8F0)
        ClubTier.STANDARD -> Color(0xFFCBD5E1)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBrush, RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(22.dp)
                .fillMaxWidth()
        ) {
            // Header: Brand & Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Simulated EMV Chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = chipColor,
                        modifier = Modifier.size(width = 34.dp, height = 26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Nfc,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "BestGroup.az",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = tier.title.uppercase() + " MEMBER",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Bonus Balance
            Text(
                text = "Mövcud Bonus Balansı",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.2f", bonusBalance),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp
                )
                Text(
                    text = "₼ (AZN)",
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer: Member Name, Referral & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = memberName.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "KOD: $referralCode",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                FilledTonalButton(
                    onClick = onUseBonusClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White,
                        contentColor = BirbankRed
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Sifarişdə İşlət", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ClubCampaignCard(campaign: ClubCampaign) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BirbankBorder),
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
                        else -> BirbankRedLight
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = campaign.type.labelAz,
                        color = when (campaign.type) {
                            ClubCampaignType.DOUBLE_BONUS -> Color(0xFFD97706)
                            ClubCampaignType.SERVICE_DISCOUNT -> Color(0xFF15803D)
                            ClubCampaignType.BIRTHDAY_SPECIAL -> Color(0xFFBE185D)
                            else -> BirbankRed
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
                        color = BirbankTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = campaign.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BirbankTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = campaign.description,
                style = MaterialTheme.typography.bodySmall,
                color = BirbankTextSecondary
            )
        }
    }
}
