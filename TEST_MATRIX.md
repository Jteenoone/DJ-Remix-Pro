# TEST_MATRIX — MixDeck (DJ Remix Pro)

Chủ file: **tester**. Nguồn đối chiếu: `DESIGN_ANALYSIS.md` (mã S/O/B), `DECISIONS.md` (D-xx), `IMPLEMENTATION_CONTRACT.md` (API), `design/DESIGNER_REPORT.md` (§2 trạng thái, §5 công thức, §6 tương tác), mã gốc `design/source/App.dc.html` (`App:NN`), `Mixer.dc.html` (`Mixer:NN`).

Giá trị kỳ vọng trong unit test cho công thức (Park–Miller, mini waveform, deck waveform, readout FX, thứ tự sắp xếp) được **tính bằng Node chạy nguyên văn đoạn JS của prototype**, không suy luận tay.

## Quy ước

- **Loại:** `JVM` = unit test cục bộ (`app/src/test`, JUnit4 + kotlinx-coroutines-test, fake repository tự viết, `Dispatchers.setMain(StandardTestDispatcher)`); `TC` = kiểm thử thủ công trên emulator (`emulator-5554`); `JVM+TC` = logic bằng JVM, phần hiển thị bằng tay.
- **Trạng thái:** `Chưa viết` → `Đã viết` (compile chưa chạy) → `PASS` / `FAIL (#lỗi)` trên bản tích hợp lead bàn giao → `TC: đạt` / `TC: lỗi` / `TC: chưa kiểm` cho phần thủ công. Không có trạng thái "đạt" nào khi chưa chạy thật.
- Package test: `com.example.djremixpro.<cùng package với class được test>`; tiện ích chung ở `com.example.djremixpro.testutil` (`MainDispatcherRule`, fake repository).
- Chạy: `.\gradlew.bat :app:testDebugUnitTest --console=plain` (theo lượt Gradle do lead cấp, D-06).
- Fake dùng trong test ViewModel: `testutil/TestRepositories.kt` (Test*Repository đồng bộ, không timer, có cờ lỗi `failWith`/`failUpdates`); repository in-memory thật (`Fake*`, `InMemoryMixSessionRepository`, `SimulatedPlaybackRepository`) được test riêng trong `FakeRepositoriesTest`.
- Số test hiện có: 213 (util 53, repository 18, ViewModel 142); các test M18 cho v1.3 đã chạy từ build 13:24; build cuối 14:21: **213 pass, 0 fail, 0 skip**. Lần 4 (2026-09-25, bản tích hợp có hợp đồng v1.2): **210 pass, 0 fail, 0 skip**, tổng thời gian test 1,25 s.

## 1. Tiện ích và công thức (`core/util`)

| # | Mã | Hành vi mong đợi | Nguồn công thức | Loại | Test class | Trạng thái |
|---|---|---|---|---|---|---|
| U01 | B18, B08 | `TimeFormat.mmss`: 0→"00:00", 5→"00:05", 59→"00:59", **60→"01:00"**, 61→"01:01", 222→"03:42", 3599→"59:59", **3600→"60:00"**, 3725→"62:05" (phút không cuộn sang giờ, như `mm()` JS) | Mixer:484 `mm(n)` | JVM | `TimeFormatTest` | PASS (lần 4) |
| U02 | B10, B11 | `TimeFormat.remaining`: 151→"−02:31" dùng **U+2212**, không phải '-' U+002D; 0→"−00:00" | Mixer:367 time '−02:31' | JVM | `TimeFormatTest` | PASS (lần 4) |
| U03 | B08 | `TimeFormat.date(2026-09-25)`→"25/09/2026"; ngày/tháng 1 chữ số có 0 đứng trước ("05/01/2026") | App:314 | JVM | `TimeFormatTest` | PASS (lần 4) |
| U04 | B06 | `TextUtils2.initials`: "Tầng thượng 102"→"TT", "Phố đêm"→"PĐ", "Ánh đèn sân khấu"→"ÁĐ", "Đi đâu cũng được"→"ĐĐ", "Mưa sao băng (Club mix)"→"MS", một từ "Mix"→"M"; cả 10 bài ra "TV,VQ,CV,SG,KC,TT,MS,PĐ,ÁĐ,ĐĐ". Biên: chuỗi rỗng / nhiều khoảng trắng liền → không crash | App:307 `ini` | JVM | `TextUtils2Test` | PASS (lần 4) |
| U05 | D-17 | `normalizeForSearch`: "Đèn Neon"→"den neon", "Tầng Thượng"→"tang thuong", "ĐI ĐÂU"→"di dau", chuỗi không dấu giữ nguyên (chỉ lowercase), rỗng→rỗng | D-17, hợp đồng §7 | JVM | `TextUtils2Test` | PASS (lần 4) |
| U06 | B08 | `ParkMiller.sequence(seed)`: seed 1 → 7.826369e-6, 0.13153779, 0.75560532; seed 5 → 3.9131846e-5, 0.65768894, 0.77802661; **không tràn số** (nhân bằng Long); giá trị ∈ (0,1) qua 10 000 lần gọi | App:278, Mixer:399 | JVM | `ParkMillerTest` | PASS (lần 4) |
| U07 | B08 | `WaveformGenerator.mini(seed)`: 16 phần tử; seed 5 (bản ghi đầu, `i*13+5`) = [3.00021, 7.47354, 9.21058, 5.56751, …, 5.84958]; mọi h ∈ [3, 12]; cùng seed → cùng kết quả (dung sai 1e-3) | App:278 `mini` | JVM | `WaveformGeneratorTest` | PASS (lần 4) |
| U08 | B14 | `WaveformGenerator.deck(seed)`: 304 phần tử, mọi h ∈ [1, 8]; seed 7 bắt đầu [7.01951, 5.73015, 2.04346, 5.49587, 3.20127, …], phần tử 303 = 4.58239; seed 29 bắt đầu [2.85955, 2.17405, 1.0, 1.69063, …]; `r()` chỉ gọi khi `i%8 ≥ 2` (sai thứ tự gọi sẽ lệch từ phần tử 2 trở đi) | Mixer:399–409 `gen` | JVM | `WaveformGeneratorTest` | PASS (lần 4) |
| U09 | B11 | `syncPitchPct(own=128, other=124)` = −3.125; `(124,128)` = +3.2258; bằng nhau → 0 | Mixer:437 | JVM | `MixMathTest` | PASS (lần 4) |
| U10 | B11 | `effectiveBpm(128, −3.125)` = 124.0 (định dạng "124.0" bằng `Locale.US`, không ra "124,0" khi default locale là vi/de) | Mixer:438, D-15 | JVM | `MixMathTest` | PASS (lần 4) |
| U11 | B12 | `pitchLabel`: 0→"+0.0%", 0.04→"+0.0%", −0.04→"+0.0%" (ngưỡng ±0.05 cho dấu), 3.2258→"+3.2%", −3.125→"−3.1%" (U+2212), 8→"+8.0%", −8→"−8.0%"; luôn dấu chấm thập phân | Mixer:441,447 | JVM | `MixMathTest` | PASS (lần 4) |
| U12 | B12 | `pitchPosition(p, 8)`: 0→0.5, +8→0.0 (lên trên), −8→1.0, −3.125→0.6953; ngoài phạm vi (±20 với range 8) → clamp 0..1; range 16: +8→0.25 | Mixer:447 `50 − pitch/8·50`, D-19 | JVM | `MixMathTest` | PASS (lần 4) |
| U13 | B13 | `spinPeriodSec`: 0→1.8, −3.125→1.8581, +3.2258→1.74375, +3.125→1.745 (toFixed(3)) | Mixer:448 | JVM | `MixMathTest` | PASS (lần 4) |
| U14 | B14 | `waveScrollDpPerSec(124)` = 66.133; `(120)` = 64 | Mixer:468 | JVM | `MixMathTest` | PASS (lần 4) |
| U15 | B19 | `isBpmMatch(candidate, reference)`: (126,124) true (+1.6%), (122,124) true (−1.6%), (125,124) true, (128,124) **false** (+3.23%), (120,124) false (−3.23%), candidate null → false, reference null → false; sát biên: (102.9,100) true, (103.1,100) false. (Đúng 3% không assert: trong JS `103/100−1` = 0.030000000000000027 > 0.03 nên prototype trả false, kết quả Float/Double phụ thuộc cài đặt) | Mixer:496–497 | JVM | `MixMathTest` | PASS (lần 4) |
| U16 | B16, B20 | `clamp01`: −0.1→0, 0→0, 0.5→0.5, 1→1, 1.2→1 | Mixer:463,481 | JVM | `MixMathTest` | PASS (lần 4) |
| U17 | B15, D-21 | `eqDb`: 0.5→0, 0.63→+3.51, 1→+13.5, 0→−13.5 | D-21 | JVM | `MixMathTest` | PASS (lần 4) |
| U18 | B20, D-22 | `fxReadout`: (0.62,0.3)→("1/2",70) (giá trị mặc định thiết kế), (0,1)→("1/16",0), **(1,0)→("1",100)** (floor(6)=6 phải kẹp về chỉ số 5), (0.5,0.5)→("1/2",50), (0.1666,0.3)→("1/16",70), (0.99,0.3)→("1",70); đầu vào ngoài 0..1 không crash | Mixer:486–488 | JVM | `MixMathTest` | PASS (lần 4) |
| U19 | B16 | `crossfaderFromTouch(x,left=0,width=236,knob=36)`: x=18→0, x=118→0.5, x=218→1, x=0→0 (clamp), x=500→1 (clamp); width == knob không chia 0 ra NaN | Mixer:481 | JVM | `MixMathTest` | PASS (lần 4) |

