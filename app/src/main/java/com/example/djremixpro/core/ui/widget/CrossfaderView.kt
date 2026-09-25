package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp
import com.example.djremixpro.core.ui.ext.hapticTick
import kotlin.math.hypot

/**
 * Crossfader (Mixer:206-210): 44dp tall; track 4dp at y 20 (left half deck A 55%, right half deck B 55%);
 * centre tick 2x16 at y 14; thumb 36x28 at y 8, left = xf·(width − 36).
 * Down/drag sets xf = clamp((x − 18)/(width − 36)) (Mixer:464), double tap → [onReset], centre detent haptic.
 */
class CrossfaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var onValueChange: ((Float) -> Unit)? = null
    var onReset: (() -> Unit)? = null
    var hapticsEnabled: Boolean = true

    private val trackA = context.colorOf(R.color.md_deck_a_55)
    private val trackB = context.colorOf(R.color.md_deck_b_55)
    private val mutedColor = context.colorOf(R.color.md_muted)
    private val raisedColor = context.colorOf(R.color.md_raised)
    private val line2Color = context.colorOf(R.color.md_line2)
    private val textColor = context.colorOf(R.color.md_text)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private val thumbW = dp(36f)
    private val thumbH = dp(28f)

    private var value = 0.5f
    private var dragging = false

    private val config = ViewConfiguration.get(context)
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastSide = 0
    private var lastTapUpTime = 0L
    private var lastTapX = 0f
    private var downX = 0f
    private var moved = false

    init {
        isFocusable = true
    }

    fun setValue(value: Float) {
        if (dragging) return
        this.value = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val top = paddingTop + ((height - paddingTop - paddingBottom) - dp(44f)) / 2f
        val mid = (left + right) / 2f
        paint.style = Paint.Style.FILL
        // Track halves, rounded ends.
        val r = dp(2f)
        rect.set(left, top + dp(20f), right, top + dp(24f))
        val save = canvas.save()
        canvas.clipRect(left, rect.top, mid, rect.bottom)
        paint.color = trackA
        canvas.drawRoundRect(rect, r, r, paint)
        canvas.restoreToCount(save)
        val save2 = canvas.save()
        canvas.clipRect(mid, rect.top, right, rect.bottom)
        paint.color = trackB
        canvas.drawRoundRect(rect, r, r, paint)
        canvas.restoreToCount(save2)
        // Centre tick.
        paint.color = mutedColor
        rect.set(mid - dp(1f), top + dp(14f), mid + dp(1f), top + dp(30f))
        canvas.drawRect(rect, paint)
        // Thumb.
        val thumbLeft = left + value * (right - left - thumbW)
        rect.set(thumbLeft, top + dp(8f), thumbLeft + thumbW, top + dp(8f) + thumbH)
        paint.color = raisedColor
        canvas.drawRoundRect(rect, dp(8f), dp(8f), paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1f)
        paint.color = line2Color
        rect.inset(dp(0.5f), dp(0.5f))
        canvas.drawRoundRect(rect, dp(8f), dp(8f), paint)
        paint.style = Paint.Style.FILL
        paint.color = textColor
        val cx = rect.centerX()
        val cy = rect.centerY()
        rect.set(cx - dp(1f), cy - dp(7f), cx + dp(1f), cy + dp(7f))
        canvas.drawRoundRect(rect, dp(1f), dp(1f), paint)
    }

    private fun valueAt(x: Float): Float {
        val left = paddingLeft.toFloat()
        val w = (width - paddingLeft - paddingRight).toFloat()
        return ((x - left - thumbW / 2f) / (w - thumbW).coerceAtLeast(1f)).coerceIn(0f, 1f)
    }

    private fun update(raw: Float) {
        // Small centre detent so the exact middle is reachable by finger.
        val next = if (kotlin.math.abs(raw - 0.5f) < DETENT) 0.5f else raw
        val side = sideOf(next)
        if (hapticsEnabled && side != lastSide && (side == 0 || side == -lastSide)) hapticTick()
        lastSide = side
        if (next != value) {
            value = next
            onValueChange?.invoke(next)
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                activePointerId = event.getPointerId(0)
                downX = event.x
                moved = false
                if (event.eventTime - lastTapUpTime <= ViewConfiguration.getDoubleTapTimeout() &&
                    hypot(event.x - lastTapX, 0f) < config.scaledDoubleTapSlop
                ) {
                    lastTapUpTime = 0L
                    value = 0.5f
                    lastSide = 0
                    invalidate()
                    onReset?.invoke()
                    return true
                }
                dragging = true
                lastSide = sideOf(value)
                update(valueAt(event.x))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return true
                val index = event.findPointerIndex(activePointerId)
                if (index < 0) return true
                if (!moved && kotlin.math.abs(event.getX(index) - downX) > config.scaledTouchSlop) moved = true
                update(valueAt(event.getX(index)))
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == activePointerId) end()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val tap = dragging && !moved
                if (tap) {
                    lastTapUpTime = event.eventTime
                    lastTapX = event.x
                }
                end()
                if (tap) performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                end()
                return true
            }
        }
        return false
    }

    private fun sideOf(v: Float) = when {
        v < 0.5f -> -1
        v > 0.5f -> 1
        else -> 0
    }

    private fun end() {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        dragging = false
    }

    override fun performClick(): Boolean = super.performClick()

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.applySlider(value, isEnabled)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (isEnabled) {
            val next = sliderActionValue(action, arguments, value)
            if (next != null) {
                update(next)
                return true
            }
        }
        return super.performAccessibilityAction(action, arguments)
    }

    private companion object {
        const val DETENT = 0.01f
    }
}
