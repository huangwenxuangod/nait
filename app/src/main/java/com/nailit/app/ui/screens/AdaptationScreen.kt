package com.nailit.app.ui.screens

import android.Manifest
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import com.nailit.app.core.preview.HandPhotoRuntime
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.SupabaseFunctionRepository
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

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
    var isPreparingGuide by remember { mutableStateOf(false) }
    var statusText by remember {
        mutableStateOf(
            if (handBitmap == null) "先拍一张手图。" else "开始生成试戴结果。"
        )
    }

    androidx.compose.runtime.LaunchedEffect(activeSession?.status) {
        when (activeSession?.status) {
            "booting" -> statusText = "正在后台同步这次试戴。"
            "source_parsing" -> statusText = "正在后台解析当前款式。"
            "bootstrap_failed" -> statusText = "初始化失败，但你仍然可以先拍手图。"
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                handBitmap = bitmap
                HandPhotoRuntime.currentBitmap = bitmap
                remoteTryOnBitmap = null
                statusText = "手图已更新。"
            } else {
                statusText = "图片读取失败，请重新选择。"
            }
        }
    }

    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            handBitmap = bitmap
            HandPhotoRuntime.currentBitmap = bitmap
            remoteTryOnBitmap = null
            statusText = "手图已更新。"
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
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    fun retryCapture() {
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

    fun prepareGuide() {
        val session = NailSessionRuntime.current ?: activeSession
        if (session == null) {
            statusText = "没有活跃会话，请返回首页重新开始。"
            return
        }

        scope.launch {
            isPreparingGuide = true
            statusText = "正在进入视频带做..."
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
                )
                statusText = "流程已就绪。"
                onContinue()
            }.onFailure { error ->
                statusText = "流程生成失败：${error.message ?: "未知错误"}"
            }
            isPreparingGuide = false
        }
    }

    fun startAsyncTryOnAndProceed() {
        val session = activeSession
        val bitmap = handBitmap
        if (session == null) {
            statusText = "没有活跃会话，请返回首页重新开始。"
            return
        }
        if (bitmap == null) {
            statusText = "请先拍摄或上传手图。"
            return
        }

        // 1. Launch Try-On in the background (survives navigation)
        NailSessionRuntime.backgroundScope.launch {
            runCatching {
                NailSessionRuntime.current = NailSessionRuntime.current?.copy(
                    tryOnStatus = "try_on_pending"
                ) ?: session.copy(tryOnStatus = "try_on_pending")

                val upload = repository.prepareAssetUpload(
                    sessionId = session.sessionId,
                    assetType = "hand_photo",
                    mimeType = "image/jpeg",
                )
                val uploadPath = upload.storage_path ?: error("prepareAssetUpload missing storage_path")
                val uploadAssetId = upload.asset_id ?: error("prepareAssetUpload missing asset_id")
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
                val tryOn = repository.createTryOn(session.sessionId)
                repository.fetchTryOnResult(session.sessionId)
                val tryOnPath = repository.fetchTryOnImagePath(session.sessionId)

                NailSessionRuntime.current = NailSessionRuntime.current?.copy(
                    status = tryOn.status ?: "try_on_ready",
                    handAssetId = uploadAssetId,
                    handStoragePath = uploadPath,
                    handPhotoUrl = publicUrl,
                    tryOnStatus = tryOn.status ?: "try_on_ready",
                    targetImagePath = tryOnPath,
                ) ?: session.copy(
                    status = tryOn.status ?: "try_on_ready",
                    handAssetId = uploadAssetId,
                    handStoragePath = uploadPath,
                    handPhotoUrl = publicUrl,
                    tryOnStatus = tryOn.status ?: "try_on_ready",
                    targetImagePath = tryOnPath,
                )
            }.onFailure { error ->
                NailSessionRuntime.current = NailSessionRuntime.current?.copy(
                    tryOnStatus = "failed"
                )
            }
        }

        // 2. Simultaneously start preparing the guide in the screen scope
        prepareGuide()
    }

    fun startDirectSop() {
        prepareGuide()
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MinimalImagePanel(
                    title = "原图",
                    bitmap = handBitmap,
                    emptyText = "还没有手图",
                    modifier = Modifier.weight(1f),
                )
                MinimalImagePanel(
                    title = "试戴结果",
                    bitmap = remoteTryOnBitmap,
                    emptyText = if (isRendering) "正在生成..." else "点下方开始试戴",
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TryOnMuted,
                    lineHeight = 18.sp,
                )
            )

            if (handBitmap == null) {
                Button(
                    onClick = { retryCapture() },
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
                        text = "拍张手图开始",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        )
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { startAsyncTryOnAndProceed() },
                        enabled = !isPreparingGuide,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TryOnAccent,
                            contentColor = Color.White,
                        )
                    ) {
                        if (isPreparingGuide) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            Text(
                                text = "AI 试戴并进入 (推荐)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { startDirectSop() },
                            enabled = !isPreparingGuide,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .border(1.dp, TryOnBorder, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = TryOnAccent,
                            )
                        ) {
                            Text(
                                text = "直达实操 (跳过试戴)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { retryCapture() },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEFE6DF),
                                contentColor = TryOnAccent,
                            )
                        ) {
                            Text(
                                text = "重拍手图",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalImagePanel(
    title: String,
    bitmap: Bitmap?,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                color = TryOnMuted,
                fontWeight = FontWeight.SemiBold,
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(TryOnCard)
                .border(1.dp, TryOnBorder, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TryOnMuted,
                    )
                )
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

private fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
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
