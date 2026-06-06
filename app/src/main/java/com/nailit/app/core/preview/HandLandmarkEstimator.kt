package com.nailit.app.core.preview

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.nailit.app.core.model.NailPositionHint
import kotlin.math.atan2

object HandLandmarkEstimator {
    private const val MODEL_ASSET = "hand_landmarker.task"

    // ⚠️ 安全开关：由于 OPPO/OnePlus 等部分机型底层 GPU/内存分配器与 MediaPipe 原生 C++ 库存在兼容性冲突
    // 会直接触发底层 SIGSEGV (Signal 11) 段错误导致 App 瞬间闪退（Native 崩溃无法被 Kotlin 的 try-catch 捕获）。
    // 如果您在测试机上遇到闪退，请将此开关设为 true，系统将跳过原生 C++ 估算，直接使用 100% 稳定的算法骨架兜底，确保业务流程畅通！
    private const val USE_FALLBACK_ONLY = true

    fun estimate(context: Context, bitmap: Bitmap): List<NailPositionHint> {
        if (USE_FALLBACK_ONLY) {
            return emptyList() // 触发外层 .ifEmpty { estimateFallbackNailPositionHints() } 极速兜底
        }
        return runCatching {
            val softwareBitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
                bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
            val handLandmarker = buildLandmarker(context)
            val mpImage = BitmapImageBuilder(softwareBitmap).build()
            val result = handLandmarker.detect(mpImage)
            handLandmarker.close()
            result.toNailHints()
        }.getOrDefault(emptyList())
    }

    private fun buildLandmarker(context: Context): HandLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setNumHands(1)
            .build()

        return HandLandmarker.createFromOptions(context, options)
    }

    private fun HandLandmarkerResult.toNailHints(): List<NailPositionHint> {
        val hand = landmarks().firstOrNull() ?: return emptyList()
        if (hand.size < 21) return emptyList()

        return listOf(
            hintFromTip(hand, "thumb", 4, 3, 2, 0.12f, 0.16f),
            hintFromTip(hand, "index", 8, 7, 6, 0.09f, 0.14f),
            hintFromTip(hand, "middle", 12, 11, 10, 0.095f, 0.15f),
            hintFromTip(hand, "ring", 16, 15, 14, 0.09f, 0.14f),
            hintFromTip(hand, "pinky", 20, 19, 18, 0.08f, 0.12f),
        )
    }

    private fun hintFromTip(
        hand: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        finger: String,
        tipIndex: Int,
        dipIndex: Int,
        pipIndex: Int,
        widthRatio: Float,
        heightRatio: Float,
    ): NailPositionHint {
        val tip = hand[tipIndex]
        val dip = hand[dipIndex]
        val pip = hand[pipIndex]

        val dx = tip.x() - pip.x()
        val dy = tip.y() - pip.y()
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        val centerX = ((tip.x() * 0.72f) + (dip.x() * 0.28f)).coerceIn(0.02f, 0.98f)
        val centerY = ((tip.y() * 0.72f) + (dip.y() * 0.28f)).coerceIn(0.02f, 0.98f)
        val segmentLength = distance(tip.x(), tip.y(), pip.x(), pip.y()).coerceAtLeast(0.03f)

        return NailPositionHint(
            finger = finger,
            center_x = centerX,
            center_y = centerY,
            width_ratio = (segmentLength * widthRatio / 0.09f).coerceIn(0.05f, 0.14f),
            height_ratio = (segmentLength * heightRatio / 0.14f).coerceIn(0.08f, 0.20f),
            angle_deg = angle,
        )
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