## 2. Repository fake (`core/data/fake`) — logic thuần, test trực tiếp

| # | Mã | Hành vi mong đợi | Nguồn | Loại | Test class | Trạng thái |
|---|---|---|---|---|---|---|
| R01 | D-08 | `FakeTrackRepository.tracks` đúng 10 bài theo thứ tự App:309, "Phố đêm" có `bpm == null`, `addedIndex` 0..9; `samples` = ("Mẫu 1 · Nhịp nhà",124) / ("Mẫu 2 · Đêm hội",126); `findById` id lạ → null | App:309,342 | JVM | `FakeRepositoriesTest` | PASS (lần 4) |
| R02 | D-08, B08 | `FakeRecordingRepository` seed 4 bản, mới nhất trước, waveSeed 5/18/31/44 | App:314 | JVM | `FakeRepositoriesTest` | PASS (lần 4) |
| R03 | D-12 | `add` thêm vào **đầu** danh sách, `createdAt` lấy từ `Clock` truyền vào | D-12 | JVM | `FakeRepositoriesTest` | PASS (lần 4) |
| R04 | D-13 | `rename` với tên rỗng / chỉ khoảng trắng → `failure(IllegalArgumentException)`, danh sách không đổi; tên có khoảng trắng hai đầu → lưu bản trim | hợp đồng §6 | JVM | `FakeRepositoriesTest` | PASS (lần 4) |
| R05 | D-13 | `delete` id không tồn tại → `failure(NoSuchElementException)`, danh sách không đổi; `rename` id không tồn tại → failure | hợp đồng §6 | JVM | `FakeRepositoriesTest` | PASS (lần 4) |
| R06 | B07 | `SimulatedPlaybackRepository.toggle`: id mới → phát từ 0; cùng id → stop (null); id khác khi đang phát → thay bằng bản mới từ 0; `positionSec` +1 mỗi 1000 ms (virtual time); tới `durationSec` → null | App:315, hợp đồng §6 | JVM | `FakeRepositoriesTest` | PASS (lần 4) |
| R07 | D-17 | `InMemoryMixSessionRepository`: seed A "Sài Gòn lên đèn", B "Mưa sao băng (Club mix)"; `setSession(null)` rồi `loadTrack(B, t)` → tạo session mới với A = null, B = t | App:343, D-17 | JVM | `FakeRepositoriesTest` | PASS (lần 4) |
| R08 | D-08 | `DataStoreSettingsRepository` (constructor phụ nhận `DataStore`, tệp tạm): kho rỗng → `AppSettings()`; `update` lưu đủ 13 trường; mở lại tệp (mô phỏng tắt app) vẫn giữ `onboardingDone`, theme; giá trị enum lạ → mặc định; lỗi IO khi đọc → phát mặc định, không throw | hợp đồng §6 | JVM | `DataStoreSettingsRepositoryTest` | PASS (lần 4) |

## 3. ViewModel app dọc

