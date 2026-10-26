# DeviceIn4

Ứng dụng Android viết bằng Kotlin và Jetpack Compose để xem thông tin thiết bị và biểu đồ tài nguyên trực tiếp. Đây là bản Android đầu tiên; lớp đọc dữ liệu tách khỏi giao diện để thuận tiện mở rộng đa nền tảng.

## Chức năng

- Vuốt ngang hoặc chọn tab để mở Overview, Device, OS, CPU/GPU, RAM, Storage, Display, Battery, Network, Camera, Sensor và Features. Giọt nước theo vị trí vuốt trang liên tục; thanh tab chỉ tự cuộn khi vuốt nội dung, còn khi chạm tab đang hiển thị thì giữ nguyên vị trí cuộn. Mỗi trang giữ dữ liệu riêng trong lúc chuyển cảnh.
- Overview hiển thị tên máy và ứng dụng, logo hãng, huy hiệu phiên bản Android, thời gian hoạt động; biểu đồ vùng RAM 60 mẫu, vòng dung lượng Storage, vòng xung từng nhân CPU, cùng hai ô Wi-Fi và pin.
- Device/OS: thông tin nhận diện phần cứng, bản dựng Android, cập nhật bảo mật, bootloader, kernel, dấu hiệu root, thời gian chạy và thông tin Widevine được thiết bị công khai.
- CPU/GPU: cấu trúc CPU, xung nhịp, GPU renderer/vendor/driver, OpenGL ES và Vulkan. RAM, Battery và Network có thông số trực tiếp; Camera và Sensor liệt kê khả năng và đặc tính theo API hệ thống.
- Storage hiển thị tổng dung lượng, ứng dụng Android cho phép nhìn thấy, và dung lượng theo ứng dụng khi người dùng bật Usage Access. Display hiển thị mã PNP của tấm nền nếu Android cung cấp.
- Logo góc trái mở bảng thông tin phần mềm. Nút góc phải mở bảng cài đặt nhỏ để chọn ngôn ngữ bằng menu có cờ, chọn màu bằng menu có mẫu và mã màu hoặc nhập mã tùy ý, giao diện sáng/tối, đơn vị GB/GiB và °C/°F, cấu hình widget Device/RAM/Storage/Pin và ghim widget lên màn hình chính.

Ứng dụng lấy mẫu RAM, Wi-Fi và pin mỗi giây khi đang ở tiền cảnh; CPU được lấy mẫu mỗi 350 ms khi xem Overview và vòng tần số chuyển động mượt. Widget được Android cập nhật định kỳ và làm mới khi ứng dụng đang mở.

Tab Sensor có tổng số cảm biến, số loại, số biến thể đánh thức/không đánh thức và số cảm biến động đang kết nối. Danh sách được làm mới mỗi 5 giây khi xem trang; số lượng do SensorManager công khai có thể gồm cảm biến ảo, không tương đương số chip vật lý. Tab RAM gộp chi tiết bộ nhớ và công nghệ phần cứng thành một thẻ, giữ ZRAM riêng; tốc độ, băng thông và hãng RAM không đọc được sẽ không được thay bằng suy đoán theo SoC.

Mở từng Sensor để xem biểu đồ dữ liệu thực trong 10 giây gần nhất và bảng thông số gọn hai cột. XYZ dùng cho cảm biến vector, cảm biến một giá trị có một kênh, các kênh bổ sung được giữ nguyên; hỗ trợ cả sự kiện chỉ khi thay đổi và kích hoạt một lần. Luồng đo chỉ đăng ký khi thẻ mở, đang ở trang Sensor và app ở tiền cảnh; tự hủy khi đóng thẻ/rời trang/ẩn app. Cảm biến bước chân và nhịp tim yêu cầu quyền tương ứng khi cần. Không mô phỏng dữ liệu nếu thiết bị không cấp sự kiện.

Storage mở thẳng Usage Access của ứng dụng (dự phòng danh sách Usage Access trên máy không hỗ trợ liên kết trực tiếp); danh sách ứng dụng dùng hàng icon/tên/dung lượng, sắp xếp giảm dần trong phạm vi Android cho thấy và thời gian quét 8 giây. Tổng dung lượng ứng dụng gồm mã + dữ liệu; cache đã nằm trong dữ liệu. Header Battery hiển thị hình pin và % hiện tại. Camera cho phép tên xuống dòng đầy đủ.

