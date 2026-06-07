package com.nailit.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.R
import com.nailit.app.core.preview.NailSessionSnapshot

private val ProfileBg = Color(0xFFF9F7F4)
private val ProfileText = Color(0xFF262328)
private val ProfileMuted = Color(0xFF8B878F)
private val ProfileLine = Color(0xFFE6E1DB)
private val ProfileCard = Color(0xFFFFFFFF)
private val ProfileShadow = Color(0x14CFC8C0)
private val ProfilePink = Color(0xFFE6BCC6)
private val ProfileMint = Color(0xFFCBE5D9)
private val ProfileGold = Color(0xFFF0D57E)
private val ProfileSoftMint = Color(0xFFF2FBF6)
private val ProfileSoftPink = Color(0xFFFFF4F7)
private val ProfileSoftGold = Color(0xFFFFFAEF)

private data class WishItem(
    val title: String,
    val imageRes: Int,
)

private data class DoneItem(
    val title: String,
    val date: String,
    val steps: String,
)

@Composable
fun ProfileScreen(
    sessionSnapshot: NailSessionSnapshot?,
    onResume: () -> Unit,
) {
    val wishItems = remember {
        listOf(
            WishItem("温柔裸粉法式美甲", R.drawable.huanhuan_nude),
            WishItem("猫眼星空紫", R.drawable.pink_blue_glass_cat_eye),
            WishItem("圣诞雪花限定款", R.drawable.ice_bear_hand_paint),
        )
    }
    val doneItems = remember(sessionSnapshot) {
        buildList<DoneItem> {
            add(
                DoneItem(
                    title = sessionSnapshot?.selectedTutorialId
                        ?.takeIf { it.isNotBlank() }
                        ?.replace("-", " ")
                        ?.split(" ")
                        ?.joinToString(" ") { token -> token.replaceFirstChar { it.uppercase() } }
                        ?: "温柔玫瑰豆沙 · 极光法式",
                    date = "2026-06-07",
                    steps = if (sessionSnapshot?.executionSteps?.isNotEmpty() == true) {
                        "${sessionSnapshot.executionSteps.size} 步完成"
                    } else {
                        "6 步完成"
                    },
                )
            )
            if (sessionSnapshot == null) {
                add(
                    DoneItem(
                        title = "奶油裸透跳色款",
                        date = "2026-06-03",
                        steps = "5 步完成",
                    )
                )
            }
        }
    }

    val finishedCount = doneItems.size
    val wishCount = wishItems.size
    val score = 200

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 22.dp,
            end = 22.dp,
            top = 24.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "我的",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = ProfileText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            letterSpacing = (-1.1).sp,
                        ),
                    )
                    Text(
                        text = "心愿单 · 制作记录",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = ProfileMuted,
                            fontSize = 15.sp,
                        ),
                    )
                }

                Surface(
                    color = ProfileSoftPink,
                    shape = RoundedCornerShape(999.dp),
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = "⭐ 入门",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color(0xFFD7A6B3),
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }

        item {
            SoftCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFF4FBF6), Color(0xFFEAF7F0)),
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "👋",
                            fontSize = 28.sp,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "欢迎来到指尖 SOP",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = ProfileText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                        )
                        Text(
                            text = "完成第一款美甲后，记录会出现在这里",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = ProfileMuted,
                                lineHeight = 20.sp,
                            ),
                        )
                    }
                }
            }
        }

        item {
            SoftCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetricColumn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FavoriteBorder,
                        iconTint = ProfilePink,
                        iconBg = ProfileSoftPink,
                        value = wishCount.toString(),
                        label = "心愿单",
                    )
                    MetricDivider()
                    MetricColumn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircleOutline,
                        iconTint = ProfileMint,
                        iconBg = ProfileSoftMint,
                        value = finishedCount.toString(),
                        label = "已完成",
                    )
                    MetricDivider()
                    MetricColumn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.StarBorder,
                        iconTint = ProfileGold,
                        iconBg = ProfileSoftGold,
                        value = score.toString(),
                        label = "积分",
                    )
                }
            }
        }

        item {
            SectionHeader(
                icon = Icons.Default.FavoriteBorder,
                iconTint = ProfilePink,
                title = "心愿单",
                trailing = "${wishItems.size} 款",
            )
        }

        items(wishItems) { item ->
            WishRow(
                item = item,
                onClick = onResume,
            )
        }

        item {
            SectionHeader(
                icon = Icons.Default.CheckCircleOutline,
                iconTint = ProfileMint,
                title = "已完成",
            )
        }

        items(doneItems) { item ->
            DoneRow(
                item = item,
                onClick = onResume,
            )
        }
    }
}

@Composable
private fun SoftCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = ProfileCard,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 3.dp,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun MetricColumn(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    value: String,
    label: String,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                color = ProfileText,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = ProfileMuted,
            ),
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .height(64.dp)
            .padding(horizontal = 6.dp)
            .size(width = 1.dp, height = 64.dp)
            .background(ProfileLine),
    )
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = ProfileText,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = ProfileMuted,
                ),
            )
        }
    }
}

@Composable
private fun WishRow(
    item: WishItem,
    onClick: () -> Unit,
) {
    Surface(
        color = ProfileCard,
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(item.imageRes),
                contentDescription = item.title,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = item.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = ProfileText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                ),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFD4D1D7),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DoneRow(
    item: DoneItem,
    onClick: () -> Unit,
) {
    Surface(
        color = ProfileCard,
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ProfileSoftMint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    tint = ProfileMint,
                    modifier = Modifier.size(30.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = ProfileText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    ),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "◷  ${item.date}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ProfileMuted,
                        ),
                    )
                    Text(
                        text = item.steps,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ProfileMint,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFD4D1D7),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