| # | Mã | Hành vi mong đợi | Nguồn | Loại | Test class | Trạng thái |
|---|---|---|---|---|---|---|
| V01 | S1 | Splash: `destination` null trước 800 ms; sau ≥ 800 ms: `onboardingDone=false` → ONBOARDING, `true` → HOME (D-25: Splash chỉ hiện khi chưa xong onboarding) | hợp đồng §8.2 | JVM | `SplashViewModelTest` | PASS (lần 4) |
| V02 | S2 | Onboarding: bắt đầu page 0, `canSkip` true ở trang 0,1, false ở trang 2; `onNext` ×2 → page 2; `onNext` ở trang cuối → event `GoToPermission`, page không vượt 2; `onSkip` → `GoToPermission` | #2e–#2g, hợp đồng §8.2 | JVM | `OnboardingViewModelTest` | PASS (lần 4) |
| V03 | S3, D-10 | Permission: `onAllowClicked` → `RequestPermission` (chưa ghi settings); `onPermissionResult(true/false)` và `onUseSamplesClicked` → ghi `onboardingDone=true` rồi `GoHome` (cả khi từ chối) | D-10 | JVM | `PermissionViewModelTest` | PASS (lần 4) |
| V04 | S4, D-09 | Home người quay lại (seed): `isNewUser=false`, `session` 2 dòng A "Sài Gòn lên đèn" "124" / B "Mưa sao băng (Club mix)" "128", `recent` = 2 bản đầu (tên, "03:12"), `showAd=true` | App:302,343, D-09 | JVM | `HomeViewModelTest` | PASS (lần 4) |
| V05 | S4, D-09 | Home **người mới**: bản ghi rỗng **và** session null → `isNewUser=true`, `samples` "124"/"126"; chỉ một trong hai rỗng → vẫn là người quay lại | D-09 | JVM | `HomeViewModelTest` | PASS (lần 4) |
| V06 | B07 | Home `onRecentPlayToggle(id)`: dòng đó `isPlaying=true`, dòng kia false; gọi lại cùng id → cả hai false; đổi sang id khác → chỉ id mới phát | App:315,317 | JVM | `HomeViewModelTest` | PASS (lần 4) |
| V07 | B04 | Library sort mặc định **BPM**; `onCycleSort` vòng TITLE→ARTIST→BPM→RECENT→TITLE (từ BPM: →RECENT→TITLE→ARTIST→BPM) | App:269,308–312, hợp đồng §4 | JVM | `LibraryViewModelTest` | PASS (lần 4) |
| V08 | B04 | Thứ tự BPM: Tết về rồi(100), Vòng quay(118), Đi đâu cũng được(120), Chạy về phía biển(122), Sài Gòn lên đèn(124), Không cần lời(125), Tầng thượng 102(126), Mưa sao băng(128), Ánh đèn sân khấu(130), **Phố đêm (null) cuối** | App:310 `b ?? 999` | JVM | `LibraryViewModelTest` | PASS (lần 4) |
| V09 | B04 | Thứ tự Tên (collator vi): Ánh đèn sân khấu, Chạy về phía biển, Đi đâu cũng được, Không cần lời, Mưa sao băng, Phố đêm, Sài Gòn lên đèn, Tầng thượng 102, Tết về rồi, Vòng quay | App:311 `localeCompare(…,'vi')` | JVM | `LibraryViewModelTest` | PASS (lần 4) |
| V10 | B04 | Thứ tự Nghệ sĩ: Bảo Trâm, Cáo Nhỏ, DJ Tâm, Đèn Neon, Hạ Vũ, Lam Anh, Mây Tầng Thượng, Mèo Mun, Nhóm Mùa Xuân, Vy Hạ (Node ICU và java.text.Collator(vi) cho cùng thứ tự, đã chạy thử bằng JDK; trên máy thật Android dùng ICU → vẫn kiểm thủ công một lần) | App:311 | JVM+TC | `LibraryViewModelTest` | PASS (lần 4) |
| V11 | B04 | Sắp xếp **ổn định**: fake có 2 bài cùng BPM → giữ thứ tự `addedIndex`; RECENT = thứ tự gốc | App:310 (Array.sort ổn định) | JVM | `LibraryViewModelTest` | PASS (lần 4) |
| V12 | B05 | Bài BPM null: `bpmLabel="…"`, `hasBpm=false`; sheet meta = "Hạ Vũ · 03:31 · Đang phân tích nhịp…" (UiText, so sánh id/args) | App:313,325 | JVM | `LibraryViewModelTest` | PASS (lần 4) |
| V13 | D-17 | Tìm kiếm: "den neon" và "Đèn" đều ra "Mưa sao băng"; "tang thuong" ra "Tầng thượng 102" **và** "Sài Gòn lên đèn" (nghệ sĩ Mây Tầng Thượng); hoa/thường không ảnh hưởng; không khớp → danh sách rỗng, `countLabel` Plural count 0; xoá query → 10 bài | D-17 | JVM | `LibraryViewModelTest` | PASS (lần 4) |
| V14 | D-17 | Chip tab khác SONGS chỉ đổi `tab`, danh sách giữ nguyên | App:311, D-17 | JVM | `LibraryViewModelTest` | PASS (lần 4) |
| V15 | O1, O3, D-17 | `onSongClicked` → sheet; `onLoadToDeck(B)` → session.deckB = bài đó, toast tone `DECK_B`, sheet null; `onPreview` → playback phát bài đó, sheet null; `onSongClicked` id lạ → không crash, sheet null | App:324–330 | JVM | `LibraryViewModelTest` | PASS (lần 4) |
| V16 | S6, B08 | Recordings: 4 dòng, waveSeed 5/18/31/44; `onRowClicked` → dòng đó `isPlaying`, meta thêm "Đang phát"; chạm lại → dừng | App:314–316 | JVM | `RecordingsViewModelTest` | PASS (lần 4) |
| V17 | S6 | Repo rỗng → `isEmpty=true`, items rỗng | App:347, #2m | JVM | `RecordingsViewModelTest` | PASS (lần 4) |
| V18 | O2, D-13 | `onMenuClicked` → sheet (title, meta "03:12 · 25/09/2026"); `onRenameClicked` → sheet null, `renameDialog` có tên hiện tại; `onRenameConfirmed("Tên mới")` → đổi tên, dialog null; **tên rỗng/khoảng trắng** → không đổi, toast ERROR, state cũ giữ; `onRenameDismissed` → dialog null | D-13, hợp đồng §6 | JVM | `RecordingsViewModelTest` | PASS (lần 4) |
| V19 | O2, D-13 | `onDeleteClicked` → `deleteConfirm`; `onDeleteConfirmed` → mất khỏi danh sách + toast; `onDeleteDismissed` → không xoá; **xoá id không tồn tại** (repo trả failure) → toast `toast_generic_error` tone ERROR, danh sách giữ nguyên | D-13, hợp đồng §6 | JVM | `RecordingsViewModelTest` | PASS (lần 4) |
| V20 | D-13 | `onShareClicked` → toast "chưa khả dụng", sheet đóng | D-13 | JVM | `RecordingsViewModelTest` | PASS (lần 4) |
| V21 | S7, B09, D-18 | Learn: 7 bài, bước [4,3,4,5,4,5,3], bài 1–2 DONE, bài 3 CURRENT, 4–7 TODO; progress 2/7; 10 thuật ngữ, 4 mẹo | App:318–321 | JVM | `LearnViewModelTest` | PASS (lần 4) |
| V22 | S7 | Segment ban đầu từ `SavedStateHandle[segment]`: "TERMS" → TERMS; thiếu → GUIDE; giá trị rác → GUIDE (không crash); `onSegmentSelected(TIPS)` đổi segment | App:344, hợp đồng §8.6 | JVM | `LearnViewModelTest` | PASS (lần 4) |
| V23 | S8, B01, B03 | Settings: giá trị mặc định (DARK, 40 ms, precue off, 80 %, MP3, 320, ±8, JOG, haptic on, awake on); `onThemeSelected(LIGHT)` ghi repo; `onToggle(PRECUE)` đảo; toggle 2 lần về như cũ | App:269, hợp đồng §4 | JVM | `SettingsViewModelTest` | PASS (lần 4) |
| V24 | D-14 | Chọn giá trị: latency/volume/quality/pitchRange/format/jog ghi repo; giá trị ngoài `SettingsOptions` → bỏ qua (ghi nhận hành vi); `onFolderClicked`, `onSupportClicked` → toast | D-14 | JVM | `SettingsViewModelTest` | PASS (lần 4) |
| V25 | S9, B02, D-15 | Language: 17 dòng đúng thứ tự tag; `vi` được chọn mặc định; `onLanguageSelected("ar")` → repo `languageTag="ar"`, chỉ dòng ar `isSelected`, toast "Đã chọn العربية"; Settings `languageName` đổi theo | App:284,298 | JVM | `LanguageViewModelTest` | PASS (lần 4) |
| V26 | S9 | Tìm ngôn ngữ: "tieng anh" → English; "ANH" → English; "日本" → 日本語; không khớp → rỗng | D-17 (áp dụng tương tự) | JVM | `LanguageViewModelTest` | PASS (lần 4) |
| V27 | O4 | Main: `nowPlaying` null → `miniPlayer` null; phát 77 s / 192 s → `timeLabel` "01:17 / 03:12", progress ≈ 0.401; `onMiniPlayerStop` → null | App:227–234 | JVM | `MainViewModelTest` | PASS (lần 4) |
| V28 | S7 (hợp đồng v1.2) | Tìm thuật ngữ: lọc theo term hoặc mô tả, bỏ dấu, không phân biệt hoa thường, có trim: "NHIP"→BPM, Sync, Beatmatch; "cross"→Crossfader; "tốc độ"→Pitch, Sync; "deck"→Deck, Crossfader, EQ, Sync; "dong bo"→rỗng; rỗng/khoảng trắng→10 mục; query giữ qua SavedStateHandle `terms_query`; không ảnh hưởng bài học/mẹo | App:320 | JVM | `LearnViewModelTest` | PASS (lần 4) |

