package com.nailit.app.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.R
import com.nailit.app.core.preview.SupabaseFunctionRepository
import kotlinx.coroutines.launch
import java.util.UUID

private val PageBackground = Color(0xFFFCF8F5)
private val CardBackground = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFF3E6DE)
private val SoftShadow = Color(0x14D8B8A8)
private val TitleColor = Color(0xFF181312)
private val BodyColor = Color(0xFF93837A)
private val Accent = Color(0xFFDF8B86)
private val AccentSoft = Color(0xFFFFF0F0)
private val AccentWarm = Color(0xFFFDE7DF)
private val StepSoft = Color(0xFFF7F2EF)
private val DividerColor = Color(0xFFF2E9E3)
private val GreenAccent = Color(0xFF5DAE8B)
private val PurpleAccent = Color(0xFF8A7EF8)
private val BlackAccent = Color(0xFF161616)
private val RedAccent = Color(0xFFF45E66)
private val YoutubeAccent = Color(0xFFFF5631)

private data class InspirationItem(
    val id: String,
    val title: String,
    val subtitle: String
)

@Composable
fun HomeScreen(
    selectedId: String?,
    onSelectId: (String) -> Unit,
    onOpenFlow: () -> Unit,
    onOpenResult: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SupabaseFunctionRepository() }
    var linkInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    val inspirations = listOf(
        InspirationItem("aurora-cat", "温柔裸粉法式", "1.2w 人解析"),
        InspirationItem("retro-wine", "焦糖琥珀晕染", "8567 人解析"),
        InspirationItem("french-soft", "透亮冰透款", "1.1w 人解析"),
        InspirationItem("pearl-milk", "清新花卉款", "9234 人解析"),
        InspirationItem("golden-amber", "气质豆沙款", "7643 人解析")
    )

    val submitSource = remember(
        context,
        repository,
        linkInput,
        selectedId,
        isSubmitting
    ) {
        {
            if (isSubmitting || (linkInput.isBlank() && selectedId == null)) return@remember

            scope.launch {
                isSubmitting = true
                statusText = "正在创建美甲会话..."

                val sourceUrl = linkInput.ifBlank {
                    when (selectedId) {
                        "aurora-cat" -> "preset://aurora-cat"
                        "french-soft" -> "preset://french-soft"
                        "retro-wine" -> "preset://retro-wine"
                        "pearl-milk" -> "preset://pearl-milk"
                        "golden-amber" -> "preset://golden-amber"
                        else -> "preset://unknown"
                    }
                }

                runCatching {
                    val installId = getInstallId(context)
                    val session = repository.createSession(
                        installId = installId,
                        sourceType = if (linkInput.isNotBlank()) "short_video_link" else "preset"
                    )
                    statusText = "正在提交教程来源..."
                    repository.submitSourceLink(
                        sessionId = session.session_id,
                        sourceUrl = sourceUrl
                    )
                }.onSuccess {
                    statusText = if (repository.isConfigured()) {
                        "已连接 Supabase，教程来源已提交。"
                    } else {
                        "当前为 demo 后端模式，已模拟提交教程来源。"
                    }
                    onOpenFlow()
                }.onFailure { error ->
                    statusText = "提交失败：${error.message ?: "未知错误"}"
                }

                isSubmitting = false
            }
        }
    }

    Scaffold(
        containerColor = PageBackground,
        bottomBar = {
            BottomNavigationBar()
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            FakeStatusBar()
            TopBrandSection()
            PasteInputCard(
                value = linkInput,
                onValueChange = { linkInput = it },
                onSubmit = submitSource,
                isSubmitting = isSubmitting
            )
            Text(
                text = "支持抖音、小红书、YouTube 等平台链接  ⓘ",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BodyColor,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
            CapabilityMetricsRow()
            TutorialExampleCard(
                onOpenFlow = onOpenFlow
            )
            InspirationGalleryCard(
                selectedId = selectedId,
                items = inspirations,
                onSelect = onSelectId
            )
            statusText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BodyColor,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun FakeStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "9:41",
            style = MaterialTheme.typography.titleMedium.copy(
                color = TitleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TitleColor)
            )
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TitleColor)
            )
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, TitleColor, RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(TitleColor)
                )
            }
        }
    }
}

@Composable
private fun TopBrandSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "指尖 SOP",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 34.sp,
                        lineHeight = 36.sp,
                        color = TitleColor
                    )
                )
                Text(
                    text = "✦",
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Accent,
                        fontSize = 14.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "AI 美甲步骤生成器",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BodyColor,
                    fontSize = 15.sp
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, CardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = BodyColor,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "历史记录",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TitleColor,
                    fontSize = 13.sp
                )
            )
        }
    }
}

