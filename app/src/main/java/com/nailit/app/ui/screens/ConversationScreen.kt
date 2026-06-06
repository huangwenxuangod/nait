package com.nailit.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.nailit.app.core.model.ExecutionStep
import com.nailit.app.core.preview.HandPhotoRuntime
import com.nailit.app.core.preview.LiveGuideMode
import com.nailit.app.core.preview.LiveGuideWorkflowEngine
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.PcmAudioPlayer
import com.nailit.app.core.preview.PcmAudioRecorder
import com.nailit.app.core.preview.QwenRealtimeSessionManager
import com.nailit.app.core.preview.QwenRealtimeStatus
import com.nailit.app.core.preview.SupabaseFunctionRepository
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private val GuideBg = Color(0xFFF6F1EB)
private val GuideCard = Color(0xFFFFFCF7)
private val GuideBorder = Color(0xFFE7DACD)
private val GuideText = Color(0xFF181311)
private val GuideMuted = Color(0xFF7C7268)
private val GuideAccent = Color(0xFF2A1D1A)
private val GuideSoft = Color(0xFFEDE0D3)
private val GuideSuccess = Color(0xFF5A6F4C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    sessionSnapshot: NailSessionSnapshot?,
    onBack: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenTryOn: () -> Unit,
    onOpenSop: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeSession = NailSessionRuntime.current ?: sessionSnapshot
    val steps = activeSession?.executionSteps.orEmpty().ifEmpty { demoSteps() }
    val repository = remember { SupabaseFunctionRepository() }
    val workflow = remember(steps) { LiveGuideWorkflowEngine(steps) }
    var uiState = remember(steps) { workflow.initialState() }
    val realtimeManager = remember { QwenRealtimeSessionManager() }
    val audioPlayer = remember { PcmAudioPlayer() }
    var isRecording by remember { mutableStateOf(false) }
    var permissionHint by remember { mutableStateOf<String?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val pendingImageFrame = remember { AtomicReference<String?>(null) }
    val hasSentImageThisTurn = remember { AtomicBoolean(false) }
    val recorder = remember {
        PcmAudioRecorder(context) { chunk ->
            realtimeManager.appendAudio(chunk)
            val pendingFrame = pendingImageFrame.get()
            if (!pendingFrame.isNullOrBlank() && hasSentImageThisTurn.compareAndSet(false, true)) {
                realtimeManager.appendImage(pendingFrame)
                pendingImageFrame.set(null)
            }
        }
    }
    val realtimeState by realtimeManager.state.collectAsState()
    var liveFrameBitmap by remember { mutableStateOf<Bitmap?>(HandPhotoRuntime.currentBitmap) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionHint = if (granted) null else "请先允许麦克风权限，才能进入实时带做。"
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionHint = if (granted) null else "请先允许相机权限，才能进入视频带做。"
    }

    LaunchedEffect(Unit) {
        runCatching { repository.createRealtimeToken() }
            .onSuccess { tokenPayload ->
                realtimeManager.connect(tokenPayload)
            }
    }

    LaunchedEffect(realtimeState.lastAudioDeltaBase64) {
        if (realtimeState.lastAudioDeltaBase64.isNotBlank()) {
            val bytes = runCatching {
                Base64.decode(realtimeState.lastAudioDeltaBase64, Base64.DEFAULT)
            }.getOrNull()
            if (bytes != null) {
                audioPlayer.write(bytes)
            }
        }
    }

    LaunchedEffect(realtimeState.status) {
        if (realtimeState.status == QwenRealtimeStatus.Connected) {
            uiState = workflow.onConnected(uiState)
        }
    }

    LaunchedEffect(realtimeState.lastTranscript) {
        if (realtimeState.lastTranscript.isNotBlank()) {
            uiState = uiState.copy(coachLine = realtimeState.lastTranscript)
        }
    }

    LaunchedEffect(realtimeState.responseDoneCount) {
        if (realtimeState.responseDoneCount > 0 && !isRecording) {
            uiState = uiState.copy(mode = LiveGuideMode.Waiting)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder.stop()
            audioPlayer.stop()
            realtimeManager.disconnect()
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    val currentStep = steps.getOrNull(uiState.stepIndex)
    val handBitmap = liveFrameBitmap ?: HandPhotoRuntime.currentBitmap

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "视频带做",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = GuideText,
                                fontWeight = FontWeight.Bold,
                            )
                        )
                        Text(
                            text = "只做当前一步，做完再继续",
                            style = MaterialTheme.typography.bodySmall.copy(color = GuideMuted),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GuideText,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GuideBg),
            )
        },
        containerColor = GuideBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LiveGuideHeader(
                stepIndex = uiState.stepIndex,
                totalSteps = steps.size,
                title = currentStep?.title ?: "等待流程包",
                remainingMinutes = remainingMinutes(
                    steps = steps,
                    currentStepIndex = uiState.stepIndex,
                    fallback = activeSession?.estimatedTotalMinutes ?: 0,
                ),
            )

            CameraStageCard(
                context = context,
                lifecycleOwner = lifecycleOwner,
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED,
                onRequestPermission = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                analyzerExecutor = cameraExecutor,
                onCameraProviderReady = { provider ->
                    cameraProvider = provider
                },
                onFrameCaptured = { bitmap ->
                    liveFrameBitmap = bitmap
                    HandPhotoRuntime.currentBitmap = bitmap
                },
                handBitmap = handBitmap,
                targetBitmap = null,
                status = cameraStatusLabel(uiState.mode, realtimeState.status),
            )

            currentStep?.let { step ->
                StepGuideCard(
                    step = step,
                    coachLine = permissionHint ?: uiState.coachLine,
                    mode = uiState.mode,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        uiState = workflow.onPause(uiState)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = GuideAccent,
                    ),
                ) {
                    Icon(
                        imageVector = if (uiState.mode == LiveGuideMode.Paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.mode == LiveGuideMode.Paused) "继续" else "暂停")
                }

                Button(
                    onClick = {
                        if (!isRecording) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@Button
                            }
                            uiState = workflow.onCheck(uiState)
                            realtimeManager.resetTranscript()
                            hasSentImageThisTurn.set(false)
                            pendingImageFrame.set(handBitmap?.toBase64Jpeg())
                            recorder.start()
                            isRecording = true
                        } else {
                            recorder.stop()
                            realtimeManager.commitTurn()
                            realtimeManager.requestResponse("请结合当前对话，输出一句简洁的中文语音指导。")
                            isRecording = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GuideAccent,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRecording) "发送语音" else "我好了")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        uiState = workflow.onRepeat(uiState)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GuideSoft,
                        contentColor = GuideAccent,
                    ),
                ) {
                    Text("再讲一遍")
                }

                Button(
                    onClick = {
                        uiState = workflow.onNext(uiState)
                        val nextStep = workflow.currentStep(uiState.stepIndex)
                        NailSessionRuntime.current = activeSession?.copy(
                            currentStepIndex = uiState.stepIndex,
                            currentStepTitle = nextStep?.title,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GuideSuccess,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("下一步")
                }
            }
        }
    }
}

