# IMPLEMENTATION_CONTRACT — MixDeck (DJ Remix Pro)

Hợp đồng chung cho **ui-developer**, **logic-developer**, **tester**, dựa trên `DESIGN_ANALYSIS.md`, `design/DESIGNER_REPORT.md` và `DECISIONS.md`.
Muốn đổi bất kỳ chữ ký, tên, ID hay quyền sở hữu nào trong file này: gửi đề xuất cho lead. Lead cập nhật file này và báo các bên liên quan **trước khi** triển khai. Mọi thay đổi được ghi ở cuối file (Changelog).

Base package: `com.example.djremixpro` (viết tắt `▸`). Kotlin, XML Views, ViewBinding, Navigation Fragment, không Compose, không WebView, không Hilt (DI thủ công qua `AppContainer`).

---

## 1. Quyền sở hữu file

| Chủ | File/thư mục (dưới `app/src/main/` nếu không ghi khác) |
|---|---|
| **lead** | Gradle (`*.gradle.kts`, `gradle/`, `gradle.properties`), `AndroidManifest.xml`, `▸/app/DJRemixProApp.kt`, `▸/app/AppContainer.kt`, `▸/MainActivity.kt`, `res/layout/activity_main.xml`, `res/navigation/nav_graph.xml`, `res/menu/*`, `res/values*/colors.xml`, `dimens.xml`, `strings.xml`, `themes.xml`, `styles.xml`, `res/font/*`, `res/drawable/ic_*.xml`, `res/color/shell_*.xml`, `res/xml/*`, `▸/core/ui/ShellContracts.kt`, các file `*.md` ở gốc dự án |
| **logic-developer** | `▸/core/model/**`, `▸/core/data/**`, `▸/core/util/**`, `▸/core/ui/UiText.kt`, `▸/feature/**/*ViewModel.kt`, `▸/feature/**/*UiState.kt` (chứa UiState + Event + class phụ của màn đó) |
| **ui-developer** | `▸/core/ui/widget/**`, `▸/core/ui/ext/**`, `▸/feature/**/*Fragment.kt`, `*Adapter.kt`, `*Sheet.kt`, `*Dialog.kt`, `▸/feature/mixer/MixerActivity.kt`, `▸/feature/mixer/view/**`, `res/layout/**` (trừ `activity_main.xml`), `res/drawable/**` (trừ `ic_*`), `res/color/**` (trừ `shell_*`), `res/anim/**`, `res/animator/**`, `res/values/attrs.xml` |
| **tester** | `app/src/test/**`, `app/src/androidTest/**`, `TEST_MATRIX.md` |

- Mọi agent được **đọc** toàn bộ dự án. Muốn sửa file của người khác: gửi yêu cầu cho chủ file, hoặc cho lead nếu là tài nguyên dùng chung.
- Chuỗi mới, dimen, style, màu, icon `ic_*`: gửi lead (tên + giá trị). Không hardcode chuỗi hiển thị trong layout hay Kotlin; ngoại lệ là dữ liệu mock và số đã định dạng.
- **Gradle** (D-06): không tự chạy. Nhắn lead "xin lượt Gradle" kèm task cần chạy. Chỉ chạy sau khi lead trả lời "CẤP LƯỢT", chạy xong báo "TRẢ LƯỢT" kèm kết quả. Task mặc định: `.\gradlew.bat :app:compileDebugKotlin --console=plain` (PowerShell, từ `D:\App_KT\DJRemixPro`).
- Tham chiếu lẫn nhau được phép (ví dụ ui dùng `core.util.WaveformGenerator`), nhưng không sửa.

## 2. Package và màn hình

