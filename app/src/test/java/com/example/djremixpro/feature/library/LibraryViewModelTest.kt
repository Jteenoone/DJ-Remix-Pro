package com.example.djremixpro.feature.library

import com.example.djremixpro.R
import com.example.djremixpro.core.data.fake.FakeTrackRepository
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.LibraryTab
import com.example.djremixpro.core.model.SortMode
import com.example.djremixpro.core.ui.ToastTone
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.testutil.MainDispatcherRule
import com.example.djremixpro.testutil.TestMixSessionRepository
import com.example.djremixpro.testutil.TestPlaybackRepository
import com.example.djremixpro.testutil.TestTrackRepository
import com.example.djremixpro.testutil.track
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

/** V07–V15: sắp xếp (App:308–312), BPM chưa phân tích, tìm kiếm (D-17), sheet bài và nạp deck (App:324–330). */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val tracks = FakeTrackRepository()
    private val session = TestMixSessionRepository(null)
    private val playback = TestPlaybackRepository()

    private fun vm() = LibraryViewModel(tracks, session, playback)

    private fun LibraryViewModel.titles() = uiState.value.songs.map { it.title }
    private fun LibraryViewModel.count() = (uiState.value.countLabel as UiText.Plural).count

    /** Từ chế độ mặc định BPM, bấm "Sắp xếp" đủ số lần để tới [mode] (uiState chỉ cập nhật sau runCurrent). */
    private fun LibraryViewModel.sortTo(mode: SortMode) {
        val cycle = LibraryViewModel.SORT_CYCLE
        repeat((cycle.indexOf(mode) - cycle.indexOf(SortMode.BPM) + cycle.size) % cycle.size) { onCycleSort() }
    }

    // V07
    @Test
    fun defaultSort_isBpm_withTenSongs() = runTest {
        val vm = vm()
        assertEquals(SortMode.BPM, vm.uiState.value.sort)
        assertEquals(10, vm.uiState.value.songs.size)
        assertEquals(10, vm.count())
    }

    @Test
    fun cycleSort_followsDesignOrder() = runTest {
        val vm = vm()
        val seen = mutableListOf(vm.uiState.value.sort)
        repeat(4) {
            vm.onCycleSort()
            runCurrent()
            seen += vm.uiState.value.sort
        }
        // Vòng Tên → Nghệ sĩ → BPM → Mới thêm, bắt đầu từ BPM.
        assertEquals(listOf(SortMode.BPM, SortMode.RECENT, SortMode.TITLE, SortMode.ARTIST, SortMode.BPM), seen)
    }

    @Test
    fun sortLabel_followsMode() = runTest {
        val vm = vm()
        assertEquals(
            UiText.Res(R.string.library_sort, listOf(UiText.Res(R.string.sort_bpm))),
            vm.uiState.value.sortLabel,
        )
        vm.onCycleSort(); runCurrent()
        assertEquals(
            UiText.Res(R.string.library_sort, listOf(UiText.Res(R.string.sort_recent))),
            vm.uiState.value.sortLabel,
        )
    }

    // V08
    @Test
    fun sortBpm_ascending_nullLast() = runTest {
        val vm = vm()
        assertEquals(
            listOf(
                "Tết về rồi", "Vòng quay", "Đi đâu cũng được", "Chạy về phía biển", "Sài Gòn lên đèn",
                "Không cần lời", "Tầng thượng 102", "Mưa sao băng (Club mix)", "Ánh đèn sân khấu", "Phố đêm",
            ),
            vm.titles(),
        )
    }

    // V09 — thứ tự do Node `localeCompare(…,'vi')` và java.text.Collator(vi) cho cùng kết quả
    @Test
    fun sortTitle_vietnameseCollation() = runTest {
        val vm = vm()
        vm.sortTo(SortMode.TITLE); runCurrent()
        assertEquals(
            listOf(
                "Ánh đèn sân khấu", "Chạy về phía biển", "Đi đâu cũng được", "Không cần lời", "Mưa sao băng (Club mix)",
                "Phố đêm", "Sài Gòn lên đèn", "Tầng thượng 102", "Tết về rồi", "Vòng quay",
            ),
            vm.titles(),
        )
    }

    // V10
    @Test
    fun sortArtist_vietnameseCollation() = runTest {
        val vm = vm()
        vm.sortTo(SortMode.ARTIST); runCurrent()
        assertEquals(
            listOf(
                "Bảo Trâm", "Cáo Nhỏ", "DJ Tâm", "Đèn Neon", "Hạ Vũ",
                "Lam Anh", "Mây Tầng Thượng", "Mèo Mun", "Nhóm Mùa Xuân", "Vy Hạ",
            ),
            vm.uiState.value.songs.map { it.subtitle.substringBefore(" · ") },
        )
    }

    @Test
    fun sortRecent_keepsOriginalOrder() = runTest {
        val vm = vm()
        vm.sortTo(SortMode.RECENT); runCurrent()
        assertEquals(tracks.tracks.value.map { it.title }, vm.titles())
    }

    // V11
    @Test
    fun sort_isStableForEqualKeys() = runTest {
        val repo = TestTrackRepository(
            listOf(
                track(0, "Bài C", artist = "Cùng", bpm = 120f),
                track(1, "Bài A", artist = "Cùng", bpm = 120f),
                track(2, "Bài B", artist = "Cùng", bpm = null),
                track(3, "Bài D", artist = "Cùng", bpm = null),
            ),
        )
        val vm = LibraryViewModel(repo, session, playback)
        assertEquals(listOf("Bài C", "Bài A", "Bài B", "Bài D"), vm.titles())
        vm.sortTo(SortMode.ARTIST); runCurrent()
        assertEquals(listOf("Bài C", "Bài A", "Bài B", "Bài D"), vm.titles())
    }

    // V12
    @Test
    fun missingBpm_showsEllipsisBadge_andAnalyzingMeta() = runTest {
        val vm = vm()
        val row = vm.uiState.value.songs.single { it.title == "Phố đêm" }
        assertEquals("…", row.bpmLabel)
        assertFalse(row.hasBpm)
        assertEquals("Hạ Vũ · 03:31", row.subtitle)
        assertEquals("PĐ", row.initials)

        vm.onSongClicked(row.id); runCurrent()
        val sheet = vm.uiState.value.sheet!!
        assertEquals(
            UiText.Res(R.string.song_sheet_meta, listOf("Hạ Vũ", "03:31", UiText.Res(R.string.song_analyzing))),
            sheet.meta,
        )
    }

    @Test
    fun knownBpm_badgeAndMeta() = runTest {
        val vm = vm()
        val row = vm.uiState.value.songs.single { it.title == "Tầng thượng 102" }
        assertEquals("126", row.bpmLabel)
        assertTrue(row.hasBpm)
        vm.onSongClicked(row.id); runCurrent()
        assertEquals(
            UiText.Res(
                R.string.song_sheet_meta,
                listOf("Cáo Nhỏ", "04:40", UiText.Res(R.string.song_bpm_value, listOf("126"))),
            ),
            vm.uiState.value.sheet!!.meta,
        )
    }

    // V13
    @Test
    fun search_ignoresAccentsAndCase() = runTest {
        val vm = vm()
        for (q in listOf("den neon", "Đèn Neon", "ĐÈN NEON", "sao bang")) {
            vm.onQueryChange(q); runCurrent()
            assertEquals("query=$q", listOf("Mưa sao băng (Club mix)"), vm.titles())
        }
    }

    @Test
    fun search_singleAccentedWord_matchesEveryTitleOrArtistContainingIt() = runTest {
        val vm = vm()
        vm.onQueryChange("Đèn"); runCurrent()
        assertEquals(
            setOf("Sài Gòn lên đèn", "Mưa sao băng (Club mix)", "Ánh đèn sân khấu"),
            vm.titles().toSet(),
        )
    }

    @Test
    fun search_matchesTitleAndArtist() = runTest {
        val vm = vm()
        vm.onQueryChange("tang thuong"); runCurrent()
        assertEquals(setOf("Tầng thượng 102", "Sài Gòn lên đèn"), vm.titles().toSet())
        assertEquals(2, vm.count())
    }

    @Test
    fun search_noMatch_emptyAndCountZero_thenClearRestoresAll() = runTest {
        val vm = vm()
        vm.onQueryChange("zzz"); runCurrent()
        assertTrue(vm.uiState.value.songs.isEmpty())
        assertEquals(0, vm.count())
        vm.onQueryChange(""); runCurrent()
        assertEquals(10, vm.count())
        vm.onQueryChange("   "); runCurrent()
        assertEquals(10, vm.count())
    }

    @Test
    fun search_keepsCurrentSort() = runTest {
        val vm = vm()
        vm.onQueryChange("t"); runCurrent()
        val bpms = vm.uiState.value.songs.map { tracks.findById(it.id)!!.bpm ?: Float.MAX_VALUE }
        assertEquals(bpms.sorted(), bpms)
    }

    // V14
    @Test
    fun tabChip_onlyChangesSelection() = runTest {
        val vm = vm()
        val before = vm.titles()
        vm.onTabSelected(LibraryTab.ALBUMS); runCurrent()
        assertEquals(LibraryTab.ALBUMS, vm.uiState.value.tab)
        assertEquals(before, vm.titles())
    }

    // V15
    @Test
    fun loadToDeckB_writesSession_toastsAndClosesSheet() = runTest {
        val vm = vm()
        val id = tracks.tracks.value.single { it.title == "Vòng quay" }.id
        vm.onSongClicked(id); runCurrent()
        assertEquals(id, vm.uiState.value.sheet?.trackId)

        vm.onLoadToDeck(DeckId.B)
        val event = vm.events.first() as LibraryEvent.ShowToast
        assertEquals(ToastTone.DECK_B, event.message.tone)
        assertEquals(UiText.Res(R.string.toast_loaded_deck, listOf("Vòng quay", "B")), event.message.text)
        runCurrent()
        assertNull(vm.uiState.value.sheet)
        assertEquals("Vòng quay", session.session.value?.deckB?.title)
        assertNull(session.session.value?.deckA)
    }

    @Test
    fun loadToDeckA_usesDeckATone() = runTest {
        val vm = vm()
        vm.onSongClicked(tracks.tracks.value[0].id)
        vm.onLoadToDeck(DeckId.A)
        assertEquals(ToastTone.DECK_A, (vm.events.first() as LibraryEvent.ShowToast).message.tone)
        assertEquals("Tết về rồi", session.session.value?.deckA?.title)
    }

    @Test
    fun loadWithoutSheet_doesNothing() = runTest {
        val vm = vm()
        vm.onLoadToDeck(DeckId.A)
        runCurrent()
        assertNull(session.session.value)
    }

    @Test
    fun preview_playsSongAndClosesSheet_repeatDoesNotStop() = runTest {
        val vm = vm()
        val t = tracks.tracks.value[3]
        vm.onSongClicked(t.id); vm.onPreview(); runCurrent()
        assertEquals(t.id, playback.nowPlaying.value?.id)
        assertEquals(t.durationSec, playback.nowPlaying.value?.durationSec)
        assertNull(vm.uiState.value.sheet)
        // "Nghe thử" luôn kết thúc với bài đang phát (App:329), không toggle tắt.
        vm.onSongClicked(t.id); vm.onPreview(); runCurrent()
        assertEquals(t.id, playback.nowPlaying.value?.id)
    }

    @Test
    fun unknownSong_opensNoSheet() = runTest {
        val vm = vm()
        vm.onSongClicked("khong-ton-tai"); runCurrent()
        assertNull(vm.uiState.value.sheet)
    }

    @Test
    fun dismissSheet() = runTest {
        val vm = vm()
        vm.onSongClicked(tracks.tracks.value[0].id); runCurrent()
        vm.onSheetDismissed(); runCurrent()
        assertNull(vm.uiState.value.sheet)
    }
}
