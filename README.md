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

## Chạy

Mở dự án bằng Android Studio hoặc chạy `./gradlew assembleDebug`. APK nằm tại `app/build/outputs/apk/debug/app-debug.apk`. Yêu cầu Android 7.0 (API 24) trở lên.

## Thay biểu tượng bằng ảnh trong thư mục

Chép ảnh PNG/JPG/WebP xuất từ Canva vào `app/src/main/assets/icons/` theo tên trong `app/src/main/assets/icons/README.md`, rồi build lại APK. Không cần sửa mã hoặc điền link. Ảnh chưa có hoặc lỗi sẽ dùng biểu tượng mặc định. Biểu tượng cài đặt trên màn hình chính Android vẫn nằm ở `app/src/main/res/drawable/ic_devicein4.xml`.

Ứng dụng đọc dữ liệu trên máy, không yêu cầu tài khoản hay tải ảnh qua internet. Android giới hạn serial, MAC thật, danh sách ứng dụng, phân loại System/Other và xác minh linh kiện đã thay; các trường không đọc được sẽ ghi rõ thay vì đưa ra giá trị suy đoán. Một số mục Camera, Network và thống kê dung lượng cần quyền hệ thống phù hợp.