| Feature | Fragment / Activity (ui) | Layout (ui) | ViewModel + UiState file (logic) |
|---|---|---|---|
| shell | `MainActivity` (**lead**) | `activity_main.xml` (**lead**) | `feature/shell/MainViewModel.kt`, `ShellUiState.kt` |
| splash | `feature/splash/SplashFragment` | `fragment_splash.xml` | `SplashViewModel`, `SplashUiState.kt` |
| onboarding | `feature/onboarding/OnboardingFragment` | `fragment_onboarding.xml` | `OnboardingViewModel`, `OnboardingUiState.kt` |
| permission | `feature/permission/PermissionFragment` | `fragment_permission.xml` | `PermissionViewModel`, `PermissionUiState.kt` |
| home | `feature/home/HomeFragment` | `fragment_home.xml` | `HomeViewModel`, `HomeUiState.kt` |
| library | `feature/library/LibraryFragment`, `SongAdapter`, `SongSheet` | `fragment_library.xml`, `item_song.xml`, `sheet_song.xml` | `LibraryViewModel`, `LibraryUiState.kt` |
| recordings | `feature/recordings/RecordingsFragment`, `RecordingAdapter`, `RecordingSheet`, `RenameDialog` | `fragment_recordings.xml`, `item_recording.xml`, `sheet_recording.xml`, `dialog_rename.xml` | `RecordingsViewModel`, `RecordingsUiState.kt` |
| learn | `feature/learn/LearnFragment`, `LessonAdapter`, `GlossaryAdapter`, `TipAdapter` | `fragment_learn.xml`, `item_lesson.xml`, `item_glossary.xml`, `item_tip.xml` | `LearnViewModel`, `LearnUiState.kt` |
| settings | `feature/settings/SettingsFragment` | `fragment_settings.xml` (+ item layout tuỳ ý) | `SettingsViewModel`, `SettingsUiState.kt` |
| language | `feature/language/LanguageFragment`, `LanguageAdapter` | `fragment_language.xml`, `item_language.xml` | `LanguageViewModel`, `LanguageUiState.kt` |
| mixer | `feature/mixer/MixerActivity` (+ `feature/mixer/view/**`) | `activity_mixer.xml`, `include_deck.xml`, `include_mixer_center.xml`, `view_library_panel.xml`, `sheet_fx.xml`, `dialog_save_mix.xml`, `view_coach.xml`, `item_pad.xml`… | `MixerViewModel`, `MixerUiState.kt` |

Custom view (ui, `core/ui/widget/`): `JogWheelView`, `VinylArtView` (hoặc mode tĩnh của Jog), `KnobView`, `VerticalFaderView`, `CrossfaderView`, `ScrollingWaveformView`, `OverviewBarView`, `VuMeterView`, `XYPadView`, `MiniWaveformView`, `CoachOverlayView`, `MiniPlayerView`, `ToastView`. API công khai của hai view lead dùng:
```kotlin
class MiniPlayerView : FrameLayout /* hoặc ConstraintLayout */ {
    fun bind(state: MiniPlayerUi?)              // null → visibility GONE
    fun setOnStopClickListener(l: () -> Unit)
}
class ToastView : FrameLayout {
    fun show(message: ToastMessage)             // tự ẩn sau 2200 ms; gọi lại thì thay nội dung + reset timer
}
```

## 3. Shell contracts (lead) — `▸/core/ui/ShellContracts.kt`
```kotlin
enum class ToastTone { SUCCESS, DECK_A, DECK_B, ERROR }   // icon ✓ màu: md_deck_b, md_deck_a, md_deck_b, md_rec
data class ToastMessage(val text: UiText, val tone: ToastTone = ToastTone.SUCCESS)
interface ToastHost { fun showToast(message: ToastMessage) }          // MainActivity, MixerActivity implement
interface ShellNavigator { fun openTab(@IdRes destinationId: Int, args: Bundle? = null) } // MainActivity implement
```
Fragment gửi toast: `(requireActivity() as ToastHost).showToast(msg)`. Chuyển tab từ Home (ô Học DJ, Xem tất cả): `(requireActivity() as ShellNavigator).openTab(R.id.learnFragment, bundleOf(LearnArgs.SEGMENT to LearnSegment.TERMS.name))`.

