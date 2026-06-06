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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
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
private val EntryLine = Color(0xFFDCCFC3)

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
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nail-It",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = EntryTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                    )
                )
                Text(
                    text = "先看效果，再决定要不要做。",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = EntryBody,
                        fontSize = 15.sp,
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFF6ECE4), Color(0xFFE8D8CB))
                        )
                    )
                    .border(1.dp, EntryBorder, RoundedCornerShape(32.dp))
                    .padding(14.dp)
            ) {
                Image(
                    painter = painterResource(selectedTemplate.imageRes),
                    contentDescription = selectedTemplate.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                        .clip(RoundedCornerShape(26.dp)),
                    contentScale = ContentScale.Crop,
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = 0.24f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = selectedTemplate.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    )
                    Text(
                        text = selectedTemplate.promptLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.82f),
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(EntryCard)
                    .border(1.dp, EntryBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                        .clip(RoundedCornerShape(16.dp))
                        .background(EntryBg)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    decorationBox = { inner ->
                        if (linkInput.isBlank()) {
                            Text(
                                text = "粘贴教程链接，或者直接用当前模板",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = EntryBody,
                                )
                            )
                        }
                        inner()
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    templates.forEach { template ->
                        val selected = template.id == selectedTemplate.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) EntryAccent else EntryBg)
                                .border(
                                    1.dp,
                                    if (selected) EntryAccent else EntryLine,
                                    RoundedCornerShape(999.dp),
                                )
                                .clickable { onSelectId(template.id) }
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = template.promptLabel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (selected) Color.White else EntryTitle,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                )
                            )
                        }
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
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EntryBody,
                        lineHeight = 18.sp,
                    )
                )
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
