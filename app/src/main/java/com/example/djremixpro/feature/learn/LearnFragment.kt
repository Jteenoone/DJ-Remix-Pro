package com.example.djremixpro.feature.learn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.djremixpro.R
import com.example.djremixpro.core.model.LearnSegment
import com.example.djremixpro.core.model.MixerArgs
import com.example.djremixpro.core.model.MixerEntry
import com.example.djremixpro.core.ui.ext.collectWhenStarted
import com.example.djremixpro.core.ui.ext.selectSegment
import com.example.djremixpro.databinding.FragmentLearnBinding

class LearnFragment : Fragment() {

    private var _binding: FragmentLearnBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LearnViewModel by viewModels { LearnViewModel.Factory }

    private val lessonAdapter = LessonAdapter { id -> openMixer(MixerEntry.LESSON, id) }
    private val glossaryAdapter = GlossaryAdapter { openMixer(MixerEntry.DEFAULT, -1) }
    private val tipAdapter = TipAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLearnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerLessons.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLessons.adapter = lessonAdapter
        binding.recyclerGlossary.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerGlossary.adapter = glossaryAdapter
        binding.recyclerTips.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTips.adapter = tipAdapter

        binding.editTermsSearch.doAfterTextChanged { viewModel.onTermsQueryChange(it?.toString().orEmpty()) }
        binding.editTermsSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                v.clearFocus()
                requireContext().getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }
        binding.segmentGuide.setOnClickListener { viewModel.onSegmentSelected(LearnSegment.GUIDE) }
        binding.segmentTerms.setOnClickListener { viewModel.onSegmentSelected(LearnSegment.TERMS) }
        binding.segmentTips.setOnClickListener { viewModel.onSegmentSelected(LearnSegment.TIPS) }

        collectWhenStarted(viewModel.uiState, ::render)
    }

    private fun render(state: LearnUiState) {
        val b = _binding ?: return
        listOf(b.segmentGuide, b.segmentTerms, b.segmentTips).selectSegment(state.segment.ordinal)
        b.sectionGuide.isVisible = state.segment == LearnSegment.GUIDE
        b.sectionTerms.isVisible = state.segment == LearnSegment.TERMS
        b.recyclerTips.isVisible = state.segment == LearnSegment.TIPS
        if (b.editTermsSearch.text.toString() != state.termsQuery) b.editTermsSearch.setText(state.termsQuery)
        b.textProgress.text = state.progressLabel.resolve(requireContext())
        lessonAdapter.submitList(state.lessons)
        glossaryAdapter.submitList(state.glossary)
        b.textTermsEmpty.isVisible = state.glossary.isEmpty() && state.termsQuery.isNotBlank()
        tipAdapter.submitList(state.tips)
    }

    private fun openMixer(entry: MixerEntry, lessonId: Int) {
        findNavController().navigate(
            R.id.action_global_mixer,
            Bundle().apply {
                putString(MixerArgs.ENTRY, entry.name)
                putInt(MixerArgs.LESSON_ID, lessonId)
            },
        )
    }

    override fun onDestroyView() {
        binding.recyclerLessons.adapter = null
        binding.recyclerGlossary.adapter = null
        binding.recyclerTips.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
