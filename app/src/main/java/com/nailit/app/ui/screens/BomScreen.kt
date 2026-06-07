package com.nailit.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.core.network.SupabaseManager
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.SupabaseFunctionRepository
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private val PrepareBg = Color(0xFFFCFAF8)
private val PrepareText = Color(0xFF1F1A17)
private val PrepareMuted = Color(0xFF8C817A)
private val PrepareLine = Color(0xFFF1E7E2)
private val PrepareAccent = Color(0xFFE9B7AE)
private val PrepareAccentStrong = Color(0xFFDFA297)
private val PrepareCard = Color(0xFFFFFFFF)
private val PrepareSoft = Color(0xFFFFF7F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BomScreen(
    sessionSnapshot: NailSessionSnapshot?,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val repository = remember { SupabaseFunctionRepository() }
    val runtimeSession = NailSessionRuntime.current ?: sessionSnapshot
    val activeSession = NailSessionRuntime.current ?: sessionSnapshot
    val scrollState = rememberScrollState()

    var isLoading by remember { mutableStateOf(runtimeSession != null && runtimeSession.bomJson == null) }
    var isGuidePreparing by remember { mutableStateOf(runtimeSession?.executionStatus == "guide_pending") }
    var isGuideReady by remember { mutableStateOf(runtimeSession?.executionSteps?.isNotEmpty() == true) }
    var targetBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(runtimeSession?.sessionId) {
        val session = NailSessionRuntime.current ?: sessionSnapshot
        if (session == null) {
            isLoading = false
            isGuidePreparing = false
            return@LaunchedEffect
        }

        if (session.executionStatus != "guide_pending" && session.executionSteps.isEmpty()) {
            isGuidePreparing = true
            NailSessionRuntime.current = session.copy(
                executionStatus = "guide_pending",
                executionError = null,
            )

            runCatching {
                repository.generateExecutionPackage(session.sessionId)
            }.onFailure { error ->
                NailSessionRuntime.current = (NailSessionRuntime.current ?: session).copy(
                    executionStatus = "guide_failed",
                    executionError = error.message,
                )
                isGuidePreparing = false
            }
        } else {
            isGuidePreparing = session.executionStatus == "guide_pending"
            isGuideReady = session.executionSteps.isNotEmpty()
        }
    }

    LaunchedEffect(runtimeSession?.sessionId, runtimeSession?.executionStatus) {
        val session = NailSessionRuntime.current ?: sessionSnapshot ?: return@LaunchedEffect
        if (session.executionSteps.isNotEmpty()) {
            isGuidePreparing = false
            isGuideReady = true
            return@LaunchedEffect
        }

        if (session.executionStatus == "guide_pending") {
            repeat(20) {
                val executionPackage = repository.fetchExecutionPackage(session.sessionId)
                if (executionPackage != null && executionPackage.steps.isNotEmpty()) {
                    NailSessionRuntime.current = (NailSessionRuntime.current ?: session).copy(
                        executionStatus = "guide_ready",
                        estimatedTotalMinutes = executionPackage.estimated_total_minutes,
                        currentStepIndex = 0,
                        currentStepTitle = executionPackage.steps.firstOrNull()?.title,
                        executionSteps = executionPackage.steps,
                        executionError = null,
                    )
                    isGuidePreparing = false
                    isGuideReady = true
                    return@LaunchedEffect
                }
                delay(1000)
            }
            isGuidePreparing = false
        }
    }

    LaunchedEffect(runtimeSession?.sessionId) {
        val session = NailSessionRuntime.current ?: sessionSnapshot
        if (session == null) {
            isLoading = false
            return@LaunchedEffect
        }

        val fetched = repository.fetchBom(session.sessionId)
        if (fetched != null) {
            NailSessionRuntime.current = session.copy(bomJson = fetched)
        }
        isLoading = false
    }

    LaunchedEffect(activeSession?.targetImagePath) {
        targetBitmap = activeSession?.targetImagePath?.let { loadTargetBitmap(it) }
    }

    val activeBom = (NailSessionRuntime.current ?: sessionSnapshot)?.bomJson
    val styleName = activeSession?.sourceParseJson?.stringValue("style_name")
        ?: activeSession?.executionSteps?.firstOrNull()?.title
        ?: "极光猫眼渐变磨砂"

    val estimatedMinutes = activeSession?.estimatedTotalMinutes?.takeIf { it > 0 }
        ?: estimateMinutesFromSteps(activeSession?.executionSteps.orEmpty())
    val totalSteps = activeSession?.executionSteps?.size?.takeIf { it > 0 } ?: 6
    val lampSteps = activeSession?.executionSteps?.count { it.needs_timer }?.takeIf { it > 0 } ?: 2

    val materials = buildMaterialList(activeBom)
    val substitutes = activeBom?.stringList("optional_substitutes")
        ?.takeIf { it.isNotEmpty() }
        ?: listOf("裸粉底色可以先用手边接近色替代", "没有双头磁铁也能先看流程顺序")

    val bottomLabel = when {
        isGuidePreparing -> "AI 正在排顺序..."
        isGuideReady -> "开始跟做"
        else -> "准备中..."
    }

    Scaffold(
        containerColor = PrepareBg,
        topBar = {
            Surface(color = PrepareBg) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrepareText,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "准备开始",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PrepareText,
                        ),
                    )
                }
            }
        },
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrepareAccentStrong, strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在准备这款的开做信息",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = PrepareMuted,
                        ),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "开始这款",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrepareText,
                                letterSpacing = (-0.8).sp,
                            ),
                        )
                        Text(
                            text = styleName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = PrepareMuted,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }

                    PrepareHeroCard(
                        bitmap = targetBitmap,
                        styleName = styleName,
                        estimatedMinutes = estimatedMinutes,
                        totalSteps = totalSteps,
                        lampSteps = lampSteps,
                    )

                    PrepareInfoCard(
                        title = "AI 已帮你排好顺序",
                        body = if (isGuideReady) {
                            "从修形、底胶到主视觉高光，接下来按页跟着做就可以。"
                        } else {
                            "顺序已经在准备中，你确认完材料就能直接进入跟做。"
                        },
                        bullets = listOf(
                            "照灯步骤会自动提醒",
                            "支持语音说“下一步”",
                            "中途可以暂停再继续",
                        ),
                        trailing = {
                            if (isGuidePreparing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = PrepareAccentStrong,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            color = PrepareSoft,
                                            shape = CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isGuideReady) PrepareAccentStrong else PrepareMuted,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                    )

                    PrepareMaterialCard(
                        title = "先确认手边材料",
                        body = "不用像任务清单一样一条条勾完，确认手边大致齐就可以开始。",
                        items = materials,
                    )

                    PrepareMaterialCard(
                        title = "还可以这样替代",
                        body = "如果有几样不完全一致，也可以先进入流程看做法。",
                        items = substitutes,
                        tinted = true,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onContinue,
                    enabled = !isGuidePreparing && isGuideReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrepareAccentStrong,
                        disabledContainerColor = PrepareLine,
                        contentColor = Color.White,
                        disabledContentColor = PrepareMuted,
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                ) {
                    Text(
                        text = bottomLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrepareHeroCard(
    bitmap: Bitmap?,
    styleName: String,
    estimatedMinutes: Int,
    totalSteps: Int,
    lampSteps: Int,
) {
    Surface(
        color = PrepareCard,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PrepareSoft),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = styleName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    PrepareImagePlaceholder()
                }
            }

            Text(
                text = styleName,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = PrepareText,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetaChip(
                    icon = Icons.Default.Timer,
                    label = "约 ${estimatedMinutes.coerceAtLeast(12)} 分钟",
                )
                MetaChip(
                    icon = Icons.Default.PlayArrow,
                    label = "$totalSteps 步完成",
                )
                MetaChip(
                    icon = Icons.Default.Check,
                    label = "含 $lampSteps 次照灯",
                )
            }
        }
    }
}

