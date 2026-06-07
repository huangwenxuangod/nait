package com.nailit.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.nailit.app.core.model.ExecutionStep
import com.nailit.app.core.network.SupabaseManager
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
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val GuideBg = Color(0xFFF6F1EB)
private val GuideText = Color(0xFF181311)
private val GuideAccent = Color(0xFF2A1D1A)

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
    var targetBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
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
        permissionHint = if (granted) null else "请先允许麦克风权限。"
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionHint = if (granted) null else "请先允许相机权限。"
    }

    LaunchedEffect(activeSession?.targetImagePath) {
        if (activeSession?.targetImagePath != null) {
            targetBitmap = loadTargetBitmap(activeSession.targetImagePath)
        }
    }

    LaunchedEffect(activeSession?.sessionId) {
        val session = NailSessionRuntime.current ?: sessionSnapshot ?: return@LaunchedEffect
        if (session.executionSteps.isNotEmpty() || session.executionStatus == "guide_pending") return@LaunchedEffect

        NailSessionRuntime.current = session.copy(
            executionStatus = "guide_pending",
            executionError = null,
        )

        runCatching {
            repository.generateExecutionPackage(session.sessionId)
            repository.fetchExecutionPackage(session.sessionId)
        }.onSuccess { executionPackage ->
            NailSessionRuntime.current = (NailSessionRuntime.current ?: session).copy(
                executionStatus = "guide_ready",
                estimatedTotalMinutes = executionPackage?.estimated_total_minutes ?: session.estimatedTotalMinutes,
                currentStepIndex = 0,
                currentStepTitle = executionPackage?.steps?.firstOrNull()?.title,
                executionSteps = executionPackage?.steps ?: session.executionSteps,
                executionError = null,
            )
        }.onFailure { error ->
            NailSessionRuntime.current = (NailSessionRuntime.current ?: session).copy(
                executionStatus = "guide_failed",
                executionError = error.message,
            )
        }
    }

    LaunchedEffect(Unit) {
        runCatching { repository.createRealtimeToken() }
            .onSuccess { tokenPayload ->
                realtimeManager.connect(tokenPayload)
            }
            .onFailure {
                permissionHint = it.message
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

    LaunchedEffect(isRecording, liveFrameBitmap) {
        while (isRecording) {
            (liveFrameBitmap ?: HandPhotoRuntime.currentBitmap)?.toBase64Jpeg()?.let { base64 ->
                pendingImageFrame.set(base64)
            }
            delay(1000)
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
    var isTimerRunning by remember(currentStep?.id) { mutableStateOf(false) }
    var timerSeconds by remember(currentStep?.id) {
        mutableStateOf(currentStep?.duration_sec ?: 0)
    }

    LaunchedEffect(currentStep?.id) {
        isTimerRunning = false
        timerSeconds = currentStep?.duration_sec ?: 0
    }

    LaunchedEffect(isTimerRunning, currentStep?.id, timerSeconds) {
        if (!isTimerRunning) return@LaunchedEffect
        while (isActive && isTimerRunning && timerSeconds > 0) {
            delay(1000)
            timerSeconds -= 1
        }
        if (timerSeconds <= 0) {
            isTimerRunning = false
        }
    }

    val handBitmap = liveFrameBitmap ?: HandPhotoRuntime.currentBitmap

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "跟做中",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = GuideText,
                            fontWeight = FontWeight.Bold,
                        )
                    )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(GuideAccent),
        ) {
            LiveCameraStage(
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
                lensFacing = lensFacing,
            )

            StatusPill(
                status = cameraStatusLabel(uiState.mode, realtimeState.status),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            )

            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.26f))
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "切换摄像头",
                    tint = Color.White,
                )
            }

            if (targetBitmap != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 74.dp, end = 16.dp)
                        .size(width = 92.dp, height = 124.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                ) {
                    Image(
                        bitmap = targetBitmap!!.asImageBitmap(),
                        contentDescription = "Target try-on preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else if (activeSession?.tryOnStatus == "try_on_pending") {
                SmallTryOnStateCard(
                    text = "AI试戴中...",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 74.dp, end = 16.dp),
                    loading = true,
                )
            } else if (activeSession?.tryOnStatus == "failed") {
                SmallTryOnStateCard(
                    text = "试戴未出图",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 74.dp, end = 16.dp),
                    loading = false,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.34f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = currentStep?.title ?: "准备开始",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                )
                Text(
                    text = bottomCoachLine(
                        permissionHint = permissionHint,
                        coachLine = uiState.coachLine,
                        mode = uiState.mode,
                        currentStep = currentStep,
                        realtimeError = realtimeState.errorMessage,
                        degradedToOffline = realtimeState.degradedToOffline,
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(alpha = 0.92f),
                        lineHeight = 24.sp,
                    )
                )

                if (activeSession?.executionStatus == "guide_pending") {
                    Text(
                        text = "步骤正在后台整理，先准备当前这一步。",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    )
                }

                if ((currentStep?.needs_timer == true) || (currentStep?.duration_sec ?: 0) >= 30) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "照灯倒计时",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            )
                            Text(
                                text = if (isTimerRunning) "剩余 ${timerSeconds}s" else "需要时再开始",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.72f),
                                )
                            )
                        }
                        Button(
                            onClick = {
                                if (isTimerRunning) {
                                    isTimerRunning = false
                                } else {
                                    timerSeconds = currentStep?.duration_sec ?: 60
                                    isTimerRunning = true
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = GuideAccent,
                            ),
                        ) {
                            Text(if (isTimerRunning) "暂停" else "开始")
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            uiState = workflow.onPause(uiState)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = if (uiState.mode == LiveGuideMode.Paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                        )
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
                                realtimeManager.requestResponse("请只输出一句中文语音指导，继续带用户做下一步。")
                                isRecording = false
                                if (uiState.stepIndex < steps.lastIndex) {
                                    uiState = workflow.onNext(uiState)
                                    val nextStep = workflow.currentStep(uiState.stepIndex)
                                    NailSessionRuntime.current = activeSession?.copy(
                                        currentStepIndex = uiState.stepIndex,
                                        currentStepTitle = nextStep?.title,
                                    )
                                } else {
                                    onOpenSop()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = GuideAccent,
                        ),
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCameraStage(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    analyzerExecutor: ExecutorService,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    onFrameCaptured: (Bitmap) -> Unit,
    handBitmap: Bitmap?,
    lensFacing: Int,
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
                        lensFacing = lensFacing,
                    )
                }
            },
            update = {
                bindCameraPreview(
                    previewView = it,
                    lifecycleOwner = lifecycleOwner,
                    analyzerExecutor = analyzerExecutor,
                    onCameraProviderReady = onCameraProviderReady,
                    onFrameCaptured = onFrameCaptured,
                    lensFacing = lensFacing,
                )
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

@Composable
private fun SmallTryOnStateCard(
    text: String,
    modifier: Modifier = Modifier,
    loading: Boolean,
) {
    Box(
        modifier = modifier
            .size(width = 92.dp, height = 124.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.58f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatusPill(
    status: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
}

private fun bottomCoachLine(
    permissionHint: String?,
    coachLine: String,
    mode: LiveGuideMode,
    currentStep: ExecutionStep?,
    realtimeError: String,
    degradedToOffline: Boolean,
): String {
    if (!permissionHint.isNullOrBlank()) return permissionHint
    if (degradedToOffline) {
        return currentStep?.instruction ?: "实时连接暂时不可用，已切换为离线带做模式。"
    }
    if (realtimeError.isNotBlank()) return realtimeError
    if (coachLine.isNotBlank()) return coachLine
    return when (mode) {
        LiveGuideMode.Connecting -> "我先接管这一轮流程。"
        LiveGuideMode.Guiding -> currentStep?.instruction ?: "跟着我做当前这一步。"
        LiveGuideMode.Waiting -> "我在持续看着，你可以直接说话。"
        LiveGuideMode.Checking -> "我正在看你当前这一步，稍等一下。"
        LiveGuideMode.Paused -> "先停在这里，准备好了再继续。"
    }
}

private fun cameraStatusLabel(
    mode: LiveGuideMode,
    realtimeStatus: QwenRealtimeStatus,
): String {
    return when {
        realtimeStatus == QwenRealtimeStatus.Connecting -> "连接中"
        realtimeStatus == QwenRealtimeStatus.Error -> "离线带做"
        mode == LiveGuideMode.Guiding -> "当前引导"
        mode == LiveGuideMode.Waiting -> "实时对话"
        mode == LiveGuideMode.Checking -> "正在复核"
        mode == LiveGuideMode.Paused -> "已暂停"
        else -> "准备中"
    }
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
    val maxSide = 960
    val scaled = if (width > maxSide || height > maxSide) {
        val scale = minOf(maxSide.toFloat() / width.toFloat(), maxSide.toFloat() / height.toFloat())
        Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        this
    }
    val output = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 68, output)
    if (scaled !== this) {
        scaled.recycle()
    }
    return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}

private suspend fun loadTargetBitmap(storagePath: String): Bitmap? {
    return runCatching {
        val bytes = SupabaseManager.client.storage
            .from("nail-it-assets")
            .downloadAuthenticated(storagePath)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun bindCameraPreview(
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    analyzerExecutor: ExecutorService,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    onFrameCaptured: (Bitmap) -> Unit,
    lensFacing: Int,
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
                .also { imageAnalysis: ImageAnalysis ->
                    imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                        val now = System.currentTimeMillis()
                        if (now - lastFrameAt >= 900) {
                            imageProxy.toBitmapCompat()?.let(onFrameCaptured)
                            lastFrameAt = now
                        }
                        imageProxy.close()
                    }
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                preview,
                analysis,
            )
        },
        ContextCompat.getMainExecutor(previewView.context),
    )
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
