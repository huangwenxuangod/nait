package com.nailit.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nailit.app.core.share.ShareIntentHelper
import com.nailit.app.core.share.SharePosterComposer
import io.github.jan.supabase.storage.storage
import java.io.File

private val ShareBg = Color(0xFFF7F2ED)
private val ShareAccent = Color(0xFF281B19)
private val ShareText = Color(0xFF171311)
private val ShareMuted = Color(0xFF80756D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePosterScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val session = com.nailit.app.core.preview.NailSessionRuntime.current
    var posterFile by remember { mutableStateOf<File?>(null) }
    var loading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("正在生成分享海报...") }

    val sourceBitmap = com.nailit.app.core.preview.HandPhotoRuntime.currentBitmap
    val finalBitmap = FinalPhotoRuntime.currentBitmap

    LaunchedEffect(Unit) {
        loading = true
        val tryOnBitmap = session?.targetImagePath?.let { path ->
            runCatching {
                val bytes = com.nailit.app.core.network.SupabaseManager.client.storage
                    .from("nail-it-assets")
                    .downloadAuthenticated(path)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }

        if (sourceBitmap == null || tryOnBitmap == null || finalBitmap == null) {
            statusText = "缺少原图、试戴图或成品图，暂时无法生成海报。"
            loading = false
            return@LaunchedEffect
        }

        val title = session.selectedTutorialId
            ?.replace("-", " ")
            ?.replaceFirstChar { it.uppercase() }
            ?: "今天试了这个款"

        posterFile = runCatching {
            SharePosterComposer.createPoster(
                context = context,
                sourceBitmap = sourceBitmap,
                tryOnBitmap = tryOnBitmap,
                finalBitmap = finalBitmap,
                styleTitle = title,
                note = "先试戴了一下，最后真的把它做出来了",
            )
        }.getOrNull()

        statusText = if (posterFile != null) "海报已生成，可以直接发抖音。" else "海报生成失败，请重试。"
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "分享海报",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = ShareText,
                            fontWeight = FontWeight.Bold,
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ShareBg),
            )
        },
        containerColor = ShareBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge.copy(color = ShareMuted),
            )

            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            } else {
                posterFile?.let { file ->
                    val bitmap = remember(file.absolutePath) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Share poster",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(560.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.White),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val file = posterFile ?: return@Button
                    ShareIntentHelper.shareImage(context, file)
                },
                enabled = posterFile != null && !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ShareAccent,
                    contentColor = Color.White,
                )
            ) {
                Text("发到抖音")
            }
        }
    }
}
