package com.example.djremixpro.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.djremixpro.app.DJRemixProApp
import com.example.djremixpro.core.data.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Reads the onboarding flag, keeps the splash for [MIN_SPLASH_MS], then publishes the destination. */
class SplashViewModel(settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            delay(MIN_SPLASH_MS)
            _uiState.value = SplashUiState(
                if (settings.onboardingDone) SplashDestination.HOME else SplashDestination.ONBOARDING,
            )
        }
    }

    companion object {
        const val MIN_SPLASH_MS = 800L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DJRemixProApp
                SplashViewModel(app.container.settingsRepository)
            }
        }
    }
}
