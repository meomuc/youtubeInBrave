# Danh sách lọc chặn quảng cáo

App tải hai danh sách lọc khi mở, theo ADR 0004:

| Danh sách | Nguồn | Mục đích |
|---|---|---|
| EasyList | https://easylist.to/easylist/easylist.txt | Chặn quảng cáo |
| EasyPrivacy | https://easylist.to/easylist/easyprivacy.txt | Chặn theo dõi |

Cache lưu trong thư mục cache của app (`adblock-filters.txt`), làm mới sau 7 ngày.

## Giấy phép

Theo trang chủ easylist.to, EasyList và EasyPrivacy phát hành theo giấy phép kép GPLv3 và CC BY-SA 3.0. Kiểm tra lại trang này trước khi phát hành APK cho người khác; nếu phát hành công khai, cần ghi nguồn và giấy phép trong ứng dụng.

Engine `adblock` (adblock-rust, Brave) phát hành theo MPL-2.0. Ghi chú sử dụng nằm trong `native/adblock-jni/Cargo.toml`.

## Cập nhật danh sách

Danh sách được cập nhật bởi nhà cung cấp. App làm mới cache khi quá 7 ngày. Khi thay đổi nguồn, sửa `FilterListLoader.kt` và ghi lại tại đây.
