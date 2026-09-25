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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Vertical fader. [value] is 0..1 with 1 at the top.
 * - volume (Mixer:191-193): rail 2dp at x 10 inset 4, thumb 22x30 radius 5, travel = height - 30.
 * - pitch (Mixer:92-97): rail 2dp at x 19, centre notch 16x2, thumb 28x16 centred on the value, inner line in deck
 *   colour when shifted; snaps to the centre detent (with a haptic tick when [hapticsEnabled]).
 * Relative drag (the thumb never jumps to the finger). While dragging the thumb rim/line turn deck colour and a
 * bubble shows [label] (or the value in %).
 */
class VerticalFaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var onValueChange: ((Float) -> Unit)? = null
    var hapticsEnabled: Boolean = true

    private var palette: DeckPalette
    private val isPitch: Boolean

    private val lineColor = context.colorOf(R.color.md_line)
    private val line2Color = context.colorOf(R.color.md_line2)
    private val raisedColor = context.colorOf(R.color.md_raised)
    private val textColor = context.colorOf(R.color.md_text)
    private val mutedColor = context.colorOf(R.color.md_muted)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val thumbRect = RectF()

    private var value = 0.5f
    private var label: String? = null
    private var shifted = false

    private val thumbW: Float
    private val thumbH: Float
    private val radius = dp(5f)

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var downY = 0f
    private var startValue = 0f
    private var dragValue = 0f
    private var dragging = false
    private var inDetent = false
    private val bubble = ValueBubble(this)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.VerticalFaderView, defStyleAttr, 0)
        palette = DeckPalette.of(context, a.getInt(R.styleable.VerticalFaderView_mdDeck, DeckPalette.DECK_A))
        isPitch = a.getInt(R.styleable.VerticalFaderView_mdFaderStyle, 0) == 1
        a.recycle()
        thumbW = if (isPitch) dp(28f) else dp(22f)
        thumbH = if (isPitch) dp(16f) else dp(30f)
        isFocusable = true
    }

    fun setDeck(deck: Int) {
        palette = DeckPalette.of(context, deck)
        invalidate()
    }

    /**
     * [label] is shown in the drag bubble (pitch: DeckUiState.pitchLabel); null → "NN%".
     * [shifted] colours the pitch thumb line with the deck colour (|pitch| > 0.05).
     */
    fun bind(value: Float, label: String? = null, shifted: Boolean = false) {
        this.value = value.coerceIn(0f, 1f)
        this.label = label
        this.shifted = shifted
        if (dragging) showBubble()
        invalidate()
    }

    private fun travelTop(): Float = if (isPitch) paddingTop + thumbH / 2f else paddingTop.toFloat()

    private fun travel(): Float = (height - paddingTop - paddingBottom - thumbH).coerceAtLeast(1f)

    private fun computeThumb(v: Float) {
        val cx = paddingLeft + (width - paddingLeft - paddingRight) / 2f
        val top = if (isPitch) {
            travelTop() + (1f - v) * travel() - thumbH / 2f
        } else {
            travelTop() + (1f - v) * travel()
        }
        thumbRect.set(cx - thumbW / 2f, top, cx + thumbW / 2f, top + thumbH)
    }

    override fun onDraw(canvas: Canvas) {
        val v = if (dragging) dragValue else value
        val cx = paddingLeft + (width - paddingLeft - paddingRight) / 2f
        paint.style = Paint.Style.FILL
        // Rail.
        paint.color = lineColor
        val railInset = if (isPitch) 0f else dp(4f)
        rect.set(cx - dp(1f), paddingTop + railInset, cx + dp(1f), height - paddingBottom - railInset)
        canvas.drawRoundRect(rect, dp(1f), dp(1f), paint)
        if (isPitch) {
            // Centre notch 16x2 in muted.
            val cy = paddingTop + (height - paddingTop - paddingBottom) / 2f
            paint.color = mutedColor
            rect.set(cx - dp(8f), cy - dp(1f), cx + dp(8f), cy + dp(1f))
            canvas.drawRect(rect, paint)
        }
        // Thumb.
        computeThumb(v)
        paint.color = raisedColor
        canvas.drawRoundRect(thumbRect, radius, radius, paint)
        paint.style = Paint.Style.STROKE
        val strokeW = if (dragging) dp(1.5f) else dp(1f)
        paint.strokeWidth = strokeW
        paint.color = if (dragging) palette.accent else line2Color
        rect.set(thumbRect)
        rect.inset(strokeW / 2f, strokeW / 2f)
        canvas.drawRoundRect(rect, radius, radius, paint)
        // Inner line: volume 12x2 text; pitch 16x2 deck colour when shifted.
        paint.style = Paint.Style.FILL
        paint.color = when {
            dragging -> palette.accent
            isPitch && shifted -> palette.accent
            else -> textColor
        }
        val half = if (isPitch) dp(8f) else dp(6f)
        rect.set(thumbRect.centerX() - half, thumbRect.centerY() - dp(1f), thumbRect.centerX() + half, thumbRect.centerY() + dp(1f))
        canvas.drawRoundRect(rect, dp(1f), dp(1f), paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                downY = event.y
                startValue = value
                dragValue = value
                dragging = false
                inDetent = isPitch && abs(value - 0.5f) < DETENT
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(activePointerId)
                if (index < 0) return true
                val y = event.getY(index)
                if (!dragging && abs(y - downY) > touchSlop) {
                    dragging = true
                    downY = y
                    invalidate()
                }
                if (dragging) {
                    var next = (startValue - (y - downY) / travel()).coerceIn(0f, 1f)
                    if (isPitch) {
                        val nowInDetent = abs(next - 0.5f) < DETENT
                        if (nowInDetent) next = 0.5f
                        if (nowInDetent && !inDetent && hapticsEnabled) hapticTick()
                        inDetent = nowInDetent
                    }
                    if (next != dragValue) {
                        dragValue = next
                        onValueChange?.invoke(next)
                        invalidate()
                    }
                    showBubble()
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == activePointerId) endDrag()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val tap = !dragging
                endDrag()
                if (tap) performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                endDrag()
                return true
            }
        }
        return false
    }

    private fun endDrag() {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        if (dragging) {
            dragging = false
            value = dragValue
        }
        bubble.hide()
        invalidate()
    }

    private fun showBubble() {
        computeThumb(dragValue)
        val text = label ?: "${(dragValue * 100).roundToInt()}%"
        bubble.show(text, ValueBubble.Placement.BESIDE, thumbRect.top, thumbRect.bottom)
    }

    override fun performClick(): Boolean = super.performClick()

    override fun onDetachedFromWindow() {
        bubble.hide()
        super.onDetachedFromWindow()
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.applySlider(value, isEnabled)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (isEnabled) {
            val next = sliderActionValue(action, arguments, value, if (isPitch) 0.025f else 0.05f)
            if (next != null) {
                value = next
                onValueChange?.invoke(next)
                invalidate()
                return true
            }
        }
        return super.performAccessibilityAction(action, arguments)
    }

    private companion object {
        /** Pitch centre detent half-width in value units (±1.5% of the travel). */
        const val DETENT = 0.015f
    }
}
