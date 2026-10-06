package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Language
import com.example.localization.LocalizationManager
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.ServiceCatalogItem
import com.example.model.ServiceType
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.TechBlue600
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val catalogServices by viewModel.catalogServices.collectAsState()
    val isServicesLoading by viewModel.isServicesLoading.collectAsState()
    val servicesError by viewModel.servicesError.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("all") }
    var selectedServiceForDetail by remember { mutableStateOf<ServiceCatalogItem?>(null) }

    val filteredServices = remember(catalogServices, searchQuery, currentLanguage) {
        catalogServices.filter { item ->
            val name = item.getLocalizedName(currentLanguage)
            val desc = item.getLocalizedDescription(currentLanguage)
            searchQuery.isBlank() ||
                name.contains(searchQuery, ignoreCase = true) ||
                desc.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        com.example.ui.components.BestGroupLogoBadge(size = 32)
                        Text(
                            text = localizedString(StringKey.SERVICES_TITLE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(localizedString(StringKey.SERVICE_SEARCH_HINT)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Gold500) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("services_search_field")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                isServicesLoading && catalogServices.isEmpty() -> {
                    // Loading State with Shimmer Skeleton
                    ServicesLoadingSkeleton()
                }

                servicesError != null && catalogServices.isEmpty() -> {
                    // Error State with Retry
                    ServicesErrorState(
                        errorMessage = when (currentLanguage) {
                            Language.AZ -> "Xidmətləri yükləmək mümkün olmadı. İnternet bağlantınızı yoxlayyn və yenidən cəhd edin."
                            Language.EN -> "Failed to load services. Please check your internet connection and try again."
                            Language.RU -> "Не удалось загрузить услуги. Проверьте подключение к Интернету и повторите попытку."
                        },
                        onRetry = { viewModel.reloadServices() }
                    )
                }

                filteredServices.isEmpty() -> {
                    // Empty State
                    ServicesEmptyState(currentLanguage = currentLanguage)
                }

                else -> {
                    // Services List
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("services_list")
                    ) {
                        items(filteredServices, key = { it.serviceId }) { serviceItem ->
                            ServiceCatalogRowCard(
                                item = serviceItem,
                                language = currentLanguage,
                                onDetailsClick = { selectedServiceForDetail = serviceItem },
                                onOrderClick = { viewModel.startNewOrder(preselectedCatalogService = serviceItem) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Service Detail Modal Sheet
    if (selectedServiceForDetail != null) {
        val srv = selectedServiceForDetail!!
        ModalBottomSheet(
            onDismissRequest = { selectedServiceForDetail = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Navy800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getServiceIconByName(srv.icon),
                            contentDescription = null,
                            tint = Gold500,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column {
                        Text(
                            text = srv.getLocalizedName(currentLanguage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = srv.getFormattedPrice(currentLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Gold500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = srv.getLocalizedDescription(currentLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Təhvil müddəti:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(srv.estimatedDuration.ifBlank { "7-15 gün" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Orijinallıq zəmanəti:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("100% Plagiat yoxlanışı", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Gold500)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Düzəlişlər:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Müdafiəyə qədər ödənişsiz", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val chosen = selectedServiceForDetail
                        selectedServiceForDetail = null
                        if (chosen != null) {
                            viewModel.startNewOrder(preselectedCatalogService = chosen)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500)
                ) {
                    Text(localizedString(StringKey.BTN_ORDER_NOW), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ServiceCatalogRowCard(
    item: ServiceCatalogItem,
    language: Language,
    onDetailsClick: () -> Unit,
    onOrderClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("service_card_${item.serviceId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Navy800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getServiceIconByName(item.icon),
                            contentDescription = null,
                            tint = Gold500,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = item.getLocalizedName(language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.getFormattedPrice(language),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Gold500
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.getLocalizedDescription(language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDetailsClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("service_details_${item.serviceId}")
                ) {
                    Text(localizedString(StringKey.BTN_DETAILS), style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onOrderClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("service_order_btn_${item.serviceId}")
                ) {
                    Text(
                        text = localizedString(StringKey.BTN_ORDER_NOW),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ServicesLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(4) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Gold500,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ServicesEmptyState(currentLanguage: Language) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = Gold500,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = when (currentLanguage) {
                    Language.AZ -> "Hazırda aktiv xidmət mövcud deyil."
                    Language.EN -> "No active services are currently available."
                    Language.RU -> "В настоящее время нет доступных услуг."
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ServicesErrorState(errorMessage: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(52.dp)
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Yenidən cəhd et", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun getServiceIconByName(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "school" -> Icons.Default.School
        "psychology" -> Icons.Default.Psychology
        "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
        "work" -> Icons.Default.Work
        "assignment" -> Icons.Default.Assignment
        "description" -> Icons.Default.Description
        "edit_note" -> Icons.Default.EditNote
        "slideshow" -> Icons.Default.Slideshow
        "article" -> Icons.Default.Article
        "analytics" -> Icons.Default.Analytics
        "spellcheck" -> Icons.Default.Spellcheck
        "format_shapes" -> Icons.Default.FormatShapes
        else -> Icons.Default.MoreHoriz
    }
}
