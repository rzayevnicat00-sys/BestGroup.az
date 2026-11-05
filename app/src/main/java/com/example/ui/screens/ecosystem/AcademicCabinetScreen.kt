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
import com.example.model.AcademicNote
import com.example.model.OrderStatus
import com.example.ui.theme.*
import com.example.viewmodel.AppDestination
import com.example.viewmodel.MainViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicCabinetScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allOrders by viewModel.orders.collectAsState()
    val myOrders = remember(allOrders, currentUser) {
        val uid = currentUser?.id ?: ""
        allOrders.filter { it.userId == uid }
    }
    val activeOrders = remember(myOrders) { myOrders.filter { it.status != OrderStatus.READY && it.status != OrderStatus.CANCELLED } }
    val completedOrders = remember(myOrders) { myOrders.filter { it.status == OrderStatus.READY } }

    var userNotes by remember {
        mutableStateOf(
            listOf(
                AcademicNote(
                    id = "note_1",
                    title = "Diplom işi elmi rəhbər qeydləri",
                    content = "Fəsil 2-də 2020-2024-cü illərin statistik cədvəlləri yenilənməlidir. Mərkəzi Bankın illik hesabatına istinad verilməlidir."
                ),
                AcademicNote(
                    id = "note_2",
                    title = "Dissertasiya müdafiəsi üçün sənədlər",
                    content = "Kafedranın ilkin müzakirə protokolu, 2 rəyçi rəyi, AAK plagiat arayışı və 3 elmi məqalənin çıxarışı."
                )
            )
        )
    }

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var newNoteTitle by remember { mutableStateOf("") }
    var newNoteContent by remember { mutableStateOf("") }

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
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Şəxsi Akademik Kabinet", fontWeight = FontWeight.Bold, color = BirbankTextPrimary, fontSize = 17.sp)
                            Text(currentUser?.fullName ?: "Tələbə / Tədqiqatçı", fontSize = 11.sp, color = BirbankTextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("academic_cabinet_back")) {
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

            // Overview Metric Cards
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Aktiv İşlər", fontSize = 11.sp, color = BirbankTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${activeOrders.size}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BirbankRed)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Tamamlanan", fontSize = 11.sp, color = BirbankTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${completedOrders.size}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF16A34A))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Qeydlərim", fontSize = 11.sp, color = BirbankTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${userNotes.size}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BirbankTextPrimary)
                        }
                    }
                }
            }

            // Quick Shortcut Actions
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.navigateTo(AppDestination.ACADEMIC_TOOLS) },
                        colors = ButtonDefaults.buttonColors(containerColor = BirbankRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Alətlər", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppDestination.AI_DOC_ASSISTANT) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sənəd Analizi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BirbankRed)
                    }
                }
            }

            // Active Orders in Progress
            item {
                Text("Aktiv Tədqiqat Sifarişləri:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BirbankTextPrimary)
            }

            if (activeOrders.isEmpty()) {
                item {
                    Text("Hazırda aktiv sifarişiniz yoxdur.", style = MaterialTheme.typography.bodySmall, color = BirbankTextSecondary)
                }
            } else {
                items(activeOrders) { ord ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.viewOrderDetail(ord) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ord.orderNumber, fontWeight = FontWeight.Bold, color = BirbankRed, fontSize = 13.sp)
                                Surface(color = BirbankRedLight, shape = RoundedCornerShape(6.dp)) {
                                    Text(ord.currentStage.titleAz, color = BirbankRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ord.serviceName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(ord.topic, style = MaterialTheme.typography.bodySmall, color = BirbankTextSecondary)
                        }
                    }
                }
            }

            // Academic Notes Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Şəxsi Elmi Qeydlər:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BirbankTextPrimary)
                    TextButton(onClick = { showAddNoteDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Yeni Qeyd", color = BirbankRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            items(userNotes) { note ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(note.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BirbankTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(note.content, style = MaterialTheme.typography.bodySmall, color = BirbankTextSecondary, lineHeight = 18.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Yeni Akademik Qeyd") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newNoteTitle,
                        onValueChange = { newNoteTitle = it },
                        label = { Text("Başlıq") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newNoteContent,
                        onValueChange = { newNoteContent = it },
                        label = { Text("Qeyd mətni") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNoteTitle.isNotBlank()) {
                            userNotes = userNotes + AcademicNote(
                                id = UUID.randomUUID().toString(),
                                title = newNoteTitle.trim(),
                                content = newNoteContent.trim()
                            )
                            newNoteTitle = ""
                            newNoteContent = ""
                            showAddNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirbankRed)
                ) {
                    Text("Əlavə et")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Ləğv et")
                }
            }
        )
    }
}
