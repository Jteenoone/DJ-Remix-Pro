package com.example.djremixpro.feature.mixer.view

import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.djremixpro.R
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.DeckMode
import com.example.djremixpro.core.model.PadTab
import com.example.djremixpro.core.ui.ext.bindToggleSemantics
import com.example.djremixpro.core.ui.ext.fontOf
import com.example.djremixpro.core.ui.widget.DeckPalette
import com.example.djremixpro.databinding.IncludeDeckBinding
import com.example.djremixpro.databinding.ItemPadBinding
import com.example.djremixpro.feature.mixer.DeckUiState
import com.example.djremixpro.feature.mixer.MixerViewModel
import com.example.djremixpro.feature.mixer.PadState
import com.example.djremixpro.feature.mixer.PadStyle
import com.example.djremixpro.feature.mixer.PadUi
import java.util.Locale

/**
 * Binds one deck panel (include_deck.xml) to [DeckUiState] and forwards gestures to [MixerViewModel].
 * Deck B mirrors the panel with layoutDirection rtl (Mixer:76 row-reverse).
 */
internal class DeckController(
    private val b: IncludeDeckBinding,
    private val deck: DeckId,
    private val viewModel: MixerViewModel,
    private val drawables: MixerDrawables,
) {
    private val context = b.root.context
    private val deckInt = if (deck == DeckId.A) DeckPalette.DECK_A else DeckPalette.DECK_B
    private val palette = DeckPalette.of(context, deckInt)
    private val letter = context.getString(if (deck == DeckId.A) R.string.deck_letter_a else R.string.deck_letter_b)
    private val pads: List<ItemPadBinding> = listOf(b.pad1, b.pad2, b.pad3, b.pad4, b.pad5, b.pad6, b.pad7, b.pad8)
    private val tabs: List<Pair<PadTab, TextView>> = listOf(
        PadTab.CUE to b.tabCue, PadTab.LOOP to b.tabLoop, PadTab.FX to b.tabFx, PadTab.SAMPLER to b.tabSampler,
    )

    private var last: DeckUiState? = null
    private val lastPads = arrayOfNulls<PadUi>(8)
    private var hapticsEnabled = true

    init {
        if (deck == DeckId.B) b.root.layoutDirection = View.LAYOUT_DIRECTION_RTL
        b.textDeckLetter.text = letter
        b.textDeckLetter.setBackgroundResource(if (deck == DeckId.A) R.drawable.bg_deck_a_r6 else R.drawable.bg_deck_b_r6)
        b.jog.setDeck(deckInt)
        b.faderPitch.setDeck(deckInt)

        b.jog.onTouchChanged = { viewModel.onJogTouch(deck, it) }
        b.jog.onRotate = { viewModel.onJogRotate(deck, it) }
        b.jog.onTap = { viewModel.onJogTapped(deck) }
        b.faderPitch.onValueChange = { value ->
            // Fader value 1 = top = +range (Mixer:447: pitchPos = 50 − pitch/range·50).
            val range = last?.pitchRangePct ?: 8
            viewModel.onPitchChanged(deck, (value - 0.5f) * 2f * range)
        }
        b.buttonModeJog.setOnClickListener { viewModel.onModeChange(deck, DeckMode.JOG) }
        b.buttonModePad.setOnClickListener { viewModel.onModeChange(deck, DeckMode.PAD) }
        b.buttonBackToJog.setOnClickListener { viewModel.onModeChange(deck, DeckMode.JOG) }
        tabs.forEach { (tab, view) -> view.setOnClickListener { viewModel.onPadTabSelected(deck, tab) } }
        pads.forEachIndexed { i, pad ->
            pad.root.setOnClickListener { viewModel.onPadTapped(deck, i) }
            pad.root.setOnLongClickListener {
                if (hapticsEnabled) it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                viewModel.onPadLongPressed(deck, i)
                true
            }
        }
        b.buttonScratch.contentDescription = context.getString(R.string.mixer_scratch_cd)
        b.buttonScratch.setOnClickListener { viewModel.onScratchToggle(deck) }
        b.buttonPlay.setOnClickListener { viewModel.onPlayToggle(deck) }
        b.buttonCue.setOnClickListener { viewModel.onCue(deck) }
        b.buttonSync.setOnClickListener { viewModel.onSyncToggle(deck) }
    }

    /** The Sync button of this deck (coach mark target, CoachTarget.SYNC_B). */
    val syncButton: View get() = b.buttonSync

    fun bind(s: DeckUiState, hapticsEnabled: Boolean) {
        this.hapticsEnabled = hapticsEnabled
        b.faderPitch.hapticsEnabled = hapticsEnabled
        val prev = last
        last = s
        val track = s.track
        val loaded = track != null

        // Header (Mixer:76-86).
        b.textTitle.text = track?.title ?: context.getString(R.string.mixer_deck_empty_title)
        b.textTitle.setTextColor(if (loaded) drawables.text else drawables.muted)
        b.textArtist.text = track?.artist ?: context.getString(R.string.mixer_deck_empty_sub)
        b.textBpm.text = s.bpmLabel
        b.textBpm.setTextColor(if (s.isSync && loaded) palette.accent else drawables.text)
        b.textTime.text = s.remainingLabel

        // Centre: jog or pad mode.
        val jogMode = s.mode == DeckMode.JOG
        b.layoutJogMode.isVisible = jogMode
        b.layoutPadMode.isVisible = !jogMode
        b.buttonModeJog.isSelected = jogMode
        b.buttonModePad.isSelected = !jogMode

        b.jog.bind(
            loaded = loaded,
            initials = track?.initials.orEmpty(),
            playing = s.isPlaying,
            spinning = s.isSpinning,
            touching = s.isTouching,
            spinPeriodSec = s.spinPeriodSec,
        )
        b.jog.contentDescription = context.getString(
            if (loaded) R.string.mixer_jog_cd else R.string.mixer_jog_empty_cd, letter,
        )

        b.faderPitch.isEnabled = loaded
        b.faderPitch.bind(1f - s.pitchPosition, s.pitchLabel, s.isPitchShifted)
        b.faderPitch.contentDescription = context.getString(
            R.string.mixer_pitch_cd, letter, String.format(Locale.US, "%.1f", s.pitchPct),
        )
        b.textPitch.text = s.pitchLabel
        b.textPitch.setTextColor(if (s.isPitchShifted) drawables.text else drawables.muted)

        if (prev == null || prev.isScratch != s.isScratch) bindScratch(s.isScratch)
        if (prev == null || prev.padTab != s.padTab) bindTabs(s.padTab)
        bindPads(s.pads)

        // Transport (Mixer:158-167): opacity .35 and inert while empty.
        b.layoutTransport.alpha = if (loaded) 1f else 0.35f
        b.buttonPlay.isEnabled = loaded
        b.buttonCue.isEnabled = loaded
        b.buttonSync.isEnabled = loaded
        if (prev == null || prev.isPlaying != s.isPlaying) {
            b.buttonPlay.background = drawables.play(if (s.isPlaying) palette.accent else drawables.raised)
            b.imagePlay.setImageResource(if (s.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            b.imagePlay.imageTintList = ColorStateList.valueOf(if (s.isPlaying) drawables.booth else drawables.text)
        }
        b.buttonPlay.contentDescription = context.getString(
            if (s.isPlaying) R.string.mixer_pause_cd else R.string.mixer_play_cd, letter,
        )
        if (prev == null || prev.isSync != s.isSync) bindSync(s.isSync)
    }

    private fun bindScratch(on: Boolean) {
        b.buttonScratch.background = if (on) {
            drawables.toggle(android.graphics.Color.TRANSPARENT, palette.accent)
        } else {
            drawables.toggle(drawables.raised, drawables.raised)
        }
        b.textScratch.setTextColor(if (on) palette.accent else drawables.muted)
        b.ledScratch.backgroundTintList = ColorStateList.valueOf(if (on) palette.accent else drawables.line)
        b.buttonScratch.bindToggleSemantics(on)
    }

    private fun bindSync(on: Boolean) {
        b.buttonSync.background = if (on) {
            drawables.toggle(palette.accentAlpha(12), palette.accent)
        } else {
            drawables.toggle(drawables.raised, drawables.line)
        }
        b.textSync.setTextColor(if (on) palette.accent else drawables.muted)
        b.ledSync.backgroundTintList = ColorStateList.valueOf(if (on) palette.accent else drawables.line)
        b.buttonSync.bindToggleSemantics(on)
    }

    private fun bindTabs(selected: PadTab) {
        tabs.forEach { (tab, view) ->
            val on = tab == selected
            view.background = drawables.tab(on, palette.accent)
            view.setTextColor(if (on) palette.accent else drawables.muted)
            view.isSelected = on
        }
    }

    private fun bindPads(list: List<PadUi>) {
        pads.forEachIndexed { i, pad ->
            val ui = list.getOrNull(i)
            if (ui == lastPads[i] && pad.root.background != null) return@forEachIndexed
            lastPads[i] = ui
            if (ui == null) {
                pad.root.isVisible = list.isNotEmpty()
                pad.textLabel.text = ""
                pad.textSub.isVisible = false
                pad.root.background = drawables.pad(drawables.raised, drawables.raised)
                pad.root.isEnabled = false
                return@forEachIndexed
            }
            pad.root.isVisible = true
            pad.root.isEnabled = true
            val (fill, border, fg) = when (ui.state) {
                PadState.OFF -> Triple(drawables.raised, drawables.raised, drawables.muted)
                PadState.ON -> Triple(palette.accentAlpha(18), palette.accent, palette.accent)
                PadState.HELD -> Triple(palette.accent, palette.accent, drawables.booth)
                PadState.NEUTRAL_ON -> Triple(drawables.text12, drawables.text, drawables.text)
            }
            pad.root.background = drawables.pad(fill, border)
            pad.textLabel.text = ui.label.resolve(context)
            pad.textLabel.setTextColor(fg)
            when (ui.style) {
                PadStyle.NUMERIC -> styleLabel(pad.textLabel, R.font.chakra_petch_semibold, 18f)
                PadStyle.NUMERIC_SMALL -> styleLabel(pad.textLabel, R.font.chakra_petch_semibold, 14f)
                PadStyle.TEXT -> styleLabel(pad.textLabel, R.font.be_vietnam_pro_semibold, 12f)
            }
            val sub = ui.sub?.resolve(context)
            pad.textSub.isVisible = !sub.isNullOrEmpty()
            pad.textSub.text = sub
            pad.textSub.setTextColor(fg)
            pad.root.isSelected = ui.state != PadState.OFF
        }
    }

    private fun styleLabel(view: TextView, font: Int, sizeSp: Float) {
        view.typeface = context.fontOf(font)
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }
}
