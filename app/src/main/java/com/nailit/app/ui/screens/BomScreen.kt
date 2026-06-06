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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BomScreen(
    sessionSnapshot: NailSessionSnapshot?,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val repository = remember { SupabaseFunctionRepository() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var isLoading by remember { mutableStateOf(sessionSnapshot != null && sessionSnapshot.bomJson == null) }
    var showDialog by remember { mutableStateOf(false) }

    // Checkbox state maps to handle dynamic list sizes smoothly
    var basicToolsChecked by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var styleItemsChecked by remember { mutableStateOf(mapOf<String, Boolean>()) }

    // Load BOM logic
    LaunchedEffect(sessionSnapshot) {
        if (sessionSnapshot == null) {
            isLoading = false
            return@LaunchedEffect
        }

        if (sessionSnapshot.bomJson != null) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        var fetched: JsonObject? = null
        runCatching {
            // Try up to 3 times to fetch (polling in case background AI is finishing up)
            for (i in 1..3) {
                fetched = repository.fetchBom(sessionSnapshot.sessionId)
                if (fetched != null) break
                delay(1000)
            }
        }

        if (fetched != null) {
            NailSessionRuntime.current = sessionSnapshot.copy(bomJson = fetched)
        }
        isLoading = false
    }

    val activeBom = sessionSnapshot?.bomJson

    // Extract lists
    val basicTools = activeBom?.stringList("basic_tools")?.ifEmpty {
        listOf("平衡液 & 强力底胶", "高光钢化封层 (免洗)", "UV/LED 美甲烤灯 (>=48W)")
    } ?: listOf("平衡液 & 强力底胶", "高光钢化封层 (免洗)", "UV/LED 美甲烤灯 (>=48W)")

    val styleItems = activeBom?.stringList("style_specific_items")?.ifEmpty {
        listOf("冰透粉底色胶 (色号 #03)", "极光碎钻猫眼胶 (银色高光)", "圆柱形强力磁铁 (双头)")
    } ?: listOf("冰透粉底色胶 (色号 #03)", "极光碎钻猫眼胶 (银色高光)", "圆柱形强力磁铁 (双头)")

    val substitutes = activeBom?.stringList("optional_substitutes") ?: emptyList()
    val warnings = activeBom?.stringList("warnings") ?: emptyList()

    // Initialize/sync checkbox states
    LaunchedEffect(basicTools, styleItems) {
        basicToolsChecked = basicTools.associateWith { basicToolsChecked[it] ?: true }
        styleItemsChecked = styleItems.associateWith { styleItemsChecked[it] ?: false }
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
                        "智能 BOM 材料清单",
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
                            "开工前材料点兵",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            "AI 已为您匹配了现有工具，并提供了专属色胶的平替方案",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        )
                    }

                    // Section 1: Basic Tools
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "1. 基础必备工具 (底胶/封层/烤灯)",
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
                            "2. 本款专属材料 (色胶/磁铁)",
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

            // Next Step Button
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                onClick = onContinue,
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "进入沉浸式实操 SOP",
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
