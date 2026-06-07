package com.nailit.app.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import android.util.Log
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.core.preview.HandPhotoRuntime
import com.nailit.app.R
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.network.SupabaseManager
import com.nailit.app.core.preview.SupabaseFunctionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.absoluteValue

private val EntryBg = Color(0xFFF8F4F0)
private val EntryTitle = Color(0xFF161211)
private val EntryBody = Color(0xFF7E726A)
private val EntryAccent = Color(0xFF2B1D1A)
private val EntrySoft = Color(0xFFF1ECE6)

private data class TemplateItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageRes: Int,
)

@Composable
fun HomeScreen(
    selectedId: String?,
    onSelectId: (String) -> Unit,
    onOpenFlow: () -> Unit,
    onOpenResult: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenInspiration: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SupabaseFunctionRepository() }
    var linkInput by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    var showSniffedDialog by remember { mutableStateOf(false) }
    var sniffedTemplate by remember { mutableStateOf<TemplateItem?>(null) }
    var sniffedUrl by remember { mutableStateOf("") }
    var isSniffing by remember { mutableStateOf(false) }

    val templates = listOf(
        TemplateItem(
            id = "polar-cat-matte",
            title = "极光猫眼渐变磨砂",
            subtitle = "磨砂质感 + 侧边聚光",
            imageRes = R.drawable.polar_cat_matte,
        ),
        TemplateItem(
            id = "french-polar-cat",
            title = "法式极光猫眼渐变",
            subtitle = "法式微光 + 极光双色猫眼",
            imageRes = R.drawable.french_polar_cat,
        ),
        TemplateItem(
            id = "blush-firework-cat",
            title = "腮红渐变烟花猫眼",
            subtitle = "腮红打底 + 细闪烟花猫眼",
            imageRes = R.drawable.blush_firework_cat,
        ),
        TemplateItem(
            id = "polar-cat-french",
            title = "极光猫眼渐变法式",
            subtitle = "极光猫眼 + 经典法式边",
            imageRes = R.drawable.polar_cat_french,
        ),
        TemplateItem(
            id = "french-gradient-struct",
            title = "法式渐变建构",
            subtitle = "微晶建构 + 粉白优雅法式",
            imageRes = R.drawable.french_gradient_struct,
        ),
        TemplateItem(
            id = "pure-yellow",
            title = "单色纯欲黄",
            subtitle = "冰透柠檬黄 + 显白单色",
            imageRes = R.drawable.pure_yellow,
        ),
        TemplateItem(
            id = "milk-dot-french",
            title = "奶白波点法式",
            subtitle = "裸透底 + 奶白波点边",
            imageRes = R.drawable.nail_showcase_01,
        ),
        TemplateItem(
            id = "mist-blue-cat-eye",
            title = "雾蓝银线猫眼",
            subtitle = "灰蓝渐层 + 细银流线",
            imageRes = R.drawable.nail_showcase_02,
        ),
        TemplateItem(
            id = "star-dot-french",
            title = "奶白星点法式",
            subtitle = "星星贴片 + 细点法式",
            imageRes = R.drawable.nail_showcase_03,
        ),
        TemplateItem(
            id = "syrup-gloss",
            title = "豆沙糖霜镜面",
            subtitle = "半透豆沙 + 玻璃光泽",
            imageRes = R.drawable.nail_showcase_04,
        ),
    )

    fun matchUrlToTemplate(url: String): TemplateItem? {
        if (url.contains("download-1") || url.contains("polar-cat-matte")) return templates.find { it.id == "polar-cat-matte" }
        if (url.contains("download-2") || url.contains("french-polar-cat")) return templates.find { it.id == "french-polar-cat" }
        if (url.contains("download-3") || url.contains("blush-firework-cat")) return templates.find { it.id == "blush-firework-cat" }
        if (url.contains("download-4") || url.contains("polar-cat-french")) return templates.find { it.id == "polar-cat-french" }
        if (url.contains("download-5") || url.contains("french-gradient-struct")) return templates.find { it.id == "french-gradient-struct" }
        if (url.contains("download-0") || url.contains("pure-yellow") || url.contains("download.mp4")) return templates.find { it.id == "pure-yellow" }
        if (url.contains("milk-dot-french")) return templates.find { it.id == "milk-dot-french" }
        if (url.contains("mist-blue-cat-eye")) return templates.find { it.id == "mist-blue-cat-eye" }
        if (url.contains("star-dot-french")) return templates.find { it.id == "star-dot-french" }
        if (url.contains("syrup-gloss")) return templates.find { it.id == "syrup-gloss" }
        return null
    }

    val clipboardManager = LocalClipboardManager.current
    LaunchedEffect(Unit) {
        runCatching {
            val text = clipboardManager.getText()?.text
            if (!text.isNullOrBlank() && (text.startsWith("http") || text.contains("douyin.com") || text.contains("xiaohongshu.com") || text.contains("v.douyin.com") || text.contains("xhslink.com"))) {
                sniffedUrl = text
                val matched = matchUrlToTemplate(text)
                if (matched != null) {
                    sniffedTemplate = matched
                    showSniffedDialog = true
                } else {
                    sniffedTemplate = null
                    showSniffedDialog = true
                    isSniffing = true
                    delay(1500)
                    isSniffing = false
                }
            }
        }
    }

    val initialPage = remember(selectedId) {
        templates.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { templates.size })
    val currentTemplate = templates[pagerState.currentPage]

    LaunchedEffect(pagerState.currentPage) {
        onSelectId(templates[pagerState.currentPage].id)
    }

    val startDirectSop = remember(context, repository) {
        { url: String, templateId: String? ->
            scope.launch {
                statusText = null
                onOpenChat() // Navigate immediately to conversation screen

                NailSessionRuntime.backgroundScope.launch {
                    runCatching {
                        val installId = getInstallId(context)
                        val session = repository.createSession(
                            installId = installId,
                            sourceType = "short_video_link",
                        )
                        val remoteSessionId = session.session_id ?: error("createSession missing session_id")

                        NailSessionRuntime.current = NailSessionSnapshot(
                            sessionId = remoteSessionId,
                            sourceUrl = url,
                            sourceType = "short_video_link",
                            selectedTutorialId = templateId,
                            status = "booting",
                        )

                        repository.submitSourceLink(
                            sessionId = remoteSessionId,
                            sourceUrl = url,
                        )
                    }.onFailure { error ->
                        Log.e("HomeScreen", "[startDirectSop] Failure: ${error.message}", error)
                    }
                }
            }
        }
    }

    val startTryOnFlow = remember(context, repository, linkInput, currentTemplate) {
        { capturedBitmap: android.graphics.Bitmap ->
            HandPhotoRuntime.currentBitmap = capturedBitmap
            scope.launch {
                isCreating = true
                statusText = null
                val sourceUrl = linkInput.ifBlank { "preset://${currentTemplate.id}" }

                val localSessionId = "local-${UUID.randomUUID()}"
                val sourceType = if (linkInput.isNotBlank()) "short_video_link" else "preset"
                NailSessionRuntime.current = NailSessionSnapshot(
                    sessionId = localSessionId,
                    sourceUrl = sourceUrl,
                    sourceType = sourceType,
                    selectedTutorialId = currentTemplate.id,
                    status = "booting",
                    sourceParseJson = null,
                )
                onOpenFlow()

                NailSessionRuntime.backgroundScope.launch {
                    runCatching {
                        val installId = getInstallId(context)
                        val session = repository.createSession(
                            installId = installId,
                            sourceType = sourceType,
                        )
                        val remoteSessionId = session.session_id ?: error("createSession missing session_id")

                        val submitDeferred = async {
                            repository.submitSourceLink(
                                sessionId = remoteSessionId,
                                sourceUrl = sourceUrl,
                            )
                        }

                        val templateUploadDeferred = async {
                            uploadPresetTemplateIfNeeded(
                                context = context,
                                repository = repository,
                                remoteSessionId = remoteSessionId,
                                template = currentTemplate,
                            )
                        }

                        NailSessionRuntime.current = NailSessionRuntime.current?.copy(
                            sessionId = remoteSessionId,
                            status = session.status ?: "draft",
                        ) ?: NailSessionSnapshot(
                            sessionId = remoteSessionId,
                            sourceUrl = sourceUrl,
                            sourceType = sourceType,
                            selectedTutorialId = currentTemplate.id,
                            status = session.status ?: "draft",
                        )

                        val submit = submitDeferred.await()
                        templateUploadDeferred.await()
                        NailSessionRuntime.current = NailSessionRuntime.current?.copy(
                            status = submit.status ?: "source_parsing",
                        )
                    }.onFailure { error ->
                        val detailedError = "会话初始化失败: [${error::class.simpleName}] ${error.message}\n原因: ${error.cause?.message ?: "无"}\n堆栈: ${error.stackTrace.take(3).joinToString("\n")}"
                        Log.e("HomeScreen", "[createSession] Failure: $detailedError", error)
                        NailSessionRuntime.current = NailSessionRuntime.current?.copy(
                            status = "bootstrap_failed",
                        )
                        statusText = "初始化失败：${error.message ?: "未知错误"}\n请检查网络连接或稍后重试。"
                    }
                }

                isCreating = false
            }
        }
    }

    fun openCaptureForTryOn() {
        val sourceUrl = linkInput.ifBlank { "preset://${currentTemplate.id}" }
        val sourceType = if (linkInput.isNotBlank()) "short_video_link" else "preset"
        val localSessionId = "local-${UUID.randomUUID()}"
        HandPhotoRuntime.currentBitmap = null
        NailSessionRuntime.current = NailSessionSnapshot(
            sessionId = localSessionId,
            sourceUrl = sourceUrl,
            sourceType = sourceType,
            selectedTutorialId = currentTemplate.id,
            status = "booting",
            sourceParseJson = null,
        )
        onOpenFlow()

        NailSessionRuntime.backgroundScope.launch {
            runCatching {
                val installId = getInstallId(context)
                val session = repository.createSession(
                    installId = installId,
                    sourceType = sourceType,
                )
                val remoteSessionId = session.session_id ?: error("createSession missing session_id")

                val submitDeferred = async {
                    repository.submitSourceLink(
                        sessionId = remoteSessionId,
                        sourceUrl = sourceUrl,
                    )
                }

                val templateUploadDeferred = async {
                    uploadPresetTemplateIfNeeded(
                        context = context,
                        repository = repository,
                        remoteSessionId = remoteSessionId,
                        template = currentTemplate,
                    )
                }

                NailSessionRuntime.current = NailSessionRuntime.current?.copy(
                    sessionId = remoteSessionId,
                    status = session.status ?: "draft",
                ) ?: NailSessionSnapshot(
                    sessionId = remoteSessionId,
                    sourceUrl = sourceUrl,
                    sourceType = sourceType,
                    selectedTutorialId = currentTemplate.id,
                    status = session.status ?: "draft",
                )

                val submit = submitDeferred.await()
                templateUploadDeferred.await()
                NailSessionRuntime.current = NailSessionRuntime.current?.copy(
                    status = submit.status ?: "source_parsing",
                )
            }.onFailure { error ->
                val detailedError = "会话初始化失败: [${error::class.simpleName}] ${error.message}\n原因: ${error.cause?.message ?: "无"}\n堆栈: ${error.stackTrace.take(3).joinToString("\n")}"
                Log.e("HomeScreen", "[openCaptureForTryOn] Failure: $detailedError", error)
                NailSessionRuntime.current = NailSessionRuntime.current?.copy(
                    status = "bootstrap_failed",
                )
                statusText = "初始化失败：${error.message ?: "未知错误"}\n请检查网络连接或稍后重试。"
            }
        }
    }

    val inspirationPreview = listOf(
        templates[0],
        templates[1],
        templates[6],
        templates[8],
    )

    Scaffold(containerColor = EntryBg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "指尖 SOP",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = EntryTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFD4A3A3),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = "试戴看效果 · AI 拆解教程 · 跟着做",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EntryBody,
                            fontSize = 14.sp,
                        )
                    )
                }
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { statusText = "历史记录能力已预留，后面可接最近解析记录。" }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = EntryTitle.copy(alpha = 0.78f),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "历史记录",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EntryBody,
                            ),
                        )
                    }
                }
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0x22D4A3A3), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFFD4A3A3),
                            modifier = Modifier.size(18.dp),
                        )
                        BasicTextField(
                            value = linkInput,
                            onValueChange = { linkInput = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = EntryTitle,
                                fontSize = 14.sp,
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (linkInput.isBlank()) {
                                    Text(
                                        text = "粘贴美甲教程链接，或者直接试戴当前这一款",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = EntryBody,
                                            fontSize = 13.sp,
                                        )
                                    )
                                }
                                inner()
                            }
                        )
                        Button(
                            onClick = {
                                if (!isCreating) {
                                    openCaptureForTryOn()
                                }
                            },
                            enabled = !isCreating,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EntryAccent,
                                contentColor = Color.White,
                            ),
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Text("开始试戴")
                            }
                        }
                    }
                    Text(
                        text = "支持抖音、小红书等短视频链接，也可以直接从当前模板开始试戴。",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EntryBody,
                            lineHeight = 18.sp,
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CapabilityStat(
                        icon = Icons.Default.Description,
                        label = "教程解析",
                        tint = Color(0xFFC97B7B),
                        bg = Color(0xFFFBE9E9),
                    )
                    CapabilityDivider()
                    CapabilityStat(
                        icon = Icons.Default.AutoAwesome,
                        label = "智能拆解",
                        tint = Color(0xFF7A6FB5),
                        bg = Color(0xFFECE9F7),
                    )
                    CapabilityDivider()
                    CapabilityStat(
                        icon = Icons.Default.PhotoLibrary,
                        label = "试戴识别",
                        tint = Color(0xFF5A9B7A),
                        bg = Color(0xFFE3F1E8),
                    )
                }
            }

            HomeSectionCard(
                title = "教程解析示例",
                action = "直接开始",
                onAction = { openCaptureForTryOn() },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("修形", "底胶", "色胶", "照灯", "封层", "精修").forEachIndexed { index, item ->
                            StepChip(
                                index = index + 1,
                                label = item,
                                active = index == 0,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { openCaptureForTryOn() },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.home_nail_reference),
                            contentDescription = "教程解析示例",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.16f),
                                        )
                                    )
                                )
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.align(Alignment.Center),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = EntryTitle,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                    Text(
                        text = "看教程 · 贴链接 · 跟着做",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EntryTitle,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }

            HomeSectionCard(
                title = "热门美甲灵感",
                action = "更多灵感",
                onAction = onOpenInspiration,
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    inspirationPreview.forEach { item ->
                        Column(
                            modifier = Modifier
                                .size(width = 104.dp, height = 134.dp)
                                .clickable {
                                    onSelectId(item.id)
                                    onOpenInspiration()
                                },
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Image(
                                painter = painterResource(item.imageRes),
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(102.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = EntryTitle,
                                ),
                                maxLines = 2,
                            )
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 18.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                val scale = 1f - (pageOffset.coerceIn(0f, 1f) * 0.08f)
                val alpha = 1f - (pageOffset.coerceIn(0f, 1f) * 0.28f)
                val template = templates[page]

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .scale(scale)
                        .alpha(alpha)
                        .clip(RoundedCornerShape(34.dp)),
                ) {
                    Image(
                        painter = painterResource(template.imageRes),
                        contentDescription = template.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.22f),
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        )
                        Text(
                            text = template.subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.86f),
                            )
                        )
                    }
                }
            }

            if (!statusText.isNullOrBlank() && (isCreating || statusText?.contains("失败") == true || statusText?.contains("重试") == true)) {
                Text(
                    text = statusText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EntryBody,
                        lineHeight = 18.sp,
                    )
                )
            }

            if (showSniffedDialog) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showSniffedDialog = false }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(EntryBg)
                            .border(1.dp, EntrySoft, RoundedCornerShape(28.dp))
                            .padding(24.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✨ 智能美甲识别",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = EntryTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            )

                            if (isSniffing) {
                                Column(
                                    modifier = Modifier.padding(vertical = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = EntryAccent,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "正在智能嗅探视频款式...",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = EntryBody)
                                    )
                                }
                            } else {
                                val title = sniffedTemplate?.title ?: "极光冰透猫眼 [新视频]"
                                val subtitle = sniffedTemplate?.subtitle ?: "半透粉底 + 极光双色猫眼"
                                val imageRes = sniffedTemplate?.imageRes ?: R.drawable.nail_showcase_02

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        painter = painterResource(imageRes),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(140.dp)
                                            .clip(RoundedCornerShape(20.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = EntryTitle,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = EntryBody,
                                                textAlign = TextAlign.Center
                                            )
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            showSniffedDialog = false
                                            linkInput = sniffedUrl
                                            openCaptureForTryOn()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EntryAccent,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(
                                            text = "一键 AI 试戴",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            showSniffedDialog = false
                                            val targetTemplateId = sniffedTemplate?.id ?: "aurora-cat"
                                            startDirectSop(sniffedUrl, targetTemplateId)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EntrySoft,
                                            contentColor = EntryTitle
                                        )
                                    ) {
                                        Text(
                                            text = "直接看 SOP (带做)",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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

@Composable
private fun HomeSectionCard(
    title: String,
    action: String,
    onAction: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = EntryTitle,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Row(
                        modifier = Modifier.clickable(onClick = onAction),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = action,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = EntryBody,
                            ),
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = EntryBody,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                content()
            }
        )
    }
}