## 4. MixerViewModel (giả lập, D-07)

| # | Mã | Hành vi mong đợi | Nguồn | Loại | Test class | Trạng thái |
|---|---|---|---|---|---|---|
| M01 | B10 | Deck trống: track null, `bpmLabel "—"`, `remainingLabel "−−:−−"`; `onPlayToggle`/`onSyncToggle`/`onCue` **không đổi state** | Mixer:437–457 | JVM (+TC opacity .35) | `MixerViewModelTest` | PASS (lần 4) |
| M02 | S10 | entry DEFAULT: deck lấy từ session, cả hai dừng; SAMPLES: A "Mẫu 1 · Nhịp nhà" 124, B "Mẫu 2 · Đêm hội" 126; DEFAULT khi session null → hai deck trống | hợp đồng §8.8 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M03 | B11, B12 | Sync B (A 124, B 128): pitchPct −3.125, `bpmLabel "124.0"`, `pitchLabel "−3.1%"`, `isPitchShifted`, pitchPosition ≈ 0.6953; tắt sync → pitch 0, "128.0", "+0.0%" | Mixer:437–447 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M04 | B11 | **Sync khi deck kia trống** → pitch 0, BPM giữ nguyên, không NaN/crash; deck kia được nạp sau đó → pitch cập nhật | Mixer:437 `otherT` | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M05 | B11 | Pitch âm và dương: B sync theo A → âm; A (124) sync theo B (128) → +3.2258, "+3.2%", "128.0" | Mixer:437 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M06 | D-19 | `onPitchChanged` tay: tắt Sync, clamp theo `pitchRangePct` (8 → ±8) | D-19 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M07 | B13 | `spinPeriodSec` theo pitch; `isSpinning` = playing && !(touching && scratch); touching + scratch off → vẫn quay | Mixer:448–452 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M08 | B16 | `onCrossfaderChanged(1.5)` → 1.0, (−0.2) → 0.0; `onCrossfaderReset` → 0.5 | Mixer:481–483 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M09 | B15, D-21 | `onEqChanged(A, MID, 0.63)` → label "+3.5 dB"; `onEqKill` → `isKill`, "Kill"; `onEqReset` → 0.5 và hết kill; FILTER < 0.5 → nhãn LPF, > 0.5 → HPF | D-21 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M10 | B18, D-12 | REC: `onRecToggle` → recording, label "00:00"; advance 60 s → "01:00"; 61 s → "01:01"; **> 60 phút** (3725 s) → "62:05"; dừng → `saveDialog` với `durationLabel` bằng thời lượng, elapsed về 0, label "REC" | Mixer:484–485, D-12 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M11 | O7, B21, D-12 | Dialog lưu: tên mặc định "Mix dd-MM HH:mm" theo `Clock` cố định (2026-09-25 21:40 → "Mix 25-09 21:40"); `onSaveFormatSelected(WAV)` → `showWavNote`; `onSaveConfirmed` → repo `add` 1 bản đúng tên/format/thời lượng + toast; `onSaveDiscarded` → không thêm; tên rỗng khi lưu → ghi nhận hành vi (không crash) | D-12 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M12 | B14 | Timer 1 s: đang phát → progress tăng theo tốc độ 1+pitch/100; dừng → không tăng; `onCue` → dừng + progress 0 (D-19) | hợp đồng §8.8, D-19 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M13 | O5, B19 | `onJogTapped` deck trống → `libraryPanel(target)`; deck có bài → không mở; khi target B và A 124: dòng 122/125/126/128 `isBpmMatch` theo ≤ 3% trên BPM **hiệu dụng** A, BPM null → false; target A → không dòng nào match | Mixer:496–500 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M14 | O5, B19 | `onLibraryLoad(id, B)` → deck B có bài, **dừng**, panel đóng, session repo cập nhật; nạp lần hai vào A **không ghi đè** B (lỗi prototype TR['lib']) | Mixer:498, DESIGNER_REPORT §5.2 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M15 | B17, D-20 | Pad: mỗi tab đúng 8 pad; CUE 1–3 ON, 4–8 OFF; LOOP pad "4" ON sau khi chạm (bật/tắt); FX chạm bật/tắt; SAMPLER bật → `NEUTRAL_ON`; `onPadLongPressed` tab FX → `fxSheet` (Echo) | Mixer:422–433, D-20 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M16 | O6, B20, D-22 | FX sheet: readout mặc định "1/2 beat · 70%"; `onFxChanged(1,0)` → "1 beat · 100%"; ngoài 0..1 bị clamp; chọn beat length không đổi readout; `onFxClose` → null | Mixer:486–488, D-22 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M17 | O8, B22, D-18 | entry LESSON lessonId 2: A phát, B có bài chưa sync, `coach` step 3/5 chưa xong; `onSyncToggle(B)` → `isDone`; `onCoachSkip` → coach null; lessonId khác → coach null | Mixer:297–317,493, D-18 | JVM | `MixerViewModelTest` | PASS (lần 4) |
| M18 | D-19 (hợp đồng v1.3) | `onQuickSettings` mở `libraryPanel` (không event): hai deck có bài → target B, 10 dòng; session null → A; chỉ A có bài → B; chỉ B có bài → A; sau đó `onLibraryLoad(id, B)` khi B đang phát → thay bài, positionSec 0, dừng, panel đóng, session cập nhật. `onBack` → event `Close` | Mixer:494, D-19 | JVM | `MixerViewModelTest` | Đã viết (v1.3, chưa chạy) |
| M19 | D-15 | Nhãn số Mixer vẫn dấu chấm khi `Locale.setDefault(vi/de/fr/ar)` | D-15 | JVM | `MixerViewModelTest`, `MixMathTest` | PASS (lần 4) |
| M20 | B14, D-19 (hợp đồng v1.2) | `positionSec`: nạp bài → 0; tick khi phát += 1 + pitch/100 (B sync 124/128: +0.96875); Cue → 0; seek f → f·duration; jog 360° khi chạm + scratch → +1.8; hot cue 2 → 64; loop 4 beat ở 124 BPM (1.935 s) quay vòng trong [start, start+1.935); hết bài → = duration, dừng | hợp đồng v1.2 | JVM | `MixerViewModelTest` | PASS (lần 4) |

