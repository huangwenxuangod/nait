package com.nailit.app.core.preview

import com.nailit.app.core.model.VideoChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class VideoChatStreamingRepository {
    fun streamReply(
        history: List<VideoChatMessage>,
        userPrompt: String,
        frameBase64: String?,
        includeVideoFrame: Boolean,
    ): Flow<String> {
        return demoStream(userPrompt, includeVideoFrame)
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

}
