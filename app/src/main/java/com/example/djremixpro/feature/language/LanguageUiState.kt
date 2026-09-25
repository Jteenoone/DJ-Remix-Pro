package com.example.djremixpro.feature.language

import com.example.djremixpro.core.ui.ToastMessage

data class LanguageRow(val tag: String, val nativeName: String, val vietnameseName: String, val isSelected: Boolean)

data class LanguageUiState(val query: String = "", val items: List<LanguageRow> = emptyList())

sealed interface LanguageEvent {
    data class ShowToast(val message: ToastMessage) : LanguageEvent
}
