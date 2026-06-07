package com.nailit.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

private val BomBg = Color(0xFFFDFBF9)
private val BomText = Color(0xFF2A2624)
private val BomMuted = Color(0xFF8D8783)
private val BomLine = Color(0xFFF0ECE8)
private val BomCard = Color(0xFFFFFFFF)
private val BomAccent = Color(0xFFA9DCC5)
private val BomAccentText = Color(0xFF5D9A84)
private val BomSoft = Color(0xFFF7FBF9)
private val BomPale = Color(0xFFF8F6F4)
private val BomDanger = Color(0xFFF2F1EF)

private data class MaterialGuide(
    val what: String,
    val looksLike: String,
    val pitfall: String,
)

private data class MaterialItem(
    val id: String,
    val name: String,
    val desc: String,
    val emoji: String,
    val group: String,
    val guide: MaterialGuide,
)

private enum class MaterialState {
    Unknown,
    Has,
    Missing,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BomScreen(
    sessionSnapshot: NailSessionSnapshot?,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val repository = remember { SupabaseFunctionRepository() }
    val runtimeSession = NailSessionRuntime.current ?: sessionSnapshot

    var isLoading by remember { mutableStateOf(runtimeSession != null && runtimeSession.bomJson == null) }
    var targetBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedMaterial by remember { mutableStateOf<MaterialItem?>(null) }
    val decisions = remember { mutableStateMapOf<String, MaterialState>() }

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

    LaunchedEffect(runtimeSession?.targetImagePath) {
        targetBitmap = runtimeSession?.targetImagePath?.let { loadTargetBitmap(it) }
    }

    val activeBom = (NailSessionRuntime.current ?: sessionSnapshot)?.bomJson
    val materials = remember(activeBom) { buildDetailedMaterialList(activeBom) }
    val confirmedCount = materials.count { decisions[it.id] != null && decisions[it.id] != MaterialState.Unknown }
    val allConfirmed = confirmedCount == materials.size && materials.isNotEmpty()
    val remaining = (materials.size - confirmedCount).coerceAtLeast(0)

    Scaffold(
        containerColor = BomBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { onBack() },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = BomMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "返回",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = BomMuted,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }

                targetBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "试戴缩略图",
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        },
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BomBg)
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator(color = BomAccentText, strokeWidth = 2.4.dp)
                    Text(
                        text = "正在准备你的物料",
                        style = MaterialTheme.typography.bodyMedium.copy(color = BomMuted),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BomBg)
                    .padding(padding)
                    .padding(horizontal = 18.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 22.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "STEP 01",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color(0xFFD8A8B2),
                                    letterSpacing = 6.sp,
                                ),
                            )
                            Text(
                                text = "准备你的物料",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = BomText,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 34.sp,
                                ),
                            )
                            Text(
                                text = "已确认 $confirmedCount / ${materials.size} · 不认识的工具点一下看看",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = BomMuted,
                                    lineHeight = 22.sp,
                                ),
                            )
                        }
                    }

                    item {
                        Surface(
                            color = BomSoft,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFD8ECE2), RoundedCornerShape(24.dp))
                                .clickable {
                                    materials.forEach { decisions[it.id] = MaterialState.Has }
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = BomAccentText,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "一键全选 · 全都准备好了",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = BomAccentText,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "基础工具",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = BomMuted,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }

                    item {
                        Surface(
                            color = BomCard,
                            shape = RoundedCornerShape(28.dp),
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                materials.forEachIndexed { index, item ->
                                    MaterialRow(
                                        item = item,
                                        state = decisions[item.id] ?: MaterialState.Unknown,
                                        onOpenGuide = { selectedMaterial = item },
                                        onSelect = { state -> decisions[item.id] = state },
                                    )
                                    if (index != materials.lastIndex) {
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(BomLine),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = if (allConfirmed) {
                                "已全部确认，可以开始跟做了"
                            } else {
                                "每一项都需确认 · 还剩 $remaining 项"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = BomMuted,
                            ),
                        )
                    }
                }

                Button(
                    onClick = onContinue,
                    enabled = allConfirmed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1EDEA),
                        disabledContainerColor = Color(0xFFF1EDEA),
                        contentColor = BomText,
                        disabledContentColor = Color(0xFFB6B0AB),
                    ),
                ) {
                    Text(
                        text = if (allConfirmed) "开始沉浸式制作" else "请确认每一项物料",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp,
                        ),
                    )
                }
            }
        }
    }

    selectedMaterial?.let { item ->
        MaterialGuideDialog(
            item = item,
            state = decisions[item.id] ?: MaterialState.Unknown,
            onDismiss = { selectedMaterial = null },
            onSelect = { state ->
                decisions[item.id] = state
                selectedMaterial = null
            },
        )
    }
}

