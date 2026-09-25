package com.example.djremixpro.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.djremixpro.R
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.LearnArgs
import com.example.djremixpro.core.model.LearnSegment
import com.example.djremixpro.core.model.MixerArgs
import com.example.djremixpro.core.model.MixerEntry
import com.example.djremixpro.core.ui.ShellNavigator
import com.example.djremixpro.core.ui.ext.collectWhenStarted
import com.example.djremixpro.core.ui.ext.colorOf
import com.example.djremixpro.core.ui.ext.dp
import com.example.djremixpro.databinding.FragmentHomeBinding
import com.example.djremixpro.databinding.ItemDeckTrackBinding
import com.example.djremixpro.databinding.ItemRecentMixBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels { HomeViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonOpenMixer.setOnClickListener { openMixer(MixerEntry.DEFAULT) }
        binding.buttonMixSamples.setOnClickListener { openMixer(MixerEntry.SAMPLES) }
        binding.cardResume.setOnClickListener { openMixer(MixerEntry.DEFAULT) }
        binding.buttonLearnAll.setOnClickListener { openTab(R.id.learnFragment, null) }
        binding.buttonRecentAll.setOnClickListener { openTab(R.id.recordingsFragment, null) }
        binding.tileGuide.setOnClickListener { openLearn(LearnSegment.GUIDE) }
        binding.tileTerms.setOnClickListener { openLearn(LearnSegment.TERMS) }
        binding.tileTips.setOnClickListener { openLearn(LearnSegment.TIPS) }

        collectWhenStarted(viewModel.uiState, ::render)
    }

    private fun render(state: HomeUiState) {
        val b = _binding ?: return
        b.cardSamples.isVisible = state.isNewUser
        if (state.isNewUser) bindDeckRows(b.layoutSampleRows, state.samples, divided = false)

        val showResume = !state.isNewUser && state.session.isNotEmpty()
        b.sectionResume.isVisible = showResume
        if (showResume) {
            bindDeckRows(b.layoutSessionRows, state.session, divided = true)
            b.cardResume.contentDescription = buildString {
                append(getString(R.string.home_resume_title))
                state.session.forEach { append(", ").append(getString(R.string.deck_name, it.deck.name)).append(' ').append(it.title) }
            }
        }

        b.cardAd.isVisible = state.showAd

        val showRecent = !state.isNewUser && state.recent.isNotEmpty()
        b.sectionRecent.isVisible = showRecent
        if (showRecent) bindRecentRows(state.recent)
    }

    /** Samples: rows 24 tall, 12 apart. Resume card: rows 44 tall separated by a line (App:57). */
    private fun bindDeckRows(container: LinearLayout, rows: List<DeckTrackRow>, divided: Boolean) {
        syncChildCount(container, rows.size) { ItemDeckTrackBinding.inflate(layoutInflater, container, false).root }
        rows.forEachIndexed { i, row ->
            val item = ItemDeckTrackBinding.bind(container.getChildAt(i))
            item.textDeck.text = row.deck.name
            item.textDeck.setBackgroundResource(if (row.deck == DeckId.A) R.drawable.bg_deck_a_r6 else R.drawable.bg_deck_b_r6)
            item.textTitle.text = row.title
            item.textBpm.text = row.bpmLabel
            val lp = item.root.layoutParams as LinearLayout.LayoutParams
            if (divided) {
                lp.height = container.dp(44f).toInt()
                lp.topMargin = 0
                if (i < rows.lastIndex) item.root.setBackgroundResource(R.drawable.bg_bottom_line) else item.root.background = null
            } else {
                lp.height = LinearLayout.LayoutParams.WRAP_CONTENT
                lp.topMargin = container.dp(12f).toInt()
                item.root.background = null
            }
            item.root.layoutParams = lp
            item.root.importantForAccessibility = if (divided) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        }
    }

    private fun bindRecentRows(rows: List<RecentMixRow>) {
        val container = binding.layoutRecentRows
        syncChildCount(container, rows.size) { ItemRecentMixBinding.inflate(layoutInflater, container, false).root }
        rows.forEachIndexed { i, row ->
            val item = ItemRecentMixBinding.bind(container.getChildAt(i))
            item.textName.text = row.name
            item.textDuration.text = row.durationLabel
            item.framePlay.setBackgroundResource(if (row.isPlaying) R.drawable.bg_circle_text else R.drawable.sel_circle_panel)
            item.imagePlay.setImageResource(if (row.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            item.imagePlay.imageTintList = android.content.res.ColorStateList.valueOf(
                requireContext().colorOf(if (row.isPlaying) R.color.md_booth else R.color.md_text),
            )
            item.root.contentDescription = getString(
                if (row.isPlaying) R.string.recording_pause_cd else R.string.recording_listen_cd,
                row.name,
            )
            item.root.setOnClickListener { viewModel.onRecentPlayToggle(row.id) }
        }
    }

    private inline fun syncChildCount(container: LinearLayout, count: Int, create: () -> View) {
        while (container.childCount > count) container.removeViewAt(container.childCount - 1)
        while (container.childCount < count) container.addView(create())
    }

    private fun openMixer(entry: MixerEntry) {
        findNavController().navigate(
            R.id.action_global_mixer,
            Bundle().apply {
                putString(MixerArgs.ENTRY, entry.name)
                putInt(MixerArgs.LESSON_ID, -1)
            },
        )
    }

    private fun openLearn(segment: LearnSegment) =
        openTab(R.id.learnFragment, Bundle().apply { putString(LearnArgs.SEGMENT, segment.name) })

    private fun openTab(destinationId: Int, args: Bundle?) {
        (requireActivity() as ShellNavigator).openTab(destinationId, args)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
