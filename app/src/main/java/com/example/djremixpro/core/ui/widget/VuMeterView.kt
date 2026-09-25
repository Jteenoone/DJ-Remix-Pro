package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.PathInterpolator
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp

/**
 * Channel VU meter (Mixer:195-198): line background, deck colour 0–88 %, rec 88–100 %, separators 5dp / 2dp in
 * panel colour. The level is simulated (D-07) with the md-vu keyframes while [setActive] is true, otherwise 2 %.
 */
class VuMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var palette: DeckPalette
    private val periodMs: Long
    private val phaseMs: Long

    private val lineColor = context.colorOf(R.color.md_line)
    private val panelColor = context.colorOf(R.color.md_panel)
    private val recColor = context.colorOf(R.color.md_rec)
    private val paint = Paint()
    private val easeInOut = PathInterpolator(0.42f, 0f, 0.58f, 1f)

    private var active = false

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.VuMeterView, defStyleAttr, 0)
        palette = DeckPalette.of(context, a.getInt(R.styleable.VuMeterView_mdDeck, DeckPalette.DECK_A))
        periodMs = a.getInt(R.styleable.VuMeterView_mdVuPeriodMs, 900).toLong().coerceAtLeast(1L)
        phaseMs = a.getInt(R.styleable.VuMeterView_mdVuPhaseMs, 0).toLong()
        a.recycle()
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.dp(2f))
            }
        }
        clipToOutline = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setDeck(deck: Int) {
        palette = DeckPalette.of(context, deck)
        invalidate()
    }

    fun setActive(active: Boolean) {
        if (this.active == active) return
        this.active = active
        invalidate()
    }

    /** md-vu: scaleY of the top mask at keyframes; level = 1 − scale. */
    private fun level(): Float {
        if (!active) return 1f - IDLE_SCALE
        val t = (((SystemClock.uptimeMillis() - phaseMs) % periodMs + periodMs) % periodMs) / periodMs.toFloat()
        var i = 0
        while (i < KEY_TIMES.size - 2 && t > KEY_TIMES[i + 1]) i++
        val span = KEY_TIMES[i + 1] - KEY_TIMES[i]
        val f = easeInOut.getInterpolation(((t - KEY_TIMES[i]) / span).coerceIn(0f, 1f))
        val scale = KEY_SCALES[i] + (KEY_SCALES[i + 1] - KEY_SCALES[i]) * f
        return 1f - scale
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.color = lineColor
        canvas.drawRect(0f, 0f, w, h, paint)

        val levelTop = h - level() * h
        val splitY = h - RED_FROM * h
        paint.color = palette.accent
        canvas.drawRect(0f, maxOf(levelTop, splitY), w, h, paint)
        if (levelTop < splitY) {
            paint.color = recColor
            canvas.drawRect(0f, levelTop, w, splitY, paint)
        }
        // repeating-linear-gradient(to top, transparent 0 5px, panel 5px 7px).
        paint.color = panelColor
        val seg = dp(5f)
        val gap = dp(2f)
        var bottom = h - seg
        while (bottom > 0f) {
            canvas.drawRect(0f, (bottom - gap).coerceAtLeast(0f), w, bottom, paint)
            bottom -= seg + gap
        }
        if (active) postInvalidateOnAnimation()
    }

    private companion object {
        const val IDLE_SCALE = 0.98f
        const val RED_FROM = 0.88f
        val KEY_TIMES = floatArrayOf(0f, 0.18f, 0.36f, 0.55f, 0.74f, 1f)
        val KEY_SCALES = floatArrayOf(0.48f, 0.2f, 0.4f, 0.16f, 0.52f, 0.48f)
    }
}
