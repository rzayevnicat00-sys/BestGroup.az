package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.BirbankRed
import com.example.ui.theme.BirbankRedLight

/**
 * Robust image loader that seamlessly supports:
 * 1. Base64 Data URLs (data:image/...;base64,...)
 * 2. Raw Base64 encoded byte strings
 * 3. Remote HTTP/HTTPS URLs via Coil
 * 4. Graceful loading indicator and fallback placeholders (NEVER a blank/white screen).
 */
@Composable
fun SafeMediaImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackInitial: String? = null,
    isAvatar: Boolean = false,
    showLoading: Boolean = true
) {
    val cleanModel = model?.trim().orEmpty()

    val isBase64 = remember(cleanModel) {
        cleanModel.startsWith("data:image", ignoreCase = true) ||
                (cleanModel.length > 150 && !cleanModel.startsWith("http://", ignoreCase = true) && !cleanModel.startsWith("https://", ignoreCase = true) && !cleanModel.startsWith("/"))
    }

    val decodedBitmap: ImageBitmap? = remember(cleanModel, isBase64) {
        if (isBase64 && cleanModel.isNotBlank()) {
            decodeDataUrlOrBase64ToBitmap(cleanModel)?.asImageBitmap()
        } else {
            null
        }
    }

    if (cleanModel.isBlank()) {
        DefaultImagePlaceholder(
            modifier = modifier,
            fallbackInitial = fallbackInitial,
            isAvatar = isAvatar
        )
    } else if (isBase64) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        } else {
            DefaultImagePlaceholder(
                modifier = modifier,
                fallbackInitial = fallbackInitial,
                isAvatar = isAvatar
            )
        }
    } else {
        // Standard HTTP / HTTPS URL or file path via Coil SubcomposeAsyncImage
        SubcomposeAsyncImage(
            model = cleanModel,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                if (showLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = BirbankRed,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE5E7EB))
                    )
                }
            },
            error = {
                DefaultImagePlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    fallbackInitial = fallbackInitial,
                    isAvatar = isAvatar
                )
            }
        )
    }
}

@Composable
fun DefaultImagePlaceholder(
    modifier: Modifier = Modifier,
    fallbackInitial: String? = null,
    isAvatar: Boolean = false
) {
    Box(
        modifier = modifier.background(BirbankRedLight),
        contentAlignment = Alignment.Center
    ) {
        if (isAvatar) {
            val initial = fallbackInitial?.trim()?.take(1)?.uppercase() ?: ""
            if (initial.isNotBlank()) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BirbankRed,
                    fontSize = 18.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = BirbankRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "Görüntü mövcud deyil",
                tint = BirbankRed.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Safely decodes a Base64 string or Data URL into an Android Bitmap.
 * Returns null gracefully if invalid, avoiding app crashes.
 */
fun decodeDataUrlOrBase64ToBitmap(dataUrlOrBase64: String): android.graphics.Bitmap? {
    val clean = dataUrlOrBase64.trim()
    if (clean.isBlank()) return null
    return try {
        val rawData = if (clean.contains(",")) {
            clean.substringAfter(",")
        } else {
            clean
        }
        val bytes = Base64.decode(rawData, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}
