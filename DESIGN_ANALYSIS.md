# DESIGN_ANALYSIS — MixDeck (DJ Remix Pro)

Nguồn thiết kế: Claude Design project "Mobile app design brief" (`ed6e3070-88b2-4aed-a92e-3bc21530a74e`), tải nguyên văn vào `design/source/` (App.dc.html, Mixer.dc.html, MixDeck.dc.html, support.js, screenshots/check.jpg; không file nào bị cắt).
Phân tích chi tiết (dẫn nguồn file:dòng, công thức, dữ liệu mẫu): **`design/DESIGNER_REPORT.md`**. File này là bản tóm tắt để đối chiếu implementation: mỗi mục có mã (S/O/B) dùng trong ma trận kiểm thử và báo cáo cuối.

Quy ước: 1 CSS px = 1 dp; font px = sp. Thứ tự ưu tiên khi nguồn mâu thuẫn: App/Mixer tương tác > canvas t4 > t3 > t2 > t1. Premium đã bị bỏ trong thiết kế → không port.

## 1. Màn hình (destination)

| Mã | Màn | Hướng | Nguồn | Ghi chú |
|---|---|---|---|---|
| S1 | Splash (logo + "MixDeck") | dọc | canvas #2d | |
| S2 | Onboarding 3 trang ("Hai deck, một chiếc điện thoại" / "Chạm SYNC để hai bài chung nhịp" / "Ghi lại bản mix và gửi cho bạn bè"), Bỏ qua, Tiếp | dọc | #2e–#2g | |
| S3 | Giải thích quyền đọc nhạc: "Cho phép truy cập nhạc" / "Dùng bài mẫu trước" | dọc | #2h | |
| S4 | Trang chủ: card "Sẵn sàng mix?", bài mẫu (người mới) hoặc "Tiếp tục phiên trước" (người quay lại), lưới Học DJ, banner quảng cáo, "Bản mix gần đây" | dọc | App isHome, #3a #3b #4c | |
| S5 | Thư viện: ô tìm, chip 5 tab, đếm + Sắp xếp, danh sách bài có badge BPM | dọc | App isLib, #3c #4d | |
| S6 | Bản ghi của tôi: danh sách có mini waveform + ⋮; trạng thái trống | dọc | App isRec, #2l #2m | |
| S7 | Học DJ: segmented Hướng dẫn / Thuật ngữ / Mẹo | dọc | App isLearn, #3d | |
| S8 | Cài đặt: segmented Giao diện + 4 nhóm (Âm thanh, Ghi âm, Mixer, Chung) | dọc | App isSettings, #4e | |
| S9 | Ngôn ngữ: 17 ngôn ngữ, radio | dọc | App isLang, #4f | |
| S10 | Mixer 2 deck (Đĩa/Pad; tab Cue/Loop/FX/Sampler), cột EQ/fader/VU, crossfader, top bar REC + waveform | **ngang** 800×360 | Mixer.dc.html, #1a–#1c, #4a #4b, check.jpg | |

## 2. Lớp phủ

| Mã | Lớp phủ | Nguồn |
|---|---|---|
| O1 | Bottom sheet bài hát: Nạp vào Deck A / Deck B / Nghe thử | App:244–258, #2k |
| O2 | Bottom sheet bản ghi: Đổi tên / Chia sẻ / Xoá | App:331–337 |
| O3 | Toast (✓ + chữ, màu theo ngữ cảnh, 2200 ms, cách đáy 88) | App:260–264 |
| O4 | Mini player 64dp (pause/stop, tên, thời gian) | App:227–234 |
| O5 | Panel thư viện trong Mixer "Chọn bài cho Deck X" + badge "Hợp nhịp A" | Mixer:319–359, #1c |
| O6 | Sheet FX (Echo): switch, Wet/Dry, Độ dài theo beat, XY pad + readout | Mixer:217–264, #2a |
| O7 | Dialog Lưu bản mix: tên, MP3/WAV (+ ghi chú WAV), chất lượng, Bỏ / Lưu | Mixer:266–295, #2b |
| O8 | Coach mark bước 3/5 "Chạm Sync ở Deck B" → "Hai bài đã chung nhịp" | Mixer:297–317, #2c |
| O9–O11 | Chỉ có ở canvas (sheet chuẩn bị ghi, dialog Đặt lại/Dừng ghi, toast có action, banner lỗi, cảnh báo quá tải) | MixDeck:663–726 — xem DECISIONS D-07 |

## 3. Luồng điều hướng

```
S1 Splash ─(chưa onboarding)► S2 ─Tiếp×3 / Bỏ qua► S3 ─Cho phép (dialog hệ thống) | Dùng bài mẫu► S4
S1 Splash ─(đã onboarding)► S4
Bottom nav: S4 ⇄ S5 ⇄ S6 ⇄ S7   ·   icon Cài đặt (tab chính) ► S8 ► Ngôn ngữ ► S9   ·   Back: S9→S8→tab trước
S4: Vào bàn DJ / Mix với bài mẫu / Tiếp tục phiên trước ► S10 · ô Học DJ ► S7 (segment tương ứng) · Xem tất cả ► S7 / S6 · play bản mix gần đây ► O4
S5: chạm bài ► O1 ► Nạp A/B (toast O3, ghi vào phiên mix) | Nghe thử (O4) · Sắp xếp: Tên→Nghệ sĩ→BPM→Mới thêm
S6: chạm dòng ► phát/dừng (O4) · ⋮ ► O2 · trống: Vào bàn DJ ► S10
S7: bài học ► S10 (bài "Dùng Sync" mở O8) · Xem trên mixer ► S10
S10: chạm jog deck trống ► O5 · nhấn giữ pad FX ► O6 · REC (đang ghi) ► O7 · Back ► màn trước
```