@Composable
private fun PrepareInfoCard(
    title: String,
    body: String,
    bullets: List<String>,
    trailing: @Composable () -> Unit,
) {
    Surface(
        color = PrepareCard,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 4.dp,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PrepareText,
                        ),
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = PrepareMuted,
                            lineHeight = 22.sp,
                        ),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                trailing()
            }

            bullets.forEach { bullet ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(PrepareAccent, CircleShape),
                    )
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = PrepareText,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrepareMaterialCard(
    title: String,
    body: String,
    items: List<String>,
    tinted: Boolean = false,
) {
    Surface(
        color = if (tinted) PrepareSoft else PrepareCard,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = if (tinted) 0.dp else 4.dp,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PrepareText,
                    ),
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = PrepareMuted,
                        lineHeight = 22.sp,
                    ),
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items.forEach { item ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = PrepareLine,
                            shape = RoundedCornerShape(18.dp),
                        ),
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = PrepareText,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Surface(
        color = PrepareSoft,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = PrepareAccentStrong,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = PrepareText,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun PrepareImagePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF5F2), Color(0xFFF8EEE8)),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color(0x14DFA297),
                topLeft = Offset(size.width * 0.10f, size.height * 0.18f),
                size = Size(size.width * 0.36f, size.height * 0.58f),
                cornerRadius = CornerRadius(80f, 80f),
            )
            drawRoundRect(
                color = Color(0x10E9B7AE),
                topLeft = Offset(size.width * 0.54f, size.height * 0.10f),
                size = Size(size.width * 0.24f, size.height * 0.30f),
                cornerRadius = CornerRadius(60f, 60f),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color.White.copy(alpha = 0.92f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = PrepareAccentStrong,
                )
            }
            Text(
                text = "试戴结果会显示在这里",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = PrepareText,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = "没有图的时候，先用这张柔和占位保持页面呼吸感",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = PrepareMuted,
                    lineHeight = 20.sp,
                ),
            )
        }
    }
}

private fun buildMaterialList(bom: JsonObject?): List<String> {
    val basicTools = bom?.stringList("basic_tools").orEmpty()
    val styleItems = bom?.stringList("style_specific_items").orEmpty()
    val combined = (basicTools + styleItems).map { item ->
        item.substringBefore(" (").substringBefore("（")
    }.distinct()

    if (combined.isNotEmpty()) return combined.take(6)

    return listOf(
        "底胶",
        "裸粉底色胶",
        "猫眼胶",
        "磁铁",
        "封层",
        "烤灯",
    )
}

private fun estimateMinutesFromSteps(steps: List<com.nailit.app.core.model.ExecutionStep>): Int {
    if (steps.isEmpty()) return 18
    val totalSeconds = steps.sumOf { it.duration_sec.coerceAtLeast(30) }
    return (totalSeconds / 60).coerceAtLeast(12)
}

private fun JsonObject.stringList(key: String): List<String> {
    return (this[key] as? JsonArray)?.mapNotNull { item ->
        (item as? JsonPrimitive)?.contentOrNull
    } ?: emptyList()
}

private fun JsonObject.stringValue(key: String): String? {
    return (this[key] as? JsonPrimitive)?.contentOrNull
}

private suspend fun loadTargetBitmap(storagePath: String): Bitmap? {
    return runCatching {
        val bytes = SupabaseManager.client.storage
            .from("nail-it-assets")
            .downloadAuthenticated(storagePath)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
