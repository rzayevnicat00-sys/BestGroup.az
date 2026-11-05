package com.example.ui.screens.ecosystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiDocumentReport
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDocAssistantScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var documentTitle by remember { mutableStateOf("") }
    var documentContent by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<AiDocumentReport?>(null) }
    val acceptedFindings = remember { mutableStateMapOf<Int, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BirbankRedLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI Sənəd Analizçisi", fontWeight = FontWeight.Bold, color = BirbankTextPrimary, fontSize = 17.sp)
                            Text("Qrammatika, Struktur və AAK Yoxlaması", fontSize = 11.sp, color = BirbankTextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ai_doc_screen_back")) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Text("Sənəd Başlığı və Mətni:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = documentTitle,
                    onValueChange = { documentTitle = it },
                    placeholder = { Text("Məs: Fəsil 1 - Nəzəri əsaslar", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = documentContent,
                    onValueChange = { documentContent = it },
                    placeholder = { Text("Analiz etmək istədiyiniz elmi mətni və ya paraqrafları bura yapışdırın...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (documentContent.isNotBlank()) {
                            isAnalyzing = true
                            report = viewModel.analyzeDocument(documentTitle, documentContent)
                            isAnalyzing = false
                        }
                    },
                    enabled = documentContent.isNotBlank() && !isAnalyzing,
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_run_ai_doc_analysis")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI ilə Sənədi Analiz Et", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (report != null) {
                val rep = report!!

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(6.dp))

                    // Score Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BirbankRedLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BirbankRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Akademik Uyğunluq:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BirbankRed)
                                Text("${rep.overallScorePercent}%", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = BirbankRed)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Struktur: ${rep.structureAssessment}", fontSize = 12.sp, color = BirbankTextPrimary)
                            Text("İstinadlar: ${rep.citationAssessment}", fontSize = 12.sp, color = BirbankTextPrimary)
                        }
                    }
                }

                item {
                    Text(
                        text = "Aşkar Edilən Təkliflər və Düzəlişlər (${rep.findings.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BirbankTextPrimary
                    )
                }

                items(rep.findings.indices.toList()) { index ->
                    val finding = rep.findings[index]
                    val isAccepted = acceptedFindings[index] == true

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isAccepted) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAccepted) Color(0xFF16A34A) else BirbankBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isAccepted) Color(0xFFDCFCE7) else BirbankRedLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = finding.category,
                                        color = if (isAccepted) Color(0xFF15803D) else BirbankRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (isAccepted) {
                                    Text("✓ Təklif qəbul edildi", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Mövcud: \"${finding.originalSnippet}\"", style = MaterialTheme.typography.bodySmall, color = BirbankTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Təklif: \"${finding.suggestedSnippet}\"", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BirbankTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Səbəb: ${finding.explanation}", style = MaterialTheme.typography.labelSmall, color = BirbankTextSecondary)

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { acceptedFindings[index] = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isAccepted) Color(0xFF15803D) else BirbankRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isAccepted) "Qəbul olunub" else "Qəbul et", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { acceptedFindings[index] = false },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Rədd et", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // General Advice
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Ümumi Elmi Redaksiya Tövsiyələri:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            rep.generalAdvice.forEach { adv ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("• ", color = BirbankRed, fontWeight = FontWeight.Bold)
                                    Text(adv, style = MaterialTheme.typography.bodySmall, color = BirbankTextPrimary)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
