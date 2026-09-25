package com.example.djremixpro.feature.mixer

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.djremixpro.R
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.EqBand
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.ui.ToastHost
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.ext.bindToggleSemantics
import com.example.djremixpro.core.ui.ext.collectWhenStarted
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.fontOf
import com.example.djremixpro.core.ui.ext.isReducedMotion
import com.example.djremixpro.core.ui.widget.DeckPalette
import com.example.djremixpro.core.ui.widget.KnobView
import com.example.djremixpro.databinding.ActivityMixerBinding
import com.example.djremixpro.feature.mixer.view.DeckController
import com.example.djremixpro.feature.mixer.view.LibraryPanelAdapter
import com.example.djremixpro.feature.mixer.view.MixerDrawables
import kotlin.math.roundToInt

/**
 * S10 Mixer (D-11): landscape, immersive, always ltr. Renders [MixerUiState] and forwards every gesture to
 * [MixerViewModel]; overlays (library panel, FX sheet, save dialog, coach) are views driven by the state.
 */
class MixerActivity : AppCompatActivity(), ToastHost {

    private lateinit var binding: ActivityMixerBinding
    private val viewModel: MixerViewModel by viewModels { MixerViewModel.Factory }

    private lateinit var drawables: MixerDrawables
    private lateinit var deckA: DeckController
    private lateinit var deckB: DeckController
    private val libraryAdapter = LibraryPanelAdapter { trackId, deck -> viewModel.onLibraryLoad(trackId, deck) }

    private var state: MixerUiState? = null
    private var recBlinkOn = true
    private val recBlink = object : Runnable {
        override fun run() {
            recBlinkOn = !recBlinkOn
            binding.viewRecDot.alpha = if (recBlinkOn) 1f else 0.25f
            binding.viewRecDot.postDelayed(this, 500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMixerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow()

        drawables = MixerDrawables(this)
        deckA = DeckController(binding.deckA, DeckId.A, viewModel, drawables)
        deckB = DeckController(binding.deckB, DeckId.B, viewModel, drawables)
        setupTopBar()
        setupChannels()
        setupOverlays()
        setupAdaptiveWidths()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val s = state
                when {
                    s?.libraryPanel != null -> viewModel.onLibraryClose()
                    s?.fxSheet != null -> viewModel.onFxClose()
                    s?.coach != null -> viewModel.onCoachSkip()
                    // The save dialog needs an explicit choice (Bỏ / Lưu bản mix): back does nothing.
                    s?.saveDialog != null -> Unit
                    else -> viewModel.onBack()
                }
            }
        })

