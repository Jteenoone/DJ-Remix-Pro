package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.accessibility.AccessibilityNodeInfo
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp
import com.example.djremixpro.core.ui.ext.fontOf
import com.example.djremixpro.core.ui.ext.sp

/**
 * FX XY pad (Mixer:253-260): radius 16, booth background, 25 % grid in grid colour, 1dp line border; fill from the
 * bottom-left (width x, height 1 − y) in deck 12 %, crosshair deck 60 %, thumb 30 (deck fill, 3dp booth border,
 * 2dp deck halo). x = time (left→right), y from the top (up = stronger), both 0..1. Drag reports [onChange].
 */
class XYPadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var onChange: ((x: Float, y: Float) -> Unit)? = null

    private var palette: DeckPalette
    private val boothColor = context.colorOf(R.color.md_booth)
    private val gridColor = context.colorOf(R.color.md_grid)
    private val lineColor = context.colorOf(R.color.md_line)
    private val mutedColor = context.colorOf(R.color.md_muted)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = context.fontOf(R.font.be_vietnam_pro_regular)
        textSize = sp(12f)
        color = mutedColor
    }
    private val rect = RectF()
    private val amountLabel = context.getString(R.string.fx_axis_amount)
    private val timeLabel = context.getString(R.string.fx_axis_time)

    private var xValue = 0.62f
    private var yValue = 0.3f
    private var dragging = false
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.XYPadView, defStyleAttr, 0)
        palette = DeckPalette.of(context, a.getInt(R.styleable.XYPadView_mdDeck, DeckPalette.DECK_A))
        a.recycle()
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.dp(16f))
            }
        }
        clipToOutline = true
        isFocusable = true
    }

    fun setDeck(deck: Int) {
        palette = DeckPalette.of(context, deck)
        invalidate()
    }

    fun setValues(x: Float, y: Float) {
        if (dragging) return
        xValue = x.coerceIn(0f, 1f)
        yValue = y.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val one = dp(1f)
        paint.style = Paint.Style.FILL
        paint.color = boothColor
        canvas.drawRect(0f, 0f, w, h, paint)
        // Grid: 1dp lines at the start of each 25 % tile.
        paint.color = gridColor
        for (i in 0..3) {
            val x = w * i / 4f
            canvas.drawRect(x, 0f, x + one, h, paint)
            val y = h * i / 4f
            canvas.drawRect(0f, y, w, y + one, paint)
        }
        val px = xValue * w
        val py = yValue * h
        paint.color = palette.accentAlpha(12)
        canvas.drawRect(0f, py, px, h, paint)
        paint.color = palette.accentAlpha(60)
        canvas.drawRect(px, 0f, px + one, h, paint)
        canvas.drawRect(0f, py, w, py + one, paint)

        canvas.drawText(amountLabel, dp(12f), dp(8f) - labelPaint.ascent(), labelPaint)
        val tw = labelPaint.measureText(timeLabel)
        canvas.drawText(timeLabel, w - dp(12f) - tw, h - dp(8f) - labelPaint.descent(), labelPaint)

        // Thumb: halo 2dp deck, 30 circle deck with 3dp booth border.
        paint.color = palette.accent
        canvas.drawCircle(px, py, dp(17f), paint)
        paint.color = boothColor
        canvas.drawCircle(px, py, dp(15f), paint)
        paint.color = palette.accent
        canvas.drawCircle(px, py, dp(12f), paint)

        // Border 1dp line.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = one
        paint.color = lineColor
        rect.set(one / 2f, one / 2f, w - one / 2f, h - one / 2f)
        canvas.drawRoundRect(rect, dp(16f), dp(16f), paint)
    }

    private fun setFromTouch(x: Float, y: Float) {
        val nx = (x / width.coerceAtLeast(1)).coerceIn(0f, 1f)
        val ny = (y / height.coerceAtLeast(1)).coerceIn(0f, 1f)
        if (nx != xValue || ny != yValue) {
            xValue = nx
            yValue = ny
            invalidate()
            onChange?.invoke(nx, ny)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                activePointerId = event.getPointerId(0)
                dragging = true
                setFromTouch(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(activePointerId)
                if (index >= 0) setFromTouch(event.getX(index), event.getY(index))
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == activePointerId) dragging = false
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean = super.performClick()

    /** TalkBack: the slider value is the time axis (x); scroll actions move x in 1/6 steps (readout buckets). */
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.applySlider(xValue, isEnabled)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (isEnabled) {
            val next = sliderActionValue(action, arguments, xValue, 1f / 6f)
            if (next != null) {
                xValue = next
                invalidate()
                onChange?.invoke(xValue, yValue)
                return true
            }
        }
        return super.performAccessibilityAction(action, arguments)
    }
}
