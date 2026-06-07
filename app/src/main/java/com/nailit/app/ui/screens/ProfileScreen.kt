package com.nailit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.core.preview.NailSessionSnapshot

private val ProfileBg = Color(0xFFFCFAF8)
private val ProfileText = Color(0xFF1F1A17)
private val ProfileMuted = Color(0xFF8C817A)
private val ProfileAccent = Color(0xFFDFA297)
private val ProfileSoft = Color(0xFFFFF7F5)

@Composable
fun ProfileScreen(
    sessionSnapshot: NailSessionSnapshot?,
    onResume: () -> Unit,
) {
    val hasSession = sessionSnapshot != null
    val currentStage = when {
        sessionSnapshot?.executionSteps?.isNotEmpty() == true -> "正在跟做"
        !sessionSnapshot?.targetImagePath.isNullOrBlank() -> "准备开始"
        sessionSnapshot != null -> "AI 试戴中"
        else -> "还没有开始新的款式"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBg)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "我的",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = ProfileText,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                ),
            )
            Text(
                text = "保留你当前这次的试戴和跟做进度。",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ProfileMuted,
                ),
            )
        }

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(30.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(ProfileSoft, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ProfileAccent,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (hasSession) "当前进行中的款式" else "还没有进行中的款式",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = ProfileText,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Text(
                            text = currentStage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = ProfileMuted,
                            ),
                        )
                    }
                }

                if (hasSession) {
                    Surface(
                        color = ProfileSoft,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            SessionMetaRow(
                                label = "来源",
                                value = sessionSnapshot?.sourceUrl?.takeIf { it.isNotBlank() }
                                    ?: "当前模板款式",
                            )
                            SessionMetaRow(
                                label = "阶段",
                                value = currentStage,
                            )
                            SessionMetaRow(
                                label = "会话 ID",
                                value = sessionSnapshot?.sessionId ?: "-",
                            )
                        }
                    }
                }

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProfileAccent,
                        contentColor = Color.White,
                    ),
                    enabled = hasSession,
                ) {
                    Text(
                        text = if (hasSession) "继续当前进度" else "暂无可继续内容",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileHintRow(
                    icon = Icons.Default.Schedule,
                    title = "后面适合加的能力",
                    body = "最近做过、我的收藏、完成海报、分享记录都可以放在这里。",
                )
                ProfileHintRow(
                    icon = Icons.Default.ArrowOutward,
                    title = "当前先保持克制",
                    body = "现在先只承接恢复进度，不把页面做成很重的个人中心。",
                )
            }
        }
    }
}

@Composable
private fun SessionMetaRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = ProfileMuted,
            ),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = ProfileText,
                lineHeight = 20.sp,
            ),
        )
    }
}

@Composable
private fun ProfileHintRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(ProfileSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ProfileAccent,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = ProfileText,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ProfileMuted,
                    lineHeight = 18.sp,
                ),
            )
        }
    }
}
