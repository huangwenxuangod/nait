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
import androidx.compose.material.icons.filled.Mic
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SopScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    var currentStep by remember { mutableStateOf(2) } // Start on Step 2 for best demo experience
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(60) }
    var showCompletionFeedback by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()

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
            timerSeconds = 60
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showCompletionFeedback) "AI 大师级实操反馈" else "沉浸式实操 SOP (第 $currentStep/4 步)",
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
                    if (!showCompletionFeedback) {
                        if (currentStep < 4) {
                            currentStep++
                        } else {
                            showCompletionFeedback = true
                        }
                    }
                }
        ) {
            if (!showCompletionFeedback) {
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
                                "说“下一步”或轻拍屏幕翻页",
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
                                        if (isTimerRunning) "倒计时 ${timerSeconds}s" else "开启 60s 烤灯",
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
                                    "步骤 $currentStep: 极光猫眼斜吸工艺",
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
                                        "核心步骤",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Text(
                                "均匀涂上极光猫眼胶。拿起磁铁，斜放在指甲边缘 45 度角（距离指甲约 0.5 厘米），停留 5 秒钟，观察高光线向中心汇聚，吸出宽光后立刻放入烤灯固化！",
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
                                    "当前材料：极光碎钻猫眼胶 + 圆柱磁铁",
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
                                "恭喜！你的极光猫眼斜吸角度非常均匀，边缘封层完全包裹。我们检测到了极高水准的宽光效果！",
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
                                        "磁铁斜吸 45 度控制得极其完美，猫眼高光线明亮集中，没有散光。钢化封层涂抹均匀，边缘无缩胶起皱。",
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
