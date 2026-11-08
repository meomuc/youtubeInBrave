# Design system (tóm tắt)

Nguồn chuẩn cho giao diện native là [DESIGN.md](../DESIGN.md) ở thư mục gốc. Tệp này chỉ ghi các token đang dùng trong code để tra cứu nhanh.

- Theme: `Theme.YoutubeTablet` (kế thừa `Theme.Material3.DayNight.NoActionBar`), Dynamic Color trong `MainActivity`.
- Icon: Phosphor regular 2.1.1 trong `res/drawable/ic_ph_*.xml`; danh sách trong `docs/ui-icons.md`.
- Bo góc: thẻ 28dp (bảng), 16dp (banner). Lề 8dp, 24dp.
- Vùng chạm: 48dp tối thiểu, 56dp cho nút nổi.

Tạo bằng tay vì môi trường không có `search.py` của UI UX Pro Max. Khi có công cụ đó, chạy lại để sinh token chính thức và đối chiếu với `DESIGN.md`.
