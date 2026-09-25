package com.example.djremixpro.feature.learn

import com.example.djremixpro.R
import com.example.djremixpro.core.model.GlossaryTerm
import com.example.djremixpro.core.model.LearnSegment
import com.example.djremixpro.core.model.LessonStatus
import com.example.djremixpro.core.model.Tip
import com.example.djremixpro.core.ui.UiText

data class LessonRow(val id: Int, val number: Int, val title: String, val stepsLabel: UiText, val status: LessonStatus)

data class LearnUiState(
    val segment: LearnSegment = LearnSegment.GUIDE,
    val progressLabel: UiText = UiText.Res(R.string.learn_progress, listOf(0, 0)),
    val lessons: List<LessonRow> = emptyList(),
    val glossary: List<GlossaryTerm> = emptyList(),
    val tips: List<Tip> = emptyList(),
    val termsQuery: String = "",
)
