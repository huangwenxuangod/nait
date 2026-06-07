package com.nailit.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BomScreen(
    sessionSnapshot: NailSessionSnapshot?,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val repository = remember { SupabaseFunctionRepository() }
    val scrollState = rememberScrollState()
    val runtimeSession = NailSessionRuntime.current ?: sessionSnapshot

    var isLoading by remember { mutableStateOf(runtimeSession != null && runtimeSession.bomJson == null) }
    var isGuidePreparing by remember { mutableStateOf(runtimeSession?.executionStatus == "guide_pending") }
    var isGuideReady by remember { mutableStateOf(runtimeSession?.executionSteps?.isNotEmpty() == true) }
    var showDialog by remember { mutableStateOf(false) }

    // Checkbox state maps to handle dynamic list sizes smoothly
    var basicToolsChecked by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var styleItemsChecked by remember { mutableStateOf(mapOf<String, Boolean>()) }

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

    val activeBom = (NailSessionRuntime.current ?: sessionSnapshot)?.bomJson

    // Extract lists
    val basicTools = activeBom?.stringList("basic_tools")?.ifEmpty {
        listOf("平衡液 & 强力底胶", "高光钢化封层 (免洗)", "UV/LED 美甲烤灯 (>=48W)")
    } ?: listOf("平衡液 & 强力底胶", "高光钢化封层 (免洗)", "UV/LED 美甲烤灯 (>=48W)")

    val styleItems = activeBom?.stringList("style_specific_items")?.ifEmpty {
        listOf("冰透粉底色胶 (色号 #03)", "极光碎钻猫眼胶 (银色高光)", "圆柱形强力磁铁 (双头)")
    } ?: listOf("冰透粉底色胶 (色号 #03)", "极光碎钻猫眼胶 (银色高光)", "圆柱形强力磁铁 (双头)")

    val substitutes = activeBom?.stringList("optional_substitutes") ?: emptyList()
    val warnings = activeBom?.stringList("warnings") ?: emptyList()

    val allChecked = remember(basicToolsChecked, styleItemsChecked) {
        basicToolsChecked.values.all { it } && styleItemsChecked.values.all { it }
    }

    // Initialize/sync checkbox states
    LaunchedEffect(basicTools, styleItems) {
        basicToolsChecked = basicTools.associateWith { basicToolsChecked[it] ?: true }
        styleItemsChecked = styleItems.associateWith { styleItemsChecked[it] ?: true }
    }

    if (showDialog) {
        val uncheckedItems = styleItemsChecked.filter { !it.value }.keys
        val itemsText = if (uncheckedItems.isNotEmpty()) {
            uncheckedItems.joinToString("、")
        } else {
            "极光猫眼色胶、高强磁铁、冰透粉底色"
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("好 的", color = Color(0xFF881337), fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    "一键加购成功",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    "已自动分析您的现有材料，并将缺少的 ${uncheckedItems.size.coerceAtLeast(1)} 件专属材料（$itemsText）一键加入淘宝/拼多多购物车，享拼团优惠价 ¥24.8！",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                )
            },
            shape = RoundedCornerShape(0.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "准备开始",
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
            if (isLoading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF881337))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "正在加载 AI 专属材料清单...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Slogan
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "开工前检查一下材料",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            "勾完就能直接开始，AI 也会同时在后台整理你的专属步骤。",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE9D8D0).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "专属步骤整理",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1412)
                                    )
                                )
                                Text(
                                    when {
                                        isGuideReady -> "已整理完成，随时可以开始跟做"
                                        isGuidePreparing -> "正在根据试戴款式生成你的步骤卡"
                                        else -> "进入本页后自动开始整理"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                )
                            }
                            if (isGuidePreparing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF881337)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isGuideReady) Color(0xFF16A34A) else Color.LightGray
                                )
                            }
                        }
                    }

                    // Section 1: Basic Tools
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "1. 基础工具",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC5A880)
                            )
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE9D8D0).copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                basicTools.forEachIndexed { index, tool ->
                                    val isChecked = basicToolsChecked[tool] ?: true
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                basicToolsChecked = basicToolsChecked + (tool to checked)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF881337))
                                        )
                                        Column {
                                            Text(tool, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                if (index == 0) "基础打底，防止美甲起翘脱落" else if (index == 1) "增亮防刮，锁住猫眼高光" else "用于快速固化甲油胶 (建议使用你现有的烤灯)",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                            )
                                        }
                                    }
                                    if (index < basicTools.size - 1) {
                                        HorizontalDivider(
                                            color = Color(0xFFE9D8D0).copy(alpha = 0.3f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Exclusive Materials
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "2. 本款材料",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC5A880)
                            )
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE9D8D0).copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                styleItems.forEachIndexed { index, item ->
                                    val isChecked = styleItemsChecked[item] ?: false
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                styleItemsChecked = styleItemsChecked + (item to checked)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF881337))
                                        )
                                        Column {
                                            Text(item, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                if (index == 0) "原版Chanel，可用你现有的 NARS 裸粉替代" else if (index == 1) "形成斜吸水光感的关键色胶" else "用于吸出猫眼宽光/斜光效果",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = if (index == 0) Color(0xFF881337) else Color.Gray
                                                )
                                            )
                                        }
                                    }
                                    if (index < styleItems.size - 1) {
                                        HorizontalDivider(
                                            color = Color(0xFFE9D8D0).copy(alpha = 0.3f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Dynamic Substitutes & Warnings
                    if (substitutes.isNotEmpty() || warnings.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF8F5)),
                            border = BorderStroke(1.dp, Color(0xFFE9D8D0).copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (substitutes.isNotEmpty()) {
                                    Text(
                                        "💡 AI 平替建议",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF881337)
                                        )
                                    )
                                    substitutes.forEach { sub ->
                                        Text("• $sub", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1A1412)))
                                    }
                                }

                                if (warnings.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "⚠️ 注意事项",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC5A880)
                                        )
                                    )
                                    warnings.forEach { warn ->
                                        Text("• $warn", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1A1412)))
                                    }
                                }
                            }
                        }
                    }

                    // One-click checkout on Taobao/Pinduoduo
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        onClick = { showDialog = true },
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = Color(0xFFEF4444)
                            )
                            Text(
                                "缺色号？一键拼多多/淘宝拼单加购",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                onClick = onContinue,
                enabled = allChecked && isGuideReady,
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    when {
                        !allChecked -> "先勾完材料"
                        !isGuideReady -> "步骤整理中..."
                        else -> "开始跟做"
                    },
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

private fun JsonObject.stringList(key: String): List<String> {
    return (this[key] as? JsonArray)?.mapNotNull { item ->
        (item as? JsonPrimitive)?.contentOrNull
    } ?: emptyList()
}
