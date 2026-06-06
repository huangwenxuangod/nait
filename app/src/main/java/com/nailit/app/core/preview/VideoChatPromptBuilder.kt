package com.nailit.app.core.preview

import com.nailit.app.core.model.OpenAiImagePart
import com.nailit.app.core.model.OpenAiImageUrl
import com.nailit.app.core.model.OpenAiMessage
import com.nailit.app.core.model.OpenAiTextPart
import com.nailit.app.core.model.VideoChatMessage
import com.nailit.app.core.model.VideoChatRole
import com.nailit.app.core.preview.NailSessionRuntime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

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

        val runtimeContext = buildRuntimeContext()

        val messages = mutableListOf(
            OpenAiMessage(
                role = "system",
                content = listOf(OpenAiTextPart(text = "$systemText\n\n$runtimeContext"))
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

    private fun buildRuntimeContext(): String {
        val session = NailSessionRuntime.current
        if (session == null) {
            return "当前没有活跃 session。优先引导用户发教程链接或拍手图。"
        }

        val parseSummary = buildParseSummary(session.sourceParseJson)
        val bomSummary = buildListSummary(session.bomJson, "basic_tools")
        val stepSummary = buildSopSummary(session.sopJson)

        return buildString {
            appendLine("当前项目上下文：")
            appendLine("- session_id=${session.sessionId}")
            appendLine("- source_type=${session.sourceType}")
            appendLine("- source_url=${session.sourceUrl}")
            appendLine("- status=${session.status}")
            appendLine("- current_step_index=${session.currentStepIndex}")
            appendLine("- current_step_title=${session.currentStepTitle ?: "none"}")
            if (parseSummary.isNotBlank()) {
                appendLine("- parse_summary=$parseSummary")
            }
            if (bomSummary.isNotBlank()) {
                appendLine("- bom_summary=$bomSummary")
            }
            if (stepSummary.isNotBlank()) {
                appendLine("- sop_summary=$stepSummary")
            }
        }.trim()
    }

    private fun buildParseSummary(parse: JsonObject?): String {
        if (parse == null) return ""
        val styleName = (parse["style_name"] as? JsonPrimitive)?.contentOrNull ?: return ""
        val tags = (parse["style_tags"]?.jsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
        return buildString {
            append(styleName)
            if (tags.isNotEmpty()) {
                append(" / ")
                append(tags.joinToString(","))
            }
        }
    }

    private fun buildListSummary(json: JsonObject?, key: String): String {
        if (json == null) return ""
        val values = json[key]?.jsonArray?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
        return values.joinToString(",")
    }

    private fun buildSopSummary(sop: JsonObject?): String {
        if (sop == null) return ""
        val steps = sop["steps"]?.jsonArray?.take(3)?.mapNotNull { item ->
            val step = item.jsonObject
            val index = (step["index"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val title = (step["title"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            "$index:$title"
        } ?: emptyList()
        return steps.joinToString(" | ")
    }
}
