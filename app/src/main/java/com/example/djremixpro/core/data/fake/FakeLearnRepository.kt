package com.example.djremixpro.core.data.fake

import com.example.djremixpro.core.data.LearnRepository
import com.example.djremixpro.core.model.GlossaryTerm
import com.example.djremixpro.core.model.Lesson
import com.example.djremixpro.core.model.Tip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lessons, glossary and tips verbatim from App.dc.html:318-321; progress 2/7 is mock (D-18). */
class FakeLearnRepository : LearnRepository {

    override val lessons: List<Lesson> = listOf(
        "Nạp bài và phát" to 4,
        "Dùng Sync" to 3,
        "Chuyển bài bằng crossfader" to 4,
        "Dùng EQ" to 5,
        "Tạo loop" to 4,
        "Thêm FX" to 5,
        "Ghi âm" to 3,
    ).mapIndexed { i, (title, steps) -> Lesson(id = i + 1, title = title, steps = steps) }

    private val _completedCount = MutableStateFlow(2)
    override val completedCount: StateFlow<Int> = _completedCount.asStateFlow()

    override val glossary: List<GlossaryTerm> = listOf(
        GlossaryTerm("Deck", "Một bên của bàn DJ, nơi phát một bài. MixDeck có Deck A bên trái và Deck B bên phải."),
        GlossaryTerm("Crossfader", "Thanh trượt ngang dưới cùng. Kéo về A để nghe Deck A, về B để nghe Deck B, để ở giữa để nghe cả hai."),
        GlossaryTerm("BPM", "Số nhịp mỗi phút, cho biết bài nhanh hay chậm. Hai bài có BPM gần nhau dễ mix hơn."),
        GlossaryTerm("Cue", "Điểm bạn đánh dấu trong bài. Bấm Cue để quay về điểm đó và bắt đầu lại."),
        GlossaryTerm("Hot cue", "Tối đa 8 điểm đánh dấu gán vào pad. Chạm pad để nhảy ngay tới điểm đó."),
        GlossaryTerm("Loop", "Lặp lại một đoạn dài theo số beat, ví dụ 4 hoặc 8 beat."),
        GlossaryTerm("Pitch", "Chỉnh tốc độ phát nhanh hơn hoặc chậm hơn, tính bằng phần trăm."),
        GlossaryTerm("EQ", "Chỉnh âm cao (High), trung (Mid) và trầm (Low) của từng deck."),
        GlossaryTerm("Sync", "Tự chỉnh tốc độ một deck cho khớp nhịp với deck còn lại."),
        GlossaryTerm("Beatmatch", "Làm cho nhịp của hai bài trùng nhau để chuyển bài mượt."),
    )

    override val tips: List<Tip> = listOf(
        Tip(
            "Chọn nhạc", "1 phút đọc", "Chọn hai bài có BPM gần nhau",
            "Mở Thư viện, sắp xếp theo BPM và chọn hai bài chênh nhau dưới 3%. Sync sẽ không phải kéo tốc độ quá nhiều.",
        ),
        Tip(
            "EQ", "1 phút đọc", "Hạ Low của bài cũ khi đưa bài mới vào",
            "Hai tiếng bass chồng lên nhau sẽ bị rền. Khi kéo crossfader sang bài mới, vặn Low của bài cũ xuống dần.",
        ),
        Tip(
            "Chuyển bài", "2 phút đọc", "Chuyển bài ở đầu một đoạn 8 hoặc 16 beat",
            "Nhạc dance thường đổi đoạn sau mỗi 8 hoặc 16 beat. Bắt đầu kéo crossfader ở đầu đoạn thì nghe tự nhiên hơn.",
        ),
        Tip(
            "FX", "1 phút đọc", "Dùng Echo để kết thúc bài cũ",
            "Bật Echo 1 beat trên bài cũ rồi hạ fader. Tiếng vọng che khoảng trống khi bài mới vào.",
        ),
    )
}
