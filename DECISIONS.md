# DECISIONS

Quyết định kỹ thuật và sản phẩm do team lead chốt. Mã D-xx được tham chiếu trong code review và README.

## Môi trường và build
- **D-01 Không có git.** Thư mục không phải git repo, nên không có thay đổi cũ của người dùng cần giữ. Bản chụp gốc (trước khi sửa) nằm trong scratchpad của phiên, tệp `baseline/djremixpro-baseline.tar`. Không tự `git init`.
- **D-02 Bộ phiên bản.** Giữ AGP 9.1.1 / Gradle 9.3.1 / Kotlin tích hợp sẵn trong AGP (2.2.10) / compileSdk 36.1 / minSdk 26 / targetSdk 36. Thư viện chọn bản đã có trong cache Gradle, không nâng chỉ vì có bản mới: appcompat 1.7.1, material 1.13.0, constraintlayout 2.2.1, recyclerview 1.4.0, fragment 1.8.9, activity 1.13.0, lifecycle 2.11.0, navigation 2.10.2, datastore 1.2.1, coroutines-test 1.9.0.
- **D-03 core-ktx hạ về 1.18.0.** Template gốc dùng core-ktx 1.19.1, bản này yêu cầu compileSdk ≥ 37 nên cấu hình gốc không build được (đã tái hiện bằng `assembleDebug`). Chọn hạ core-ktx thay vì nâng compileSdk, vì đây là thay đổi nhỏ nhất.
- **D-04 Bỏ Compose, dùng XML Views + ViewBinding** theo yêu cầu. Xoá plugin `kotlin.compose`, các thư viện Compose và `ui/theme/*.kt`. Java target nâng lên 17.
- **D-05 Font đóng gói trong `res/font/`** (OFL): Be Vietnam Pro 400/500/600 (TTF lấy từ máy) và Chakra Petch 500/600 (tải từ github.com/google/fonts). Không dùng downloadable fonts, vì cách đó phụ thuộc Google Play Services.
- **D-06 Chỉ lead chạy Gradle** trong giai đoạn triển khai. Teammate phải xin lượt; tại mỗi thời điểm chỉ một agent chạy Gradle.

## Phạm vi sản phẩm
- **D-07 Không có audio thật (giai đoạn 1).** Prototype không phát âm thanh. Engine DJ thật (time-stretch, EQ, FX, ghi mix) cần Oboe/AAudio cùng DSP native, nên nằm ngoài phạm vi. Mixer, mini player và REC được **giả lập bằng ViewModel**: timer, vị trí phát, BPM/pitch và animation, tất cả khớp công thức của prototype. Sheet chuẩn bị ghi (O9), dialog "Đặt lại" và "Dừng ghi và lưu" (O10), toast có action, banner lỗi và cảnh báo quá tải (O11) chỉ có trên canvas, không có trong prototype tương tác, nên **chưa port**.
- **D-08 Dữ liệu mock.** Danh sách bài (10), bản ghi (4), bài học, thuật ngữ và mẹo lấy nguyên văn từ prototype, lưu in-memory trong `core.data.fake`. Thao tác Đổi tên, Xoá và Lưu bản mix thay đổi dữ liệu in-memory và **mất khi tắt app**. Cài đặt (theme, ngôn ngữ, switch, giá trị) và cờ đã xem onboarding được lưu thật bằng DataStore. Các giá trị mock mà thiết kế không có, do logic-developer bổ sung:
  - 2 bài mẫu có nghệ sĩ "MixDeck", dài 3:00 và 3:20, màu cover lấy theo màu nhãn đĩa.
  - Bản ghi "Tiệc sinh nhật Linh" tạo lúc 20:00.
  - Khi mở Mixer, EQ ở 0,5 và volume A/B là 0,86/0,72 (giá trị demo).
