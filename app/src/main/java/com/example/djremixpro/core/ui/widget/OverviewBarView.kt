package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp

/**
 * Track overview (Mixer:58-64): bar 8dp, radius 2, line background, played part in deck colour at 50% opacity,
 * 2dp cursor in text colour. Touch/drag seeks (D-19) through [onSeek]. The bar is vertically centred so the view
 * can be taller than [mdBarHeight] for a bigger touch target.
 */
class OverviewBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var onSeek: ((Float) -> Unit)? = null

    private var palette: DeckPalette
    private val barHeight: Float
    private val lineColor = context.colorOf(R.color.md_line)
    private val textColor = context.colorOf(R.color.md_text)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var progress = 0f
    private var dragging = false
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.OverviewBarView, defStyleAttr, 0)
        palette = DeckPalette.of(context, a.getInt(R.styleable.OverviewBarView_mdDeck, DeckPalette.DECK_A))
        barHeight = a.getDimension(R.styleable.OverviewBarView_mdBarHeight, dp(8f))
        a.recycle()
        isFocusable = true
    }

    fun setDeck(deck: Int) {
        palette = DeckPalette.of(context, deck)
        invalidate()
    }

    fun setProgress(progress: Float) {
        if (dragging) return
        this.progress = progress.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val top = paddingTop + ((height - paddingTop - paddingBottom) - barHeight) / 2f
        val r = dp(2f)
        rect.set(left, top, right, top + barHeight)
        val save = canvas.save()
        canvas.clipRect(rect)
        paint.color = lineColor
        canvas.drawRoundRect(rect, r, r, paint)
        val x = left + progress * (right - left)
        paint.color = palette.accent
        paint.alpha = 128
        // Rounded on the left like the clipped track; the right end is covered by the cursor.
        canvas.drawRoundRect(left, top, x + r, top + barHeight, r, r, paint)
        paint.alpha = 255
        paint.color = textColor
        canvas.drawRect(x, top, x + dp(2f), top + barHeight, paint)
        canvas.restoreToCount(save)
    }

    private fun seekTo(x: Float) {
        val w = (width - paddingLeft - paddingRight).toFloat().coerceAtLeast(1f)
        val f = ((x - paddingLeft) / w).coerceIn(0f, 1f)
        if (f != progress) {
            progress = f
            invalidate()
            onSeek?.invoke(f)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                activePointerId = event.getPointerId(0)
                dragging = true
                seekTo(event.x)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(activePointerId)
                if (index >= 0) seekTo(event.getX(index))
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

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.applySlider(progress, isEnabled)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (isEnabled) {
            val next = sliderActionValue(action, arguments, progress)
            if (next != null) {
                progress = next
                invalidate()
                onSeek?.invoke(next)
                return true
            }
        }
        return super.performAccessibilityAction(action, arguments)
    }
}
