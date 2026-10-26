# Kiểm tra trước phát hành Google Play

Ngày kiểm tra: 25/10/2026. Đây là đánh giá mã nguồn, không phải xác nhận Google Play sẽ duyệt.

## Đã có
- targetSdk 36, minSdk 24; phù hợp yêu cầu target API hiện hành cho app điện thoại.
- Không có INTERNET, quảng cáo, tài khoản hay SDK analytics trong các dependency trực tiếp hiện tại. Dữ liệu thiết bị được xử lý tại chỗ.
- Không yêu cầu QUERY_ALL_PACKAGES, MANAGE_EXTERNAL_STORAGE, quyền nền vị trí hoặc quyền root. Danh sách ứng dụng chỉ trong phạm vi Android cho phép.
- Năm bộ tài nguyên ngôn ngữ, adaptive/themed launcher icon; logo bên trong đồng bộ.
- Footer CPU không xuất hiện khi trang chưa có dữ liệu hoặc đang chuyển tab.

## Cần hoàn thành trước khi chốt bản public
1. **Chính sách quyền riêng tư:** mục About mới có mô tả ngắn, chưa có chính sách đầy đủ hay URL công khai. Cần tên nhà phát hành, email liên hệ, cách xử lý/lưu/xóa dữ liệu, quyền Camera/Vị trí/Điện thoại/Usage Access/cảm biến sức khỏe. Đưa link vào app và Play Console. Không tự đặt email hoặc URL giả.
2. **Ký phát hành:** build release hiện chưa cấu hình upload signing key. AAB tạo được nhưng chưa ký, không dùng APK debug để nộp. Tạo và giữ an toàn upload key, bật Play App Signing, xác nhận applicationId `com.devicein4.app` và versionCode trước lần nộp đầu tiên. Không lưu mật khẩu/key vào Git.
3. **App content / Data safety:** khai báo chính xác theo binary cuối. Chỉ xử lý tại thiết bị khác với gửi dữ liệu ra ngoài; rà lại nếu thêm SDK. Hoàn tất content rating, đối tượng người dùng, quảng cáo và quyền truy cập ứng dụng.
4. **Cảm biến sức khỏe:** app có BODY_SENSORS / READ_HEART_RATE, nên cần khai báo việc truy cập nhịp tim kể cả phục vụ chẩn đoán phần cứng, và giải trình phạm vi sử dụng. Nếu không muốn chức năng này trong bản public, cần quyết định bỏ chức năng và quyền tương ứng trước khi phát hành; không tự xóa trong lần sửa này.
5. **Store listing:** chuẩn bị icon PNG 512×512, feature graphic 1024×500, ảnh chụp màn hình thật, mô tả và email hỗ trợ. Icon adaptive trong APK không thay thế file ảnh listing. Kiểm tra quyền sử dụng các logo hãng trong assets.
6. **QA bản release:** chạy Play pre-launch report, thử cài/nâng cấp bản ký, Android 7–16, cỡ chữ lớn, xoay màn hình, từ chối/thu hồi quyền, không SIM, không cảm biến, thiết bị 16 KB page size. Chưa được coi là hoàn tất chỉ vì unit tests/lint thành công.
7. **Tài khoản developer:** nếu là tài khoản cá nhân mới thuộc diện yêu cầu thử nghiệm, cần closed test với ít nhất 12 người tham gia liên tục 14 ngày và xin quyền production. Kiểm tra trạng thái xác minh tài khoản trong Console.

## Các điểm kỹ thuật cần xác minh thêm
- `AdvancedRepository.getSystemProp` đang dùng reflection vào `android.os.SystemProperties` (API không công khai). Có fallback null nhưng cần kiểm thử trên nhiều hãng/Android; không xem đây là dữ liệu luôn đọc được.
- Một số nhận diện công nghệ lưu trữ có fallback theo SoC. Cần ghi rõ đó là suy luận và kiểm tra tính chính xác trước khi quảng bá khả năng xác minh linh kiện.
- Lint vẫn có cảnh báo API cũ, API nội bộ và tương thích phiên bản. Không có nghĩa là tất cả cảnh báo đều gây crash, nhưng vẫn cần QA release.

## Nguồn chính thức
- Target SDK: https://developer.android.com/google/play/requirements/target-sdk
- User Data / privacy policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Health declaration: https://support.google.com/googleplay/android-developer/answer/14738291
- Closed testing: https://support.google.com/googleplay/android-developer/answer/14151465

Không nên thêm tính năng lớn trước khi chốt. Ưu tiên chính sách quyền riêng tư, ký AAB và QA release.

## Kết quả kiểm tra lần sửa này
- 10 unit tests: không lỗi; 5 locale, mỗi locale 534 strings, kiểm tra tham số/plural đạt.
- Lint debug: 0 lỗi, 67 cảnh báo; không dùng baseline để bỏ qua lỗi.
- ELF LOAD alignment của libandroidx.graphics.path.so: 16384 trên cả 4 ABI. Vẫn cần chạy máy/emulator 16 KB và kiểm tra gói release thực tế.
- Chưa xác nhận lại vuốt nhanh/ngôn ngữ bằng thao tác trên máy thật trong lần sửa này.
