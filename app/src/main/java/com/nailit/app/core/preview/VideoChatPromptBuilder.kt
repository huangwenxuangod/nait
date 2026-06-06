package com.nailit.app.core.preview

import com.nailit.app.core.model.OpenAiImagePart
import com.nailit.app.core.model.OpenAiImageUrl
import com.nailit.app.core.model.OpenAiMessage
import com.nailit.app.core.model.OpenAiTextPart
import com.nailit.app.core.model.VideoChatMessage
import com.nailit.app.core.model.VideoChatRole

object VideoChatPromptBuilder {
    fun buildMessages(
        history: List<VideoChatMessage>,
        userPrompt: String,
        frameBase64: String?,
        includeVideoFrame: Boolean,
    ): List<OpenAiMessage> {
        val systemText = """
            你是 Nail-It，一个像豆包视频聊天一样工作的美甲助手。
            你正在通过用户当前相机画面和用户口头问题进行实时交流。
            规则：
            1. 只返回自然语言，短句优先，口语化。
            2. 如果看不清画面，要明确让用户移动镜头、靠近手部、切亮环境。
            3. 如果用户在问美甲执行问题，优先给出下一步动作建议。
            4. 不输出 JSON，不输出 markdown 列表，不解释系统设计。
            5. 回复要像实时视频聊天，不要写成长文。
        """.trimIndent()

        val messages = mutableListOf(
            OpenAiMessage(
                role = "system",
                content = listOf(OpenAiTextPart(text = systemText))
            )
        )

        history.takeLast(6).forEach { message ->
            messages += OpenAiMessage(
                role = if (message.role == VideoChatRole.User) "user" else "assistant",
                content = listOf(OpenAiTextPart(text = message.text))
            )
        }

        val userParts = mutableListOf<Any>(OpenAiTextPart(text = userPrompt))
        if (includeVideoFrame && !frameBase64.isNullOrBlank()) {
            userParts += OpenAiImagePart(
                imageUrl = OpenAiImageUrl(url = frameBase64)
            )
        }

        @Suppress("UNCHECKED_CAST")
        messages += OpenAiMessage(
            role = "user",
            content = userParts as List<com.nailit.app.core.model.OpenAiContentPart>
        )

        return messages
    }
}
