# DESIGNER_REPORT — MixDeck (Claude Design → Android Views)

Nguồn: `design/source/` (lead tải bằng DesignSync, không file nào bị cắt).
Ký hiệu dẫn nguồn: `App:NN` = `App.dc.html` dòng NN, `Mixer:NN`, `MixDeck:NN`, `support:NN`. Id canvas (`#2k`, `#4e`…) là id section/artboard trong `MixDeck.dc.html`.

Giả định: **1 CSS px = 1 dp** (canvas ghi rõ "Khung 800 × 360 dp" `MixDeck:483` và "màn dọc 360 × 800 dp" `MixDeck:92`); font px → sp.
Không thấy đoạn nào trong các file có dạng lệnh gửi cho AI. Nội dung chỉ là thiết kế và code prototype.

---

## 0. Tổng quan nguồn

| File | Vai trò | Khung |
|---|---|---|
| `App.dc.html` | Shell app dọc: 4 tab (Trang chủ, Thư viện, Bản ghi, Học DJ), Cài đặt, Ngôn ngữ, bottom sheet, toast, mini player | 360×800, portrait, `data-props $preview` `App:267` |
| `Mixer.dc.html` | Bàn DJ 2 deck; các biến thể `variant` = play, pad, library, fx, save, coach | 800×360, **landscape** `Mixer:362` |
| `MixDeck.dc.html` | Canvas tài liệu (`design_doc_mode=canvas` `MixDeck:11`) nhúng App/Mixer bằng `<dc-import>`, kèm các màn tĩnh chỉ có ở đây: splash, onboarding, xin quyền, bảng component/trạng thái | — |
| `support.js` | Runtime chung của Claude Design (dc-runtime), **không chứa logic app** | — |
| `screenshots/check.jpg` | Ảnh chụp artboard `#1a` (Mixer dark, variant play) | 909×525 |

**Thứ tự phiên bản của canvas:** section đứng trên cùng là mới nhất. Thứ tự: `t4` (mới nhất) → `t3` → `t2` → `t1` (cũ nhất). Khi canvas tĩnh (t2/t1) khác với App/Mixer tương tác, **ưu tiên App/Mixer và t4/t3**. Các điểm khác biệt được liệt kê ở mục 8.

Quyết định sản phẩm ghi trong canvas: *"Đã bỏ Premium: 8 hot cue, toàn bộ FX, sampler và ghi WAV đều miễn phí."* (`MixDeck:30`). Vì vậy artboard `2p` (bảng so sánh Premium) đã bị bỏ: id nhảy từ `2o` sang `2q`, nhưng mảng `premRows` vẫn còn trong script (`MixDeck:770`) mà không được render. Pad `locked`/nhãn "Premium" vẫn còn trong template (`Mixer:150`, `MixDeck:603`) nhưng không dùng. **Không port Premium.**

### 0.1 support.js — runtime (tóm tắt)
- `parseDcDocument` (`support:24`) lấy template bên trong `<x-dc>`, lấy JS trong `<script type="text/x-dc" data-dc-script>` và `data-props` (JSON; khoá `$preview` là kích thước preview, các khoá còn lại là prop có `editor` enum/boolean và `default`).
- Script định nghĩa `class Component extends DCLogic`. `DCLogic` = `StreamableLogic` (`support:817`) với `props`, `state`, `setState`, `componentDidMount/DidUpdate/WillUnmount` và `renderVals()`. Runtime ghép `{...props, ...renderVals()}` (`support:1085`) rồi render template bằng React 18 (tải từ unpkg, `support:1143`).
- Template: `{{ expr }}` là đường dẫn thuộc tính hoặc so sánh đơn giản (`support:205`). `<sc-if value>` render có điều kiện. `<sc-for list as>` lặp. `<dc-import name=… prop=…>` nhúng component khác (`support:559`); thuộc tính kebab (`start-tab`) được đổi sang camel (`startTab`). `style-hover="…"` thành lớp CSS `:hover` (`support:429`, `support:1567`). `hint-placeholder-*` và `hint-size` chỉ là gợi ý khi render placeholder.
- **Hệ quả cho Android:** toàn bộ logic app nằm trong `renderVals()` của từng `.dc.html` (mục 5). Mọi `style-hover` là hiệu ứng web; trên Android thay bằng ripple/pressed state.

---

## 1. Danh sách màn hình / view

Tổng cộng **10 màn hình** (destination) và **11 lớp phủ** (sheet/dialog/overlay/toast/panel).

| # | Màn hình | Nguồn | Hướng |
|---|---|---|---|
| S1 | Splash | canvas `#2d` `MixDeck:112` | dọc |
| S2 | Onboarding (3 trang) | `#2e` `#2f` `#2g` `MixDeck:120-175` | dọc |
| S3 | Giải thích trước khi xin quyền đọc nhạc | `#2h` `MixDeck:177` | dọc |
| S4 | Trang chủ (biến thể người mới / người quay lại) | App `isHome` `App:30-97`; `#3a` `#3b` `#4c`; tĩnh `#2i` `#2j` | dọc |
| S5 | Thư viện | App `isLib` `App:99-120`; `#3c` `#4d` `#2k` | dọc |
| S6 | Bản ghi của tôi (có dữ liệu / trống) | App `isRec` `App:122-143`; `#2l` `#2m` | dọc |
| S7 | Học DJ (3 segment: Hướng dẫn, Thuật ngữ, Mẹo) | App `isLearn` `App:145-189`; `#3d` `#2n` `#2o` | dọc |
| S8 | Cài đặt | App `isSettings` `App:191-212`; `#4e` (mới nhất); `#2q` (cũ) | dọc |
| S9 | Ngôn ngữ | App `isLang` `App:214-224`; `#4f` | dọc |
| S10 | Mixer (bàn DJ) | `Mixer.dc.html`; `#1a` `#1b` `#1c` `#2a` `#2b` `#2c` `#4a` `#4b` | **ngang** |

Lớp phủ:

| # | Overlay | Nguồn |
|---|---|---|
| O1 | Bottom sheet bài hát (Nạp Deck A/B, Nghe thử) | `App:244-258`, `App:324-330`; `#2k` |
| O2 | Bottom sheet bản ghi (Đổi tên, Chia sẻ, Xoá) | `App:331-337`. `#2l` vẽ dạng popup menu, xem mục 8 |
| O3 | Toast (icon ✓ + text, 2,2 s) | `App:260-264`, `flash()` `App:279` |
| O4 | Mini player | `App:227-234`; `#2l` |
| O5 | Panel thư viện trong Mixer ("Chọn bài cho Deck X") | `Mixer:319-359`; `#1c` |
| O6 | Bottom sheet FX chi tiết (Echo + XY pad) | `Mixer:217-264`; `#2a` |
| O7 | Dialog "Lưu bản mix" | `Mixer:266-295`; `#2b` |
| O8 | Coach mark (hướng dẫn tương tác, bước 3/5) | `Mixer:297-317`; `#2c` |
| O9 | Sheet "Ghi âm bản mix" (trước khi ghi, có switch micro) | chỉ có ở canvas `MixDeck:682-693` |
| O10 | Dialog "Đặt lại bản mix?" và "Dừng ghi và lưu bản mix?" | chỉ có ở canvas `MixDeck:695-710` |
| O11 | Toast có action ("Đã lưu bản mix", Nghe, Chia sẻ); banner lỗi tệp; banner lỗi âm thanh ("Mở Cài đặt"); cảnh báo quá tải VU | chỉ có ở canvas `MixDeck:663`, `MixDeck:713-726` |

### 1.1 Khung chung của app dọc (App shell) — thứ tự từ trên xuống `App:19-265`
1. Thanh trạng thái giả 28dp (`App:20`). **Chỉ để trang trí**, dùng status bar hệ thống (edge-to-edge).
2. Top bar 60dp, padding ngang 8, gap 4 (`App:22-26`): nút Back 44×44 (chỉ hiện ở màn phụ, `isSub`) · tiêu đề (Chakra Petch 600 24/30, padding-left 12, flex 1) · nút Cài đặt (icon tune 22, màu muted, chỉ hiện ở 4 tab chính `isMain`). Tiêu đề theo tab `App:306`: home "MixDeck", lib "Thư viện", rec "Bản ghi của tôi", learn "Học DJ", settings "Cài đặt", lang "Ngôn ngữ".
3. Vùng nội dung cuộn dọc (`App:28`): padding 4/16/24, các khối con cách nhau (gap) 24, ẩn scrollbar.
4. Mini player 64dp (khi `playing != null`, `App:227`).
5. Bottom nav 72dp (chỉ hiện ở tab chính, `App:236-242`): nền panel, viền trên 1px line; 4 mục đều nhau; mỗi mục gồm pill 56×30 bo 15 (nền raised khi đang chọn) chứa icon 22, bên dưới là label 12/16 (600 khi chọn, 500 khi không).
6. Lớp phủ: sheet, toast.
7. Bo góc khung 20 (`App:19`) là khung thiết bị, **không port**.

