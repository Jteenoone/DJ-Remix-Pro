package com.example.djremixpro.feature.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.djremixpro.app.DJRemixProApp
import com.example.djremixpro.core.data.SettingsRepository
import com.example.djremixpro.core.util.resultOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Whatever the permission outcome, onboarding is marked done and the user goes Home (D-10). */
class PermissionViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    private val _events = Channel<PermissionEvent>(Channel.BUFFERED)
    val events: Flow<PermissionEvent> = _events.receiveAsFlow()

    fun onAllowClicked() {
        if (_uiState.value.isBusy) return
        _events.trySend(PermissionEvent.RequestPermission)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPermissionResult(granted: Boolean) = finishOnboarding()

    fun onUseSamplesClicked() = finishOnboarding()

    private fun finishOnboarding() {
        if (_uiState.value.isBusy) return
        _uiState.value = PermissionUiState(isBusy = true)
        viewModelScope.launch {
            // A failed write only means onboarding shows again next launch; the user still goes Home.
            resultOf { settingsRepository.update { it.copy(onboardingDone = true) } }
            _events.send(PermissionEvent.GoHome)
            _uiState.value = PermissionUiState(isBusy = false)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DJRemixProApp
                PermissionViewModel(app.container.settingsRepository)
            }
        }
    }
}
