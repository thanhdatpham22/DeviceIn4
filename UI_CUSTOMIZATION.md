# Chỉnh giao diện DeviceIn4

Bạn có thể chỉnh toàn bộ các phần chính mà không cần tìm khắp dự án:

| Muốn chỉnh | Vị trí |
| --- | --- |
| Thay logo hãng, biểu tượng trang, cảm biến, Android 7–17 | `app/src/main/assets/icons/` và danh sách tên file trong `app/src/main/assets/icons/README.md` |
| Phóng to/thu nhỏ logo | `app/src/main/java/com/devicein4/app/IconSizes.kt` |
| Logo và tên app ở đầu màn hình | hàm `Header` trong `MainActivity.kt` |
| Thanh tab và hiệu ứng giọt nước | hàm `LiquidTabs` trong `MainActivity.kt` |
| Khối tên máy, logo hãng, Android và thời gian chạy | hàm `OverviewHero` trong `MainActivity.kt` |
| Khối RAM + vòng Storage | hàm `MemoryCard` trong `MainActivity.kt` |
| Màu và nét của biểu đồ RAM | hàm `RamChart` trong `MainActivity.kt` |
| Các vòng CPU | hàm `CpuActivityCard` trong `MainActivity.kt` |
| Hai ô Wi-Fi và pin | hàm `QuickStats` trong `MainActivity.kt` |
| Đầu trang Device, OS, CPU… | hàm `PageIntro` trong `MainActivity.kt` |
| Màu nền, màu chữ, màu chủ đạo | các giá trị `ink`, `panel`, `border`, `muted`, `cyan`, `violet` gần đầu `MainActivity.kt` |
| Dữ liệu CPU, Wi-Fi, pin và RAM | `DeviceRepository.kt`; chu kỳ quét nằm trong `DeviceApp` của `MainActivity.kt` |

Trong `IconSizes.kt`, đơn vị `dp` là kích thước trên màn hình. Tăng `brandWidth`/`brandHeight` để logo hãng có nhiều chỗ hơn, giảm `brandImagePadding` để logo chiếm nhiều diện tích hơn. `androidVersionWidth`/`androidVersionHeight` chỉnh toàn bộ huy hiệu Android; `androidVersionIcon` chỉnh riêng hình bên trong. `appLogo`, `pageIcon` và `sensorIcon` điều chỉnh các icon còn lại. Ảnh được giữ đúng tỷ lệ; nếu logo trông nhỏ dù đã tăng kích thước, hãy cắt bớt vùng trong suốt thừa khi xuất ảnh từ Canva.

Để thay ảnh, xuất PNG nền trong suốt từ Canva, đặt đúng tên trong thư mục `assets/icons`, rồi build/cài lại APK. Biểu tượng launcher ngoài màn hình chính Android nằm ở `app/src/main/res/drawable/ic_devicein4.xml`.

Để build trên Windows: chạy `.\gradlew.bat assembleDebug --offline` tại thư mục gốc. APK mới nằm ở `app/build/outputs/apk/debug/app-debug.apk`.