## 5. Quyết định (D-xx) không có hoặc chỉ có một phần logic kiểm được bằng JVM

| # | Mã | Kiểm tra | Loại | Trạng thái |
|---|---|---|---|---|
| T01 | D-02, D-03, D-04 | `:app:assembleDebug` thành công, không còn Compose | Lead chạy build | Lead báo assembleDebug thành công và đã cài APK; trong `verification/` không có log build → **chưa có bằng chứng tester tự kiểm** |
| T02 | D-05, B01 | Font Be Vietnam Pro / Chakra Petch hiển thị đúng; theme Tối mặc định, đổi Sáng / Theo hệ thống áp dụng ngay toàn app | TC | **Đạt.** Dark mặc định (06, 18). Sáng áp dụng ngay (21, 22, 73). "Theo hệ thống": system night=no → nền #EEEAF4 (93), `cmd uimode night yes` → nền #1C1726 (94). Đã trả chế độ hệ thống về night=no. Font Be Vietnam Pro / Chakra Petch hiển thị (06, 30) |
| T03 | D-10, D-25, S3 | Dialog quyền hệ thống xuất hiện (API 33+ READ_MEDIA_AUDIO); cho phép hoặc từ chối đều vào Home; mở lại app → vào thẳng Home, không qua Splash (D-25) | TC | **Đạt** (tester, 2026-09-25). `pm clear` → onboarding → "Cho phép truy cập nhạc" → dialog hệ thống READ_MEDIA_AUDIO (61) → Allow → Home, `granted=true` (62). `pm clear` → "Bỏ qua" → Cho phép → Don't allow → vẫn vào Home, `granted=false` (64). force-stop rồi mở lại → vào thẳng Home, không qua onboarding (63). Splash hệ thống hiện icon Android mặc định (63a, VIS-5) |
| T04 | D-11 | Mixer ngang, immersive; xoay máy không mất state; ngôn ngữ ar → app dọc RTL, Mixer vẫn A trái / B phải | TC | **Đạt một phần.** Mixer ngang, immersive (30–41, 83–91). Ngôn ngữ ar: app dọc chuyển RTL (79, 80, 82), Mixer vẫn A trái / B phải, số dạng "124.0" (83). **Chưa kiểm** xoay máy (không đổi cài đặt xoay của thiết bị) |
| T05 | D-14 | Dòng giá trị Cài đặt mở dialog chọn; "Quyền truy cập" mở App info hệ thống | TC | **Đạt.** Dialog chọn giá trị: độ trễ (72), âm lượng, định dạng, chất lượng, pitch, jog; giá trị cập nhật ngay (73). Toast "Thư mục lưu cố định: Music/MixDeck" (74), toast Liên hệ hỗ trợ (75), "Quyền truy cập" mở App info của hệ thống (76), Back quay lại app |
| T06 | D-15, B02 | Chọn ngôn ngữ → toast, Back về Cài đặt, dòng Ngôn ngữ hiện tên bản địa; khởi động lại vẫn giữ | TC | **Đạt.** Chọn العربية → toast "Đã chọn العربية" + radio (79), bố cục RTL (80–82); chọn lại Tiếng Việt → LTR; dòng Ngôn ngữ hiện tên bản địa (18b). Giữ sau khi khởi động lại: chưa force-stop riêng cho ngôn ngữ, nhưng DataStore đã được kiểm ở T10 |
| T07 | D-16 | Chế độ Sáng: đĩa jog và nhãn đĩa vẫn tối | TC | **Đạt.** Chế độ Sáng: đĩa jog và nhãn đĩa vẫn tối (30); đĩa minh hoạ ở Home vẫn tối (22) |
| T08 | D-24, O3, O4, B23 | Toast nằm trên mini player, tự tắt ~2,2 s, mở toast thì đóng sheet | TC | **Đạt một phần.** Toast nằm ngay trên mini player khi cả hai cùng hiện (71, D-24); toast trên bottom nav (10, 67, 68, 70); toast Xoá có icon màu rec (70); mở toast thì sheet đóng (68). **Chưa đo chính xác** thời gian tự tắt 2,2 s (uiautomator dump chậm hơn 1 s mỗi lần) |
| T09 | B13, B14, B15, B16 | Jog quay / dừng khi chạm (scratch), waveform cuộn, knob kéo dọc / chạm đúp / nhấn giữ kill, crossfader chạm đúp về giữa + rung ở nấc giữa | TC | **Đạt một phần.** Crossfader: kéo về trái → 2 %, kéo phải → 76 %, chạm đúp → 50 % (85, 86). Knob Mid: kéo lên +4.6 dB, kéo xuống −1.3 dB; nhấn giữ Low → Kill, viền đỏ (87); chạm đúp → 0.0 dB. Pitch: Sync A bật → 128.0 / +3.2 % (88); kéo fader → Sync tắt, 118.5 / −4.4 %, phạm vi ±16 % theo cài đặt (89). **Chưa kiểm** rung ở nấc giữa (emulator không rung), giữ jog + scratch, cuộn waveform bằng mắt |
| T10 | D-08 | Tắt app hẳn → đổi tên / xoá / bản mix đã lưu mất; cài đặt và cờ onboarding còn | TC | **Đạt.** Đổi 9 cài đặt liên tiếp (theme Sáng, 100 ms, pre-cue bật, 60 %, WAV, 192 kbps, ±16 %, Pad, rung tắt) (73) → force-stop → mở lại: mọi giá trị còn giữ (77); Mixer mở ở chế độ Pad và pitch ±16 % (83, 89). Đổi tên + xoá bản ghi (67, 70) → force-stop → danh sách về seed 4 bản (78), đúng D-08 |
| T11 | Luồng §3 | Bottom nav 4 tab; icon Cài đặt; Back: Ngôn ngữ→Cài đặt→tab trước; Home "Xem tất cả" / ô Học DJ mở đúng segment | TC | **Đạt.** Ô "Thuật ngữ" ở Home mở Học DJ đúng segment Thuật ngữ (92); "Xem tất cả" (Học DJ) → tab Học DJ; Back: Ngôn ngữ → Cài đặt → Học DJ (tab trước) → Home. Bottom nav 4 tab (06, 08, 11, 15) |
| T12 | D-23 | Banner quảng cáo placeholder hiện ở Home | TC | **Đạt.** Placeholder quảng cáo hiện ở Home (06, 22) |


