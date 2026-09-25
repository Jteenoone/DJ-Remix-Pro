package com.example.djremixpro.feature.settings

import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.core.ui.ToastMessage

/** [languageName] is the native name of the selected language. */
data class SettingsUiState(val settings: AppSettings = AppSettings(), val languageName: String = "Tiếng Việt")

sealed interface SettingsEvent {
    data class ShowToast(val message: ToastMessage) : SettingsEvent
}

enum class SettingToggle { PRECUE, HAPTIC, KEEP_SCREEN_ON }