private fun cameraStatusLabel(
    mode: LiveGuideMode,
    realtimeStatus: QwenRealtimeStatus,
): String {
    return when {
        realtimeStatus == QwenRealtimeStatus.Connecting -> "连接中"
        realtimeStatus == QwenRealtimeStatus.Error -> "连接异常"
        mode == LiveGuideMode.Guiding -> "当前引导"
        mode == LiveGuideMode.Waiting -> "等待你操作"
        mode == LiveGuideMode.Checking -> "正在复核"
        mode == LiveGuideMode.Paused -> "已暂停"
        else -> "准备中"
    }
}

@Composable
private fun LiveGuideHeader(
    stepIndex: Int,
    totalSteps: Int,
    title: String,
    remainingMinutes: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeaderPill(
            label = "步骤",
            value = "${stepIndex + 1}/$totalSteps",
            modifier = Modifier.weight(0.9f),
        )
        HeaderPill(
            label = "当前",
            value = title,
            modifier = Modifier.weight(1.7f),
        )
        HeaderPill(
            label = "剩余",
            value = "${remainingMinutes}m",
            modifier = Modifier.weight(0.8f),
        )
    }
}

@Composable
private fun HeaderPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GuideCard)
            .border(1.dp, GuideBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = GuideMuted,
                letterSpacing = 1.sp,
            ),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = GuideText,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun CameraStageCard(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    analyzerExecutor: ExecutorService,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    onFrameCaptured: (Bitmap) -> Unit,
    handBitmap: Bitmap?,
    targetBitmap: Bitmap?,
    status: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(32.dp))
            .background(GuideAccent),
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = {
                    PreviewView(context).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        bindCameraPreview(
                            previewView = this,
                            lifecycleOwner = lifecycleOwner,
                            analyzerExecutor = analyzerExecutor,
                            onCameraProviderReady = onCameraProviderReady,
                            onFrameCaptured = onFrameCaptured,
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else if (handBitmap != null) {
            Image(
                bitmap = handBitmap.asImageBitmap(),
                contentDescription = "Live hand preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF322522)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "这里会放实时镜头",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.84f)),
                    )
                    Button(
                        onClick = onRequestPermission,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = GuideAccent,
                        ),
                    ) {
                        Text("开启相机")
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
                .size(width = 110.dp, height = 146.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(GuideCard)
                .border(1.dp, GuideBorder, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (targetBitmap != null) {
                Image(
                    bitmap = targetBitmap.asImageBitmap(),
                    contentDescription = "Target try-on preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = "目标图",
                    style = MaterialTheme.typography.bodyMedium.copy(color = GuideMuted),
                )
            }
        }
    }
}

@Composable
private fun StepGuideCard(
    step: ExecutionStep,
    coachLine: String,
    mode: LiveGuideMode,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(GuideCard)
            .border(1.dp, GuideBorder, RoundedCornerShape(30.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = GuideText,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = GuideMuted,
                        lineHeight = 22.sp,
                    ),
                )
            }

            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(GuideSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (step.needs_timer) "${step.duration_sec}s" else "观察",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = GuideAccent,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GuideBg)
                .padding(16.dp),
        ) {
            Text(
                text = coachLineForMode(mode, coachLine, step),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = GuideText,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetaChip(if (step.needs_visual_check) "需要看镜头" else "无需看镜头")
            MetaChip(if (step.needs_timer) "带计时" else "无计时")
            if (step.voiceShortcut.isNotBlank()) {
                MetaChip("口令：${step.voiceShortcut}")
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GuideSoft)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = GuideAccent,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

private fun coachLineForMode(
    mode: LiveGuideMode,
    coachLine: String,
    step: ExecutionStep,
): String {
    return when (mode) {
        LiveGuideMode.Connecting -> "我先接管这一轮流程。${step.instruction}"
        LiveGuideMode.Guiding -> coachLine
        LiveGuideMode.Waiting -> "先做这一层。做完直接说“我好了”。"
        LiveGuideMode.Checking -> "我正在按你当前画面复核这一步，重点看：${step.voiceGoal.ifBlank { "边缘是否干净、厚度是否均匀" }}。"
        LiveGuideMode.Paused -> "先停在这里，别急着继续。"
    }
}

private fun remainingMinutes(
    steps: List<ExecutionStep>,
    currentStepIndex: Int,
    fallback: Int,
): Int {
    val remaining = steps.drop(currentStepIndex).sumOf { it.duration_sec } / 60
    return if (remaining > 0) remaining else fallback
}

private fun demoSteps(): List<ExecutionStep> {
    return listOf(
        ExecutionStep(
            id = "prep_clean",
            title = "清洁甲面",
            instruction = "先擦净油脂，保持甲面干爽。",
            duration_sec = 45,
            needs_timer = false,
            needs_visual_check = true,
            voiceGoal = "确认甲面已经清洁干净",
            voiceShortcut = "我好了",
        ),
        ExecutionStep(
            id = "base_apply",
            title = "薄涂底胶",
            instruction = "薄薄一层，边缘不要碰皮。",
            duration_sec = 60,
            needs_timer = true,
            needs_visual_check = true,
            voiceGoal = "确认底胶没有堆厚",
            voiceShortcut = "看一下",
        ),
        ExecutionStep(
            id = "cat_eye",
            title = "做猫眼高光",
            instruction = "磁铁斜吸两秒，看到一条柔光就停。",
            duration_sec = 75,
            needs_timer = true,
            needs_visual_check = true,
            voiceGoal = "确认高光已经形成",
            voiceShortcut = "下一步",
        ),
    )
}

private fun Bitmap.toBase64Jpeg(): String {
    val output = java.io.ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 82, output)
    return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}

private fun bindCameraPreview(
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
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
                        if (now - lastFrameAt >= 900) {
                            imageProxy.toBitmap()?.let(onFrameCaptured)
                            lastFrameAt = now
                        }
                        imageProxy.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                analysis,
            )
        },
        ContextCompat.getMainExecutor(previewView.context),
    )
}

private fun ImageProxy.toBitmap(): Bitmap? {
    val nv21 = yuv420888ToNv21(this)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val output = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, output)
    val bytes = output.toByteArray()
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
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