### 1.2 S4 Trang chủ `App:30-97`
1. **Card "Sẵn sàng mix?"** (panel, bo 16, padding 20, gap 14, căn giữa): 2 đĩa minh hoạ 76×76 chồng nhau trong khung 132×76 (viền 2dp màu a/b, vân đĩa; nhãn tròn 26 chữ "A"/"B" Chakra 12) · text 18/24 600 · nút primary "Vào bàn DJ" cao 52, full width, bo 12, nền text, chữ booth, icon ic_deck 18 bên phải → Mixer.
2. *(người mới `isNew`)* **Card "Thử mix với 2 bài mẫu"** + badge "Mới" (nền b, cao 22, bo 6) `App:42-50`: 2 dòng mẫu (ô chữ cái deck 24 bo 6 · tên · BPM Chakra 13 + "BPM" 12 muted); nút outline "Mix với bài mẫu" cao 48 (viền 1.5 line).
3. *(người quay lại)* **"Tiếp tục phiên trước"** `App:52-64`: tiêu đề 18/24 600; card panel bo 16 bấm được, gồm 2 dòng 44 (A/B · tên ellipsize · BPM) ngăn bằng line, chevron phải.
4. **"Học DJ"** + nút text "Xem tất cả" (12, muted, cao 32) → tab Học DJ. Lưới 3 cột gap 8, ô cao 72 bo 12 (icon 20 + label 12/500) `App:66-73`.
5. *(showAd)* **Banner quảng cáo** placeholder cao 72, viền dashed line, bo 12, nhãn "Quảng cáo", text "Vị trí banner quảng cáo (320 × 50)" `App:75-80`.
6. *(người quay lại)* **"Bản mix gần đây"** + "Xem tất cả" → tab Bản ghi. 2 dòng cao 56: nút tròn 40 (play/pause) · tên · thời lượng Chakra 500 14 muted, số tabular `App:82-96`.

### 1.3 S5 Thư viện `App:99-120`
1. Ô tìm kiếm cao 44, bo 12, nền panel, icon search 18 + hint "Tìm bài hát, nghệ sĩ". **Chưa có hành vi** (là div tĩnh).
2. Dải chip cuộn ngang (tràn phải −16): "Bài hát, Playlist, Album, Nghệ sĩ, Thư mục". Chip cao 32, padding 14, bo 16, chữ 12/500. Chip chọn: nền text, chữ booth; chip thường: nền panel, chữ muted.
3. Dòng meta: "248 bài" (12, muted, **cố định**) · nút "Sắp xếp: {sortLabel}" + chevron xuống.
4. Danh sách bài, mỗi dòng cao 60, gap 12 (margin-top −16 để kéo sát): cover 44 bo 8 (màu + chữ tắt Chakra 12) · tiêu đề 14/20 ellipsize · phụ đề "Nghệ sĩ · mm:ss" 12/16 muted · badge BPM (min-width 52, cao 24, bo 6, nền panel, viền line; số Chakra 600 13 + "BPM" 12 muted; khi chưa có BPM thì hiện "…" màu muted).

### 1.4 S6 Bản ghi `App:122-143`
- Có dữ liệu: danh sách dòng cao 72, bo 12 (margin ngang −8, gap 2). Khi đang phát: nền panel. Mỗi dòng gồm vùng bấm (thumbnail 56×44 bo 8 nền panel chứa **mini waveform 56×28** · tên 14/20 500 · meta "mm:ss · dd/MM/yyyy[ · Đang phát]" 12/16 muted) và nút ⋮ 44.
- Trống (`noRecs`): căn giữa, padding-top 80: vòng tròn 88 nền panel chứa icon waveform 40 muted · text "Chưa có bản mix nào. Vào bàn DJ và bấm REC để ghi bản đầu tiên." 14/20 muted căn giữa · nút primary "Vào bàn DJ" cao 52 padding 28.

### 1.5 S7 Học DJ `App:145-189`
- Segmented 3 mục (nền panel bo 12 padding 3, gap 4; nút cao 40 bo 10; mục chọn nền raised, chữ text 600; mục khác chữ muted 500).
- **Hướng dẫn:** dòng "Đã học 2/7 bài · mỗi bài mở bàn DJ và chỉ từng nút" (12 muted); 7 bài, mỗi dòng cao 60 bo 12: vòng số 32 (xong: nền raised + ✓ muted; bài hiện tại: nền text, số màu booth; chưa học: viền line 1.5, số muted) · tên 14/20 500 (bài xong: chữ muted) · "N bước" · chip "Bắt đầu" (cao 32, bo 10, nền text) chỉ ở bài hiện tại. Bấm dòng → Mixer.
- **Thuật ngữ:** ô tìm "Tìm thuật ngữ" (tĩnh); 10 mục (tên 16/22 600 · mô tả 14/20 muted · link "Xem trên mixer →" 14/600 cao 40 → Mixer), ngăn bằng line.
- **Mẹo:** 4 card (panel bo 16 padding 16 gap 8): tag (nền raised, cao 20, bo 4) + "N phút đọc" · tiêu đề 16/22 600 · nội dung 14/20 muted. Không bấm được.

### 1.6 S8 Cài đặt `App:191-212`, `App:286-297`
1. "Giao diện" (label nhóm 12/16 600 muted) + segmented Tối / Sáng / Theo hệ thống (nút cao 44, `aria-pressed`).
2. Nhóm "Âm thanh": Độ trễ âm thanh `40 ms` › · switch "Nghe trước qua tai nghe (pre-cue)" (phụ đề "Cần tai nghe tách kênh") · Âm lượng tổng mặc định `80%` ›
3. Nhóm "Ghi âm": Định dạng mặc định `MP3` › · Chất lượng `320 kbps` › · Thư mục lưu `Music/MixDeck` ›
4. Nhóm "Mixer": Phạm vi Pitch mặc định `±8%` › · Chế độ jog wheel mặc định `Đĩa` › · switch "Rung phản hồi" · switch "Giữ màn hình luôn sáng"
5. Nhóm "Chung": Ngôn ngữ `{tên bản địa}` › (→ S9) · Quyền truy cập › · Liên hệ hỗ trợ ›

Mỗi dòng cao tối thiểu 52, padding dọc 6, viền dưới line. Switch 44×26 bo 13, núm 18: bật = nền text, viền text, núm booth, left 20; tắt = nền raised, viền line2, núm muted, left 2 (`App:286`, `App:207`).

### 1.7 S9 Ngôn ngữ `App:214-224`, `App:284`
Ô tìm "Tìm ngôn ngữ" (tĩnh). 17 dòng, mỗi dòng cao tối thiểu 60, padding 8/4, viền dưới line: tên bản địa 16/22 500 (`dir=auto`) · tên tiếng Việt 12/16 muted · radio 22 (viền 2; chọn = viền text + chấm 10 màu text; không chọn = viền line2).
Thứ tự: vi, hi, es, pt-BR, en, pt-PT, fr, ar, bn, ru, de, ja, tr, ko, id, zh-Hans, zh-Hant.

### 1.8 S10 Mixer 800×360 `Mixer:23-215`
**Top bar** (cao 72, padding 7/6, gap 6) `Mixer:25-71`:
- Back 44 (muted).
- Khối REC rộng 104: nút cao 44 bo 12 nền panel, viền 1.5 (khi ghi: màu rec, không ghi: màu panel), chấm 12 viền rec 2dp (khi ghi thì tô đặc và nhấp nháy) + nhãn "REC" hoặc "mm:ss" (Chakra 500 14/18, tabular, letter-spacing .02em). Dưới nút là "Đang ghi bản mix" 12/14 màu rec (khi ghi).
- **Khung waveform** (flex, nền panel, bo 10, padding dọc 4, gap 3): 2 làn cao 18 (A trên, B dưới), mỗi làn có nhãn deck góc trái (Chakra 600 12, nền panel, bo 4); lớp mờ `--dim` phủ nửa trái (phần đã phát, cao 46); vạch phát 2×42 ở giữa màu text; bên dưới là 2 thanh overview cao 8 (gap 10, padding 6, bo 2, nền line; phần đã phát là màu deck với opacity .5; con trỏ 2dp màu text), `role=slider`.
- Nút "Cài đặt nhanh" 44 (icon tune). **Lưu ý: gắn handler `openLib`, nhưng `openLib` là hàm rỗng** `Mixer:494`.

