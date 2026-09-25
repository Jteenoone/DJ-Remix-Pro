package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.dp
import com.example.djremixpro.core.ui.ext.fontOf
import kotlin.math.min

/**
 * Static illustration disc (Home "Sẵn sàng mix?" App:33-34, onboarding 1/3 MixDeck:127, 2/3 MixDeck:200).
 * One disc per view: grooves over the whole circle, 2dp rim, centre label with optional letter. Never animates.
 */
class VinylArtView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val palette: DeckPalette
    private val rimThemed: Boolean
    private val labelSize: Float
    private var labelText: String

    private val vinyl = VinylPainter(context)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = context.fontOf(R.font.chakra_petch_semibold)
        textAlign = Paint.Align.CENTER
    }
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.VinylArtView, defStyleAttr, 0)
        palette = DeckPalette.of(context, a.getInt(R.styleable.VinylArtView_mdDeck, DeckPalette.DECK_A))
        rimThemed = a.getBoolean(R.styleable.VinylArtView_mdRimThemed, true)
        labelSize = a.getDimension(R.styleable.VinylArtView_mdLabelSize, dp(26f))
        labelText = a.getString(R.styleable.VinylArtView_mdLabelText).orEmpty()
        textPaint.textSize = a.getDimension(R.styleable.VinylArtView_mdLabelTextSize, dp(12f))
        a.recycle()
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setLabelText(text: String) {
        labelText = text
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f
        cy = h / 2f
        radius = min(w, h) / 2f
        vinyl.update(cx, cy, dp(3f))
    }

    override fun onDraw(canvas: Canvas) {
        val stroke = dp(2f)
        canvas.drawCircle(cx, cy, radius, vinyl.paint)
        strokePaint.strokeWidth = stroke
        strokePaint.color = if (rimThemed) palette.accent else palette.vinyl
        canvas.drawCircle(cx, cy, radius - stroke / 2f, strokePaint)

        val labelR = labelSize / 2f
        fillPaint.color = palette.labelBg
        canvas.drawCircle(cx, cy, labelR, fillPaint)
        strokePaint.color = palette.vinyl
        canvas.drawCircle(cx, cy, labelR - stroke / 2f, strokePaint)
        if (labelText.isNotEmpty()) {
            textPaint.color = palette.vinyl
            canvas.drawText(labelText, cx, cy - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
        }
    }
}
