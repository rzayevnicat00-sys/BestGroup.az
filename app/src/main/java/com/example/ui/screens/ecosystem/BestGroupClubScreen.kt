package com.example.ui.screens.ecosystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClubTier
import com.example.ui.theme.*
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

    val currentTier = membership.tier
    val nextTier = when (currentTier) {
        ClubTier.STANDARD -> ClubTier.SILVER
        ClubTier.SILVER -> ClubTier.GOLD
        ClubTier.GOLD -> ClubTier.PLATINUM
        ClubTier.PLATINUM -> null
    }

    val progressFraction = if (nextTier != null) {
        (membership.totalSpentAzn / nextTier.minSpendAzn).toFloat().coerceIn(0f, 1f)
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
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("BestGroup Club", fontWeight = FontWeight.Bold, color = BirbankTextPrimary, fontSize = 17.sp)
                            Text("Loyallıq və Bonus Sistemi", fontSize = 11.sp, color = BirbankTextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("club_screen_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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

            // VIP Membership Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("BestGroup Club Kartı", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentTier.title.uppercase(),
                                    color = Color(currentTier.badgeColorHex),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }

                            Surface(
                                color = Color(currentTier.badgeColorHex).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${(currentTier.bonusPercent * 100).toInt()}% Keşbek",
                                    color = Color(currentTier.badgeColorHex),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text("Bonus Balansı:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.2f", membership.bonusBalanceAzn)} ₼",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (nextTier != null) {
                            val diff = (nextTier.minSpendAzn - membership.totalSpentAzn).coerceAtLeast(0.0)
                            Text(
                                text = "Növbəti səviyyə: ${nextTier.title} (Daha ${diff.toInt()} ₼ sifariş edin)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(nextTier.badgeColorHex),
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        } else {
                            Text("Təbriklər! Siz ən yüksək Platinum səviyyəsindəsiniz.", color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Referral Code Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dostunu Dəvət Et və Qazan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Dəvət kodunuzla qeydiyyatdan keçən hər tələbə dostunuz üçün hər ikiniz 5 ₼ bonus qazanırsınız!",
                            style = MaterialTheme.typography.bodySmall,
                            color = BirbankTextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BirbankRedLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Şəxsi Dəvət Kodunuz:", fontSize = 11.sp, color = BirbankTextSecondary)
                                    Text(
                                        text = membership.referralCode,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = BirbankRed
                                    )
                                }

                                Row {
                                    IconButton(onClick = {
                                        val clip = ClipData.newPlainText("Referral", membership.referralCode)
                                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                        Toast.makeText(context, "Kod kopyalandı!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = BirbankRed)
                                    }

                                    IconButton(onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "BestGroup.az tətbiqinə qoşul və mənim '${membership.referralCode}' kodumu istifadə edərək 5 ₼ xoş gəldin bonusu qazan!")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Dəvət kodunu paylaş"))
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "Paylaş", tint = BirbankRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Current Tier Perks List
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${currentTier.title} Səviyyəsinin Üstünlükləri", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        membership.perkList.forEach { perk ->
                            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(perk, style = MaterialTheme.typography.bodySmall, color = BirbankTextPrimary)
                            }
                        }
                    }
                }
            }

            // Bonus History Title
            item {
                Text(
                    text = "Bonus Tarixçəsi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BirbankTextPrimary
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "Hələ ki heç bir bonus əməliyyatı aparılmayıb.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BirbankTextSecondary
                    )
                }
            } else {
                items(transactions) { trans ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trans.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(trans.timestamp, fontSize = 11.sp, color = BirbankTextSecondary)
                            }

                            Text(
                                text = "${if (trans.isCredit) "+" else "-"}${String.format(java.util.Locale.US, "%.2f", trans.amountAzn)} ₼",
                                fontWeight = FontWeight.Bold,
                                color = if (trans.isCredit) Color(0xFF16A34A) else BirbankRed,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
