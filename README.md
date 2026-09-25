# DJ Remix Pro (MixDeck)

App Android native (Kotlin + XML Views) chuyển từ thiết kế Claude Design **"Mobile app design brief"** (MixDeck). App có:
- 9 màn dọc: Splash, Onboarding, Xin quyền, Trang chủ, Thư viện, Bản ghi, Học DJ, Cài đặt, Ngôn ngữ;
- 1 bàn DJ ngang (Mixer) cùng các lớp phủ: sheet, dialog, toast, mini player, panel thư viện, FX, coach.

> **Phạm vi:** đây là bản chuyển giao diện và hành vi của prototype. **Chưa có âm thanh thật**: Mixer, REC và mini player được giả lập bằng ViewModel, dùng đúng công thức của prototype (DECISIONS D-07). Dữ liệu bài hát, bản ghi và bài học là **mock**. Xem mục "Thật / mock / chưa kiểm chứng" bên dưới.

## Yêu cầu và cách chạy
- JDK 17 trở lên (JAVA_HOME). Gradle daemon tự dùng JDK 21 qua foojay toolchain.
- Android SDK có platform **36.1** và build-tools 36.1.0 (`local.properties` → `sdk.dir`).
- Gradle 9.3.1 (wrapper), AGP 9.1.1, Kotlin tích hợp sẵn trong AGP (2.2.10).

Chạy trên Windows, tại thư mục gốc dự án:
```
.\gradlew.bat :app:assembleDebug        # APK: app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat :app:lintDebug            # báo cáo: app\build\reports\lint-results-debug.html
.\gradlew.bat :app:testDebugUnitTest    # báo cáo: app\build\reports\tests\testDebugUnitTest\index.html
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
Test chạy trên JVM, không cần emulator.

## Cấu trúc
```
app/src/main/java/com/example/djremixpro/
  MainActivity.kt            shell: top bar, NavHost, mini player, bottom nav, toast (ToastHost, ShellNavigator)
  app/                       DJRemixProApp (áp dụng theme/ngôn ngữ), AppContainer (DI thủ công)
  core/model/                Track, Recording, AppSettings, Languages, SettingsOptions, NavArgs…
  core/data/                 interface repository; fake/ (dữ liệu mock), settings/ (DataStore)
  core/util/                 TimeFormat, TextUtils2, ParkMiller, WaveformGenerator, MixMath, BpmFormat
  core/ui/                   UiText, ShellContracts; widget/ (custom View: Jog, Knob, Fader, Crossfader,
                             Waveform, Overview, VU, XYPad, MiniWaveform, Coach, MiniPlayer, Toast…)
  feature/<màn>/             Fragment + Adapter (UI) · ViewModel + UiState (logic)
  feature/mixer/             MixerActivity (ngang, immersive) + view/ + MixerViewModel
app/src/main/res/            layout, navigation/nav_graph.xml, values(-night) tokens md_*, font/, drawable/ic_*
design/source/               nguồn thiết kế gốc (App/Mixer/MixDeck.dc.html, support.js, check.jpg)
design/DESIGNER_REPORT.md    phân tích thiết kế chi tiết · design/VISUAL_REVIEW.md: đối chiếu ảnh
verification/screens/       ảnh chụp emulator dùng làm bằng chứng
```
Tài liệu điều phối:
- `DESIGN_ANALYSIS.md`: danh mục màn/hành vi để đối chiếu, theo mã S/O/B.
- `IMPLEMENTATION_CONTRACT.md`: hợp đồng API và quyền sở hữu file.
- `DECISIONS.md`: các quyết định D-01 → D-27.
- `TEST_MATRIX.md`: ma trận kiểm thử và nhật ký chạy.

Kiến trúc: một Activity cho luồng dọc, dùng Navigation Fragment, cộng `MixerActivity` riêng cho chế độ ngang. Mỗi màn có một ViewModel expose `StateFlow<UiState>` và `Flow<Event>`. Không dùng Compose, không dùng WebView, không dùng Hilt.

## Thật / mock / chưa kiểm chứng
| Loại | Nội dung |
|---|---|
| **Thật** | Điều hướng, bottom nav, Back; theme Tối/Sáng/Theo hệ thống; chọn ngôn ngữ (per-app locale, RTL); mọi cài đặt và cờ onboarding lưu bằng DataStore, còn giữ sau khi tắt app; xin quyền READ_MEDIA_AUDIO/READ_EXTERNAL_STORAGE; tìm kiếm và sắp xếp thư viện; lọc thuật ngữ; đổi tên/xoá/lưu bản ghi (in-memory); toàn bộ tương tác Mixer ở mức UI (jog, knob, fader, crossfader, pad, XY pad, coach) |
| **Mock / giả lập** | Danh sách 10 bài, 4 bản ghi, phiên mix, 7 bài học, thuật ngữ, mẹo (in-memory, về seed khi khởi động lại app); phát nhạc, mini player, BPM/pitch/sync, VU, waveform, REC; quảng cáo là placeholder; chia sẻ bản ghi hiện toast |
| **Không có** | Engine âm thanh, quét MediaStore, ghi file MP3/WAV, bản dịch ngoài tiếng Việt (chọn ngôn ngữ khác vẫn hiện chữ tiếng Việt, D-15), kịch bản coach cho 6 bài học còn lại |
| **Chưa kiểm chứng** | Rung phản hồi (emulator không rung), xoay máy khi đang ở Mixer, máy thật / màn hình lớn / foldable, TalkBack đầy đủ |

## Trạng thái kiểm thử (bản cuối `verification/app-debug-final.apk`, 2026-09-25 14:21)
| Bước | Kết quả |
|---|---|
| `:app:assembleDebug` | Thành công |
| `:app:lintDebug` | **0 lỗi**, 115 warning. Không chặn build: tài nguyên chưa dùng, gợi ý KTX/Overdraw, và gợi ý nâng phiên bản mà nhóm cố ý không nâng (D-02) |
| `:app:testDebugUnitTest` | **213/213 pass**: util 53, repository 18, ViewModel 142 |
| Kiểm thử thủ công trên emulator Pixel_8 (API 34, 1080×2400) | T01–T12: 9 đạt, 3 đạt một phần (xoay máy, rung, giữ jog + scratch chưa kiểm được) |
| Đối chiếu thiết kế (designer, 2 vòng) | V01–V13, V15, N01–N04 đã sửa; V14, V16 và chữ số theo locale được chấp nhận (D-27); không còn sai khác mức cao hay trung bình |

Bằng chứng: ảnh trong `verification/screens/` (`F_*`, `Z_*`, `5x_*`…), `TEST_MATRIX.md` (mục 5, 7, 8) và `design/VISUAL_REVIEW.md`.

**Chưa kiểm chứng trực quan:**
- Trạng thái người mới trên Trang chủ và Bản ghi trống: dữ liệu seed luôn có bản ghi (D-09), nên trạng thái này chỉ được kiểm bằng unit test.
- Cảnh báo VU quá tải và waveform "đang phân tích": chỉ có trên canvas, không phát sinh với dữ liệu mock.
- Xoay máy, rung phản hồi, máy thật.
# DJ-Remix-Pro
