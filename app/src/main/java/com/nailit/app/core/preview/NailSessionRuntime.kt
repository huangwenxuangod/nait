package com.nailit.app.core.preview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nailit.app.core.model.ExecutionStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.JsonObject

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
    val currentStepIndex: Int = 0,
    val currentStepTitle: String? = null,
    val estimatedTotalMinutes: Int = 0,
    val targetImagePath: String? = null,
    val finalResultPath: String? = null,
    val executionSteps: List<ExecutionStep> = emptyList(),
    // AI Parsed Cache
    val sourceParseJson: JsonObject? = null,
    val bomJson: JsonObject? = null,
    val sopJson: JsonObject? = null,
    val tryOnError: String? = null,
    val executionError: String? = null,
)

object NailSessionRuntime {
    var current: NailSessionSnapshot? by mutableStateOf(null)
    val backgroundScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
}
