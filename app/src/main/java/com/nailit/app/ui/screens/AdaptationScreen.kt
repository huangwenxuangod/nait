package com.nailit.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.ImageFormat
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.nailit.app.core.model.NailPositionHint
import com.nailit.app.core.network.SupabaseManager
import com.nailit.app.core.preview.HandLandmarkEstimator
import com.nailit.app.core.preview.HandPhotoRuntime
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.SupabaseFunctionRepository
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember { SupabaseFunctionRepository() }
    val scope = rememberCoroutineScope()
    val activeSession = NailSessionRuntime.current ?: sessionSnapshot
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var handBitmap by remember { mutableStateOf(HandPhotoRuntime.currentBitmap) }
    var remoteTryOnBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }
    var debugErrorText by remember { mutableStateOf<String?>(null) }
    var captureNonce by remember { mutableLongStateOf(0L) }
    var latestPreviewFrame by remember { mutableStateOf<Bitmap?>(null) }
    var latestFrameAtMs by remember { mutableLongStateOf(0L) }

    fun resetToCapture() {
        handBitmap = null
        remoteTryOnBitmap = null
        debugErrorText = null
        isRendering = false
        captureNonce += 1
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
        handBitmap = bitmap
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

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            debugErrorText = "TRYON_CAMERA_DENIED: 相机权限未开启，请改用相册上传。"
        }
    }

    fun captureCurrentFrame() {
        val frame = latestPreviewFrame
        if (frame == null) {
            debugErrorText = "TRYON_CAPTURE_EMPTY: 还没拿到相机画面，请稍等一秒再拍。"
            return
        }
        val age = System.currentTimeMillis() - latestFrameAtMs
        if (age > 3000) {
            debugErrorText = "TRYON_CAPTURE_STALE: 当前预览帧已过期，请再拍一次。"
            return
        }
        consumeCapturedBitmap(frame)
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

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
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
                        text = if (isRendering) "AI 正在试戴" else "拍一张手图",
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
                            "把手放进虚线框里，拍完就直接出结果。"
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
                        TryOnCameraStage(
                            hasCameraPermission = hasCameraPermission,
                            onRequestPermission = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            lifecycleOwner = lifecycleOwner,
                            cameraExecutor = cameraExecutor,
                            onCameraProviderReady = { provider ->
                                cameraProvider = provider
                            },
                            onPreviewFrame = { bitmap ->
                                latestPreviewFrame = bitmap
                                latestFrameAtMs = System.currentTimeMillis()
                            },
                            captureNonce = captureNonce,
                        )

                        CaptureOverlay(
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
                        onClick = { captureCurrentFrame() },
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
                            text = "拍这一张",
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
private fun TryOnCameraStage(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    onPreviewFrame: (Bitmap) -> Unit,
    captureNonce: Long,
) {
    val context = LocalContext.current

    if (hasCameraPermission) {
        AndroidView(
            factory = {
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    bindTryOnCameraPreview(
                        previewView = this,
                        lifecycleOwner = lifecycleOwner,
                        analyzerExecutor = cameraExecutor,
                        onCameraProviderReady = onCameraProviderReady,
                        onFrameCaptured = onPreviewFrame,
                    )
                }
            },
            update = {
                bindTryOnCameraPreview(
                    previewView = it,
                    lifecycleOwner = lifecycleOwner,
                    analyzerExecutor = cameraExecutor,
                    onCameraProviderReady = onCameraProviderReady,
                    onFrameCaptured = onPreviewFrame,
                )
            },
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF5F2), Color(0xFFF7EDE7)),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "开启相机后就能直接拍",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TryOnText,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TryOnAccent,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("开启相机")
                }
            }
        }
    }
}