**Thân** (top 72, cách mép 4, grid `280 | 1fr | 280`, gap 4) `Mixer:73`:
- **Deck panel** (A trái; B phải và bố cục đảo ngược `row-reverse`), nền panel, bo 12, padding 8, gap 6:
  1. Header cao 36: ô deck 24 bo 6 (nền màu deck, chữ Chakra 14 màu booth) · tiêu đề 13/18 500 + nghệ sĩ 12/16 muted (ellipsize; B căn phải) · BPM Chakra 600 22/22 tabular (khi Sync thì đổi màu deck) + "BPM" 12 muted · thời gian còn lại Chakra 500 14/14 muted (ví dụ "−02:31").
  2. Khối giữa cao 160:
     - *Chế độ Đĩa (`jogMode`):* cột Pitch rộng 40 (nhãn "Pitch" · rãnh dọc 2dp · vạch giữa 16×2 · núm 28×16 bo 5 nền raised viền line2 chứa vạch 16×2 · nhãn "±x.x%" Chakra 500 14/18) · **jog wheel 152** · cột phải rộng 48: segmented dọc "Đĩa / Pad" (nút cao 42 bo 10, nền booth bo 12 padding 2), khoảng trống, rồi nút toggle "Scratch" cao 44 (LED 6).
     - *Chế độ Pad (`padMode`):* hàng tab cao 40 (nút về Đĩa 44 + 4 tab Cue / Loop / FX / Sampler; tab chọn có gạch chân inset 2dp màu deck) · **lưới pad 4×2** gap 6, pad bo 10 viền 1.5.
  3. Hàng transport cao 56, gap 6: Play (flex 1.25), Cue (flex 1, nền raised viền line, **không có handler**), Sync (flex 1, toggle có LED). Khi deck trống: opacity .35.
- **Cột giữa (mixer)** nền panel bo 12 padding 8: grid 7 cột `44 22 8 1fr 8 22 44` × 4 hàng 44 (row-gap 6, col-gap 4):
  - cột 1: 4 knob EQ của A (High, Mid, Low, Filter) · cột 2: fader âm lượng A (cao 4 hàng) · cột 3: VU A · cột 4: nhãn hàng (12/16 muted) · cột 5: VU B · cột 6: fader B · cột 7: knob B.
  - Dưới cùng: nhãn "Crossfader" (12/14) + hàng cao 44: chữ "A" (Chakra 14 màu a) · **crossfader** · chữ "B".

---

## 2. Trạng thái từng màn hình

### App
| Màn | Trạng thái | Điều kiện / chuyển trạng thái |
|---|---|---|
| Shell | `tab` ∈ home, lib, rec, learn, settings, lang; `isMain` = 4 tab đầu → hiện bottom nav + nút Cài đặt, ẩn Back | `App:282` |
| Home | **người mới** (`user='new'`): card bài mẫu, ẩn "Tiếp tục phiên trước" và "Bản mix gần đây"; **người quay lại**: ngược lại | prop `user` `App:302`, `App:341` |
| Home | có/không banner quảng cáo | prop `showAd` (mặc định true) `App:341` |
| Home, Bản mix gần đây | từng dòng playing (nút nền text + icon pause màu booth) / paused (nút nền panel + icon play màu text) | `s.playing.n === name` `App:317` |
| Thư viện | `libTab` 0..4 (chỉ đổi màu chip, **không lọc dữ liệu**) · `sort` 0..3 · BPM null → "…" | `App:311-313`, `App:348` |
| Thư viện | **loading/analyzing**: BPM chưa có → badge "…" màu muted; sheet meta "Đang phân tích nhịp…" | `App:313`, `App:325` |
| Bản ghi | `hasRecs` / `noRecs` (người mới thì danh sách rỗng) · dòng đang phát (nền panel, waveform màu text, meta thêm " · Đang phát") | `App:314-316`, `App:347` |
| Học DJ | `seg` ∈ guide, terms, tips; bài học: done (i<2) / current (i==2) / todo | `App:319`, `App:351` |
| Cài đặt | `theme` ∈ dark, light, system (system đọc `prefers-color-scheme`); 3 switch `sw.precue=false, haptic=true, awake=true` | `App:269`, `App:283` |
| Ngôn ngữ | `lang` (mặc định 'vi'), radio chọn | `App:298` |
| Sheet | `sheet` = null / {kind:'song', x} / {kind:'rec', n, d, date} | `App:322-338` |
| Toast | `toast` (chuỗi) + `toastColor`; tự tắt sau 2200 ms; mở toast thì đóng sheet | `App:279` |
| Mini player | hiện khi `playing != null`; thanh tiến độ cố định 40%, thời gian cố định "01:17 / {dur}" | `App:229-232` |

### Mixer — mỗi deck (`init` `Mixer:381`)
Mặc định deck: `{track:null, playing:false, sync:false, mode:'jog', tab:'cue', scratch:true, touch:false}`.

| Trạng thái | Hiển thị |
|---|---|
| **Trống** (`track=null`) | tiêu đề "Chưa có bài" (muted), phụ đề "Chạm đĩa để chọn", BPM "—", thời gian "−−:−−"; jog viền **dashed** line, bên trong icon + "Chạm để chọn bài"; làn waveform hiện "Deck X chưa có bài"; transport opacity .35 và không phản hồi |
| **Đã nạp, dừng** | jog viền solid màu deck, 36 chấm màu line, đĩa đứng yên; Play nền raised + icon play |
| **Đang phát** | chấm màu deck, đĩa quay, waveform cuộn, VU nhảy; Play nền màu deck, icon pause màu booth |
| **Đang chạm jog** (`touch`) | box-shadow vòng 4dp màu deck 33%; nếu `scratch` thì đĩa **dừng quay** (người dùng đang giữ) |
| **Sync bật** | nút Sync viền + chữ + LED màu deck, nền deck 12%; BPM đổi màu deck; pitch tự tính (mục 5) |
| **Scratch bật/tắt** | bật: viền, chữ, LED màu deck, nền trong suốt; tắt: nền raised, chữ muted, LED line |
| **Mode Pad** + tab cue/loop/fx/sampler | xem mục 5.2 `pads()` |
| **Loop đang chạy** | vùng loop trên waveform (chỉ Deck A, khi `loopA && playing`) |

Trạng thái toàn mixer: `xf` 0..1 · `rec` / `recSec` · `lib` + `libTarget` · `fx` (sheet FX) + `fxX`, `fxY` · `save` + `fmt` ('mp3'/'wav') · `coach` (todo khi B chưa sync, done khi B đã sync).

Biến thể demo (`variant`) → trạng thái khởi tạo `Mixer:382-388`:
- **play** (mặc định): A phát, B phát + sync, xf .36, loopA.
- **pad:** A pad/cue, đang ghi, recSec 222, xf .3.
- **library:** A phát, B trống, panel thư viện mở cho B, xf .2.
- **fx:** A phát ở pad/fx, sheet FX mở.
- **save:** A và B phát, B sync, dialog lưu mở, recSec 222.
- **coach:** A phát, B đã nạp, xf .2, coach mở.

### Trạng thái chỉ có ở canvas (`#1d` `MixDeck:506-731`)
- **Waveform:** "Đang phân tích" (vạch nằm ngang + shimmer 1.4s + "Đang phân tích nhịp…") · Bình thường · Có vùng loop 4 beat · Có điểm cue (vạch 2dp màu text + cờ số "2" 18×14).
- **Jog:** Trống, Đang dừng, Đang phát, Đang chạm (ring 5px màu deck với alpha 0x55; ở Mixer là 4px 33%).
- **Knob:** Mặc định · Đang kéo (viền thân màu deck + bong bóng giá trị "Mid +3.5 dB") · Kill (arc, kim, viền đỏ; nhãn "Kill" nền rec).
- **Fader:** Mặc định · Đang kéo (viền, vạch màu deck, bong bóng "82%") · Pitch có nấc giữa.
- **Pad:** Tắt · Bật · Đang giữ (nền đặc màu deck, chữ booth) · Sampler bật (màu trung tính text 12%).
- **Toggle:** Sync tắt/bật A/bật B · Loop bật · FX bật · Scratch.
- **VU:** Bình thường · Quá tải (100%, vùng đỏ trên 88%) + hộp cảnh báo "Âm lượng đang quá tải. Hạ fader hoặc âm lượng tổng." (viền rec).
- **Badge:** BPM thường · BPM "…" · BPM đã sync (nền a, "124.0") · "Mới" · "Hợp nhịp Deck A" (chấm 5 + chữ màu a).
- **Lỗi:** "Không đọc được tệp này. Hãy chọn tệp MP3, M4A, WAV hoặc FLAC." · "Không phát được âm thanh. Thử tăng độ trễ âm thanh trong Cài đặt." + nút "Mở Cài đặt".

**Error/loading không có trong thiết kế:** loading khi quét thư viện, trạng thái bị từ chối quyền (permission denied), thư viện trống (máy không có nhạc), lỗi khi lưu bản ghi.

---

## 3. Luồng điều hướng