@Composable
private fun MaterialRow(
    item: MaterialItem,
    state: MaterialState,
    onOpenGuide: () -> Unit,
    onSelect: (MaterialState) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(BomPale),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.emoji,
                fontSize = 28.sp,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = BomText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 19.sp,
                ),
            )
            Text(
                text = item.desc,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = BomMuted,
                    lineHeight = 27.sp,
                    fontSize = 15.sp,
                ),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        IconButton(onClick = onOpenGuide) {
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = "这是什么",
                tint = Color(0xFFC4BDB8),
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        DecisionPill(
            text = "我有",
            selected = state == MaterialState.Has,
            selectedBg = BomAccent,
            selectedText = BomAccentText,
            onClick = { onSelect(MaterialState.Has) },
            leading = Icons.Default.Check,
        )

        Spacer(modifier = Modifier.width(10.dp))

        DecisionPill(
            text = "没有",
            selected = state == MaterialState.Missing,
            selectedBg = BomDanger,
            selectedText = BomText,
            onClick = { onSelect(MaterialState.Missing) },
            leading = Icons.Default.Close,
        )
    }
}

@Composable
private fun DecisionPill(
    text: String,
    selected: Boolean,
    selectedBg: Color,
    selectedText: Color,
    onClick: () -> Unit,
    leading: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Surface(
        color = if (selected) selectedBg else Color(0xFFF1EFED),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = leading,
                contentDescription = null,
                tint = if (selected) selectedText else Color(0xFF7E7772),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (selected) selectedText else Color(0xFF6E6762),
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
private fun MaterialGuideDialog(
    item: MaterialItem,
    state: MaterialState,
    onDismiss: () -> Unit,
    onSelect: (MaterialState) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(36.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF4F2F0))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color(0xFF918984),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFFFFF8F9)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = item.emoji, fontSize = 70.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = BomText,
                            fontWeight = FontWeight.Light,
                            fontSize = 32.sp,
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DecisionPill(
                            text = "我有",
                            selected = state == MaterialState.Has,
                            selectedBg = BomAccent,
                            selectedText = BomAccentText,
                            onClick = { onSelect(MaterialState.Has) },
                            leading = Icons.Default.Check,
                        )
                        DecisionPill(
                            text = "没有",
                            selected = state == MaterialState.Missing,
                            selectedBg = BomDanger,
                            selectedText = BomText,
                            onClick = { onSelect(MaterialState.Missing) },
                            leading = Icons.Default.Close,
                        )
                    }
                }

                GuideBlock(
                    title = "📖 这是什么？",
                    body = item.guide.what,
                )
                GuideBlock(
                    title = "👀 长什么样？",
                    body = item.guide.looksLike,
                )

                Surface(
                    color = Color(0xFFF8FBF8),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "💡 新手避坑",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFFA9DCC5),
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                        Text(
                            text = item.guide.pitfall,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = BomText,
                                lineHeight = 34.sp,
                                fontSize = 17.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideBlock(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFFD8A8B2),
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = BomText,
                lineHeight = 34.sp,
                fontSize = 17.sp,
            ),
        )
    }
}

