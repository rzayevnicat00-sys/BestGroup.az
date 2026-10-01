package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.FileUploadState
import com.example.model.OrderAttachedFile
import com.example.model.OrderPriority
import com.example.model.ServiceType
import com.example.services.firebase.FirebaseStorageService
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.AppDestination
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderWizardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.wizardState.collectAsState()
    val totalSteps = 8

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = localizedString(StringKey.WIZARD_TITLE),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Addım ${state.currentStep} / $totalSteps: ${getStepTitle(state.currentStep)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.currentStep > 1 && state.completedOrder == null) {
                                viewModel.wizardPreviousStep()
                            } else {
                                viewModel.navigateTo(AppDestination.MAIN)
                            }
                        },
                        modifier = Modifier.testTag("wizard_nav_back")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (state.completedOrder == null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (state.currentStep > 1) {
                            OutlinedButton(
                                onClick = { viewModel.wizardPreviousStep() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("wizard_prev_btn")
                            ) {
                                Text(localizedString(StringKey.BTN_BACK), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = { viewModel.wizardNextStep() },
                            shape = RoundedCornerShape(12.dp),
                            enabled = !state.isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500),
                            modifier = Modifier
                                .weight(if (state.currentStep > 1) 1.5f else 1f)
                                .height(50.dp)
                                .testTag("wizard_next_btn")
                        ) {
                            if (state.isSubmitting) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Gold500,
                                        strokeWidth = 2.dp
                                    )
                                    val pct = (state.uploadProgress * 100).toInt()
                                    Text(
                                        text = if (pct > 0) "Yüklənir... $pct%" else "Göndərilir...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = if (state.currentStep == 7) localizedString(StringKey.BTN_SUBMIT) else localizedString(StringKey.BTN_NEXT),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Linear Progress Bar across the 8 steps
            LinearProgressIndicator(
                progress = { (state.currentStep.toFloat() / totalSteps) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Gold500,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            if (state.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Step Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                if (state.completedOrder != null) {
                    // Step 8: Order Confirmation Success
                    OrderSuccessStep(
                        order = state.completedOrder!!,
                        onViewOrder = {
                            viewModel.viewOrderDetail(state.completedOrder!!)
                        },
                        onGoHome = {
                            viewModel.navigateTo(AppDestination.MAIN)
                        }
                    )
                } else {
                    when (state.currentStep) {
                        1 -> Step1ServiceSelect(state, viewModel)
                        2 -> Step2TopicScope(state, viewModel)
                        3 -> Step3AcademicInfo(state, viewModel)
                        4 -> Step4FilesUpload(state, viewModel)
                        5 -> Step5DeadlinePriority(state, viewModel)
                        6 -> Step6FormattingNotes(state, viewModel)
                        7 -> Step7ReviewOrder(state, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun Step1ServiceSelect(state: com.example.viewmodel.OrderWizardUiState, viewModel: MainViewModel) {
    val catalogServices by viewModel.catalogServices.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = localizedString(StringKey.WIZARD_SELECT_SERVICE_PROMPT),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(catalogServices, key = { it.serviceId }) { cat ->
            val isSelected = state.selectedCatalogService?.serviceId == cat.serviceId ||
                (state.selectedCatalogService == null && state.selectedService?.name.equals(cat.serviceId, ignoreCase = true))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val matchingType = ServiceType.values().find {
                            it.name.equals(cat.serviceId, ignoreCase = true) || it.name.replace("_", "").equals(cat.serviceId.replace("_", ""), ignoreCase = true)
                        } ?: ServiceType.DIPLOMA
                        viewModel.updateWizardState { it.copy(selectedCatalogService = cat, selectedService = matchingType, error = null) }
                    }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Gold500 else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("wizard_service_choice_${cat.serviceId}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            val matchingType = ServiceType.values().find {
                                it.name.equals(cat.serviceId, ignoreCase = true) || it.name.replace("_", "").equals(cat.serviceId.replace("_", ""), ignoreCase = true)
                            } ?: ServiceType.DIPLOMA
                            viewModel.updateWizardState { it.copy(selectedCatalogService = cat, selectedService = matchingType, error = null) }
                        }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cat.getLocalizedName(currentLanguage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = cat.getFormattedPrice(currentLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step2TopicScope(state: com.example.viewmodel.OrderWizardUiState, viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "İşin mövzusu və istiqaməti *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tədqiqat mövzusunu tam və aydın şəkildə daxil edin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.topic,
            onValueChange = { newTopic -> viewModel.updateWizardState { it.copy(topic = newTopic) } },
            label = { Text(localizedString(StringKey.WIZARD_WORK_TOPIC_LABEL)) },
            placeholder = { Text(localizedString(StringKey.WIZARD_WORK_TOPIC_HINT)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wizard_input_topic"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "İşin əhatə dairəsi və tələbləri",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = state.scopeDescription,
            onValueChange = { newDesc -> viewModel.updateWizardState { it.copy(scopeDescription = newDesc) } },
            label = { Text(localizedString(StringKey.WIZARD_WORK_SCOPE_LABEL)) },
            placeholder = { Text(localizedString(StringKey.WIZARD_WORK_SCOPE_HINT)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("wizard_input_scope"),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun Step3AcademicInfo(state: com.example.viewmodel.OrderWizardUiState, viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = localizedString(StringKey.WIZARD_ACADEMIC_INFO_TITLE),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.university,
            onValueChange = { v -> viewModel.updateWizardState { it.copy(university = v) } },
            label = { Text(localizedString(StringKey.LABEL_UNIVERSITY)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = state.faculty,
            onValueChange = { v -> viewModel.updateWizardState { it.copy(faculty = v) } },
            label = { Text(localizedString(StringKey.LABEL_FACULTY)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.language,
                onValueChange = { v -> viewModel.updateWizardState { it.copy(language = v) } },
                label = { Text(localizedString(StringKey.WIZARD_LANGUAGE_LABEL)) },
                modifier = Modifier
                    .weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.pageCount,
                onValueChange = { v -> viewModel.updateWizardState { it.copy(pageCount = v) } },
                label = { Text(localizedString(StringKey.WIZARD_PAGES_LABEL)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun Step4FilesUpload(state: com.example.viewmodel.OrderWizardUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val allowedMimeTypes = arrayOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "image/jpeg",
        "image/png"
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
                    if (c.moveToFirst()) {
                        val displayName = if (nameIndex != -1) c.getString(nameIndex) else "sənəd"
                        val size = if (sizeIndex != -1) c.getLong(sizeIndex) else 1024000L
                        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        viewModel.addWizardPickedFile(displayName, size, mime, uri.toString())
                    }
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = localizedString(StringKey.WIZARD_FILES_TITLE),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = localizedString(StringKey.WIZARD_FILES_DESC),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Professional Dropzone / Upload Action Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { filePickerLauncher.launch(allowedMimeTypes) }
                .border(
                    1.5.dp,
                    Gold500,
                    RoundedCornerShape(16.dp)
                )
                .testTag("wizard_upload_dropzone"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Gold500.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Gold500,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = localizedString(StringKey.WIZARD_FILES_DROPZONE),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PDF, DOC, DOCX, PPT, PPTX, JPG və PNG (maks. 20 MB)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { filePickerLauncher.launch(allowedMimeTypes) },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500)
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Fayl seç", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.addWizardSampleFile() },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nümunə sənəd", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Attached Files List Header
        Text(
            text = "Əlavə olunmuş sənədlər (${state.attachedFiles.size}/10)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.attachedFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localizedString(StringKey.FILE_EMPTY_TITLE),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.attachedFiles, key = { it.id }) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = Gold500
                                    )
                                    Column {
                                        Text(
                                            text = file.name,
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
                                            val (statusText, statusColor) = when (file.state) {
                                                FileUploadState.PENDING -> "Gözləyir" to MaterialTheme.colorScheme.onSurfaceVariant
                                                FileUploadState.UPLOADING -> "Yüklənir..." to Gold500
                                                FileUploadState.UPLOADED, FileUploadState.SUCCESS -> "Yükləndi" to Color(0xFF2E7D32)
                                                FileUploadState.FAILED, FileUploadState.ERROR -> "Uğursuz oldu" to MaterialTheme.colorScheme.error
                                                FileUploadState.CANCELLED -> "Ləğv edildi" to MaterialTheme.colorScheme.error
                                            }
                                            Text(
                                                text = "• $statusText",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = statusColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.removeWizardFile(file.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Faylı sil",
                                        tint = MaterialTheme.colorScheme.error
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

@Composable
private fun Step5DeadlinePriority(state: com.example.viewmodel.OrderWizardUiState, viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = localizedString(StringKey.WIZARD_DEADLINE_LABEL),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.deadline,
            onValueChange = { v -> viewModel.updateWizardState { it.copy(deadline = v) } },
            label = { Text("Təhvil tarixi") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Gold500) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = localizedString(StringKey.WIZARD_PRIORITY_LABEL),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        val priorities = listOf(
            OrderPriority.NORMAL to ("Standart icra müddəti (Gündəlik rejim)" to Icons.Default.Schedule),
            OrderPriority.HIGH to ("Yüksək prioritet (Qısa müddətli təhvil)" to Icons.Default.PriorityHigh),
            OrderPriority.URGENT to ("Təcili ekspress rejim (24-48 saat ərzində)" to Icons.Default.Bolt)
        )

        priorities.forEach { (pri, meta) ->
            val isSelected = state.priority == pri
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.updateWizardState { it.copy(priority = pri) } }
                    .border(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) Gold500 else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.updateWizardState { it.copy(priority = pri) } }
                    )
                    Column {
                        PriorityBadge(priority = pri)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = meta.first,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step6FormattingNotes(state: com.example.viewmodel.OrderWizardUiState, viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = localizedString(StringKey.WIZARD_FORMAT_STANDARD_LABEL),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        val standards = listOf("APA 7th Edition", "Harvard Reference", "AAK Standartı", "MLA 9th Edition", "IEEE Format")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            standards.take(3).forEach { std ->
                val isSel = state.formattingStandard == std
                FilterChip(
                    selected = isSel,
                    onClick = { viewModel.updateWizardState { it.copy(formattingStandard = std) } },
                    label = { Text(std, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = localizedString(StringKey.WIZARD_NOTES_LABEL),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.specialNotes,
            onValueChange = { v -> viewModel.updateWizardState { it.copy(specialNotes = v) } },
            label = { Text(localizedString(StringKey.WIZARD_NOTES_LABEL)) },
            placeholder = { Text(localizedString(StringKey.WIZARD_NOTES_HINT)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun Step7ReviewOrder(state: com.example.viewmodel.OrderWizardUiState, viewModel: MainViewModel) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = localizedString(StringKey.WIZARD_REVIEW_TITLE),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Məlumatları nəzərdən keçirin və təsdiqləyin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ReviewRow("Xidmət növü", localizedString(state.selectedService?.nameKey ?: StringKey.SRV_DIPLOMA_NAME))
                    ReviewRow("Mövzu", state.topic.ifBlank { "Mövzu qeyd edilməyib" })
                    ReviewRow("Universitet", state.university)
                    ReviewRow("Fakültə", state.faculty)
                    ReviewRow("Dil və Səhifə", "${state.language} • ${state.pageCount} səhifə")
                    ReviewRow("Deadline", state.deadline)
                    ReviewRow("Prioritet", localizedString(state.priority.labelKey))
                    ReviewRow("Format Standartı", state.formattingStandard)
                    ReviewRow("Qoşulan sənədlər", "${state.attachedFiles.size} ədəd fayl")
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
}

@Composable
private fun OrderSuccessStep(
    order: com.example.model.Order,
    onViewOrder: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Gold500),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Navy900,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = localizedString(StringKey.WIZARD_SUCCESS_TITLE),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = localizedString(StringKey.WIZARD_SUCCESS_DESC),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Order tracking code card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = localizedString(StringKey.WIZARD_ORDER_CODE),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = order.orderNumber,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold500
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onViewOrder,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Gold500),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("wizard_view_order_btn")
        ) {
            Text("Sifarişə bax", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoHome,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Ana səhifəyə qayıt", fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun getStepTitle(step: Int): String = when (step) {
    1 -> "Xidmət seçimi"
    2 -> "İş haqqında məlumat"
    3 -> "Akademik məlumatlar"
    4 -> "Fayllar"
    5 -> "Deadline və Prioritet"
    6 -> "Əlavə qeydlər"
    7 -> "Sifarişin yoxlanılması"
    8 -> "Göndərilməsi"
    else -> ""
}
