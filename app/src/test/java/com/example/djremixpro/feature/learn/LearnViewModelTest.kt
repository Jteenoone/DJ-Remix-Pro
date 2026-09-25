package com.example.djremixpro.feature.learn

import androidx.lifecycle.SavedStateHandle
import com.example.djremixpro.R
import com.example.djremixpro.core.data.fake.FakeLearnRepository
import com.example.djremixpro.core.model.LearnArgs
import com.example.djremixpro.core.model.LearnSegment
import com.example.djremixpro.core.model.LessonStatus
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** V21–V22: Học DJ (App:145–189, 318–321), segment ban đầu từ argument (App:344). */
@OptIn(ExperimentalCoroutinesApi::class)
class LearnViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private fun vm(segment: String? = null) = LearnViewModel(
        SavedStateHandle(if (segment == null) emptyMap() else mapOf(LearnArgs.SEGMENT to segment)),
        FakeLearnRepository(),
    )

    // V21
    @Test
    fun lessons_statusAndSteps_matchPrototype() = runTest {
        val s = vm().uiState.value
        assertEquals(
            listOf(
                "Nạp bài và phát", "Dùng Sync", "Chuyển bài bằng crossfader", "Dùng EQ", "Tạo loop", "Thêm FX", "Ghi âm",
            ),
            s.lessons.map { it.title },
        )
        assertEquals((1..7).toList(), s.lessons.map { it.number })
        assertEquals((1..7).toList(), s.lessons.map { it.id })
        assertEquals(
            listOf(4, 3, 4, 5, 4, 5, 3).map { UiText.Res(R.string.learn_steps, listOf(it)) },
            s.lessons.map { it.stepsLabel },
        )
        assertEquals(
            listOf(LessonStatus.DONE, LessonStatus.DONE, LessonStatus.CURRENT) + List(4) { LessonStatus.TODO },
            s.lessons.map { it.status },
        )
        assertEquals(1, s.lessons.count { it.status == LessonStatus.CURRENT })
    }

    @Test
    fun progressLabel_is2of7() = runTest {
        assertEquals(UiText.Res(R.string.learn_progress, listOf(2, 7)), vm().uiState.value.progressLabel)
    }

    @Test
    fun glossaryAndTips_counts() = runTest {
        val s = vm().uiState.value
        assertEquals(10, s.glossary.size)
        assertEquals("Beatmatch", s.glossary.last().term)
        assertEquals(listOf("Chọn nhạc", "EQ", "Chuyển bài", "FX"), s.tips.map { it.tag })
    }

    // V22
    @Test
    fun initialSegment_fromArgument() = runTest {
        assertEquals(LearnSegment.TERMS, vm("TERMS").uiState.value.segment)
        assertEquals(LearnSegment.TIPS, vm("TIPS").uiState.value.segment)
    }

    @Test
    fun initialSegment_missingOrGarbage_fallsBackToGuide() = runTest {
        assertEquals(LearnSegment.GUIDE, vm(null).uiState.value.segment)
        assertEquals(LearnSegment.GUIDE, vm("terms").uiState.value.segment)
        assertEquals(LearnSegment.GUIDE, vm("???").uiState.value.segment)
    }

    // V28 — kỳ vọng do Node lọc glossary nguyên văn App:320 (bỏ dấu, lowercase, trim)
    @Test
    fun termsSearch_matchesTermOrDescription_ignoringAccentsAndCase() = runTest {
        val vm = vm()
        fun terms() = vm.uiState.value.glossary.map { it.term }
        val expected = mapOf(
            "NHIP" to listOf("BPM", "Sync", "Beatmatch"),
            "cross" to listOf("Crossfader"),
            "tốc độ" to listOf("Pitch", "Sync"),
            "diem" to listOf("Cue", "Hot cue"),
            "  cue " to listOf("Cue", "Hot cue"),
            "beat" to listOf("Loop", "Beatmatch"),
            "deck" to listOf("Deck", "Crossfader", "EQ", "Sync"),
            "dong bo" to emptyList(),
        )
        for ((q, terms) in expected) {
            vm.onTermsQueryChange(q)
            runCurrent()
            assertEquals("query=$q", terms, terms())
            assertEquals(q, vm.uiState.value.termsQuery)
        }
    }

    @Test
    fun termsSearch_blank_showsAllTen() = runTest {
        val vm = vm()
        vm.onTermsQueryChange("xyz"); runCurrent()
        assertEquals(0, vm.uiState.value.glossary.size)
        vm.onTermsQueryChange("   "); runCurrent()
        assertEquals(10, vm.uiState.value.glossary.size)
        vm.onTermsQueryChange(""); runCurrent()
        assertEquals(10, vm.uiState.value.glossary.size)
    }

    @Test
    fun termsSearch_doesNotAffectLessonsOrTips() = runTest {
        val vm = vm()
        vm.onTermsQueryChange("sync"); runCurrent()
        assertEquals(7, vm.uiState.value.lessons.size)
        assertEquals(4, vm.uiState.value.tips.size)
    }

    @Test
    fun termsQuery_survivesProcessDeath() = runTest {
        val handle = SavedStateHandle(mapOf(LearnArgs.SEGMENT to "TERMS"))
        LearnViewModel(handle, FakeLearnRepository()).onTermsQueryChange("loop")
        val restored = LearnViewModel(handle, FakeLearnRepository())
        assertEquals("loop", restored.uiState.value.termsQuery)
        assertEquals(listOf("Loop"), restored.uiState.value.glossary.map { it.term })
        assertEquals(LearnSegment.TERMS, restored.uiState.value.segment)
    }

    @Test
    fun selectSegment_updatesState() = runTest {
        val vm = vm()
        vm.onSegmentSelected(LearnSegment.TIPS)
        runCurrent()
        assertEquals(LearnSegment.TIPS, vm.uiState.value.segment)
    }
}
