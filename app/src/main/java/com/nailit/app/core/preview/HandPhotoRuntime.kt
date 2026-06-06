package com.nailit.app.core.preview

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object HandPhotoRuntime {
    var currentBitmap: Bitmap? by mutableStateOf(null)
}