## 4. Model (logic) — `▸/core/model/`
```kotlin
data class Track(val id: String, val title: String, val artist: String, val durationSec: Int,
                 val bpm: Float?, @ColorInt val coverColor: Int, val addedIndex: Int)
enum class RecordFormat { MP3, WAV }
data class Recording(val id: String, val name: String, val durationSec: Int,
                     val createdAt: java.time.LocalDateTime, val waveSeed: Int, val format: RecordFormat)
enum class DeckId { A, B }
enum class DeckMode { JOG, PAD }
enum class PadTab { CUE, LOOP, FX, SAMPLER }
enum class EqBand { HIGH, MID, LOW, FILTER }
enum class SortMode { BPM, RECENT, TITLE, ARTIST }  // thứ tự khai báo tuỳ ý; vòng Sắp xếp: TITLE→ARTIST→BPM→RECENT→TITLE; mặc định BPM
enum class LibraryTab { SONGS, PLAYLISTS, ALBUMS, ARTISTS, FOLDERS }
enum class LearnSegment { GUIDE, TERMS, TIPS }
data class Lesson(val id: Int, val title: String, val steps: Int)            // id 1..7
enum class LessonStatus { DONE, CURRENT, TODO }
data class GlossaryTerm(val term: String, val description: String)
data class Tip(val tag: String, val readTime: String, val title: String, val body: String)
data class AppLanguage(val tag: String, val nativeName: String, val vietnameseName: String)
object Languages { val all: List<AppLanguage> }   // 17 mục, đúng thứ tự thiết kế; tag: vi, hi, es, pt-BR, en, pt-PT, fr, ar, bn, ru, de, ja, tr, ko, id, zh-Hans, zh-Hant
enum class ThemeMode { DARK, LIGHT, SYSTEM }
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK, val languageTag: String = "vi",
    val latencyMs: Int = 40, val precue: Boolean = false, val masterVolumePct: Int = 80,
    val recordFormat: RecordFormat = RecordFormat.MP3, val recordQualityKbps: Int = 320,
    val saveFolder: String = "Music/MixDeck", val pitchRangePct: Int = 8,
    val defaultJogMode: DeckMode = DeckMode.JOG, val haptic: Boolean = true,
    val keepScreenOn: Boolean = true, val onboardingDone: Boolean = false,
)
object SettingsOptions {
    val latencyMs = listOf(20, 40, 60, 80, 100, 150, 200)
    val masterVolumePct = listOf(50, 60, 70, 80, 90, 100)
    val recordQualityKbps = listOf(128, 192, 256, 320)
    val pitchRangePct = listOf(6, 8, 10, 16, 50)
}
data class MixSession(val deckA: Track?, val deckB: Track?)
data class NowPlaying(val id: String, val title: String, val durationSec: Int, val positionSec: Int)
enum class MixerEntry { DEFAULT, SAMPLES, LESSON }
object MixerArgs { const val ENTRY = "entry"; const val LESSON_ID = "lessonId" }   // tên trùng argument trong nav_graph
object LearnArgs { const val SEGMENT = "segment" }                                 // giá trị = LearnSegment.name
```

## 5. UiText (logic) — `▸/core/ui/UiText.kt`
```kotlin
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val value: String) : UiText
    fun resolve(context: Context): String
}
```
Quy tắc: ViewModel trả **UiText** cho chữ cần dịch, trả **String** cho số/định dạng không phụ thuộc ngôn ngữ ("124.0", "03:12", "−02:31", "+0.0%"). Định dạng số dùng `Locale.US`.

## 6. Repository (logic) — `▸/core/data/` (interface) + `▸/core/data/fake/`, `▸/core/data/settings/` (impl)
```kotlin
interface TrackRepository {
    val tracks: StateFlow<List<Track>>                 // 10 bài, App:309 (thứ tự gốc = addedIndex)
    val samples: Pair<Track, Track>                    // "Mẫu 1 · Nhịp nhà" 124 (A), "Mẫu 2 · Đêm hội" 126 (B)
    fun findById(id: String): Track?
}
interface RecordingRepository {
    val recordings: StateFlow<List<Recording>>          // mới nhất trước; seed 4 bản App:314 (waveSeed = i*13+5)
    suspend fun add(name: String, durationSec: Int, format: RecordFormat): Result<Recording>
    suspend fun rename(id: String, newName: String): Result<Unit>   // tên trim rỗng → Result.failure(IllegalArgumentException)
    suspend fun delete(id: String): Result<Unit>                     // id không tồn tại → failure(NoSuchElementException)
}
interface LearnRepository {
    val lessons: List<Lesson>; val completedCount: StateFlow<Int>    // 7 bài, 2 đã xong
    val glossary: List<GlossaryTerm>; val tips: List<Tip>
}
interface SettingsRepository {
    val settings: Flow<AppSettings>                     // lỗi IO → phát AppSettings() mặc định, không throw
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
interface MixSessionRepository {
    val session: StateFlow<MixSession?>                 // seed: A "Sài Gòn lên đèn", B "Mưa sao băng (Club mix)"
    fun loadTrack(deck: DeckId, track: Track)          // tạo session nếu null
    fun setSession(session: MixSession?)
}
interface PlaybackRepository {                           // mini player giả lập, chỉ 1 bản phát
    val nowPlaying: StateFlow<NowPlaying?>
    fun toggle(id: String, title: String, durationSec: Int)   // cùng id đang phát → stop; khác → phát từ 0
    fun stop()
}
```
Impl (tên và constructor cố định để lead viết `AppContainer`):
`FakeTrackRepository()`, `FakeRecordingRepository(clock: java.time.Clock = Clock.systemDefaultZone())`, `FakeLearnRepository()`, `DataStoreSettingsRepository(context: Context)`, `InMemoryMixSessionRepository(trackRepository: TrackRepository)`, `SimulatedPlaybackRepository(scope: CoroutineScope)` (tăng `positionSec` mỗi 1 s, hết bài → `null`).

