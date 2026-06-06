package com.nailit.app.core.preview

import com.nailit.app.BuildConfig
import com.nailit.app.core.model.OpenAiChatRequest
import com.nailit.app.core.model.VideoChatMessage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class VideoChatStreamingRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.NONE
        }
    }

    fun streamReply(
        history: List<VideoChatMessage>,
        userPrompt: String,
        frameBase64: String?,
        includeVideoFrame: Boolean,
    ): Flow<String> {
        return if (BuildConfig.OPENAI_API_KEY.isBlank()) {
            demoStream(userPrompt, includeVideoFrame)
        } else {
            providerStream(history, userPrompt, frameBase64, includeVideoFrame)
        }
    }

    private fun demoStream(
        userPrompt: String,
        includeVideoFrame: Boolean,
    ): Flow<String> = flow {
        val fullText = when {
            "显黑" in userPrompt -> "我先给你一个直觉判断：这类颜色如果你是黄皮，确实有可能显黑。你把手再靠近镜头一点，我可以继续帮你看得更准。"
            "步骤" in userPrompt || "下一步" in userPrompt -> "如果你现在已经打磨完甲面，下一步就先薄涂底胶。不要急着堆厚度，先求均匀。照灯前再给我看一眼。"
            includeVideoFrame -> "我已经看到你当前镜头画面了。先说结论：这个交互形态是对的，视频聊天感已经能成立。下一步最值得做的是让模型针对当前画面给更具体的美甲建议。"
            else -> "现在这版会先走最轻的视频聊天逻辑：你说话，我拿你的文字和当前画面去问模型，再把回答流式回给你。"
        }

        fullText.forEachIndexed { index, _ ->
            emit(fullText.substring(0, index + 1))
            delay(18)
        }
    }

    private fun providerStream(
        history: List<VideoChatMessage>,
        userPrompt: String,
        frameBase64: String?,
        includeVideoFrame: Boolean,
    ): Flow<String> = flow {
        val request = OpenAiChatRequest(
            model = BuildConfig.OPENAI_MODEL,
            stream = false,
            messages = VideoChatPromptBuilder.buildMessages(
                history = history,
                userPrompt = userPrompt,
                frameBase64 = frameBase64,
                includeVideoFrame = includeVideoFrame,
            )
        )

        val responseText = client.post("${BuildConfig.OPENAI_BASE_URL.trimEnd('/')}/chat/completions") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.OPENAI_API_KEY}")
            setBody(request)
        }.body<String>()

        val answer = extractAssistantText(responseText).ifBlank {
            "我已经收到你的画面和问题了，但模型这次没有返回可用文本。你可以再说一次，或者先关闭视频理解只测文本聊天。"
        }

        answer.forEachIndexed { index, _ ->
            emit(answer.substring(0, index + 1))
            delay(10)
        }
    }

    private fun extractAssistantText(raw: String): String {
        val regex = """"content"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
        val matches = regex.findAll(raw).map { it.groupValues[1] }.toList()
        if (matches.isEmpty()) return ""
        return matches.last()
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\/", "/")
    }
}