Chạm vào hàng ứng dụng trong Storage để mở đúng trang chi tiết của ứng dụng đó trong cài đặt Android.

Danh sách ứng dụng Storage nằm trong một mục xổ xuống, mặc định đóng; chạm để xem hoặc thu gọn và vẫn có thể mở chi tiết từng ứng dụng. Network có thẻ Modem & phần cứng 4G/5G với baseband, số modem logic, khả năng LTE/NR và nguồn xác nhận. Android 12+ cập nhật thông tin hiển thị 5G NSA/Advanced khi trang Network đang mở và đã cấp quyền điện thoại. Danh sách công nghệ modem chỉ được truy vấn khi có quyền nhà mạng; kết nối LTE không bị diễn giải là thiết bị thiếu 5G. Khi không đủ dữ liệu, app ghi chưa thể xác minh. Khối tổng hợp CPU dành cột riêng cho tải trung bình và đo trước chiều cao tối đa của ba trạng thái theo chiều rộng/cỡ chữ hiện tại, giúp chuyển trạng thái không làm thay đổi chiều cao bố cục.

Camera đọc các đặc tính Android cho phép ngay cả khi chưa cấp quyền; quyền camera bổ sung những trường bị giới hạn. Khi quay lại từ cài đặt, app kiểm tra lại quyền Camera/Vị trí/Điện thoại/Usage Access và tải lại dữ liệu. Với quyền đã bị từ chối vĩnh viễn, nút cấp quyền mở trang cài đặt ứng dụng. App không vượt sandbox, SELinux hoặc quyền đặc biệt của hệ thống.

## Ngôn ngữ, biểu tượng và hoạt cảnh

- Hỗ trợ Việt, Anh (mặc định), Pháp, Nhật, Trung giản thể bằng tài nguyên Android `res/values*/strings.xml`. Chọn riêng trong Cài đặt của app hoặc trang ngôn ngữ ứng dụng Android 13+; chọn Hệ thống để dùng ngôn ngữ thiết bị. Preferences lưu lựa chọn trên Android cũ và AppCompat áp dụng trước onCreate và chuyển lựa chọn đã lưu của bản trước sang cơ chế mới một lần. Các tên ứng dụng/cảm biến và dữ liệu phần cứng gốc không được dịch.
- Bản dịch được quản lý trong `tools/localization_catalog.json`, `tools/localization_translations.tsv`, `tools/localization_extra.tsv`. Sau khi sửa, chạy `python tools/emit_locales.py`, rồi `python tools/check_locales.py` để kiểm tra đủ khóa, số lượng và tham số định dạng của cả năm ngôn ngữ. `LegacyStringResources.kt` là cầu nối cho nhãn dữ liệu cũ; bản dịch không còn được lưu trong từ điển Kotlin.
- Icon cài đặt là điện thoại có chữ “i”, có bản adaptive, round, legacy và monochrome. Nội dung chính nằm trong vùng an toàn của adaptive icon.
- Dòng nguồn cuối trang được thay bằng hoạt cảnh CPU chạy nhảy trên cỏ, vẽ trực tiếp bằng Compose. Chỉ chạy khi footer đang hiển thị và ứng dụng ở tiền cảnh; tôn trọng chế độ tắt hoạt ảnh trên Android 8+.

## Build APK

Mở dự án bằng Android Studio hoặc chạy `./gradlew assembleDebug`. APK nằm tại `app/build/outputs/apk/debug/app-debug.apk`. Yêu cầu Android 7.0 (API 24) trở lên.

## Thay biểu tượng bằng ảnh trong thư mục

Chép ảnh PNG/JPG/WebP xuất từ Canva vào `app/src/main/assets/icons/` theo tên trong `app/src/main/assets/icons/README.md`, rồi build lại APK. Không cần sửa mã hoặc điền link. Ảnh chưa có hoặc lỗi sẽ dùng biểu tượng mặc định. Biểu tượng launcher nằm trong `res/mipmap-anydpi*`; logo trong app dùng `res/drawable/ic_devicein4.xml` cùng thiết kế.

Ứng dụng đọc dữ liệu trên máy, không yêu cầu tài khoản hay tải ảnh qua internet. Android giới hạn serial, MAC thật, danh sách ứng dụng, phân loại System/Other và xác minh linh kiện đã thay; các trường không đọc được sẽ ghi rõ thay vì đưa ra giá trị suy đoán. Một số mục Camera, Network và thống kê dung lượng cần quyền hệ thống phù hợp.