**Quy tắc lỗi:** thao tác ghi trả `Result`. ViewModel gặp `failure` → phát event toast `UiText.Res(R.string.toast_generic_error)` với tone `ERROR`, và giữ state cũ. Flow không bao giờ throw ra UI.

`AppContainer` (lead) cung cấp các property: `trackRepository`, `recordingRepository`, `learnRepository`, `settingsRepository`, `mixSessionRepository`, `playbackRepository`, `appScope: CoroutineScope`. Truy cập: `(application as DJRemixProApp).container`.
Mỗi ViewModel có `companion object { val Factory: ViewModelProvider.Factory = viewModelFactory { initializer { … this[APPLICATION_KEY] as DJRemixProApp … createSavedStateHandle() … } } }`. ui lấy bằng `by viewModels { XxxViewModel.Factory }`; `MainViewModel` lấy bằng `by viewModels` trong MainActivity.

## 7. Util (logic) — `▸/core/util/`
```kotlin
object TimeFormat { fun mmss(sec: Int): String; fun remaining(sec: Int): String /* "−02:31" (U+2212) */; fun date(d: LocalDate): String /* dd/MM/yyyy */ }
object TextUtils2 { fun initials(title: String): String /* App:307 */; fun normalizeForSearch(s: String): String /* bỏ dấu, đ→d, lowercase */ }
object ParkMiller { fun sequence(seed: Int): () -> Float }   // s = s*16807 % 2147483647 (Long); trả s/2147483647
object WaveformGenerator {
    fun deck(seed: Int): FloatArray   // 304 chiều cao nửa-bar (1..8) theo Mixer:399-409; view nhân đôi để cuộn
    fun mini(seed: Int): FloatArray   // 16 chiều cao (App:278), dùng viewBox 56×28
}
object MixMath {
    fun syncPitchPct(ownBpm: Float, otherBpm: Float): Float          // (other/own − 1)·100
    fun effectiveBpm(bpm: Float, pitchPct: Float): Float
    fun pitchLabel(pitchPct: Float): String                           // "+0.0%", "−3.1%"
    fun pitchPosition(pitchPct: Float, rangePct: Int): Float          // 0..1 từ trên xuống: 0.5 − pitch/range·0.5, clamp
    fun spinPeriodSec(pitchPct: Float): Float                         // 1.8/(1+pitch/100)
    fun waveScrollDpPerSec(bpm: Float): Float                         // 32·bpm/60
    fun isBpmMatch(candidateBpm: Float?, referenceBpm: Float?): Boolean   // ≤ 3%
    fun clamp01(v: Float): Float
    fun eqDb(v: Float): Float                                         // (v−0.5)·27 (D-21)
    fun fxReadout(x: Float, y: Float): Pair<String, Int>              // ("1/2", 70) theo Mixer:486-488
    fun crossfaderFromTouch(x: Float, left: Float, width: Float, knobPx: Float): Float  // clamp((x−left−knob/2)/(width−knob))
}
```

## 8. UiState, sự kiện, hàm công khai (logic viết; ui tiêu thụ)
Tất cả ViewModel expose `val uiState: StateFlow<XxxUiState>`, event một lần: `val events: Flow<XxxEvent>` (Channel BUFFERED + receiveAsFlow). ui thu thập bằng `viewLifecycleOwner.repeatOnLifecycle(STARTED)` (Activity: `repeatOnLifecycle` của activity).

