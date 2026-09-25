package com.example.djremixpro.feature.learn

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.djremixpro.R
import com.example.djremixpro.app.DJRemixProApp
import com.example.djremixpro.core.data.LearnRepository
import com.example.djremixpro.core.model.LearnArgs
import com.example.djremixpro.core.model.LearnSegment
import com.example.djremixpro.core.model.LessonStatus
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.core.util.TextUtils2
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** "Học DJ" (App.dc.html:145-189, 318-321). The initial segment comes from the [LearnArgs.SEGMENT] argument. */
class LearnViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val learnRepository: LearnRepository,
) : ViewModel() {

    private val segment: StateFlow<String> = savedStateHandle.getStateFlow(LearnArgs.SEGMENT, LearnSegment.GUIDE.name)

    private val termsQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_TERMS_QUERY, "")

    val uiState: StateFlow<LearnUiState> =
        combine(segment, learnRepository.completedCount, termsQuery) { seg, done, query ->
            buildState(parseSegment(seg), done, query)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            buildState(parseSegment(segment.value), learnRepository.completedCount.value, termsQuery.value),
        )

    fun onSegmentSelected(segment: LearnSegment) {
        savedStateHandle[LearnArgs.SEGMENT] = segment.name
    }

    /** Filters the glossary by term or description, ignoring case and Vietnamese accents. */
    fun onTermsQueryChange(q: String) {
        savedStateHandle[KEY_TERMS_QUERY] = q
    }

    private fun parseSegment(name: String?): LearnSegment =
        LearnSegment.entries.firstOrNull { it.name == name } ?: LearnSegment.GUIDE

    private fun buildState(segment: LearnSegment, completed: Int, termsQuery: String): LearnUiState {
        val lessons = learnRepository.lessons
        val needle = TextUtils2.normalizeForSearch(termsQuery.trim())
        val glossary = if (needle.isEmpty()) learnRepository.glossary else learnRepository.glossary.filter {
            TextUtils2.normalizeForSearch(it.term).contains(needle) ||
                TextUtils2.normalizeForSearch(it.description).contains(needle)
        }
        return LearnUiState(
            segment = segment,
            progressLabel = UiText.Res(R.string.learn_progress, listOf(completed, lessons.size)),
            lessons = lessons.mapIndexed { i, lesson ->
                LessonRow(
                    id = lesson.id,
                    number = i + 1,
                    title = lesson.title,
                    stepsLabel = UiText.Res(R.string.learn_steps, listOf(lesson.steps)),
                    // App.dc.html:319: done for i < completed, current for i == completed.
                    status = when {
                        i < completed -> LessonStatus.DONE
                        i == completed -> LessonStatus.CURRENT
                        else -> LessonStatus.TODO
                    },
                )
            },
            glossary = glossary,
            tips = learnRepository.tips,
            termsQuery = termsQuery,
        )
    }

    companion object {
        private const val KEY_TERMS_QUERY = "terms_query"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DJRemixProApp
                LearnViewModel(createSavedStateHandle(), app.container.learnRepository)
            }
        }
    }
}
