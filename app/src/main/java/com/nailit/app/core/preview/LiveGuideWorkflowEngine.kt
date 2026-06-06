package com.nailit.app.core.preview

import com.nailit.app.core.model.ExecutionStep

data class LiveGuideUiState(
    val mode: LiveGuideMode = LiveGuideMode.Connecting,
    val stepIndex: Int = 0,
    val coachLine: String = "正在连接视频带做助手...",
)

enum class LiveGuideMode {
    Connecting,
    Guiding,
    Waiting,
    Checking,
    Paused,
}

class LiveGuideWorkflowEngine(
    private val steps: List<ExecutionStep>,
) {
    fun initialState(): LiveGuideUiState {
        val firstStep = steps.firstOrNull()
        return LiveGuideUiState(
            mode = LiveGuideMode.Connecting,
            stepIndex = 0,
            coachLine = firstStep?.instruction ?: "正在准备流程...",
        )
    }

    fun onConnected(current: LiveGuideUiState): LiveGuideUiState {
        return current.copy(
            mode = LiveGuideMode.Guiding,
            coachLine = currentStep(current.stepIndex)?.instruction ?: "这一层先做薄一点。",
        )
    }

    fun onPause(current: LiveGuideUiState): LiveGuideUiState {
        return current.copy(
            mode = if (current.mode == LiveGuideMode.Paused) LiveGuideMode.Waiting else LiveGuideMode.Paused,
            coachLine = if (current.mode == LiveGuideMode.Paused) {
                "继续这一层，做完再告诉我。"
            } else {
                "先停在这里，别急着继续。"
            },
        )
    }

    fun onRepeat(current: LiveGuideUiState): LiveGuideUiState {
        return current.copy(
            mode = LiveGuideMode.Guiding,
            coachLine = currentStep(current.stepIndex)?.instruction ?: current.coachLine,
        )
    }

    fun onCheck(current: LiveGuideUiState): LiveGuideUiState {
        val step = currentStep(current.stepIndex)
        return current.copy(
            mode = LiveGuideMode.Checking,
            coachLine = "我正在复核这一步，重点看：${step?.voiceGoal?.ifBlank { "边缘是否干净、厚度是否均匀" } ?: "当前步骤是否完成"}。",
        )
    }

    fun onNext(current: LiveGuideUiState): LiveGuideUiState {
        val nextIndex = (current.stepIndex + 1).coerceAtMost((steps.size - 1).coerceAtLeast(0))
        val nextStep = currentStep(nextIndex)
        return current.copy(
            mode = if (nextIndex == current.stepIndex && nextIndex == steps.lastIndex) {
                LiveGuideMode.Waiting
            } else {
                LiveGuideMode.Guiding
            },
            stepIndex = nextIndex,
            coachLine = if (nextIndex == current.stepIndex && nextIndex == steps.lastIndex) {
                "已经到最后一步了，做完这层就能收尾。"
            } else {
                nextStep?.instruction ?: "继续下一步。"
            },
        )
    }

    fun currentStep(index: Int): ExecutionStep? = steps.getOrNull(index)
}