### 5.1 Quan sát hiển thị từ ảnh của lead (`verification/screens/`)

| # | Ảnh | Quan sát | Mức |
|---|---|---|---|
| VIS-1 | 30, 31, 32, 36, 38, 40, 41 | Header deck Mixer: dòng thời gian còn lại dưới BPM bị cắt, chỉ thấy nửa trên chữ "04:12" | Lỗi hiển thị |
| VIS-2 | 30, 32, 36 | Nhãn pitch dưới fader hiện "+0.0", thiếu "%" so với thiết kế "+0.0%" (có thể bị cắt) | Lỗi hiển thị |
| VIS-3 | 30, 32, 36, 40 | Chữ "Scratch" tràn sát hai mép nút 48 dp | Lỗi hiển thị |
| VIS-4 | 36 | Dòng "Đang ghi bản mix" dưới nút REC bị cắt chân chữ | Lỗi hiển thị |
| VIS-5 | 01 | Splash hệ thống và icon launcher là icon Android mặc định của template, chưa phải logo MixDeck | Thiếu asset |
| VIS-6 | 40, 41 | Coach: dải bên trái (vùng inset/cutout, khoảng 130 px) có vẻ không bị scrim phủ | Cần xác nhận |
| VIS-7 | 39 | Bản mới "Mix 25-09 13:04" đứng trên "Mix 25-09 21:40" dù giờ sớm hơn: danh sách xếp theo thứ tự thêm, còn giờ seed là mock | Ghi nhận, không phải lỗi |
| VIS-1..3 | 87, 89 | Đã sửa trên bản mới: header deck hiện đủ "−04:12", nhãn pitch "+0.0%", chữ Scratch nằm gọn trong nút | Đã kiểm lại |
| VIS-8 | 79, 82 | RTL (ar): ở màn Ngôn ngữ, tên bản địa căn phải nhưng tên tiếng Việt căn trái; ở Bản ghi, tên bản ghi căn trái nhưng meta căn phải, và meta bị đảo thành "25/09/2026 · 03:12" do bidi | Lỗi hiển thị RTL (nhỏ) |
| VIS-9 | 79, 81 | RTL: mũi tên Back trên top bar không lật chiều (vẫn chỉ sang trái dù nằm bên phải) | Lỗi hiển thị RTL (nhỏ) |
| A11Y-1 | uiautomator dump màn Cài đặt | Ba `switch_view` luôn có `checked=false` dù đang bật, nên TalkBack không đọc được trạng thái bật/tắt | Lỗi accessibility |
| OK-1 | 90 | Panel "Chọn bài cho Deck B" mở từ nút tune; "Hợp nhịp A" tính trên BPM hiệu dụng của A (118.5 → 118/120/122 khớp, 124 không); nạp "Vòng quay" vào B → B 118.0, panel đóng (91) | Đạt |
| OK-2 | 65_* | Sắp xếp trên thiết bị (ICU): vòng BPM → Mới thêm → Tên → Nghệ sĩ → BPM; thứ tự Tên và Nghệ sĩ khớp đúng kỳ vọng unit test (Bảo Trâm, Cáo Nhỏ, DJ Tâm, Đèn Neon, …) | Đạt |
| OK-3 | 66, 67, 68, 69, 70 | Đổi tên: xoá hết hoặc chỉ khoảng trắng → nút Lưu bị disable, Enter trên bàn phím không đóng dialog (66); tên hợp lệ → toast "Đã đổi tên bản mix" (67). Chia sẻ → toast chưa khả dụng (68). Xoá → dialog "Xoá bản mix?" (69) → mất khỏi danh sách + toast (70) | Đạt |
| VIS-8, VIS-9 | 114, 115, 116 | Kiểm lại trên bản 13:24: **vẫn còn** (tên tiếng Việt / tên bản ghi căn trái trong RTL, meta bị đảo, mũi tên Back không lật chiều) | Lỗi hiển thị RTL (nhỏ) |
| VIS-10 | 100 | Splash bản 13:24 đã dùng logo MixDeck (thay VIS-5), nhưng mép trái và phải của hai vòng bị mask tròn cắt (lead đang sửa `ic_launcher_foreground` bằng inset) | Lỗi hiển thị (nhỏ) |
| VIS-11 | 110 | Dialog Đổi tên: khi tên rỗng thì nút "Lưu" bị `enabled=false` nhưng vẫn giữ nền sáng như lúc bật, không có trạng thái disabled trực quan | Lỗi hiển thị (nhỏ) |
| VIS-8, VIS-9, A11Y-1, VIS-10 | F_rtl_*, F_settings, F_splash_system | **Đã sửa trên APK cuối**: RTL căn phải đồng nhất, meta "03:12 · 25/09/2026" không bị đảo, mũi tên Back và chevron tự lật; switch báo checked đúng; logo splash không còn bị mask cắt | Đã kiểm lại |
| VIS-11 | F_rename_blank | APK 13:43 còn; **APK 13:55 đã sửa** (nền tối, chữ mờ khi disable) | Đã kiểm lại |
| VIS-12 | F_splash_system, F_splash_app | Logo trên splash hệ thống bị kéo méo: vòng tròn thành elip. Đo vòng cam: rộng 308 px, cao 402 px (tỉ lệ 0,77; đúng ra ≈ 1) | Lỗi hiển thị (nhỏ) |
| VIS-13 | F_splash_blackframe | Khi mở lần đầu (sau pm clear), khoảng 1,9 s sau khi launch có một khung hình đen toàn màn giữa splash hệ thống (≈0,6–1,3 s) và onboarding (≈2,6 s). Không thấy màn Splash trong app có chữ "MixDeck" (S1, `fragment_splash.xml`); dumpsys cho thấy onboarding chạy trong `IntroFragment`. Cần lead xác nhận đây có phải thiết kế theo D-25 không | Cần xác nhận |
| VIS-12, VIS-13 | F_splash_system, F_splash_app, F_launcher_icon | **APK final2 đã sửa**: logo tròn đều (224×224), không còn khung đen khi mở lần đầu; bắt được splash của app (S1: logo + "MixDeck") | Đã kiểm lại |
| INFO-1 | F_rtl_settings | Ở tiếng Ả Rập, giá trị Cài đặt hiện chữ số Ả Rập-Ấn ("٤٠ ms", "٨٠%"). Mixer vẫn dùng Locale.US, đúng D-15 ("định dạng hệ thống thay đổi") | Ghi nhận |
| INFO-2 | F_home | Sau pm clear, Home vẫn hiện trạng thái người quay lại vì dữ liệu seed có 4 bản ghi và 1 phiên (D-09), nên **không chụp được F_home_new_user**. Trạng thái người mới đã được kiểm bằng unit test (V05) | Ghi nhận |

