package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.StoryItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun StoryBar(
    stories: List<StoryItem>,
    currentUserId: String,
    isStaff: Boolean,
    onStoryClick: (Int) -> Unit,
    onAddStoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Staff Add Story button
        if (isStaff) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clickable { onAddStoryClick() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, BirbankRed, CircleShape)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(BirbankRedLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Story əlavə et",
                                tint = BirbankRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Story əlavə et",
                        style = MaterialTheme.typography.labelSmall,
                        color = BirbankTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 2. Active Stories
        itemsIndexed(stories) { index, story ->
            val isViewed = story.isViewedBy(currentUserId)
            val ringColor = if (isViewed) Color(0xFFD0D5DD) else BirbankRed
            val ringWidth = if (isViewed) 1.5.dp else 2.dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onStoryClick(index) }
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .border(ringWidth, ringColor, CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (story.mediaUrl.isNotBlank()) {
                        AsyncImage(
                            model = story.mediaUrl,
                            contentDescription = story.caption,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(BirbankRedLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = story.creatorName.take(1).uppercase(),
                                color = BirbankRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = story.creatorName.ifBlank { "BestGroup" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isViewed) BirbankTextSecondary else BirbankTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = if (isViewed) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StoryViewerDialog(
    stories: List<StoryItem>,
    initialIndex: Int,
    isStaff: Boolean,
    onDismiss: () -> Unit,
    onMarkViewed: (String) -> Unit,
    onDeleteStory: (String) -> Unit
) {
    if (stories.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, stories.lastIndex)) }
    val currentStory = stories.getOrNull(currentIndex) ?: return
    var isPaused by remember { mutableStateOf(false) }

    val progress = remember(currentIndex) { Animatable(0f) }

    LaunchedEffect(currentStory.id) {
        onMarkViewed(currentStory.id)
    }

    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            val remaining = (1f - progress.value).coerceIn(0f, 1f)
            val duration = (5000L * remaining).toLong().coerceAtLeast(100L)
            val result = progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = duration.toInt(), easing = LinearEasing)
            )
            if (result.endReason == androidx.compose.animation.core.AnimationEndReason.Finished) {
                if (currentIndex < stories.lastIndex) {
                    currentIndex++
                } else {
                    onDismiss()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(currentIndex) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                // Previous
                                if (currentIndex > 0) {
                                    currentIndex--
                                } else {
                                    onDismiss()
                                }
                            } else {
                                // Next
                                if (currentIndex < stories.lastIndex) {
                                    currentIndex++
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Main Media Image
            if (currentStory.mediaUrl.isNotBlank()) {
                AsyncImage(
                    model = currentStory.mediaUrl,
                    contentDescription = currentStory.caption,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E232A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentStory.caption.ifBlank { "BestGroup.az Rəsmi Məlumat" },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            // Top Progress Bars & Header Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Segmented Progress Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { i, _ ->
                        val segmentProgress = when {
                            i < currentIndex -> 1f
                            i == currentIndex -> progress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Author Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BirbankRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentStory.creatorName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentStory.creatorName.ifBlank { "BestGroup.az Admin" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Rəsmi Təmsilçi",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }

                    // Delete button for staff
                    if (isStaff) {
                        IconButton(
                            onClick = {
                                onDeleteStory(currentStory.id)
                                if (stories.size <= 1) {
                                    onDismiss()
                                } else if (currentIndex < stories.lastIndex) {
                                    // advance
                                } else {
                                    currentIndex = (currentIndex - 1).coerceAtLeast(0)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Story-ni sil",
                                tint = Color.White
                            )
                        }
                    }

                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Bağla",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom Caption Box (if present)
            if (currentStory.caption.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = currentStory.caption,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onPublish: (bytes: ByteArray, caption: String, mimeType: String) -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            errorMessage = null
        }
    }

    Dialog(onDismissRequest = { if (!isUploading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Yeni Story Paylaş",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BirbankTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Paylaşılan story 24 saat ərzində bütün istifadəçilərə görünəcək",
                    style = MaterialTheme.typography.bodySmall,
                    color = BirbankTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Image Preview or Select Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BirbankBgLight)
                        .border(1.dp, BirbankBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            photoPicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUri != null) {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = "Seçilən şəkil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Şəkil seç",
                                tint = BirbankRed,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Qalereyadan şəkil seç",
                                color = BirbankRed,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "PNG, JPG və ya WEBP",
                                color = BirbankTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Açıqlama və ya başlıq") },
                    placeholder = { Text("Məs: Yeni kampaniyamız başladı!") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BirbankRed,
                        cursorColor = BirbankRed
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = BirbankRed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isUploading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İmtina", color = BirbankTextSecondary)
                    }

                    Button(
                        onClick = {
                            val uri = selectedUri
                            if (uri == null) {
                                errorMessage = "Zəhmət olmasa bir şəkil seçin."
                                return@Button
                            }
                            isUploading = true
                            try {
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                if (bytes == null || bytes.isEmpty()) {
                                    errorMessage = "Şəkil oxunarkən xəta baş verdi."
                                    isUploading = false
                                    return@Button
                                }
                                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                                onPublish(bytes, caption, mimeType)
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Şəkil oxuna bilmədi."
                                isUploading = false
                            }
                        },
                        enabled = !isUploading && selectedUri != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BirbankRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Paylaş", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