```
Splash ─► Onboarding 1/3 ─Tiếp─► 2/3 ─Tiếp─► 3/3 ─Tiếp─► Xin quyền (2h)
            └─ "Bỏ qua" (trang 1, 2) ─► (không rõ đích; đề xuất: Xin quyền)
Xin quyền: "Cho phép truy cập nhạc" ─► dialog quyền hệ thống ─► Home (người mới)
           "Dùng bài mẫu trước"    ─► Home (người mới)          [đích chưa rõ, mục 8]

Home ─"Vào bàn DJ"────────────► Mixer
     ─"Mix với bài mẫu"──────► Mixer (nạp 2 bài mẫu A 124 / B 126 BPM)
     ─card "Tiếp tục phiên trước"► Mixer (khôi phục phiên: A "Sài Gòn lên đèn", B "Mưa sao băng")
     ─ô Học DJ (Hướng dẫn/Thuật ngữ/Mẹo) ► tab Học DJ, set seg tương ứng   App:344
     ─"Xem tất cả" (Học DJ) ► tab learn; "Xem tất cả" (Bản mix) ► tab rec   App:345
     ─nút play bản mix gần đây ► mini player (toggle)
     ─icon Cài đặt ► Settings (lưu prev = tab hiện tại)                 App:345
Thư viện ─chạm bài► Sheet bài hát ─Nạp A/B► toast "Đã nạp “…” vào Deck A/B" (ở lại Thư viện)
                                  ─Nghe thử► đóng sheet + mini player
         ─"Sắp xếp"► vòng Tên → Nghệ sĩ → BPM → Mới thêm
Bản ghi  ─chạm dòng► toggle mini player; ⋮ ► Sheet bản ghi (Đổi tên / Chia sẻ / Xoá)
         ─trống: "Vào bàn DJ" ► Mixer
Học DJ   ─bài học► Mixer (đề xuất mở coach mark của bài đó)  ·  "Xem trên mixer" ► Mixer
Settings ─Ngôn ngữ► Lang ;  Back ► prev tab                             App:290
Lang     ─chọn► set lang + toast "Đã chọn {native}" (ở lại màn Lang) ;  Back ► Settings
Mixer    ─Back► quay về App (trong prototype không có handler)
         ─chạm jog deck trống► panel thư viện (libTarget = deck)        Mixer:452
         ─panel: nút A/B► nạp bài, đóng panel ; X/scrim ► đóng
         ─nhấn giữ pad Echo► Sheet FX (canvas #2a; code không có handler)
         ─REC (đang ghi) ► dừng ► dialog Lưu (canvas #2b "Hộp thoại lưu sau khi dừng ghi")
         ─coach: chạm Sync Deck B ► bước "Hai bài đã chung nhịp"; "Bỏ qua" ► đóng
```

**Back behavior** (`App:290`): ở lang → settings; ở settings → `prev` (tab trước đó, mặc định home). Ở 4 tab chính không có nút Back. Đề xuất cho Android: Back hệ thống ở tab khác Home → về Home; ở Home → thoát. Sheet/toast đóng bằng Back hoặc chạm scrim. Chuyển tab thì đóng sheet (`go()` `App:303`).

**Tham số truyền sang Mixer** (prototype chỉ dùng `href="Mixer.dc.html"`, không có tham số; đây là đề xuất): `mode` ∈ {empty, samples, resume, lesson(n), glossary(term)}; deck đích khi nạp từ Thư viện (khi Mixer đang mở thì phát kết quả nạp, còn không thì lưu cho phiên sau).

**Hướng màn hình:** App dọc, Mixer ngang. Đề xuất: Mixer là Activity riêng `screenOrientation="sensorLandscape"` + immersive, hoặc một Fragment đặt `requestedOrientation`. Lead quyết định.

---

## 4. Design tokens

### 4.1 Màu (CSS variable → hex) `App:272-273`, `Mixer:392-393`
| Token | Dark (mặc định) | Light | Dùng cho |
|---|---|---|---|
| `--booth` | #1C1726 | #EEEAF4 | nền màn hình, chữ trên nút primary |
| `--panel` | #282133 | #FBFAFD | card, nav, sheet, deck panel |
| `--raised` | #362D44 | #E2DCEB | mục đang chọn, núm, toast, nút phụ |
| `--line` | #3E3550 | #D2CADD | đường kẻ, track, viền |
| `--line2` | #4B4160 | #B9AECA | viền núm, fader, knob, switch tắt |
| `--text` | #F4EFF9 | #1F1829 | chữ chính, nền nút primary |
| `--muted` | #A89EB8 | #5E5475 | chữ phụ, icon phụ |
| `--a` | #FFB547 | #A35F00 | Deck A |
| `--b` | #45D4C4 | #08786D | Deck B |
| `--rec` | #FF4F61 | #C8243A | ghi âm, kill, xoá, lỗi |
| `--grid` | #2A2335 | #E6E0EE | lưới XY pad |
| `--dim` | rgba(40,33,51,.5) | rgba(251,250,253,.55) | che phần waveform đã phát |
| `--inv-muted` | #4A4158 | #C4BAD3 | chữ phụ trong bong bóng coach |
| `--inv-dim` | #C9C1D4 | #5E5475 | chấm bước chưa tới trên coach |

Màu **cố định** (không đổi theo theme): vân đĩa `#1E1828`/`#272031`, nền jog `#211B2B`, nhãn đĩa A `#3A2C1E` / B `#1B3432`, viền và chữ nhãn đĩa `#FFB547`/`#45D4C4` (`Mixer:102-110`, `Mixer:368-369`, `Mixer:449`). Scrim: sheet App rgba(20,16,25,.66); FX .6, dialog lưu .7, panel thư viện .55 (rgba 28,23,38); coach spotlight rgba(20,16,25,.78). Màu cover bài: xem `colors_mixdeck.xml`. Nền canvas `#141019` và viền `#2E2639` chỉ thuộc tài liệu, không port.
`color-mix(... N%, transparent)` = màu với alpha N%: 12% (nền Sync bật, nền pad sampler), 18% (nền pad bật), 20% (vùng loop), 33% (vòng chạm jog), 55% (track crossfader), 60% (crosshair XY). Đã xuất sẵn vào `design/assets/values*/colors_mixdeck.xml`.

**Theme:** App có 3 lựa chọn: dark (mặc định), light, system. Mixer chỉ nhận dark/light (`Mixer:362`). Đề xuất: DayNight, `values/` = light, `values-night/` = dark, và `AppCompatDelegate.setDefaultNightMode` theo lựa chọn trong Cài đặt (mặc định MODE_NIGHT_YES).

### 4.2 Typography
Font (Google Fonts `App:12`): **Be Vietnam Pro** 400/500/600 (UI; fallback 'Noto Sans', system-ui) và **Chakra Petch** 500/600 (tiêu đề, số, BPM, thời gian, chữ cái deck). Cả hai là **font custom**. Đề xuất đóng gói file font tĩnh vào `res/font/` (OFL); downloadable fonts cũng được. Lead quyết định.

| Vai trò | Font | Size/LH (sp) | Weight | Khác | Nguồn |
|---|---|---|---|---|---|
| Splash wordmark | Chakra | 32/36 | 600 | letter-spacing .02em | `MixDeck:116` |
| Tiêu đề màn / onboarding | Chakra | 24/30 | 600 | | `App:24`, `MixDeck:132` |
| Heading khối, tiêu đề sheet/dialog | Be Vietnam | 18/24 | 600 | | `App:36`, `Mixer:269` |
| Tiêu đề mục (glossary, tip, sheet) | Be Vietnam | 16/22 | 600 | | `App:171` |
| Tên ngôn ngữ | Be Vietnam | 16/22 | 500 | | `App:219` |
| Body / tên item | Be Vietnam | 14/20 | 400 (500 cho tên bản ghi, bài học) | | `App:115` |
| Nút | Be Vietnam | 14 | 600 | | `App:37` |
| Caption, phụ đề, label nav | Be Vietnam | 12/16 | 400–600 | | `App:115`, `App:239` |
| BPM lớn (deck) | Chakra | 22/22 | 600 | tabular | `Mixer:83` |
| BPM badge | Chakra | 13 | 600 | tabular | `App:116` |
| Thời gian, pitch, REC, readout | Chakra | 14 (LH 14 hoặc 18) | 500 | tabular; REC letter-spacing .02em | `Mixer:32`, `Mixer:84` |
| Chữ cái deck/badge | Chakra | 14/24 (sheet 15/28) | 600 | | `App:46`, `App:254` |
| Số pad | Chakra | 18/18 (In/Out 14) | 600 | | `Mixer:428-429` |
| Chữ tắt nhãn đĩa | Chakra | 14/1 | 600 | letter-spacing .04em | `Mixer:110` |

Cỡ chữ nhỏ nhất là 12. Số dùng `font-variant-numeric: tabular-nums` → Android `fontFeatureSettings="tnum"`.

