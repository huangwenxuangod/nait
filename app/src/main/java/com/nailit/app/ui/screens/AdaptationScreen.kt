package com.nailit.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.R
import com.nailit.app.core.model.NailPositionHint
import com.nailit.app.core.network.SupabaseManager
import com.nailit.app.core.preview.HandLandmarkEstimator
import com.nailit.app.core.preview.HandPhotoRuntime
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.SupabaseFunctionRepository
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TryOnBg = Color(0xFFFCFAF8)
private val TryOnSurface = Color(0xFFFFFFFF)
private val TryOnSurfaceSoft = Color(0xFFFFF7F5)
private val TryOnLine = Color(0xFFF1E7E2)
private val TryOnText = Color(0xFF1F1A17)
private val TryOnMuted = Color(0xFF8C817A)
private val TryOnAccent = Color(0xFFDFA297)
private val TryOnAccentSoft = Color(0xFFE9B7AE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptationScreen(
    selectedId: String?,
    sessionSnapshot: NailSessionSnapshot?,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { SupabaseFunctionRepository() }
    val scope = rememberCoroutineScope()
    val activeSession = NailSessionRuntime.current ?: sessionSnapshot

    var remoteTryOnBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }
    var debugErrorText by remember { mutableStateOf<String?>(null) }

    fun resetToCapture() {
        HandPhotoRuntime.currentBitmap = null
        remoteTryOnBitmap = null
        debugErrorText = null
        isRendering = false
    }

    fun startTryOn(bitmap: Bitmap) {
        val initialSession = NailSessionRuntime.current ?: activeSession
        if (initialSession == null) {
            debugErrorText = "TRYON_SESSION_MISSING: 没有活跃会话，请返回首页重新开始。"
            return
        }

        scope.launch {
            isRendering = true
            remoteTryOnBitmap = null
            debugErrorText = null

            runCatching {
                var session = NailSessionRuntime.current ?: initialSession
                if (session.sessionId.startsWith("local-")) {
                    repeat(25) {
                        delay(200)
                        val refreshed = NailSessionRuntime.current
                        if (refreshed != null && !refreshed.sessionId.startsWith("local-")) {
                            session = refreshed
                            return@repeat
                        }
                    }
                    session = NailSessionRuntime.current ?: session
                }
                if (session.sessionId.startsWith("local-")) {
                    error("TRYON_SESSION_NOT_READY: 远端会话还没创建完成")
                }

                NailSessionRuntime.current = (NailSessionRuntime.current ?: session).copy(
                    tryOnStatus = "try_on_pending",
                    tryOnError = null,
                )

                val upload = repository.prepareAssetUpload(
                    sessionId = session.sessionId,
                    assetType = "hand_photo",
                    mimeType = "image/jpeg",
                )
                val uploadPath = upload.storage_path ?: error("TRYON_PREPARE_UPLOAD_PATH_EMPTY")
                val uploadAssetId = upload.asset_id ?: error("TRYON_PREPARE_UPLOAD_ASSET_ID_EMPTY")

                val publicUrl = SupabaseManager.uploadHandPhotoToPath(
                    photoBytes = bitmapToJpegBytes(bitmap),
                    storagePath = uploadPath,
                )

                repository.confirmAssetUpload(
                    sessionId = session.sessionId,
                    assetId = uploadAssetId,
                    assetType = "hand_photo",
                    storagePath = uploadPath,
                )

                val nailHints = HandLandmarkEstimator.estimate(context, bitmap)
                    .ifEmpty { estimateFallbackNailPositionHints() }

                val tryOn = repository.createTryOn(
                    sessionId = session.sessionId,
                    nailPositionHints = nailHints,
                )
                NailSessionRuntime.current = (NailSessionRuntime.current ?: session).copy(
                    status = tryOn.status ?: "try_on_pending",
                    handAssetId = uploadAssetId,
                    handStoragePath = uploadPath,
                    handPhotoUrl = publicUrl,
                    tryOnStatus = tryOn.status ?: "try_on_pending",
                    targetImagePath = null,
                    tryOnError = null,
                )

                NailSessionRuntime.backgroundScope.launch {
                    runCatching {
                        repository.renderTryOn(
                            sessionId = session.sessionId,
                            nailPositionHints = nailHints,
                        )
                    }.onFailure { error ->
                        NailSessionRuntime.current = (NailSessionRuntime.current ?: session).copy(
                            tryOnStatus = "failed",
                            tryOnError = error.message ?: "TRYON_RENDER_TRIGGER_FAILED",
                        )
                    }
                }

                val tryOnPath = waitForTryOnPath(repository, session.sessionId)
                    ?: error("TRYON_RESULT_TIMEOUT: 试戴结果生成超时，请稍后再试。")
                val tryOnBitmap = loadRemoteBitmap(tryOnPath)
                    ?: error("TRYON_RESULT_LOAD_FAILED: 结果图路径存在，但图片加载失败。")

                NailSessionRuntime.current = (NailSessionRuntime.current ?: session).copy(
                    status = "try_on_ready",
                    tryOnStatus = "try_on_ready",
                    targetImagePath = tryOnPath,
                    tryOnError = null,
                )

                remoteTryOnBitmap = tryOnBitmap
            }.onFailure { error ->
                val rawError = error.message ?: error::class.java.simpleName
                NailSessionRuntime.current = (NailSessionRuntime.current ?: initialSession).copy(
                    tryOnStatus = "failed",
                    tryOnError = rawError,
                )
                debugErrorText = rawError
            }

            isRendering = false
        }
    }

    val consumeCapturedBitmap: (Bitmap) -> Unit = { bitmap ->
        HandPhotoRuntime.currentBitmap = bitmap
        remoteTryOnBitmap = null
        debugErrorText = null
        startTryOn(bitmap)
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val bitmap = uri?.let { loadBitmapFromUri(context, it) }
        if (bitmap != null) {
            consumeCapturedBitmap(bitmap)
        } else if (uri != null) {
            debugErrorText = "TRYON_PICK_FAILED: 图片读取失败，请重新选择。"
        }
    }

    LaunchedEffect(activeSession?.targetImagePath, activeSession?.tryOnStatus) {
        when {
            activeSession?.targetImagePath != null -> {
                isRendering = true
                remoteTryOnBitmap = loadRemoteBitmap(activeSession.targetImagePath)
                isRendering = false
                if (remoteTryOnBitmap == null) {
                    debugErrorText = "TRYON_RESULT_LOAD_FAILED: 结果图路径存在，但图片加载失败。"
                }
            }
            activeSession?.tryOnStatus == "try_on_pending" -> {
                isRendering = true
            }
            activeSession?.tryOnStatus == "failed" -> {
                isRendering = false
                debugErrorText = activeSession.tryOnError ?: "TRYON_FAILED: 未知错误"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "",
                        color = TryOnText,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TryOnText,
                        )
                    }
                },
                actions = {
                    if (!isRendering && remoteTryOnBitmap == null) {
                        IconButton(
                            onClick = {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "从相册上传",
                                tint = TryOnText,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TryOnBg),
            )
        },
        containerColor = TryOnBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (remoteTryOnBitmap == null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isRendering) "AI 正在试戴" else "开始试戴前，先摆好手势",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = TryOnText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.8).sp,
                        ),
                    )
                    Text(
                        text = if (isRendering) {
                            "已经开始生成，不需要再点第二次。"
                        } else {
                            "直接参考示意图调整手势，准备好后在当前页面上传手图。"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TryOnMuted,
                            lineHeight = 21.sp,
                        ),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(TryOnSurface)
                    .border(1.dp, TryOnLine, RoundedCornerShape(30.dp)),
            ) {
                when {
                    remoteTryOnBitmap != null -> {
                        Image(
                            bitmap = remoteTryOnBitmap!!.asImageBitmap(),
                            contentDescription = "AI try-on result",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    isRendering -> {
                        TryOnLoadingStage(
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        TryOnGuideStage(
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            if (!debugErrorText.isNullOrBlank()) {
                Surface(
                    color = TryOnSurfaceSoft,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = debugErrorText.orEmpty(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9E2A2B),
                            lineHeight = 20.sp,
                        ),
                    )
                }
            }

            when {
                remoteTryOnBitmap != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { resetToCapture() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = TryOnText,
                            ),
                        ) {
                            Text(
                                text = "重试",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                )
                            )
                        }
                        Button(
                            onClick = onContinue,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TryOnAccent,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = "语音继续",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                )
                            )
                        }
                    }
                }
                !isRendering -> {
                    Button(
                        onClick = {
                            pickMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TryOnAccent,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "准备好",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TryOnGuideStage(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF9F6), Color(0xFFF7EFE9)),
                )
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.try_on_hand_guide),
            contentDescription = "试戴手势引导图",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun TryOnLoadingStage(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF8F4), Color(0xFFF8EEE8)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    color = TryOnAccent,
                    strokeWidth = 2.5.dp,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "正在生成试戴图",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TryOnText,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    text = "会直接基于你的手图开始处理",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TryOnMuted,
                    ),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == 1) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == 1) TryOnAccent else TryOnAccentSoft.copy(alpha = 0.6f)
                            ),
                    )
                }
            }
        }
    }
}

private suspend fun loadRemoteBitmap(storagePath: String): Bitmap? {
    return runCatching {
        val bytes = SupabaseManager.client.storage
            .from("nail-it-assets")
            .downloadAuthenticated(storagePath)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun bitmapToJpegBytes(bitmap: Bitmap): ByteArray {
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
    return output.toByteArray()
}

private fun estimateFallbackNailPositionHints(): List<NailPositionHint> {
    return listOf(
        NailPositionHint("index", 0.22f, 0.34f, 0.08f, 0.13f, -32f),
        NailPositionHint("middle", 0.43f, 0.29f, 0.085f, 0.14f, -14f),
        NailPositionHint("ring", 0.63f, 0.34f, 0.08f, 0.13f, 6f),
        NailPositionHint("pinky", 0.82f, 0.49f, 0.065f, 0.11f, 18f),
        NailPositionHint("thumb", 0.12f, 0.56f, 0.10f, 0.15f, -48f),
    )
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()
}

private suspend fun waitForTryOnPath(
    repository: SupabaseFunctionRepository,
    sessionId: String,
): String? {
    repeat(90) {
        val path = repository.fetchTryOnImagePath(sessionId)
        if (!path.isNullOrBlank()) return path
        delay(2000)
    }
    return null
}
