package com.example.djremixpro.feature.onboarding

/** [canSkip] is true on pages 0 and 1 only. */
data class OnboardingUiState(val page: Int = 0, val pageCount: Int = 3, val canSkip: Boolean = true)

sealed interface OnboardingEvent {
    data object GoToPermission : OnboardingEvent
}
