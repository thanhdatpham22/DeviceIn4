package com.devicein4.app

import android.Manifest
import android.appwidget.AppWidgetManager
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

private val lightTheme = mutableStateOf(false)
private val englishLanguage = mutableStateOf(false)
private val customAccent = mutableStateOf<Color?>(null)
private val ink get() = if (lightTheme.value) Color(0xFFF4F7FC) else Color(0xFF0B1020)
private val panel get() = if (lightTheme.value) Color.White else Color(0xFF151D30)
private val border get() = if (lightTheme.value) Color(0xFFDCE3EF) else Color(0xFF293449)
private val muted get() = if (lightTheme.value) Color(0xFF526176) else Color(0xFF99A9BD)
private val cyan get() = customAccent.value ?: if (lightTheme.value) Color(0xFF087E82) else Color(0xFF5FE1D5)
private val accentText get() = if (cyan.luminance() > .45f) Color(0xFF0B1020) else Color.White
private val violet get() = if (lightTheme.value) Color(0xFF7151C3) else Color(0xFFA78BFA)
private val primaryText get() = if (lightTheme.value) Color(0xFF11192C) else Color.White

private val englishText = mapOf(
    "Tổng quan" to "Overview", "Cảm biến" to "Sensors", "Thiết bị" to "Device",
    "Danh tính và phần cứng thiết bị" to "Device identity and hardware",
    "Android, kernel và thông tin bản dựng" to "Android, kernel and build information",
    "Cấu trúc CPU và khả năng đồ họa" to "CPU architecture and graphics capabilities",
    "RAM trực tiếp" to "Live RAM", "Lấy mẫu mỗi giây khi ứng dụng đang mở" to "One sample per second while the app is open",
    "Bộ nhớ trong, ngoài và dung lượng còn lại" to "Internal and external storage",
    "Độ phân giải, tần số quét và HDR" to "Resolution, refresh rate and HDR",
    "Dòng điện và công suất trực tiếp" to "Live current and power",
    "Kết nối và dữ liệu sử dụng" to "Connectivity and data usage",
    "Ống kính, cảm biến và khả năng chụp" to "Lenses, sensors and capture capabilities",
    "Danh sách cảm biến Android công khai" to "Available Android sensors",
    "Tính năng phần cứng và hệ thống" to "Hardware and system features",
    "Nhận diện thiết bị" to "Device identity", "Phần cứng" to "Hardware",
    "Phiên bản Android" to "Android version", "Bản cập nhật" to "Updates",
    "Bảo mật & khởi động" to "Security and boot", "Thời gian hoạt động" to "Uptime",
    "Môi trường" to "Environment", "Thời gian chạy" to "Runtime",
    "CPU · cấu trúc" to "CPU architecture", "GPU & đồ họa" to "GPU and graphics",
    "Bộ nhớ trong" to "Internal storage", "Bộ nhớ ngoài" to "External storage",
    "Màn hình" to "Display", "Tấm nền & hiển thị" to "Panel and display",
    "Pin & nguồn điện" to "Battery and power", "Kết nối & phần cứng mạng" to "Network and hardware",
    "Dữ liệu sử dụng" to "Data usage", "Tính năng phần cứng & hệ thống" to "Hardware and system features",
    "Chi tiết bộ nhớ" to "Memory details", "Phân loại dung lượng" to "Storage breakdown",
    "Ứng dụng dùng nhiều dung lượng" to "Largest visible apps",
    "RAM ĐANG SỬ DỤNG" to "RAM IN USE", "Khả dụng" to "Available",
    "Dung lượng còn lại" to "Remaining capacity", "Trạng thái" to "Status", "Sức khỏe" to "Health",
    "Nguồn" to "Power source", "Nhiệt độ" to "Temperature", "Điện áp" to "Voltage",
    "Dòng điện hiện tại" to "Current", "Dòng điện trung bình" to "Average current",
    "Công suất tức thời" to "Instant power", "Chu kỳ sạc" to "Charge cycles",
    "Công nghệ" to "Technology", "Đang sạc" to "Charging", "Không sạc" to "Not charging",
    "Đang xả" to "Discharging", "Đã đầy" to "Full", "Tốt" to "Good",
    "Có" to "Yes", "Không" to "No", "Không rõ" to "Unknown",
    "Không công khai" to "Unavailable", "Không được cung cấp" to "Not provided",
    "Không được Android công khai" to "Not exposed by Android",
    "Đang đọc dữ liệu" to "Reading device data", "Không thể đọc dữ liệu" to "Unable to read data",
    "Cần quyền camera" to "Camera permission needed", "Chưa có camera" to "No camera available",
    "Chưa có cảm biến" to "No sensor available", "Cho phép đọc thông số" to "Allow access",
    "Mã" to "Code", "Dữ liệu" to "Data", "Cache" to "Cache",
    "Loại" to "Type", "Tổng" to "Total", "Còn trống" to "Free",
    "Đã dùng" to "Used", "Ứng dụng nhìn thấy" to "Visible apps",
    "Ứng dụng hệ thống nhìn thấy" to "Visible system apps",
    "Thống kê theo tháng" to "Monthly usage", "Lỗi" to "Error",
    "Kiến trúc" to "Architecture", "Số nhân logic" to "Logical cores", "Số nhân" to "Cores",
    "Đối chiếu linh kiện" to "Component verification", "Từ lần khởi động cuối" to "Since last boot",
    "Thời gian CPU hoạt động" to "CPU uptime", "Kernel build" to "Kernel build",
    "Nhà sản xuất" to "Manufacturer", "Xung tối đa từng nhân" to "Max frequency per core",
    "Độ phân giải thực" to "Physical resolution", "Mật độ" to "Density",
    "Kích thước xấp xỉ" to "Approximate size", "Refresh hiện tại" to "Current refresh rate",
    "Chế độ hỗ trợ" to "Supported modes", "Độ sáng hệ thống" to "System brightness",
    "Hướng" to "Orientation", "Tên tấm nền" to "Panel name", "Mã OEM tấm nền" to "Panel OEM code",
    "Mã sản phẩm tấm nền" to "Panel product ID", "OEM tấm nền" to "Panel OEM",
    "Tình trạng đã thay" to "Replacement status", "Tổng dung lượng" to "Total capacity",
    "Ứng dụng khác / System / Cache / Other" to "Other apps / System / Cache / Other",
    "Cache của DeviceIn4" to "DeviceIn4 cache", "Có thể tháo rời" to "Removable",
    "Ứng dụng & phân loại" to "Apps and breakdown", "Mã ứng dụng" to "App code",
    "Dữ liệu ứng dụng" to "App data", "Cache ứng dụng" to "App cache",
    "Chưa phân loại" to "Unclassified", "Phạm vi" to "Scope",
    "Ứng dụng hệ thống" to "System app", "Ứng dụng người dùng" to "User app",
    "FIFO tối đa" to "Max FIFO", "Độ trễ tối thiểu" to "Minimum delay",
    "Nhà mạng" to "Carrier", "Loại SIM" to "SIM type", "Phần cứng Wi-Fi" to "Wi-Fi hardware",
    "Phần cứng di động" to "Cellular hardware", "Khả năng 4G/5G" to "4G/5G capability",
    "Thiết bị nhận từ lúc khởi động" to "Device received since boot",
    "Thiết bị gửi từ lúc khởi động" to "Device sent since boot",
    "App nhận từ lúc khởi động" to "App received since boot",
    "App gửi từ lúc khởi động" to "App sent since boot",
    "Wi-Fi tháng này" to "Wi-Fi this month", "Di động tháng này" to "Cellular this month",
    "Thống kê tháng" to "Monthly statistics", "Không hỗ trợ" to "Unsupported",
    "Ngoại tuyến" to "Offline", "Di động" to "Cellular", "Khác" to "Other",
    "Có internet" to "Internet available", "Không kết nối" to "Disconnected",
    "Đã kết nối, chưa xác thực internet" to "Connected, internet unverified",
    "Cần quyền vị trí" to "Location permission required", "Cần quyền điện thoại" to "Phone permission required",
    "Quá nóng" to "Overheated", "Hỏng" to "Dead", "Quá áp" to "Overvoltage",
    "Quá lạnh" to "Too cold", "Nguồn AC" to "AC", "Không dây" to "Wireless",
    "Không cắm sạc" to "Unplugged", "Điện tích còn lại" to "Remaining charge",
    "Trước" to "Front", "Sau" to "Back", "Ngoài" to "External",
    "Cấp phần cứng" to "Hardware level", "Độ phân giải JPEG tối đa" to "Max JPEG resolution",
    "Kích thước cảm biến" to "Sensor size", "Tiêu cự" to "Focal length", "Khẩu độ" to "Aperture",
    "Chống rung quang học" to "Optical stabilization", "Đèn flash" to "Flash",
    "Hướng cảm biến" to "Sensor orientation", "Độ phân giải cảm biến" to "Sensor resolution",
    "Vùng cảm biến hoạt động" to "Active sensor area", "Zoom số tối đa" to "Max digital zoom",
    "AF regions tối đa" to "Max AF regions", "AE regions tối đa" to "Max AE regions",
    "Capabilities (mã)" to "Capabilities (codes)"
    ,"Phân loại System / Other" to "System / Other breakdown"
    ,"Dung lượng từng ứng dụng" to "Storage by app"
    ,"Wi-Fi SSID" to "Wi-Fi SSID", "Mạng di động" to "Cellular network"
    ,"Cần quyền camera" to "Camera permission required"
    ,"Chưa có camera" to "No cameras reported"
    ,"Chưa có cảm biến" to "No sensors reported"
    ,"Đang đọc dữ liệu" to "Loading data"
    ,"Android giới hạn quyền đọc dung lượng của ứng dụng khác. Mở cài đặt lưu trữ của hệ thống để xem phân loại do thiết bị cung cấp." to "Android limits access to other apps' storage. Open system storage settings for the device's breakdown."
    ,"Bật Usage Access để xem mã, dữ liệu và cache của các ứng dụng Android cho phép nhìn thấy." to "Enable Usage Access to see app code, data and cache for visible apps."
    ,"Cho phép vị trí để Android cung cấp tên mạng Wi-Fi hiện tại." to "Allow location access to show the current Wi-Fi network name."
    ,"Cho phép trạng thái điện thoại để xem loại mạng 2G/3G/4G/5G." to "Allow phone state access to see the current 2G/3G/4G/5G network type."
    ,"Bật Usage Access để đọc tổng dữ liệu Wi-Fi và di động trong tháng này." to "Enable Usage Access for this month's Wi-Fi and cellular totals."
    ,"Android yêu cầu quyền camera để đọc đầy đủ đặc tính ống kính. Ứng dụng chỉ đọc thông số, không chụp ảnh." to "Android requires camera access to read lens characteristics. DeviceIn4 only reads specifications and does not take photos."
    ,"Thiết bị không công khai camera qua Camera2." to "This device reports no cameras through Camera2."
    ,"Thiết bị không công khai cảm biến qua Android SensorManager." to "This device reports no sensors through SensorManager."
    ,"Đợi thiết bị trả về thông số của trang này…" to "Waiting for device data…"
    ,"Android không cung cấp nguồn để xác định linh kiện đã thay" to "Android cannot verify replaced components"
    ,"Android 10+ giới hạn cho ứng dụng thường" to "Restricted for regular apps on Android 10+"
    ,"Chưa phát hiện dấu hiệu root" to "No root indicators detected"
    ,"Có dấu hiệu root (chưa xác nhận)" to "Possible root indicators (unconfirmed)"
    ,"Không thể xác minh qua API Android" to "Cannot be verified through Android APIs"
    ,"Mã PNP do hệ thống báo; không xác thực linh kiện vật lý" to "System-reported PNP code; physical component not verified"
    ,"Android không công khai nhà sản xuất cell pin" to "Android does not expose the battery cell manufacturer"
    ,"Không được cung cấp" to "Not provided", "Thiết bị không công khai" to "Not reported by device"
    ,"Không có" to "None", "Không công khai" to "Not reported"
    ,"Ngang" to "Landscape", "Dọc" to "Portrait"
    ,"Quyền truy cập" to "Access", "Không đọc được dữ liệu pin" to "Battery data unavailable"
    ,"Sau" to "Rear", "Trước" to "Front", "Ngoài" to "External"
)

