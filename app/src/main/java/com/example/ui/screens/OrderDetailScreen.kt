package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.FileUploadState
import com.example.model.Order
import com.example.model.OrderAttachedFile
import com.example.model.OrderStatus
import com.example.model.UserRole
import kotlinx.coroutines.launch
import com.example.ui.components.InteractiveRatingBar
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.AppDestination
import com.example.viewmodel.BottomTab
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    order: Order,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isStaff = currentUser?.role in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.OPERATOR) ||
                  com.example.services.firebase.FirebaseAuthService.isSuperAdminEmail(currentUser?.email)
    val conversations by viewModel.conversations.collectAsState()
    val orderConv = conversations.find { it.orderId == order.id || it.id == "order_${order.id}" }
    var showReviewDialog by remember { mutableStateOf(false) }
    var ratingStars by remember { mutableStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    // Admin status edit sheet & cancel reason dialog
    var showAdminStatusSheet by remember { mutableStateOf(false) }
    var showCancelReasonDialog by remember { mutableStateOf(false) }
    var cancelReasonText by remember { mutableStateOf("") }
    var fileToDelete by remember { mutableStateOf<OrderAttachedFile?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            uris.forEach { uri ->
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                var name = "material_${System.currentTimeMillis()}"
                var size = 0L
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx != -1) name = it.getString(nameIdx)
                        if (sizeIdx != -1) size = it.getLong(sizeIdx)
                    }
                }
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                viewModel.adminAttachFileToOrder(
                    orderId = order.id,
                    uriString = uri.toString(),
                    fileName = name,
                    sizeBytes = size,
                    mimeType = mime,
                    onSuccess = {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Fayl uğurla əlavə edildi: $name") }
                    },
                    onError = { err ->
                        coroutineScope.launch { snackbarHostState.showSnackbar("Xəta: $err") }
                    }
                )
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${localizedString(StringKey.ORDER_NUMBER)} ${order.orderNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppDestination.MAIN) },
                        modifier = Modifier.testTag("order_detail_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (isStaff) {
                        IconButton(onClick = { showAdminStatusSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Statusu dəyiş", tint = Gold500)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Chat with Curator action
                    Button(
                        onClick = {
                            viewModel.openOrderChat(order)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("order_detail_chat_btn")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(localizedString(StringKey.ORDER_CHAT_ACTION), fontWeight = FontWeight.Bold)
                    }

                    // Rating button if completed
                    if (order.status == OrderStatus.READY) {
                        OutlinedButton(
                            onClick = { showReviewDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("order_detail_rate_btn")
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Gold500, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(localizedString(StringKey.ORDER_RATE_ACTION), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Topic & Status Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = localizedString(order.serviceType.nameKey),
                                style = MaterialTheme.typography.labelMedium,
                                color = Gold500,
                                fontWeight = FontWeight.Bold
                            )
                            StatusBadge(status = order.status)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = order.topic,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = Gold500, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "${localizedString(StringKey.ORDER_DEADLINE)}: ${order.deadline}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            PriorityBadge(priority = order.priority)
                        }
                    }
                }
            }

            // 2. Order Consultation & Curator Chat Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.openOrderChat(order) }
                        .border(1.dp, Gold500.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .testTag("order_chat_section_card"),
                    colors = CardDefaults.cardColors(containerColor = Navy800)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Gold500.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = Gold500,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = localizedString(StringKey.ORDER_CHAT_SECTION_TITLE),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${order.orderNumber} • ${orderConv?.curatorName ?: "Akademik Kurator"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gold500
                                    )
                                }
                            }

                            if ((orderConv?.unreadCount ?: 0) > 0) {
                                Badge(
                                    containerColor = Gold500,
                                    contentColor = Navy900
                                ) {
                                    Text("${orderConv?.unreadCount}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = orderConv?.lastMessage?.takeIf { it.isNotBlank() }
                                ?: "Sifariş üzrə suallarınızı, metodik tələblərinizi və ya əlavələrinizi birbaşa kuratorunuza yaza bilərsiniz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.openOrderChat(order) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("order_detail_open_chat_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold500,
                                contentColor = Navy900
                            )
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = localizedString(StringKey.ORDER_CHAT_OPEN_BUTTON),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 3. Interactive Status Timeline
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = localizedString(StringKey.ORDER_TIMELINE_TITLE),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        order.statusHistory.forEachIndexed { index, step ->
                            val isLast = index == order.statusHistory.size - 1
                            TimelineStepItem(step = step, isLast = isLast)
                        }
                    }
                }
            }

            // 3. Academic Details & Specifications
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Akademik Məlumatlar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailRow("Universitet", order.university)
                        DetailRow("Fakültə", order.faculty)
                        DetailRow("Təhsil pilləsi", order.academicLevel)
                        DetailRow("İşin dili", order.language)
                        DetailRow("Səhifə həcmi", "${order.pageCount} səhifə")
                        DetailRow("Standart", order.formattingStandard)
                        DetailRow("Qiymətləndirmə", "${order.estimatedPriceAzn} AZN")
                    }
                }
            }

            // 4. Attached Files Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${localizedString(StringKey.ORDER_FILES_TITLE)} (${order.files.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isStaff) {
                                Button(
                                    onClick = {
                                        filePickerLauncher.launch(
                                            arrayOf(
                                                "application/pdf",
                                                "application/msword",
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                "application/vnd.ms-powerpoint",
                                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                                "image/*",
                                                "video/mp4",
                                                "*/*"
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("admin_attach_file_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fayl əlavə et", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (viewModel.isAttachingFile.value) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val pct = (viewModel.attachFileProgress.value * 100).toInt()
                            LinearProgressIndicator(
                                progress = { viewModel.attachFileProgress.value },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = Gold500
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (pct > 0) "Fayl yüklənir... $pct%" else "Fayl Firebase Storage-a yüklənir...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold500,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (order.files.isEmpty()) {
                            Text(
                                text = localizedString(StringKey.FILE_EMPTY_TITLE),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            order.files.forEach { file ->
                                val isUploadedByAdmin = file.uploadedBy.equals("Admin", ignoreCase = true) ||
                                        file.name.contains("admin", ignoreCase = true) ||
                                        file.name.contains("material", ignoreCase = true)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        file.extension.equals("mp4", ignoreCase = true) -> Icons.Default.VideoFile
                                                        file.extension.equals("pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
                                                        listOf("jpg", "jpeg", "png").contains(file.extension.lowercase()) -> Icons.Default.Image
                                                        else -> Icons.Default.InsertDriveFile
                                                    },
                                                    contentDescription = null,
                                                    tint = Gold500
                                                )
                                                Column {
                                                    Text(
                                                        text = file.originalFileName.ifBlank { file.name },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text(
                                                            text = file.sizeFormatted,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (isUploadedByAdmin) Gold500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                                        ) {
                                                            Text(
                                                                text = if (isUploadedByAdmin) "👑 Admin materialı" else "Müştəri faylı",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = if (isUploadedByAdmin) Gold500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontSize = 10.sp,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (file.isFailed) {
                                                    FilledTonalButton(
                                                        onClick = { viewModel.retryOrderFileUpload(order.id, file.id) },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Təkrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.openOrDownloadOrderFile(context, file) { _, msg ->
                                                                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                                    ) {
                                                        Text("Aç", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold500)
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.openOrDownloadOrderFile(context, file) { _, msg ->
                                                                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Download, contentDescription = "Yüklə", tint = Gold500)
                                                    }
                                                }

                                                if (isStaff) {
                                                    IconButton(
                                                        onClick = { fileToDelete = file },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.DeleteOutline,
                                                            contentDescription = "Faylı sil",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Existing Review (if present)
            if (order.review != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Müştəri Rəyi və Qiymətləndirmə",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                for (i in 1..order.review.rating) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Gold500, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = order.review.comment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = order.review.submittedAt,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Rating & Review Modal
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = {
                Text(localizedString(StringKey.RATING_TITLE), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(localizedString(StringKey.RATING_QUESTION), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(14.dp))
                    InteractiveRatingBar(
                        rating = ratingStars,
                        onRatingChange = { ratingStars = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        placeholder = { Text(localizedString(StringKey.RATING_COMMENT_HINT)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitRating(order.id, ratingStars, reviewComment)
                        showReviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500)
                ) {
                    Text(localizedString(StringKey.BTN_CONFIRM))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text(localizedString(StringKey.BTN_CANCEL))
                }
            }
        )
    }

    // Admin Status Sheet
    if (showAdminStatusSheet) {
        ModalBottomSheet(onDismissRequest = { showAdminStatusSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Admin: Status Yeniləməsi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Status dəyişdikdə bildiriş yalnız bu sifarişin sahibinə göndəriləcək.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Qəbul edildi (Accepted)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            viewModel.adminUpdateOrderStatus(
                                order.id,
                                OrderStatus.ACCEPTED,
                                "Sifarişiniz inzibatçı tərəfindən qəbul edildi və icraya yönləndirildi."
                            )
                            showAdminStatusSheet = false
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Qəbul edildi", fontWeight = FontWeight.Bold)
                            Text("Sifarişi təsdiq et və kurator təyin et", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(status = OrderStatus.ACCEPTED)
                    }
                }

                // 2. Hazırlanır (In Progress)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            viewModel.adminUpdateOrderStatus(
                                order.id,
                                OrderStatus.IN_PROGRESS,
                                "Sifarişinizin icrasına başlanıldı və hazırda mütəxəssis tərəfindən hazırlanır."
                            )
                            showAdminStatusSheet = false
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Hazırlanır", fontWeight = FontWeight.Bold)
                            Text("İş icra prosesindədir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(status = OrderStatus.IN_PROGRESS)
                    }
                }

                // 3. Hazırdır (Ready)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            viewModel.adminUpdateOrderStatus(
                                order.id,
                                OrderStatus.READY,
                                "Sifarişiniz tam hazırlandı. Əlavə edilmiş sənədləri yoxlaya və yükləyə bilərsiniz."
                            )
                            showAdminStatusSheet = false
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Hazırdır", fontWeight = FontWeight.Bold)
                            Text("Sifariş tamamlandı və hazırdır", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(status = OrderStatus.READY)
                    }
                }

                // 4. Ləğv edildi (Cancelled - prompts reason dialog)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            showAdminStatusSheet = false
                            cancelReasonText = ""
                            showCancelReasonDialog = true
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Ləğv edildi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Səbəb qeyd edərək sifarişi ləğv et", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                        }
                        StatusBadge(status = OrderStatus.CANCELLED)
                    }
                }
            }
        }
    }

    // Cancellation Reason Dialog
    if (showCancelReasonDialog) {
        AlertDialog(
            onDismissRequest = { showCancelReasonDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Sifarişin ləğv edilməsi", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Zəhmət olmasa ləğv edilmə səbəbini qeyd edin. Bu səbəb müştəriyə bildiriş və status qeydi kimi çatdırılacaq:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = cancelReasonText,
                        onValueChange = { cancelReasonText = it },
                        placeholder = { Text("Məsələn: Tələb olunan mövzu ixtisas profilinə uyğun deyil...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = cancelReasonText.trim().ifBlank { "İnzibatçı tərəfindən ləğv edildi." }
                        viewModel.adminUpdateOrderStatus(
                            order.id,
                            OrderStatus.CANCELLED,
                            "Sifariş ləğv edildi. Səbəb: $reason"
                        )
                        showCancelReasonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ləğvi təsdiqlə", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelReasonDialog = false }) {
                    Text(localizedString(StringKey.BTN_CANCEL))
                }
            }
        )
    }

    // File Deletion Confirmation Dialog
    if (fileToDelete != null) {
        val f = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Faylı silmək istəyirsiniz?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "'${f.originalFileName.ifBlank { f.name }}' adlı fayl sifarişdən və Firebase Storage-dan silinəcək.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDeleteId = f.id
                        fileToDelete = null
                        viewModel.adminDeleteOrderFile(
                            orderId = order.id,
                            fileId = toDeleteId,
                            onSuccess = {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Fayl uğurla silindi.") }
                            },
                            onError = { err ->
                                coroutineScope.launch { snackbarHostState.showSnackbar("Xəta: $err") }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Bəli, sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text(localizedString(StringKey.BTN_CANCEL))
                }
            }
        )
    }
}

@Composable
private fun TimelineStepItem(
    step: com.example.model.OrderStatusHistoryItem,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Gold500)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(54.dp)
                        .background(Gold500.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = if (!isLast) 20.dp else 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = localizedString(step.status.labelKey),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• ${step.timestamp}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = step.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