### 4.3 Spacing, bo góc, kích thước
- **Spacing** hay dùng: 2, 3, 4, 6, 8, 10, 12, 14, 16, 20, 24 (gap giữa các section), 28, 32. Padding ngang màn hình 16; card 16/20.
- **Bo góc:** 4 (tag), 5 (núm pitch/fader), 6 (badge, ô deck), 7 (badge sheet), 8 (cover, núm crossfader), 10 (pad, segment con), 12 (nút, ô input, card nhỏ, deck panel), 13 (switch), 15/16 (pill nav, card lớn, chip), 20 (dialog), 24 (sheet, cạnh trên), 50% (tròn).
- **Touch target tối thiểu 44** (mọi icon button 44×44).
- **Kích thước chính:** top bar 60 · bottom nav 72 (pill 56×30) · mini player 64 · nút primary 52 / outline 48 / dialog 44 · ô tìm 44 (panel thư viện Mixer 40) · chip 32 (Mixer 28) · dòng list 60 / 56 / 72 / 52 · switch 44×26 · radio 22 · handle sheet 36×4 · toast cao 52, cách đáy 88, cách lề 16.
- **Mixer:** top bar 72 · deck panel 280 · jog 152 (nhãn giữa 60, chấm 4 ở bán kính 67, vạch mốc 3×18 cách đỉnh 14) · knob 44 (dialog FX 52) · fader 22×(194 hành trình, núm 30) · VU rộng 8 · crossfader: núm 36×28, track 4 · pad lưới 4×2.
- **Elevation/shadow:** thiết kế **phẳng**, không có drop shadow. Phân lớp bằng màu nền (booth < panel < raised), viền 1px line và scrim. Chỉ có box-shadow dạng vòng: vòng chạm jog, núm XY `0 0 0 2px a`, spotlight coach `0 0 0 2px text, 0 0 0 2000px scrim`. Trên Android: elevation 0 và dùng stroke.
- **Gradient:** vân đĩa `repeating-radial-gradient(#1E1828 0 2px, #272031 2px 3px)` (chu kỳ 3dp); track crossfader nửa a 55% / nửa b 55%; VU `linear-gradient(to top, color 0–88%, rec 88–100%)` + vạch ngăn 5dp/2dp màu panel; XY pad lưới 25%. Tất cả cần vẽ bằng Canvas (không có XML gradient lặp).
- **Opacity:** transport khi deck trống .35; overview phần đã phát .5; phụ đề pad .85; blink .25.

---

## 5. Logic (renderVals) và dữ liệu mẫu — cơ sở port sang Kotlin

### 5.1 App.dc.html
**Props** (`App:267`): `theme` 'dark'|'light'|'system' = 'dark' · `user` 'returning'|'new' = 'returning' · `showAd` boolean = true · `startTab` home|lib|rec|learn|settings|lang = 'home'.

**State khởi tạo** `App:269`:
```
{ tab: startTab||'home', seg:'guide', libTab:0, sort:2, sheet:null, playing:null, toast:null,
  theme: props.theme||'dark', lang:'vi', prev:'home', sw:{precue:false, haptic:true, awake:true} }
```
Lưu ý: `sort` mặc định = 2 → **mặc định sắp xếp theo BPM**.

**Hàm:**
- `mini(seed)` `App:278`: mini waveform 16 cột, viewBox 56×28, sinh số giả ngẫu nhiên kiểu Park–Miller:
  ```
  s = seed; r() { s = (s*16807) % 2147483647; return s/2147483647 }
  for i in 0..15: h = 3 + r()*9*(0.6 + 0.4*sin(i/2.5))
      rect x = 4 + i*3, y = 14 - h, w = 2, height = 2h        // toFixed(1)
  ```
  seed của dòng thứ i = `i*13 + 5`. Kotlin: dùng `Long` cho `s*16807`.
- `flash(msg, color)` `App:279`: huỷ timer cũ; `toast = msg`, `toastColor = color ?: var(--b)`, `sheet = null`; sau **2200 ms** thì `toast = null`.
- `ini(t)` `App:307`: `t.split(' ').take(2).map{ it[0] }.join().uppercase()` (ví dụ "Tầng thượng 102" → "TT").
- **Sắp xếp** `App:308-312`: nhãn `['Tên','Nghệ sĩ','BPM','Mới thêm']`. `key`: tên (t), nghệ sĩ (a), `b ?: 999` (bài chưa có BPM xuống cuối), còn "Mới thêm" giữ thứ tự gốc. Chuỗi so sánh bằng `localeCompare(…, 'vi')` → `Collator.getInstance(Locale("vi"))`. `cycleSort`: `sort = (sort+1) % 4`.
- **Phát** `play(n, d)` `App:315`: bài đang phát trùng tên thì `playing = null`, khác thì `playing = {n, d}` (chỉ một bản phát tại một thời điểm).
- **Sheet bài** `App:324-330`: meta = `"${a} · ${d} · ${b ? b+' BPM' : 'Đang phân tích nhịp…'}"`. Action: "Nạp vào Deck A" → `flash('Đã nạp “${t}” vào Deck A', --a)`; B tương tự với --b; "Nghe thử" → `sheet = null, playing = {n: t, d}`.
- **Sheet bản ghi** `App:331-337`: title = n, meta = `"${d} · ${date}"`, chữ cover "MIX". Đổi tên / Chia sẻ → chỉ đóng sheet (**chưa định nghĩa**). Xoá → `flash('Đã xoá bản mix', --rec)` (**không xoá dữ liệu**, không có dialog xác nhận).
- **Lesson** `App:318-319`: `steps = [4,3,4,5,4,5,3]`; done `i<2`, current `i==2`. Tiến độ "2/7" **cố định**.
- `settingsVals` `App:281-299`: `resolved` = system ? (`prefers-color-scheme: light` ? light : dark) : theme; `applyTheme` đặt CSS var trên root. Hàm `sw(k)` toggle `sw[k]`. `val()` là dòng giá trị (onClick rỗng, trừ Ngôn ngữ → `tab='lang'`). Chọn ngôn ngữ → `lang = code` + `flash('Đã chọn ' + native)`.
- `openSettings` = `{prev: tab, tab:'settings', sheet:null}`. `goBack` = lang → settings, còn lại → `prev || 'home'`.

**Dữ liệu mẫu:**
- Songs `App:309` `[t, a, d, b(bpm|null), c(cover)]`, 10 bài:
  Tết về rồi / Nhóm Mùa Xuân / 03:15 / 100 / #3B5E4A · Vòng quay / Lam Anh / 03:58 / 118 / #4A4A5E · Chạy về phía biển / Vy Hạ / 03:48 / 122 / #4A3B5E · Sài Gòn lên đèn / Mây Tầng Thượng / 04:12 / 124 / #5E4A3B · Không cần lời / DJ Tâm / 05:02 / 125 / #3B4A5E · Tầng thượng 102 / Cáo Nhỏ / 04:40 / 126 / #5E3B4A · Mưa sao băng (Club mix) / Đèn Neon / 04:26 / 128 / #2E4E4A · Phố đêm / Hạ Vũ / 03:31 / **null** / #4E452E · Ánh đèn sân khấu / Bảo Trâm / 03:44 / 130 / #5E3B3B · Đi đâu cũng được / Mèo Mun / 04:05 / 120 / #3B3B5E.
  Model đề xuất: `Track(id, title, artist, durationMs, bpm: Float?, coverColor)`.
- Recordings `App:314` `[name, dur, date]`: Mix 25-09 21:40 · 03:12 · 25/09/2026; Mix 24-09 22:05 · 05:47 · 24/09/2026; Tiệc sinh nhật Linh · 12:30 · 20/09/2026; Mix 18-09 23:10 · 02:05 · 18/09/2026. Người mới → `[]`. "Bản mix gần đây" = 2 bản đầu. Tên mặc định có dạng `"Mix dd-MM HH:mm"`.
- Samples `App:342`: A "Mẫu 1 · Nhịp nhà" 124; B "Mẫu 2 · Đêm hội" 126.
- Session `App:343`: A "Sài Gòn lên đèn" 124; B "Mưa sao băng (Club mix)" 128.
- Learn tiles `App:344`: Hướng dẫn / Thuật ngữ / Mẹo → `{tab:'learn', seg}`.
- Lessons: Nạp bài và phát (4 bước), Dùng Sync (3), Chuyển bài bằng crossfader (4), Dùng EQ (5), Tạo loop (4), Thêm FX (5), Ghi âm (3).
- Glossary `App:320`: 10 mục (Deck, Crossfader, BPM, Cue, Hot cue, Loop, Pitch, EQ, Sync, Beatmatch), lấy nguyên văn từ file.
- Tips `App:321`: 4 mục `[tag, read, title, body]` (Chọn nhạc, EQ, Chuyển bài, FX), nguyên văn.
- Lib tabs: Bài hát, Playlist, Album, Nghệ sĩ, Thư mục.
- Settings groups / LANGS: xem 1.6 và 1.7.

### 5.2 Mixer.dc.html
**Props** `Mixer:362`: `theme` dark|light; `variant` play|pad|library|fx|save|coach.

**Dữ liệu:** `TR` `Mixer:367-370`:
- `a`: Sài Gòn lên đèn / Mây Tầng Thượng / bpm 124 / time '−02:31' / ini 'SG' / pos 41 (%) / labelBg #3A2C1E
- `b`: Mưa sao băng (Club mix) / Đèn Neon / 128 / '−03:10' / 'MS' / 14 / #1B3432