private fun localized(text: String): String {
    if (!englishLanguage.value) return text
    englishText[text]?.let { return it }
    Regex("^(\\d+) ngày (\\d+) giờ (\\d+) phút$").matchEntire(text)?.let { m ->
        return "${m.groupValues[1]}d ${m.groupValues[2]}h ${m.groupValues[3]}m"
    }
    return text
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DeviceApp() }
    }
}

private val tabs = listOf("Tổng quan", "Device", "OS", "RAM", "Storage", "Display", "Battery", "Network", "Camera", "Sensor", "Features")

private fun androidCodename(): String = when (Build.VERSION.SDK_INT) {
    37 -> "Cinnamon Bun"
    36 -> "Baklava"
    35 -> "Vanilla Ice Cream"
    34 -> "Upside Down Cake"
    33 -> "Tiramisu"
    32 -> "Snow Cone v2"
    31 -> "Snow Cone"
    30 -> "Red Velvet Cake"
    29 -> "Quince Tart"
    28 -> "Pie"
    26, 27 -> "Oreo"
    24, 25 -> "Nougat"
    else -> Build.VERSION.CODENAME.takeUnless { it == "REL" || it.isBlank() } ?: "Không công khai"
}

@Composable
private fun DeviceApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repo = remember { DeviceRepository(context.applicationContext) }
    val advanced = remember { AdvancedRepository(context.applicationContext) }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val tabScrollState = rememberScrollState()
    var tabViewportWidth by remember { mutableIntStateOf(0) }
    val tabDensity = androidx.compose.ui.platform.LocalDensity.current
    val tabLeftPx = with(tabDensity) { 18.dp.toPx() }
    val tabStepPx = with(tabDensity) { 104.dp.toPx() }
    val tabChipPx = with(tabDensity) { 96.dp.toPx() }
    var tappedTab by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pagerState.isScrollInProgress, tappedTab, tabViewportWidth) {
        if (!pagerState.isScrollInProgress || tappedTab != null || tabViewportWidth == 0) return@LaunchedEffect
        snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }
            .collectLatest { position ->
                val center = tabLeftPx + position * tabStepPx + tabChipPx / 2f
                val desired = (center - tabViewportWidth / 2f).roundToInt().coerceIn(0, tabScrollState.maxValue)
                tabScrollState.scrollTo(desired)
            }
    }
    var themeChoice by remember { mutableStateOf(Preferences.theme(context)) }
    var widgetMetric by remember { mutableStateOf(Preferences.widgetMetric(context)) }
    var languageChoice by remember { mutableStateOf(Preferences.language(context)) }
    var unitChoice by remember { mutableStateOf(Preferences.units(context)) }
    var temperatureChoice by remember { mutableStateOf(Preferences.temperature(context)) }
    var accentHex by remember { mutableStateOf(Preferences.accent(context)) }
    var showInfo by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()
    lightTheme.value = when (themeChoice) { "light" -> true; "dark" -> false; else -> !systemDark }
    FormatOptions.binary.value = unitChoice == "binary"
    FormatOptions.fahrenheit.value = temperatureChoice == "f"
    englishLanguage.value = (if (languageChoice == "system") java.util.Locale.getDefault().language else languageChoice) == "en"
    customAccent.value = try { Color(android.graphics.Color.parseColor(accentHex)) } catch (_: Exception) { null }
    var foreground by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var sample by remember { mutableStateOf(repo.memory()) }
    var history by remember { mutableStateOf(listOf(sample.percent)) }
    var cores by remember { mutableStateOf(repo.cpuLive()) }
    var wifiLive by remember { mutableStateOf(repo.wifiLive()) }
    var batteryLive by remember { mutableStateOf(repo.batteryLive()) }
    var powerHistory by remember { mutableStateOf(listOf(0f)) }
    var battery by remember { mutableStateOf(repo.battery()) }
    var cameraGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var locationGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    var phoneGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) }
    var storageTotal by remember { mutableStateOf(android.os.StatFs(android.os.Environment.getDataDirectory().path).totalBytes) }
    var detailByPage by remember { mutableStateOf<Map<Int, List<Section>>>(emptyMap()) }
    var errorByPage by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraGranted = it }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { locationGranted = it }
    val phonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { phoneGranted = it }

    LaunchedEffect(lightTheme.value) {
        val activity = context as? Activity ?: return@LaunchedEffect
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = if (lightTheme.value) android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
        activity.window.navigationBarColor = if (lightTheme.value) android.graphics.Color.rgb(244, 247, 252) else android.graphics.Color.rgb(11, 16, 32)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(foreground) {
        var ticks = 0
        while (foreground) {
            val live = withContext(Dispatchers.IO) { Triple(repo.memory(), repo.batteryLive(), repo.wifiLive()) }
            sample = live.first
            batteryLive = live.second
            wifiLive = live.third
            if (ticks % 5 == 0) battery = withContext(Dispatchers.IO) { repo.battery() }
            history = (history + sample.percent).takeLast(60)
            powerHistory = (powerHistory + ((batteryLive.powerW ?: 0.0).toFloat())).takeLast(60)
            if (pagerState.currentPage == 7 && ticks % 5 == 0) {
                try { detailByPage = detailByPage + (7 to withContext(Dispatchers.IO) { advanced.network(locationGranted, phoneGranted) }) }
                catch (e: Exception) { errorByPage = errorByPage + (7 to "Lỗi Network: ${e.javaClass.simpleName}") }
            }
            if (ticks % 10 == 0) withContext(Dispatchers.IO) { DeviceWidgetProvider.updateAll(context) }
            ticks++
            delay(1000)
        }
    }

    LaunchedEffect(foreground, pagerState.currentPage) {
        if (!foreground || pagerState.currentPage != 0) return@LaunchedEffect
        while (true) {
            cores = withContext(Dispatchers.IO) { repo.cpuLive() }
            delay(350)
        }
    }

    LaunchedEffect(pagerState.targetPage, cameraGranted, locationGranted, phoneGranted, unitChoice, temperatureChoice) {
        val requestedPage = pagerState.targetPage
        if (requestedPage == 8 && !cameraGranted) return@LaunchedEffect
        if (requestedPage in detailByPage && requestedPage != 7) return@LaunchedEffect
        try {
            val loaded = withContext(Dispatchers.IO) {
                when (requestedPage) {
                    0 -> emptyList()
                    1 -> advanced.device() + advanced.cpuGpu()
                    2 -> advanced.os()
                    3 -> advanced.ramExtras()
                    4 -> advanced.storage().also { storageTotal = it.first }.second + advanced.storageAnalysis()
                    5 -> advanced.display()
                    7 -> advanced.network(locationGranted, phoneGranted)
                    8 -> repo.cameras()
                    9 -> advanced.sensors()
                    10 -> advanced.features()
                    else -> emptyList()
                }
            }
            detailByPage = detailByPage + (requestedPage to loaded)
            errorByPage = errorByPage - requestedPage
        } catch (e: Exception) {
            errorByPage = errorByPage + (requestedPage to "Lỗi ${e.javaClass.simpleName}: ${e.message ?: "thiết bị không cho phép đọc dữ liệu"}")
        }
    }

    MaterialTheme {
        Surface(color = ink, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column {
                Header(onInfo = { showInfo = true }, onSettings = { showSettings = true })
                LiquidTabs(pagerState.settledPage, pagerState.currentPage + pagerState.currentPageOffsetFraction,
                    pagerState.isScrollInProgress, tappedTab, tabScrollState,
                    onViewportWidth = { tabViewportWidth = it }) { index ->
                    if (index != pagerState.settledPage) {
                        tappedTab = index
                        scope.launch {
                            try { pagerState.animateScrollToPage(index) }
                            finally { if (tappedTab == index) tappedTab = null }
                        }
                    }
                }
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val pageListState = remember(page) { LazyListState() }
                val detail = detailByPage[page] ?: emptyList()
                val detailError = errorByPage[page]
                LazyColumn(
                    state = pageListState,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val distance = kotlin.math.abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
                        alpha = 1f - distance * .14f
                        scaleX = 1f - distance * .025f
                        scaleY = scaleX
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        when (page) {
                            0 -> OverviewHero()
                            1 -> DeviceIntro()
                            2 -> OsIntro()
                            3 -> PageIntro("RAM trực tiếp", "Lấy mẫu mỗi giây khi ứng dụng đang mở",
                                if (englishLanguage.value) "RAM manufacturer: Not exposed by Android" else "Nhà sản xuất RAM: Android không công khai")
                            4 -> PageIntro("Storage", "Bộ nhớ trong, ngoài và dung lượng còn lại")
                            5 -> PageIntro("Display", "Độ phân giải, tần số quét và HDR",
                                displayManufacturer(detail))
                            6 -> PageIntro("Battery", "Dòng điện và công suất trực tiếp",
                                if (englishLanguage.value) "Battery cell manufacturer: Not exposed by Android" else "Nhà sản xuất cell pin: Android không công khai")
                            7 -> PageIntro("Network", "Kết nối và dữ liệu sử dụng")
                            8 -> PageIntro("Camera", "Ống kính, cảm biến và khả năng chụp",
                                if (englishLanguage.value) "Camera module manufacturer: Not exposed by Camera2" else "Hãng module camera: Camera2 không công khai")
                            9 -> PageIntro("Sensor", "Danh sách cảm biến Android công khai")
                            10 -> PageIntro("Features", "Tính năng phần cứng và hệ thống")
                        }
                    }
                    if (page == 0 || page == 3) {
                        item { MemoryCard(sample, history, expanded = page == 3, storageTotal = storageTotal) }
                    }
                    if (page == 0) item { CpuActivityCard(cores) }
                    if (page == 0) item { QuickStats(batteryLive, wifiLive) }
                    if (page == 4) item { StorageCard(storageTotal, detail) }
                    if (page == 4) item {
                        InfoCard("Phân loại System / Other", "Android giới hạn quyền đọc dung lượng của ứng dụng khác. Mở cài đặt lưu trữ của hệ thống để xem phân loại do thiết bị cung cấp.") {
                            context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
                        }
                    }
                    if (page == 4 && !advanced.hasUsageAccess()) item {
                        InfoCard("Dung lượng từng ứng dụng", "Bật Usage Access để xem mã, dữ liệu và cache của các ứng dụng Android cho phép nhìn thấy.") {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                    }
                    if (page == 3) item {
                        SectionCard(Section("Chi tiết bộ nhớ", listOf(
                            Field("Nhà sản xuất RAM", "Android không công khai"),
                            Field("RAM hệ thống", bytes(sample.total)),
                            Field("RAM đang sử dụng", bytes(sample.used)),
                            Field("RAM khả dụng", bytes(sample.available)),
                            Field("Ngưỡng thiếu RAM", bytes(sample.threshold)),
                            Field("Bộ nhớ heap của app", bytes(sample.appUsed)),
                            Field("RAM quảng cáo", sample.advertised?.let(::bytes) ?: "Không công khai"),
                            Field("Áp lực bộ nhớ", if (sample.low) "Hệ thống báo thiếu RAM" else "Bình thường")
                        )))
                    }
                    if (page == 6) item { BatteryDashboard(batteryLive, powerHistory) }
                    if (page == 6) item { SectionCard(battery) }
                    if (page == 7 && !locationGranted) item {
                        InfoCard("Wi-Fi SSID", "Cho phép vị trí để Android cung cấp tên mạng Wi-Fi hiện tại.") {
                            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                    if (page == 7 && !phoneGranted) item {
                        InfoCard("Mạng di động", "Cho phép trạng thái điện thoại để xem loại mạng 2G/3G/4G/5G.") {
                            phonePermission.launch(Manifest.permission.READ_PHONE_STATE)
                        }
                    }
                    if (page == 7 && !advanced.hasUsageAccess()) item {
                        InfoCard("Thống kê theo tháng", "Bật Usage Access để đọc tổng dữ liệu Wi-Fi và di động trong tháng này.") {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                    }
                    if (page == 8 && !cameraGranted) item {
                        InfoCard("Cần quyền camera", "Android yêu cầu quyền camera để đọc đầy đủ đặc tính ống kính. Ứng dụng chỉ đọc thông số, không chụp ảnh.") {
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                    if (detailError != null) item { InfoCard("Không thể đọc dữ liệu", detailError!!) }
                    if (page == 8 && cameraGranted && page in detailByPage && detail.isEmpty() && detailError == null) item {
                        InfoCard("Chưa có camera", "Thiết bị không công khai camera qua Camera2.")
                    }
                    if (page == 9 && page in detailByPage && detail.isEmpty() && detailError == null) item {
                        InfoCard("Chưa có cảm biến", "Thiết bị không công khai cảm biến qua Android SensorManager.")
                    }
                    if (page !in detailByPage && detailError == null && page !in listOf(0, 6) && (page != 8 || cameraGranted)) item {
                        InfoCard("Đang đọc dữ liệu", "Đợi thiết bị trả về thông số của trang này…")
                    }
                    items(if (page == 0) emptyList() else detail) { section ->
                        when {
                            page == 9 -> SensorCard(section)
                            page == 1 && section.title in listOf("CPU · cấu trúc", "GPU & đồ họa") -> AnalysisCard(section)
                            else -> SectionCard(section)
                        }
                    }
                    item {
                        Text(if (englishLanguage.value) "Source: Android system APIs · Restricted data is noted in each section · Swipe to change pages" else "Nguồn: API hệ thống Android · Dữ liệu bị giới hạn được ghi rõ ở từng mục · Vuốt ngang để chuyển trang",
                            color = muted.copy(alpha = .7f), fontSize = 11.sp, lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
                    }
                }
                }
            }
            if (showInfo || showSettings) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)).clickable { showInfo = false; showSettings = false })
            AnimatedVisibility(showInfo, enter = slideInHorizontally { -it }, exit = slideOutHorizontally { -it }, modifier = Modifier.align(Alignment.CenterStart)) {
                InfoDrawer { showInfo = false }
            }
            AnimatedVisibility(showSettings, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }, modifier = Modifier.align(Alignment.CenterEnd)) {
                SettingsContent(themeChoice, languageChoice, unitChoice, temperatureChoice, accentHex, widgetMetric,
                    onTheme = { themeChoice = it; Preferences.setTheme(context, it); DeviceWidgetProvider.updateAll(context) },
                    onLanguage = { languageChoice = it; Preferences.setLanguage(context, it); DeviceWidgetProvider.updateAll(context) },
                    onUnits = { unitChoice = it; Preferences.setUnits(context, it); detailByPage = emptyMap(); DeviceWidgetProvider.updateAll(context) },
                    onTemperature = { temperatureChoice = it; Preferences.setTemperature(context, it); detailByPage = emptyMap() },
                    onAccent = { accentHex = it; Preferences.setAccent(context, it); DeviceWidgetProvider.updateAll(context) },
                    onMetric = { widgetMetric = it; Preferences.setWidgetMetric(context, it); DeviceWidgetProvider.updateAll(context) },
                    onPin = {
                        val manager = AppWidgetManager.getInstance(context)
                        val provider = ComponentName(context, DeviceWidgetProvider::class.java)
                        if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(provider, null, null)
                    }, onClose = { showSettings = false })
            }
            }
        }
    }
}

