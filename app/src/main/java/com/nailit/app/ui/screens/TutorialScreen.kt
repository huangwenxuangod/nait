package com.nailit.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.SupabaseFunctionRepository
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    selectedId: String?,
    sessionSnapshot: NailSessionSnapshot?,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val repository = remember { SupabaseFunctionRepository() }
    var isParsing by remember { mutableStateOf(sessionSnapshot?.sourceParseJson == null) }
    var parseProgress by remember { mutableStateOf(if (sessionSnapshot?.sourceParseJson != null) 1.0f else 0f) }
    var currentLog by remember { mutableStateOf("正在读取视频流...") }

    val scrollState = rememberScrollState()

    // Determine fallback/preset title
    val presetTitle = when (selectedId) {
        "aurora-cat" -> "极光冰透猫眼"
        "french-soft" -> "法式温柔渐变"
        "retro-wine" -> "复古红酒排钻"
        "pearl-milk" -> "清新花卉款"
        "golden-amber" -> "气质豆沙款"
        else -> "极光冰透猫眼"
    }

    // Parse logic
    LaunchedEffect(sessionSnapshot) {
        if (sessionSnapshot == null) {
            // No session, simulate parsing of preset
            isParsing = true
            parseProgress = 0.1f
            currentLog = "正在加载预设款式..."
            delay(400)
            parseProgress = 0.5f
            currentLog = "正在提取款式标签..."
            delay(500)
            parseProgress = 1.0f
            currentLog = "预设加载完成"
            delay(200)
            isParsing = false
            return@LaunchedEffect
        }

        if (sessionSnapshot.sourceParseJson != null) {
            // Already cached
            isParsing = false
            parseProgress = 1.0f
            return@LaunchedEffect
        }

        // Has session but no cache, fetch from remote database
        isParsing = true
        parseProgress = 0.2f
        currentLog = "正在连接 Supabase 数据库..."
        delay(300)
        parseProgress = 0.4f
        currentLog = "正在查询款式解析任务..."

        var fetched: JsonObject? = null
        runCatching {
            // Try up to 3 times to fetch (polling in case background AI is finishing up)
            for (i in 1..3) {
                fetched = repository.fetchSourceParse(sessionSnapshot.sessionId)
                if (fetched != null) break
                parseProgress = 0.4f + (i * 0.15f)
                currentLog = "AI 正在提取款式标签与工艺大纲 (第 $i 次尝试)..."
                delay(1200)
            }
        }

        if (fetched != null) {
            parseProgress = 1.0f
            currentLog = "AI 款式解析已就绪！"
            delay(300)
            NailSessionRuntime.current = sessionSnapshot.copy(sourceParseJson = fetched)
            isParsing = false
        } else {
            // Fallback to mock data if database fetch returns null (offline or mock mode)
            parseProgress = 0.8f
            currentLog = "未检测到后端解析记录，正为您加载本地精美预设..."
            delay(1000)
            parseProgress = 1.0f
            isParsing = false
        }
    }

    // Resolve display data
    val activeParse = sessionSnapshot?.sourceParseJson
    val displayTitle = activeParse?.string("style_name", presetTitle) ?: presetTitle
    val displayTags = activeParse?.stringList("style_tags")?.ifEmpty {
        when (selectedId) {
            "aurora-cat" -> listOf("极光猫眼", "冰透水光", "斜吸45度", "显白暖色")
            "french-soft" -> listOf("经典法式", "温柔百搭", "微宽白边", "拉线细节")
            else -> listOf("复古单色", "奢华排钻", "填缝工艺", "超亮封层")
        }
    } ?: when (selectedId) {
        "aurora-cat" -> listOf("极光猫眼", "冰透水光", "斜吸45度", "显白暖色")
        "french-soft" -> listOf("经典法式", "温柔百搭", "微宽白边", "拉线细节")
        else -> listOf("复古单色", "奢华排钻", "填缝工艺", "超亮封层")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI 视频款式解析",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (isParsing) {
                // Parsing State
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { parseProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF881337),
                            strokeWidth = 3.dp,
                            trackColor = Color(0xFFE9D8D0)
                        )
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Parsing",
                            tint = Color(0xFFC5A880),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        displayTitle.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        currentLog,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )
                }
            } else {
                // Parsed Results
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Header
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "解析款式：" + displayTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            "AI 已将教程会话推进到结构化解析阶段",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        )
                        sessionSnapshot?.let { snapshot ->
                            Text(
                                "Session: ${snapshot.sessionId.take(8)}... | Source: ${snapshot.sourceType} | Status: ${snapshot.status}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF881337),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Tags Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE9D8D0))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "款式特征标签",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC5A880)
                                )
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                displayTags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFAF8F5))
                                            .border(1.dp, Color(0xFFE9D8D0), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            tag,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = Color(0xFF881337)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Step Outline
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "SOP 步骤大纲",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        )

                        val stepsList = activeParse?.get("steps") as? JsonArray
                        if (stepsList != null && stepsList.isNotEmpty()) {
                            stepsList.forEachIndexed { index, item ->
                                val stepObj = item.jsonObject
                                val sTitle = stepObj.string("title", "未命名步骤")
                                val sTime = stepObj.string("timer_seconds", "0")
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(0.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE9D8D0).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(0xFFE9D8D0), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                (index + 1).toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF881337)
                                                )
                                            )
                                        }
                                        Text(
                                            "$sTitle (照灯 ${sTime}s)",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            // Fallback steps
                            val steps = when (selectedId) {
                                "aurora-cat" -> listOf(
                                    "步骤 1: 基础修甲与平衡底胶 (照灯30s)",
                                    "步骤 2: 涂冰透粉色背景胶两层 (每层照灯60s)",
                                    "步骤 3: 涂极光猫眼胶，磁铁斜吸45度 (照灯60s)",
                                    "步骤 4: 涂超亮钢化封层 (照灯90s)"
                                )
                                "french-soft" -> listOf(
                                    "步骤 1: 基础修甲与粘合底胶 (照灯30s)",
                                    "步骤 2: 涂裸粉色打底胶 (照灯60s)",
                                    "步骤 3: 使用拉线笔勾勒法式微宽白边 (照灯60s)",
                                    "步骤 4: 涂免洗免封层照灯 (照灯90s)"
                                )
                                else -> listOf(
                                    "步骤 1: 基础修甲与底胶 (照灯30s)",
                                    "步骤 2: 涂红酒单色胶两层 (每层照灯60s)",
                                    "步骤 3: 使用粘钻胶在指甲根部排钻 (照灯60s)",
                                    "步骤 4: 填缝与全甲封层 (照灯90s)"
                                )
                            }

                            steps.forEachIndexed { index, step ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(0.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE9D8D0).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(0xFFE9D8D0), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                (index + 1).toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF881337)
                                                )
                                            )
                                        }
                                        Text(
                                            step,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Continue Button
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                onClick = onContinue,
                enabled = !isParsing,
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "进行 AI 虚拟试戴",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}

private fun JsonObject.string(key: String, fallback: String): String {
    return (this[key] as? JsonPrimitive)?.contentOrNull ?: fallback
}

private fun JsonObject.stringList(key: String): List<String> {
    return (this[key] as? JsonArray)?.mapNotNull { item ->
        (item as? JsonPrimitive)?.contentOrNull
    } ?: emptyList()
}
