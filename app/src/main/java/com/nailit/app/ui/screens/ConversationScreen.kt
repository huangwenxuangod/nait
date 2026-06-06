package com.nailit.app.ui.screens

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.core.model.VideoChatMessage
import com.nailit.app.core.model.VideoChatRole
import com.nailit.app.core.preview.HandPhotoRuntime
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.VideoChatStreamingRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

private val ConversationBackground = Color(0xFFFBF6F2)
private val AssistantBubble = Color(0xFFFFFFFF)
private val UserBubble = Color(0xFF2C1D1B)
private val BubbleBorder = Color(0xFFF0E2DA)
private val WarmAccent = Color(0xFF9A4D45)
private val WarmSoft = Color(0xFFFCEBE4)
private val WarmMuted = Color(0xFF7F6B63)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    sessionSnapshot: NailSessionSnapshot?,
    onBack: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenTryOn: () -> Unit,
    onOpenSop: () -> Unit,
) {
    val repository = remember { VideoChatStreamingRepository() }
    val scope = rememberCoroutineScope()
    val activeSession = NailSessionRuntime.current ?: sessionSnapshot
    val handBitmap = HandPhotoRuntime.currentBitmap

    var messages by remember {
        mutableStateOf(
            listOf(
                VideoChatMessage(
                    id = "welcome",
                    role = VideoChatRole.Assistant,
                    text = "我是 Nail-It。你可以直接把想法发给我，我会像豆包一样陪你拆款式、看手图、推进步骤。",
                ),
                VideoChatMessage(
                    id = "guide",
                    role = VideoChatRole.Assistant,
                    text = if (activeSession == null) {
                        "现在最适合先贴教程链接，或者先去拍一张手图。"
                    } else {
                        "我已经接上你当前的美甲 session 了。你可以直接问我“这个适合我吗”或者“下一步做什么”。"
                    },
                ),
            )
        )
    }
    var draft by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var statusText by remember {
        mutableStateOf(
            if (activeSession == null) {
                "还没有活跃会话"
            } else {
                "当前会话状态：${activeSession.status}"
            }
        )
    }

    val quickPrompts = remember(activeSession, handBitmap) {
        buildList {
            add("帮我总结当前这个款式")
            if (activeSession != null) add("根据当前 session 告诉我下一步")
            if (handBitmap != null) add("结合我的手图，判断这个款式适不适合我")
            add("像豆包一样陪我一步步做美甲")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "聊天助手",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            "像豆包一样边聊边做美甲",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = WarmMuted
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConversationBackground
                )
            )
        },
        containerColor = ConversationBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SessionContextCard(
                    snapshot = activeSession,
                    handBitmap = handBitmap,
                    onOpenTutorial = onOpenTutorial,
                    onOpenTryOn = onOpenTryOn,
                    onOpenSop = onOpenSop
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPrompts.forEach { prompt ->
                        QuickPromptChip(
                            text = prompt,
                            onClick = {
                                if (!isStreaming) {
                                    draft = prompt
                                }
                            }
                        )
                    }
                }

                messages.forEach { message ->
                    MessageBubble(message = message)
                }

                if (isStreaming) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = WarmAccent
                        )
                        Text(
                            statusText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = WarmMuted
                            )
                        )
                    }
                } else {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = WarmMuted
                        ),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionShortcutRow(
                    onOpenTryOn = onOpenTryOn,
                    onOpenSop = onOpenSop
                )
                ComposerBar(
                    draft = draft,
                    onDraftChange = { draft = it },
                    isStreaming = isStreaming,
                    onSend = {
                        if (draft.isBlank() || isStreaming) return@ComposerBar
                        val userPrompt = draft.trim()
                        draft = ""
                        val userMessage = VideoChatMessage(
                            id = UUID.randomUUID().toString(),
                            role = VideoChatRole.User,
                            text = userPrompt,
                        )
                        val streamingId = UUID.randomUUID().toString()
                        val streamingMessage = VideoChatMessage(
                            id = streamingId,
                            role = VideoChatRole.Assistant,
                            text = "",
                            isStreaming = true,
                        )
                        messages = messages + userMessage + streamingMessage
                        isStreaming = true
                        statusText = "正在思考你的美甲问题..."

                        scope.launch {
                            repository.streamReply(
                                history = messages.filter { !it.isStreaming },
                                userPrompt = userPrompt,
                                frameBase64 = handBitmap?.toDataUrl(),
                                includeVideoFrame = handBitmap != null,
                            ).collectLatest { partial ->
                                messages = messages.map {
                                    if (it.id == streamingId) {
                                        it.copy(text = partial, isStreaming = true)
                                    } else {
                                        it
                                    }
                                }
                            }
                            messages = messages.map {
                                if (it.id == streamingId) {
                                    it.copy(isStreaming = false)
                                } else {
                                    it
                                }
                            }
                            isStreaming = false
                            statusText = if (handBitmap != null) {
                                "已结合当前手图完成回复"
                            } else if (activeSession != null) {
                                "已根据当前 session 完成回复"
                            } else {
                                "回复完成"
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionContextCard(
    snapshot: NailSessionSnapshot?,
    handBitmap: Bitmap?,
    onOpenTutorial: () -> Unit,
    onOpenTryOn: () -> Unit,
    onOpenSop: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BubbleBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(WarmSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = WarmAccent
                    )
                }
                Column {
                    Text(
                        "当前项目上下文",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        snapshot?.let { "Session ${it.sessionId.take(8)} | ${it.status}" } ?: "还没有创建 session",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = WarmMuted
                        )
                    )
                }
            }

            if (handBitmap != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = handBitmap.asImageBitmap(),
                        contentDescription = "Current hand photo",
                        modifier = Modifier
                            .size(72.dp)
                            .border(1.dp, BubbleBorder, RoundedCornerShape(16.dp))
                            .background(Color.White, RoundedCornerShape(16.dp))
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "已捕获真实手图",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            "聊天时我会优先结合这张手图来判断适配度与下一步建议。",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = WarmMuted
                            )
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniActionButton(
                    label = "看解析",
                    icon = Icons.Default.AutoAwesome,
                    onClick = onOpenTutorial
                )
                MiniActionButton(
                    label = "拍手试戴",
                    icon = Icons.Default.CameraAlt,
                    onClick = onOpenTryOn
                )
                MiniActionButton(
                    label = "继续 SOP",
                    icon = Icons.Default.GraphicEq,
                    onClick = onOpenSop
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: VideoChatMessage) {
    val isUser = message.role == VideoChatRole.User
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .background(
                    color = if (isUser) UserBubble else AssistantBubble,
                    shape = RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isUser) 22.dp else 8.dp,
                        bottomEnd = if (isUser) 8.dp else 22.dp
                    )
                )
                .border(
                    width = if (isUser) 0.dp else 1.dp,
                    color = BubbleBorder,
                    shape = RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isUser) 22.dp else 8.dp,
                        bottomEnd = if (isUser) 8.dp else 22.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (message.text.isBlank() && message.isStreaming) "..." else message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isUser) Color.White else Color(0xFF1A1412),
                    lineHeight = 21.sp
                )
            )
        }
    }
}

