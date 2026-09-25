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
import com.example.djremixpro.core.ui.ext.hapticLongPress
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * EQ / filter / wet-dry knob drawn from the 44 viewBox of Mixer:411-420 (scaled to the view size, 44 or 52dp).
 * Track arc -135°..+135°, two-sided value arc from 12 o'clock, pointer rotated (v-0.5)*270°.
 *
 * Gestures (MixDeck:539): vertical drag changes the value, double tap → [onReset], long press → [onKill].
 * While dragging the body rim turns deck colour and a bubble "<band> <valueLabel>" is shown above.
 */
class KnobView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var onValueChange: ((Float) -> Unit)? = null
    var onReset: (() -> Unit)? = null
    var onKill: (() -> Unit)? = null

    /** false = display only (e.g. fixed Wet/Dry in the FX sheet). */
    var interactive: Boolean = true

    var hapticsEnabled: Boolean = true

    private var palette: DeckPalette
    private var bandLabel: String

    private val lineColor = context.colorOf(R.color.md_line)
    private val line2Color = context.colorOf(R.color.md_line2)
    private val raisedColor = context.colorOf(R.color.md_raised)
    private val textColor = context.colorOf(R.color.md_text)
    private val mutedColor = context.colorOf(R.color.md_muted)
    private val recColor = context.colorOf(R.color.md_rec)

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcRect = RectF()

    private var value = 0.5f
    private var isKill = false
    private var valueLabel = ""

    private var cx = 0f
    private var cy = 0f
    private var unit = 1f

    // Gesture state
    private val config = ViewConfiguration.get(context)
    private val touchSlop = config.scaledTouchSlop
    private val dragRangePx = dp(DRAG_RANGE_DP)
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var downY = 0f
    private var downX = 0f
    private var startValue = 0f
    private var dragValue = 0f
    private var dragging = false
    private var longPressed = false
    private var lastTapUpTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private val bubble = ValueBubble(this)
    private val longPressRunnable = Runnable {
        longPressed = true
        if (hapticsEnabled) hapticLongPress()
        onKill?.invoke()
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.KnobView, defStyleAttr, 0)
        palette = DeckPalette.of(context, a.getInt(R.styleable.KnobView_mdDeck, DeckPalette.DECK_A))
        bandLabel = a.getString(R.styleable.KnobView_mdBandLabel).orEmpty()
        a.recycle()
        isFocusable = true
    }

    fun setDeck(deck: Int) {
        palette = DeckPalette.of(context, deck)
        invalidate()
    }

    fun setBandLabel(label: String) {
        bandLabel = label
    }

    /** [valueLabel] e.g. "+3.5 dB", "Kill", "LPF 40%" (KnobUi.valueLabel). */
    fun bind(value: Float, isKill: Boolean, valueLabel: String) {
        this.value = value.coerceIn(0f, 1f)
        this.isKill = isKill
        this.valueLabel = valueLabel
        if (dragging) showBubble()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val size = min(w - paddingLeft - paddingRight, h - paddingTop - paddingBottom).toFloat()
        unit = size / VIEWBOX
        cx = paddingLeft + (w - paddingLeft - paddingRight) / 2f
        cy = paddingTop + (h - paddingTop - paddingBottom) / 2f
        arcRect.set(cx - 18f * unit, cy - 18f * unit, cx + 18f * unit, cy + 18f * unit)
    }

    override fun onDraw(canvas: Canvas) {
        val v = when {
            isKill && !dragging -> 0f
            dragging -> dragValue
            else -> value
        }
        val kill = isKill && !dragging
        // Track: stroke 3, line colour, -135°..+135° (0° = 12 o'clock, clockwise).
        arcPaint.strokeWidth = 3f * unit
        arcPaint.color = lineColor
        canvas.drawArc(arcRect, -135f - 90f, 270f, false, arcPaint)
        // Value arc from the top towards the value.
        val a1 = (v - 0.5f) * 270f
        if (abs(a1) >= 2f) {
            arcPaint.color = if (kill) recColor else palette.accent
            canvas.drawArc(arcRect, min(0f, a1) - 90f, abs(a1), false, arcPaint)
        }
        // Top mark (22,1.2) r1.2.
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = mutedColor
        canvas.drawCircle(cx, cy - 20.8f * unit, 1.2f * unit, fillPaint)
        // Body r13.
        fillPaint.color = raisedColor
        canvas.drawCircle(cx, cy, 13f * unit, fillPaint)
        fillPaint.style = Paint.Style.STROKE
        fillPaint.strokeWidth = 1.2f * unit
        fillPaint.color = when {
            kill -> recColor
            dragging -> palette.accent
            else -> line2Color
        }
        canvas.drawCircle(cx, cy, 13f * unit, fillPaint)
        // Pointer (22,11)→(22,17), stroke 2.5.
        val save = canvas.save()
        canvas.rotate(a1, cx, cy)
        arcPaint.strokeWidth = 2.5f * unit
        arcPaint.color = if (kill) recColor else textColor
        canvas.drawLine(cx, cy - 11f * unit, cx, cy - 5f * unit, arcPaint)
        canvas.restoreToCount(save)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactive || !isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                downX = event.x
                downY = event.y
                startValue = if (isKill) 0f else value
                dragValue = startValue
                dragging = false
                longPressed = false
                parent?.requestDisallowInterceptTouchEvent(true)
                val now = event.eventTime
                if (now - lastTapUpTime <= ViewConfiguration.getDoubleTapTimeout() &&
                    hypot(event.x - lastTapX, event.y - lastTapY) < config.scaledDoubleTapSlop
                ) {
                    lastTapUpTime = 0L
                    longPressed = true // consume the rest of this gesture
                    onReset?.invoke()
                    return true
                }
                postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (longPressed) return true
                val index = event.findPointerIndex(activePointerId)
                if (index < 0) return true
                val dy = event.getY(index) - downY
                if (!dragging && hypot(event.getX(index) - downX, dy) > touchSlop) {
                    dragging = true
                    removeCallbacks(longPressRunnable)
                    downY = event.getY(index) // start from here to avoid a jump of touchSlop
                }
                if (dragging) {
                    val newValue = (startValue - (event.getY(index) - downY) / dragRangePx).coerceIn(0f, 1f)
                    if (newValue != dragValue) {
                        dragValue = newValue
                        onValueChange?.invoke(newValue)
                        invalidate()
                    }
                    showBubble()
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == activePointerId) finishGesture(tapCandidate = false, event)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val tap = !dragging && !longPressed
                finishGesture(tapCandidate = tap, event)
                if (tap) performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                finishGesture(tapCandidate = false, event)
                return true
            }
        }
        return false
    }

    private fun finishGesture(tapCandidate: Boolean, event: MotionEvent) {
        removeCallbacks(longPressRunnable)
        activePointerId = MotionEvent.INVALID_POINTER_ID
        if (tapCandidate) {
            lastTapUpTime = event.eventTime
            lastTapX = event.x
            lastTapY = event.y
        }
        if (dragging) {
            dragging = false
            value = dragValue
            invalidate()
        }
        bubble.hide()
    }

    private fun showBubble() {
        val text = if (bandLabel.isEmpty()) valueLabel else "$bandLabel $valueLabel"
        if (text.isNotBlank()) bubble.show(text, ValueBubble.Placement.ABOVE)
    }

    override fun performClick(): Boolean = super.performClick()

    override fun onDetachedFromWindow() {
        removeCallbacks(longPressRunnable)
        bubble.hide()
        super.onDetachedFromWindow()
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.applySlider(if (isKill) 0f else value, interactive && isEnabled)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (interactive) {
            val next = sliderActionValue(action, arguments, if (isKill) 0f else value)
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
        const val VIEWBOX = 44f
        /** Vertical drag distance for the full 0..1 range. */
        const val DRAG_RANGE_DP = 160f
    }
}
