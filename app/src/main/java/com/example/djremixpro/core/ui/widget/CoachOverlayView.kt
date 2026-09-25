package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp
import kotlin.math.sqrt

/**
 * Coach mark overlay (Mixer:297-317). Draws the scrim (md_scrim_coach) with a rounded (16dp) hole around the
 * target view plus a 2dp text-coloured ring, and places its first child (the bubble from view_coach.xml) above the
 * hole with a 12dp diamond arrow pointing at the target (below the hole when there is no room above).
 * Touches inside the hole fall through to the views underneath; touches elsewhere on the scrim are consumed.
 */
class CoachOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.colorOf(R.color.md_scrim_coach) }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = context.colorOf(R.color.md_text)
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.colorOf(R.color.md_text) }

    private val scrimPath = Path().apply { fillType = Path.FillType.EVEN_ODD }
    private val arrowPath = Path()
    private val hole = RectF()
    private val ringRect = RectF()
    private val holeRadius = dp(16f)
    private val holeInset = dp(6f)
    private val bubbleGap = dp(18f)
    private val edgeMargin = dp(8f)
    /** Arrow centre from the bubble's left edge in the design (left 218 + 6). */
    private val arrowDesignOffset = dp(224f)

    private var target: View? = null
    private var hasHole = false
    private val targetLoc = IntArray(2)
    private val selfLoc = IntArray(2)
    private var lastL = Int.MIN_VALUE
    private var lastT = Int.MIN_VALUE
    private var lastW = -1
    private var lastH = -1

    private val preDraw = ViewTreeObserver.OnPreDrawListener {
        if (updateHole()) {
            requestLayout()
            invalidate()
        }
        true
    }

    init {
        setWillNotDraw(false)
        isClickable = false
    }

    /** View to spotlight (e.g. Sync of Deck B for CoachTarget.SYNC_B); null = plain scrim. */
    fun setTarget(view: View?) {
        if (view === target) return
        target = view
        lastW = -1
        updateHole()
        requestLayout()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnPreDrawListener(preDraw)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnPreDrawListener(preDraw)
        super.onDetachedFromWindow()
    }

    /** @return true when the hole moved or changed size. */
    private fun updateHole(): Boolean {
        val t = target
        if (t == null || !t.isAttachedToWindow || t.width == 0) {
            val changed = hasHole
            hasHole = false
            if (changed) rebuildPath()
            return changed
        }
        t.getLocationInWindow(targetLoc)
        getLocationInWindow(selfLoc)
        val l = targetLoc[0] - selfLoc[0]
        val top = targetLoc[1] - selfLoc[1]
        if (hasHole && l == lastL && top == lastT && t.width == lastW && t.height == lastH) return false
        lastL = l
        lastT = top
        lastW = t.width
        lastH = t.height
        hasHole = true
        hole.set(l - holeInset, top - holeInset, l + t.width + holeInset, top + t.height + holeInset)
        rebuildPath()
        return true
    }

    private fun rebuildPath() {
        scrimPath.reset()
        scrimPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        if (hasHole) scrimPath.addRoundRect(hole, holeRadius, holeRadius, Path.Direction.CW)
        ringRect.set(hole)
        ringRect.inset(-ringPaint.strokeWidth / 2f, -ringPaint.strokeWidth / 2f)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildPath()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateHole()
        val bubble = getChildAt(0) ?: return
        arrowPath.reset()
        if (!hasHole || bubble.visibility == GONE) return
        val bw = bubble.measuredWidth
        val bh = bubble.measuredHeight
        val bl = (hole.centerX() - arrowDesignOffset).coerceIn(edgeMargin, (width - bw - edgeMargin).coerceAtLeast(edgeMargin))
        val above = hole.top - bubbleGap - bh >= edgeMargin
        val bt = if (above) hole.top - bubbleGap - bh else hole.bottom + bubbleGap
        bubble.layout(bl.toInt(), bt.toInt(), bl.toInt() + bw, bt.toInt() + bh)
        // Diamond 12x12 rotated 45°, centred on the bubble edge facing the hole.
        val half = dp(6f) * sqrt(2f)
        val ax = hole.centerX().coerceIn(bl + holeRadius, bl + bw - holeRadius)
        val ay = if (above) bt + bh else bt
        arrowPath.moveTo(ax, ay - half)
        arrowPath.lineTo(ax + half, ay)
        arrowPath.lineTo(ax, ay + half)
        arrowPath.lineTo(ax - half, ay)
        arrowPath.close()
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.drawPath(scrimPath, scrimPaint)
        if (hasHole) canvas.drawRoundRect(ringRect, holeRadius + ringPaint.strokeWidth / 2f, holeRadius + ringPaint.strokeWidth / 2f, ringPaint)
        canvas.drawPath(arrowPath, arrowPaint)
        super.dispatchDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Let the spotlighted control receive the touch (pointer-events: none on the spotlight).
        if (event.actionMasked == MotionEvent.ACTION_DOWN && hasHole && hole.contains(event.x, event.y)) return false
        // The scrim swallows touches outside the spotlight; a tap counts as a (no-op) click for accessibility.
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