@Composable
private fun CapabilityStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    bg: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = EntryTitle.copy(alpha = 0.8f),
            ),
        )
    }
}

@Composable
private fun CapabilityDivider() {
    Spacer(
        modifier = Modifier
            .size(width = 1.dp, height = 24.dp)
            .background(Color(0xFFE9E0D9))
    )
}

@Composable
private fun StepChip(
    index: Int,
    label: String,
    active: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(end = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (active) EntryAccent else EntrySoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (active) Color.White else EntryBody,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (active) EntryTitle else EntryBody,
            ),
        )
    }
}

private fun getInstallId(context: Context): String {
    val prefs = context.getSharedPreferences("nailit-local", Context.MODE_PRIVATE)
    return prefs.getString("install_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("install_id", it).apply()
    }
}

private suspend fun uploadPresetTemplateIfNeeded(
    context: Context,
    repository: SupabaseFunctionRepository,
    remoteSessionId: String,
    template: TemplateItem,
) {
    val upload = repository.prepareAssetUpload(
        sessionId = remoteSessionId,
        assetType = "tutorial_frame",
        mimeType = "image/jpeg",
    )
    val uploadPath = upload.storage_path ?: return
    val assetId = upload.asset_id ?: return
    val bytes = templateBitmapBytes(context, template.imageRes) ?: return
    SupabaseManager.uploadHandPhotoToPath(bytes, uploadPath)
    repository.confirmAssetUpload(
        sessionId = remoteSessionId,
        assetId = assetId,
        assetType = "tutorial_frame",
        storagePath = uploadPath,
    )
}

private fun templateBitmapBytes(context: Context, imageRes: Int): ByteArray? {
    val bitmap = BitmapFactory.decodeResource(context.resources, imageRes) ?: return null
    val output = ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, output)
    return output.toByteArray()
}
