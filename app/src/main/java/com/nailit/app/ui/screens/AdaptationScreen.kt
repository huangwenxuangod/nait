package com.nailit.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nailit.app.core.network.SupabaseManager
import com.nailit.app.core.preview.HandPhotoRuntime
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.core.preview.NailSessionSnapshot
import com.nailit.app.core.preview.SupabaseFunctionRepository
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

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
    var handBitmap by remember { mutableStateOf(HandPhotoRuntime.currentBitmap) }
    var hasHandPhoto by remember { mutableStateOf(handBitmap != null) }
    var showTryOnResult by remember { mutableStateOf(false) }
    var isGeneratingTryOn by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    val title = when (selectedId) {
        "aurora-cat" -> "极光冰透猫眼"
        "french-soft" -> "法式温柔渐变"
        else -> "复古红酒排钻"
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                handBitmap = bitmap
                HandPhotoRuntime.currentBitmap = bitmap
                hasHandPhoto = true
                statusText = "已选择真实手部照片，可以上传试戴。"
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
            hasHandPhoto = true
            statusText = "真实拍照已完成，可以上传试戴。"
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
            statusText = "没有相机权限，已切换为相册导入。"
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showTryOnResult) "AI 美甲试戴效果" else "手部条件采集",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showTryOnResult) {
                            showTryOnResult = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (!showTryOnResult) {
                    // Hand Capture Mode
                    Text(
                        "扫描您的手部照片",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )

                    // Hand Camera Box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clickable {
                                if (!hasHandPhoto) {
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
                            },
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE9D8D0))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasHandPhoto && handBitmap != null) {
                                // Draw Hand Skeleton & Nail boxes
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        bitmap = handBitmap!!.asImageBitmap(),
                                        contentDescription = "Captured hand photo",
                                    )

                                    // Skeleton Canvas
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val color = Color(0xFFC5A880)
                                        val strokeWidth = 1.5.dp.toPx()

                                        // Draw 5 nail landmark rectangles
                                        val nails = listOf(
                                            Offset(size.width * 0.2f, size.height * 0.4f), // Thumb
                                            Offset(size.width * 0.35f, size.height * 0.25f), // Index
                                            Offset(size.width * 0.5f, size.height * 0.2f), // Middle
                                            Offset(size.width * 0.65f, size.height * 0.28f), // Ring
                                            Offset(size.width * 0.8f, size.height * 0.42f) // Pinky
                                        )

                                        nails.forEach { nail ->
                                            drawRect(
                                                color = color,
                                                topLeft = Offset(nail.x - 12.dp.toPx(), nail.y - 18.dp.toPx()),
                                                size = Size(24.dp.toPx(), 36.dp.toPx()),
                                                style = Stroke(width = strokeWidth)
                                            )
                                            drawCircle(
                                                color = Color(0xFF881337),
                                                radius = 3.dp.toPx(),
                                                center = nail
                                            )
                                        }

                                        // Draw skeleton lines connecting joints
                                        val joints = listOf(
                                            Offset(size.width * 0.5f, size.height * 0.8f), // Wrist
                                            Offset(size.width * 0.5f, size.height * 0.5f) // Palm center
                                        )
                                        drawLine(
                                            color = color.copy(alpha = 0.5f),
                                            start = joints[0],
                                            end = joints[1],
                                            strokeWidth = strokeWidth
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Camera",
                                        tint = Color(0xFFC5A880),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        "点击真实拍照",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        "请将手背平放于镜头前。拒绝权限时会自动转相册导入。",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (hasHandPhoto) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF8F5)),
                            border = BorderStroke(1.dp, Color(0xFFE9D8D0).copy(alpha = 0.8f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "手部条件分析报告",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC5A880),
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    "• 肤色诊断：暖黄皮 2.5 度 (适合暖调猫眼)\n" +
                                    "• 甲床诊断：偏短偏宽甲床 (建议椭圆甲型修饰)\n" +
                                    "• 手部骨骼：骨节匀称，适合透亮光感美甲",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }
                    }

                    statusText?.let { text ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                            border = BorderStroke(1.dp, Color(0xFFF5CBA7))
                        ) {
                            Text(
                                text,
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF9A3412),
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }

                    if (isGeneratingTryOn) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF881337),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                "正在融合款式与光影...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF881337),
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }

                } else {
                    // Try-On Result Mode
                    Text(
                        "AI 试戴效果预览",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )

                    // Try On Rendering Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f),
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE9D8D0))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Simulated rendering background
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFFAF8F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "AI 极光猫眼光影融合图",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray
                                    )
                                )
                            }

                            // Rendered nails on canvas
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val nails = listOf(
                                    Offset(size.width * 0.2f, size.height * 0.4f),
                                    Offset(size.width * 0.35f, size.height * 0.25f),
                                    Offset(size.width * 0.5f, size.height * 0.2f),
                                    Offset(size.width * 0.65f, size.height * 0.28f),
                                    Offset(size.width * 0.8f, size.height * 0.42f)
                                )

                                nails.forEach { nail ->
                                    // Draw beautifully rendered styled nail (Solid pink with gold glitter glow)
                                    drawRoundRect(
                                        color = Color(0xFFFDA4AF), // Ice pink base
                                        topLeft = Offset(nail.x - 12.dp.toPx(), nail.y - 18.dp.toPx()),
                                        size = Size(24.dp.toPx(), 36.dp.toPx()),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                    )
                                    // Draw cat eye magnetic light line (Diagonal gold glow line)
                                    drawLine(
                                        color = Color(0xFFFEF08A).copy(alpha = 0.8f),
                                        start = Offset(nail.x - 10.dp.toPx(), nail.y + 10.dp.toPx()),
                                        end = Offset(nail.x + 10.dp.toPx(), nail.y - 10.dp.toPx()),
                                        strokeWidth = 4.dp.toPx()
                                    )
                                }
                            }
                        }
                    }

                    // Matching Analysis Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE9D8D0).copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "款式与肤色适配度",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF881337), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "显白 1.5 度",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "原视频款式",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        "方型甲床，高饱和冰透粉背景，偏冷光泽",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1.2f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "AI 个性化定制版",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF881337),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        "修剪为椭圆甲型拉长视觉。调暖底色 +15% 以完美衬托您的暖黄皮。",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Button
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                onClick = {
                    if (showTryOnResult) {
                        scope.launch {
                            val active = NailSessionRuntime.current ?: sessionSnapshot
                            if (active == null) {
                                statusText = "未找到当前 session，暂时无法生成 BOM。"
                                return@launch
                            }

                            statusText = "正在生成执行包与 BOM 清单..."
                            runCatching {
                                val result = repository.generateExecutionPackage(active.sessionId)
                                NailSessionRuntime.current = active.copy(
                                    status = result.status,
                                    executionStatus = result.status,
                                )
                            }.onSuccess {
                                statusText = "执行包已生成，进入材料清单。"
                                onContinue()
                            }.onFailure { error ->
                                statusText = "执行包生成失败：${error.message ?: "未知错误"}"
                            }
                        }
                    } else {
                        scope.launch {
                            val active = NailSessionRuntime.current ?: sessionSnapshot
                            if (active == null) {
                                statusText = "未找到当前 session，请先从首页重新开始。"
                                return@launch
                            }
                            if (!hasHandPhoto) {
                                statusText = "请先拍摄或模拟捕获手部照片。"
                                return@launch
                            }

                            isGeneratingTryOn = true
                            statusText = "正在准备 hand photo 上传..."

                            runCatching {
                                val upload = repository.prepareAssetUpload(
                                    sessionId = active.sessionId,
                                    assetType = "hand_photo",
                                    mimeType = "image/jpeg",
                                )
                                val publicUrl = SupabaseManager.uploadHandPhotoToPath(
                                    photoBytes = bitmapToJpegBytes(handBitmap),
                                    storagePath = upload.storage_path,
                                )
                                repository.confirmAssetUpload(
                                    sessionId = active.sessionId,
                                    assetId = upload.asset_id,
                                    assetType = "hand_photo",
                                    storagePath = upload.storage_path,
                                )
                                val tryOn = repository.createTryOn(active.sessionId)

                                NailSessionRuntime.current = active.copy(
                                    status = tryOn.status,
                                    handAssetId = upload.asset_id,
                                    handStoragePath = upload.storage_path,
                                    handPhotoUrl = publicUrl,
                                    tryOnStatus = tryOn.status,
                                )
                            }.onSuccess {
                                statusText = "手部照片已上传并触发试戴任务。"
                                showTryOnResult = true
                            }.onFailure { error ->
                                statusText = "试戴触发失败：${error.message ?: "未知错误"}"
                            }

                            isGeneratingTryOn = false
                        }
                    }
                },
                enabled = hasHandPhoto || showTryOnResult,
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.LightGray
                )
            ) {
                Text(
                    if (showTryOnResult) "绝美！我要开始做了" else "生成 AI 虚拟试戴效果",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}

private fun bitmapToJpegBytes(bitmap: Bitmap?): ByteArray {
    requireNotNull(bitmap) { "Hand bitmap is required before upload." }
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
