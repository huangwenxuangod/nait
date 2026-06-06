package com.nailit.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.SupabaseFunctionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SopScreen(
    sessionSnapshot: NailSessionSnapshot?,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val repository = remember { SupabaseFunctionRepository() }
    val scope = rememberCoroutineScope()
    val runtimeSession = NailSessionRuntime.current ?: sessionSnapshot
    
    var isLoading by remember { mutableStateOf(runtimeSession != null && runtimeSession.sopJson == null) }
    var currentStep by remember {
        mutableStateOf(((runtimeSession?.currentStepIndex ?: 0) + 1).coerceAtLeast(1))
    }
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(60) }
    var showCompletionFeedback by remember { mutableStateOf(false) }

    // Load SOP logic
    LaunchedEffect(sessionSnapshot) {
        if (sessionSnapshot == null) {
            isLoading = false
            return@LaunchedEffect
        }

        if (sessionSnapshot.sopJson != null) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        var fetched: JsonObject? = null
        runCatching {
            // Try up to 3 times to fetch (polling in case background AI is finishing up)
            for (i in 1..3) {
                fetched = repository.fetchSop(sessionSnapshot.sessionId)
                if (fetched != null) break
                delay(1000)
            }
        }

        if (fetched != null) {
            NailSessionRuntime.current = sessionSnapshot.copy(sopJson = fetched)
        }
        isLoading = false
    }

    val activeSop = sessionSnapshot?.sopJson
    val rawSteps = activeSop?.get("steps") as? JsonArray

    // Convert to structured steps
    data class SopStep(
        val index: Int,
        val title: String,
        val instruction: String,
        val timerSeconds: Int,
        val voiceShortcut: String
    )

    val steps = remember(rawSteps) {
        if (rawSteps != null && rawSteps.isNotEmpty()) {
            rawSteps.mapNotNull { item ->
                val obj = item.jsonObject
                val idx = (obj["index"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 1
                val title = (obj["title"] as? JsonPrimitive)?.contentOrNull ?: "未命名步骤"
                val instruction = (obj["instruction"] as? JsonPrimitive)?.contentOrNull ?: "暂无说明"
                val timer = (obj["timer_seconds"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 60
                val voice = (obj["voice_shortcut"] as? JsonPrimitive)?.contentOrNull ?: "下一步"
                SopStep(idx, title, instruction, timer, voice)
            }
        } else {
            // Fallback preset steps
            listOf(
                SopStep(1, "基础修甲与平衡底胶", "使用死皮推清理甲面，薄涂一层平衡液与强力底胶，确保边缘包裹完整。", 30, "下一步"),
                SopStep(2, "极光猫眼斜吸工艺", "均匀涂上极光猫眼胶。拿起磁铁，斜放在指甲边缘 45 度角（距离指甲约 0.5 厘米），停留 5 秒钟，观察高光线向中心汇聚，吸出宽光后立刻放入烤灯固化！", 60, "下一步"),
                SopStep(3, "涂冰透粉色背景胶两层", "薄涂冰透粉色底色胶，每层均匀照灯，形成饱满透亮的水光质感。", 60, "下一步"),
                SopStep(4, "超亮钢化封层", "涂抹免洗钢化封层，加固耐磨，锁住晶莹猫眼光泽，边缘重点包裹。", 90, "完成")
            )
        }
    }

    val totalSteps = steps.size
    val currentStepData = steps.getOrNull((currentStep - 1).coerceIn(0, totalSteps - 1))

    // Sync timer initial seconds when step changes
    LaunchedEffect(currentStep, currentStepData) {
        isTimerRunning = false
        timerSeconds = currentStepData?.timerSeconds ?: 60
        val active = NailSessionRuntime.current ?: sessionSnapshot
        if (active != null && currentStepData != null) {
            NailSessionRuntime.current = active.copy(
                currentStepIndex = (currentStep - 1).coerceAtLeast(0),
                currentStepTitle = currentStepData.title,
                sopJson = active.sopJson ?: activeSop,
            )
        }
    }

    // Doubao Voice Orb Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    val orbPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // UV Lamp timer countdown
    LaunchedEffect(isTimerRunning, timerSeconds) {
        if (isTimerRunning && timerSeconds > 0) {
            delay(1000)
            timerSeconds--
        } else if (timerSeconds == 0) {
            isTimerRunning = false
            timerSeconds = currentStepData?.timerSeconds ?: 60
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showCompletionFeedback) "AI 大师级实操反馈" else "沉浸式实操 SOP (第 $currentStep/$totalSteps 步)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showCompletionFeedback) {
                            showCompletionFeedback = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Anywhere tap on screen to progress (contactless simulation when hands are wet)
                .clickable {
                    if (!showCompletionFeedback && !isLoading) {
                        if (currentStep < totalSteps) {
                            currentStep++
                        } else {
                            showCompletionFeedback = true
                        }
                    }
                }
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFC5A880))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "正在加载 AI 专属实操指南...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f))
                        )
                    }
                }
            } else if (!showCompletionFeedback) {
                // Real-time Hand-Tracking Camera View (Simulated)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF151413)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "实时手部骨骼与甲床追踪中",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                // Simulated Nail Overlay skeletal lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val color = Color(0xFFC5A880).copy(alpha = 0.4f)
                    val center = Offset(size.width / 2, size.height * 0.35f)
                    val strokeWidth = 1.5.dp.toPx()

                    // Draw hand palm boundaries
                    drawCircle(
                        color = color,
                        center = center,
                        radius = 80.dp.toPx(),
                        style = Stroke(width = strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )

                    // Draw 5 finger outline skeletons
                    val fingerTips = listOf(
                        Offset(center.x - 100.dp.toPx(), center.y - 120.dp.toPx()), // Thumb
                        Offset(center.x - 50.dp.toPx(), center.y - 180.dp.toPx()),  // Index
                        Offset(center.x, center.y - 200.dp.toPx()),                // Middle
                        Offset(center.x + 50.dp.toPx(), center.y - 170.dp.toPx()),  // Ring
                        Offset(center.x + 100.dp.toPx(), center.y - 110.dp.toPx())  // Pinky
                    )

                    fingerTips.forEach { tip ->
                        drawLine(
                            color = color,
                            start = center,
                            end = tip,
                            strokeWidth = strokeWidth
                        )
                        drawCircle(
                            color = Color(0xFF881337).copy(alpha = 0.6f),
                            radius = 6.dp.toPx(),
                            center = tip
                        )
                    }
                }

                // Doubao-style Voice Call Orb (Floating Top-Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0xFFC5A880).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pulsing Glowing Voice Orb
                        Box(contentAlignment = Alignment.Center) {
                            // Outer pulsing glow
                            Box(
                                modifier = Modifier
                                    .size((14 * orbPulseScale).dp)
                                    .background(Color(0xFF881337).copy(alpha = 1f - orbAlpha), CircleShape)
                            )
                            // Inner solid core
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF881337), CircleShape)
                            )
                        }
                        
                        Column {
                            Text(
                                "语音助手已就绪",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFC5A880),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                            Text(
                                "说“${currentStepData?.voiceShortcut ?: "下一步"}”或轻拍屏幕翻页",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 8.sp
                                )
                            )
                        }
                    }
                }

                // Floating Step Guidance Card (Bottom Half)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // UV Lamp Timer Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1917).copy(alpha = 0.9f))
                            .border(1.dp, Color(0xFFC5A880).copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isTimerRunning) Color.Green else Color.Gray, CircleShape)
                                )
                                Text(
                                    if (isTimerRunning) "UV 烤灯固化中..." else "UV 烤灯就绪",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    isTimerRunning = !isTimerRunning
                                },
                                shape = RoundedCornerShape(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTimerRunning) Color(0xFF881337) else Color(0xFFC5A880)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Timer",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        if (isTimerRunning) "倒计时 ${timerSeconds}s" else "开启 ${currentStepData?.timerSeconds ?: 60}s 烤灯",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // SOP Action Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1917).copy(alpha = 0.95f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC5A880).copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "步骤 $currentStep: ${currentStepData?.title ?: "实操步骤"}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color(0xFFC5A880),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF881337))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (currentStep == 2) "核心步骤" else "基础工艺",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Text(
                                currentStepData?.instruction ?: "按照 AI 提取的工艺指令进行操作。",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    lineHeight = 18.sp
                                )
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "BOM",
                                    tint = Color(0xFFC5A880),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "语音口令：说“${currentStepData?.voiceShortcut ?: "下一步"}”自动翻页",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFC5A880),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    // Navigation Hint
                    Text(
                        "手上沾了胶？轻拍屏幕任意位置即可切到下一步",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Merged AI Feedback / Completion Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Score Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1917)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC5A880))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "美甲 DIY 还原度评分",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFC5A880),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                "95% 还原",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp,
                                    letterSpacing = 2.sp
                                )
                            )
                            Text(
                                "恭喜！你的工艺还原度极高，磁铁斜吸角度非常均匀，边缘封层完全包裹。我们检测到了高水准的宽光效果！",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    // Detailed Analysis
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "AI 智能细节诊断",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )

                        // What went right
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14532D)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color.Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "完美工艺点",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.Green,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        "磁铁斜吸角度控制得极其完美，高光线明亮集中，没有散光。钢化封层涂抹均匀，边缘无缩胶起皱。",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    )
                                }
                            }
                        }

                        // Refinement needed
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF78350F)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "建议微调点",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFFBBF24),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        "左手大拇指外侧边缘有极其微小的色胶溢出。建议下次在照灯前，先用死皮推或棉签清理干净溢胶，防止照灯固化后引起美甲边缘起翘。",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Return Button
                    Button(
                        onClick = onFinish,
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A880)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            "完成并返回首页",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFF1C1917),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
