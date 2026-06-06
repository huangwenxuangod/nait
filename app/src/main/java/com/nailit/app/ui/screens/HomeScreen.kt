package com.nailit.app.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import android.util.Log
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.R
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.SupabaseFunctionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SupabaseFunctionRepository() }
    var linkInput by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    val templates = listOf(
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

    val initialPage = remember(selectedId) {
        templates.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { templates.size })
    val currentTemplate = templates[pagerState.currentPage]

    LaunchedEffect(pagerState.currentPage) {
        onSelectId(templates[pagerState.currentPage].id)
    }

    val createSession = remember(context, repository, linkInput, isCreating, currentTemplate) {
        {
            if (isCreating) return@remember
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

    Scaffold(containerColor = EntryBg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Nail-It",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = EntryTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                    )
                )
                Text(
                    text = "先看效果，再决定要不要做。",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = EntryBody,
                        fontSize = 15.sp,
                    )
                )
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
                        .height(500.dp)
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

            BasicTextField(
                value = linkInput,
                onValueChange = { linkInput = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = EntryTitle,
                    fontSize = 15.sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(EntrySoft)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                decorationBox = { inner ->
                    if (linkInput.isBlank()) {
                        Text(
                            text = "粘贴教程链接，或者直接试戴当前这一款",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = EntryBody,
                            )
                        )
                    }
                    inner()
                }
            )

            Button(
                onClick = createSession,
                enabled = !isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EntryAccent,
                    contentColor = Color.White,
                )
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text(
                        text = "开始试戴",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                        )
                    )
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
        }
    }
}

private fun getInstallId(context: Context): String {
    val prefs = context.getSharedPreferences("nailit-local", Context.MODE_PRIVATE)
    return prefs.getString("install_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("install_id", it).apply()
    }
}
