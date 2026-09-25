package com.example.djremixpro.core.model

enum class MixerEntry { DEFAULT, SAMPLES, LESSON }

/** Argument names of the `mixerActivity` destination in nav_graph.xml. */
object MixerArgs {
    const val ENTRY = "entry"
    const val LESSON_ID = "lessonId"
}

/** Argument of `learnFragment`; the value is a [LearnSegment.name]. */
object LearnArgs {
    const val SEGMENT = "segment"
}