## 6. Rủi ro môi trường test (JVM)

- `testOptions.unitTests.isReturnDefaultValues = true`: mọi API `android.*` trả 0/null/false. Hệ quả: `Color.parseColor` → 0 (test không assert `coverColor`); nếu logic dùng `android.icu.text.Collator` hoặc `android.text.TextUtils`, test sẽ nhận `null`/NPE → cần dùng `java.text.Collator` / `java.text.Normalizer` / Kotlin thuần trong `core.util` và ViewModel.
- ViewModel dùng `viewModelScope` (Dispatchers.Main.immediate) → test đặt `Dispatchers.setMain(StandardTestDispatcher)`; timer 1 s kiểm bằng `advanceTimeBy` + `runCurrent`, không dùng thời gian thật.
- Test không phụ thuộc giờ hệ thống: dùng `Clock.fixed` (`2026-09-25T21:40`, `ZoneOffset.UTC`).
- DataStore trên JVM Windows không rename đè tệp `.preferences_pb` đã có ("Unable to rename …tmp"), nên mỗi tệp test chỉ ghi một lần. Ghi nhiều lần liên tiếp (đổi theme rồi đổi ngôn ngữ…) chỉ kiểm được trên thiết bị → thuộc T06/T10.
- `runTest` chạy hết scheduler sau thân test; ticker 1 s của Mixer vô hạn khi deck không thể hết bài (loop, giữ jog + scratch) hoặc đang REC. Mọi test Mixer chạy trong `mixerTest {}` (timeout 30 s, cuối test dừng deck/REC).

## 7. Nhật ký chạy

| Ngày | Bản | Lệnh | Kết quả |
|---|---|---|---|
| 2026-09-25 11:46 | lượt 1 (có v1.2) | `:app:testDebugUnitTest` | Treo ở `MixerViewModelTest.positionSec_fourBeatLoopAt124Bpm_wrapsWithinLoop` (jstack xác nhận): runTest chạy hết scheduler sau thân test mà ticker của loop không bao giờ dừng → lỗi của test, đã sửa. Trước khi treo: 125 test, 2 fail do kỳ vọng sai trong test (`MixMathTest.spinPeriod_followsPitch` dùng 1.745 cho +3.2258 thay vì 1.74375; `LibraryViewModelTest.search_ignoresAccentsAndCase` với "Đèn" khớp cả 3 bài có "đèn"), đã sửa |
| 2026-09-25 12:0x | lượt 1, chạy lại | `:app:testDebugUnitTest` | Bị hệ thống dừng vì thiếu RAM. 12/17 lớp có kết quả: 167 test, 1 fail, 1 treo. `DataStoreSettingsRepositoryTest.update_transformSeesCurrentValue` fail: `IOException: Unable to rename …preferences_pb.tmp` ở lần ghi thứ hai (JVM trên Windows không rename đè tệp đã có; hạn chế môi trường, không phải lỗi app) → sửa test chỉ ghi một lần. `MixerViewModelTest.touchingWithScratch_holdsPosition` treo 44 phút (deck phát + giữ jog → ticker vô hạn) → thêm `mixerTest {}` dừng mọi deck/REC cuối mỗi test. Chưa có kết quả cho Onboarding, Permission, Recordings, Settings, Main, Splash |
| 2026-09-25 (lần 4, lượt lead cấp) | tích hợp v1.2, main compile sạch | `:app:testDebugUnitTest` | **BUILD SUCCESSFUL, 210 test / 0 fail / 0 skip** (18 lớp; Mixer 51 test 0,5 s). Không phát hiện lỗi code ứng dụng. Các lỗi ở lần 1–2 đều do test và đã sửa: kỳ vọng spinPeriod, kỳ vọng tìm "Đèn", DataStore ghi hai lần trên Windows, ticker vô hạn sau thân `runTest` (thêm `mixerTest {}` có `timeout = 30.seconds`) |
| 2026-09-25 13:10–13:30 | APK lead cài (v1.3) | kiểm thử thủ công emulator-5554 (ảnh 60–94) | T02, T03, T05, T06, T10, T11 đạt; T04, T08, T09 đạt một phần; phát hiện VIS-8, VIS-9, A11Y-1 (nhỏ), VIS-5 vẫn còn |
| 2026-09-25 13:24 (lead chạy) | tích hợp 13:24 (font, onboarding, dialog Đổi tên/Xoá, icon/splash, Deck B căn phải) | `assembleDebug`, `lintDebug`, `:app:testDebugUnitTest` | Theo lead báo: assembleDebug OK, lint 0 lỗi, **213/213 PASS** (tester không tự chạy lại lần này) |
| 2026-09-25 13:42–13:53 | APK 13:24, emulator-5554 dùng riêng | kiểm thủ công lại phần UI đổi (ảnh 100–119) | Onboarding đủ 3 trang (trang 3 không có Bỏ qua) → quyền: Allow → Home, granted=true (105, 106); Bỏ qua → Don't allow → Home, granted=false (107); Bỏ qua → Dùng bài mẫu → Home, mở lại vào thẳng Home (108). Dialog Đổi tên mới: tên rỗng/khoảng trắng → nút Lưu disable, Enter không đóng; tên hợp lệ → đổi (109–111). Dialog Xoá mới: Huỷ giữ nguyên, Xoá → mất khỏi danh sách (112, 113). RTL ar: toast + radio (114), Mixer vẫn LTR, Deck B căn phải (118); đã trả về Tiếng Việt (119). Hiện tượng "Tiếp nhảy thẳng về Home" lúc 13:33 là do hai bên cùng thao tác emulator, không tái hiện khi dùng riêng |
| 2026-09-25 13:43 (lead chạy) | **APK cuối** `verification/app-debug-final.apk` | `assembleDebug`, `lintDebug`, `:app:testDebugUnitTest` | Theo lead báo: assembleDebug OK, lint 0 lỗi, **213/213 PASS** |
| 2026-09-25 13:54–14:09 | APK cuối, emulator-5554 dùng độc quyền | kiểm thủ công + bộ ảnh `F_*` cho designer | Onboarding đủ 3 trang (F_onb1–3), Allow → Home granted=true (F_permission_dialog, F_home_after_allow); Bỏ qua → Don't allow → Home granted=false (F_home_after_deny); mở lại vào thẳng Home. Đổi tên rỗng → Lưu disable, dialog không đóng; tên hợp lệ → đổi; Xoá → Huỷ giữ nguyên / Xoá mất khỏi danh sách (F_rename, F_rename_blank, F_delete). A11Y-1 đã sửa: `row_*`/`switch_view` checkable=true, checked đúng và đổi theo khi chạm. RTL: VIS-8, VIS-9 đã sửa (F_rtl_language, F_rtl_recordings, F_rtl_settings); Mixer vẫn LTR. Mixer: A phát + B sync 124.0/−3.1% (F_mixer_play), FX sheet "1/2 beat · 70%" (F_fx_sheet), REC 00:06 → dialog lưu "Mix 25-09 14:07" → toast "Đã lưu bản mix" hiện ở trên (F_rec_running, F_save_dialog, F_mixer_toast), panel thư viện từ nút tune với "Hợp nhịp A" (F_library_panel), coach 3/5 → hoàn thành khi Sync B (F_coach, F_coach_done). Đã trả về Tiếng Việt |
| 2026-09-25 13:55 (lead chạy) | **APK cuối 13:55** `verification/app-debug-final.apk` | `assembleDebug`, `lintDebug` (0 lỗi, 115 warning), `:app:testDebugUnitTest` | Theo lead báo: **213/213 PASS** |
| 2026-09-25 14:10–14:20 | APK 13:55, emulator dùng độc quyền | kiểm lại VIS-8/9/10/11/13, A11Y-1 + bộ ảnh `F_*` mới (bộ 13:43 chuyển vào `screens/old_F_1343/`) | VIS-8, VIS-9 đạt (F_rtl_*); VIS-11 đạt: nút Lưu khi disable có nền tối và chữ mờ (F_rename_blank); A11Y-1 đạt (checked false/true/true, đổi khi chạm). **VIS-12 vẫn còn**: vòng splash 308×402 px (F_splash_system), icon launcher bị dẹt ngang (F_launcher_icon). **VIS-13 vẫn còn**: 2/2 lần mở sau pm clear có 1 khung đen (khoảng khung thứ 4 trong chuỗi chụp). Mã nguồn `ic_launcher_foreground.xml` và `DJRemixProApp.applyLanguage` đã được sửa **sau** khi APK 13:55 được build, nên hai lỗi này cần kiểm lại trên bản build tiếp theo. Đã trả về Tiếng Việt |
| 2026-09-25 14:12 (lead chạy) / 14:21–14:29 (tester kiểm) | **APK final2 14:12** `verification/app-debug-final2.apk` | lead: lint 0 lỗi, `:app:testDebugUnitTest` **213/213 PASS**; tester: kiểm thủ công 3 mục | **VIS-12 ĐẠT**: vòng splash hệ thống 224×224 px (F_splash_system), splash app 104×102 px (F_splash_app), icon launcher tròn đều (F_launcher_icon). **VIS-13 ĐẠT**: 3/3 lần mở sau pm clear, chuỗi khung là splash hệ thống → splash app (logo + "MixDeck") → onboarding, **không còn khung đen**. **VIS-11 ĐẠT**: Đổi tên để trống thì Lưu enabled=false, nền tối và chữ mờ (F_rename_blank); nút Xoá ở trạng thái bật hiển thị bình thường |

