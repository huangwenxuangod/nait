package com.nailit.app.core.preview

import com.nailit.app.BuildConfig
import com.nailit.app.core.model.CreateRealtimeTokenResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import android.util.Base64

data class QwenRealtimeState(
    val status: QwenRealtimeStatus = QwenRealtimeStatus.Idle,
    val lastEvent: String = "",
    val lastTranscript: String = "",
    val lastAudioDeltaBase64: String = "",
    val responseDoneCount: Int = 0,
    val errorMessage: String = "",
)

enum class QwenRealtimeStatus {
    Idle,
    Connecting,
    Connected,
    Error,
    Closed,
}

class QwenRealtimeSessionManager {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _state = MutableStateFlow(QwenRealtimeState())
    val state: StateFlow<QwenRealtimeState> = _state.asStateFlow()

    private var webSocket: WebSocket? = null

    fun connect(tokenPayload: CreateRealtimeTokenResponse) {
        _state.value = QwenRealtimeState(
            status = QwenRealtimeStatus.Connecting,
            lastEvent = "opening_socket",
        )

        val request = Request.Builder()
            .url(tokenPayload.websocket_url.ifBlank { BuildConfig.QWEN_REALTIME_WS_URL })
            .addHeader("Authorization", "Bearer ${tokenPayload.token}")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _state.value = QwenRealtimeState(
                    status = QwenRealtimeStatus.Connected,
                    lastEvent = "socket_open",
                )
                webSocket.send(buildSessionUpdate(tokenPayload.model))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingText(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _state.value = _state.value.copy(
                    status = QwenRealtimeStatus.Closed,
                    lastEvent = "closing:$code:$reason",
                )
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _state.value = _state.value.copy(
                    status = QwenRealtimeStatus.Closed,
                    lastEvent = "closed:$code:$reason",
                )
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _state.value = _state.value.copy(
                    status = QwenRealtimeStatus.Error,
                    lastEvent = t.message ?: "socket_failure",
                    errorMessage = t.message ?: "socket_failure",
                )
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "client_close")
        webSocket = null
        _state.value = QwenRealtimeState(
            status = QwenRealtimeStatus.Closed,
            lastEvent = "client_close",
        )
    }

    fun appendAudio(bytes: ByteArray) {
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        webSocket?.send(
            """
            {
              "type": "input_audio_buffer.append",
              "audio": "$base64"
            }
            """.trimIndent()
        )
    }

    fun appendImage(base64Jpeg: String) {
        webSocket?.send(
            """
            {
              "type": "input_image_buffer.append",
              "image": "$base64Jpeg"
            }
            """.trimIndent()
        )
    }

    fun commitTurn() {
        webSocket?.send("""{"type":"input_audio_buffer.commit"}""")
    }

    fun requestResponse(instruction: String? = null) {
        val responsePayload = if (instruction.isNullOrBlank()) {
            """{"type":"response.create"}"""
        } else {
            """
            {
              "type": "response.create",
              "response": {
                "instructions": "$instruction"
              }
            }
            """.trimIndent()
        }
        webSocket?.send(responsePayload)
    }

    fun resetTranscript() {
        _state.value = _state.value.copy(
            lastTranscript = "",
            lastAudioDeltaBase64 = "",
        )
    }

    private fun buildSessionUpdate(model: String): String {
        return """
            {
              "type": "session.update",
              "session": {
                "model": "${model.ifBlank { BuildConfig.QWEN_REALTIME_MODEL }}",
                "modalities": ["text", "audio"],
                "input_audio_format": "pcm16",
                "output_audio_format": "pcm16",
                "turn_detection": {
                  "type": "server_vad"
                }
              }
            }
        """.trimIndent()
    }

    private fun handleIncomingText(text: String) {
        runCatching {
            val payload = json.parseToJsonElement(text).jsonObject
            val type = payload["type"]?.jsonPrimitive?.content ?: return
            when (type) {
                "response.audio.delta" -> {
                    val delta = payload["delta"]?.jsonPrimitive?.content.orEmpty()
                    _state.value = _state.value.copy(
                        status = QwenRealtimeStatus.Connected,
                        lastEvent = "response.audio.delta",
                        lastAudioDeltaBase64 = delta,
                    )
                }
                "response.audio_transcript.delta" -> {
                    val delta = payload["delta"]?.jsonPrimitive?.content.orEmpty()
                    _state.value = _state.value.copy(
                        status = QwenRealtimeStatus.Connected,
                        lastEvent = "response.audio_transcript.delta",
                        lastTranscript = _state.value.lastTranscript + delta,
                    )
                }
                "response.done" -> {
                    _state.value = _state.value.copy(
                        status = QwenRealtimeStatus.Connected,
                        lastEvent = "response_done",
                        responseDoneCount = _state.value.responseDoneCount + 1,
                    )
                }
                "error" -> {
                    val message = payload["message"]?.jsonPrimitive?.content ?: "realtime_error"
                    _state.value = _state.value.copy(
                        status = QwenRealtimeStatus.Error,
                        lastEvent = "error",
                        errorMessage = message,
                    )
                }
                else -> {
                    _state.value = _state.value.copy(
                        status = QwenRealtimeStatus.Connected,
                        lastEvent = type,
                    )
                }
            }
        }
    }
}