`LIB` `Mixer:371-378`: 6 bài (Chạy về phía biển 122; Không cần lời 125; Tầng thượng 102 126; Mưa sao băng 128; Phố đêm null; Tết về rồi 100) kèm `dur` và `cover`. **Lưu ý:** màu cover của "Tết về rồi" ở đây là #5E4A3B, còn ở App là #3B5E4A.

**Công thức (nguyên văn, kèm diễn giải):**
- **Pitch khi Sync** `Mixer:437`: `pitch = (t && s.sync && otherT) ? (otherT.bpm / t.bpm - 1) * 100 : 0` (%). Dùng BPM **gốc** của deck kia, không dùng BPM hiệu dụng. Ngoài Sync thì prototype không có cách chỉnh pitch (fader pitch chỉ để hiển thị).
- **BPM hiệu dụng:** `bpm = t.bpm * (1 + pitch/100)`, hiển thị `toFixed(1)` (ví dụ "124.0"). Deck trống thì "—".
- **Nhãn pitch** `Mixer:441,447`: `sign = pitch > 0.05 ? '+' : pitch < -0.05 ? '−'(U+2212) : '+'`, label = `sign + abs(pitch).toFixed(1) + '%'`. Chữ màu text khi `|pitch| > 0.05`, còn lại muted; vạch trên núm màu deck khi lệch.
- **Vị trí núm pitch:** `pitchPos = 50 - pitch/8*50` (%) → phạm vi **±8%** (trùng với Cài đặt "Phạm vi Pitch mặc định ±8%"). Pitch dương thì núm đi **lên**. top = `pitchPos% - 8dp`. Ví dụ B sync theo A: (124/128−1)·100 = −3.125 → "−3.1%", núm ở 69.5%.
- **Tốc độ quay jog:** `spinDur = (1.8 / (1 + pitch/100)).toFixed(3)` giây/vòng (≈ 33⅓ rpm). Quay khi `playing && !(touch && scratch)`. 36 chấm cách nhau 10°, màu deck khi đang phát, ngược lại màu line.
- **Waveform** `gen(seed)` `Mixer:399-409` (seed A = 7, B = 29):
  ```
  for i in 0..303: k = i % 8; env = 0.55 + 0.35*sin(i/304*PI*6 + seed)
     base = k==0 ? 1 : k==1 ? 0.8 : 0.28 + 0.5*r()        // r() CHỈ gọi khi k>=2
     h = clamp(1, 8, base*env*9)
  2 lần lặp (c = 0, 1): rect x = c*1216 + i*4, y = 9 - h, w = 3, height = 2h   // làn cao 18
  ```
  Mỗi bar cách 4dp → 304 bar = 1216dp, nhân đôi để cuộn liền mạch.
  `beatPath` `Mixer:410`: vạch dọc 1dp màu line tại `x = 0, 32, 64, …` (< 2432) → **32dp = 1 beat**.
- **Tốc độ cuộn waveform** `Mixer:468`: `dur = 1216 / (32 * bpm / 60)` giây cho 1216dp → vận tốc = `32*bpm/60` dp/s (ví dụ 124 BPM ≈ 66.1 dp/s). Deck không phát thì dừng. Deck chưa nạp thì dur = 10.
- **Vùng loop** `Mixer:48`: left = giữa − 48, rộng 128 (= **4 beat**); vạch phát nằm cách mép trái vùng loop 48dp (1.5 beat). Nền a 20%, viền trái/phải 1.5 màu a. Chỉ hiện khi `loopA && A.playing`.
- **Overview** `Mixer:475`: `pct = t.pos` (%). Phần đã phát tô màu deck với opacity .5, con trỏ 2dp.
- **Knob** `P(), arc(), knob()` `Mixer:411-420`, viewBox 44:
  ```
  P(a, r=18, c=22) = (c + r*sin(a°), c - r*cos(a°))      // 0° = đỉnh, chiều kim đồng hồ
  track: arc từ -135° đến +135° (large-arc=1, sweep=1), stroke 3 màu line, bo tròn
  value arc: a1 = (v-0.5)*270; nếu |a1| < 2° thì không vẽ; vẽ từ min(0,a1) tới max(0,a1)  // arc hai phía, tính từ tâm 12h
  kim: line (22,11)→(22,17), stroke 2.5, rotate(deg = (v-0.5)*270) quanh tâm
  thân: circle r13 nền raised, viền 1.2 (line2 | rec khi kill); chấm mốc (22,1.2) r1.2 màu muted
  kill: v = 0, arc/kim/viền màu rec; aria "EQ {label} Deck {X}, {kill | round(v*100)%}"
  ```
  Giá trị demo `Mixer:469`: A `[0.5, 0.56, 0.42, 0.5]`, B `[0.6, 0.5, 'kill', 0.64]` (High, Mid, Low, Filter).
- **Fader âm lượng** `Mixer:478-479`: `top = round((1 - vol) * 164)` dp (hành trình 164 = cao cột 194 − cao núm 30). Demo volA .86, volB .72.
- **VU** `Mixer:16,195-198`: nền line; màu kênh ở 0–88%, rec ở 88–100%; lớp che từ trên xuống có `scaleY = s` → mức = `1 − s`. Keyframes: 0% .48 → 18% .2 → 36% .4 → 55% .16 → 74% .52 → 100% .48 (tức mức 52 → 80 → 60 → 84 → 48 → 52%). A `.9s ease-in-out infinite`, B `1.07s ease-in-out -.3s`. Khi không phát: scaleY .98 (mức 2%). Vạch ngăn 5dp/2dp. Đây là **giả lập**; bản thật phải đo RMS/peak.
- **Crossfader** `Mixer:464,481-483`: `xf ∈ [0,1]` (0 = A, 1 = B). Kéo: `xf = clamp((x - left - 18) / (width - 36), 0, 1)`. Núm: `left = xf*100% - xf*36dp`. Chạm đúp → 0.5. Pointer capture khi kéo. aria "Crossfader, {round(xf*100)}% về phía B". **Chưa có đường cong trộn âm thanh (curve)**.
- **REC** `Mixer:397,484-485`: `setInterval` 1000 ms, `recSec++` khi đang ghi. `toggleRec`: `rec = !rec; recSec = rec_cũ ? 0 : recSec` (dừng thì reset về 0). Nhãn `mm(n) = pad2(n/60) + ':' + pad2(n%60)`, không ghi thì "REC". Chấm nhấp nháy `md-blink 1s steps(1)`.
- **Pads** `pads(tab, color)` `Mixer:422-433`:
  - on = nền deck 18%, viền deck, chữ deck; off = nền raised, viền raised, chữ muted; lock = nền panel, viền line **dashed**.
  - `cue`: số 1..8; pad 1–3 bật, phụ đề `'0:32','1:04','2:08'`; pad 4–8 tắt, phụ đề "Trống".
  - `loop`: `'1/4','1/2','1','2','4','8','16','In/Out'`; phụ đề "beat" (trừ In/Out); pad "4" bật; cỡ chữ 18, In/Out 14.
  - `fx`: Echo, Flanger, Bitcrush, Gate, Reverb, Phaser, Roll, Brake (Echo bật; font UI 12).
  - `sampler`: Còi hơi, Scratch, Drop, Vỗ tay, Siren, Đám đông, Laser, Rewind ("Vỗ tay" bật với màu trung tính text 12%).
  - onClick của mọi pad đều rỗng.
- **Deck handlers** `Mixer:451-457`: jog down → `touch = true` (nếu đã nạp) · up/leave → `touch = false` · click khi trống → `{lib: true, libTarget: id}` · Scratch toggle · Pad/Đĩa đổi mode · Play/Sync toggle (chỉ khi đã nạp) · tab pad.
- **Panel thư viện** `Mixer:496-500`: bài **hợp nhịp** khi `A đã nạp && r.bpm && |r.bpm / A.bpm_hiệu_dụng − 1| ≤ 0.03 && libTarget == 'B'` → badge nền a, chữ booth, thêm dòng "Hợp nhịp A". Nạp: `{title, artist, bpm: r.bpm || 120, time: '−' + dur, ini, pos: 0, labelBg theo deck}` → deck `{track, playing: false}` và đóng panel. (Prototype dùng chung key `TR['lib']` cho cả hai deck nên nạp lần hai sẽ ghi đè bài của deck kia. Bản thật phải lưu track theo từng deck.) Tab và sort trong panel là tĩnh ("Sắp xếp: BPM").
- **FX XY pad** `Mixer:463,486-488`: `fxX = clamp((x - left)/w)`, `fxY = clamp((y - top)/h)` (mặc định .62/.3; fallback .6/.3). Readout: `"${['1/16','1/8','1/4','1/2','3/4','1'][min(5, floor(fxX*6))]} beat · ${round((1 - fxY)*100)}%"`. X = thời gian, Y hướng lên = độ mạnh. Vùng tô: rộng fxX%, cao (1−fxY)% tính từ đáy trái. Wet/Dry cố định 60% (`arc(0.6)`, kim 27°). "Độ dài theo beat" `['1/4','1/2','1','2','4']`, chọn cố định index 2, không bấm được. Switch Echo và nút "Đặt lại" tĩnh.
- **Save** `Mixer:490-492`: `saveDur = mm(recSec)`; `fmt` mp3/wav; khi wav thì hiện ghi chú "WAV không nén, tệp lớn hơn MP3 khoảng 4 lần.". "Bỏ" và "Lưu bản mix" đều chỉ đóng dialog. Tên mặc định "Mix 25-09 21:40" (ô nhập giả, có con trỏ). Chất lượng "320 kbps" dạng dropdown tĩnh.
- **Coach** `Mixer:297-317,493`: spotlight (516, 286, 90×68, bo 16) = vị trí nút Sync Deck B; bong bóng 256 tại (296, 92), mũi tên ở left 218 phía dưới. Chỉ báo bước 3/5: 5 vạch 16×4, 3 vạch màu booth, 2 vạch màu inv-dim. todo: "Chạm Sync ở Deck B" / "Sync chỉnh tốc độ Deck B cho khớp nhịp Deck A đang phát." → khi `b.sync` thì done: "Hai bài đã chung nhịp" / "BPM Deck B giờ bằng Deck A. Tiếp theo: bấm Play ở Deck B.". Spotlight `pointer-events: none` nên chạm xuyên được tới Sync. "Bỏ qua" đóng coach; "Tiếp" chưa có handler.

