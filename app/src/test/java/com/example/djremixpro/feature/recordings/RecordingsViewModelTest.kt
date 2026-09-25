package com.example.djremixpro.feature.recordings

import com.example.djremixpro.R
import com.example.djremixpro.core.data.fake.FakeRecordingRepository
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.ToastTone
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.testutil.MainDispatcherRule
import com.example.djremixpro.testutil.TestPlaybackRepository
import com.example.djremixpro.testutil.TestRecordingRepository
import com.example.djremixpro.testutil.recording
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** V16–V20: danh sách bản ghi (App:314–316), trạng thái trống, sheet Đổi tên / Chia sẻ / Xoá (D-13). */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordingsViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val playback = TestPlaybackRepository()
    private val errorToast = RecordingsEvent.ShowToast(
        ToastMessage(UiText.Res(R.string.toast_generic_error), ToastTone.ERROR),
    )

    private fun seeded(repo: FakeRecordingRepository = FakeRecordingRepository()) = RecordingsViewModel(repo, playback)

    // V16
    @Test
    fun seededList_rowsAndWaveSeeds() = runTest {
        val s = seeded().uiState.value
        assertFalse(s.isEmpty)
        assertEquals(listOf(5, 18, 31, 44), s.items.map { it.waveSeed })
        assertEquals(UiText.Res(R.string.recording_meta, listOf("03:12", "25/09/2026")), s.items[0].meta)
        assertTrue(s.items.none { it.isPlaying })
    }

    @Test
    fun rowClick_togglesPlaying_withPlayingMeta() = runTest {
        val vm = seeded()
        vm.onRowClicked("rec-2"); runCurrent()
        val row = vm.uiState.value.items.single { it.id == "rec-2" }
        assertTrue(row.isPlaying)
        assertEquals(UiText.Res(R.string.recording_meta_playing, listOf("05:47", "24/09/2026")), row.meta)
        assertEquals(1, vm.uiState.value.items.count { it.isPlaying })

        vm.onRowClicked("rec-1"); runCurrent()
        assertEquals(listOf("rec-1"), vm.uiState.value.items.filter { it.isPlaying }.map { it.id })

        vm.onRowClicked("rec-1"); runCurrent()
        assertTrue(vm.uiState.value.items.none { it.isPlaying })
    }

    // V17
    @Test
    fun emptyRepository_showsEmptyState() = runTest {
        val vm = RecordingsViewModel(TestRecordingRepository(), playback)
        runCurrent()
        assertTrue(vm.uiState.value.isEmpty)
        assertTrue(vm.uiState.value.items.isEmpty())
    }

    @Test
    fun deletingLastItem_switchesToEmptyState() = runTest {
        val repo = TestRecordingRepository(listOf(recording(1)))
        val vm = RecordingsViewModel(repo, playback)
        vm.onMenuClicked("r-1"); vm.onDeleteClicked(); vm.onDeleteConfirmed()
        vm.events.first()
        runCurrent()
        assertTrue(vm.uiState.value.isEmpty)
    }

    // V18
    @Test
    fun menu_opensSheetWithMeta() = runTest {
        val vm = seeded()
        vm.onMenuClicked("rec-1"); runCurrent()
        assertEquals(RecordingSheetUi("rec-1", "Mix 25-09 21:40", "03:12 · 25/09/2026"), vm.uiState.value.sheet)
        vm.onSheetDismissed(); runCurrent()
        assertNull(vm.uiState.value.sheet)
    }

    @Test
    fun menu_unknownId_opensNothing() = runTest {
        val vm = seeded()
        vm.onMenuClicked("rec-99"); runCurrent()
        assertNull(vm.uiState.value.sheet)
    }

    @Test
    fun rename_success() = runTest {
        val repo = FakeRecordingRepository()
        val vm = seeded(repo)
        vm.onMenuClicked("rec-3"); vm.onRenameClicked(); runCurrent()
        assertNull(vm.uiState.value.sheet)
        assertEquals(RenameDialogUi("rec-3", "Tiệc sinh nhật Linh"), vm.uiState.value.renameDialog)

        vm.onRenameConfirmed("Sinh nhật Linh 2026")
        val event = vm.events.first() as RecordingsEvent.ShowToast
        assertEquals(ToastTone.SUCCESS, event.message.tone)
        runCurrent()
        assertNull(vm.uiState.value.renameDialog)
        assertEquals("Sinh nhật Linh 2026", vm.uiState.value.items.single { it.id == "rec-3" }.name)
    }

    @Test
    fun rename_blankName_keepsNameAndShowsError() = runTest {
        val repo = FakeRecordingRepository()
        val vm = seeded(repo)
        for (blank in listOf("", "   ")) {
            vm.onMenuClicked("rec-1"); vm.onRenameClicked()
            vm.onRenameConfirmed(blank)
            assertEquals(errorToast, vm.events.first())
        }
        runCurrent()
        assertEquals("Mix 25-09 21:40", vm.uiState.value.items.single { it.id == "rec-1" }.name)
        assertEquals(4, vm.uiState.value.items.size)
    }

    @Test
    fun rename_dismiss_closesDialogWithoutChange() = runTest {
        val vm = seeded()
        vm.onMenuClicked("rec-1"); vm.onRenameClicked(); vm.onRenameDismissed(); runCurrent()
        assertNull(vm.uiState.value.renameDialog)
        assertEquals("Mix 25-09 21:40", vm.uiState.value.items[0].name)
    }

    @Test
    fun renameConfirmed_withoutDialog_isIgnored() = runTest {
        val repo = FakeRecordingRepository()
        val vm = seeded(repo)
        vm.onRenameConfirmed("Tên")
        runCurrent()
        assertEquals(listOf("Mix 25-09 21:40", "Mix 24-09 22:05", "Tiệc sinh nhật Linh", "Mix 18-09 23:10"),
            repo.recordings.value.map { it.name })
    }

    // V19
    @Test
    fun delete_confirm_removesItemAndToasts() = runTest {
        val vm = seeded()
        vm.onMenuClicked("rec-2"); vm.onDeleteClicked(); runCurrent()
        assertNull(vm.uiState.value.sheet)
        assertEquals(DeleteConfirmUi("rec-2", "Mix 24-09 22:05"), vm.uiState.value.deleteConfirm)

        vm.onDeleteConfirmed()
        val event = vm.events.first() as RecordingsEvent.ShowToast
        assertEquals(UiText.Res(R.string.toast_recording_deleted), event.message.text)
        runCurrent()
        assertNull(vm.uiState.value.deleteConfirm)
        assertEquals(listOf("rec-1", "rec-3", "rec-4"), vm.uiState.value.items.map { it.id })
    }

    @Test
    fun delete_dismiss_keepsItem() = runTest {
        val vm = seeded()
        vm.onMenuClicked("rec-2"); vm.onDeleteClicked(); vm.onDeleteDismissed(); runCurrent()
        assertNull(vm.uiState.value.deleteConfirm)
        assertEquals(4, vm.uiState.value.items.size)
    }

    @Test
    fun delete_idAlreadyGone_showsErrorAndKeepsList() = runTest {
        val repo = FakeRecordingRepository()
        val vm = seeded(repo)
        vm.onMenuClicked("rec-4"); vm.onDeleteClicked()
        repo.delete("rec-4") // bị xoá ở nơi khác trước khi người dùng xác nhận
        vm.onDeleteConfirmed()
        assertEquals(errorToast, vm.events.first())
        runCurrent()
        assertEquals(listOf("rec-1", "rec-2", "rec-3"), vm.uiState.value.items.map { it.id })
    }

    @Test
    fun delete_repositoryFailure_showsErrorAndKeepsList() = runTest {
        val repo = TestRecordingRepository(listOf(recording(1), recording(2)))
        val vm = RecordingsViewModel(repo, playback)
        repo.failWith = IOException("io")
        vm.onMenuClicked("r-1"); vm.onDeleteClicked(); vm.onDeleteConfirmed()
        assertEquals(errorToast, vm.events.first())
        runCurrent()
        assertEquals(listOf("r-1", "r-2"), vm.uiState.value.items.map { it.id })
    }

    @Test
    fun delete_playingItem_stopsMiniPlayer() = runTest {
        val vm = seeded()
        vm.onRowClicked("rec-1")
        vm.onMenuClicked("rec-1"); vm.onDeleteClicked(); vm.onDeleteConfirmed()
        vm.events.first()
        assertNull(playback.nowPlaying.value)
    }

    // V20
    @Test
    fun share_showsUnavailableToast_andClosesSheet() = runTest {
        val vm = seeded()
        vm.onMenuClicked("rec-1")
        vm.onShareClicked()
        assertEquals(
            RecordingsEvent.ShowToast(ToastMessage(UiText.Res(R.string.toast_share_unavailable))),
            vm.events.first(),
        )
        runCurrent()
        assertNull(vm.uiState.value.sheet)
    }
}
