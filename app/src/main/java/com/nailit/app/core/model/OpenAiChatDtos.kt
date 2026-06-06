package com.nailit.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val stream: Boolean,
    val messages: List<OpenAiMessage>,
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: List<OpenAiContentPart>,
)

@Serializable
sealed interface OpenAiContentPart

@Serializable
@SerialName("text")
data class OpenAiTextPart(
    val type: String = "text",
    val text: String,
) : OpenAiContentPart

@Serializable
@SerialName("image_url")
data class OpenAiImagePart(
    val type: String = "image_url",
    @SerialName("image_url")
    val imageUrl: OpenAiImageUrl,
) : OpenAiContentPart

@Serializable
data class OpenAiImageUrl(
    val url: String,
)
