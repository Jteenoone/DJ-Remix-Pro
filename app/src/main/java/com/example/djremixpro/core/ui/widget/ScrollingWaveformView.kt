package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp
import com.example.djremixpro.core.ui.ext.fontOf
import com.example.djremixpro.core.ui.ext.sp
import com.example.djremixpro.core.util.WaveformGenerator
import kotlin.math.ceil
import kotlin.math.floor

/**
 * One waveform lane of the mixer top bar (Mixer:41-54), designed for 18dp height:
 * beat grid every 32dp (1 beat), bars 3dp every 4dp from [WaveformGenerator.deck] (period 1216dp, repeated),
 * offset = positionSec·32·bpm/60 dp (bpm = the track's own tempo), interpolated between state updates at the
 * effective tempo so it scrolls at 32·effectiveBpm/60 dp/s like md-wave; loop region (playhead 48/128 inside) and the
 * deck label chip. Empty deck: "Deck X chưa có bài". The half-width dim layer and the playhead span both lanes
 * and are separate views in the layout.
 */
class ScrollingWaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var palette: DeckPalette
    private var deckLetter: String

    private val lineColor = context.colorOf(R.color.md_line)
    private val panelColor = context.colorOf(R.color.md_panel)
    private val mutedColor = context.colorOf(R.color.md_muted)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chipText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = context.fontOf(R.font.chakra_petch_semibold)
        textSize = sp(12f)
    }
    private val emptyText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = context.fontOf(R.font.be_vietnam_pro_regular)
        textSize = sp(12f)
        color = mutedColor
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    private val barStep = dp(4f)
    private val barWidth = dp(3f)
    private val beatStep = dp(32f)
    private val period = barStep * WaveformGenerator.DECK_BARS

    private var heights: FloatArray = WaveformGenerator.deck(7)
    private var seed = 7
    private var loaded = false
    private var playing = false
    private var bpm = 120f
    private var effectiveBpm = 120f
    /** Position shown on screen (s); follows [targetPosition] and advances on its own while playing. */
    private var displayPosition = 0f
    private var reportedPosition = Float.NaN
    private var loopBeats: Float? = null
    private var emptyLabel = ""
    private val showLabel: Boolean

    private var lastFrameNanos = 0L

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ScrollingWaveformView, defStyleAttr, 0)
        val deck = a.getInt(R.styleable.ScrollingWaveformView_mdDeck, DeckPalette.DECK_A)
        showLabel = a.getBoolean(R.styleable.ScrollingWaveformView_mdShowLabel, true)
        a.recycle()
        palette = DeckPalette.of(context, deck)
        deckLetter = context.getString(if (deck == DeckPalette.DECK_B) R.string.deck_letter_b else R.string.deck_letter_a)
        emptyLabel = context.getString(R.string.mixer_deck_empty_lane, deckLetter)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setDeck(deck: Int) {
        palette = DeckPalette.of(context, deck)
        deckLetter = context.getString(if (deck == DeckPalette.DECK_B) R.string.deck_letter_b else R.string.deck_letter_a)
        emptyLabel = context.getString(R.string.mixer_deck_empty_lane, deckLetter)
        invalidate()
    }

    /**
     * [bpm] is the track's own tempo, [effectiveBpm] the tempo after pitch (DeckUiState.waveBpm);
     * [positionSec] the play position (DeckUiState.positionSec); [loopBeats] null = no loop region.
     */
    fun bind(
        loaded: Boolean,
        seed: Int,
        bpm: Float,
        effectiveBpm: Float,
        playing: Boolean,
        loopBeats: Float?,
        positionSec: Float,
    ) {
        if (seed != this.seed) {
            this.seed = seed
            heights = WaveformGenerator.deck(seed)
        }
        val wasAnimating = isAnimating()
        this.loaded = loaded
        this.playing = playing
        this.bpm = if (bpm > 0f) bpm else 120f
        this.effectiveBpm = if (effectiveBpm > 0f) effectiveBpm else this.bpm
        this.loopBeats = loopBeats
        // Re-emissions for other controls repeat the last position: they carry no timing information.
        // A new position far from the interpolated one is a jump (seek, cue, loop, jog): snap to it.
        val newReport = positionSec != reportedPosition
        reportedPosition = positionSec
        if (!playing || !wasAnimating ||
            (newReport && kotlin.math.abs(positionSec - displayPosition) > SNAP_THRESHOLD_SEC)
        ) {
            displayPosition = positionSec
        }
        if (!wasAnimating) lastFrameNanos = 0L
        invalidate()
    }

    private fun isAnimating() = loaded && playing

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val center = w / 2f
        if (loaded) {
            advance()
            val scaleY = h / dp(18f)
            val scrollPx = (displayPosition * dp(32f) * bpm / 60f) % period
            val stripX0 = center - dp(320f) - (if (scrollPx < 0f) scrollPx + period else scrollPx)
            // Beat grid: 1dp lines at stripX0 + 32n + 0.5.
            paint.color = lineColor
            paint.style = Paint.Style.FILL
            var n = ceil((-stripX0) / beatStep).toInt() - 1
            while (true) {
                val x = stripX0 + n * beatStep + dp(0.5f)
                if (x > w) break
                if (x >= -dp(1f)) canvas.drawRect(x - dp(0.5f), 0f, x + dp(0.5f), h, paint)
                n++
            }
            // Bars.
            paint.color = palette.accent
            val midY = h / 2f
            var k = floor((-stripX0) / period).toInt()
            while (true) {
                val base = stripX0 + k * period
                if (base > w) break
                val iStart = ceil((-base - barWidth) / barStep).toInt().coerceAtLeast(0)
                val iEnd = floor((w - base) / barStep).toInt().coerceAtMost(WaveformGenerator.DECK_BARS - 1)
                for (i in iStart..iEnd) {
                    val x = base + i * barStep
                    val half = heights[i] * dp(1f) * scaleY
                    canvas.drawRect(x, midY - half, x + barWidth, midY + half, paint)
                }
                k++
            }
            // Loop region.
            val beats = loopBeats
            if (beats != null && beats > 0f) {
                val loopW = beats * beatStep
                val left = center - loopW * LOOP_PLAYHEAD_FRACTION
                paint.color = palette.accentAlpha(20)
                canvas.drawRect(left, 0f, left + loopW, h, paint)
                paint.color = palette.accent
                val b = dp(1.5f)
                canvas.drawRect(left, 0f, left + b, h, paint)
                canvas.drawRect(left + loopW - b, 0f, left + loopW, h, paint)
            }
        } else {
            val baseline = h / 2f - (emptyText.descent() + emptyText.ascent()) / 2f
            canvas.drawText(emptyLabel, center, baseline, emptyText)
        }
        if (showLabel) drawChip(canvas)
        if (isAnimating()) postInvalidateOnAnimation()
    }

    private fun advance() {
        val now = System.nanoTime()
        if (isAnimating()) {
            if (lastFrameNanos != 0L) {
                val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.1f)
                // Position advances at the playback rate (effective / own tempo).
                displayPosition += dt * effectiveBpm / bpm
            }
            lastFrameNanos = now
        } else {
            lastFrameNanos = 0L
        }
    }

    private fun drawChip(canvas: Canvas) {
        // left 4, top 1, height 16, padding 0 5, radius 4, panel background, Chakra 600 12 in deck colour.
        val textW = chipText.measureText(deckLetter)
        rect.set(dp(4f), dp(1f), dp(4f) + textW + dp(10f), dp(17f))
        paint.color = panelColor
        canvas.drawRoundRect(rect, dp(4f), dp(4f), paint)
        chipText.color = palette.accent
        val baseline = rect.centerY() - (chipText.descent() + chipText.ascent()) / 2f
        canvas.drawText(deckLetter, rect.left + dp(5f), baseline, chipText)
    }

    private companion object {
        /** Playhead sits 48dp into the 128dp loop (Mixer:48). */
        const val LOOP_PLAYHEAD_FRACTION = 48f / 128f
        /** Larger gaps between the shown and the reported position are jumps, not drift. */
        const val SNAP_THRESHOLD_SEC = 0.35f
    }
}