@Composable
private fun PasteInputCard(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = SoftShadow,
                spotColor = SoftShadow
            )
            .clip(RoundedCornerShape(26.dp))
            .background(CardBackground)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFF1CDC9), CardBorder, Color(0xFFF1CDC9))
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = BodyColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = TitleColor,
                    fontSize = 15.sp
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = "粘贴美甲教程链接（抖音 / 小红书 / YouTube）",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = BodyColor,
                                fontSize = 15.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Accent)
                    .clickable(enabled = !isSubmitting, onClick = onSubmit)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "粘贴",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityMetricsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MetricItem(
            tint = Accent,
            icon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp)) },
            text = "12K+ 教程解析"
        )
        MetricDivider()
        MetricItem(
            tint = PurpleAccent,
            icon = { Icon(Icons.Default.FactCheck, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(20.dp)) },
            text = "6 步拆解"
        )
        MetricDivider()
        MetricItem(
            tint = GreenAccent,
            icon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(20.dp)) },
            text = "AI 智能识别"
        )
    }
}

@Composable
private fun MetricItem(
    tint: Color,
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TitleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(22.dp)
            .background(DividerColor)
    )
}

@Composable
private fun TutorialExampleCard(
    onOpenFlow: () -> Unit
) {
    SurfaceCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✦",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Accent,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "教程解析示例",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TitleColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onOpenFlow)
                ) {
                    Text(
                        text = "查看全部",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BodyColor,
                            fontSize = 13.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BodyColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            StepProgressRow()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(208.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenFlow)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_nail_ui_reference),
                    contentDescription = "Tutorial sample",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = TitleColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "在此粘贴 抖音 / 小红书 / YouTube 美甲教程链接",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TitleColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                )
                Text(
                    text = "支持链接自动识别，生成步骤拆解",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BodyColor,
                        fontSize = 13.sp
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlatformChip("抖音", BlackAccent, "♪")
                    PlatformChip("小红书", RedAccent, "红")
                    PlatformChip("YouTube", YoutubeAccent, "▶")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = BodyColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "支持长链接",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BodyColor,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StepProgressRow() {
    val steps = listOf("修剪", "底油", "颜色", "封层", "亮油", "完成")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (index == 0) AccentSoft else StepSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (index == 0) Accent else BodyColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TitleColor,
                        fontSize = 13.sp,
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    maxLines = 1
                )
                if (index != steps.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .weight(1f),
                        thickness = 1.dp,
                        color = DividerColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PlatformChip(
    label: String,
    color: Color,
    badge: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TitleColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun InspirationGalleryCard(
    selectedId: String?,
    items: List<InspirationItem>,
    onSelect: (String) -> Unit
) {
    SurfaceCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "♨",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Accent,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "热门美甲灵感",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TitleColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "更多灵感",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BodyColor,
                            fontSize = 13.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BodyColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEachIndexed { index, item ->
                    val selected = selectedId == item.id || (selectedId == null && index == 0)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(item.id) }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.home_nail_ui_reference),
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(122.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) Accent else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TitleColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BodyColor,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 34.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomTabItem(
            selected = true,
            selectedIcon = { Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(28.dp)) },
            unselectedIcon = { Icon(Icons.Outlined.Home, contentDescription = null, modifier = Modifier.size(28.dp)) },
            label = "首页"
        )
        BottomTabItem(
            selected = false,
            selectedIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(28.dp)) },
            unselectedIcon = { Icon(Icons.Outlined.TipsAndUpdates, contentDescription = null, modifier = Modifier.size(28.dp)) },
            label = "灵感"
        )
        BottomTabItem(
            selected = false,
            selectedIcon = { Icon(Icons.Filled.PersonOutline, contentDescription = null, modifier = Modifier.size(28.dp)) },
            unselectedIcon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null, modifier = Modifier.size(28.dp)) },
            label = "我的"
        )
    }
}

@Composable
private fun BottomTabItem(
    selected: Boolean,
    selectedIcon: @Composable () -> Unit,
    unselectedIcon: @Composable () -> Unit,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentWarm)
                )
                Box(
                    modifier = Modifier.padding(3.dp)
                ) {
                    CompositionLocalProvider(LocalContentColor provides Accent) {
                        selectedIcon()
                    }
                }
            } else {
                CompositionLocalProvider(LocalContentColor provides BodyColor) {
                    unselectedIcon()
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (selected) Accent else BodyColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun SurfaceCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = SoftShadow,
                spotColor = SoftShadow
            )
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .border(1.dp, Color(0xFFF6EEEA), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

private fun getInstallId(context: Context): String {
    val prefs = context.getSharedPreferences("nailit-local", Context.MODE_PRIVATE)
    return prefs.getString("install_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("install_id", it).apply()
    }
}
