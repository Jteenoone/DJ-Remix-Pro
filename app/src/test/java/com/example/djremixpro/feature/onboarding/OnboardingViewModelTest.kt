package com.example.djremixpro.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.example.djremixpro.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** V02: 3 trang (#2e–#2g), Bỏ qua ở trang 1–2, Tiếp ở trang cuối → xin quyền. */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    @Test
    fun startsOnFirstPage_canSkip() {
        val s = OnboardingViewModel(SavedStateHandle()).uiState.value
        assertEquals(0, s.page)
        assertEquals(3, s.pageCount)
        assertTrue(s.canSkip)
    }

    @Test
    fun next_advancesPages_skipHiddenOnLastPage() = runTest {
        val vm = OnboardingViewModel(SavedStateHandle())
        vm.onNext()
        assertEquals(1, vm.uiState.value.page)
        assertTrue(vm.uiState.value.canSkip)
        vm.onNext()
        assertEquals(2, vm.uiState.value.page)
        assertFalse(vm.uiState.value.canSkip)
        assertNull(withTimeoutOrNull(100) { vm.events.first() })
    }

    @Test
    fun nextOnLastPage_emitsGoToPermission_andStaysOnLastPage() = runTest {
        val vm = OnboardingViewModel(SavedStateHandle())
        repeat(3) { vm.onNext() }
        assertEquals(OnboardingEvent.GoToPermission, vm.events.first())
        assertEquals(2, vm.uiState.value.page)
        vm.onNext()
        assertEquals(2, vm.uiState.value.page)
    }

    @Test
    fun skip_emitsGoToPermission() = runTest {
        val vm = OnboardingViewModel(SavedStateHandle())
        vm.onSkip()
        assertEquals(OnboardingEvent.GoToPermission, vm.events.first())
    }

    @Test
    fun pageSurvivesProcessDeath() {
        val handle = SavedStateHandle()
        OnboardingViewModel(handle).onNext()
        assertEquals(1, OnboardingViewModel(handle).uiState.value.page)
    }
}
