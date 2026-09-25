package com.example.djremixpro.feature.settings

import com.example.djremixpro.R
import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.core.model.DeckMode
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.model.ThemeMode
import com.example.djremixpro.core.ui.ToastTone
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.testutil.MainDispatcherRule
import com.example.djremixpro.testutil.TestSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** V23–V24: Cài đặt (App:191–212, 269), dòng giá trị (D-14), tên ngôn ngữ (B02). */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = TestSettingsRepository()

    // V23
    @Test
    fun defaults_matchDesign() = runTest {
        val vm = SettingsViewModel(repo)
        runCurrent()
        val s = vm.uiState.value.settings
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertEquals(40, s.latencyMs)
        assertFalse(s.precue)
        assertEquals(80, s.masterVolumePct)
        assertEquals(RecordFormat.MP3, s.recordFormat)
        assertEquals(320, s.recordQualityKbps)
        assertEquals("Music/MixDeck", s.saveFolder)
        assertEquals(8, s.pitchRangePct)
        assertEquals(DeckMode.JOG, s.defaultJogMode)
        assertTrue(s.haptic)
        assertTrue(s.keepScreenOn)
        assertEquals("Tiếng Việt", vm.uiState.value.languageName)
    }

    @Test
    fun themeSelection_isPersisted() = runTest {
        val vm = SettingsViewModel(repo)
        vm.onThemeSelected(ThemeMode.LIGHT); runCurrent()
        assertEquals(ThemeMode.LIGHT, repo.settings.value.themeMode)
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.settings.themeMode)
        vm.onThemeSelected(ThemeMode.SYSTEM); runCurrent()
        assertEquals(ThemeMode.SYSTEM, vm.uiState.value.settings.themeMode)
    }

    @Test
    fun toggles_flipAndFlipBack() = runTest {
        val vm = SettingsViewModel(repo)
        vm.onToggle(SettingToggle.PRECUE); runCurrent()
        assertTrue(vm.uiState.value.settings.precue)
        vm.onToggle(SettingToggle.HAPTIC); runCurrent()
        assertFalse(vm.uiState.value.settings.haptic)
        vm.onToggle(SettingToggle.KEEP_SCREEN_ON); runCurrent()
        assertFalse(vm.uiState.value.settings.keepScreenOn)

        vm.onToggle(SettingToggle.PRECUE)
        vm.onToggle(SettingToggle.HAPTIC)
        vm.onToggle(SettingToggle.KEEP_SCREEN_ON)
        runCurrent()
        assertEquals(AppSettings(), repo.settings.value)
    }

    @Test
    fun rapidDoubleToggle_endsWhereItStarted() = runTest {
        val vm = SettingsViewModel(repo)
        vm.onToggle(SettingToggle.PRECUE)
        vm.onToggle(SettingToggle.PRECUE)
        runCurrent()
        assertFalse(repo.settings.value.precue)
    }

    // V24
    @Test
    fun valueRows_arePersisted() = runTest {
        val vm = SettingsViewModel(repo)
        vm.onLatencySelected(100)
        vm.onMasterVolumeSelected(60)
        vm.onFormatSelected(RecordFormat.WAV)
        vm.onQualitySelected(192)
        vm.onPitchRangeSelected(16)
        vm.onJogModeSelected(DeckMode.PAD)
        runCurrent()
        val s = repo.settings.value
        assertEquals(100, s.latencyMs)
        assertEquals(60, s.masterVolumePct)
        assertEquals(RecordFormat.WAV, s.recordFormat)
        assertEquals(192, s.recordQualityKbps)
        assertEquals(16, s.pitchRangePct)
        assertEquals(DeckMode.PAD, s.defaultJogMode)
    }

    @Test
    fun folderAndSupport_showToasts() = runTest {
        val vm = SettingsViewModel(repo)
        runCurrent()
        vm.onFolderClicked()
        val folder = vm.events.first() as SettingsEvent.ShowToast
        assertEquals(UiText.Res(R.string.toast_folder_fixed, listOf("Music/MixDeck")), folder.message.text)
        vm.onSupportClicked()
        val support = vm.events.first() as SettingsEvent.ShowToast
        assertEquals(UiText.Res(R.string.toast_support_unavailable), support.message.text)
    }

    @Test
    fun writeFailure_showsErrorToast_andKeepsState() = runTest {
        val vm = SettingsViewModel(repo)
        runCurrent()
        repo.failUpdates = true
        vm.onThemeSelected(ThemeMode.LIGHT)
        val e = vm.events.first() as SettingsEvent.ShowToast
        assertEquals(ToastTone.ERROR, e.message.tone)
        assertEquals(UiText.Res(R.string.toast_generic_error), e.message.text)
        runCurrent()
        assertEquals(ThemeMode.DARK, vm.uiState.value.settings.themeMode)
    }

    @Test
    fun languageName_followsStoredTag() = runTest {
        val vm = SettingsViewModel(repo)
        repo.settings.value = AppSettings(languageTag = "ja"); runCurrent()
        assertEquals("日本語", vm.uiState.value.languageName)
        repo.settings.value = AppSettings(languageTag = "xx-unknown"); runCurrent()
        assertEquals("Tiếng Việt", vm.uiState.value.languageName)
    }
}
