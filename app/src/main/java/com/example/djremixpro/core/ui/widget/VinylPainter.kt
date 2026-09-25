package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf

/**
 * `repeating-radial-gradient(circle, #1E1828 0 2px, #272031 2px 3px)` (Mixer:105, App:33): 3dp period,
 * first 2dp groove colour 1, last 1dp colour 2. Colours are fixed (dark in both themes, D-16).
 */
internal class VinylPainter(context: Context) {
    private val groove1 = context.colorOf(R.color.md_fixed_vinyl_groove_1)
    private val groove2 = context.colorOf(R.color.md_fixed_vinyl_groove_2)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    /** Rebuilds the shader; call from onSizeChanged, never from onDraw. [periodPx] is 3dp scaled. */
    fun update(cx: Float, cy: Float, periodPx: Float) {
        paint.shader = RadialGradient(
            cx, cy, periodPx,
            intArrayOf(groove1, groove1, groove2, groove2),
            floatArrayOf(0f, 2f / 3f, 2f / 3f, 1f),
            Shader.TileMode.REPEAT,
        )
    }
}
