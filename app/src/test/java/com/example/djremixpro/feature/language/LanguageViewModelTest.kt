package com.example.djremixpro.feature.language

import com.example.djremixpro.R
import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.ToastTone
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.testutil.MainDispatcherRule
import com.example.djremixpro.testutil.TestSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** V25–V26: 17 ngôn ngữ (App:284, 298), chọn → toast "Đã chọn {native}", tìm kiếm. */
@OptIn(ExperimentalCoroutinesApi::class)
class LanguageViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = TestSettingsRepository()

    private fun LanguageViewModel.tags() = uiState.value.items.map { it.tag }

    // V25
    @Test
    fun list_hasSeventeenInDesignOrder_viSelected() = runTest {
        val vm = LanguageViewModel(repo)
        runCurrent()
        assertEquals(
            listOf(
                "vi", "hi", "es", "pt-BR", "en", "pt-PT", "fr", "ar", "bn",
                "ru", "de", "ja", "tr", "ko", "id", "zh-Hans", "zh-Hant",
            ),
            vm.tags(),
        )
        assertEquals(listOf("vi"), vm.uiState.value.items.filter { it.isSelected }.map { it.tag })
    }

    @Test
    fun select_persistsTag_marksRow_andToastsNativeName() = runTest {
        val vm = LanguageViewModel(repo)
        vm.onLanguageSelected("ar")
        val e = vm.events.first()
        assertEquals(
            LanguageEvent.ShowToast(ToastMessage(UiText.Res(R.string.toast_language_selected, listOf("العربية")))),
            e,
        )
        runCurrent()
        assertEquals("ar", repo.settings.value.languageTag)
        assertEquals(listOf("ar"), vm.uiState.value.items.filter { it.isSelected }.map { it.tag })
    }

    @Test
    fun select_unknownTag_isIgnored() = runTest {
        val vm = LanguageViewModel(repo)
        vm.onLanguageSelected("xx")
        runCurrent()
        assertEquals("vi", repo.settings.value.languageTag)
        assertNull(withTimeoutOrNull(100) { vm.events.first() })
    }

    @Test
    fun select_writeFailure_showsErrorToast() = runTest {
        repo.failUpdates = true
        val vm = LanguageViewModel(repo)
        vm.onLanguageSelected("en")
        val e = vm.events.first() as LanguageEvent.ShowToast
        assertEquals(ToastTone.ERROR, e.message.tone)
        runCurrent()
        assertEquals("vi", repo.settings.value.languageTag)
    }

    @Test
    fun storedTag_isReflected() = runTest {
        repo.settings.value = AppSettings(languageTag = "ja")
        val vm = LanguageViewModel(repo)
        runCurrent()
        assertEquals(listOf("ja"), vm.uiState.value.items.filter { it.isSelected }.map { it.tag })
    }

    // V26
    @Test
    fun search_vietnameseNameWithoutAccents() = runTest {
        val vm = LanguageViewModel(repo)
        vm.onQueryChange("tieng anh"); runCurrent()
        assertEquals(listOf("en"), vm.tags())
        vm.onQueryChange("TIẾNG NHẬT"); runCurrent()
        assertEquals(listOf("ja"), vm.tags())
    }

    @Test
    fun search_nativeScriptAndPartial() = runTest {
        val vm = LanguageViewModel(repo)
        vm.onQueryChange("日本"); runCurrent()
        assertEquals(listOf("ja"), vm.tags())
        vm.onQueryChange("bo dao nha"); runCurrent()
        assertEquals(listOf("pt-BR", "pt-PT"), vm.tags())
        vm.onQueryChange("trung"); runCurrent()
        assertEquals(listOf("zh-Hans", "zh-Hant"), vm.tags())
    }

    @Test
    fun search_noMatch_thenClear() = runTest {
        val vm = LanguageViewModel(repo)
        vm.onQueryChange("klingon"); runCurrent()
        assertTrue(vm.uiState.value.items.isEmpty())
        vm.onQueryChange(""); runCurrent()
        assertEquals(17, vm.uiState.value.items.size)
    }
}