| 2026-09-25 14:21 (lead chạy + kiểm) | **APK CUỐI CÙNG** `verification/app-debug-final.apk` (md5 a163793a…) — thêm N01–N04 | `assembleDebug`, `lintDebug` (0 lỗi, 115 warning), `:app:testDebugUnitTest` **213/213 PASS**; emulator-5554 | VIS-13 đạt (12 khung liên tiếp từ launch tới onboarding không có khung đen); VIS-12 đạt (splash lạnh vòng tròn đều, icon launcher MixDeck); N01 đạt (chữ A/B Chakra Petch: Z_home, Z_mixer); N04 đạt (số BPM và "BPM" chung baseline, Z_home); Mixer đối xứng, Deck B căn phải (Z_mixer). N02, N03 đạt trên RTL (Z_rtl_language, Z_rtl_library: tiêu đề ở phía đầu dòng, badge "100 BPM" không đảo); đã trả về Tiếng Việt (Z_back_vi) |

## 8. Lỗi đã báo (tổng hợp, do lead ghi sau khi kết thúc tester)

| # | Nguồn | Mô tả | Chủ sửa | Trạng thái |
|---|---|---|---|---|
| U-B1..U-B4 | lead, emulator | Header Mixer cắt thời gian còn lại; pitch mất "%"; "Scratch" tràn; scrim coach hở mép trái | ui | Đã sửa, xác nhận (50, 51, F_coach) |
| U-B5 / V04 | tester VIS-4 | "Đang ghi bản mix" bị cắt chân chữ | ui | Đã sửa (F_rec_running) |
| L-B1 | lead, emulator | Panel thư viện (O5) không mở được vì seed luôn có 2 deck → nút tune mở panel (D-19) | logic | Đã sửa, xác nhận (51, F_library_panel) |
| S-1 | lead | Bottom nav nổi trên bàn phím | lead | Đã sửa (53) |
| S-2 | lead | Toast "Đã chọn …" mất khi đổi ngôn ngữ (Activity recreate) | lead | Đã sửa (52, F_rtl_language) |
| V01 | designer | Font theme đè textAppearance → toàn app Be Vietnam Regular | lead | Đã sửa |
| V02, V03, V07–V13, V15 | designer | Onboarding vỡ layout/tiêu đề chồng; dialog Đổi tên/Xoá; Deck B căn phải; toast Mixer; LED Sync; bo góc bài học; tab pad; switch Echo; Mixer lệch | ui | Đã sửa (designer kiểm lại) |
| V05, V06, VIS-5, VIS-10, VIS-12 | designer/tester | Splash/icon mặc định; tint M3 trên sheet; logo bị mask cắt rồi bị méo | lead | Đã sửa (F_splash_system, Z_launcher_icon) |
| VIS-8, VIS-9, N02, N03 | tester/designer | RTL: căn lề, bidi meta, mũi tên Back, tiêu đề top bar, badge BPM | ui + lead | Đã sửa (F_rtl_*, Z_rtl_*) |
| VIS-11 | tester | Nút primary khi disable không đổi hình | lead (style) + ui | Đã sửa (F_rename_blank) |
| VIS-13 | tester | Khung đen khi mở lần đầu (setApplicationLocales("vi") gây recreate) | lead | Đã sửa |
| A11Y-1 | tester | Switch Cài đặt luôn báo checked=false | ui | Đã sửa |
| N01, N04 | designer | Chữ A/B chưa dùng Chakra 600; BPM lệch baseline | ui | Đã sửa (Z_home, Z_mixer) |
| V14, V16, N03 (chữ số theo locale) | designer | Scrim màu đen hệ thống; font không có `tnum`; chữ số Ả Rập-Ấn trong Cài đặt | — | Chấp nhận (D-27) |
