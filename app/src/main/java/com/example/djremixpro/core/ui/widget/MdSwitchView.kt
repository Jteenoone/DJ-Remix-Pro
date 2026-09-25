package com.example.djremixpro.core.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.graphics.ColorUtils
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp

/**
 * Settings switch drawn to the design (App:207, App:286): 44x26, radius 13, border 1.5, knob 18 at left 2 / 20.
 * On = text fill + text border + booth knob; off = raised fill + line2 border + muted knob.
 * Display only: the whole settings row is the touch target and exposes the switch semantics.
 * The FX sheet reuses it with the deck colour as [onColor] (Mixer:226: track in deck colour, booth knob).
 */
class MdSwitchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val textColor = context.colorOf(R.color.md_text)

    /** Track + border colour when on (text by default). */
    var onColor: Int = textColor
        set(value) {
            field = value
            invalidate()
        }
    private val raisedColor = context.colorOf(R.color.md_raised)
    private val line2Color = context.colorOf(R.color.md_line2)
    private val boothColor = context.colorOf(R.color.md_booth)
    private val mutedColor = context.colorOf(R.color.md_muted)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var checked = false
    /** 0 = off, 1 = on; animated between states. */
    private var fraction = 0f
    private var animator: ValueAnimator? = null

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    val isChecked: Boolean get() = checked

    fun setChecked(checked: Boolean, animate: Boolean = true) {
        if (this.checked == checked && animator == null) {
            fraction = if (checked) 1f else 0f
            invalidate()
            return
        }
        val changed = this.checked != checked
        this.checked = checked
        if (changed) sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
        animator?.cancel()
        val target = if (checked) 1f else 0f
        if (!animate || !isLaidOut) {
            fraction = target
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(fraction, target).apply {
            duration = 150L
            addUpdateListener {
                fraction = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    animator = null
                }
            })
            start()
        }
    }

    /** Exposes role Switch + checked state (the host row may also carry them for TalkBack focus). */
    @Suppress("DEPRECATION") // setChecked(Boolean) is the only API below 36
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Switch"
        info.isCheckable = true
        info.isChecked = checked
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.className = "android.widget.Switch"
        event.isChecked = checked
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(44f).toInt(), widthMeasureSpec),
            resolveSize(dp(26f).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = dp(44f)
        val h = dp(26f)
        val left = (width - w) / 2f
        val top = (height - h) / 2f
        val border = dp(1.5f)
        rect.set(left, top, left + w, top + h)
        paint.style = Paint.Style.FILL
        paint.color = ColorUtils.blendARGB(raisedColor, onColor, fraction)
        canvas.drawRoundRect(rect, h / 2f, h / 2f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = border
        paint.color = ColorUtils.blendARGB(line2Color, onColor, fraction)
        rect.inset(border / 2f, border / 2f)
        canvas.drawRoundRect(rect, h / 2f, h / 2f, paint)
        // Knob 18 inside the 1.5 border: left 2 (off) → 20 (on), top 2.5.
        paint.style = Paint.Style.FILL
        paint.color = ColorUtils.blendARGB(mutedColor, boothColor, fraction)
        val knobLeft = left + border + dp(2f) + fraction * dp(18f)
        val r = dp(9f)
        canvas.drawCircle(knobLeft + r, top + border + dp(2.5f) + r, r, paint)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        fraction = if (checked) 1f else 0f
        super.onDetachedFromWindow()
    }
}
