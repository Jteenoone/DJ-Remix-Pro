package com.example.djremixpro.core.model

import androidx.annotation.ColorInt
import java.time.LocalDateTime

/** A song in the (mock) library. [bpm] is null while the beat analysis is "running" (App.dc.html:313). */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val durationSec: Int,
    val bpm: Float?,
    @param:ColorInt val coverColor: Int,
    val addedIndex: Int,
)

enum class RecordFormat { MP3, WAV }

data class Recording(
    val id: String,
    val name: String,
    val durationSec: Int,
    val createdAt: LocalDateTime,
    val waveSeed: Int,
    val format: RecordFormat,
)

enum class DeckId { A, B }

enum class DeckMode { JOG, PAD }

enum class PadTab { CUE, LOOP, FX, SAMPLER }

enum class EqBand { HIGH, MID, LOW, FILTER }

/** Cycle order of the "Sắp xếp" button is TITLE → ARTIST → BPM → RECENT (App.dc.html:308); default BPM. */
enum class SortMode { TITLE, ARTIST, BPM, RECENT }

enum class LibraryTab { SONGS, PLAYLISTS, ALBUMS, ARTISTS, FOLDERS }

data class MixSession(val deckA: Track?, val deckB: Track?)

/** The single simulated mini-player item. */
data class NowPlaying(val id: String, val title: String, val durationSec: Int, val positionSec: Int)
