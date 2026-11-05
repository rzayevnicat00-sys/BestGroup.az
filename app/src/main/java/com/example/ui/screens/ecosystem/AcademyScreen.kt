package com.example.ui.screens.ecosystem

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AcademyCategory
import com.example.model.AcademyMaterial
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val materials by viewModel.academyMaterials.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedAcademyIds.collectAsState()
    val readIds by viewModel.readAcademyIds.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<AcademyCategory?>(null) }
    var viewingMaterial by remember { mutableStateOf<AcademyMaterial?>(null) }

    val filteredMaterials = remember(materials, searchQuery, selectedCategory) {
        materials.filter { mat ->
            val matchCat = selectedCategory == null || mat.category == selectedCategory
            val matchQuery = searchQuery.isBlank() ||
                    mat.title.contains(searchQuery, ignoreCase = true) ||
                    mat.subtitle.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

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
                            Icon(Icons.Default.School, contentDescription = null, tint = BirbankRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("BestGroup Akademiya", fontWeight = FontWeight.Bold, color = BirbankTextPrimary, fontSize = 17.sp)
                            Text("Elmi və Akademik Bələdçi", fontSize = 11.sp, color = BirbankTextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("academy_screen_back")) {
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Mövzu, məqalə və ya qayda axtar...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BirbankRed) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("academy_search_input")
            )

            // Categories Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Hamısı", fontSize = 11.sp) }
                    )
                }
                items(AcademyCategory.values()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.titleAz, fontSize = 11.sp) }
                    )
                }
            }

            // Material Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMaterials) { mat ->
                    val isBookmarked = bookmarkedIds.contains(mat.id)
                    val isRead = readIds.contains(mat.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewingMaterial = mat
                                viewModel.markAcademyAsRead(mat.id)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BirbankBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = BirbankRedLight,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = mat.category.titleAz,
                                        color = BirbankRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isRead) {
                                        Surface(
                                            color = Color(0xFFDCFCE7),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "✓ Oxunub",
                                                color = Color(0xFF15803D),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleAcademyBookmark(mat.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Yadda saxla",
                                            tint = if (isBookmarked) BirbankRed else BirbankTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = mat.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BirbankTextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = mat.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = BirbankTextSecondary,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = BirbankTextSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${mat.durationMinutes} dəq oxu", fontSize = 11.sp, color = BirbankTextSecondary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = BirbankTextSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${mat.viewsCount} baxış", fontSize = 11.sp, color = BirbankTextSecondary)
                                }

                                Text(
                                    text = "Məqaləni oxu →",
                                    color = BirbankRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Material Reader Dialog
    if (viewingMaterial != null) {
        val mat = viewingMaterial!!
        Dialog(
            onDismissRequest = { viewingMaterial = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Reader Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = BirbankRedLight, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = mat.category.titleAz,
                                color = BirbankRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = { viewingMaterial = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Bağla")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(mat.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BirbankTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Müəllif: ${mat.author} • ${mat.publishedAt}", fontSize = 11.sp, color = BirbankTextSecondary)
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        item {
                            Text(
                                text = mat.contentMarkdown,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BirbankTextPrimary,
                                lineHeight = 24.sp
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Oxunma qeydə alındı ✓", fontSize = 12.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Medium)
                            Button(
                                onClick = { viewingMaterial = null },
                                colors = ButtonDefaults.buttonColors(containerColor = BirbankRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Bağla")
                            }
                        }
                    }
                }
            }
        }
    }
}
