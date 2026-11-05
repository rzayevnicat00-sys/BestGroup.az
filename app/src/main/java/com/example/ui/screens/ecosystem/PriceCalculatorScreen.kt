package com.example.ui.screens.ecosystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ServiceType
import com.example.ui.theme.*
import com.example.viewmodel.AppDestination
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceCalculatorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var selectedServiceType by remember { mutableStateOf(ServiceType.DIPLOMA) }
    var pageCount by remember { mutableIntStateOf(50) }
    var selectedLanguage by remember { mutableStateOf("Azərbaycan dili") }
    var deadlineDays by remember { mutableIntStateOf(15) }
    var isUrgent by remember { mutableStateOf(false) }
    var isAakCheck by remember { mutableStateOf(true) }

    val estimate = remember(selectedServiceType, pageCount, selectedLanguage, deadlineDays, isUrgent, isAakCheck) {
        viewModel.calculateEstimate(
            serviceType = selectedServiceType,
            pageCount = pageCount,
            language = selectedLanguage,
            deadlineDays = deadlineDays,
            isUrgent = isUrgent,
            isAakCheck = isAakCheck
        )
    }

    val availableServices = listOf(
        ServiceType.DIPLOMA,
        ServiceType.MASTER,
        ServiceType.COURSE_WORK,
        ServiceType.ARTICLE,
        ServiceType.ESSAY,
        ServiceType.PRESENTATION,
        ServiceType.PROJECT_WORK,
        ServiceType.STATISTICAL_ANALYSIS
    )

    val languages = listOf("Azərbaycan dili", "İngilis dili", "Rus dili", "Türk dili")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = BirbankRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xidmət Qiymət Kalkulyatoru", fontWeight = FontWeight.Bold, color = BirbankTextPrimary, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("calculator_screen_back")) {
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
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Xidmət Növü:", fontWeight = FontWeight.Bold, color = BirbankTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableServices) { st ->
                        val isSelected = selectedServiceType == st
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BirbankRed else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedServiceType = st }
                        ) {
                            Text(
                                text = st.name.replace("_", " "),
                                color = if (isSelected) Color.White else BirbankTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Page Count
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Səhifə Sayı:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("$pageCount səhifə", color = BirbankRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Slider(
                                value = pageCount.toFloat(),
                                onValueChange = { pageCount = it.toInt() },
                                valueRange = 5f..150f,
                                steps = 28,
                                colors = SliderDefaults.colors(thumbColor = BirbankRed, activeTrackColor = BirbankRed)
                            )
                        }

                        // Deadline Days
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Təhvil Müddəti (Gün):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("$deadlineDays gün", color = BirbankRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Slider(
                                value = deadlineDays.toFloat(),
                                onValueChange = { deadlineDays = it.toInt() },
                                valueRange = 1f..45f,
                                steps = 43,
                                colors = SliderDefaults.colors(thumbColor = BirbankRed, activeTrackColor = BirbankRed)
                            )
                        }

                        // Language
                        Column {
                            Text("Tədris Dili:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                languages.forEach { lang ->
                                    val isSel = selectedLanguage == lang
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedLanguage = lang },
                                        label = { Text(lang, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Switches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Təcili İcra (Priority)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Sifariş ən qısa müddətdə prioritet növbəyə alınır", fontSize = 11.sp, color = BirbankTextSecondary)
                            }
                            Switch(
                                checked = isUrgent,
                                onCheckedChange = { isUrgent = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BirbankRed)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AAK Anti-Plagiat Yoxlaması", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Rəsmi AAK sertifikatı və unikallıq hesabatı (+35 ₼)", fontSize = 11.sp, color = BirbankTextSecondary)
                            }
                            Switch(
                                checked = isAakCheck,
                                onCheckedChange = { isAakCheck = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BirbankRed)
                            )
                        }
                    }
                }
            }

            // Estimate Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BirbankRedLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BirbankRed.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hesablama Detalları", fontWeight = FontWeight.Bold, color = BirbankRed, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Baza Qiymət ($pageCount səhifə):", style = MaterialTheme.typography.bodySmall)
                            Text("${estimate.basePriceAzn} ₼", fontWeight = FontWeight.Bold)
                        }
                        if (estimate.urgencySurchargeAzn > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Təcililik Əlavəsi:", style = MaterialTheme.typography.bodySmall)
                                Text("+${estimate.urgencySurchargeAzn} ₼", fontWeight = FontWeight.Bold, color = BirbankRed)
                            }
                        }
                        if (estimate.additionalServicesAzn > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("AAK Plagiat Sertifikatı:", style = MaterialTheme.typography.bodySmall)
                                Text("+${estimate.additionalServicesAzn} ₼", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (estimate.clubDiscountAzn > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("BestGroup Club Endirimi:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF16A34A))
                                Text("-${estimate.clubDiscountAzn} ₼", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                        }

                        HorizontalDivider(color = BirbankRed.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Ümumi Təxmini Məbləğ:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${estimate.estimatedTotalAzn} ₼", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BirbankRed)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = BirbankTextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Yekun qiymət admin və elmi rəhbər təsdiqindən sonra dəqiqləşdiriləcək.",
                                fontSize = 11.sp,
                                color = BirbankTextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.navigateTo(AppDestination.ORDER_WIZARD)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_proceed_from_calculator")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bu Parametrlərlə Sifariş Formasına Keç", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
