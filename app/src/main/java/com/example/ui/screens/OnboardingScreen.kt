package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.localization.StringKey
import com.example.localization.localizedString
import com.example.ui.components.BestGroupLogoBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingSlideData(
    val titleKey: StringKey,
    val descKey: StringKey,
    val imageResId: Int,
    val imageContentDescription: String,
    val benefits: List<StringKey> = emptyList()
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val slides = remember {
        listOf(
            OnboardingSlideData(
                titleKey = StringKey.ONBOARDING_TITLE_1,
                descKey = StringKey.ONBOARDING_DESC_1,
                imageResId = R.drawable.img_onboard_hero_1,
                imageContentDescription = "Akademik və təhsil mühiti"
            ),
            OnboardingSlideData(
                titleKey = StringKey.ONBOARDING_TITLE_2,
                descKey = StringKey.ONBOARDING_DESC_2,
                imageResId = R.drawable.img_onboard_hero_2,
                imageContentDescription = "Akademik sənədlər və elmi tədqiqat"
            ),
            OnboardingSlideData(
                titleKey = StringKey.ONBOARDING_TITLE_3,
                descKey = StringKey.ONBOARDING_DESC_3,
                imageResId = R.drawable.img_onboard_hero_3,
                imageContentDescription = "Peşəkar akademik komanda və uğurlu müdafiə",
                benefits = listOf(
                    StringKey.ONBOARDING_BENEFIT_1,
                    StringKey.ONBOARDING_BENEFIT_2,
                    StringKey.ONBOARDING_BENEFIT_3,
                    StringKey.ONBOARDING_BENEFIT_4
                )
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val currentPage = pagerState.currentPage

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Horizontal Pager for slides
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val slide = slides[pageIndex]
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top section: Full-width expansive hero illustration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.15f)
                ) {
                    Image(
                        painter = painterResource(id = slide.imageResId),
                        contentDescription = slide.imageContentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top gradient for status bar visibility
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Bottom gradient blend transition into sheet
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }

                // Bottom section: Card with typography, benefits, and call-to-action
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Page indicator dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 14.dp)
                            ) {
                                slides.indices.forEach { index ->
                                    val isSelected = index == currentPage
                                    Box(
                                        modifier = Modifier
                                            .height(6.dp)
                                            .width(if (isSelected) 28.dp else 8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) BirbankRed else BirbankBorder
                                            )
                                    )
                                }
                            }

                            // Title
                            Text(
                                text = localizedString(slide.titleKey),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = BirbankTextPrimary,
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Description
                            Text(
                                text = localizedString(slide.descKey),
                                style = MaterialTheme.typography.bodyMedium,
                                color = BirbankTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )

                            // Benefits on Slide 3
                            if (slide.benefits.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(BirbankRedLight)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    slide.benefits.forEach { benefitKey ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = BirbankRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = localizedString(benefitKey),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BirbankRed
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Next / Start Button
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    if (currentPage < slides.size - 1) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(currentPage + 1)
                                        }
                                    } else {
                                        onFinish()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BirbankRed,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("onboarding_next_button")
                            ) {
                                Text(
                                    text = if (currentPage == slides.size - 1) {
                                        localizedString(StringKey.BTN_GET_STARTED)
                                    } else {
                                        "Növbəti"
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating TopBar with Logo and Skip Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BestGroupLogoBadge(size = 28)
                    Text(
                        text = "BestGroup.az",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
            ) {
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier
                        .testTag("onboarding_skip_button")
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = localizedString(StringKey.BTN_SKIP),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
