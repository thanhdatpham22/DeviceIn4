# Ảnh biểu tượng DeviceIn4

Xuất ảnh từ Canva dưới dạng PNG nền trong suốt rồi chép vào thư mục này. JPG và WebP cũng được hỗ trợ. Tên file phải viết chữ thường đúng như danh sách dưới đây; bạn có thể đổi phần mở rộng `.png` thành `.webp`, `.jpg` hoặc `.jpeg`.

- `app_logo.png`: logo D4 ở góc trên trong ứng dụng.
- `android_version_10.png`, `android_version_11.png`, `android_version_12.png`, `android_version_13.png`, `android_version_14.png`, `android_version_15.png`, `android_version_16.png`…: logo riêng của từng phiên bản Android. Có thể thêm `android_version_default.png` làm logo dự phòng.
- `brand_xiaomi.png`, `brand_redmi.png`, `brand_oppo.png`, `brand_samsung.png`, `brand_google.png`, `brand_oneplus.png`, `brand_vivo.png`, `brand_huawei.png`: logo hãng điện thoại.
- `brand_qualcomm.png`, `brand_mediatek.png`, `brand_arm.png`, `brand_nvidia.png`: logo hãng chip/GPU.
- Thẻ CPU/GPU trong Device tự chọn ảnh theo tên hãng/chip/renderer: Snapdragon/Adreno → `brand_qualcomm.png`; MediaTek/Dimensity/Helio → `brand_mediatek.png`; Mali/Immortalis → `brand_arm.png`; Exynos → `brand_samsung.png`; Tensor → `brand_google.png`; Xclipse/Radeon → `brand_amd.png`; PowerVR → `brand_imagination.png`; Kirin → `brand_hisilicon.png`; UNISOC → `brand_unisoc.png`; Intel → `brand_intel.png`; NVIDIA/Tegra → `brand_nvidia.png`. Không nhận diện được hoặc ảnh thiếu/hỏng sẽ giữ icon mặc định. Ảnh được nạp từ chính thư mục này, không tải mạng.
- `page_device.png`, `page_os.png`, `page_cpu_gpu.png`, `page_ram.png`, `page_storage.png`, `page_display.png`, `page_battery.png`, `page_network.png`, `page_camera.png`, `page_sensor.png`, `page_features.png`: biểu tượng đầu mỗi trang.
- `sensor_accelerometer.png`, `sensor_gyroscope.png`, `sensor_light.png`, `sensor_proximity.png`, `sensor_pressure.png`, `sensor_magnetic.png`, `sensor_step.png`, `sensor_heart.png`, `sensor_temperature.png`: biểu tượng loại cảm biến.

Thư mục đã có logo Xiaomi, Samsung, OPPO, vivo, Huawei, OnePlus và Google, được chuyển thành PNG nền trong suốt từ [Simple Icons](https://github.com/simple-icons/simple-icons). Mã màu nền của mỗi hãng lấy từ [dữ liệu Simple Icons](https://github.com/simple-icons/simple-icons/blob/develop/data/simple-icons.json). Các huy hiệu `android_version_7.png` đến `android_version_17.png` là thiết kế số phiên bản của DeviceIn4 dựa trên biểu tượng Android từ cùng nguồn; đây không phải logo phiên bản chính thức. Logo là nhãn hiệu của từng hãng; xem [lưu ý của nguồn](https://github.com/simple-icons/simple-icons/blob/develop/DISCLAIMER.md).

Bạn không cần thêm đủ tất cả ảnh. Ảnh nào chưa có sẽ dùng biểu tượng hiện tại. Sau khi chép hoặc thay ảnh, build/cài lại APK. Nên xuất biểu tượng khoảng 256 px theo chiều dài nhất với nền trong suốt; ảnh lớn hơn sẽ được thu nhỏ khi đọc.

Biểu tượng **cài đặt trên màn hình chính Android** dùng `res/mipmap-anydpi*/ic_launcher*.xml`: Android 8+ có lớp nền và lớp điện thoại/chữ “i” để launcher tự cắt tròn, vuông, squircle; Android 13+ có thêm lớp đơn sắc cho themed icons. Android 7 có bản vector thường và bản tròn; `res/drawable/ic_devicein4.xml` là bản vector dùng chung. Ảnh trong thư mục assets này chỉ dùng bên trong ứng dụng.

Kích thước logo khi hiển thị được gom ở `app/src/main/java/com/devicein4/app/IconSizes.kt`.