### 5.3 MixDeck.dc.html (canvas)
Các hàm `P`, `arc`, `mini` giống hệt App/Mixer. `wave()` `MixDeck:740-744` là waveform tĩnh 75 cột cao 44 (dùng cho onboarding 2/3 và bảng component): `s = 11; h = clamp(2, 18, base*(0.6 + 0.3*sin(i/9))*20)` với base giống `gen`; rect `x = i*4, y = 22 - h, w = 3`. beatPath: `x = 6, 38, …` (<300), cao 44. Còn lại là dữ liệu cho các artboard tĩnh (jogs, knobs, faders, xfs, padStates, waves, toggles, vus) như đã nêu ở mục 2. `vus` normal: h 64, split 138 (toàn màu a); overload: h 100, split 88.

### 5.4 Validation / giới hạn
Không có validation form nào (tên bản mix không có ràng buộc). Các giới hạn duy nhất: clamp [0,1] cho xf/fxX/fxY; pitch ±8%; "hợp nhịp" ≤ 3%; hot cue tối đa 8 (glossary); định dạng tệp hỗ trợ MP3, M4A, WAV, FLAC (banner lỗi); ghi âm MP3 320 kbps hoặc WAV.

---

## 6. Tương tác và animation

| Control | Cử chỉ | Phản hồi | Nguồn |
|---|---|---|---|
| Jog wheel | chạm giữ + xoay → scratch hoặc chỉnh lệch nhịp (nudge); chạm khi trống → mở thư viện | vòng sáng màu deck; đĩa dừng khi scratch | `Mixer:102`, `MixDeck:511` |
| Knob EQ/Filter | **kéo lên/xuống**, **chạm đúp → về giữa**, **nhấn giữ → kill** | bong bóng giá trị khi kéo ("Mid +3.5 dB"), viền màu deck | `MixDeck:539` |
| Fader âm lượng / pitch | kéo dọc; pitch có nấc giữa | bong bóng "%" khi kéo | `MixDeck:562` |
| Crossfader | kéo ngang; **nấc giữa có rung** (haptic); chạm đúp → giữa | — | `MixDeck:579`, `Mixer:206` |
| Overview | `role=slider` (seek) | chưa định nghĩa hành vi | `Mixer:60` |
| Pad | chạm; **nhấn giữ pad Echo → sheet FX chi tiết** | trạng thái "Đang giữ" = nền đặc màu deck | `MixDeck:96`, `MixDeck:807` |
| XY pad | kéo 2 chiều | readout, crosshair | `Mixer:253` |
| Play/Sync/Scratch/REC | tap toggle | xem mục 2 | |
| Settings switch, segmented, radio, chip, sort | tap | | |
| Sheet / panel | tap scrim đóng; handle 36×4 (gợi ý kéo để đóng) | | |
| Hover (`style-hover`) | chỉ có trên web | → ripple / pressed state nền raised hoặc panel | |

"Rung phản hồi" (Cài đặt, bật mặc định) → dùng `performHapticFeedback` cho nấc giữa crossfader/pitch, kill, pad.
"Giữ màn hình luôn sáng" (bật mặc định) → `FLAG_KEEP_SCREEN_ON` ở Mixer.

**Animation** (CSS keyframes):
| Tên | Thông số | Dùng |
|---|---|---|
| `md-spin` | rotate 0→360°, `spinDur` s (1.8/(1+pitch/100)), linear, vô hạn; pause khi dừng | jog `Mixer:14,104` |
| `md-wave` | translateX 0→−1216dp, `1216/(32*bpm/60)` s, linear, vô hạn | waveform `Mixer:15,43` |
| `md-vu` | keyframe scaleY như 5.2; .9s / 1.07s (delay −.3s), ease-in-out | VU `Mixer:16` |
| `md-blink` | opacity 1 (0–49%) / .25 (50–100%), 1s steps(1) | chấm REC `Mixer:17` |
| `md-shimmer` | translateX −100%→100%, 1.4s linear | waveform đang phân tích `MixDeck:20,619` |
| Toast | hiện 2200 ms, không có animation vào/ra | `App:279` |
| reduced motion | `prefers-reduced-motion: reduce` → tắt animation của `[data-strobe]` (jog) | `Mixer:18` → Android: kiểm tra `Settings.Global.ANIMATOR_DURATION_SCALE == 0` |

Chuyển màn, mở sheet, dialog, coach: **không định nghĩa animation** → đề xuất dùng mặc định Material (BottomSheet slide, fade 150–250 ms).

---

## 7. Asset

Đã xuất vào `design/assets/`:
- `svg/*.svg` (28 file, `currentColor`) và `drawable/*.xml` (28 VectorDrawable 24dp; glyph trắng, tint bằng `app:tint`/`imageTintList`; circle và rect đã đổi sang path). Mỗi file có comment dẫn nguồn.
  `ic_back, ic_tune, ic_deck, ic_chevron_right, ic_chevron_down, ic_search, ic_play, ic_pause, ic_nav_home, ic_nav_library, ic_nav_recordings, ic_nav_learn, ic_learn_guide, ic_learn_terms, ic_learn_tips, ic_more_vert, ic_check, ic_arrow_forward, ic_headphones, ic_edit, ic_share, ic_delete, ic_close, ic_add, ic_jog_mode, ic_reset, ic_error` + `ic_logo_mixdeck` (64×40, màu cố định, không tint).
  Kích thước khi hiển thị: nav 22, nút top bar 22–24, list 16–20, sheet 20, empty state 40 (ic_nav_recordings / ic_nav_library).
- `values/colors_mixdeck.xml` (light + màu cố định + scrim + cover) và `values-night/colors_mixdeck.xml` (dark). Tên màu `md_*`.

Không xuất (cần vẽ bằng code hoặc thay thế):
- Thanh trạng thái giả (signal, pin) → bỏ, dùng system bar.
- Đĩa vinyl (vân, nhãn, chấm), waveform, mini waveform, knob, fader, VU, crossfader, XY pad → custom View (mục 8).
- Đĩa minh hoạ ở Home và onboarding 1/3 → dùng lại `VinylView` tĩnh (không quay).
- Có thể thay bằng Material Symbols nếu lead muốn: back = `arrow_back_ios_new`, tune = `tune`, search, play_arrow, pause, home, library_music, graphic_eq, school, menu_book, lightbulb, more_vert, check, arrow_forward, headphones, edit, share (glyph trong thiết kế là "upload"/ios_share), delete, close, add, album, restart_alt, error. **Khuyến nghị dùng file đã xuất** để giữ nét 2dp bo tròn đồng nhất.
- Không có ảnh bitmap. Cover bài là ô màu + chữ tắt ("Nhãn giữa đĩa đang là ô màu thay cho ảnh bìa bài hát" `MixDeck:483`); bản thật có thể dùng album art từ MediaStore, nếu không có thì fallback về ô màu.

---

## 8. Điểm mơ hồ, phần trang trí và rủi ro