### 8.1 Shell
```kotlin
data class MiniPlayerUi(val id: String, val title: String, val timeLabel: String /* "01:17 / 03:12" */, val progress: Float)
data class ShellUiState(val miniPlayer: MiniPlayerUi? = null)
class MainViewModel { val uiState; fun onMiniPlayerStop() }
```
### 8.2 Splash / Onboarding / Permission
```kotlin
enum class SplashDestination { ONBOARDING, HOME }
data class SplashUiState(val destination: SplashDestination? = null)      // ≥ 800 ms sau khi đọc settings
class SplashViewModel { val uiState }

data class OnboardingUiState(val page: Int = 0, val pageCount: Int = 3, val canSkip: Boolean = true /* trang 0,1 */)
sealed interface OnboardingEvent { data object GoToPermission : OnboardingEvent }
class OnboardingViewModel { val uiState; val events; fun onNext(); fun onSkip() }   // onNext ở trang cuối hoặc onSkip → GoToPermission

data class PermissionUiState(val isBusy: Boolean = false)
sealed interface PermissionEvent { data object RequestPermission : PermissionEvent; data object GoHome : PermissionEvent }
class PermissionViewModel { val uiState; val events
    fun onAllowClicked()                       // → RequestPermission
    fun onPermissionResult(granted: Boolean)   // lưu onboardingDone=true → GoHome
    fun onUseSamplesClicked() }                // lưu onboardingDone=true → GoHome
```
### 8.3 Home
```kotlin
data class DeckTrackRow(val deck: DeckId, val title: String, val bpmLabel: String /* "124" */)
data class RecentMixRow(val id: String, val name: String, val durationLabel: String, val isPlaying: Boolean)
data class HomeUiState(val isNewUser: Boolean = false, val samples: List<DeckTrackRow> = emptyList(),
    val session: List<DeckTrackRow> = emptyList(), val showAd: Boolean = true, val recent: List<RecentMixRow> = emptyList())
class HomeViewModel { val uiState; fun onRecentPlayToggle(id: String) }
```
Điều hướng thuần do Fragment tự gọi: Vào bàn DJ → mixer `DEFAULT`; Mix với bài mẫu → `SAMPLES`; Tiếp tục phiên → `DEFAULT`; ô Học DJ, Xem tất cả → `ShellNavigator.openTab`.
### 8.4 Library
```kotlin
data class SongRow(val id: String, val title: String, val subtitle: String /* "Nghệ sĩ · 03:15" */,
    val initials: String, @ColorInt val coverColor: Int, val bpmLabel: String /* "124" | "…" */, val hasBpm: Boolean)
data class SongSheetUi(val trackId: String, val title: String, val meta: UiText, val initials: String, @ColorInt val coverColor: Int)
data class LibraryUiState(val query: String = "", val tab: LibraryTab = LibraryTab.SONGS, val sort: SortMode = SortMode.BPM,
    val sortLabel: UiText, val countLabel: UiText /* Plural library_count */, val songs: List<SongRow> = emptyList(), val sheet: SongSheetUi? = null)
sealed interface LibraryEvent { data class ShowToast(val message: ToastMessage) : LibraryEvent }
class LibraryViewModel { val uiState; val events
    fun onQueryChange(q: String); fun onTabSelected(tab: LibraryTab); fun onCycleSort()
    fun onSongClicked(id: String); fun onSheetDismissed()
    fun onLoadToDeck(deck: DeckId)   // MixSessionRepository.loadTrack + toast "Đã nạp “…” vào Deck X" (tone DECK_A/DECK_B) + đóng sheet
    fun onPreview() }                // PlaybackRepository.toggle + đóng sheet
```
### 8.5 Recordings
```kotlin
data class RecordingRow(val id: String, val name: String, val meta: UiText, val waveSeed: Int, val isPlaying: Boolean)
data class RecordingSheetUi(val id: String, val title: String, val meta: String /* "03:12 · 25/09/2026" */)
data class RenameDialogUi(val id: String, val currentName: String)
data class DeleteConfirmUi(val id: String, val name: String)
data class RecordingsUiState(val items: List<RecordingRow> = emptyList(), val isEmpty: Boolean = false,
    val sheet: RecordingSheetUi? = null, val renameDialog: RenameDialogUi? = null, val deleteConfirm: DeleteConfirmUi? = null)
sealed interface RecordingsEvent { data class ShowToast(val message: ToastMessage) : RecordingsEvent }
class RecordingsViewModel { val uiState; val events
    fun onRowClicked(id: String); fun onMenuClicked(id: String); fun onSheetDismissed()
    fun onRenameClicked(); fun onRenameConfirmed(newName: String); fun onRenameDismissed()
    fun onShareClicked()                        // toast share unavailable (D-13)
    fun onDeleteClicked(); fun onDeleteConfirmed(); fun onDeleteDismissed() }
```
### 8.6 Learn
```kotlin
data class LessonRow(val id: Int, val number: Int, val title: String, val stepsLabel: UiText, val status: LessonStatus)
data class LearnUiState(val segment: LearnSegment = LearnSegment.GUIDE, val progressLabel: UiText,
    val lessons: List<LessonRow> = emptyList(), val glossary: List<GlossaryTerm> = emptyList(), val tips: List<Tip> = emptyList())
class LearnViewModel(savedStateHandle) { val uiState; fun onSegmentSelected(segment: LearnSegment) }  // segment ban đầu từ arg LearnArgs.SEGMENT
```
Bấm bài học → Fragment mở mixer với `entry=LESSON`, `lessonId=id`. "Xem trên mixer" → `DEFAULT`.
### 8.7 Settings / Language
```kotlin
data class SettingsUiState(val settings: AppSettings = AppSettings(), val languageName: String = "Tiếng Việt")
sealed interface SettingsEvent { data class ShowToast(val message: ToastMessage) : SettingsEvent }
enum class SettingToggle { PRECUE, HAPTIC, KEEP_SCREEN_ON }
class SettingsViewModel { val uiState; val events
    fun onThemeSelected(mode: ThemeMode); fun onToggle(toggle: SettingToggle)
    fun onLatencySelected(ms: Int); fun onMasterVolumeSelected(pct: Int); fun onFormatSelected(f: RecordFormat)
    fun onQualitySelected(kbps: Int); fun onPitchRangeSelected(pct: Int); fun onJogModeSelected(mode: DeckMode)
    fun onFolderClicked(); fun onSupportClicked() }     // toast (D-14); "Quyền truy cập" và "Ngôn ngữ": Fragment tự xử lý

data class LanguageRow(val tag: String, val nativeName: String, val vietnameseName: String, val isSelected: Boolean)
data class LanguageUiState(val query: String = "", val items: List<LanguageRow> = emptyList())
sealed interface LanguageEvent { data class ShowToast(val message: ToastMessage) : LanguageEvent }
class LanguageViewModel { val uiState; val events; fun onQueryChange(q: String); fun onLanguageSelected(tag: String) }
```
Áp dụng theme và locale (lead): `DJRemixProApp` thu `settings`, khi `themeMode`/`languageTag` đổi thì gọi `AppCompatDelegate`. ViewModel chỉ ghi repository.
### 8.8 Mixer
```kotlin
enum class PadState { OFF, ON, HELD, NEUTRAL_ON }
enum class PadStyle { NUMERIC, NUMERIC_SMALL, TEXT }       // Chakra 18 / Chakra 14 / Be Vietnam 12
data class PadUi(val label: UiText, val sub: UiText?, val state: PadState, val style: PadStyle)
data class DeckTrackUi(val trackId: String, val title: String, val artist: String, val initials: String, val bpm: Float)
data class DeckUiState(
    val id: DeckId, val track: DeckTrackUi? = null,
    val isPlaying: Boolean = false, val isSync: Boolean = false, val isScratch: Boolean = true, val isTouching: Boolean = false,
    val mode: DeckMode = DeckMode.JOG, val padTab: PadTab = PadTab.CUE, val pads: List<PadUi> = emptyList(),   // luôn 8 khi có track
    val pitchPct: Float = 0f, val pitchRangePct: Int = 8, val pitchLabel: String = "+0.0%", val isPitchShifted: Boolean = false,
    val pitchPosition: Float = 0.5f, val bpmLabel: String = "—", val remainingLabel: String = "−−:−−",
    val spinPeriodSec: Float = 1.8f, val isSpinning: Boolean = false,
    val waveSeed: Int = 7, val waveBpm: Float = 120f, val progress: Float = 0f, val loopBeats: Float? = null)
data class KnobUi(val band: EqBand, val value: Float, val isKill: Boolean, val valueLabel: String /* "+3.5 dB" | "Kill" | "LPF 40%" */)
data class ChannelsUiState(val crossfader: Float = 0.5f, val volumeA: Float = 0.86f, val volumeB: Float = 0.72f,
    val eqA: List<KnobUi> = emptyList(), val eqB: List<KnobUi> = emptyList(), val vuActiveA: Boolean = false, val vuActiveB: Boolean = false)
data class RecUiState(val isRecording: Boolean = false, val elapsedSec: Int = 0, val label: String = "REC")
data class LibraryPanelRow(val trackId: String, val title: String, val artist: String, val durationLabel: String,
    val bpmLabel: String, val initials: String, @ColorInt val coverColor: Int, val isBpmMatch: Boolean)
data class LibraryPanelUi(val target: DeckId, val rows: List<LibraryPanelRow>)
data class FxSheetUi(val deck: DeckId, val fxName: String, val enabled: Boolean, val x: Float, val y: Float,
    val readout: String /* "1/2 beat · 70%" */, val wetDry: Float = 0.6f, val beatLengths: List<String>, val selectedBeatLength: Int)
data class SaveDialogUi(val durationLabel: String, val name: String, val format: RecordFormat, val qualityKbps: Int, val showWavNote: Boolean)
enum class CoachTarget { SYNC_B }
data class CoachUi(val step: Int, val totalSteps: Int, val isDone: Boolean, val title: UiText, val body: UiText, val target: CoachTarget)
data class MixerUiState(
    val deckA: DeckUiState = DeckUiState(DeckId.A), val deckB: DeckUiState = DeckUiState(DeckId.B),
    val channels: ChannelsUiState = ChannelsUiState(), val rec: RecUiState = RecUiState(),
    val libraryPanel: LibraryPanelUi? = null, val fxSheet: FxSheetUi? = null, val saveDialog: SaveDialogUi? = null,
    val coach: CoachUi? = null, val hapticsEnabled: Boolean = true, val keepScreenOn: Boolean = true)
sealed interface MixerEvent {
    data class ShowToast(val message: ToastMessage) : MixerEvent
    data object Close : MixerEvent }
class MixerViewModel(savedStateHandle /* MixerArgs.ENTRY, MixerArgs.LESSON_ID */, …, clock: Clock) {
    val uiState; val events
    // deck
    fun onPlayToggle(deck: DeckId); fun onCue(deck: DeckId); fun onSyncToggle(deck: DeckId); fun onScratchToggle(deck: DeckId)
    fun onModeChange(deck: DeckId, mode: DeckMode); fun onPadTabSelected(deck: DeckId, tab: PadTab)
    fun onPadTapped(deck: DeckId, index: Int); fun onPadLongPressed(deck: DeckId, index: Int)   // FX tab → mở fxSheet
    fun onJogTouch(deck: DeckId, touching: Boolean); fun onJogTapped(deck: DeckId)               // trống → libraryPanel
    fun onJogRotate(deck: DeckId, deltaDegrees: Float)                                             // D-19
    fun onPitchChanged(deck: DeckId, pitchPct: Float); fun onOverviewSeek(deck: DeckId, fraction: Float)
    // channels
    fun onVolumeChanged(deck: DeckId, value: Float); fun onEqChanged(deck: DeckId, band: EqBand, value: Float)
    fun onEqReset(deck: DeckId, band: EqBand); fun onEqKill(deck: DeckId, band: EqBand); fun onCrossfaderChanged(value: Float)
    fun onCrossfaderReset()
    // top bar & overlays
    fun onBack(); fun onQuickSettings(); fun onRecToggle()
    fun onLibraryClose(); fun onLibraryLoad(trackId: String, deck: DeckId)
    fun onFxChanged(x: Float, y: Float); fun onFxToggle(); fun onFxBeatLengthSelected(index: Int); fun onFxReset(); fun onFxClose()
    fun onSaveNameChanged(name: String); fun onSaveFormatSelected(format: RecordFormat); fun onSaveConfirmed(); fun onSaveDiscarded()
    fun onCoachNext(); fun onCoachSkip()
}
```
Mixer (D-07, D-11 → D-22): khởi tạo theo `entry`. `DEFAULT`: lấy deck từ `MixSessionRepository.session`, cả hai deck dừng. `SAMPLES`: nạp 2 bài mẫu. `LESSON` với `lessonId == 2`: bảo đảm hai deck có bài, A đang phát, B chưa sync, bật `coach`. Timer 1 s: tăng vị trí phát (theo tốc độ 1 + pitch/100), tăng `rec.elapsedSec`. Nạp bài → cập nhật `MixSessionRepository`. Số đếm 8 pad, nhãn pad theo Mixer:422–433.

