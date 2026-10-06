package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.model.OrderPriority
import com.example.model.OrderStatus
import com.example.ui.theme.*

@Composable
fun BestGroupLogoBadge(
    modifier: Modifier = Modifier,
    size: Int = 40,
    forceWhiteLogo: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.28).dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Navy800, Navy900)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(Gold500, Gold600)),
                shape = RoundedCornerShape((size * 0.28).dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(
                id = com.example.R.drawable.ic_bestgroup_logo_white
            ),
            contentDescription = "BestGroup.az Logo",
            modifier = Modifier.size((size * 0.72).dp)
        )
    }
}

@Composable
fun StatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor) = when (status) {
        OrderStatus.PENDING -> Triple(StatusPendingBg, StatusPending, StatusPending)
        OrderStatus.ACCEPTED -> Triple(StatusAcceptedBg, StatusAccepted, StatusAccepted)
        OrderStatus.IN_PROGRESS -> Triple(StatusInProgressBg, StatusInProgress, StatusInProgress)
        OrderStatus.READY -> Triple(StatusReadyBg, StatusReady, StatusReady)
        OrderStatus.CANCELLED -> Triple(StatusCancelledBg, StatusCancelled, StatusCancelled)
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        color = bgColor.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = localizedString(status.labelKey),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
fun PriorityBadge(
    priority: OrderPriority,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (priority) {
        OrderPriority.NORMAL -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.Schedule)
        OrderPriority.HIGH -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.PriorityHigh)
        OrderPriority.URGENT -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Icons.Default.Bolt)
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = localizedString(priority.labelKey),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color = Gold500,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    titleKey: StringKey,
    descKey: StringKey,
    actionButtonTextKey: StringKey? = null,
    onActionClick: (() -> Unit)? = null,
    icon: ImageVector = Icons.Default.Inbox,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Gold500,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = localizedString(titleKey),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = localizedString(descKey),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionButtonTextKey != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy800,
                    contentColor = Gold500
                ),
                modifier = Modifier.testTag("empty_state_action_button")
            ) {
                Text(
                    text = localizedString(actionButtonTextKey),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun InteractiveRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxStars) {
            val isSelected = i <= rating
            val starColor by animateColorAsState(
                targetValue = if (isSelected) Gold500 else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                label = "star_color"
            )
            IconButton(
                onClick = { onRatingChange(i) },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("star_rate_$i")
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "$i ulduz",
                    tint = starColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
