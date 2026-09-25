package com.example.djremixpro.feature.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.djremixpro.R
import com.example.djremixpro.app.DJRemixProApp
import com.example.djremixpro.core.data.SettingsRepository
import com.example.djremixpro.core.model.Languages
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.ToastTone
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.core.util.TextUtils2
import com.example.djremixpro.core.util.resultOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Language list (App.dc.html:214-224, 284, 298). Selecting saves the tag and shows "Đã chọn {native}" (D-15). */
class LanguageViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<LanguageUiState> = combine(query, settingsRepository.settings.map { it.languageTag }) { q, tag ->
        buildState(q, tag)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, buildState("", DEFAULT_TAG))

    private val _events = Channel<LanguageEvent>(Channel.BUFFERED)
    val events: Flow<LanguageEvent> = _events.receiveAsFlow()

    fun onQueryChange(q: String) {
        query.value = q
    }

    fun onLanguageSelected(tag: String) {
        val language = Languages.find(tag) ?: return
        viewModelScope.launch {
            resultOf { settingsRepository.update { it.copy(languageTag = tag) } }
                .onSuccess {
                    _events.send(
                        LanguageEvent.ShowToast(
                            ToastMessage(UiText.Res(R.string.toast_language_selected, listOf(language.nativeName))),
                        ),
                    )
                }
                .onFailure {
                    _events.send(
                        LanguageEvent.ShowToast(ToastMessage(UiText.Res(R.string.toast_generic_error), ToastTone.ERROR)),
                    )
                }
        }
    }

    private fun buildState(q: String, selectedTag: String): LanguageUiState {
        val needle = TextUtils2.normalizeForSearch(q.trim())
        val items = Languages.all
            .filter {
                needle.isEmpty() ||
                    TextUtils2.normalizeForSearch(it.nativeName).contains(needle) ||
                    TextUtils2.normalizeForSearch(it.vietnameseName).contains(needle) ||
                    it.tag.lowercase().contains(needle)
            }
            .map { LanguageRow(it.tag, it.nativeName, it.vietnameseName, isSelected = it.tag == selectedTag) }
        return LanguageUiState(query = q, items = items)
    }

    companion object {
        private const val DEFAULT_TAG = "vi"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DJRemixProApp
                LanguageViewModel(app.container.settingsRepository)
            }
        }
    }
}
