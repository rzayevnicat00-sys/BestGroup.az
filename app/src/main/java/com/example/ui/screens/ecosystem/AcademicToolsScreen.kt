package com.example.ui.screens.ecosystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CitationFormat
import com.example.model.CitationInput
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicToolsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val academicService = remember { viewModel.repository.academicToolsService }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mətn Sayğacı", "İstinad Generatoru", "Təqdimat Planı", "AAK Qaydaları")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Akademik Alətlər", fontWeight = FontWeight.Bold, color = BirbankTextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("academic_tools_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BirbankRed,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) BirbankRed else BirbankTextSecondary
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> WordCountToolTab(academicService = academicService)
                1 -> CitationGeneratorTab(context = context, academicService = academicService)
                2 -> PresentationOutlineTab(academicService = academicService)
                3 -> AakChecklistTab(academicService = academicService)
            }
        }
    }
}

@Composable
fun WordCountToolTab(academicService: com.example.services.academic.AcademicToolsService) {
    var textInput by remember { mutableStateOf("") }
    val analysis = remember(textInput) { academicService.analyzeText(textInput) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Mətninizi daxil edin və ya yapışdırın:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BirbankTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Məqalə, fəsil və ya tezisin mətnini bura yapışdırın...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("word_counter_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Text(
                text = "Statistik Göstəricilər",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BirbankTextPrimary
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(title = "Söz Sayı", value = "${analysis.wordCount}", modifier = Modifier.weight(1f))
                StatCard(title = "Standart Səhifə", value = "${analysis.estimatedStandardPages}", modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(title = "Simvol (boşluqla)", value = "${analysis.characterCountWithSpaces}", modifier = Modifier.weight(1f))
                StatCard(title = "Simvol (boşluqsuz)", value = "${analysis.characterCountNoSpaces}", modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(title = "Cümlə Sayı", value = "${analysis.sentenceCount}", modifier = Modifier.weight(1f))
                StatCard(title = "Oxunma Müddəti", value = "~${analysis.estimatedReadingTimeMinutes} dəq", modifier = Modifier.weight(1f))
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Mətnin Mürəkkəblik Dərəcəsi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = analysis.readabilityScore,
                        color = BirbankRed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = BirbankTextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BirbankRed)
        }
    }
}

@Composable
fun CitationGeneratorTab(
    context: Context,
    academicService: com.example.services.academic.AcademicToolsService
) {
    var author by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var urlOrDoi by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(CitationFormat.APA_7TH) }

    val resultCitation = remember(author, title, publisher, year, city, urlOrDoi, selectedFormat) {
        academicService.generateCitation(
            CitationInput(author, title, publisher, year, city, urlOrDoi),
            selectedFormat
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("İstinad Formatı:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                CitationFormat.values().forEach { fmt ->
                    FilterChip(
                        selected = selectedFormat == fmt,
                        onClick = { selectedFormat = fmt },
                        label = { Text(fmt.name.replace("_", " "), fontSize = 11.sp) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Müəllif (Soyad, Adın baş hərfi)") },
                placeholder = { Text("Məs: Məmmədov, E.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Kitab və ya Məqalənin Adı") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("İl") },
                    placeholder = { Text("2024") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Şəhər") },
                    placeholder = { Text("Bakı") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        item {
            OutlinedTextField(
                value = publisher,
                onValueChange = { publisher = it },
                label = { Text("Nəşriyyat / Jurnal") },
                placeholder = { Text("Elm və Təhsil") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = urlOrDoi,
                onValueChange = { urlOrDoi = it },
                label = { Text("DOI və ya URL (İstəyə bağlı)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hazır Biblioqrafik Qeyd:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        IconButton(onClick = {
                            val clip = ClipData.newPlainText("Citation", resultCitation)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, "İstinad kopyalandı!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = BirbankRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = resultCitation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BirbankTextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PresentationOutlineTab(academicService: com.example.services.academic.AcademicToolsService) {
    var topic by remember { mutableStateOf("") }
    var generatedSlides by remember { mutableStateOf<List<Pair<String, List<String>>>>(emptyList()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Tədqiqat Mövzunuzu Qeyd Edin:",
                fontWeight = FontWeight.Bold,
                color = BirbankTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                placeholder = { Text("Məs: Rəqəmsal bankçılıqda risklərin idarə edilməsi") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    generatedSlides = academicService.generatePresentationOutline(topic)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BirbankRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Slideshow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("8 Slaydlıq Təqdimat Planı Yarat", fontWeight = FontWeight.Bold)
            }
        }

        if (generatedSlides.isNotEmpty()) {
            items(generatedSlides) { (slideTitle, points) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(slideTitle, fontWeight = FontWeight.Bold, color = BirbankRed, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        points.forEach { pt ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", color = BirbankRed, fontWeight = FontWeight.Bold)
                                Text(pt, style = MaterialTheme.typography.bodySmall, color = BirbankTextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AakChecklistTab(academicService: com.example.services.academic.AcademicToolsService) {
    val checklist = remember { academicService.getAcademicStructureChecklist() }
    val checkedState = remember { mutableStateMapOf<Int, Boolean>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "AAK Struktur və Format Tələbləri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BirbankTextPrimary
            )
            Text(
                text = "Diplom və dissertasiya işinizi müdafiəyə təqdim etməzdən əvvəl aşağıdakı bəndləri yoxlayın:",
                style = MaterialTheme.typography.bodySmall,
                color = BirbankTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(checklist.indices.toList()) { index ->
            val (title, desc) = checklist[index]
            val isChecked = checkedState[index] == true
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isChecked) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isChecked) Color(0xFF16A34A) else BirbankBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { checkedState[index] = !isChecked }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checkedState[index] = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF16A34A))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isChecked) Color(0xFF15803D) else BirbankTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = BirbankTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
