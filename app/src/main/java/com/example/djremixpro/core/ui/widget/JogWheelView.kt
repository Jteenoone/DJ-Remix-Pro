package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.fontOf
import com.example.djremixpro.core.ui.ext.isReducedMotion
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Jog wheel of a deck (Mixer:102-121). Geometry is in design dp for a 152dp disc and scales with the view:
 * the view is expected to be 160x160 so the 4dp touch ring (box-shadow) fits around the disc.
 *
 * States: empty (dashed rim, "+" and "Chạm để chọn bài"), loaded (solid rim in deck colour, 36 dots),
 * playing (dots in deck colour), spinning (rotation at [spinPeriodSec] per turn, paused under reduced motion),
 * touching (ring 4dp deck 33%). While a finger is down the platter follows the finger.
 */
class JogWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** Finger down (true) / up (false). Only fired when a track is loaded. */
    var onTouchChanged: ((Boolean) -> Unit)? = null

    /** Clockwise rotation of the finger around the centre since the previous move, in degrees. */
    var onRotate: ((Float) -> Unit)? = null

    /** Tap without drag (also the accessibility click). Fired for empty and loaded decks. */
    var onTap: (() -> Unit)? = null

    private var palette: DeckPalette
    private val density = resources.displayMetrics.density
    private val vinyl = VinylPainter(context)

    private val jogBg = context.colorOf(R.color.md_fixed_jog_bg)
    private val lineColor = context.colorOf(R.color.md_line)
    private val textColor = context.colorOf(R.color.md_text)
    private val boothColor = context.colorOf(R.color.md_booth)
    private val mutedColor = context.colorOf(R.color.md_muted)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = context.fontOf(R.font.chakra_petch_semibold)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.04f
    }
    private val emptyTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = context.fontOf(R.font.be_vietnam_pro_regular)
        color = mutedColor
    }
    private var emptyLayout: StaticLayout? = null
    private val emptyText = context.getString(R.string.mixer_jog_empty)

    private var dashEffect: DashPathEffect? = null
    private val tickRect = RectF()
    private var dotPoints = FloatArray(DOT_COUNT * 2)

    // Geometry in px, computed in onSizeChanged.
    private var cx = 0f
    private var cy = 0f
    private var unit = density // px per design dp
    private var outerR = 0f

    private var loaded = false
    private var playing = false
    private var spinning = false
    private var touchingState = false
    private var spinPeriodSec = 1.8f
    private var initials = ""

    private var angleDeg = 0f
    private var lastFrameNanos = 0L

    // Touch tracking (single pointer, other pointers ignored).
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var fingerDown = false
    private var lastFingerAngle = 0f
    private var downX = 0f
    private var downY = 0f
    private var moved = false

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.JogWheelView, defStyleAttr, 0)
        palette = DeckPalette.of(context, a.getInt(R.styleable.JogWheelView_mdDeck, DeckPalette.DECK_A))
        a.recycle()
        isClickable = true
        isFocusable = true
    }

    fun setDeck(deck: Int) {
        palette = DeckPalette.of(context, deck)
        invalidate()
    }

    fun bind(
        loaded: Boolean,
        initials: String,
        playing: Boolean,
        spinning: Boolean,
        touching: Boolean,
        spinPeriodSec: Float,
    ) {
        val wasSpinning = isAnimating()
        this.loaded = loaded
        this.initials = initials
        this.playing = playing
        this.spinning = spinning
        this.touchingState = touching
        this.spinPeriodSec = if (spinPeriodSec > 0f) spinPeriodSec else 1.8f
        if (!wasSpinning && isAnimating()) lastFrameNanos = 0L
        invalidate()
    }

    private fun isAnimating() = loaded && spinning && !fingerDown && !isReducedMotion()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val content = min(w - paddingLeft - paddingRight, h - paddingTop - paddingBottom).toFloat()
        cx = paddingLeft + (w - paddingLeft - paddingRight) / 2f
        cy = paddingTop + (h - paddingTop - paddingBottom) / 2f
        // Disc 152 + ring 4 on each side = 160 design dp.
        unit = content / (DISC_DP + 2 * RING_DP)
        outerR = DISC_DP / 2f * unit
        vinyl.update(cx, cy, 3f * unit)
        dashEffect = DashPathEffect(floatArrayOf(6f * unit, 5f * unit), 0f)
        labelTextPaint.textSize = 14f * unit * resources.configuration.fontScale.coerceAtMost(1.3f)
        emptyTextPaint.textSize = 12f * unit * resources.configuration.fontScale.coerceAtMost(1.3f)
        emptyLayout = StaticLayout.Builder
            .obtain(emptyText, 0, emptyText.length, emptyTextPaint, (88f * unit).toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(16f * unit - emptyTextPaint.fontSpacing, 1f)
            .setIncludePad(false)
            .build()
        // 36 dots every 10°, radius 67 (translateY(-67px) then rotate).
        dotPoints = FloatArray(DOT_COUNT * 2)
        for (i in 0 until DOT_COUNT) {
            val rad = Math.toRadians(i * 10.0)
            dotPoints[i * 2] = cx + (67f * unit * sin(rad)).toFloat()
            dotPoints[i * 2 + 1] = cy - (67f * unit * cos(rad)).toFloat()
        }
        // Tick 3x18 at 14dp below the inner edge of the 2dp border (padding box radius 74).
        tickRect.set(cx - 1.5f * unit, cy - 60f * unit, cx + 1.5f * unit, cy - 42f * unit)
    }

    override fun onDraw(canvas: Canvas) {
        advanceSpin()
        val ringVisible = loaded && (touchingState || fingerDown)
        if (ringVisible) {
            strokePaint.pathEffect = null
            strokePaint.strokeWidth = RING_DP * unit
            strokePaint.color = palette.accentAlpha(33)
            canvas.drawCircle(cx, cy, outerR + RING_DP / 2f * unit, strokePaint)
        }
        // Disc background (#211B2B, fixed).
        fillPaint.shader = null
        fillPaint.color = jogBg
        canvas.drawCircle(cx, cy, outerR, fillPaint)

        if (loaded) {
            drawPlatter(canvas)
        } else {
            drawEmpty(canvas)
        }

        // Rim: 2dp, solid deck colour when loaded, dashed line colour when empty.
        strokePaint.strokeWidth = 2f * unit
        strokePaint.color = if (loaded) palette.accent else lineColor
        strokePaint.pathEffect = if (loaded) null else dashEffect
        canvas.drawCircle(cx, cy, outerR - unit, strokePaint)
        strokePaint.pathEffect = null

        if (isAnimating()) postInvalidateOnAnimation()
    }

    private fun advanceSpin() {
        val now = System.nanoTime()
        if (isAnimating()) {
            if (lastFrameNanos != 0L) {
                val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.1f)
                angleDeg = (angleDeg + 360f * dt / spinPeriodSec) % 360f
            }
            lastFrameNanos = now
        } else {
            lastFrameNanos = 0L
        }
    }

    private fun drawPlatter(canvas: Canvas) {
        // Grooves: inset 12 from the padding box → radius 62. Concentric, so rotation-invariant.
        canvas.drawCircle(cx, cy, 62f * unit, vinyl.paint)

        val save = canvas.save()
        canvas.rotate(angleDeg, cx, cy)
        dotPaint.strokeWidth = 4f * unit
        dotPaint.color = if (playing) palette.accent else lineColor
        canvas.drawPoints(dotPoints, dotPaint)

        // Centre label 60, border 2 in vinyl colour, initials Chakra 600 14.
        fillPaint.color = palette.labelBg
        canvas.drawCircle(cx, cy, 30f * unit, fillPaint)
        strokePaint.strokeWidth = 2f * unit
        strokePaint.color = palette.vinyl
        canvas.drawCircle(cx, cy, 29f * unit, strokePaint)
        if (initials.isNotEmpty()) {
            labelTextPaint.color = palette.vinyl
            val baseline = cy - (labelTextPaint.descent() + labelTextPaint.ascent()) / 2f
            canvas.drawText(initials, cx, baseline, labelTextPaint)
        }

        fillPaint.color = textColor
        canvas.drawRoundRect(tickRect, 2f * unit, 2f * unit, fillPaint)
        fillPaint.color = boothColor
        canvas.drawCircle(cx, cy, 3f * unit, fillPaint)
        canvas.restoreToCount(save)
    }

    private fun drawEmpty(canvas: Canvas) {
        val layout = emptyLayout ?: return
        val icon = 24f * unit
        val gap = 6f * unit
        val total = icon + gap + layout.height
        val top = cy - total / 2f
        // "+" glyph: M12 5v14 M5 12h14 in a 24 box, stroke 2, round caps.
        dotPaint.strokeWidth = 2f * unit
        dotPaint.color = mutedColor
        val iconCy = top + icon / 2f
        canvas.drawLine(cx, iconCy - 7f * unit, cx, iconCy + 7f * unit, dotPaint)
        canvas.drawLine(cx - 7f * unit, iconCy, cx + 7f * unit, iconCy, dotPaint)
        val save = canvas.save()
        canvas.translate(cx - layout.width / 2f, top + icon + gap)
        layout.draw(canvas)
        canvas.restoreToCount(save)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (hypot(event.x - cx, event.y - cy) > outerR + RING_DP * unit) return false
                activePointerId = event.getPointerId(0)
                downX = event.x
                downY = event.y
                moved = false
                lastFingerAngle = angleOf(event.x, event.y)
                parent?.requestDisallowInterceptTouchEvent(true)
                isPressed = true
                if (loaded) {
                    fingerDown = true
                    onTouchChanged?.invoke(true)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(activePointerId)
                if (index < 0) return true
                val x = event.getX(index)
                val y = event.getY(index)
                if (!moved && hypot(x - downX, y - downY) > touchSlop) moved = true
                if (fingerDown) {
                    val a = angleOf(x, y)
                    var delta = a - lastFingerAngle
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f
                    lastFingerAngle = a
                    // Ignore jitter right at the centre where the angle is unstable.
                    if (hypot(x - cx, y - cy) > 12f * unit && delta != 0f) {
                        angleDeg = (angleDeg + delta) % 360f
                        onRotate?.invoke(delta)
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == activePointerId) endTouch()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val tap = !moved
                endTouch()
                if (tap) performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                endTouch()
                return true
            }
        }
        return false
    }

    private fun endTouch() {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        isPressed = false
        if (fingerDown) {
            fingerDown = false
            onTouchChanged?.invoke(false)
        }
        lastFrameNanos = 0L
        invalidate()
    }

    override fun performClick(): Boolean {
        super.performClick()
        onTap?.invoke()
        return true
    }

    private fun angleOf(x: Float, y: Float): Float =
        Math.toDegrees(atan2((y - cy).toDouble(), (x - cx).toDouble())).toFloat()

    override fun getAccessibilityClassName(): CharSequence = "android.widget.Button"

    private companion object {
        const val DISC_DP = 152f
        const val RING_DP = 4f
        const val DOT_COUNT = 36
    }
}