## 4. Design tokens (tóm tắt — chi tiết DESIGNER_REPORT §4)
- Màu: 14 token × 2 theme (dark mặc định / light) + màu cố định (đĩa, nhãn), scrim, 10 màu cover → `res/values/colors.xml`, `res/values-night/colors.xml` (tên `md_*`).
- Font: Be Vietnam Pro 400/500/600 (UI), Chakra Petch 500/600 (tiêu đề, số, tabular) → `res/font/`, `TextAppearance.MixDeck.*` trong `styles.xml`.
- Cỡ chữ 32/24/18/16/14/13/12; bo góc 4–24; touch ≥ 44; top bar 60, nav 72, mini player 64; Mixer top 72, deck 280, jog 152 → `dimens.xml`.
- Thiết kế phẳng, không shadow; phân lớp bằng booth < panel < raised + viền line.

## 5. Hành vi cần đối chiếu (B = behavior)

| Mã | Hành vi | Nguồn công thức |
|---|---|---|
| B01 | Theme Tối/Sáng/Theo hệ thống đổi ngay toàn app | App:281–283 |
| B02 | Chọn ngôn ngữ: radio + toast "Đã chọn {tên bản địa}", Back về Cài đặt | App:284 |
| B03 | 3 switch Cài đặt (pre-cue off, rung on, sáng màn hình on) | App:269 |
| B04 | Sắp xếp thư viện vòng 4 chế độ, mặc định BPM; BPM null xuống cuối; so sánh chuỗi collator "vi" | App:308–312 |
| B05 | Badge BPM "…" khi chưa phân tích; sheet meta "Đang phân tích nhịp…" | App:313, 325 |
| B06 | Chữ tắt cover = chữ cái đầu của 2 từ đầu, viết hoa | App:307 |
| B07 | Chỉ một bản phát tại một thời điểm; chạm lại cùng bản → dừng | App:315 |
| B08 | Bản ghi: dòng đang phát có nền panel + " · Đang phát"; mini waveform theo seed Park–Miller | App:278, 314–316 |
| B09 | Học DJ: 7 bài, 2 xong, bài 3 hiện tại có "Bắt đầu" | App:318–319 |
| B10 | Deck trống: "Chưa có bài", BPM "—", "−−:−−", jog viền dashed, transport mờ .35 và không phản hồi | Mixer:437–457 |
| B11 | Play/Pause, Sync, Scratch toggle; Sync tính pitch = (bpm deck kia / bpm gốc − 1)·100; BPM hiệu dụng 1 chữ số thập phân | Mixer:437–447 |
| B12 | Nhãn pitch "±x.x%" (dấu − U+2212), vị trí núm = 50 − pitch/8·50 (%) | Mixer:441–447 |
| B13 | Jog quay 1.8/(1+pitch/100) s/vòng khi phát và không (chạm + scratch); vòng sáng khi chạm | Mixer:448–452 |
| B14 | Waveform cuộn 32·bpm/60 dp/s, 32 dp = 1 beat, vùng loop 4 beat | Mixer:399–410, 468 |
| B15 | Knob EQ: góc (v−.5)·270°, arc hai phía, kill (đỏ); kéo dọc, chạm đúp về giữa, nhấn giữ kill | Mixer:411–420, MixDeck:539 |
| B16 | Crossfader 0..1, chạm đúp về giữa, nấc giữa rung | Mixer:481–483 |
| B17 | Pad 4×2 theo tab (Cue 1–8, Loop 1/4…16 + In/Out, 8 FX, 8 sampler) | Mixer:422–433 |
| B18 | REC: bộ đếm mm:ss mỗi giây, chấm nhấp nháy; dừng → dialog Lưu | Mixer:484–485, #2b |
| B19 | Panel thư viện: "Hợp nhịp A" khi |bpm/bpmA − 1| ≤ 3% (chỉ khi chọn cho Deck B); nạp bài → deck dừng | Mixer:496–500 |
| B20 | XY pad: readout "{1/16…1} beat · {độ mạnh}%" | Mixer:486–488 |
| B21 | Dialog lưu: MP3/WAV, ghi chú WAV | Mixer:490–492 |
| B22 | Coach: bước 3/5, chạm Sync Deck B → trạng thái hoàn thành | Mixer:493 |
| B23 | Toast tự tắt sau 2200 ms, mở toast thì đóng sheet | App:279 |

## 6. Ngoài phạm vi / không có trong thiết kế
Âm thanh thật (engine DJ), quảng cáo thật, Premium, màn con của Playlist/Album/Nghệ sĩ/Thư mục, kịch bản coach cho 6 bài còn lại, trạng thái bị từ chối quyền. Cách xử lý: `DECISIONS.md`.
