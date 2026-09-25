package com.example.djremixpro.core.model

enum class LearnSegment { GUIDE, TERMS, TIPS }

/** [id] runs 1..7 (App.dc.html:318-319). */
data class Lesson(val id: Int, val title: String, val steps: Int)

enum class LessonStatus { DONE, CURRENT, TODO }

data class GlossaryTerm(val term: String, val description: String)

data class Tip(val tag: String, val readTime: String, val title: String, val body: String)
