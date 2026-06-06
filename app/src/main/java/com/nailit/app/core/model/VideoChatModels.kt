package com.nailit.app.core.model

enum class VideoChatRole {
    User,
    Assistant,
}

data class VideoChatMessage(
    val id: String,
    val role: VideoChatRole,
    val text: String,
    val isStreaming: Boolean = false,
)

data class VideoChatUiState(
    val messages: List<VideoChatMessage> = emptyList(),
    val draft: String = "",
    val isListening: Boolean = false,
    val isStreaming: Boolean = false,
    val status: String = "准备开始视频聊天",
    val videoEnabled: Boolean = true,
)
