package com.example.djremixpro.feature.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.collectWhenStarted
import com.example.djremixpro.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val viewModel: SplashViewModel by viewModels { SplashViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSplashBinding.inflate(inflater, container, false)
        _binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        collectWhenStarted(viewModel.uiState) { state ->
            val destination = state.destination ?: return@collectWhenStarted
            val nav = findNavController()
            if (nav.currentDestination?.id != R.id.splashFragment) return@collectWhenStarted
            when (destination) {
                SplashDestination.ONBOARDING -> nav.navigate(R.id.action_splash_to_onboarding)
                SplashDestination.HOME -> nav.navigate(R.id.action_splash_to_home)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
