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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BomScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showDialog by remember { mutableStateOf(false) }

    // Checkbox states
    var tool1 by remember { mutableStateOf(true) }
    var tool2 by remember { mutableStateOf(true) }
    var tool3 by remember { mutableStateOf(false) }
    var tool4 by remember { mutableStateOf(false) }

    var mat1 by remember { mutableStateOf(false) }
    var mat2 by remember { mutableStateOf(false) }
    var mat3 by remember { mutableStateOf(false) }

    if (showDialog) {
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
                    "已自动分析您的现有材料，并将缺少的 3 件专属材料（极光猫眼色胶、高强磁铁、冰透粉底色）一键加入淘宝/拼多多购物车，享拼团优惠价 ¥24.8！",
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
                            // Item 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = tool1,
                                    onCheckedChange = { tool1 = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF881337))
                                )
                                Column {
                                    Text("平衡液 & 强力底胶", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("基础打底，防止美甲起翘脱落", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                }
                            }
                            Divider(color = Color(0xFFE9D8D0).copy(alpha = 0.3f))
                            // Item 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = tool2,
                                    onCheckedChange = { tool2 = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF881337))
                                )
                                Column {
                                    Text("高光钢化封层 (免洗)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("增亮防刮，锁住猫眼高光", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                }
                            }
                            Divider(color = Color(0xFFE9D8D0).copy(alpha = 0.3f))
                            // Item 3
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = tool3,
                                    onCheckedChange = { tool3 = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF881337))
                                )
                                Column {
                                    Text("UV/LED 美甲烤灯 (>=48W)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("用于快速固化甲油胶 (建议使用你现有的烤灯)", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
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
                            // Item 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = mat1,
                                    onCheckedChange = { mat1 = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF881337))
                                )
                                Column {
                                    Text("冰透粉底色胶 (色号 #03)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("原版为 Chanel 105，可用你现有的 NARS 裸粉替代", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF881337)))
                                }
                            }
                            Divider(color = Color(0xFFE9D8D0).copy(alpha = 0.3f))
                            // Item 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = mat2,
                                    onCheckedChange = { mat2 = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF881337))
                                )
                                Column {
                                    Text("极光碎钻猫眼胶 (银色高光)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("形成斜吸水光感的关键色胶", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                }
                            }
                            Divider(color = Color(0xFFE9D8D0).copy(alpha = 0.3f))
                            // Item 3
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = mat3,
                                    onCheckedChange = { mat3 = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF881337))
                                )
                                Column {
                                    Text("圆柱形强力磁铁 (双头)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("用于吸出猫眼宽光/斜光效果", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
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
