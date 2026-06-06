package com.nailit.app.core.preview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class NailSessionSnapshot(
    val sessionId: String,
    val sourceUrl: String,
    val sourceType: String,
    val selectedTutorialId: String?,
    val status: String,
    val handAssetId: String? = null,
    val handStoragePath: String? = null,
    val handPhotoUrl: String? = null,
    val tryOnStatus: String? = null,
    val executionStatus: String? = null,
)

object NailSessionRuntime {
    var current: NailSessionSnapshot? by mutableStateOf(null)
}
