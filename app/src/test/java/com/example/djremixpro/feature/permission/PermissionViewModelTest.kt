package com.example.djremixpro.feature.permission

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

/** V03 / D-10: cho phép, từ chối hay dùng bài mẫu đều đánh dấu xong onboarding rồi về Home. */
@OptIn(ExperimentalCoroutinesApi::class)
class PermissionViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val settings = TestSettingsRepository()

    @Test
    fun allow_requestsPermission_withoutWritingSettings() = runTest {
        val vm = PermissionViewModel(settings)
        vm.onAllowClicked()
        assertEquals(PermissionEvent.RequestPermission, vm.events.first())
        runCurrent()
        assertFalse(settings.settings.value.onboardingDone)
    }

    @Test
    fun granted_marksOnboardingDone_andGoesHome() = runTest {
        val vm = PermissionViewModel(settings)
        vm.onPermissionResult(granted = true)
        assertEquals(PermissionEvent.GoHome, vm.events.first())
        assertTrue(settings.settings.value.onboardingDone)
    }

    @Test
    fun denied_stillMarksOnboardingDone_andGoesHome() = runTest {
        val vm = PermissionViewModel(settings)
        vm.onPermissionResult(granted = false)
        assertEquals(PermissionEvent.GoHome, vm.events.first())
        assertTrue(settings.settings.value.onboardingDone)
    }

    @Test
    fun useSamples_marksOnboardingDone_andGoesHome() = runTest {
        val vm = PermissionViewModel(settings)
        vm.onUseSamplesClicked()
        assertEquals(PermissionEvent.GoHome, vm.events.first())
        assertTrue(settings.settings.value.onboardingDone)
        runCurrent()
        assertFalse(vm.uiState.value.isBusy)
    }

    @Test
    fun writeFailure_stillGoesHome() = runTest {
        settings.failUpdates = true
        val vm = PermissionViewModel(settings)
        vm.onUseSamplesClicked()
        assertEquals(PermissionEvent.GoHome, vm.events.first())
    }

    @Test
    fun doubleTap_writesOnce() = runTest {
        val vm = PermissionViewModel(settings)
        vm.onUseSamplesClicked()
        vm.onUseSamplesClicked()
        runCurrent()
        assertEquals(1, settings.updateCount)
    }
}
