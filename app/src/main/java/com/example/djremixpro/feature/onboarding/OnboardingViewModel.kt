package com.example.djremixpro.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** Three pages (canvas #2e–#2g); "Bỏ qua" on pages 1–2 and "Tiếp" on the last page lead to the permission screen. */
class OnboardingViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _uiState = MutableStateFlow(stateFor(savedStateHandle.get<Int>(KEY_PAGE) ?: 0))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = _events.receiveAsFlow()

    fun onNext() {
        val page = _uiState.value.page
        if (page >= PAGE_COUNT - 1) {
            _events.trySend(OnboardingEvent.GoToPermission)
        } else {
            setPage(page + 1)
        }
    }

    fun onSkip() {
        _events.trySend(OnboardingEvent.GoToPermission)
    }

    private fun setPage(page: Int) {
        savedStateHandle[KEY_PAGE] = page
        _uiState.value = stateFor(page)
    }

    private fun stateFor(page: Int): OnboardingUiState {
        val p = page.coerceIn(0, PAGE_COUNT - 1)
        return OnboardingUiState(page = p, pageCount = PAGE_COUNT, canSkip = p < PAGE_COUNT - 1)
    }

    companion object {
        private const val PAGE_COUNT = 3
        private const val KEY_PAGE = "onboarding_page"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { OnboardingViewModel(createSavedStateHandle()) }
        }
    }
}