@Composable
private fun Header(onInfo: () -> Unit, onSettings: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(ink).padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Surface(onClick = onInfo, color = Color.Transparent, shape = RoundedCornerShape(11.dp)) {
            Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(cyan, violet))), contentAlignment = Alignment.Center) {
                AssetIcon(IconAssets.appLogo, Modifier.size(IconSizes.appLogo), "DeviceIn4") {
                    Text("D4", color = accentText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text("DeviceIn4", color = primaryText, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(if (englishLanguage.value) "Device intelligence" else "Thông tin thiết bị", color = cyan, fontSize = 9.sp)
        }
        Spacer(Modifier.weight(1f))
        Surface(onClick = onSettings, color = panel, shape = CircleShape) {
            Text("⚙", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = primaryText, fontSize = 18.sp)
        }
    }
}

@Composable
private fun LiquidTabs(selected: Int, pagePosition: Float, pagerMoving: Boolean, tappedTab: Int?,
                       scrollState: androidx.compose.foundation.ScrollState, onViewportWidth: (Int) -> Unit,
                       onSelect: (Int) -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val leftPx = with(density) { 18.dp.toPx() }
    val chipPx = with(density) { 96.dp.toPx() }
    val stepPx = with(density) { 104.dp.toPx() }
    val progress = (tappedTab?.toFloat() ?: pagePosition).coerceIn(0f, tabs.lastIndex.toFloat())
    // Every chip has a fixed width. Its canvas coordinate is exactly its content coordinate
    // minus the horizontal scroll offset, even when the selected chip is off screen.
    val targetX = leftPx + progress * stepPx - scrollState.value
    val liquidX = remember { Animatable(leftPx) }
    LaunchedEffect(targetX, pagerMoving, tappedTab, scrollState.isScrollInProgress) {
        if (tappedTab == null && (pagerMoving || scrollState.isScrollInProgress)) {
            liquidX.snapTo(targetX)
        } else {
            liquidX.animateTo(targetX, spring(dampingRatio = .72f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val fill = cyan
    val outline = border
    val labelColor = muted
    Box(Modifier.fillMaxWidth().padding(vertical = 3.dp).height(48.dp).clipToBounds()
        .onSizeChanged { onViewportWidth(it.width) }) {
        Canvas(Modifier.fillMaxSize()) {
            val h = 36.dp.toPx()
            val y = (size.height - h) / 2f
            val x = if (tappedTab == null && (pagerMoving || scrollState.isScrollInProgress)) targetX else liquidX.value
            val travel = targetX - x
            val swipePhase = kotlin.math.abs(progress - kotlin.math.floor(progress))
            val swipeStretch = if (pagerMoving && tappedTab == null) kotlin.math.sin(Math.PI * swipePhase).toFloat() * 13.dp.toPx() else 0f
            val stretch = kotlin.math.min(kotlin.math.abs(travel) * .32f + swipeStretch, 32.dp.toPx())
            drawRoundRect(fill, Offset(x, y), Size(chipPx, h), CornerRadius(h / 2f, h / 2f))
            if (stretch > 3.dp.toPx()) {
                val right = if (kotlin.math.abs(travel) > 1f) travel > 0 else progress >= selected
                val edge = if (right) x + chipPx else x
                val start = if (right) edge - 5.dp.toPx() else edge - stretch
                drawOval(fill.copy(alpha = .82f), Offset(start, y + h * .25f), Size(stretch + 5.dp.toPx(), h * .5f))
                drawCircle(fill.copy(alpha = .70f), radius = h * .16f,
                    center = Offset(edge + (if (right) stretch else -stretch), y + h / 2f))
            }
        }
        Row(Modifier.fillMaxSize().horizontalScroll(scrollState), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(18.dp))
            tabs.forEachIndexed { index, title ->
                if (index > 0) Spacer(Modifier.width(8.dp))
                val emphasis = (1f - kotlin.math.abs(index - progress)).coerceIn(0f, 1f)
                Box(Modifier.width(96.dp).height(48.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClick = { onSelect(index) }), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(50))
                        .border(1.dp, outline.copy(alpha = 1f - emphasis), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center) {
                        Text(localized(title), color = lerp(labelColor, accentText, emphasis),
                            fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
                    }
                }
            }
            Spacer(Modifier.width(18.dp))
        }
    }
}

@Composable
private fun OverviewHero() {
    val uptime = android.os.SystemClock.elapsedRealtime() / 1000
    val base = panel
    val heroGradient = Brush.linearGradient(listOf(
        lerp(base, cyan, if (lightTheme.value) .12f else .18f),
        base,
        lerp(base, violet, if (lightTheme.value) .10f else .18f)
    ))
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(heroGradient).border(1.dp, border, RoundedCornerShape(24.dp))
        .padding(20.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f)) {
            Text("DEVICEIN4  /  ${if (englishLanguage.value) "YOUR DEVICE" else "THIẾT BỊ CỦA BẠN"}",
                color = cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
            Spacer(Modifier.height(10.dp))
            Text("${Build.MANUFACTURER} ${Build.MODEL}", color = primaryText,
                fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Text("Android ${Build.VERSION.RELEASE} · ${androidCodename()}", color = primaryText,
                fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Text("${if (englishLanguage.value) "Uptime" else "Thời gian hoạt động"} · ${uptime / 86_400}d ${uptime % 86_400 / 3_600}h ${uptime % 3_600 / 60}m",
                color = muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        BrandBadge(Build.MANUFACTURER, IconSizes.heroBrandWidth, IconSizes.heroBrandHeight,
            IconSizes.heroBrandPadding)
    }
}

@Composable
private fun BrandBadge(brand: String, width: Dp = IconSizes.brandWidth,
                       height: Dp = IconSizes.brandHeight,
                       imagePadding: Dp = IconSizes.brandImagePadding) {
    val lower = brand.lowercase()
    val imageKey = IconAssets.brand(brand)
    val mark = when {
        "xiaomi" in lower || "redmi" in lower -> "mi"
        "oppo" in lower -> "oppo"
        "samsung" in lower -> "SAMSUNG"
        "google" in lower -> "G"
        "oneplus" in lower -> "1+"
        "vivo" in lower -> "vivo"
        "huawei" in lower -> "HUAWEI"
        else -> brand.take(2).uppercase()
    }
    val tint = when {
        "xiaomi" in lower || "redmi" in lower -> Color(0xFFFF6900)
        "oppo" in lower -> Color(0xFF2D683D)
        "samsung" in lower -> Color(0xFF1428A0)
        "google" in lower -> Color(0xFF4285F4)
        "oneplus" in lower -> Color(0xFFF5010C)
        "vivo" in lower -> Color(0xFF415FFF)
        "huawei" in lower -> Color(0xFFFF0000)
        else -> violet
    }
    Surface(color = tint, shape = RoundedCornerShape(11.dp)) {
        Box(Modifier.width(width).height(height),
            contentAlignment = Alignment.Center) {
            AssetIcon(imageKey, Modifier.fillMaxSize().padding(imagePadding), brand) {
                Text(mark, color = Color.White, fontSize = if (mark.length > 4) 15.sp else 22.sp,
                    fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DeviceIntro() {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        BrandBadge(Build.MANUFACTURER, IconSizes.devicePageBrandWidth,
            IconSizes.devicePageBrandHeight, IconSizes.devicePageBrandPadding)
        Column(Modifier.weight(1f)) {
            Text("Device Information", color = primaryText, fontWeight = FontWeight.Bold,
                fontSize = 24.sp, lineHeight = 28.sp)
            Text("${Build.MANUFACTURER} ${Build.MODEL}", color = muted,
                fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun OsIntro() {
    val release = Build.VERSION.RELEASE
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)) {
        Box(Modifier.size(IconSizes.osPageLogo).clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF3DDC84).copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            AssetIcon(IconAssets.androidVersion(release), Modifier.fillMaxSize().padding(6.dp),
                "Android $release") {
                Text(release, color = Color(0xFF3DDC84), fontSize = 24.sp,
                    fontWeight = FontWeight.Black)
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Android $release", color = primaryText, fontWeight = FontWeight.Bold,
                fontSize = 25.sp)
            Text(androidCodename(), color = cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(localized("Android, kernel và thông tin bản dựng"), color = muted,
                fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun AndroidVersionBadge() {
    val release = Build.VERSION.RELEASE
    Surface(color = Color(0xFF3DDC84).copy(alpha = .16f), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.width(IconSizes.androidVersionWidth).height(IconSizes.androidVersionHeight)
            .padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            AssetIcon(IconAssets.androidVersion(release), Modifier.size(IconSizes.androidVersionIcon),
                "Android $release") {
                Icon(Icons.Default.Android, contentDescription = null, tint = Color(0xFF3DDC84),
                    modifier = Modifier.size(IconSizes.androidVersionIcon))
            }
            Spacer(Modifier.width(5.dp))
            Column {
                Text("Android", color = primaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(release, color = cyan, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun CpuActivityCard(cores: List<CpuCore>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(22.dp)).padding(18.dp)) {
        Text(if (englishLanguage.value) "CPU ACTIVITY" else "CPU ĐANG HOẠT ĐỘNG", color = muted, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        if (cores.none { it.currentKhz != null }) Text(if (englishLanguage.value) "Current frequency unavailable" else "Android không cho đọc xung hiện tại", color = muted, fontSize = 12.sp)
        cores.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { core ->
                    Column(Modifier.weight(1f).padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val ring = cyan
                        val track = border
                        val smoothFraction by animateFloatAsState(core.fraction,
                            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
                            label = "Core ${core.index + 1} frequency")
                        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawArc(track, 0f, 360f, false, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
                                drawArc(ring, -90f, 360f * smoothFraction, false, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
                            }
                            Text(core.name, color = primaryText, fontWeight = FontWeight.Bold,
                                fontSize = 8.sp, lineHeight = 9.sp, textAlign = TextAlign.Center,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 7.dp))
                        }
                        Text(core.currentKhz?.let { "%.2f GHz".format(java.util.Locale.US, it / 1_000_000.0) } ?: "—",
                            color = muted, fontSize = 10.sp, maxLines = 1)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun QuickStats(battery: BatteryLive, wifi: WifiLive) {
    val batteryStatus = when (battery.status) {
        android.os.BatteryManager.BATTERY_STATUS_CHARGING -> if (englishLanguage.value) "Charging" else "Đang sạc"
        android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> if (englishLanguage.value) "Discharging" else "Đang xả"
        android.os.BatteryManager.BATTERY_STATUS_FULL -> if (englishLanguage.value) "Full" else "Đã đầy"
        android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> if (englishLanguage.value) "Not charging" else "Không sạc"
        else -> if (englishLanguage.value) "Unknown" else "Không rõ"
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(panel)
            .border(1.dp, border, RoundedCornerShape(16.dp)).padding(14.dp)) {
            Text("Wi-Fi", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(if (wifi.connected) wifi.linkMbps?.let { "$it Mbps" } ?: "— Mbps"
                else if (englishLanguage.value) "Offline" else "Ngoại tuyến",
                color = primaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${wifi.frequencyMHz?.let { "$it MHz" } ?: "— MHz"} · ${if (englishLanguage.value) "BW" else "Băng thông"} ${wifi.bandwidthMbps?.let { "$it Mbps" } ?: "—"}",
                color = muted, fontSize = 10.sp, maxLines = 2, lineHeight = 14.sp)
        }
        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(panel)
            .border(1.dp, border, RoundedCornerShape(16.dp)).padding(14.dp)) {
            Text(if (englishLanguage.value) "BATTERY" else "PIN", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(if (battery.level >= 0) "${battery.level}%" else "—", color = primaryText,
                fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("$batteryStatus · ${battery.powerW?.let { "%.1f W".format(java.util.Locale.US, kotlin.math.abs(it)) } ?: "— W"}",
                color = muted, fontSize = 10.sp, maxLines = 2, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier, darkSurface: Boolean = false) {
    Column(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .06f)).padding(13.dp)) {
        Text(label, color = if (darkSurface) Color(0xFFB6C7DB) else muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(5.dp))
        Text(value, color = if (darkSurface) Color.White else primaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

private fun displayManufacturer(detail: List<Section>): String {
    val panel = detail.firstOrNull { it.title == "Tấm nền & hiển thị" }
    val name = panel?.fields?.firstOrNull { it.label == "Tên tấm nền" }?.value
        ?.takeUnless { it.isBlank() || it.startsWith("Không") }
    val pnp = panel?.fields?.firstOrNull { it.label == "Mã OEM tấm nền" }?.value
        ?.takeUnless { it.isBlank() || it.startsWith("Không") }
    return if (englishLanguage.value) {
        "Display name: ${name ?: "Not reported"} · OEM PNP code: ${pnp ?: "Not reported"}"
    } else {
        "Tên màn hình: ${name ?: "Không công khai"} · Mã OEM PNP: ${pnp ?: "Không công khai"}"
    }
}

@Composable
private fun PageIntro(title: String, subtitle: String, manufacturer: String? = null) {
    val icon = when (title) {
        "RAM trực tiếp" -> Icons.Default.Memory
        "Storage" -> Icons.Default.Storage
        "Display" -> Icons.Default.Visibility
        "Battery" -> Icons.Default.BatteryFull
        "Network" -> Icons.Default.Wifi
        "Camera" -> Icons.Default.CameraAlt
        "Sensor" -> Icons.Default.Sensors
        "Features" -> Icons.Default.Devices
        else -> Icons.Default.Devices
    }
    Row(Modifier.padding(top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(cyan.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
            AssetIcon(IconAssets.page(title), Modifier.size(IconSizes.pageIcon), title) {
                Icon(icon, contentDescription = null, tint = cyan, modifier = Modifier.size(IconSizes.pageIcon))
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(localized(title), color = primaryText, fontWeight = FontWeight.Bold, fontSize = 25.sp)
            Text(localized(subtitle), color = muted, fontSize = 12.sp, lineHeight = 16.sp)
            if (manufacturer != null) {
                Spacer(Modifier.height(3.dp))
                Text(manufacturer, color = cyan, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun MemoryCard(sample: MemorySample, history: List<Float>, expanded: Boolean, storageTotal: Long) {
    val storageAvailable = android.os.StatFs(android.os.Environment.getDataDirectory().path).availableBytes
    val storageUsed = (storageTotal - storageAvailable).coerceAtLeast(0)
    val storageFraction = if (storageTotal > 0) (storageUsed.toFloat() / storageTotal).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(22.dp)).padding(if (expanded) 20.dp else 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(localized("RAM ĐANG SỬ DỤNG"), color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(bytes(sample.used), color = primaryText, fontSize = if (expanded) 27.sp else 22.sp,
                    fontWeight = FontWeight.Bold)
                Text("${(sample.percent * 100).toInt()}% · ${bytes(sample.total)}",
                    color = cyan, fontSize = 11.sp)
            }
            if (!expanded) Column(Modifier.width(86.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                    val ring = violet
                    val track = border
                    Canvas(Modifier.fillMaxSize()) {
                        drawArc(track, 0f, 360f, false, style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
                        drawArc(ring, -90f, 360f * storageFraction, false,
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Text("${(storageFraction * 100).toInt()}%", color = primaryText,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(if (englishLanguage.value) "STORAGE" else "BỘ NHỚ", color = muted,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(if (expanded) 16.dp else 8.dp))
        RamChart(history, if (expanded) 180 else 78)
        Spacer(Modifier.height(7.dp))
        Row {
            Text("${localized("Khả dụng")}: ${bytes(sample.available)}", color = muted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(if (expanded) {
                if (englishLanguage.value) "LAST 60 SECONDS" else "60 GIÂY GẦN NHẤT"
            } else bytes(storageUsed), color = muted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun RamChart(values: List<Float>, height: Int) {
    Canvas(Modifier.fillMaxWidth().height(height.dp)) {
        if (values.isEmpty()) return@Canvas
        val shown = values.takeLast(60)
        val points = shown.mapIndexed { index, value ->
            Offset(if (shown.size == 1) size.width else size.width * index / (shown.size - 1),
                size.height * (1f - value.coerceIn(0f, 1f)))
        }
        val line = Path().apply {
            moveTo(if (points.size == 1) 0f else points.first().x, points.first().y)
            if (points.size == 1) lineTo(size.width, points.first().y)
            for (index in 1 until points.size) {
                val before = points[index - 1]
                val after = points[index]
                val middle = (before.x + after.x) / 2f
                cubicTo(middle, before.y, middle, after.y, after.x, after.y)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, brush = Brush.verticalGradient(listOf(cyan.copy(alpha = .36f), cyan.copy(alpha = .04f))))
        drawPath(line, color = cyan, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(cyan, 3.5.dp.toPx(), points.last())
    }
}

@Composable
private fun SectionCard(section: Section) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(20.dp)).padding(19.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized(section.title), color = primaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (section.title == "GPU & đồ họa") section.fields.firstOrNull { it.label == "GPU vendor" }?.value
                ?.takeIf { it.isNotBlank() && !it.startsWith("Lỗi") }?.let { BrandBadge(it) }
        }
        Spacer(Modifier.height(12.dp))
        section.fields.forEachIndexed { index, field ->
            if (index > 0) Spacer(Modifier.fillMaxWidth().height(1.dp).background(border))
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(localized(field.label), color = muted, fontSize = 12.sp, modifier = Modifier.weight(.43f))
                Text(localized(field.value), color = primaryText, fontSize = 12.sp, lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(.57f))
            }
        }
    }
}

@Composable
private fun AnalysisCard(section: Section) {
    var expanded by rememberSaveable(section.title) { mutableStateOf(false) }
    val cpu = section.title.startsWith("CPU")
    val basicLabels = if (cpu) setOf("Processor", "SoC OEM", "Số nhân logic")
        else setOf("GPU renderer", "GPU vendor", "OpenGL ES")
    val visible = if (expanded) section.fields else section.fields.filter { it.label in basicLabels }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(20.dp)).padding(18.dp)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                .background(cyan.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Icon(if (cpu) Icons.Default.Memory else Icons.Default.Visibility,
                    contentDescription = null, tint = cyan, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(localized(section.title), color = primaryText, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
                Text(if (expanded) {
                    if (englishLanguage.value) "Collapse analysis" else "Thu gọn phân tích"
                } else {
                    if (englishLanguage.value) "In-depth analysis" else "Phân tích chuyên sâu"
                }, color = cyan, fontSize = 11.sp)
            }
            val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "Analysis arrow")
            Icon(Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = cyan, modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = arrow })
        }
        Spacer(Modifier.height(10.dp))
        visible.forEachIndexed { index, field ->
            if (index > 0) Spacer(Modifier.fillMaxWidth().height(1.dp).background(border))
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(localized(field.label), color = muted, fontSize = 12.sp,
                    modifier = Modifier.weight(.43f))
                Text(localized(field.value), color = primaryText, fontSize = 12.sp,
                    lineHeight = 17.sp, modifier = Modifier.weight(.57f))
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, message: String, action: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(panel).padding(20.dp)) {
        Text(localized(title), color = primaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text(localized(message), color = muted, fontSize = 13.sp, lineHeight = 20.sp)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = action, colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = accentText)) {
                Text(localized("Cho phép đọc thông số"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class StorageSlice(val label: String, val percent: Float, val color: Color)

@Composable
private fun StorageCard(total: Long, detail: List<Section>) {
    val fs = android.os.StatFs(android.os.Environment.getDataDirectory().path)
    val available = fs.availableBytes
    val used = (total - available).coerceAtLeast(0)
    val fraction = if (total > 0) used.toFloat() / total else 0f
    val fieldLabels = listOf("Mã ứng dụng", "Dữ liệu ứng dụng (không cache)", "Cache ứng dụng",
        "Chưa phân loại", "Trống dùng được")
    val detailed = detail.firstOrNull { it.title == "Phân loại dung lượng" }
    val colors = listOf(cyan, violet, Color(0xFFFFB454), Color(0xFFEF7186), muted)
    val parsed = fieldLabels.mapIndexed { index, label ->
        val percent = detailed?.fields?.firstOrNull { it.label == label }?.value
            ?.let { Regex("([0-9]+(?:\\.[0-9]+)?)%").find(it)?.groupValues?.get(1)?.toFloatOrNull() }
        percent?.let { StorageSlice(localized(label), it, colors[index]) }
    }
    val slices = if (parsed.all { it != null }) parsed.filterNotNull() else {
        val occupied = (total - fs.freeBytes).coerceAtLeast(0)
        val reserved = (fs.freeBytes - available).coerceAtLeast(0)
        fun percent(value: Long): Float = if (total > 0) value.toDouble().div(total).times(100.0).toFloat() else 0f
        listOf(
            StorageSlice(if (englishLanguage.value) "Occupied files" else "Tệp đang chiếm", percent(occupied), cyan),
            StorageSlice(if (englishLanguage.value) "Reserved free space" else "Trống dành riêng", percent(reserved), violet),
            StorageSlice(if (englishLanguage.value) "Available" else "Trống dùng được", percent(available), muted)
        )
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(20.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                val trackColor = border
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(trackColor, 0f, 360f, false,
                        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round))
                    var start = -90f
                    slices.forEach { slice ->
                        val sweep = 360f * (slice.percent / 100f).coerceIn(0f, 1f)
                        drawArc(slice.color, start, sweep, false,
                            style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round))
                        start += sweep
                    }
                }
                Text("${(fraction * 100).toInt()}%", color = primaryText,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text(if (englishLanguage.value) "INTERNAL STORAGE" else "BỘ NHỚ TRONG", color = muted,
                    fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Text("${(fraction * 100).toInt()}% ${if (englishLanguage.value) "used" else "đã dùng"}",
                    color = primaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${bytes(used)} / ${bytes(total)}", color = muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        slices.forEach { slice ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(slice.color))
                Spacer(Modifier.width(8.dp))
                Text(slice.label, color = muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("%.1f%%".format(java.util.Locale.US, slice.percent), color = primaryText,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(if (englishLanguage.value) "ZRAM is compressed RAM and is shown on the RAM page. Cache is separated when Usage Access is available."
            else "ZRAM là RAM nén và nằm ở trang RAM. Cache được tách riêng khi có quyền Usage Access.",
            color = muted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun BatteryDashboard(live: BatteryLive, values: List<Float>) {
    val max = (values.maxOfOrNull { kotlin.math.abs(it) } ?: 1f).coerceAtLeast(1f)
    val normalized = values.map { (kotlin.math.abs(it) / max).coerceIn(0f, 1f) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(20.dp)).padding(19.dp)) {
        Text(if (englishLanguage.value) "LIVE POWER" else "ĐIỆN NĂNG TRỰC TIẾP", color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(if (englishLanguage.value) "CURRENT" else "DÒNG", live.currentMa?.let { "%+.0f mA".format(java.util.Locale.US, it) } ?: "—", Modifier.weight(1f))
            Metric(if (englishLanguage.value) "POWER" else "CÔNG SUẤT", live.powerW?.let { "%+.2f W".format(java.util.Locale.US, it) } ?: "—", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        RamChart(normalized, 115)
        Text(if (englishLanguage.value) "|P| over 60 seconds · The +/- sign depends on device reporting" else "|P| trong 60 giây · Dấu +/− tùy cách thiết bị báo dòng sạc/xả", color = muted, fontSize = 11.sp)
    }
}

@Composable
private fun SensorCard(section: Section) {
    val type = section.fields.firstOrNull { it.label == "Sensor Type" }?.value?.lowercase() ?: ""
    val sensorId = section.fields.firstOrNull { it.label == "Sensor ID" }?.value ?: section.title
    var expanded by rememberSaveable(sensorId) { mutableStateOf(false) }
    val basicLabels = setOf("Sensor Type", "Vendor / OEM", "Resolution", "Power")
    val icon = when {
        "accelerometer" in type -> Icons.Default.Speed
        "gyroscope" in type -> Icons.Default.Sensors
        "light" in type -> Icons.Default.LightMode
        "proximity" in type -> Icons.Default.Visibility
        "pressure" in type -> Icons.Default.Air
        "magnetic" in type -> Icons.Default.Explore
        "step" in type -> Icons.Default.DirectionsWalk
        "heart" in type -> Icons.Default.Favorite
        "temperature" in type -> Icons.Default.Thermostat
        else -> Icons.Default.Sensors
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(20.dp)).padding(19.dp)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(cyan.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                AssetIcon(IconAssets.sensor(type), Modifier.size(IconSizes.sensorIcon), section.title) {
                    Icon(icon, contentDescription = null, tint = cyan, modifier = Modifier.size(IconSizes.sensorIcon))
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(localized(section.title), color = primaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "Sensor arrow")
            Icon(Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = cyan, modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = arrow })
        }
        Spacer(Modifier.height(10.dp))
        section.fields.filter { it.label in basicLabels }.forEach { field ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(localized(field.label), color = muted, fontSize = 12.sp, modifier = Modifier.weight(.43f))
                Text(localized(field.value), color = primaryText, fontSize = 12.sp, modifier = Modifier.weight(.57f))
            }
        }
        AnimatedVisibility(expanded) {
            Column {
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(border))
                section.fields.filterNot { it.label in basicLabels }.forEach { field ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(localized(field.label), color = muted, fontSize = 12.sp,
                            modifier = Modifier.weight(.43f))
                        Text(localized(field.value), color = primaryText, fontSize = 12.sp,
                            modifier = Modifier.weight(.57f))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoDrawer(onClose: () -> Unit) {
    Column(Modifier.width(292.dp).fillMaxHeight().background(panel).padding(20.dp)) {
        Text("DeviceIn4", color = cyan, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Version 1.0", color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(30.dp))
        Text(if (englishLanguage.value) "About" else "Về ứng dụng", color = primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(if (englishLanguage.value) "Explore device hardware, Android, sensors and live resources." else "Khám phá phần cứng, Android, cảm biến và tài nguyên trực tiếp của thiết bị.", color = muted, fontSize = 13.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(24.dp))
        Text(if (englishLanguage.value) "Privacy" else "Quyền riêng tư", color = primaryText, fontWeight = FontWeight.Bold)
        Text(if (englishLanguage.value) "Device data is read locally. No account or cloud sync." else "Thông số được đọc trên thiết bị. Không yêu cầu tài khoản hay đồng bộ đám mây.", color = muted, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(24.dp))
        Text("© 2026 DeviceIn4", color = muted, fontSize = 12.sp)
        Text(if (englishLanguage.value) "Built with Kotlin, Jetpack Compose and Android APIs. Third-party licenses follow their respective packages." else "Xây dựng bằng Kotlin, Jetpack Compose và Android API. Bản quyền thư viện theo từng gói tương ứng.", color = muted, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(24.dp))
        ChoiceChip(if (englishLanguage.value) "Close" else "Đóng", false, onClose)
    }
}

@Composable
private fun SettingsContent(theme: String, language: String, units: String, temperature: String,
                            accent: String, widgetMetric: String, onTheme: (String) -> Unit,
                            onLanguage: (String) -> Unit, onUnits: (String) -> Unit,
                            onTemperature: (String) -> Unit, onAccent: (String) -> Unit,
                            onMetric: (String) -> Unit, onPin: () -> Unit, onClose: () -> Unit) {
    var editedAccent by remember(accent) { mutableStateOf(accent) }
    Column(Modifier.width(318.dp).fillMaxHeight().background(panel).verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (englishLanguage.value) "Settings" else "Cài đặt", color = primaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            ChoiceChip("✕", false, onClose)
        }
        Spacer(Modifier.height(24.dp))
        Text(if (englishLanguage.value) "Language" else "Ngôn ngữ", color = primaryText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LanguageDropdown(language, onLanguage)
        Spacer(Modifier.height(22.dp))
        Text(if (englishLanguage.value) "Appearance" else "Giao diện", color = primaryText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("system" to "System", "dark" to "Dark", "light" to "Light").forEach { (key, label) -> ChoiceChip(label, theme == key) { onTheme(key) } }
        }
        Spacer(Modifier.height(14.dp))
        Text(if (englishLanguage.value) "Accent color" else "Màu chủ đạo", color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        AccentDropdown(accent, onAccent)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = editedAccent, onValueChange = { editedAccent = it.take(7) }, label = { Text("#RRGGBB") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onAccent(editedAccent) }, enabled = Regex("#[0-9A-Fa-f]{6}").matches(editedAccent), colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = accentText)) {
            Text(if (englishLanguage.value) "Apply color" else "Áp dụng màu")
        }
        Spacer(Modifier.height(18.dp))
        Text(if (englishLanguage.value) "Units" else "Đơn vị đo", color = primaryText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ChoiceChip("GB / MB", units == "decimal") { onUnits("decimal") }
            ChoiceChip("GiB / MiB", units == "binary") { onUnits("binary") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ChoiceChip("°C", temperature == "c") { onTemperature("c") }
            ChoiceChip("°F", temperature == "f") { onTemperature("f") }
        }
        Spacer(Modifier.height(22.dp))
        Text(if (englishLanguage.value) "Home screen widget" else "Widget màn hình chính", color = primaryText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("device" to "Device", "ram" to "RAM", "storage" to "Storage", "battery" to (if (englishLanguage.value) "Battery" else "Pin")).forEach { (key, label) -> ChoiceChip(label, widgetMetric == key) { onMetric(key) } }
        }
        Spacer(Modifier.height(13.dp))
        Button(onClick = onPin, colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = accentText)) { Text(if (englishLanguage.value) "Add widget" else "Thêm widget") }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun LanguageDropdown(language: String, onLanguage: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "system" to (if (englishLanguage.value) "🌐 System language" else "🌐 Theo ngôn ngữ hệ thống"),
        "vi" to "🇻🇳 Việt Nam · Tiếng Việt",
        "en" to "🇺🇸 United States · English"
    )
    Box(Modifier.fillMaxWidth()) {
        Surface(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), color = ink,
            shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, border)) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(options.firstOrNull { it.first == language }?.second ?: options.first().second,
                    color = primaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("▾", color = muted, fontSize = 17.sp)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = panel,
            modifier = Modifier.width(280.dp)) {
            options.forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label, color = primaryText, fontSize = 13.sp) },
                    onClick = { expanded = false; onLanguage(key) })
            }
        }
    }
}

@Composable
private fun AccentDropdown(accent: String, onAccent: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val defaultHex = if (lightTheme.value) "#087E82" else "#5FE1D5"
    val options = listOf("" to (if (englishLanguage.value) "Default" else "Mặc định"),
        "#5FE1D5" to "Mint", "#A78BFA" to "Violet", "#FFAB64" to "Amber",
        "#F46B9B" to "Rose", "#5EA6FF" to "Blue")
    val label = options.firstOrNull { it.first.equals(accent, ignoreCase = true) }?.second
        ?: (if (englishLanguage.value) "Custom" else "Tùy chỉnh")
    val shownHex = accent.ifBlank { defaultHex }
    val swatch = try { Color(android.graphics.Color.parseColor(shownHex)) } catch (_: Exception) { cyan }
    Box(Modifier.fillMaxWidth()) {
        Surface(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), color = ink,
            shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, border)) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(18.dp).clip(CircleShape).background(swatch))
                Spacer(Modifier.width(10.dp))
                Text("$label · $shownHex", color = primaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("▾", color = muted, fontSize = 17.sp)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = panel,
            modifier = Modifier.width(280.dp)) {
            options.forEach { (hex, name) ->
                val code = hex.ifBlank { defaultHex }
                DropdownMenuItem(text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(18.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(code))))
                        Spacer(Modifier.width(10.dp))
                        Text("$name · $code", color = primaryText, fontSize = 13.sp)
                    }
                }, onClick = { expanded = false; onAccent(hex) })
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, color = if (selected) cyan else ink, shape = RoundedCornerShape(50),
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, border)) {
        Text(label, color = if (selected) accentText else primaryText,
            fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
    }
}
