package com.example.djremixpro.core.data.fake

import com.example.djremixpro.core.data.TrackRepository
import com.example.djremixpro.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Songs of App.dc.html:309 in their original order (D-08). Colours are plain ARGB ints so unit tests see them. */
class FakeTrackRepository : TrackRepository {

    private val library: List<Track> = listOf(
        track(0, "Tết về rồi", "Nhóm Mùa Xuân", "03:15", 100f, 0xFF3B5E4A),
        track(1, "Vòng quay", "Lam Anh", "03:58", 118f, 0xFF4A4A5E),
        track(2, "Chạy về phía biển", "Vy Hạ", "03:48", 122f, 0xFF4A3B5E),
        track(3, "Sài Gòn lên đèn", "Mây Tầng Thượng", "04:12", 124f, 0xFF5E4A3B),
        track(4, "Không cần lời", "DJ Tâm", "05:02", 125f, 0xFF3B4A5E),
        track(5, "Tầng thượng 102", "Cáo Nhỏ", "04:40", 126f, 0xFF5E3B4A),
        track(6, "Mưa sao băng (Club mix)", "Đèn Neon", "04:26", 128f, 0xFF2E4E4A),
        track(7, "Phố đêm", "Hạ Vũ", "03:31", null, 0xFF4E452E),
        track(8, "Ánh đèn sân khấu", "Bảo Trâm", "03:44", 130f, 0xFF5E3B3B),
        track(9, "Đi đâu cũng được", "Mèo Mun", "04:05", 120f, 0xFF3B3B5E),
    )

    // App.dc.html:342 only gives title and BPM; artist, duration and cover (deck label colours) are filled in here.
    override val samples: Pair<Track, Track> = Track(
        id = SAMPLE_A_ID, title = "Mẫu 1 · Nhịp nhà", artist = "MixDeck", durationSec = 180,
        bpm = 124f, coverColor = 0xFF3A2C1E.toInt(), addedIndex = -1,
    ) to Track(
        id = SAMPLE_B_ID, title = "Mẫu 2 · Đêm hội", artist = "MixDeck", durationSec = 200,
        bpm = 126f, coverColor = 0xFF1B3432.toInt(), addedIndex = -1,
    )

    private val _tracks = MutableStateFlow(library)
    override val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    override fun findById(id: String): Track? =
        _tracks.value.firstOrNull { it.id == id }
            ?: samples.first.takeIf { it.id == id }
            ?: samples.second.takeIf { it.id == id }

    private fun track(index: Int, title: String, artist: String, duration: String, bpm: Float?, cover: Long) = Track(
        id = "track-${index + 1}",
        title = title,
        artist = artist,
        durationSec = duration.substringBefore(':').toInt() * 60 + duration.substringAfter(':').toInt(),
        bpm = bpm,
        coverColor = cover.toInt(),
        addedIndex = index,
    )

    companion object {
        const val SAMPLE_A_ID = "sample-a"
        const val SAMPLE_B_ID = "sample-b"
    }
}
