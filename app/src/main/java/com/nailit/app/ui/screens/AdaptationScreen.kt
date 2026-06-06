package com.nailit.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nailit.app.core.network.SupabaseManager
import com.nailit.app.core.preview.HandLandmarkEstimator
import com.nailit.app.core.preview.HandPhotoRuntime
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.SupabaseFunctionRepository
import com.nailit.app.core.model.NailPositionHint
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TryOnBg = Color(0xFFF7F2ED)
private val TryOnCard = Color(0xFFFFFEFC)
private val TryOnBorder = Color(0xFFE8DDD2)
private val TryOnText = Color(0xFF171311)
private val TryOnMuted = Color(0xFF80756D)
private val TryOnAccent = Color(0xFF281B19)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptationScreen(
    selectedId: String?,
    sessionSnapshot: NailSessionSnapshot?,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SupabaseFunctionRepository() }
    val scope = rememberCoroutineScope()
    val activeSession = NailSessionRuntime.current ?: sessionSnapshot

    var handBitmap by remember { mutableStateOf(HandPhotoRuntime.currentBitmap) }
    var remoteTryOnBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }
    var debugErrorText by remember { mutableStateOf<String?>(null) }
    var statusText by remember {
        mutableStateOf(
            if (handBitmap == null) "拍一张手图后会直接开始试戴。" else "正在准备试戴结果。"
        )
    }

    fun startTryOn(bitmapOverride: Bitmap? = handBitmap) {
        val initialSession = NailSessionRuntime.current ?: activeSession
        val bitmap = bitmapOverride
        if (initialSession == null) {
            statusText = "没有活跃会话，请返回首页重新开始。"
            return
        }
        if (bitmap == null) {
            statusText = "请先拍摄或上传手图。"
            return
        }

        scope.launch {
            isRendering = true
            remoteTryOnBitmap = null
            debugErrorText = null
            statusText = "正在准备试戴。"

            runCatching {
                var session = NailSessionRuntime.current ?: initialSession
                if (session.sessionId.startsWith("local-")) {
                    statusText = "正在同步会话..."
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

                statusText = "正在生成试戴图。"

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
                repository.fetchTryOnResult(session.sessionId)
                val tryOnPath = repository.fetchTryOnImagePath(session.sessionId)
                val tryOnBitmap = tryOnPath?.let { loadRemoteBitmap(it) }

                NailSessionRuntime.current = (NailSessionRuntime.current ?: session).copy(
                    status = tryOn.status ?: "try_on_ready",
                    handAssetId = uploadAssetId,
                    handStoragePath = uploadPath,
                    handPhotoUrl = publicUrl,
                    tryOnStatus = tryOn.status ?: "try_on_ready",
                    targetImagePath = tryOnPath,
                    tryOnError = null,
                )

                remoteTryOnBitmap = tryOnBitmap
                statusText = if (tryOnBitmap != null) {
                    "这是你的 AI 试戴结果。"
                } else {
                    "TRYON_RESULT_PATH_EMPTY: 已完成试戴调用，但没有取到结果图。"
                }
            }.onFailure { error ->
                val rawError = error.message ?: error::class.java.simpleName
                NailSessionRuntime.current = (NailSessionRuntime.current ?: initialSession).copy(
                    tryOnStatus = "failed",
                    tryOnError = rawError,
                )
                debugErrorText = rawError
                statusText = rawError
            }

            isRendering = false
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap = loadBitmapFromUri(context, uri)
        if (bitmap != null) {
            handBitmap = bitmap
            HandPhotoRuntime.currentBitmap = bitmap
            remoteTryOnBitmap = null
            debugErrorText = null
            startTryOn(bitmap)
        } else {
            statusText = "图片读取失败，请重新选择。"
        }
    }

    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            handBitmap = bitmap
            HandPhotoRuntime.currentBitmap = bitmap
            remoteTryOnBitmap = null
            debugErrorText = null
            startTryOn(bitmap)
        } else {
            statusText = "未拍到照片，请再试一次。"
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            takePicturePreviewLauncher.launch(null)
        } else {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    fun openCapture() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            takePicturePreviewLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(activeSession?.targetImagePath, activeSession?.tryOnStatus) {
        when {
            activeSession?.targetImagePath != null -> {
                isRendering = true
                remoteTryOnBitmap = loadRemoteBitmap(activeSession.targetImagePath)
                isRendering = false
                statusText = if (remoteTryOnBitmap != null) {
                    "这是你的 AI 试戴结果。"
                } else {
                    "TRYON_RESULT_LOAD_FAILED: 结果图路径存在，但图片加载失败。"
                }
            }
            activeSession?.tryOnStatus == "try_on_pending" -> {
                isRendering = true
                statusText = "正在生成试戴图。"
            }
            activeSession?.tryOnStatus == "failed" -> {
                isRendering = false
                val runtimeError = activeSession?.tryOnError ?: "TRYON_FAILED: 未知错误"
                debugErrorText = runtimeError
                statusText = runtimeError
            }
            handBitmap == null -> {
                statusText = "拍一张手图后会直接开始试戴。"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI 试戴",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TryOnText,
                            fontWeight = FontWeight.Bold,
                        )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TryOnBg)
            )
        },
        containerColor = TryOnBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "试戴结果",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = TryOnText,
                    fontWeight = FontWeight.Bold,
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(TryOnCard)
                    .border(1.dp, TryOnBorder, RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (handBitmap != null) {
                    SourceReadyPill(
                        onClick = { openCapture() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                    )
                }

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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CircularProgressIndicator(
                                color = TryOnAccent,
                                strokeWidth = 2.5.dp,
                            )
                            Text(
                                text = "AI 正在为你试戴",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TryOnText,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            )
                        }
                    }
                    handBitmap == null -> {
                        Text(
                            text = "拍张手图，直接开始试戴",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = TryOnMuted,
                            )
                        )
                    }
                    else -> {
                        Text(
                            text = "手图已上传，正在准备试戴结果",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = TryOnMuted,
                            )
                        )
                    }
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (debugErrorText != null) Color(0xFF9E2A2B) else TryOnMuted,
                    lineHeight = 20.sp,
                )
            )

            if (debugErrorText != null) {
                TextButton(
                    onClick = { debugErrorText = null },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "清除错误",
                        color = TryOnAccent,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            if (handBitmap == null) {
                Button(
                    onClick = { openCapture() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TryOnAccent,
                        contentColor = Color.White,
                    )
                ) {
                    Text(
                        text = "拍张手图直接试戴",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        )
                    )
                }
            } else if (remoteTryOnBitmap != null) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TryOnAccent,
                        contentColor = Color.White,
                    )
                ) {
                    Text(
                        text = "语音继续",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        )
                    )
                }

                Spacer(modifier = Modifier.size(0.dp))

                Button(
                    onClick = { openCapture() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(1.dp, TryOnBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = TryOnAccent,
                    )
                ) {
                    Text(
                        text = "换张手图重试",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        )
                    )
                }
            } else {
                Button(
                    onClick = { openCapture() },
                    enabled = !isRendering,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = TryOnAccent,
                    )
                ) {
                    Text(
                        text = "重新拍图",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        )
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

@Composable
private fun SourceReadyPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.92f),
            contentColor = TryOnAccent,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "已上传手图",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                )
            )
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = TryOnMuted,
            )
        }
    }
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
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()
}
