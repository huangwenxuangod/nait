package com.nailit.app.core.preview

import com.nailit.app.BuildConfig
import com.nailit.app.core.model.ConfirmAssetUploadRequest
import com.nailit.app.core.model.ConfirmAssetUploadResponse
import com.nailit.app.core.model.CreateRealtimeTokenResponse
import com.nailit.app.core.model.CreateSessionRequest
import com.nailit.app.core.model.CreateSessionResponse
import com.nailit.app.core.model.NailPositionHint
import com.nailit.app.core.model.CreateTryOnRequest
import com.nailit.app.core.model.CreateTryOnResponse
import com.nailit.app.core.model.RenderTryOnRequest
import com.nailit.app.core.model.RenderTryOnResponse
import com.nailit.app.core.model.ExecutionPackageResponse
import com.nailit.app.core.model.GenerateExecutionPackageRequest
import com.nailit.app.core.model.GenerateExecutionPackageResponse
import com.nailit.app.core.model.PrepareAssetUploadRequest
import com.nailit.app.core.model.PrepareAssetUploadResponse
import com.nailit.app.core.model.SubmitSourceLinkRequest
import com.nailit.app.core.model.SubmitSourceLinkResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
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
        install(HttpTimeout) {
            requestTimeoutMillis = 60000L // 60s for AI tasks
            connectTimeoutMillis = 30000L // 30s connection
            socketTimeoutMillis = 60000L  // 60s socket read
        }
    }

    private val longRunningClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 240000L
            connectTimeoutMillis = 30000L
            socketTimeoutMillis = 240000L
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

        return safePost(
            functionName = "create_session",
            requestBody = CreateSessionRequest(
                install_id = installId,
                source_type = sourceType,
            )
        )
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

        return safePost(
            functionName = "submit_source_link",
            requestBody = SubmitSourceLinkRequest(
                session_id = sessionId,
                source_url = sourceUrl,
            )
        )
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

        return safePost(
            functionName = "prepare_asset_upload",
            requestBody = PrepareAssetUploadRequest(
                session_id = sessionId,
                asset_type = assetType,
                mime_type = mimeType,
            )
        )
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

        return safePost(
            functionName = "confirm_asset_upload",
            requestBody = ConfirmAssetUploadRequest(
                session_id = sessionId,
                asset_id = assetId,
                asset_type = assetType,
                storage_path = storagePath,
            )
        )
    }

    suspend fun createTryOn(
        sessionId: String,
        nailPositionHints: List<NailPositionHint> = emptyList(),
    ): CreateTryOnResponse {
        if (!isConfigured()) {
            return CreateTryOnResponse(
                session_id = sessionId,
                status = "try_on_ready",
            )
        }

        return safePost(
            functionName = "create_try_on",
            requestBody = CreateTryOnRequest(
                session_id = sessionId,
                nail_position_hints = nailPositionHints,
            )
        )
    }

    suspend fun renderTryOn(
        sessionId: String,
        nailPositionHints: List<NailPositionHint> = emptyList(),
    ): RenderTryOnResponse {
        if (!isConfigured()) {
            return RenderTryOnResponse(
                session_id = sessionId,
                status = "try_on_ready",
            )
        }

        return safePost(
            functionName = "render_try_on",
            requestBody = RenderTryOnRequest(
                session_id = sessionId,
                nail_position_hints = nailPositionHints,
            ),
            useLongRunningClient = true,
        )
    }

    suspend fun generateExecutionPackage(sessionId: String): GenerateExecutionPackageResponse {
        if (!isConfigured()) {
            return GenerateExecutionPackageResponse(
                session_id = sessionId,
                status = "execution_package_ready",
            )
        }

        return safePost(
            functionName = "generate_execution_package",
            requestBody = GenerateExecutionPackageRequest(session_id = sessionId)
        )
    }

    suspend fun fetchExecutionPackage(sessionId: String): ExecutionPackageResponse? {
        if (!isConfigured()) return null

        val raw = client.get(
            "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/sop_guides?session_id=eq.$sessionId&select=sop_json"
        ) {
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }.body<String>()

        val records = json.parseToJsonElement(raw).jsonArray
        if (records.isEmpty()) return null

        val payload = records.first().jsonObject["sop_json"] ?: return null
        return json.decodeFromJsonElement(ExecutionPackageResponse.serializer(), payload)
    }

    suspend fun createRealtimeToken(): CreateRealtimeTokenResponse {
        if (!isConfigured()) {
            return CreateRealtimeTokenResponse(
                token = "demo-qwen-token",
                websocket_url = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime",
                model = "qwen3.5-omni-plus-realtime",
            )
        }

        val httpResponse = client.post("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/create_qwen_temp_token") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(emptyMap<String, String>())
        }

        val rawBody = httpResponse.body<String>()
        if (httpResponse.status.value >= 400) {
            val errorMessage = runCatching {
                json.parseToJsonElement(rawBody).jsonObject["error"]?.jsonPrimitive?.contentOrNull
            }.getOrNull() ?: rawBody.ifBlank { "HTTP error ${httpResponse.status.value}" }
            throw Exception("REALTIME_TOKEN_FAILED: $errorMessage")
        }

        return runCatching {
            json.decodeFromString(CreateRealtimeTokenResponse.serializer(), rawBody)
        }.getOrElse { error ->
            throw Exception(
                "REALTIME_TOKEN_SCHEMA_INVALID: ${error.message ?: "unknown"} | body=$rawBody"
            )
        }
    }

    suspend fun fetchSourceParse(sessionId: String): JsonObject? {
        return fetchJsonPayload(
            table = "source_parses",
            sessionId = sessionId,
            column = "parse_json",
        )
    }

    suspend fun fetchTryOnResult(sessionId: String): JsonObject? {
        return fetchJsonPayload(
            table = "try_on_results",
            sessionId = sessionId,
            column = "result_json",
        )
    }

    suspend fun fetchTryOnImagePath(sessionId: String): String? {
        if (!isConfigured()) return null

        val raw = client.get(
            "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/try_on_results?session_id=eq.$sessionId&select=result_image_path"
        ) {
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }.body<String>()

        val records = json.parseToJsonElement(raw).jsonArray
        if (records.isEmpty()) return null
        return (records.first().jsonObject["result_image_path"] as? JsonPrimitive)?.contentOrNull
    }

    suspend fun fetchSessionStatus(sessionId: String): String? {
        if (!isConfigured()) return null

        val raw = client.get(
            "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/sessions?id=eq.$sessionId&select=status"
        ) {
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }.body<String>()

        val records = json.parseToJsonElement(raw).jsonArray
        if (records.isEmpty()) return null
        return (records.first().jsonObject["status"] as? JsonPrimitive)?.contentOrNull
    }

    suspend fun fetchBom(sessionId: String): JsonObject? {
        return fetchJsonPayload(
            table = "bom_lists",
            sessionId = sessionId,
            column = "bom_json",
        )
    }

    suspend fun fetchSop(sessionId: String): JsonObject? {
        return fetchJsonPayload(
            table = "sop_guides",
            sessionId = sessionId,
            column = "sop_json",
        )
    }

    private suspend fun fetchJsonPayload(
        table: String,
        sessionId: String,
        column: String,
    ): JsonObject? {
        if (!isConfigured()) return null

        val raw = client.get(
            "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/$table?session_id=eq.$sessionId&select=$column"
        ) {
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }.body<String>()

        val records = json.parseToJsonElement(raw).jsonArray
        if (records.isEmpty()) return null

        val first = records.first().jsonObject
        return first[column]?.jsonObject
    }

    private suspend inline fun <reified REQ : Any, reified RES : Any> safePost(
        functionName: String,
        requestBody: REQ,
        useLongRunningClient: Boolean = false,
    ): RES {
        val httpClient = if (useLongRunningClient) longRunningClient else client
        val httpResponse = httpClient.post("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/$functionName") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setBody(requestBody)
        }

        if (httpResponse.status.value >= 400) {
            val errorBody = runCatching { httpResponse.body<JsonObject>() }.getOrNull()
            val errorMessage = errorBody?.get("error")?.jsonPrimitive?.contentOrNull
                ?: "HTTP error ${httpResponse.status.value}"
            throw Exception(errorMessage)
        }

        return httpResponse.body()
    }
}