        collectWhenStarted(viewModel.uiState, ::render)
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is MixerEvent.ShowToast -> showToast(event.message)
                MixerEvent.Close -> finish()
            }
        }
    }

    override fun showToast(message: ToastMessage) {
        binding.toast.show(message)
    }

    // region Setup

    /**
     * Immersive landscape: hide system bars. Only the mixer content is inset from display cutouts;
     * the root and its overlays (scrims, coach) still cover the whole window.
     */
    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            // Symmetric horizontally so the 800-dp layout stays centred (V15).
            val side = maxOf(cutout.left, cutout.right)
            binding.content.updatePadding(left = side, top = cutout.top, right = side, bottom = cutout.bottom)
            insets
        }
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun setupTopBar() {
        binding.buttonBack.setOnClickListener { viewModel.onBack() }
        binding.buttonQuickSettings.setOnClickListener { viewModel.onQuickSettings() }
        binding.buttonRec.setOnClickListener { viewModel.onRecToggle() }
        binding.frameWaveform.clipToOutline = true
        binding.overviewA.onSeek = { viewModel.onOverviewSeek(DeckId.A, it) }
        binding.overviewB.onSeek = { viewModel.onOverviewSeek(DeckId.B, it) }
        binding.overviewA.contentDescription = getString(R.string.mixer_overview_cd, getString(R.string.deck_letter_a))
        binding.overviewB.contentDescription = getString(R.string.mixer_overview_cd, getString(R.string.deck_letter_b))
    }

    private fun knobsA(): List<Pair<EqBand, KnobView>> = with(binding.center) {
        listOf(EqBand.HIGH to knobAHigh, EqBand.MID to knobAMid, EqBand.LOW to knobALow, EqBand.FILTER to knobAFilter)
    }

    private fun knobsB(): List<Pair<EqBand, KnobView>> = with(binding.center) {
        listOf(EqBand.HIGH to knobBHigh, EqBand.MID to knobBMid, EqBand.LOW to knobBLow, EqBand.FILTER to knobBFilter)
    }

    private fun setupChannels() {
        fun wire(deck: DeckId, knobs: List<Pair<EqBand, KnobView>>) = knobs.forEach { (band, knob) ->
            knob.onValueChange = { viewModel.onEqChanged(deck, band, it) }
            knob.onReset = { viewModel.onEqReset(deck, band) }
            knob.onKill = { viewModel.onEqKill(deck, band) }
        }
        wire(DeckId.A, knobsA())
        wire(DeckId.B, knobsB())
        val c = binding.center
        c.faderVolumeA.onValueChange = { viewModel.onVolumeChanged(DeckId.A, it) }
        c.faderVolumeB.onValueChange = { viewModel.onVolumeChanged(DeckId.B, it) }
        c.crossfader.onValueChange = { viewModel.onCrossfaderChanged(it) }
        c.crossfader.onReset = { viewModel.onCrossfaderReset() }
    }

    private fun setupOverlays() {
        val lib = binding.libraryPanel
        lib.scrim.setOnClickListener { viewModel.onLibraryClose() }
        lib.buttonClose.setOnClickListener { viewModel.onLibraryClose() }
        lib.recyclerRows.layoutManager = LinearLayoutManager(this)
        lib.recyclerRows.adapter = libraryAdapter
        lib.textSort.text = getString(R.string.library_sort, getString(R.string.sort_bpm))

        val fx = binding.fxSheet
        fx.scrim.setOnClickListener { viewModel.onFxClose() }
        fx.buttonClose.setOnClickListener { viewModel.onFxClose() }
        fx.switchFx.setOnClickListener { viewModel.onFxToggle() }
        fx.buttonReset.setOnClickListener { viewModel.onFxReset() }
        fx.xyPad.onChange = { x, y -> viewModel.onFxChanged(x, y) }
        fx.knobWetDry.interactive = false

        val save = binding.saveDialog
        save.editName.doAfterTextChanged { text ->
            val s = state?.saveDialog ?: return@doAfterTextChanged
            val value = text?.toString().orEmpty()
            if (value != s.name) viewModel.onSaveNameChanged(value)
        }
        save.buttonMp3.setOnClickListener { viewModel.onSaveFormatSelected(RecordFormat.MP3) }
        save.buttonWav.setOnClickListener { viewModel.onSaveFormatSelected(RecordFormat.WAV) }
        save.buttonDiscard.setOnClickListener { viewModel.onSaveDiscarded() }
        save.buttonSave.setOnClickListener { viewModel.onSaveConfirmed() }

        binding.coach.buttonSkip.setOnClickListener { viewModel.onCoachSkip() }
        binding.coach.buttonNext.setOnClickListener { viewModel.onCoachNext() }
    }

    /**
     * Decks are 280 wide at 800dp; on narrower screens they shrink so the mixer column keeps at least
     * [MIN_CENTER_DP] (the 7-column grid needs ~188 + padding). The jog wheel scales with its box.
     */
    private fun setupAdaptiveWidths() {
        binding.content.addOnLayoutChangeListener { v, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left == oldRight - oldLeft) return@addOnLayoutChangeListener
            val available = (right - left) - dp(4f * 4) // outer insets 4 + 4, gaps 4 + 4
            val deckWidth = minOf(dp(280f), (available - dp(MIN_CENTER_DP)) / 2f).toInt().coerceAtLeast(dp(220f).toInt())
            var changed = false
            listOf(binding.deckA.root, binding.deckB.root).forEach { deck ->
                if (deck.layoutParams.width != deckWidth) {
                    deck.layoutParams.width = deckWidth
                    changed = true
                }
            }
            // Library panel: 480 wide, but never more than 60 % of the screen.
            val panelWidth = minOf(dp(480f), (right - left) * 0.6f).toInt()
            if (binding.libraryPanel.panel.layoutParams.width != panelWidth) {
                binding.libraryPanel.panel.layoutParams.width = panelWidth
                changed = true
            }
            // Layout params changed inside a layout pass: apply them on the next frame.
            if (changed) v.post { binding.root.requestLayout() }
        }
    }

    // endregion

    // region Render

    private fun render(s: MixerUiState) {
        val prev = state
        state = s
        if (prev?.keepScreenOn != s.keepScreenOn) {
            if (s.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        deckA.bind(s.deckA, s.hapticsEnabled)
        deckB.bind(s.deckB, s.hapticsEnabled)
        renderTopBar(s, prev)
        renderChannels(s.channels, s.hapticsEnabled)
        renderLibraryPanel(s.libraryPanel)
        renderFxSheet(s.fxSheet)
        renderSaveDialog(s.saveDialog)
        renderCoach(s.coach)
    }

    private fun renderTopBar(s: MixerUiState, prev: MixerUiState?) {
        val rec = s.rec
        binding.textRec.text = rec.label
        if (prev?.rec?.isRecording != rec.isRecording) {
            binding.buttonRec.setBackgroundResource(if (rec.isRecording) R.drawable.bg_rec_active else R.drawable.bg_rec_idle)
            binding.viewRecDot.setBackgroundResource(
                if (rec.isRecording) R.drawable.shape_rec_dot_active else R.drawable.shape_rec_dot_idle,
            )
            binding.textRec.setTextColor(colorOf(if (rec.isRecording) R.color.md_rec else R.color.md_text))
            binding.textRecording.isVisible = rec.isRecording
            binding.buttonRec.contentDescription = getString(
                if (rec.isRecording) R.string.mixer_rec_stop_cd else R.string.mixer_rec_start_cd,
            )
            binding.viewRecDot.removeCallbacks(recBlink)
            binding.viewRecDot.alpha = 1f
            recBlinkOn = true
            // md-blink 1s steps(1): 1 for the first half, .25 for the second (Mixer:17).
            if (rec.isRecording && !isReducedMotion()) binding.viewRecDot.postDelayed(recBlink, 500L)
        }
        bindWave(s.deckA, binding.waveA, binding.overviewA)
        bindWave(s.deckB, binding.waveB, binding.overviewB)
    }

    private fun bindWave(
        d: DeckUiState,
        wave: com.example.djremixpro.core.ui.widget.ScrollingWaveformView,
        overview: com.example.djremixpro.core.ui.widget.OverviewBarView,
    ) {
        val loaded = d.track != null
        wave.bind(
            loaded = loaded,
            seed = d.waveSeed,
            bpm = d.track?.bpm ?: d.waveBpm,
            effectiveBpm = d.waveBpm,
            playing = d.isPlaying,
            loopBeats = if (d.isPlaying) d.loopBeats else null,
            positionSec = d.positionSec,
        )
        overview.isEnabled = loaded
        overview.setProgress(if (loaded) d.progress else 0f)
    }

    private fun renderChannels(c: ChannelsUiState, haptics: Boolean) {
        val center = binding.center
        bindKnobs(knobsA(), c.eqA, getString(R.string.deck_letter_a), haptics)
        bindKnobs(knobsB(), c.eqB, getString(R.string.deck_letter_b), haptics)
        center.faderVolumeA.hapticsEnabled = haptics
        center.faderVolumeB.hapticsEnabled = haptics
        center.faderVolumeA.bind(c.volumeA)
        center.faderVolumeB.bind(c.volumeB)
        center.faderVolumeA.contentDescription =
            getString(R.string.mixer_volume_cd, getString(R.string.deck_letter_a), (c.volumeA * 100).roundToInt())
        center.faderVolumeB.contentDescription =
            getString(R.string.mixer_volume_cd, getString(R.string.deck_letter_b), (c.volumeB * 100).roundToInt())
        center.vuA.setActive(c.vuActiveA)
        center.vuB.setActive(c.vuActiveB)
        center.crossfader.hapticsEnabled = haptics
        center.crossfader.setValue(c.crossfader)
        center.crossfader.contentDescription = getString(R.string.mixer_crossfader_cd, (c.crossfader * 100).roundToInt())
    }

    private fun bindKnobs(knobs: List<Pair<EqBand, KnobView>>, eq: List<KnobUi>, letter: String, haptics: Boolean) {
        knobs.forEach { (band, knob) ->
            val ui = eq.firstOrNull { it.band == band } ?: return@forEach
            knob.hapticsEnabled = haptics
            knob.bind(ui.value, ui.isKill, ui.valueLabel)
            knob.contentDescription = getString(R.string.mixer_eq_cd, bandLabel(band), letter, ui.valueLabel)
        }
    }

    private fun bandLabel(band: EqBand): String = getString(
        when (band) {
            EqBand.HIGH -> R.string.mixer_eq_high
            EqBand.MID -> R.string.mixer_eq_mid
            EqBand.LOW -> R.string.mixer_eq_low
            EqBand.FILTER -> R.string.mixer_eq_filter
        },
    )

    private fun letterOf(deck: DeckId) = getString(if (deck == DeckId.A) R.string.deck_letter_a else R.string.deck_letter_b)

    private fun renderLibraryPanel(ui: LibraryPanelUi?) {
        val lib = binding.libraryPanel
        lib.root.isVisible = ui != null
        if (ui == null) return
        lib.textTitle.text = getString(R.string.mixer_library_title, letterOf(ui.target))
        libraryAdapter.submitList(ui.rows)
    }

    private var fxDeck: DeckId? = null

    private fun renderFxSheet(ui: FxSheetUi?) {
        val fx = binding.fxSheet
        fx.root.isVisible = ui != null
        if (ui == null) {
            fxDeck = null
            return
        }
        val deckInt = if (ui.deck == DeckId.A) DeckPalette.DECK_A else DeckPalette.DECK_B
        val palette = DeckPalette.of(this, deckInt)
        if (fxDeck != ui.deck) {
            fxDeck = ui.deck
            fx.textDeckLetter.text = letterOf(ui.deck)
            fx.textDeckLetter.setBackgroundResource(if (ui.deck == DeckId.A) R.drawable.bg_deck_a_r6 else R.drawable.bg_deck_b_r6)
            fx.xyPad.setDeck(deckInt)
            fx.knobWetDry.setDeck(deckInt)
        }
        fx.textFxName.text = ui.fxName
        // Switch: on = deck colour track + booth knob (Mixer:226); off = Settings style (App:207).
        fx.switchFxView.onColor = palette.accent
        fx.switchFxView.setChecked(ui.enabled)
        fx.switchFx.contentDescription = getString(R.string.fx_toggle_cd, ui.fxName)
        fx.switchFx.bindToggleSemantics(ui.enabled, "android.widget.Switch")

        val wetPct = (ui.wetDry * 100).roundToInt()
        fx.knobWetDry.bind(ui.wetDry, false, "")
        fx.knobWetDry.contentDescription = getString(R.string.fx_wet_dry_cd, ui.fxName, wetPct)
        fx.textWetDry.text = getString(R.string.settings_percent_value, wetPct)
        fx.textReadout.text = ui.readout
        fx.xyPad.setValues(ui.x, ui.y)
        fx.xyPad.contentDescription = getString(R.string.fx_xy_cd, ui.fxName)
        bindBeatLengths(ui, palette)
    }

    private fun bindBeatLengths(ui: FxSheetUi, palette: DeckPalette) {
        val row = binding.fxSheet.layoutBeatLengths
        if (row.childCount != ui.beatLengths.size) {
            row.removeAllViews()
            ui.beatLengths.forEachIndexed { i, _ ->
                val item = TextView(this).apply {
                    gravity = Gravity.CENTER
                    typeface = fontOf(R.font.chakra_petch_semibold)
                    textSize = 14f
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { viewModel.onFxBeatLengthSelected(i) }
                }
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                if (i > 0) lp.marginStart = dp(4f).toInt()
                row.addView(item, lp)
            }
        }
        ui.beatLengths.forEachIndexed { i, label ->
            val item = row.getChildAt(i) as TextView
            val selected = i == ui.selectedBeatLength
            item.text = label
            item.background = drawables.withRipple(
                drawables.rounded(if (selected) palette.accent else drawables.raised, dp(10f)),
            )
            item.setTextColor(if (selected) drawables.booth else drawables.muted)
            item.isSelected = selected
        }
    }

    private fun renderSaveDialog(ui: SaveDialogUi?) {
        val save = binding.saveDialog
        val wasVisible = save.root.isVisible
        save.root.isVisible = ui != null
        if (ui == null) return
        save.textDuration.text = ui.durationLabel
        if (save.editName.text.toString() != ui.name) {
            save.editName.setText(ui.name)
            if (!wasVisible) save.editName.setSelection(ui.name.length)
        }
        save.buttonMp3.isSelected = ui.format == RecordFormat.MP3
        save.buttonWav.isSelected = ui.format == RecordFormat.WAV
        save.textQuality.text = getString(R.string.settings_quality_value, ui.qualityKbps)
        save.textWavNote.isVisible = ui.showWavNote
        save.buttonSave.isEnabled = ui.name.isNotBlank()
    }

    private fun renderCoach(ui: CoachUi?) {
        val coach = binding.coach
        coach.root.isVisible = ui != null
        if (ui == null) {
            coach.root.setTarget(null)
            return
        }
        coach.root.setTarget(
            when (ui.target) {
                CoachTarget.SYNC_B -> deckB.syncButton
            },
        )
        coach.textStep.text = getString(R.string.coach_step, ui.step, ui.totalSteps)
        coach.textTitle.text = ui.title.resolve(this)
        coach.textBody.text = ui.body.resolve(this)
        bindCoachSteps(ui.step, ui.totalSteps)
    }

    /** 5 bars 16x4 r2 gap 4: reached steps in booth, the rest in inv-dim (Mixer:301). */
    private fun bindCoachSteps(step: Int, total: Int) {
        val row = binding.coach.layoutSteps
        if (row.childCount != total) {
            row.removeAllViews()
            repeat(total) { i ->
                val lp = LinearLayout.LayoutParams(dp(16f).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
                if (i > 0) lp.marginStart = dp(4f).toInt()
                row.addView(View(this), lp)
            }
        }
        for (i in 0 until total) {
            row.getChildAt(i).background = drawables.rounded(
                colorOf(if (i < step) R.color.md_booth else R.color.md_inv_dim), dp(2f),
            )
        }
    }

    // endregion

    override fun onDestroy() {
        if (::binding.isInitialized) binding.viewRecDot.removeCallbacks(recBlink)
        super.onDestroy()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private companion object {
        const val MIN_CENTER_DP = 216f
    }
}