- **D-09 Người mới / người quay lại.** `isNewUser = bản ghi rỗng && không có phiên mix`. Dữ liệu seed có 4 bản ghi và một phiên (A "Sài Gòn lên đèn", B "Mưa sao băng"), nên mặc định là người quay lại, khớp prop mặc định `user='returning'`. Trạng thái người mới được kiểm chứng bằng unit test.
- **D-10 Quyền đọc nhạc.** Nút "Cho phép truy cập nhạc" xin quyền thật (READ_MEDIA_AUDIO trên API 33+, READ_EXTERNAL_STORAGE trên API ≤ 32). Dù người dùng cho phép hay từ chối, và kể cả khi bấm "Dùng bài mẫu trước" hoặc "Bỏ qua", app đều đánh dấu đã xong onboarding rồi vào Trang chủ. Vì thư viện là dữ liệu mock (D-08), quyền chưa được dùng để quét MediaStore. Thiết kế không có màn "bị từ chối quyền".
- **D-11 Mixer là Activity riêng** (`MixerActivity`, `sensorLandscape`, immersive), đặt trong `nav_graph` dưới dạng destination kiểu `<activity>`, tham số `entry` và `lessonId`. Cách này tránh việc app dọc bị tạo lại khi đổi hướng. Hai activity chia sẻ trạng thái qua `MixSessionRepository`. Mixer luôn giữ hướng LTR (Deck A trái, Deck B phải) kể cả khi ngôn ngữ là RTL.
- **D-12 REC.** Bấm REC để bắt đầu đếm. Bấm lần nữa để dừng; dialog Lưu (O7) mở ra với thời lượng vừa ghi, tên mặc định "Mix dd-MM HH:mm", định dạng và chất lượng lấy theo Cài đặt. "Lưu bản mix" thêm bản ghi vào danh sách in-memory và hiện toast "Đã lưu bản mix"; "Bỏ" huỷ bản ghi. Bộ đếm về 0 sau khi dừng.
- **D-13 Menu bản ghi dùng bottom sheet** (theo App.dc.html, không dùng popup của #2l cũ). Đổi tên mở dialog nhập tên (không cho tên rỗng). Xoá có dialog xác nhận rồi xoá khỏi danh sách; đây là bổ sung an toàn so với prototype, vốn chỉ hiện toast. Chia sẻ hiện toast "chưa khả dụng trong bản mock", vì không có tệp âm thanh thật.
- **D-14 Dòng giá trị trong Cài đặt** mở dialog chọn một giá trị (MaterialAlertDialog), với danh sách lựa chọn trong `SettingsOptions`. Ba dòng có hành vi riêng: "Thư mục lưu" hiện toast (thư mục cố định), "Quyền truy cập" mở màn thông tin ứng dụng của hệ thống, "Liên hệ hỗ trợ" hiện toast chưa khả dụng.
- **D-15 Ngôn ngữ.** Lựa chọn được lưu và áp dụng bằng `AppCompatDelegate.setApplicationLocales`. **App chỉ có chuỗi tiếng Việt**, nên với ngôn ngữ khác, chữ vẫn là tiếng Việt; chỉ hướng bố cục (RTL với tiếng Ả Rập) và định dạng hệ thống thay đổi. Số trên Mixer luôn định dạng theo `Locale.US` (dấu chấm thập phân) như thiết kế.
- **D-16 Theme.** Tối là mặc định. `values/` chứa bảng màu sáng, `values-night/` chứa bảng màu tối, áp dụng bằng `AppCompatDelegate.setDefaultNightMode`. Đĩa jog và nhãn đĩa giữ màu tối cố định trong cả chế độ sáng, đúng như thiết kế (#4a).
- **D-17 Thư viện.** Ô tìm kiếm lọc thật theo tên bài và nghệ sĩ, không phân biệt hoa thường và dấu tiếng Việt. Số bài hiển thị theo số thực tế sau khi lọc, không dùng con số cố định "248 bài" của prototype. Chip Playlist, Album, Nghệ sĩ, Thư mục chỉ đổi trạng thái chọn, giống prototype: thiết kế không có màn con cho các chip này. Nạp bài vào deck khi Mixer chưa mở sẽ ghi bài vào phiên mix; lần tới mở Mixer, deck đó đã có bài.
- **D-18 Học DJ.** Tiến độ 2/7 là dữ liệu mock. Bấm một bài học sẽ mở Mixer; riêng bài "Dùng Sync" (bài 2) có coach mark (O8), vì thiết kế chỉ có kịch bản cho bài này. Coach tự hoàn thành khi bật Sync ở Deck B; bấm "Tiếp" hoặc "Bỏ qua" để đóng.
- **D-19 Mixer: các nút prototype chưa có hành vi.**
  - Nút "Cài đặt nhanh" (icon tune) mở panel thư viện, đúng handler `openLib` mà prototype gắn cho nút này (Mixer:494). Deck đích là deck trống đầu tiên; nếu cả hai deck đều có bài thì chọn B, theo `libTarget` mặc định là 'B'. Nếu không có lối vào này, panel thư viện (O5) không mở được khi cả hai deck đã có bài, vì dữ liệu seed luôn có phiên mix. Việc này được phát hiện khi kiểm thử trên emulator.
  - Nút Cue: dừng phát và đưa vị trí về điểm cue (0:00).
  - Overview: chạm hoặc kéo để tua.
  - Pitch fader kéo tay được trong phạm vi Cài đặt (mặc định ±8%); kéo tay sẽ tắt Sync.
  - Jog: khi đang chạm và bật Scratch, xoay 360° tương ứng 1,8 s vị trí phát.
- **D-20 Pad (mock).**
  - Cue: pad 1–3 có sẵn 0:32 / 1:04 / 2:08. Chạm pad trống để đặt điểm tại vị trí hiện tại; chạm pad đã đặt để nhảy tới điểm đó.
  - Loop: chạm một độ dài để bật loop với độ dài đó, chạm lại để tắt.
  - FX: chạm để bật/tắt hiệu ứng; nhấn giữ để mở sheet FX của hiệu ứng đó.
  - Sampler: chạm để bật/tắt (màu trung tính).
  - Không có âm thanh (D-07).
- **D-21 Ánh xạ EQ.** Giá trị v ∈ [0, 1] ánh xạ sang dB = (v − 0,5) × 27, khớp ví dụ "Mid +3.5 dB" tại v ≈ 0,63 trong thiết kế. Filter: v < 0,5 là LPF, v > 0,5 là HPF, nhãn hiển thị theo %. Trạng thái Kill hiện "Kill".
- **D-22 FX.** Readout của XY pad theo đúng công thức Mixer:486–488 (1/16…1 beat). Hàng "Độ dài theo beat" (1/4…4) chỉ là một lựa chọn trạng thái, không đổi readout. Thiết kế có mâu thuẫn ở điểm này và lead giữ đúng nguyên văn. Wet/Dry cố định 60% như thiết kế.
- **D-23 Quảng cáo** chỉ là placeholder (`showAd = true`), không tích hợp SDK.
- **D-25 Splash chỉ ở lần mở đầu.** Canvas đặt Splash, Onboarding và Xin quyền trong nhóm "Mở app lần đầu" (MixDeck:109). `nav_graph` bắt đầu ở `homeFragment` để `NavigationUI` chuyển tab và xử lý Back đúng. Khi chưa xong onboarding, MainActivity điều hướng sang `splashFragment` (pop Home). Từ những lần mở sau, app vào thẳng Trang chủ; cửa sổ khởi động của hệ thống vẫn hiện icon app như thường.
- **D-26 Chi tiết UI mà thiết kế chưa định nghĩa** (ui-developer đề xuất, lead chấp thuận):
  - Khi dialog Lưu bản mix đang mở, Back không đóng dialog để tránh mất bản ghi; người dùng phải chọn "Bỏ" hoặc "Lưu bản mix".
  - Núm pitch được kẹp trong rãnh để không bị cắt ở hai đầu, nên lệch nhẹ so với công thức `pitchPos% − 8dp`.
  - Trên màn ngang hẹp hơn 800dp, mỗi deck co từ 280dp xuống tối thiểu 220dp để cột mixer ở giữa còn ít nhất 216dp; jog wheel co giãn theo.
  - Ô "Tìm thuật ngữ" lọc thật, giống D-17.
  - Waveform bám theo vị trí phát (hợp đồng v1.2).
- **D-27 Giới hạn hiển thị được chấp nhận.**
  - Be Vietnam Pro và Chakra Petch không có tính năng OpenType `tnum`, nên số thời gian và BPM có thể xê dịch bề rộng vài px khi đổi giá trị (VISUAL_REVIEW, mức thấp). App vẫn giữ `fontFeatureSettings="tnum"`; font nào hỗ trợ thì dùng, không hỗ trợ thì bị bỏ qua.
  - Với các locale dùng chữ số riêng (ví dụ tiếng Ả Rập), giá trị trong Cài đặt và số bài được định dạng theo locale, như "٤٠ ms". Mixer và BPM luôn định dạng theo `Locale.US` (D-15). Đây là hành vi bản địa hoá đúng nên được giữ (VISUAL_REVIEW N03).
  - Scrim của bottom sheet và dialog dùng màu dim của hệ thống (đen, với alpha đúng như thiết kế: 0,66 và 0,7), không dùng màu #141019 tuyệt đối.
- **D-24 Toast** được neo ngay trên mini player hoặc bottom nav nên không đè lên mini player (thiết kế gốc có chồng lấn).
