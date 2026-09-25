package com.example.djremixpro.feature.splash

import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.testutil.MainDispatcherRule
import com.example.djremixpro.testutil.TestSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** V01: splash giữ ≥ 800 ms rồi chọn đích theo cờ onboarding. */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    @Test
    fun firstLaunch_goesToOnboardingAfterMinimumTime() = runTest {
        val vm = SplashViewModel(TestSettingsRepository(AppSettings(onboardingDone = false)))
        runCurrent()
        assertNull(vm.uiState.value.destination)
        advanceTimeBy(SplashViewModel.MIN_SPLASH_MS - 1); runCurrent()
        assertNull(vm.uiState.value.destination)
        advanceTimeBy(1); runCurrent()
        assertEquals(SplashDestination.ONBOARDING, vm.uiState.value.destination)
    }

    @Test
    fun returningUser_goesHome() = runTest {
        val vm = SplashViewModel(TestSettingsRepository(AppSettings(onboardingDone = true)))
        advanceTimeBy(SplashViewModel.MIN_SPLASH_MS); runCurrent()
        assertEquals(SplashDestination.HOME, vm.uiState.value.destination)
    }
}