## 9. Navigation (lead) — `res/navigation/nav_graph.xml`
Start: `homeFragment`. Lần đầu (chưa xong onboarding), MainActivity điều hướng sang `splashFragment` (D-25). SplashViewModel vẫn trả ONBOARDING hoặc HOME. Destination ID: `splashFragment`, `onboardingFragment`, `permissionFragment`, `homeFragment`, `libraryFragment`, `recordingsFragment`, `learnFragment` (arg `segment`: string, mặc định `"GUIDE"`), `settingsFragment`, `languageFragment`, `mixerActivity` (activity, arg `entry`: string mặc định `"DEFAULT"`, `lessonId`: integer mặc định `-1`).
Action: `action_splash_to_onboarding`, `action_splash_to_home` (popUpTo splash inclusive), `action_onboarding_to_permission`, `action_permission_to_home` (popUpTo onboardingFragment inclusive), `action_settings_to_language`, global `action_global_settings`, `action_global_mixer`.
Mở mixer: `findNavController().navigate(R.id.action_global_mixer, bundleOf(MixerArgs.ENTRY to MixerEntry.SAMPLES.name, MixerArgs.LESSON_ID to -1))`.
Bottom nav: `res/menu/bottom_nav.xml`, id item = id destination (`homeFragment`, `libraryFragment`, `recordingsFragment`, `learnFragment`). Top bar, bottom nav, mini player và toast nằm trong `activity_main.xml`. Fragment **không** tự vẽ top bar, và phải để nội dung cuộn trong vùng `nav_host`. Inset hệ thống do MainActivity xử lý.
Top bar: tiêu đề lấy từ `android:label` của destination. Back hiện ở settings và language. Icon cài đặt hiện ở 4 tab chính. Splash, onboarding và permission ẩn toàn bộ chrome.

