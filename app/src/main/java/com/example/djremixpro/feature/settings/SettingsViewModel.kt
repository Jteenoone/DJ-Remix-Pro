package com.example.djremixpro.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.djremixpro.R
import com.example.djremixpro.app.DJRemixProApp
import com.example.djremixpro.core.data.SettingsRepository
import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.core.model.DeckMode
import com.example.djremixpro.core.model.Languages
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.model.ThemeMode
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.ToastTone
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.core.util.resultOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Settings (App.dc.html:191-212, D-14). Theme and language are applied by DJRemixProApp from the repository. */
class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.settings
        .map { SettingsUiState(settings = it, languageName = languageName(it.languageTag)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    fun onThemeSelected(mode: ThemeMode) = save { it.copy(themeMode = mode) }

    fun onToggle(toggle: SettingToggle) = save {
        when (toggle) {
            SettingToggle.PRECUE -> it.copy(precue = !it.precue)
            SettingToggle.HAPTIC -> it.copy(haptic = !it.haptic)
            SettingToggle.KEEP_SCREEN_ON -> it.copy(keepScreenOn = !it.keepScreenOn)
        }
    }

    fun onLatencySelected(ms: Int) = save { it.copy(latencyMs = ms) }

    fun onMasterVolumeSelected(pct: Int) = save { it.copy(masterVolumePct = pct) }

    fun onFormatSelected(f: RecordFormat) = save { it.copy(recordFormat = f) }

    fun onQualitySelected(kbps: Int) = save { it.copy(recordQualityKbps = kbps) }

    fun onPitchRangeSelected(pct: Int) = save { it.copy(pitchRangePct = pct) }

    fun onJogModeSelected(mode: DeckMode) = save { it.copy(defaultJogMode = mode) }

    fun onFolderClicked() =
        toast(UiText.Res(R.string.toast_folder_fixed, listOf(uiState.value.settings.saveFolder)))

    fun onSupportClicked() = toast(UiText.Res(R.string.toast_support_unavailable))

    private fun save(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            resultOf { settingsRepository.update(transform) }
                .onFailure { toast(UiText.Res(R.string.toast_generic_error), ToastTone.ERROR) }
        }
    }

    private fun toast(text: UiText, tone: ToastTone = ToastTone.SUCCESS) {
        _events.trySend(SettingsEvent.ShowToast(ToastMessage(text, tone)))
    }

    private fun languageName(tag: String): String =
        (Languages.find(tag) ?: Languages.all.first()).nativeName

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DJRemixProApp
                SettingsViewModel(app.container.settingsRepository)
            }
        }
    }
}