@Composable
private fun QuickPromptChip(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, BubbleBorder, RoundedCornerShape(16.dp))
            .padding(end = 0.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, BubbleBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .padding(end = 0.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = WarmAccent,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun ActionShortcutRow(
    onOpenTryOn: () -> Unit,
    onOpenSop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ShortcutPill(
            label = "拍手",
            icon = Icons.Default.CameraAlt,
            onClick = onOpenTryOn
        )
        ShortcutPill(
            label = "继续步骤",
            icon = Icons.Default.GraphicEq,
            onClick = onOpenSop
        )
    }
}

@Composable
private fun ShortcutPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = WarmAccent
        ),
        border = BorderStroke(1.dp, BubbleBorder),
        modifier = Modifier.height(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label)
    }
}

@Composable
private fun ComposerBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    isStreaming: Boolean,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(22.dp))
            .border(1.dp, BubbleBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF1A1412)
            ),
            decorationBox = { innerTextField ->
                if (draft.isBlank()) {
                    Text(
                        "问我：这个款式适合我吗？下一步做什么？",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = WarmMuted
                        )
                    )
                }
                innerTextField()
            }
        )
        Spacer(modifier = Modifier.width(10.dp))
        Button(
            onClick = onSend,
            enabled = !isStreaming && draft.isNotBlank(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = WarmAccent
            )
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = Color.White
            )
        }
    }
}

private fun Bitmap.toDataUrl(): String {
    val output = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 90, output)
    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    return "data:image/jpeg;base64,$encoded"
}