## 10. Quy ước resource
- Layout: `fragment_*`, `activity_*`, `item_*`, `sheet_*`, `dialog_*`, `view_*`, `include_*`. ID view: `snake_case` mô tả, ví dụ `button_open_mixer`, `text_title`, `recycler_songs`.
- Drawable ui: `bg_*` (shape/nền), `sel_*` (selector), `shape_*`. Icon `ic_*` thuộc lead: có sẵn 28 icon từ designer, xem `res/drawable/ic_*.xml`. Icon là glyph trắng, tô màu bằng `app:tint`/`iconTint`.
- Màu: chỉ dùng `@color/md_*`, không hardcode hex trong layout. Alpha variant có sẵn (`md_deck_a_18`…).
- Chữ: `TextAppearance.MixDeck.*` (Heading, Subheading[.Medium], Body[.Medium|.Muted], Button, Caption[.Medium|.Semibold], Display, Title, Numeric[.Time|.Rec|.Bold|.BpmLarge|.Badge|.Letter|.Small|.Pad]).
- Nút: `Widget.MixDeck.Button.Primary|Outline|Text`, `Widget.MixDeck.IconButton`.
- Kích thước: `@dimen/*` trong `dimens.xml` (spacing `space_N`, radius `radius_N`, và các kích thước component).
- Touch target ≥ 44dp. `contentDescription` lấy từ các chuỗi `*_cd`.
- RTL: dùng start/end. Riêng `activity_mixer.xml` đặt `android:layoutDirection="ltr"` (D-11).