private fun buildDetailedMaterialList(bom: JsonObject?): List<MaterialItem> {
    val names = (
        bom?.stringList("basic_tools").orEmpty() +
            bom?.stringList("style_specific_items").orEmpty()
        ).map { it.substringBefore(" (").substringBefore("（") }
        .distinct()

    val defaults = listOf(
        material("base_coat", "底胶", "增强甲面附着力，防止色素沉淀和甲面发黄", "📦", "基础工具"),
        material("lamp", "烤灯", "固化所有光疗胶层，确保成膜硬度与持久度", "💡", "基础工具"),
        material("top_coat", "封层", "形成高亮耐磨表层，锁住色彩与渐变纹理", "📦", "基础工具"),
        material("sponge", "海绵搓条", "用于干搓甲面边缘与渐变过渡区，制造柔和晕染效果", "📦", "基础工具"),
        material("cleanser", "清洁液", "去除封层表面粘腻层，提升光泽度与触感顺滑度", "📦", "基础工具"),
        material("cat_eye", "猫眼胶", "提供主视觉高光和流动磁感效果", "🧲", "款式物料"),
        material("magnet", "磁铁", "吸出猫眼高光线条和光斑移动效果", "🧲", "款式物料"),
        material("pink_gel", "裸粉底色胶", "作为冰透底色，让后续高光更干净", "🩷", "款式物料"),
    )

    if (names.isEmpty()) return defaults

    return names.mapIndexed { index, name ->
        defaults.firstOrNull { it.name == name } ?: material(
            id = "dynamic_$index",
            name = name,
            desc = "这是这款里会用到的关键材料，建议先确认手边是否有接近替代。",
            emoji = if (name.contains("灯")) "💡" else "📦",
            group = "基础工具",
        )
    }
}

private fun material(
    id: String,
    name: String,
    desc: String,
    emoji: String,
    group: String,
): MaterialItem {
    val guide = when (name) {
        "底胶" -> MaterialGuide(
            what = "涂在正式上色前的第一层透明胶，用来提升附着力，让后面颜色和猫眼效果更稳定。",
            looksLike = "通常是一小瓶透明胶，看起来和普通甲油胶很像，刷头细，质地偏稀一点。",
            pitfall = "底胶一定要薄，碰到皮肤会导致后面整片翘边；没照干前也不要来回碰。",
        )
        "烤灯" -> MaterialGuide(
            what = "美甲专用的小型灯箱，发出 UV 或 LED 紫外线光，用来快速照干 / 固化甲胶。不做美甲胶的话这个步骤没法跳过。",
            looksLike = "一个白色或粉色的小盒子，大小和鞋盒差不多。中间有个凹槽可以把手伸进去。顶部和侧面排列着一排排小灯珠（LED 型号）或者灯管（UV 型号）。通常带一个定时按钮。",
            pitfall = "手放进去后中途不要拿出来看，否则没固化的胶会沾灰或起皱。一般 60-90 秒就能固化一层。",
        )
        "封层" -> MaterialGuide(
            what = "最后一层透明保护层，用来锁住颜色、亮片和猫眼纹理，同时提升光泽度和耐磨性。",
            looksLike = "也是一瓶透明胶，刷头和底胶很像，但上手后会更顺滑、更亮。",
            pitfall = "封层别涂太厚，容易表面鼓包；照灯后别急着碰，先等它完全稳定。",
        )
        "海绵搓条" -> MaterialGuide(
            what = "一种柔软细磨条，用来轻磨甲面、去掉油脂和浮尘，也能做柔和过渡。",
            looksLike = "像一块长条小海绵或小砂条，通常是浅灰、白色、粉色。",
            pitfall = "别太用力，轻轻均匀带过就行，磨重了甲面会变薄。",
        )
        "清洁液" -> MaterialGuide(
            what = "专门擦拭甲面和封层表面粘腻层的液体，也能帮助去灰除油。",
            looksLike = "通常装在透明或半透明瓶里，看起来像酒精或爽肤水。",
            pitfall = "别直接倒太多在甲面上，配合棉片轻擦就行，不然容易影响刚做好的光泽。",
        )
        else -> MaterialGuide(
            what = "这是这套款式里会用到的关键材料，主要用来完成最终颜色或特殊效果。",
            looksLike = "通常是一小瓶胶或一件小工具，和常规美甲用品放在一起就能认出来。",
            pitfall = "如果没有一模一样的，先找最接近的替代，不要一开始就卡死在材料完全一致上。",
        )
    }
    return MaterialItem(id, name, desc, emoji, group, guide)
}

private fun JsonObject.stringList(key: String): List<String> {
    return (this[key] as? JsonArray)?.mapNotNull { item ->
        (item as? JsonPrimitive)?.contentOrNull
    } ?: emptyList()
}

private suspend fun loadTargetBitmap(storagePath: String): Bitmap? {
    return runCatching {
        val bytes = SupabaseManager.client.storage
            .from("nail-it-assets")
            .downloadAuthenticated(storagePath)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
