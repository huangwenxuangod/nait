package com.nailit.app.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.R
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.SupabaseFunctionRepository
import kotlinx.coroutines.launch
import java.util.UUID

private val EntryBg = Color(0xFFF8F4F0)
private val EntryCard = Color(0xFFFFFEFC)
private val EntryBorder = Color(0xFFE9DDD3)
private val EntryTitle = Color(0xFF161211)
private val EntryBody = Color(0xFF7E726A)
private val EntryAccent = Color(0xFF2B1D1A)
private val EntrySoft = Color(0xFFF2E7DF)

private data class TemplateItem(
    val id: String,
    val title: String,
    val promptLabel: String,
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
        TemplateItem("aurora-cat", "温柔裸粉法式", "暖粉猫眼", R.drawable.nail_showcase_01),
        TemplateItem("retro-wine", "焦糖琥珀晕染", "焦糖琥珀", R.drawable.nail_showcase_02),
        TemplateItem("french-soft", "透亮冰透款", "法式渐变", R.drawable.nail_showcase_03),
        TemplateItem("pearl-milk", "清新花卉款", "清透花卉", R.drawable.nail_showcase_04),
    )
    val selectedTemplate = templates.firstOrNull { it.id == selectedId } ?: templates.first()

    val createSession = remember(context, repository, selectedTemplate, linkInput, isCreating) {
        {
            if (isCreating) return@remember
            scope.launch {
                isCreating = true
                statusText = "正在准备试戴会话..."
                val sourceUrl = linkInput.ifBlank { "preset://${selectedTemplate.id}" }
                runCatching {
                    val installId = getInstallId(context)
                    val sourceType = if (linkInput.isNotBlank()) "short_video_link" else "preset"
                    val session = repository.createSession(
                        installId = installId,
                        sourceType = sourceType,
                    )
                    val sessionId = session.session_id ?: error("createSession missing session_id")
                    val submit = repository.submitSourceLink(
                        sessionId = sessionId,
                        sourceUrl = sourceUrl,
                    )
                    val parse = repository.fetchSourceParse(sessionId)
                    NailSessionRuntime.current = NailSessionSnapshot(
                        sessionId = sessionId,
                        sourceUrl = sourceUrl,
                        sourceType = sourceType,
                        selectedTutorialId = selectedTemplate.id,
                        status = if (parse != null) "source_parsed" else (submit.status ?: "source_parsing"),
                        sourceParseJson = parse,
                    )
                }.onSuccess {
                    statusText = "会话已准备好，进入上传手图。"
                    onOpenFlow()
                }.onFailure { error ->
                    statusText = error.message ?: "创建失败，请重试"
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Nail-It",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = EntryTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp,
                    )
                )
                Text(
                    text = "先试戴，再决定要不要做。",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = EntryBody,
                        fontSize = 16.sp,
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(EntryCard)
                    .border(1.dp, EntryBorder, RoundedCornerShape(28.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(selectedTemplate.imageRes),
                    contentDescription = selectedTemplate.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.04f)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop,
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = selectedTemplate.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = EntryTitle,
                            fontWeight = FontWeight.SemiBold,
                        )
                    )
                    Text(
                        text = "模板关键词：${selectedTemplate.promptLabel}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EntryBody,
                        )
                    )
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
                        .background(EntryBg)
                        .border(1.dp, EntryBorder, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    decorationBox = { inner ->
                        if (linkInput.isBlank()) {
                            Text(
                                text = "可选：贴教程链接，不贴就直接用当前模板",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = EntryBody,
                                )
                            )
                        }
                        inner()
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                templates.forEach { template ->
                    val selected = template.id == selectedTemplate.id
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) EntryAccent else EntryCard)
                            .border(
                                1.dp,
                                if (selected) EntryAccent else EntryBorder,
                                RoundedCornerShape(20.dp),
                            )
                            .clickable { onSelectId(template.id) }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (selected) Color.White else EntryTitle,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        )
                        Text(
                            text = template.promptLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (selected) Color.White.copy(alpha = 0.72f) else EntryBody,
                                fontSize = 11.sp,
                            )
                        )
                    }
                }
            }

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

            statusText?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EntrySoft)
                        .padding(14.dp)
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EntryBody,
                            lineHeight = 20.sp,
                        )
                    )
                }
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