## 11. Thứ tự phụ thuộc
1. **L1 (logic, trước tiên):** model, UiText, toàn bộ file `*UiState.kt` và chữ ký public của ViewModel (thân hàm có thể TODO nhưng phải compile), `Factory`, interface repository và impl rỗng đúng constructor. Xong thì báo lead và ui.
2. **U1 (ui, song song với L1):** custom view (không phụ thuộc logic), `MiniPlayerView` và `ToastView`, drawable nền. Xong thì báo lead.
3. **Lead:** `AppContainer`, `DJRemixProApp`, `MainActivity`, `activity_main.xml`, `nav_graph.xml`, Manifest. Cần L1 và U1 để compile.
4. **U2 (ui):** Fragment và layout theo từng feature, dựa trên UiState của L1. **L2 (logic):** logic đầy đủ, fake data, timer. U2 và L2 làm song song.
5. **Tester:** ma trận kiểm thử và unit test cho `core.util` cùng các ViewModel. Có thể viết test từ sau L1; chỉ chạy trên bản đã được lead tích hợp.

Mỗi lần bàn giao cho lead cần nêu: file đã đổi, hành vi đã xong, interface hoặc resource cần bên khác làm, cách đã kiểm chứng, và phần còn thiếu.

## Changelog
- v1 (lead): bản đầu.
- v1.3 (lead): `MixerViewModel.onQuickSettings()` mở `libraryPanel` với target = deck trống đầu tiên, nếu cả hai đã có bài thì target = B. Không còn toast "chưa có" (D-19).
- v1.2 (lead):
  - `LearnUiState` thêm `termsQuery: String = ""`; `LearnViewModel.onTermsQueryChange(q: String)` lọc glossary theo term/description, bỏ dấu (giống D-17).
  - `DeckUiState` thêm `positionSec: Float = 0f`. `ScrollingWaveformView` tính offset = positionSec·32·bpm/60 dp, để waveform nhảy theo khi Cue/tua/scratch; khi đang phát vẫn nội suy mượt giữa các tick 1 s.
- v1.1 (lead): start destination đổi thành `homeFragment`, Splash chỉ chạy lần đầu (D-25). Lead sở hữu `res/color/shell_*.xml` (tint bottom nav).
