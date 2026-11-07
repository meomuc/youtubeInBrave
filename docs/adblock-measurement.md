# Quy trình đo quảng cáo còn sót (ticket #8)

Mục đích: đo số quảng cáo còn xuất hiện khi xem video thật trong app, sau khi áp danh sách chặn (`AdBlocker.kt`). Ticket 09 dùng lại số liệu này để nghiệm thu.

Người đo: chủ tài khoản (cần đăng nhập Google trong app). Thiết bị: Xiaomi Pad 7 Pro (kết quả nghiệm thu), hoặc emulator (chỉ để thử quy trình, không ghi nghiệm thu).

## 1. Chuẩn bị

1. Build APK debug mới nhất (`./gradlew.bat :app:assembleDebug`) và cài: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Đăng nhập tài khoản trong app (ticket 04). Ghi lại phiên bản build: `git rev-parse --short HEAD`.
3. Tạo file kết quả `docs/adblock-results/<ngày>-<model>.csv` theo mẫu ở mục 5.
4. Chọn mẫu: **20 video**, lấy từ trang chủ và kết quả tìm kiếm, trộn nhiều kênh và nhiều độ dài (dưới 5 phút, 5–20 phút, trên 20 phút). Không chọn video đã xem trước đó, để tránh bị cá nhân hóa quảng cáo. Ghi tiêu đề và thời lượng vào CSV.
5. Mỗi lần đo, để video ở chế độ toàn màn hình. Không bỏ qua quảng cáo trước khi nó kết thúc tự nhiên; nếu có nút "Bỏ qua" sau N giây thì chờ đủ N giây rồi bấm và ghi là đã bỏ qua.

## 2. Thao tác cho từng video

Trước mỗi video, xóa log cũ:

```bash
adb logcat -c
```

1. Mở video từ trang chủ hoặc tìm kiếm. Bấm phát.
2. Xem đến hết video, hoặc tối đa 25 phút. Không tua qua quảng cáo.
3. Khi xong, lấy số request đã chặn và danh sách tên miền:

```bash
adb logcat -d -s AdBlock | grep "blocked host=" | wc -l
adb logcat -d -s AdBlock | grep "blocked host=" | sed 's/.*host=//' | sort | uniq -c
```

Dòng `--------- beginning of main` không phải dữ liệu, nên lọc bằng `grep "blocked host="`.

4. Ghi vào CSV: số quảng cáo còn sót (mục 3), số request đã chặn, thời lượng video đã xem.

## 3. Định nghĩa "quảng cáo còn sót"

Một lần quảng cáo còn sót là một lần xuất hiện có nhãn "Quảng cáo" hoặc "Ad" trên màn hình, gồm:

- **Trước video** (pre-roll): video quảng cáo chạy trước video chính.
- **Giữa video** (mid-roll): quảng cáo chèn giữa chừng.
- **Băng quảng cáo nổi** (overlay): biểu ngữ hoặc khung quảng cáo trên video.
- **Quảng cáo trên trang**: khung quảng cáo hiện trên trang chủ hoặc cạnh video, nếu có.

Mỗi lần chèn đếm là 1, kể cả khi bị bỏ qua. Hai quảng cáo liên tiếp không có video chính xen giữa đếm là 1 lần chèn.

Ghi thêm: quảng cáo có tiếng hay không (có tiếng nghĩa là quảng cáo video, không phải biểu ngữ tĩnh).

## 4. Tính số liệu nghiệm thu

- **Số video đã thử:** số dòng có dữ liệu trong CSV. Mục tiêu: 20.
- **Số video có quảng cáo còn sót:** số video có ít nhất 1 lần quảng cáo còn sót.
- **Tỉ lệ video có quảng cáo còn sót:** số video có quảng cáo / số video đã thử.
- **Tổng lần quảng cáo còn sót** và **trung bình trên mỗi video**.
- **Số request đã chặn trung bình trên mỗi video** (từ log `AdBlock`).

Tiêu chí đạt do ticket 09 quyết định. Quy trình này chỉ cung cấp số liệu.

## 5. Mẫu CSV

```csv
stt,ngay,model,build,tieu_de,thoi_luong,quang_cao_con_sot,loai_quang_cao,co_tieng,request_bi_chan,ghi_chu
1,2026-11-08,Xiaomi Pad 7 Pro,27808b8,"...",4:12,0,,,3,
2,2026-11-08,Xiaomi Pad 7 Pro,27808b8,"...",12:40,1,pre-roll,co,5,bo qua sau 5s
```

Cột `loai_quang_cao` ghi: `pre-roll`, `mid-roll`, `overlay`, `trang`. Nhiều loại thì cách nhau bằng dấu `;`.

## 6. Đối chứng (bắt buộc để biết chặn có hiệu quả không)

Không có đối chứng thì số "còn sót" không cho biết danh sách chặn giúp được bao nhiêu. Đo lại 5 video (lấy từ mẫu 20) với bản **không chặn**:

1. Trong `AdBlocker.kt`, tạm đặt `isBlocked` trả về `false`. Chỉ làm trên nhánh đo, không commit.
2. Build và cài lại, đo 5 video theo mục 2. Ghi cột `build` là `baseline-no-block`.
3. Khôi phục `AdBlocker.kt`, build lại.

So sánh số quảng cáo trung bình trên 5 video giữa bản không chặn và bản có chặn.

## 7. Ghi lại kết quả

- Lưu CSV vào `docs/adblock-results/`.
- Đăng comment lên issue #8 với: build, số video, tỉ lệ video có quảng cáo còn sót, số request chặn trung bình, và đường dẫn CSV.
- Ghi vào `docs/devlog.md` một mục mới, không sửa mục cũ.

## 8. Giới hạn đã biết

- Danh sách chặn theo tên miền không chặn được quảng cáo chèn trong video vì video quảng cáo và video chính dùng cùng host (`googlevideo.com`). Số liệu ở mục 4 sẽ phản ánh đúng giới hạn này.
- Đo bằng quan sát nên có sai số. Nếu có điều kiện, nhờ người thứ hai đếm 5 video để kiểm tra.
- Kết quả trên emulator không dùng cho nghiệm thu.
