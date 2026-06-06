package com.nailit.app.core.preview

import com.nailit.app.BuildConfig
import com.nailit.app.core.model.ConfirmAssetUploadRequest
import com.nailit.app.core.model.ConfirmAssetUploadResponse
import com.nailit.app.core.model.CreateSessionRequest
import com.nailit.app.core.model.CreateSessionResponse
import com.nailit.app.core.model.CreateTryOnRequest
import com.nailit.app.core.model.CreateTryOnResponse
import com.nailit.app.core.model.GenerateExecutionPackageRequest
import com.nailit.app.core.model.GenerateExecutionPackageResponse
import com.nailit.app.core.model.PrepareAssetUploadRequest
import com.nailit.app.core.model.PrepareAssetUploadResponse
import com.nailit.app.core.model.SubmitSourceLinkRequest
import com.nailit.app.core.model.SubmitSourceLinkResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.UUID

class SupabaseFunctionRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    fun isConfigured(): Boolean {
        return BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
    }

    suspend fun createSession(
        installId: String,
        sourceType: String,
    ): CreateSessionResponse {
        if (!isConfigured()) {
            return CreateSessionResponse(
                session_id = "demo-session-${UUID.randomUUID()}",
                status = "draft",
            )
        }

        return client.post("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/create_session") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(
                CreateSessionRequest(
                    install_id = installId,
                    source_type = sourceType,
                )
            )
        }.body()
    }

    suspend fun submitSourceLink(
        sessionId: String,
        sourceUrl: String,
    ): SubmitSourceLinkResponse {
        if (!isConfigured()) {
            return SubmitSourceLinkResponse(
                session_id = sessionId,
                status = "source_parsing",
            )
        }

        return client.post("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/submit_source_link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(
                SubmitSourceLinkRequest(
                    session_id = sessionId,
                    source_url = sourceUrl,
                )
            )
        }.body()
    }

    suspend fun prepareAssetUpload(
        sessionId: String,
        assetType: String,
        mimeType: String,
    ): PrepareAssetUploadResponse {
        if (!isConfigured()) {
            val assetId = "demo-asset-${UUID.randomUUID()}"
            return PrepareAssetUploadResponse(
                asset_id = assetId,
                storage_path = "$sessionId/$assetType/$assetId.jpg",
                bucket = "nail-it-assets",
            )
        }

        return client.post("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/prepare_asset_upload") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(
                PrepareAssetUploadRequest(
                    session_id = sessionId,
                    asset_type = assetType,
                    mime_type = mimeType,
                )
            )
        }.body()
    }

    suspend fun confirmAssetUpload(
        sessionId: String,
        assetId: String,
        assetType: String,
        storagePath: String,
    ): ConfirmAssetUploadResponse {
        if (!isConfigured()) {
            return ConfirmAssetUploadResponse(ok = true)
        }

        return client.post("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/confirm_asset_upload") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(
                ConfirmAssetUploadRequest(
                    session_id = sessionId,
                    asset_id = assetId,
                    asset_type = assetType,
                    storage_path = storagePath,
                )
            )
        }.body()
    }

    suspend fun createTryOn(sessionId: String): CreateTryOnResponse {
        if (!isConfigured()) {
            return CreateTryOnResponse(
                session_id = sessionId,
                status = "try_on_ready",
            )
        }

        return client.post("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/create_try_on") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(CreateTryOnRequest(session_id = sessionId))
        }.body()
    }

    suspend fun generateExecutionPackage(sessionId: String): GenerateExecutionPackageResponse {
        if (!isConfigured()) {
            return GenerateExecutionPackageResponse(
                session_id = sessionId,
                status = "execution_package_ready",
            )
        }

        return client.post("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/generate_execution_package") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(GenerateExecutionPackageRequest(session_id = sessionId))
        }.body()
    }
}
