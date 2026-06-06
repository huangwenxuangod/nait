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
import com.nailit.app.core.preview.NailSessionSnapshot
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    selectedId: String?,
    sessionSnapshot: NailSessionSnapshot?,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var isParsing by remember { mutableStateOf(true) }
    var parseProgress by remember { mutableStateOf(0f) }
    var currentLog by remember { mutableStateOf("正在读取视频流...") }

    val scrollState = rememberScrollState()

    val title = when (selectedId) {
        "aurora-cat" -> "极光冰透猫眼"
        "french-soft" -> "法式温柔渐变"
        else -> "复古红酒排钻"
    }

    // Parse simulation
    LaunchedEffect(Unit) {
        delay(500)
        parseProgress = 0.3f
        currentLog = "已提交教程来源，正在提取款式标签..."
        delay(600)
        parseProgress = 0.7f
        currentLog = "正在重构工艺顺序与烤灯参数..."
        delay(700)
        parseProgress = 1.0f
        currentLog = "后端解析占位结果已写入，指尖 SOP 可以继续推进。"
        delay(400)
        isParsing = false
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
                        title.uppercase(),
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
                            "解析款式：" + title,
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
                                "Session: ${snapshot.sessionId} | Source: ${snapshot.sourceType} | Status: ${snapshot.status}",
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
                                listOf("极光猫眼", "冰透水光", "斜吸45度", "显白暖色").forEach { tag ->
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