@Composable
private fun CaptureOverlay(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameWidth = size.width * 0.72f
            val frameHeight = size.height * 0.52f
            val left = (size.width - frameWidth) / 2f
            val top = size.height * 0.15f
            val radius = 46f

            drawRoundRect(
                color = Color.White.copy(alpha = 0.84f),
                topLeft = Offset(left, top),
                size = Size(frameWidth, frameHeight),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 18f)),
                    cap = StrokeCap.Round,
                ),
            )

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.08f),
                topLeft = Offset(left + 18f, top + 18f),
                size = Size(frameWidth - 36f, frameHeight - 36f),
                cornerRadius = CornerRadius(radius * 0.8f, radius * 0.8f),
                style = Stroke(width = 2f),
            )
        }

        Surface(
            color = Color.Black.copy(alpha = 0.22f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = "手背朝上，五指自然分开，尽量放满虚线框",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                ),
                textAlign = TextAlign.Center,
            )
        }

        HandPoseHint(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-6).dp)
                .size(width = 146.dp, height = 188.dp),
        )

        Surface(
            color = Color.White.copy(alpha = 0.9f),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Text(
                text = "拍完直接生成，不再二次确认",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TryOnMuted,
                    fontWeight = FontWeight.Medium,
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HandPoseHint(
    modifier: Modifier = Modifier,
) {
    val hintBitmap = remember { createHandPoseHintBitmap() }
    Image(
        bitmap = hintBitmap.asImageBitmap(),
        contentDescription = "hand pose hint",
        modifier = modifier,
        alpha = 0.88f,
    )
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

private fun bindTryOnCameraPreview(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    analyzerExecutor: ExecutorService,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    onFrameCaptured: (Bitmap) -> Unit,
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
    cameraProviderFuture.addListener(
        {
            val cameraProvider = cameraProviderFuture.get()
            onCameraProviderReady(cameraProvider)

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            var lastFrameAt = 0L
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                        val now = System.currentTimeMillis()
                        if (now - lastFrameAt >= 700) {
                            imageProxy.toBitmapCompat()?.let(onFrameCaptured)
                            lastFrameAt = now
                        }
                        imageProxy.close()
                    }
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
        },
        ContextCompat.getMainExecutor(previewView.context),
    )
}

private fun createHandPoseHintBitmap(): Bitmap {
    val width = 420
    val height = 540
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.TRANSPARENT)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(185, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 9f
        strokeCap = Paint.Cap.ROUND
        pathEffect = DashPathEffect(floatArrayOf(20f, 12f), 0f)
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(30, 255, 255, 255)
        style = Paint.Style.FILL
    }

    val palmPath = Path().apply {
        moveTo(124f, 360f)
        cubicTo(92f, 320f, 98f, 250f, 142f, 214f)
        cubicTo(180f, 182f, 246f, 174f, 286f, 210f)
        cubicTo(328f, 248f, 332f, 328f, 294f, 382f)
        cubicTo(270f, 418f, 230f, 442f, 192f, 446f)
        cubicTo(162f, 450f, 138f, 422f, 124f, 360f)
        close()
    }
    canvas.drawPath(palmPath, fillPaint)
    canvas.drawPath(palmPath, strokePaint)

    fun drawFinger(x: Float, top: Float, right: Float, bottom: Float, radius: Float = 28f) {
        canvas.drawRoundRect(x, top, right, bottom, radius, radius, fillPaint)
        canvas.drawRoundRect(x, top, right, bottom, radius, radius, strokePaint)
    }

    drawFinger(110f, 98f, 156f, 276f)
    drawFinger(164f, 64f, 214f, 294f)
    drawFinger(224f, 84f, 274f, 304f)
    drawFinger(280f, 128f, 324f, 292f)

    val thumbPath = Path().apply {
        moveTo(118f, 296f)
        cubicTo(84f, 282f, 62f, 296f, 54f, 328f)
        cubicTo(48f, 354f, 64f, 378f, 92f, 388f)
        cubicTo(120f, 398f, 144f, 384f, 154f, 356f)
    }
    canvas.drawPath(thumbPath, strokePaint)

    return bitmap
}

private fun ImageProxy.toBitmapCompat(): Bitmap? {
    val nv21 = yuv420888ToNv21(this)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val output = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, output)
    val bytes = output.toByteArray()
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val rotationDegrees = imageInfo.rotationDegrees
    if (rotationDegrees == 0) return bitmap

    val matrix = Matrix().apply {
        postRotate(rotationDegrees.toFloat())
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)

    val chromaRowStride = image.planes[1].rowStride
    val chromaPixelStride = image.planes[1].pixelStride
    val width = image.width
    val height = image.height
    var outputOffset = ySize

    val uBytes = ByteArray(uSize)
    val vBytes = ByteArray(vSize)
    uBuffer.get(uBytes)
    vBuffer.get(vBytes)

    for (row in 0 until height / 2) {
        for (col in 0 until width / 2) {
            val index = row * chromaRowStride + col * chromaPixelStride
            nv21[outputOffset++] = vBytes[index]
            nv21[outputOffset++] = uBytes[index]
        }
    }

    return nv21
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
