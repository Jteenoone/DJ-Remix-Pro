package com.example.djremixpro.feature.onboarding

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
import com.example.djremixpro.core.ui.ext.collectWhenStarted
import com.example.djremixpro.core.ui.ext.dp
import com.example.djremixpro.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnboardingViewModel by viewModels { OnboardingViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonNext.setOnClickListener { viewModel.onNext() }
        binding.buttonSkip.setOnClickListener { viewModel.onSkip() }
        // Static illustration of page 2 (MixDeck:149-150): same wave on both decks, not scrolling.
        listOf(binding.waveA, binding.waveB).forEach {
            it.bind(
                loaded = true, seed = ILLUSTRATION_SEED, bpm = 124f, effectiveBpm = 124f,
                playing = false, loopBeats = null, positionSec = 0f,
            )
        }

        collectWhenStarted(viewModel.uiState, ::render)
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                OnboardingEvent.GoToPermission -> {
                    val nav = findNavController()
                    if (nav.currentDestination?.id == R.id.onboardingFragment) {
                        nav.navigate(R.id.action_onboarding_to_permission)
                    }
                }
            }
        }
    }

    private fun render(state: OnboardingUiState) {
        val b = _binding ?: return
        b.buttonSkip.visibility = if (state.canSkip) View.VISIBLE else View.INVISIBLE
        b.pageDecks.isVisible = state.page == 0
        b.pageSync.isVisible = state.page == 1
        b.pageRecord.isVisible = state.page == 2
        b.textTitle.setText(
            when (state.page) {
                0 -> R.string.onboarding_1_title
                1 -> R.string.onboarding_2_title
                else -> R.string.onboarding_3_title
            },
        )
        renderIndicator(state.page, state.pageCount)
    }

    /** Active page 24x6 in text colour, others 6x6 in line colour, gap 6 (MixDeck:134). */
    private fun renderIndicator(page: Int, count: Int) {
        val container = binding.layoutIndicator
        if (container.childCount != count) {
            container.removeAllViews()
            repeat(count) { i ->
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT)
                if (i > 0) lp.marginStart = container.dp(6f).toInt()
                container.addView(View(requireContext()), lp)
            }
        }
        for (i in 0 until count) {
            val dot = container.getChildAt(i)
            val active = i == page
            dot.setBackgroundResource(if (active) R.drawable.shape_dot_text else R.drawable.shape_dot_line)
            val lp = dot.layoutParams as LinearLayout.LayoutParams
            lp.width = container.dp(if (active) 24f else 6f).toInt()
            dot.layoutParams = lp
        }
        container.contentDescription = getString(R.string.onboarding_page_indicator, page + 1, count)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        /** Seed of the canvas wave() illustration (MixDeck:740). */
        const val ILLUSTRATION_SEED = 11
    }
}