### 8.1 Cần lead quyết định
1. **Phạm vi âm thanh:** prototype không có audio thật. Các chức năng time-stretch/pitch, Sync, EQ 3 band + Filter, 8 FX, sampler, hot cue, loop, scratch, ghi mix (MP3 320/WAV), pre-cue qua tai nghe tách kênh, đo VU đều cần audio engine native (Oboe/AAudio + DSP; ExoPlayer không đủ). Đề xuất: giai đoạn 1 chỉ làm UI + ViewModel giả lập (timer, animation) như prototype.
2. **Đích của "Bỏ qua" (onboarding) và "Dùng bài mẫu trước" (2h):** Home (người mới) hay Mixer với bài mẫu? Thiết kế cũng chưa có màn **bị từ chối quyền** hoặc "Không bao giờ hỏi lại".
3. **Dừng REC:** code reset `recSec` về 0 và không mở gì, trong khi canvas mô tả "Hộp thoại lưu sau khi dừng ghi" (#2b) và có thêm dialog "Dừng ghi và lưu bản mix?" (Lưu / Bỏ bản ghi / Tiếp tục ghi) cùng sheet "Ghi âm bản mix" trước khi ghi (switch micro). Luồng đề xuất: REC → sheet chuẩn bị (lần đầu?) → ghi → REC → dialog Lưu (2b) → toast "Đã lưu bản mix" [Nghe] [Chia sẻ]. Cần chốt.
4. **Menu bản ghi:** App dùng bottom sheet (O2), còn canvas cũ `#2l` dùng popup menu neo ở nút ⋮. Đề xuất theo App (bottom sheet). Đổi tên, Chia sẻ, Xoá chưa có UI chi tiết (dialog đổi tên? xác nhận xoá?).
5. **Cài đặt:** canvas cũ `#2q` có slider độ trễ + nút "Kiểm tra độ trễ" và dòng "Giao diện: Tối"; bản mới (App, #4e) có segmented giao diện ở đầu và độ trễ là dòng giá trị. Các dòng giá trị (độ trễ, âm lượng, định dạng, chất lượng, thư mục, pitch range, jog mode, quyền, hỗ trợ) **chưa có màn chọn** → đề xuất dùng dialog hoặc bottom sheet lựa chọn đơn.
6. **Nút "Cài đặt nhanh" trên Mixer** (icon tune) không có nội dung. Nút Back và nút Cue trên Mixer không có handler.
7. **Ánh xạ dB của knob** chưa được định nghĩa ("Mid +3.5 dB" ở giá trị .63). Filter là knob hai phía (LPF/HPF?) chưa mô tả.
8. **Độ dài FX:** hàng nút 1/4–4 beat mâu thuẫn với readout XY (1/16–1 beat). Wet/Dry là knob cố định. Mở sheet FX bằng nhấn giữ pad chỉ áp dụng cho Echo hay mọi FX?
9. **Ngôn ngữ:** 17 ngôn ngữ nhưng chỉ có chuỗi tiếng Việt. Có Arabic (RTL) → layout phải dùng start/end; Mixer có nên mirror không? (Deck A trái/B phải là quy ước DJ, đề xuất **không** mirror Mixer.) Chọn ngôn ngữ trong app → dùng per-app language `AppCompatDelegate.setApplicationLocales`.
10. **Quảng cáo:** chỉ là placeholder 320×50 trong card 72; SDK quảng cáo chưa chốt. Dù đã bỏ Premium, prop `showAd` vẫn mặc định true.
11. **Toast đè lên mini player:** toast cách đáy 88 (trên nav 72), mini player 64 nằm trên nav, nên khi cả hai cùng hiện thì toast đè lên player. Đề xuất neo Snackbar lên trên mini player.
12. **Thư viện:** chip chỉ đổi màu, không lọc; "248 bài" cố định; ô tìm tĩnh → cần định nghĩa màn Playlist/Album/Nghệ sĩ/Thư mục và kết quả tìm kiếm (thiết kế không có).
13. **Nạp bài từ Thư viện** khi Mixer chưa mở: chỉ hiện toast. Có tự mở Mixer không?
14. **Học DJ:** tiến độ 2/7 cố định; bài học mở Mixer, coach 5 bước, nhưng chỉ có nội dung bước 3 của bài "Dùng Sync". Cần kịch bản cho 7 bài.
15. **Light theme Mixer:** màu đĩa, nhãn, vân đĩa hardcode theo dark nên jog vẫn tối trên nền sáng (ảnh #4a). Giữ nguyên như thiết kế?
16. **Pitch fader:** hướng + là lên (ngược một số bàn DJ thật). Người dùng có kéo pitch tay được không, hay chỉ Sync?

### 8.2 Chỉ là trang trí trong prototype (không port như logic)
Thanh trạng thái giả; bo góc khung thiết bị; nền canvas và nhãn artboard; `hint-placeholder-*`; con trỏ giả trong ô nhập; `style-hover`; thời gian "01:17" và thanh tiến độ 40% của mini player; "248 bài"; "Đã học 2/7"; giá trị EQ/volume demo; `variant` (chỉ để trình diễn).

### 8.3 Rủi ro khi chuyển sang Android Views (custom View cần làm)
| Custom View | Nội dung | Ghi chú |
|---|---|---|
| `JogWheelView` | vân đĩa (vẽ vòng tròn đồng tâm chu kỳ 3dp), 36 chấm, nhãn giữa, vạch mốc, quay theo `spinDur`, vòng chạm, trạng thái trống dashed + "Chạm để chọn bài"; bắt cử chỉ xoay (góc giữa 2 lần move) cho scratch/nudge | dùng `ValueAnimator`/`Choreographer`; tôn trọng reduced motion |
| `KnobView` | arc track/giá trị theo công thức `P/arc`, kim, kill; kéo dọc, chạm đúp, nhấn giữ; bong bóng giá trị | `GestureDetector`; TalkBack: `AccessibilityNodeInfo.RangeInfo` + action tăng/giảm |
| `VerticalFaderView` | rãnh, nấc giữa (pitch), núm 22×30 hoặc 28×16, bong bóng | dùng cho volume và pitch |
| `CrossfaderView` | track gradient 2 màu, vạch giữa, núm 36×28; nấc giữa rung | công thức 5.2 |
| `ScrollingWaveformView` | bar 3/4dp, beat grid 32dp, cuộn theo BPM, vạch phát giữa, lớp dim nửa trái, vùng loop, cờ cue, shimmer khi phân tích | vẽ bằng Canvas + `Path` cache; 60fps; tránh cấp phát trong `onDraw` |
| `OverviewBarView` | thanh tiến độ + seek | |
| `VuMeterView` | gradient 88%, vạch ngăn, mức động | |
| `XYPadView` | lưới 25%, crosshair, vùng tô, núm 30 | |
| `MiniWaveformView` | 16 bar theo `mini(seed)` | dùng trong RecyclerView |
| `VinylArtView` | đĩa minh hoạ tĩnh ở Home/Onboarding | có thể gộp vào JogWheelView (mode tĩnh) |
| `PadGridView` hoặc 8 `MaterialButton` | trạng thái on/off/held/lock (dashed) | viền dashed dùng `GradientDrawable.setStroke(w, c, dash, gap)` |
| Coach overlay | scrim có lỗ bo 16 (`Path.FillType.EVEN_ODD` hoặc `PorterDuff.CLEAR`), bong bóng + mũi tên, cho chạm xuyên vào vùng spotlight | |

Rủi ro khác:
- **Layout ngang cố định 800×360:** máy thực tế có chiều cao landscape 360–430dp và thêm cutout. Cần immersive (ẩn system bars) và cho cột giữa co giãn (`0dp`/weight); deck 280 cố định. Máy hẹp hơn 800dp: cột giữa chỉ còn 224 → 208 nội dung, nên cột nhãn "Filter" còn khoảng 36dp, rất chật.
- **Đa chạm:** DJ cần chạm nhiều điểm cùng lúc (jog A + crossfader). Mỗi custom View phải xử lý `pointerId` riêng và `requestDisallowInterceptTouchEvent`.
- **Hai hướng màn hình** trong cùng app: nếu dùng một Activity + nav_graph thì việc đổi orientation sẽ tạo lại Activity → state phải nằm trong ViewModel/SavedState.
- **Line-height** chính xác: dùng `lineHeight` (API 28+ hoặc `TextViewCompat`) và `includeFontPadding=false` cho số Chakra Petch.
- **Tabular numbers** cho thời gian và BPM để chữ không nhảy: `fontFeatureSettings="tnum"` (cần kiểm tra Chakra Petch có hỗ trợ tnum; nếu không thì đặt minWidth).
- **Quyền:** READ_MEDIA_AUDIO (API 33+) hoặc READ_EXTERNAL_STORAGE (≤32); RECORD_AUDIO nếu bật "Thêm giọng của tôi qua micro"; ghi vào `Music/MixDeck` bằng MediaStore.

---

## 9. File đã tạo
- `D:\App_KT\DJRemixPro\design\DESIGNER_REPORT.md` (file này)
- `D:\App_KT\DJRemixPro\design\assets\svg\` — 28 SVG
- `D:\App_KT\DJRemixPro\design\assets\drawable\` — 28 VectorDrawable (`ic_*.xml`)
- `D:\App_KT\DJRemixPro\design\assets\values\colors_mixdeck.xml`, `...\values-night\colors_mixdeck.xml`

Không sửa `design/source/` và không đụng tới app/, Gradle, res/.
