package com.nailit.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionRequest(
    val install_id: String,
    val source_type: String,
)

@Serializable
data class CreateSessionResponse(
    val session_id: String? = null,
    val status: String? = null,
)

@Serializable
data class SubmitSourceLinkRequest(
    val session_id: String,
    val source_url: String,
)

@Serializable
data class SubmitSourceLinkResponse(
    val session_id: String? = null,
    val status: String? = null,
)

@Serializable
data class PrepareAssetUploadRequest(
    val session_id: String,
    val asset_type: String,
    val mime_type: String,
)

@Serializable
data class PrepareAssetUploadResponse(
    val asset_id: String? = null,
    val storage_path: String? = null,
    val bucket: String? = null,
)

@Serializable
data class ConfirmAssetUploadRequest(
    val session_id: String,
    val asset_id: String,
    val asset_type: String,
    val storage_path: String,
)

@Serializable
data class ConfirmAssetUploadResponse(
    val ok: Boolean = false,
)

@Serializable
data class CreateTryOnRequest(
    val session_id: String,
)

@Serializable
data class CreateTryOnResponse(
    val session_id: String? = null,
    val status: String? = null,
)

@Serializable
data class GenerateExecutionPackageRequest(
    val session_id: String,
)

@Serializable
data class GenerateExecutionPackageResponse(
    val session_id: String? = null,
    val status: String? = null,
)

@Serializable
data class CreateRealtimeTokenResponse(
    val token: String,
    val expires_at: String? = null,
    val websocket_url: String,
    val model: String,
)

@Serializable
data class ExecutionPackageResponse(
    val session_id: String,
    val style_name: String,
    val target_image_path: String? = null,
    val estimated_total_minutes: Int = 0,
    val steps: List<ExecutionStep> = emptyList(),
)

@Serializable
data class ExecutionStep(
    val id: String,
    val title: String,
    val instruction: String,
    val duration_sec: Int = 0,
    val needs_timer: Boolean = false,
    val needs_visual_check: Boolean = false,
    @SerialName("voice_goal")
    val voiceGoal: String = "",
    @SerialName("voice_shortcut")
    val voiceShortcut: String = "",
)
