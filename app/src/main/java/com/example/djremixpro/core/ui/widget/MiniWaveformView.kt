package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.util.WaveformGenerator
import kotlin.math.min

/**
 * Recording thumbnail waveform (App:128, App:278): 16 bars in a 56x28 viewBox, bar i at x = 4 + 3i, 2 wide,
 * centred on y 14. Colour: text when playing, muted otherwise. Heights come from [WaveformGenerator.mini].
 */
class MiniWaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val textColor = context.colorOf(R.color.md_text)
    private val mutedColor = context.colorOf(R.color.md_muted)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var seed = Int.MIN_VALUE
    private var heights = FloatArray(WaveformGenerator.MINI_BARS)
    private var active = false

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setSeed(seed: Int) {
        if (seed == this.seed) return
        this.seed = seed
        heights = WaveformGenerator.mini(seed)
        invalidate()
    }

    fun setActive(active: Boolean) {
        if (active == this.active) return
        this.active = active
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val unit = min(width / VIEW_W, height / VIEW_H)
        val left = (width - VIEW_W * unit) / 2f
        val top = (height - VIEW_H * unit) / 2f
        paint.color = if (active) textColor else mutedColor
        val cy = top + 14f * unit
        for (i in heights.indices) {
            val x = left + (4f + i * 3f) * unit
            val h = heights[i] * unit
            canvas.drawRect(x, cy - h, x + 2f * unit, cy + h, paint)
        }
    }

    private companion object {
        const val VIEW_W = 56f
        const val VIEW_H = 28f
    }
}
