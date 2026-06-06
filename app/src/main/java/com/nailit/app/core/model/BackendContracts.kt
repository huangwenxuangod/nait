package com.nailit.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionRequest(
    val install_id: String,
    val source_type: String,
)

@Serializable
data class CreateSessionResponse(
    val session_id: String,
    val status: String,
)

@Serializable
data class SubmitSourceLinkRequest(
    val session_id: String,
    val source_url: String,
)

@Serializable
data class SubmitSourceLinkResponse(
    val session_id: String,
    val status: String,
)

@Serializable
data class PrepareAssetUploadRequest(
    val session_id: String,
    val asset_type: String,
    val mime_type: String,
)

@Serializable
data class PrepareAssetUploadResponse(
    val asset_id: String,
    val storage_path: String,
    val bucket: String,
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
    val ok: Boolean,
)

@Serializable
data class CreateTryOnRequest(
    val session_id: String,
)

@Serializable
data class CreateTryOnResponse(
    val session_id: String,
    val status: String,
)

@Serializable
data class GenerateExecutionPackageRequest(
    val session_id: String,
)

@Serializable
data class GenerateExecutionPackageResponse(
    val session_id: String,
    val status: String,
)
