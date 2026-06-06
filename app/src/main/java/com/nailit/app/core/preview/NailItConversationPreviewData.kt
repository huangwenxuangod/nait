package com.nailit.app.core.preview

import com.nailit.app.core.model.NailItCard
import com.nailit.app.core.model.NailItCardType
import com.nailit.app.core.model.NailItMessage
import com.nailit.app.core.model.NailItQuickAction
import com.nailit.app.core.model.NailItSender

object NailItConversationPreviewData {
    val starterMessages = listOf(
        NailItMessage(
            id = "welcome",
            sender = NailItSender.Assistant,
            text = "我是 Nail-It，你可以直接把抖音美甲链接、分享文案，或者一句想做的款式发给我。我会先告诉你能不能解析，再把结果拆成试戴、材料和 SOP。"
        ),
        NailItMessage(
            id = "scope",
            sender = NailItSender.Assistant,
            text = "如果你想做成像豆包一样的对话式美甲助手，P0 最稳的是：对话承接 + 视频理解 + OCR 补强 + BOM/SOP 卡片化输出。实时万能 AI 先别承诺。"
        ),
        NailItMessage(
            id = "source-plan",
            sender = NailItSender.Assistant,
            text = "我已经按你现在的 PRD 拆好第一阶段执行方案。",
            card = NailItCard(
                type = NailItCardType.SourcePlan,
                title = "第一阶段主链路",
                subtitle = "抖音链接优先，上传录屏/截图兜底",
                bullets = listOf(
                    "1. 贴链接或分享文案，先走 source ingestion",
                    "2. 解析视频款式、字幕、关键帧，输出结构化美甲步骤",
                    "3. 结合手部照片生成虚拟试戴，再给 BOM 与 SOP"
                ),
                cta = "开始对话式拆解"
            )
        )
    )

    val quickActions = listOf(
        NailItQuickAction(
            label = "评估可行性",
            prompt = "你先告诉我，这个项目做到像豆包一样的对话体验，哪些能做，哪些现在不能承诺？"
        ),
        NailItQuickAction(
            label = "搜开源方案",
            prompt = "按照这个 PRD，把现成开源方案按对话、语音、视频理解、试戴、工作流都帮我搜出来。"
        ),
        NailItQuickAction(
            label = "开始做 P0",
            prompt = "不要再解释，直接按对话式入口开始实现 P0。"
        ),
        NailItQuickAction(
            label = "设计试戴链路",
            prompt = "如果我最想要 AI 试戴，你先给我一个最小可行的试戴架构。"
        ),
    )

    fun replyFor(prompt: String): NailItMessage {
        val normalized = prompt.lowercase()
        return when {
            "开源" in prompt || "搜" in prompt || "github" in normalized -> NailItMessage(
                id = "reply-open-source-${prompt.hashCode()}",
                sender = NailItSender.Assistant,
                text = "开源层面我建议直接拼装，而不是幻想一个现成的“美甲豆包”。对话层、语音层、视频理解层、试穿/试戴层、状态机层都能找到积木，但没有一个仓库能一把梭。",
                card = NailItCard(
                    type = NailItCardType.Feasibility,
                    title = "开源积木建议",
                    subtitle = "优先找成熟通用模块，再为美甲场景做编排",
                    bullets = listOf(
                        "对话工作流：LangGraph / Rasa 适合编排 agent 状态机",
                        "Android 聊天体验：Compose 自定义消息流比硬套 SDK 更稳",
                        "本地语音：sherpa-onnx、Vosk、whisper.cpp 都有 Android 路线",
                        "视觉与手部：MediaPipe Hands/Hand Landmarker 能给你稳定的手部基础能力",
                        "虚拟试戴：现成多是服饰 try-on，真正美甲试戴要自己做 hand+nail 贴合层"
                    ),
                    cta = "继续细化技术选型"
                )
            )

            "可行" in prompt || "豆包" in prompt || "能做" in prompt -> NailItMessage(
                id = "reply-feasibility-${prompt.hashCode()}",
                sender = NailItSender.Assistant,
                text = "如果你说的是“像豆包一样通过对话推进任务”，完全可以做；如果你说的是“像豆包一样又会聊又会看图又会稳定吃任意抖音链接还自动试戴”，首版不能这么承诺。",
                card = NailItCard(
                    type = NailItCardType.Feasibility,
                    title = "现在能做 vs 不能承诺",
                    subtitle = "把豆包式体验拆开看，会清楚很多",
                    bullets = listOf(
                        "能做：聊天主界面、上下文记忆、推荐下一步、BOM/SOP 卡片式输出",
                        "能做：手部照片上传后触发虚拟试戴任务，再把结果回灌到对话里",
                        "不能直接承诺：任意抖音链接 100% 可解析，这取决于内容接入层",
                        "不能直接承诺：高保真实时美甲 AR 试戴，P0 更适合做静态图生成"
                    ),
                    cta = "继续拆 P0 范围"
                )
            )

            "试戴" in prompt -> NailItMessage(
                id = "reply-tryon-${prompt.hashCode()}",
                sender = NailItSender.Assistant,
                text = "试戴在美甲场景是极强的冲动触发点。我们设计的静态 try-on 链路如下：",
                card = NailItCard(
                    type = NailItCardType.TryOnDecision,
                    title = "静态 Try-on 算法链路",
                    subtitle = "避免复杂的实时 3D 渲染，用高精度 2D 贴图加光影融合",
                    bullets = listOf(
                        "1. 客户端拍摄高清晰度手部照片，上传到 Supabase Storage",
                        "2. Cloud 调用手部检测模型，提取 5 个指甲的 Mask 与轮廓边界",
                        "3. 将从视频中提取的款式（如极光猫眼）通过 ControlNet/SDXL 局部重绘到指甲区域",
                        "4. 做边缘高斯模糊和高光融合，让美甲看起来贴合在手指上",
                        "5. 结果以图片形式回传给客户端，展示高清晰度试戴效果"
                    ),
                    cta = "看效果展示"
                )
            )

            else -> NailItMessage(
                id = "reply-default-${prompt.hashCode()}",
                sender = NailItSender.Assistant,
                text = "收到你的指令！我现在已经完全进入 Nail-It 指尖 SOP 模式。接下来我会把注意力集中在 1. 链接解析 2. 款式提取 3. AI 试戴 4. BOM 清单 5. 沉浸式 SOP 的全链路构建上。"
            )
        }
    }
}
