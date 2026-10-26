package com.devicein4.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import java.util.Locale
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
import com.devicein4.app.LocalizedText as Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.tooling.preview.Preview
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

private fun localized(text: String): String = LocalizationManager.localize(text)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguages.migrate(this)
        super.onCreate(savedInstanceState)
        setContent { DeviceApp() }
    }
}

internal fun requestDevicePermission(context: android.content.Context, permission: String, request: () -> Unit) {
    val activity = context as? Activity ?: return
    val preferences = context.getSharedPreferences("permission_requests", android.content.Context.MODE_PRIVATE)
    if (preferences.getBoolean(permission, false) &&
        !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:${context.packageName}")))
    } else {
        preferences.edit().putBoolean(permission, true).apply()
        request()
    }
}

private fun openAppDetails(context: android.content.Context, packageName: String) {
    try {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", packageName, null)))
    } catch (_: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, localized(if (englishLanguage.value) "App details unavailable" else "Không mở được thông tin ứng dụng"), android.widget.Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        android.widget.Toast.makeText(context, localized(if (englishLanguage.value) "Android restricted app settings" else "Android giới hạn truy cập cài đặt ứng dụng"), android.widget.Toast.LENGTH_SHORT).show()
    }
}

private val tabs = listOf("Tổng quan", "Device", "OS", "RAM", "Storage", "Display", "Battery", "Network", "Camera", "Sensor", "Features")

private fun openUsageAccessSettings(context: android.content.Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS,
            android.net.Uri.parse("package:${context.packageName}")))
    } catch (_: android.content.ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}

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
    var languageChoice by remember { mutableStateOf(AppLanguages.selection(context)) }
    var unitChoice by remember { mutableStateOf(Preferences.units(context)) }
    var temperatureChoice by remember { mutableStateOf(Preferences.temperature(context)) }
    var accentHex by remember { mutableStateOf(Preferences.accent(context)) }
    var showInfo by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()
    lightTheme.value = when (themeChoice) { "light" -> true; "dark" -> false; else -> !systemDark }
    FormatOptions.binary.value = unitChoice == "binary"
    FormatOptions.fahrenheit.value = temperatureChoice == "f"
    LocalizationManager.configure(context)
    englishLanguage.value = LocalizationManager.isEnglish
    customAccent.value = try { Color(android.graphics.Color.parseColor(accentHex)) } catch (_: Exception) { null }
    var foreground by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var sample by remember { mutableStateOf(repo.memory()) }
    var history by remember { mutableStateOf(listOf(sample.percent)) }
    var cores by remember { mutableStateOf(repo.cpuLive()) }
    var coreHistories by remember { mutableStateOf<Map<Int, List<Float>>>(emptyMap()) }
    var wifiLive by remember { mutableStateOf(repo.wifiLive()) }
    var batteryLive by remember { mutableStateOf(repo.batteryLive()) }
    var powerHistory by remember { mutableStateOf(listOf(0f)) }
    var cpuHistory by remember { mutableStateOf(listOf(0f)) }
    var showCpuDialog by remember { mutableStateOf(false) }
    var showCpuDeepAnalysis by remember { mutableStateOf(false) }
    var showGpuDeepAnalysis by remember { mutableStateOf(false) }
    var battery by remember { mutableStateOf(repo.battery()) }
    var cameraGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var locationGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    var phoneGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) }
    var storageTotal by remember { mutableStateOf(safeTotalBytes()) }
    var detailByPage by remember { mutableStateOf<Map<Int, List<Section>>>(emptyMap()) }
    var storageAppsExpanded by rememberSaveable { mutableStateOf(false) }
    val detailKeysByPage = remember { mutableMapOf<Int, List<Any>>() }
    var errorByPage by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var accessRevision by remember { mutableStateOf(0) }
    var usageGranted by remember { mutableStateOf(advanced.hasUsageAccess()) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraGranted = it }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val phonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { phoneGranted = it }

    LaunchedEffect(lightTheme.value) {
        val activity = context as? Activity ?: return@LaunchedEffect
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = if (lightTheme.value) android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
        activity.window.navigationBarColor = if (lightTheme.value) android.graphics.Color.rgb(244, 247, 252) else android.graphics.Color.rgb(11, 16, 32)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (event == Lifecycle.Event.ON_RESUME) {
                cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                phoneGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                usageGranted = advanced.hasUsageAccess()
                detailByPage = emptyMap()
                detailKeysByPage.clear()
                errorByPage = emptyMap()
                accessRevision++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(advanced, lifecycleOwner, phoneGranted, pagerState.settledPage) {
        var stop: (() -> Unit)? = null
        fun update() {
            stop?.invoke()
            stop = null
            if (phoneGranted && pagerState.settledPage == 7 && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                stop = advanced.observeMobileNetwork()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_PAUSE) update()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        update()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop?.invoke()
        }
    }

    LaunchedEffect(foreground, locationGranted, phoneGranted, usageGranted) {
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
                try {
                    val network = withContext(Dispatchers.IO) { advanced.network(locationGranted, phoneGranted) }
                    detailByPage = detailByPage + (7 to network)
                    errorByPage = errorByPage - 7
                }
                catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    errorByPage = errorByPage + (7 to "Lỗi Network: ${e.javaClass.simpleName}")
                }
            }
            if (pagerState.currentPage == 9 && ticks % 5 == 0) {
                try {
                    val sensors = withContext(Dispatchers.IO) { advanced.sensors() }
                    detailByPage = detailByPage + (9 to sensors)
                    errorByPage = errorByPage - 9
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    errorByPage = errorByPage + (9 to "Lỗi Sensor: ${e.javaClass.simpleName}")
                }
            }
            ticks++
            delay(1000)
        }
    }

    LaunchedEffect(foreground, pagerState.currentPage, showCpuDialog, showCpuDeepAnalysis) {
        if (!foreground) return@LaunchedEffect
        while (true) {
            val liveCores = withContext(Dispatchers.IO) { repo.cpuLive() }
            cores = liveCores
            val avg = if (liveCores.isNotEmpty()) liveCores.map { it.fraction }.average().toFloat() else 0f
            cpuHistory = (cpuHistory + avg).takeLast(60)

            val updated = coreHistories.toMutableMap()
            for (core in liveCores) {
                val list = updated[core.index] ?: emptyList()
                updated[core.index] = (list + core.fraction).takeLast(25)
            }
            coreHistories = updated

            delay(350)
        }
    }

    LaunchedEffect(pagerState.targetPage, cameraGranted, locationGranted, phoneGranted, usageGranted, accessRevision, unitChoice, temperatureChoice) {
        val requestedPage = pagerState.targetPage
        val dataKey = listOf<Any>(cameraGranted, locationGranted, phoneGranted, usageGranted, accessRevision, unitChoice, temperatureChoice)
        if (requestedPage !in listOf(4, 7, 9) && requestedPage in detailByPage && detailKeysByPage[requestedPage] == dataKey) return@LaunchedEffect
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
            detailKeysByPage[requestedPage] = dataKey
            errorByPage = errorByPage - requestedPage
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
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
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
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
                            3 -> PageIntro("RAM trực tiếp", "Lấy mẫu mỗi giây khi ứng dụng đang mở")
                            4 -> PageIntro("Storage", "Bộ nhớ trong, ngoài và dung lượng còn lại")
                            5 -> PageIntro("Display", "Độ phân giải, tần số quét và HDR",
                                displayManufacturer(context, detail))
                            6 -> PageIntro("Battery", "Dòng điện và công suất trực tiếp",
                                localized(if (englishLanguage.value) "Battery cell manufacturer: Not exposed by Android" else "Nhà sản xuất cell pin: Android không công khai"), batteryLive.level)
                            7 -> PageIntro("Network", "Kết nối và dữ liệu sử dụng")
                            8 -> PageIntro("Camera", "Ống kính, cảm biến và khả năng chụp",
                                localized(if (englishLanguage.value) "Camera module manufacturer: Not exposed by Camera2" else "Hãng module camera: Camera2 không công khai"))
                            9 -> PageIntro("Sensor", "Danh sách cảm biến Android công khai")
                            10 -> PageIntro("Features", "Tính năng phần cứng và hệ thống")
                        }
                    }
                    if (page == 0 || page == 3) {
                        item { MemoryCard(sample, history, expanded = page == 3, storageTotal = storageTotal, storageDetail = detailByPage[4].orEmpty()) }
                    }
                    if (page == 0) item {
                        CpuActivityCard(
                            cores = cores,
                            cpuHistory = cpuHistory,
                            onClick = { showCpuDialog = true }
                        )
                    }
                    if (page == 0) item { QuickStats(batteryLive, wifiLive) }
                    if (page == 4) item { StorageCard(storageTotal, detail) }
                    if (page == 4 && !usageGranted && Build.VERSION.SDK_INT >= 26) item {
                        InfoCard("Dung lượng từng ứng dụng", "Bật Usage Access để xem mã, dữ liệu và cache của các ứng dụng Android cho phép nhìn thấy.") {
                            openUsageAccessSettings(context)
                        }
                    }
                    if (page == 6) item { BatteryDashboard(batteryLive, powerHistory) }
                    if (page == 6) item { SectionCard(battery) }
                    if (page == 7 && !locationGranted) item {
                        InfoCard("Wi-Fi SSID", "Cho phép vị trí để Android cung cấp tên mạng Wi-Fi hiện tại.") {
                            requestDevicePermission(context, Manifest.permission.ACCESS_FINE_LOCATION) {
                                locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        }
                    }
                    if (page == 7 && !phoneGranted) item {
                        InfoCard("Mạng di động", "Cho phép trạng thái điện thoại để xem loại mạng 2G/3G/4G/5G.") {
                            requestDevicePermission(context, Manifest.permission.READ_PHONE_STATE) { phonePermission.launch(Manifest.permission.READ_PHONE_STATE) }
                        }
                    }
                    if (page == 7 && !usageGranted) item {
                        InfoCard("Thống kê theo tháng", "Bật Usage Access để đọc tổng dữ liệu Wi-Fi và di động trong tháng này.") {
                            openUsageAccessSettings(context)
                        }
                    }
                    if (page == 8 && !cameraGranted) item {
                        InfoCard("Cần quyền camera", "Android yêu cầu quyền camera để đọc đầy đủ đặc tính ống kính. Ứng dụng chỉ đọc thông số, không chụp ảnh.") {
                            requestDevicePermission(context, Manifest.permission.CAMERA) { cameraPermission.launch(Manifest.permission.CAMERA) }
                        }
                    }
                    if (detailError != null) item { InfoCard("Không thể đọc dữ liệu", detailError!!) }
                    if (page == 8 && page in detailByPage && detail.isEmpty() && detailError == null) item {
                        InfoCard("Chưa có camera", "Thiết bị không công khai camera qua Camera2.")
                    }
                    if (page == 9 && page in detailByPage && detail.isEmpty() && detailError == null) item {
                        InfoCard("Chưa có cảm biến", "Thiết bị không công khai cảm biến qua Android SensorManager.")
                    }
                    if (page !in detailByPage && detailError == null && page !in listOf(0, 6)) item {
                        InfoCard("Đang đọc dữ liệu", "Đợi thiết bị trả về thông số của trang này…")
                    }
                    items(if (page == 0) emptyList() else if (page == 4) detail.filterNot {
                        it.title == "Ứng dụng dùng nhiều dung lượng" || it.fields.any { field -> field.label == "Package" }
                    } else detail) { section ->
                        when {
                            page == 3 && section.title == "Phần cứng & Công nghệ RAM" -> SectionCard(section.copy(
                                title = "Chi tiết & phần cứng RAM",
                                fields = section.fields + listOf(
                                    Field("RAM hệ thống", bytes(sample.total)),
                                    Field("RAM đang sử dụng", bytes(sample.used)),
                                    Field("RAM khả dụng", bytes(sample.available)),
                                    Field("Ngưỡng thiếu RAM", bytes(sample.threshold)),
                                    Field("Bộ nhớ heap của app", bytes(sample.appUsed)),
                                    Field("RAM quảng cáo", sample.advertised?.let(::bytes) ?: "Không công khai"),
                                    Field("Áp lực bộ nhớ", if (sample.low) "Hệ thống báo thiếu RAM" else "Bình thường")
                                )
                            ))
                            page == 9 && section.title == "Thống kê cảm biến" -> SectionCard(section)
                            page == 9 -> SensorCard(section, pagerState.settledPage == 9 && foreground)
                            page == 8 -> CameraCard(section)
                            page == 1 && section.title in listOf("CPU · cấu trúc", "GPU & đồ họa") -> {
                                val isCpu = section.title.startsWith("CPU")
                                AnalysisCard(
                                    section = section,
                                    onDeepAnalysis = {
                                        if (isCpu) showCpuDeepAnalysis = true else showGpuDeepAnalysis = true
                                    }
                                )
                            }
                            else -> SectionCard(section)
                        }
                    }
                    if (page == 4 && detail.any { it.title == "Ứng dụng dùng nhiều dung lượng" }) {
                        val apps = detail.filter { it.fields.any { field -> field.label == "Package" } }
                        item(key = "storage-apps-selector") {
                            StorageAppsSelector(apps.size, storageAppsExpanded,
                                detail.first { it.title == "Ứng dụng dùng nhiều dung lượng" }.fields.firstOrNull()?.value.orEmpty()) {
                                storageAppsExpanded = !storageAppsExpanded
                            }
                        }
                        if (storageAppsExpanded) items(apps, key = { app -> "storage-app:" + app.fields.first { it.label == "Package" }.value }) { app ->
                            StorageAppRow(app)
                        }
                    }
                    // Keep the footer's scroll extent stable, including while the pager settles.
                    // Removing this item during a gesture makes the list clamp its scroll position.
                    item(key = "cpu-playground") {
                        Box(Modifier.fillMaxWidth().height(106.dp)) {
                            if ((page in detailByPage || page in listOf(0, 6)) &&
                                detailError == null && pagerState.settledPage == page &&
                                !pagerState.isScrollInProgress) {
                                CpuPlayground(
                                    active = foreground &&
                                        pageListState.layoutInfo.visibleItemsInfo.any { it.key == "cpu-playground" },
                                    dark = !lightTheme.value
                                )
                            }
                        }
                    }
                }
                }
            }
            if (showInfo || showSettings) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)).clickable { showInfo = false; showSettings = false })
            AnimatedVisibility(showInfo, enter = slideInHorizontally { -it }, exit = slideOutHorizontally { -it }, modifier = Modifier.align(Alignment.CenterStart)) {
                InfoDrawer { showInfo = false }
            }
            AnimatedVisibility(showSettings, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }, modifier = Modifier.align(Alignment.CenterEnd)) {
                SettingsContent(themeChoice, languageChoice, unitChoice, temperatureChoice, accentHex,
                    onTheme = { themeChoice = it; Preferences.setTheme(context, it) },
                    onLanguage = { languageChoice = it; Preferences.setLanguage(context, it); AppLanguages.select(it) },
                    onUnits = { unitChoice = it; Preferences.setUnits(context, it); detailByPage = emptyMap() },
                    onTemperature = { temperatureChoice = it; Preferences.setTemperature(context, it); detailByPage = emptyMap() },
                    onAccent = { accentHex = it; Preferences.setAccent(context, it) },
                    onClose = { showSettings = false })
            }
            if (showCpuDialog) CpuDetailDialog(
                cores = cores,
                history = cpuHistory,
                coreHistories = coreHistories,
                onOpenDeepAnalysis = {
                    showCpuDialog = false
                    showCpuDeepAnalysis = true
                },
                onClose = { showCpuDialog = false }
            )
            if (showCpuDeepAnalysis) CpuInDepthDialog(
                cores = cores,
                cpuHistory = cpuHistory,
                coreHistories = coreHistories,
                advanced = advanced,
                onClose = { showCpuDeepAnalysis = false }
            )
            if (showGpuDeepAnalysis) GpuInDepthDialog(
                advanced = advanced,
                onClose = { showGpuDeepAnalysis = false }
            )
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
                Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_devicein4), contentDescription = "DeviceIn4",
                    tint = Color.Unspecified, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text("Device Info", color = primaryText, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(localized(if (englishLanguage.value) "Device intelligence" else "Thông tin thiết bị"), color = cyan, fontSize = 9.sp)
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
    val uptimeText = LocalContext.current.getString(R.string.duration_format,
        uptime / 86_400, uptime % 86_400 / 3_600, uptime % 3_600 / 60)
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
            Text("DEVICE INFO  /  ${localized(if (englishLanguage.value) "YOUR DEVICE" else "THIẾT BỊ CỦA BẠN")}",
                color = cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
            Spacer(Modifier.height(10.dp))
            Text("${Build.MANUFACTURER} ${Build.MODEL}", color = primaryText,
                fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Text("Android ${Build.VERSION.RELEASE} · ${androidCodename()}", color = primaryText,
                fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Text("${localized("Thời gian hoạt động")} · $uptimeText",
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
    Box(
        Modifier.width(width).height(height),
        contentAlignment = Alignment.Center
    ) {
        AssetIcon(imageKey, Modifier.fillMaxSize().padding(imagePadding), brand) {
            Text(
                mark,
                color = primaryText,
                fontSize = if (mark.length > 4) 15.sp else 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
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
private fun CoreLiveChart(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) {
            drawLine(color.copy(alpha = 0.2f), Offset(0f, size.height * 0.8f), Offset(size.width, size.height * 0.8f), strokeWidth = 1.dp.toPx())
            return@Canvas
        }
        val shown = values.takeLast(25)
        val points = shown.mapIndexed { index, value ->
            Offset(
                if (shown.size == 1) size.width else size.width * index / (shown.size - 1),
                size.height * (1f - value.coerceIn(0.05f, 0.95f))
            )
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
        drawPath(fill, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0.02f))))
        drawPath(line, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color, 2.5.dp.toPx(), points.last())
    }
}

@Composable
private fun CpuActivityCard(
    cores: List<CpuCore>,
    cpuHistory: List<Float>,
    onClick: () -> Unit
) {
    val avgFraction = if (cores.isNotEmpty()) cores.map { it.fraction }.average().toFloat() else 0f
    val peakFraction = (cpuHistory.maxOrNull() ?: avgFraction).coerceAtLeast(avgFraction)
    val activeCores = cores.count { (it.currentKhz ?: 0) > 0 }
    val maxGhz = cores.mapNotNull { it.currentKhz }.maxOrNull()?.let { it / 1_000_000.0 } ?: 0.0

    val (statusLabel, statusColor) = when {
        avgFraction < 0.25f -> (localized(if (englishLanguage.value) "Power Saving / Idle" else "Rảnh rỗi / Tiết kiệm điện")) to Color(0xFF5FE1D5)
        avgFraction <= 0.65f -> (localized(if (englishLanguage.value) "Balanced / Routine" else "Cân bằng / Tác vụ thường")) to Color(0xFFFFB454)
        else -> (localized(if (englishLanguage.value) "High Performance / Peak Load" else "Tải cao / Hiệu năng đỉnh")) to Color(0xFFEF7186)
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(panel)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                localized(if (englishLanguage.value) "LIVE CPU LOAD" else "TẢI CPU TRỰC TIẾP"),
                color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            // State pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(6.dp))
                Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Metrics row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric(localized(if (englishLanguage.value) "CURRENT" else "TẢI HIỆN TẠI"), "${(avgFraction * 100).toInt()}%", Modifier.weight(1f))
            Metric(localized(if (englishLanguage.value) "PEAK (60S)" else "TẢI ĐỈNH (60S)"), "${(peakFraction * 100).toInt()}%", Modifier.weight(1f))
            Metric(localized(if (englishLanguage.value) "MAX FREQ" else "XUNG CAO NHẤT"), if (maxGhz > 0) "%.2f GHz".format(Locale.US, maxGhz) else "—", Modifier.weight(1f))
            Metric(localized(if (englishLanguage.value) "ACTIVE" else "LÕI THỨC"), "$activeCores/${cores.size}", Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))

        // Live wave chart (matching Battery/RAM dashboard)
        RamChart(cpuHistory, 95)

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                localized(if (englishLanguage.value) "Real-time CPU load history over 60 seconds" else "Biểu đồ tải CPU thời gian thực 60 giây gần nhất"),
                color = muted, fontSize = 10.sp, modifier = Modifier.weight(1f)
            )
            Text(
                localized(if (englishLanguage.value) "Core details & /proc/cpuinfo ↗" else "Chi tiết từng lõi & /proc/cpuinfo ↗"),
                color = cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CpuStatusHeader(labels: List<String>, descriptions: List<String>, label: String, color: Color, description: String, average: Float) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Bold)
    val descriptionStyle = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.sp)
    val averageLabel = localized(if (englishLanguage.value) "avg load" else "tải trung bình")
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val averageWidth = with(density) {
            measurer.measure(averageLabel, TextStyle(fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.sp)).size.width.toDp()
        }.coerceAtLeast(72.dp).coerceAtMost(maxWidth * .4f)
        val labelWidth = (maxWidth - averageWidth - 30.dp).coerceAtLeast(1.dp)
        // Reserve the largest label and description at the actual width and font scale.
        // CPU sampling changes text, never the height of this block or the load column.
        val heights = remember(labels, descriptions, maxWidth, density, averageWidth) {
            with(density) {
                val headerHeight = labels.maxOf { measurer.measure(it, labelStyle,
                    constraints = Constraints(maxWidth = labelWidth.roundToPx().coerceAtLeast(1))).size.height }.toDp().coerceAtLeast(36.dp)
                val bodyHeight = descriptions.maxOf { measurer.measure(it, descriptionStyle,
                    constraints = Constraints(maxWidth = maxWidth.roundToPx().coerceAtLeast(1))).size.height }.toDp()
                headerHeight to bodyHeight
            }
        }
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(8.dp))
                Text(label, style = labelStyle, color = color, modifier = Modifier.weight(1f).height(heights.first))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.width(averageWidth), horizontalAlignment = Alignment.End) {
                    Text("${(average * 100).toInt()}%", color = primaryText, fontSize = 18.sp, lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, style = TextStyle(fontFeatureSettings = "tnum"))
                    Text(averageLabel, color = muted, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.sp, textAlign = TextAlign.End)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(description, style = descriptionStyle, color = primaryText.copy(alpha = .9f), modifier = Modifier.height(heights.second))
        }
    }
}

@Composable
private fun CpuDetailDialog(
    cores: List<CpuCore>,
    history: List<Float>,
    coreHistories: Map<Int, List<Float>>,
    onOpenDeepAnalysis: () -> Unit,
    onClose: () -> Unit
) {
    val avgFraction = if (cores.isNotEmpty()) cores.map { it.fraction }.average().toFloat() else 0f
    val peakFraction = (history.maxOrNull() ?: avgFraction).coerceAtLeast(avgFraction)
    val activeCores = cores.count { (it.currentKhz ?: 0) > 0 }
    val maxGhz = cores.mapNotNull { it.currentKhz }.maxOrNull()?.let { it / 1_000_000.0 } ?: 0.0

    val statusOptions = listOf(
        Triple(
            localized(if (englishLanguage.value) "Power Saving / Idle" else "Rảnh rỗi / Tiết kiệm điện"),
            Color(0xFF5FE1D5),
            localized(if (englishLanguage.value) "CPU operating at low power state. Background tasks are handled by efficient little cores. Temperature and battery drain are minimal." else "CPU đang ở mức tiêu thụ điện năng tối thiểu. Các luồng nền được xử lý nhẹ nhàng bởi cụm nhân tiết kiệm điện, nhiệt độ mát mẻ.")
        ),
        Triple(
            localized(if (englishLanguage.value) "Balanced / Daily Activity" else "Cân bằng / Tác vụ thường nhật"),
            Color(0xFFFFB454),
            localized(if (englishLanguage.value) "CPU handling UI, streaming or routine multitasking smoothly with balanced clock distribution." else "CPU đang xử lý giao diện, đa nhiệm hoặc xem video ổn định. Xung nhịp các lõi được điều phối hài hòa giữa tiết kiệm điện và hiệu năng.")
        ),
        Triple(
            localized(if (englishLanguage.value) "High Performance / Heavy Load" else "Tải cao / Hiệu năng tối đa"),
            Color(0xFFEF7186),
            localized(if (englishLanguage.value) "High CPU load active. Big cores boosted for heavy processing, 3D rendering or gaming." else "CPU đang chạy công suất cao. Các nhân hiệu năng (Big Cores) được đẩy xung nhịp tối đa để xử lý đồ họa, chơi game hoặc tính toán nặng.")
        )
    )
    val (statusLabel, statusColor, statusDesc) = statusOptions[when {
        avgFraction < .25f -> 0
        avgFraction <= .65f -> 1
        else -> 2
    }]

    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(panel)
                .border(1.dp, border, RoundedCornerShape(24.dp)).padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(localized(if (englishLanguage.value) "CPU SYNTHESIS & ANALYSIS" else "PHÂN TÍCH TỔNG HỢP CPU"), color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(localized(if (englishLanguage.value) "Core Performance & Status" else "Hiệu suất các lõi & Trạng thái"), color = primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                ChoiceChip("✕", false, onClose)
            }

            Spacer(Modifier.height(16.dp))

            // Synthesis card
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(statusColor.copy(alpha = .12f)).border(1.dp, statusColor.copy(alpha = .35f), RoundedCornerShape(16.dp)).padding(14.dp)) {
                CpuStatusHeader(statusOptions.map { it.first }, statusOptions.map { it.third },
                    statusLabel, statusColor, statusDesc, avgFraction)

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(ink.copy(alpha = 0.6f)).padding(8.dp)) {
                        Text(localized(if (englishLanguage.value) "AVG LOAD" else "TẢI TB"), color = muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${(avgFraction * 100).toInt()}%", color = primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(ink.copy(alpha = 0.6f)).padding(8.dp)) {
                        Text(localized(if (englishLanguage.value) "PEAK LOAD" else "TẢI ĐỈNH"), color = muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${(peakFraction * 100).toInt()}%", color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(ink.copy(alpha = 0.6f)).padding(8.dp)) {
                        Text(localized(if (englishLanguage.value) "ACTIVE" else "LÕI THỨC"), color = muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("$activeCores/${cores.size}", color = primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(localized(if (englishLanguage.value) "CORE FREQUENCY & PERFORMANCE TABLE" else "BẢNG HIỆU SUẤT CHI TIẾT TỪNG LÕI"), color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))

            cores.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { core ->
                        val loadColor = when {
                            core.fraction < 0.35f -> cyan
                            core.fraction <= 0.70f -> Color(0xFFFFB454)
                            else -> Color(0xFFEF7186)
                        }
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(ink).padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(core.name, color = primaryText, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("${(core.fraction * 100).toInt()}%", color = loadColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(core.cluster, color = muted, fontSize = 9.sp)
                            Spacer(Modifier.height(4.dp))
                            val coreHistory = coreHistories[core.index].orEmpty().ifEmpty { listOf(core.fraction) }
                            CoreLiveChart(values = coreHistory, color = loadColor, modifier = Modifier.fillMaxWidth().height(22.dp))
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    core.currentKhz?.let { "%.2f GHz".format(java.util.Locale.US, it / 1_000_000.0) } ?: "—",
                                    color = primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    core.maxKhz?.let { "max %.2f".format(java.util.Locale.US, it / 1_000_000.0) } ?: "",
                                    color = muted, fontSize = 9.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Canvas(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))) {
                                drawRect(border)
                                drawRect(loadColor, size = Size(size.width * core.fraction, size.height))
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(18.dp))

            // Action button to open deep analysis
            Button(
                onClick = onOpenDeepAnalysis,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = cyan.copy(alpha = 0.2f), contentColor = cyan),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    localized(if (englishLanguage.value) "In-Depth CPU & /proc/cpuinfo Analysis ↗" else "Phân tích chuyên sâu CPU & Xem /proc/cpuinfo ↗"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(10.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = accentText), shape = RoundedCornerShape(14.dp)) {
                Text(localized(if (englishLanguage.value) "Close" else "Đóng bảng phân tích"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CpuInDepthDialog(
    cores: List<CpuCore>,
    cpuHistory: List<Float>,
    coreHistories: Map<Int, List<Float>>,
    advanced: AdvancedRepository,
    onClose: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Hiệu suất lõi, 1: /proc/cpuinfo
    val clipboard = LocalClipboardManager.current
    var copiedMessage by remember { mutableStateOf(false) }

    val cpuDetail = remember { advanced.cpuDetailedAnalysis() }

    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f).clip(RoundedCornerShape(24.dp)).background(panel)
                .border(1.dp, border, RoundedCornerShape(24.dp)).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(localized(if (englishLanguage.value) "IN-DEPTH CPU ANALYSIS" else "PHÂN TÍCH CHUYÊN SÂU CPU"), color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(localized(if (englishLanguage.value) "Performance & /proc/cpuinfo" else "Hiệu suất các core & /proc/cpuinfo"), color = primaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                ChoiceChip("✕", false, onClose)
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(localized(if (englishLanguage.value) "Core Performance (Live)" else "Hiệu suất các core (Live)"), selectedTab == 0) { selectedTab = 0 }
                ChoiceChip(localized(if (englishLanguage.value) "proc.cpuinfo (${cpuDetail.processors.size} cores)" else "proc.cpuinfo (${cpuDetail.processors.size} nhân)"), selectedTab == 1) { selectedTab = 1 }
            }

            Spacer(Modifier.height(14.dp))

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedTab == 0) {
                    // Live 60s load trend chart
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ink).border(1.dp, border, RoundedCornerShape(16.dp)).padding(14.dp)) {
                        Text(localized(if (englishLanguage.value) "LIVE CPU LOAD (60 SECONDS)" else "TẢI CPU TRỰC TIẾP (60 GIÂY)"), color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        RamChart(cpuHistory, 105)
                    }

                    // Cores Table with Live Charts
                    Text(localized(if (englishLanguage.value) "REAL-TIME LOGICAL CORE METRICS" else "THÔNG SỐ CHI TIẾT TỪNG NHÂN LOGIC"), color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                    cores.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { core ->
                                val loadColor = when {
                                    core.fraction < 0.35f -> cyan
                                    core.fraction <= 0.70f -> Color(0xFFFFB454)
                                    else -> Color(0xFFEF7186)
                                }
                                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(ink).border(1.dp, border.copy(alpha = 0.7f), RoundedCornerShape(14.dp)).padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Core ${core.index + 1}", color = primaryText, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        Text("${(core.fraction * 100).toInt()}%", color = loadColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Text("${core.name} · ${core.cluster}", color = muted, fontSize = 9.sp)
                                    Spacer(Modifier.height(5.dp))
                                    val coreHistory = coreHistories[core.index].orEmpty().ifEmpty { listOf(core.fraction) }
                                    CoreLiveChart(values = coreHistory, color = loadColor, modifier = Modifier.fillMaxWidth().height(26.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(core.currentKhz?.let { "%.2f GHz".format(java.util.Locale.US, it / 1_000_000.0) } ?: "—", color = primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.weight(1f))
                                        Text(core.maxKhz?.let { "max %.2f".format(java.util.Locale.US, it / 1_000_000.0) } ?: "", color = muted, fontSize = 10.sp)
                                    }
                                    core.minKhz?.let { minK ->
                                        Text("Dải xung: %.2f – %.2f GHz".format(java.util.Locale.US, minK / 1_000_000.0, (core.maxKhz ?: minK) / 1_000_000.0), color = muted, fontSize = 9.sp)
                                    }
                                    core.governor?.let { gov ->
                                        Text("Governor: $gov", color = cyan.copy(alpha = 0.9f), fontSize = 9.sp)
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    // /proc/cpuinfo Tab
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ink).border(1.dp, border, RoundedCornerShape(16.dp)).padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized(if (englishLanguage.value) "SYSTEM HARDWARE IDENTIFIERS" else "ĐỊNH DANH PHẦN CỨNG TỪ /proc/cpuinfo"), color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(cpuDetail.rawText))
                                    copiedMessage = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = cyan.copy(alpha = 0.15f), contentColor = cyan),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (copiedMessage) (localized(if (englishLanguage.value) "Copied!" else "Đã sao chép!")) else (localized(if (englishLanguage.value) "Copy All" else "Sao chép toàn bộ")), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("Hardware", color = muted, fontSize = 12.sp, modifier = Modifier.weight(.4f))
                            Text(cpuDetail.hardware, color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(.6f))
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("Revision", color = muted, fontSize = 12.sp, modifier = Modifier.weight(.4f))
                            Text(cpuDetail.revision, color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(.6f))
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("Serial", color = muted, fontSize = 12.sp, modifier = Modifier.weight(.4f))
                            Text(cpuDetail.serial, color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(.6f))
                        }
                    }

                    // Processors list
                    cpuDetail.processors.forEach { proc ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ink).border(1.dp, border, RoundedCornerShape(14.dp)).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Processor ${proc.processorId}", color = cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text(proc.modelName, color = primaryText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            listOf(
                                "BogoMIPS" to proc.bogoMips,
                                "CPU architecture" to proc.architecture,
                                "CPU implementer" to proc.implementer,
                                "CPU part" to proc.part,
                                "CPU variant" to proc.variant,
                                "CPU revision" to proc.revision,
                                "Features" to proc.features
                            ).forEach { (lbl, valStr) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(lbl, color = muted, fontSize = 11.sp, modifier = Modifier.weight(.4f))
                                    Text(valStr, color = primaryText, fontSize = 11.sp, modifier = Modifier.weight(.6f))
                                }
                            }
                        }
                    }

                    // Raw text box
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ink).border(1.dp, border, RoundedCornerShape(14.dp)).padding(12.dp)) {
                        Text(localized(if (englishLanguage.value) "RAW /proc/cpuinfo OUTPUT" else "DỮ LIỆU THÔ /proc/cpuinfo"), color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.35f))
                                .padding(8.dp).verticalScroll(rememberScrollState())
                        ) {
                            Text(cpuDetail.rawText, color = cyan.copy(alpha = 0.9f), fontSize = 10.sp, lineHeight = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = accentText), shape = RoundedCornerShape(14.dp)) {
                Text(localized(if (englishLanguage.value) "Close" else "Đóng"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GpuInDepthDialog(
    advanced: AdvancedRepository,
    onClose: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Năng lực đồ họa, 1: GL_EXTENSIONS
    val clipboard = LocalClipboardManager.current
    var copiedMessage by remember { mutableStateOf(false) }
    var extFilter by remember { mutableStateOf("") }

    val gpuDetail = remember { advanced.gpuDetailedAnalysis() }

    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f).clip(RoundedCornerShape(24.dp)).background(panel)
                .border(1.dp, border, RoundedCornerShape(24.dp)).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(localized(if (englishLanguage.value) "IN-DEPTH GPU ANALYSIS" else "PHÂN TÍCH CHUYÊN SÂU GPU"), color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(localized(if (englishLanguage.value) "Graphics Capabilities & Hardware Limits" else "Năng lực đồ họa & Tiện ích mở rộng"), color = primaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                ChoiceChip("✕", false, onClose)
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(localized(if (englishLanguage.value) "Capabilities & Limits" else "Năng lực & Giới hạn"), selectedTab == 0) { selectedTab = 0 }
                ChoiceChip(localized(if (englishLanguage.value) "GL_EXTENSIONS (${gpuDetail.extensions.size})" else "GL_EXTENSIONS (${gpuDetail.extensions.size})"), selectedTab == 1) { selectedTab = 1 }
            }

            Spacer(Modifier.height(14.dp))

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedTab == 0) {
                    // Gaming assessment card
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cyan.copy(alpha = .12f)).border(1.dp, cyan.copy(alpha = .35f), RoundedCornerShape(16.dp)).padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(cyan))
                            Spacer(Modifier.width(8.dp))
                            Text(gpuDetail.gamingTier, color = cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(gpuDetail.gamingAssessment, color = primaryText, fontSize = 12.sp, lineHeight = 17.sp)
                    }

                    // Hardware limits
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ink).border(1.dp, border, RoundedCornerShape(16.dp)).padding(14.dp)) {
                        Text(localized(if (englishLanguage.value) "OPENGL ES HARDWARE LIMITS" else "GIỚI HẠN PHẦN CỨNG OPENGL ES"), color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "Max Texture Size" to if (gpuDetail.maxTextureSize > 0) "${gpuDetail.maxTextureSize} × ${gpuDetail.maxTextureSize} px" else "Không công khai",
                            "Max Renderbuffer Size" to if (gpuDetail.maxRenderbufferSize > 0) "${gpuDetail.maxRenderbufferSize} × ${gpuDetail.maxRenderbufferSize} px" else "Không công khai",
                            "Max Viewport Dims" to if (gpuDetail.maxViewportDims.first > 0) "${gpuDetail.maxViewportDims.first} × ${gpuDetail.maxViewportDims.second} px" else "Không công khai",
                            "Max Vertex Attributes" to if (gpuDetail.maxVertexAttribs > 0) "${gpuDetail.maxVertexAttribs} thuộc tính" else "Không công khai",
                            "Max Combined Texture Units" to if (gpuDetail.maxCombinedTextureUnits > 0) "${gpuDetail.maxCombinedTextureUnits} units" else "Không công khai"
                        ).forEach { (lbl, valStr) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(lbl, color = muted, fontSize = 12.sp, modifier = Modifier.weight(.5f))
                                Text(valStr, color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(.5f))
                            }
                        }
                    }

                    // Renderer specifications
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ink).border(1.dp, border, RoundedCornerShape(16.dp)).padding(14.dp)) {
                        Text(localized(if (englishLanguage.value) "DRIVER & COMPUTE CAPABILITIES" else "DRIVER VÀ KHẢ NĂNG TÍNH TOÁN"), color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "GPU Renderer" to gpuDetail.renderer,
                            "GPU Vendor" to gpuDetail.vendor,
                            "OpenGL Driver Version" to gpuDetail.glesVersion,
                            "GLSL Version" to gpuDetail.glslVersion,
                            "OpenGL ES Config" to gpuDetail.openGlEs,
                            "Vulkan Hardware Version" to gpuDetail.vulkanVersion,
                            "Vulkan Level" to gpuDetail.vulkanLevel,
                            "Vulkan Compute Support" to if (gpuDetail.vulkanCompute) "Có hỗ trợ (Vulkan Compute Shader)" else "Không hỗ trợ"
                        ).forEach { (lbl, valStr) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(lbl, color = muted, fontSize = 12.sp, modifier = Modifier.weight(.45f))
                                Text(valStr, color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(.55f))
                            }
                        }
                    }
                } else {
                    // GL_EXTENSIONS tab
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ink).border(1.dp, border, RoundedCornerShape(16.dp)).padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${gpuDetail.extensions.size} Tiện ích mở rộng (Extensions)", color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(gpuDetail.extensions.joinToString("\n")))
                                    copiedMessage = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = cyan.copy(alpha = 0.15f), contentColor = cyan),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (copiedMessage) (localized(if (englishLanguage.value) "Copied!" else "Đã sao chép!")) else (localized(if (englishLanguage.value) "Copy All" else "Sao chép tất cả")), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = extFilter,
                            onValueChange = { extFilter = it },
                            placeholder = { Text(localized(if (englishLanguage.value) "Search extensions..." else "Tìm kiếm extension..."), fontSize = 12.sp, color = muted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = primaryText)
                        )

                        Spacer(Modifier.height(10.dp))
                        val filtered = gpuDetail.extensions.filter { it.contains(extFilter.trim(), ignoreCase = true) }
                        Text(localized(if (englishLanguage.value) "Showing ${filtered.size} of ${gpuDetail.extensions.size}" else "Hiển thị ${filtered.size} / ${gpuDetail.extensions.size} tiện ích"), color = muted, fontSize = 10.sp)
                        Spacer(Modifier.height(8.dp))

                        filtered.forEachIndexed { idx, ext ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(panel).padding(horizontal = 10.dp, vertical = 6.dp).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${idx + 1}.", color = muted, fontSize = 10.sp, modifier = Modifier.width(28.dp))
                                Text(ext, color = cyan, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = accentText), shape = RoundedCornerShape(14.dp)) {
                Text(localized(if (englishLanguage.value) "Close" else "Đóng"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickStats(battery: BatteryLive, wifi: WifiLive) {
    val batteryStatus = when (battery.status) {
        android.os.BatteryManager.BATTERY_STATUS_CHARGING -> localized(if (englishLanguage.value) "Charging" else "Đang sạc")
        android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> localized(if (englishLanguage.value) "Discharging" else "Đang xả")
        android.os.BatteryManager.BATTERY_STATUS_FULL -> localized(if (englishLanguage.value) "Full" else "Đã đầy")
        android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> localized(if (englishLanguage.value) "Not charging" else "Không sạc")
        else -> localized(if (englishLanguage.value) "Unknown" else "Không rõ")
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(panel)
            .border(1.dp, border, RoundedCornerShape(16.dp)).padding(14.dp)) {
            Text("Wi-Fi", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(if (wifi.connected) wifi.linkMbps?.let { "$it Mbps" } ?: "— Mbps"
                else localized(if (englishLanguage.value) "Offline" else "Ngoại tuyến"),
                color = primaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${wifi.frequencyMHz?.let { "$it MHz" } ?: "— MHz"} · ${localized(if (englishLanguage.value) "BW" else "Băng thông")} ${wifi.bandwidthMbps?.let { "$it Mbps" } ?: "—"}",
                color = muted, fontSize = 10.sp, maxLines = 2, lineHeight = 14.sp)
        }
        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(panel)
            .border(1.dp, border, RoundedCornerShape(16.dp)).padding(14.dp)) {
            Text(localized(if (englishLanguage.value) "BATTERY" else "PIN"), color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

private fun displayManufacturer(context: android.content.Context, detail: List<Section>): String {
    val panel = detail.firstOrNull { it.title == "Tấm nền & hiển thị" }
    val name = panel?.fields?.firstOrNull { it.label == "Tên tấm nền" }?.value
        ?.takeUnless { it.isBlank() || it.startsWith("Không") }
    val pnp = panel?.fields?.firstOrNull { it.label == "Mã OEM tấm nền" }?.value
        ?.takeUnless { it.isBlank() || it.startsWith("Không") }
    val bidi = android.text.BidiFormatter.getInstance(context.resources.configuration.locales[0])
    val unknown = AppStrings.localize(context, "Không công khai")
    return context.getString(R.string.panel_oem_format, bidi.unicodeWrap(name ?: unknown), bidi.unicodeWrap(pnp ?: unknown))
}

@Composable
private fun PageIntro(title: String, subtitle: String, manufacturer: String? = null, batteryLevel: Int? = null) {
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
            if (title == "Battery") {
                BatteryGlyph(batteryLevel ?: -1)
            } else {
                AssetIcon(IconAssets.page(title), Modifier.size(IconSizes.pageIcon), title) {
                    Icon(icon, contentDescription = null, tint = cyan, modifier = Modifier.size(IconSizes.pageIcon))
                }
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

private fun safeAvailableBytes(): Long = try {
    android.os.StatFs(android.os.Environment.getDataDirectory().path).availableBytes
} catch (_: Throwable) { 40_000_000_000L }

private fun safeFreeBytes(): Long = try {
    android.os.StatFs(android.os.Environment.getDataDirectory().path).freeBytes
} catch (_: Throwable) { 42_000_000_000L }

private fun safeTotalBytes(): Long = try {
    android.os.StatFs(android.os.Environment.getDataDirectory().path).totalBytes
} catch (_: Throwable) { 128_000_000_000L }

private data class StorageSlice(val label: String, val percent: Float, val color: Color)

@Composable
private fun getStorageSlices(total: Long, detail: List<Section>): List<StorageSlice> {
    val available = safeAvailableBytes()
    val free = safeFreeBytes()
    val fieldLabels = listOf(
        "Mã ứng dụng" to Color(0xFF5FE1D5),                   // Cyan/Mint
        "Dữ liệu ứng dụng (không cache)" to Color(0xFFA78BFA), // Purple/Violet
        "Cache ứng dụng" to Color(0xFFFFB454),                 // Gold/Amber
        "Chưa phân loại" to Color(0xFFEF7186),                 // Coral/Rose
        "Trống dùng được" to Color(0xFF3B4A61)                 // Muted Dark Slate
    )
    val detailed = detail.firstOrNull { it.title == "Phân loại dung lượng" }
    val parsed = fieldLabels.mapNotNull { (label, color) ->
        val value = detailed?.fields?.firstOrNull { it.label == label }?.value
        val percent = value?.let { Regex("([0-9]+(?:\\.[0-9]+)?)%").find(it)?.groupValues?.get(1)?.toFloatOrNull() }
        percent?.let { StorageSlice(localized(label), it, color) }
    }
    if (parsed.isNotEmpty()) return parsed

    val occupied = (total - free).coerceAtLeast(0)
    val reserved = (free - available).coerceAtLeast(0)
    fun percent(v: Long): Float = if (total > 0) (v.toDouble() / total * 100.0).toFloat() else 0f
    return listOf(
        StorageSlice(localized(if (englishLanguage.value) "System & Files" else "Tệp & Hệ thống"), percent(occupied), Color(0xFF5FE1D5)),
        StorageSlice(localized(if (englishLanguage.value) "Reserved free" else "Trống dành riêng"), percent(reserved), Color(0xFFA78BFA)),
        StorageSlice(localized(if (englishLanguage.value) "Available" else "Trống dùng được"), percent(available), Color(0xFF3B4A61))
    )
}

@Composable
private fun MemoryCard(sample: MemorySample, history: List<Float>, expanded: Boolean, storageTotal: Long, storageDetail: List<Section> = emptyList()) {
    val storageAvailable = safeAvailableBytes()
    val storageUsed = (storageTotal - storageAvailable).coerceAtLeast(0)
    val storageFraction = if (storageTotal > 0) (storageUsed.toFloat() / storageTotal).coerceIn(0f, 1f) else 0f
    val slices = getStorageSlices(storageTotal, storageDetail)

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(22.dp)).padding(if (expanded) 20.dp else 16.dp)) {
        if (expanded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(localized("RAM ĐANG SỬ DỤNG"), color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(bytes(sample.used), color = primaryText, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text("${(sample.percent * 100).toInt()}% · ${bytes(sample.total)}", color = cyan, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            RamChart(history, 180)
            Spacer(Modifier.height(7.dp))
            Row {
                Text("${localized("Khả dụng")}: ${bytes(sample.available)}", color = muted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(localized(if (englishLanguage.value) "LAST 60 SECONDS" else "60 GIÂY GẦN NHẤT"), color = muted, fontSize = 10.sp)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // LEFT SIDE: RAM
                Column(Modifier.weight(1f)) {
                    Text(localized("RAM (SỬ DỤNG)"), color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(bytes(sample.used), color = primaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${(sample.percent * 100).toInt()}% · ${bytes(sample.total)}", color = cyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    RamChart(history, 48)
                    Spacer(Modifier.height(6.dp))
                    Text("${localized("Khả dụng")}: ${bytes(sample.available)}", color = muted, fontSize = 10.sp, maxLines = 1)
                }

                // Vertical Divider Line
                Box(Modifier.width(1.dp).height(130.dp).background(border))

                // RIGHT SIDE: STORAGE WITH COLORFUL BREAKDOWN
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(localized(if (englishLanguage.value) "STORAGE" else "BỘ NHỚ TRONG"), color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, modifier = Modifier.weight(1f))
                        Text("${(storageFraction * 100).toInt()}%", color = primaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(bytes(storageUsed), color = primaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${bytes(storageUsed)} / ${bytes(storageTotal)}", color = muted, fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))

                    // Multi-colored Storage Progress Bar
                    Canvas(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))) {
                        var startX = 0f
                        slices.forEach { slice ->
                            val w = size.width * (slice.percent / 100f).coerceIn(0f, 1f)
                            if (w > 0f) {
                                drawRect(color = slice.color, topLeft = Offset(startX, 0f), size = Size(w, size.height))
                                startX += w
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Storage Breakdown Slices Legend
                    slices.take(4).forEach { slice ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(slice.color))
                            Spacer(Modifier.width(5.dp))
                            Text(slice.label, color = muted, fontSize = 9.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("%.1f%%".format(java.util.Locale.US, slice.percent), color = primaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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
private fun AnalysisCard(section: Section, onDeepAnalysis: (() -> Unit)? = null) {
    var expanded by rememberSaveable(section.title) { mutableStateOf(false) }
    val cpu = section.title.startsWith("CPU")
    val basicLabels = if (cpu) setOf("Processor", "SoC OEM", "Số nhân logic", "Bộ nhớ đệm (Cache)", "Dải xung từng nhân (Min - Max)")
        else setOf("GPU renderer", "GPU vendor", "OpenGL ES", "Vulkan version", "Max Texture Size")
    val visible = if (expanded) section.fields else section.fields.filter { it.label in basicLabels }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(20.dp)).padding(18.dp)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                .background(cyan.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                val identityLabels = if (cpu) setOf("Processor", "SoC OEM", "Hardware") else setOf("GPU renderer", "GPU vendor")
                val chipName = section.fields.filter { it.label in identityLabels }.joinToString(" ") { it.value }
                AssetIcon(IconAssets.chip(chipName), Modifier.size(29.dp), chipName) {
                    Icon(if (cpu) Icons.Default.Memory else Icons.Default.Visibility,
                        contentDescription = null, tint = cyan, modifier = Modifier.size(23.dp))
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(localized(section.title), color = primaryText, fontSize = 16.sp,
                    lineHeight = 18.sp, fontWeight = FontWeight.Bold)
                Text(if (expanded) {
                    localized(if (englishLanguage.value) "Collapse analysis" else "Thu gọn phân tích")
                } else {
                    localized(if (englishLanguage.value) "In-depth analysis" else "Phân tích chuyên sâu")
                }, color = cyan, fontSize = 11.sp)
            }
            val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "Analysis arrow")
            Icon(Icons.Default.KeyboardArrowDown,
                contentDescription = localized(if (expanded) "Collapse" else "Expand"),
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
        if (expanded && onDeepAnalysis != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cyan.copy(alpha = 0.12f))
                    .border(1.dp, cyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onDeepAnalysis)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(cyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (cpu) Icons.Default.Speed else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = cyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (cpu) {
                            localized(if (englishLanguage.value) "In-Depth CPU & /proc/cpuinfo Analysis" else "Phân tích chuyên sâu CPU & /proc/cpuinfo")
                        } else {
                            localized(if (englishLanguage.value) "In-Depth GPU & Extensions Analysis" else "Phân tích chuyên sâu GPU & Tiện ích mở rộng")
                        },
                        color = primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (cpu) {
                            localized(if (englishLanguage.value) "Live core metrics & structured /proc/cpuinfo" else "Hiệu suất từng core thời gian thực & dữ liệu từ /proc/cpuinfo")
                        } else {
                            localized(if (englishLanguage.value) "Hardware limits, Vulkan & GL_EXTENSIONS" else "Giới hạn đồ họa, Vulkan & danh sách GL_EXTENSIONS")
                        },
                        color = cyan,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    localized(if (englishLanguage.value) "Open ↗" else "Mở ↗"),
                    color = cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
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

@Composable
private fun StorageCard(total: Long, detail: List<Section>) {
    val available = safeAvailableBytes()
    val used = (total - available).coerceAtLeast(0)
    val fraction = if (total > 0) used.toFloat() / total else 0f
    val slices = getStorageSlices(total, detail)
    val internalSection = detail.firstOrNull { it.title == "Bộ nhớ trong" }
    val flashTech = internalSection?.fields?.firstOrNull { it.label == "Chuẩn lưu trữ" }?.value
    val flashVendor = internalSection?.fields?.firstOrNull { it.label == "Nhà sản xuất chip Flash" }?.value

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(20.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                val trackColor = border.copy(alpha = 0.55f)
                val strokeWidthDp = 14.dp
                Canvas(Modifier.fillMaxSize()) {
                    val strokePx = strokeWidthDp.toPx()
                    // Background total circular track (unallocated/free space) with StrokeCap.Butt
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )

                    // Draw the USED segment starting at -90f (12 o'clock)
                    val totalUsedSweep = (360f * fraction).coerceIn(0f, 360f)
                    if (totalUsedSweep > 0f) {
                        val usedSlices = slices.filter { it.label != localized("Trống dùng được") && it.label != "Available" && it.percent > 0f }
                        var start = -90f
                        if (usedSlices.isNotEmpty()) {
                            usedSlices.forEach { slice ->
                                val sweep = 360f * (slice.percent / 100f).coerceIn(0f, 1f)
                                if (sweep > 0f) {
                                    drawArc(
                                        color = slice.color,
                                        startAngle = start,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                                    )
                                    start += sweep
                                }
                            }
                            // Bo cong tròn 2 đầu thanh của phần đang sử dụng
                            if (totalUsedSweep > 2f) {
                                drawArc(
                                    color = usedSlices.first().color,
                                    startAngle = -90f,
                                    sweepAngle = 0.5f,
                                    useCenter = false,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = usedSlices.last().color,
                                    startAngle = -90f + totalUsedSweep - 0.5f,
                                    sweepAngle = 0.5f,
                                    useCenter = false,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )
                            }
                        } else {
                            drawArc(
                                color = cyan,
                                startAngle = -90f,
                                sweepAngle = totalUsedSweep,
                                useCenter = false,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
                Text("${(fraction * 100).toInt()}%", color = primaryText,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text(localized(if (englishLanguage.value) "INTERNAL STORAGE" else "BỘ NHỚ TRONG"), color = muted,
                    fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Text("${(fraction * 100).toInt()}% ${localized(if (englishLanguage.value) "used" else "đã dùng")}",
                    color = primaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${bytes(used)} / ${bytes(total)}", color = muted, fontSize = 12.sp)

                if (!flashTech.isNullOrBlank() || !flashVendor.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(cyan.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            listOfNotNull(flashTech, flashVendor?.substringBefore("(")?.trim()).joinToString(" · "),
                            color = cyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Multi-colored Storage Bar
        Canvas(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
            var startX = 0f
            slices.forEach { slice ->
                val w = size.width * (slice.percent / 100f).coerceIn(0f, 1f)
                if (w > 0f) {
                    drawRect(color = slice.color, topLeft = Offset(startX, 0f), size = Size(w, size.height))
                    startX += w
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        slices.forEach { slice ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(slice.color))
                Spacer(Modifier.width(9.dp))
                Text(slice.label, color = muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("%.1f%%".format(java.util.Locale.US, slice.percent), color = primaryText,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(localized(if (englishLanguage.value) "ZRAM is compressed RAM and is shown on the RAM page. Cache is separated when Usage Access is available."
            else "ZRAM là RAM nén và nằm ở trang RAM. Cache được tách riêng khi có quyền Usage Access."),
            color = muted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun BatteryDashboard(live: BatteryLive, values: List<Float>) {
    val max = (values.maxOfOrNull { kotlin.math.abs(it) } ?: 1f).coerceAtLeast(1f)
    val normalized = values.map { (kotlin.math.abs(it) / max).coerceIn(0f, 1f) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(20.dp)).padding(19.dp)) {
        Text(localized(if (englishLanguage.value) "LIVE POWER" else "ĐIỆN NĂNG TRỰC TIẾP"), color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(localized(if (englishLanguage.value) "CURRENT" else "DÒNG"), live.currentMa?.let { "%+.0f mA".format(java.util.Locale.US, it) } ?: "—", Modifier.weight(1f))
            Metric(localized(if (englishLanguage.value) "POWER" else "CÔNG SUẤT"), live.powerW?.let { "%+.2f W".format(java.util.Locale.US, it) } ?: "—", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        RamChart(normalized, 115)
        Text(localized(if (englishLanguage.value) "|P| over 60 seconds · The +/- sign depends on device reporting" else "|P| trong 60 giây · Dấu +/− tùy cách thiết bị báo dòng sạc/xả"), color = muted, fontSize = 11.sp)
    }
}

@Composable
private fun BatteryGlyph(level: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(34.dp, 18.dp)) {
            val bodyWidth = size.width - 4.dp.toPx()
            drawRoundRect(cyan, size = Size(bodyWidth, size.height), cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            drawRect(cyan, topLeft = Offset(bodyWidth + 1.dp.toPx(), size.height * .3f), size = Size(3.dp.toPx(), size.height * .4f))
            if (level >= 0) drawRoundRect(cyan.copy(alpha = .6f), topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                size = Size((bodyWidth - 6.dp.toPx()) * level.coerceIn(0, 100) / 100f, size.height - 6.dp.toPx()), cornerRadius = CornerRadius(1.dp.toPx()))
        }
        Text(if (level >= 0) "$level%" else "—", color = cyan, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StorageAppsSelector(count: Int, expanded: Boolean, scope: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(16.dp))) {
        Row(Modifier.fillMaxWidth().clickable(onClickLabel = localized(if (expanded) "Collapse apps" else "Show apps"), onClick = onClick)
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Android, contentDescription = null, tint = cyan, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                Text(LocalContext.current.resources.getQuantityString(R.plurals.visible_app_count, count, count), color = primaryText,
                    fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                Text(if (expanded) { localized(if (englishLanguage.value) "Tap to collapse" else "Chạm để thu gọn") }
                    else { localized(if (englishLanguage.value) "Tap to view app storage" else "Chạm để xem dung lượng ứng dụng") },
                    color = muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = cyan,
                modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = if (expanded) 180f else 0f })
        }
        if (expanded) Text(scope, color = muted, fontSize = 10.sp, lineHeight = 14.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
    }
}

@Composable
private fun StorageAppRow(section: Section) {
    val context = LocalContext.current
    val packageName = section.fields.first { it.label == "Package" }.value
    val icon by produceState<android.graphics.Bitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            try { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96) }
            catch (_: Exception) { null }
        }
    }
    Row(Modifier.fillMaxWidth().clickable { openAppDetails(context, packageName) }.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) Image(icon!!.asImageBitmap(), section.title, Modifier.size(40.dp))
        else Icon(Icons.Default.Android, section.title, tint = cyan, modifier = Modifier.size(40.dp))
        Column(Modifier.weight(1f)) {
            androidx.compose.material3.Text(section.title, color = primaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
            androidx.compose.material3.Text(packageName, color = muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(section.fields.firstOrNull { it.label == "Tổng" }?.value.orEmpty(), color = primaryText, fontSize = 12.sp)
    }
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(border))
}

@Composable
private fun SensorCard(section: Section, active: Boolean = false) {
    val type = section.fields.firstOrNull { it.label == "Sensor Type" }?.value.orEmpty()
    val key = section.fields.firstOrNull { it.label == "Sensor ID" }?.value.orEmpty() + section.title + type
    var expanded by rememberSaveable(key) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(panel)
        .border(1.dp, border, RoundedCornerShape(18.dp)).padding(14.dp)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
            AssetIcon(IconAssets.sensor(type.lowercase()), Modifier.size(28.dp), section.title) {
                Icon(Icons.Default.Sensors, contentDescription = null, tint = cyan, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                androidx.compose.material3.Text(section.title, color = primaryText, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
                Text(section.fields.firstOrNull { it.label == "Vendor / OEM" }?.value.orEmpty(), color = muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = localized(if (expanded) "Collapse" else "Expand"),
                tint = cyan, modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = if (expanded) 180f else 0f })
        }
        AnimatedVisibility(expanded) {
            Column {
                Spacer(Modifier.height(10.dp))
                SensorLiveChart(section, active && expanded, cyan, primaryText, muted, englishLanguage.value)
                Spacer(Modifier.height(8.dp))
                section.fields.filterNot { it.label in setOf("Sensor Name", "Vendor / OEM") }.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { field ->
                            Column(Modifier.weight(1f)) {
                                Text(localized(field.label), color = muted, fontSize = 9.sp, lineHeight = 12.sp)
                                Text(localized(field.value), color = primaryText, fontSize = 10.sp, lineHeight = 13.sp)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
@Composable
private fun CameraCard(section: Section) {
    val title = section.title
    val isFront = title.contains("Trước") || title.contains("Front") || title.contains("Selfie")
    val isUltraWide = title.contains("Siêu Rộng") || title.contains("Ultra-Wide")
    val isTele = title.contains("Telephoto") || title.contains("Zoom") || title.contains("Periscope")
    val isMacro = title.contains("Cận Cảnh") || title.contains("Macro")
    val isDepth = title.contains("Chiều Sâu") || title.contains("Depth")
    val isMono = title.contains("Đơn Sắc") || title.contains("Monochrome")
    val isMulti = title.contains("Đa Ống Kính") || title.contains("Multi-Camera")

    val mpVal = section.fields.firstOrNull { it.label.contains("phân giải") }?.value?.substringBefore("(")?.trim() ?: ""
    val apertureVal = section.fields.firstOrNull { it.label == "Khẩu độ" }?.value ?: ""
    val oisVal = section.fields.firstOrNull { it.label.contains("Chống rung") }?.value ?: ""
    val hasOis = oisVal.contains("Có") || oisVal.contains("OIS")

    val (badgeText, badgeColor) = when {
        isFront -> (if (isUltraWide) "0.8x · SELFIE" else "1x · SELFIE") to Color(0xFFFFB454)
        isUltraWide -> "0.6x · ULTRA-WIDE" to Color(0xFF5FE1D5)
        isTele -> "OPTICAL ZOOM" to Color(0xFFA78BFA)
        isMacro -> "MACRO · 2-5CM" to Color(0xFF5FE1A0)
        isDepth -> "3D DEPTH" to Color(0xFF9E86EB)
        isMono -> "MONO B&W" to Color(0xFF94A3B8)
        isMulti -> "MULTI-LENS" to cyan
        else -> "1x · MAIN WIDE" to cyan
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(panel)
            .border(1.dp, border, RoundedCornerShape(20.dp)).padding(19.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                CameraLensIcon(
                    isFront = isFront,
                    isUltraWide = isUltraWide,
                    isTele = isTele,
                    isMacro = isMacro,
                    isDepth = isDepth,
                    isMono = isMono,
                    tint = badgeColor
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    localized(section.title),
                    color = primaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(3.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(badgeColor.copy(alpha = 0.18f)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badgeText, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    if (mpVal.isNotBlank()) {
                        Text(mpVal, color = primaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (apertureVal.isNotBlank() && apertureVal != "Không công khai") {
                        Text("•  $apertureVal", color = muted, fontSize = 11.sp)
                    }
                    if (hasOis) {
                        Box(
                            Modifier.clip(RoundedCornerShape(5.dp)).background(cyan.copy(alpha = 0.2f)).padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("OIS", color = cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        section.fields.forEachIndexed { index, field ->
            if (index > 0) Spacer(Modifier.fillMaxWidth().height(1.dp).background(border))
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(localized(field.label), color = muted, fontSize = 12.sp, modifier = Modifier.weight(.43f))
                Text(
                    localized(field.value),
                    color = primaryText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(.57f)
                )
            }
        }
    }
}

@Composable
private fun CameraLensIcon(
    isFront: Boolean,
    isUltraWide: Boolean,
    isTele: Boolean,
    isMacro: Boolean,
    isDepth: Boolean,
    isMono: Boolean,
    tint: Color
) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        when {
            isMacro -> {
                val center = Offset(w / 2f, h / 2f)
                val petalRadius = w * 0.28f
                drawCircle(color = tint, radius = w * 0.12f, center = center)
                for (angle in 0 until 360 step 60) {
                    val rad = Math.toRadians(angle.toDouble())
                    val cx = center.x + (petalRadius * Math.cos(rad)).toFloat()
                    val cy = center.y + (petalRadius * Math.sin(rad)).toFloat()
                    drawCircle(color = tint.copy(alpha = 0.75f), radius = w * 0.15f, center = Offset(cx, cy))
                }
            }
            isUltraWide -> {
                val path = Path().apply {
                    moveTo(w * 0.1f, h * 0.25f)
                    lineTo(w * 0.9f, h * 0.15f)
                    lineTo(w * 0.9f, h * 0.85f)
                    lineTo(w * 0.1f, h * 0.75f)
                    close()
                }
                drawPath(path, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(color = tint, radius = w * 0.18f, center = Offset(w * 0.45f, h * 0.5f), style = Stroke(width = 1.8.dp.toPx()))
            }
            isTele -> {
                val center = Offset(w / 2f, h / 2f)
                drawCircle(color = tint, radius = w * 0.44f, center = center, style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = tint, radius = w * 0.26f, center = center, style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = tint, radius = w * 0.1f, center = center)
                drawLine(tint, Offset(w * 0.05f, h / 2f), Offset(w * 0.25f, h / 2f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.75f, h / 2f), Offset(w * 0.95f, h / 2f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w / 2f, h * 0.05f), Offset(w / 2f, h * 0.25f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w / 2f, h * 0.75f), Offset(w / 2f, h * 0.95f), strokeWidth = 2.dp.toPx())
            }
            isDepth -> {
                drawCircle(color = tint.copy(alpha = 0.5f), radius = w * 0.32f, center = Offset(w * 0.38f, h * 0.5f))
                drawCircle(color = tint, radius = w * 0.32f, center = Offset(w * 0.62f, h * 0.5f), style = Stroke(width = 2.dp.toPx()))
            }
            isMono -> {
                val center = Offset(w / 2f, h / 2f)
                drawCircle(color = tint, radius = w * 0.42f, center = center, style = Stroke(width = 2.dp.toPx()))
                drawArc(color = tint, startAngle = 90f, sweepAngle = 180f, useCenter = true)
            }
            isFront -> {
                val center = Offset(w / 2f, h * 0.36f)
                drawCircle(color = tint, radius = w * 0.22f, center = center)
                val body = Path().apply {
                    moveTo(w * 0.15f, h * 0.9f)
                    cubicTo(w * 0.2f, h * 0.62f, w * 0.8f, h * 0.62f, w * 0.85f, h * 0.9f)
                    close()
                }
                drawPath(body, color = tint)
            }
            else -> {
                val center = Offset(w / 2f, h / 2f)
                drawCircle(color = tint, radius = w * 0.44f, center = center, style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = tint.copy(alpha = 0.25f), radius = w * 0.35f, center = center)
                drawCircle(color = tint, radius = w * 0.16f, center = center)
            }
        }
    }
}

@Composable
private fun InfoDrawer(onClose: () -> Unit) {
    Column(Modifier.width(292.dp).fillMaxHeight().background(panel).padding(20.dp)) {
        Text("Device Info", color = cyan, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Version 1.0", color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(30.dp))
        Text(localized(if (englishLanguage.value) "About" else "Về ứng dụng"), color = primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(localized(if (englishLanguage.value) "Explore device hardware, Android, sensors and live resources." else "Khám phá phần cứng, Android, cảm biến và tài nguyên trực tiếp của thiết bị."), color = muted, fontSize = 13.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(24.dp))
        Text(localized(if (englishLanguage.value) "Privacy" else "Quyền riêng tư"), color = primaryText, fontWeight = FontWeight.Bold)
        Text(localized(if (englishLanguage.value) "Device data is read locally. No account or cloud sync." else "Thông số được đọc trên thiết bị. Không yêu cầu tài khoản hay đồng bộ đám mây."), color = muted, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(24.dp))
        Text("© 2026 Device Info", color = muted, fontSize = 12.sp)
        Text(localized(if (englishLanguage.value) "Built with Kotlin, Jetpack Compose and Android APIs. Third-party licenses follow their respective packages." else "Xây dựng bằng Kotlin, Jetpack Compose và Android API. Bản quyền thư viện theo từng gói tương ứng."), color = muted, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(24.dp))
        ChoiceChip(localized(if (englishLanguage.value) "Close" else "Đóng"), false, onClose)
    }
}

@Composable
private fun SettingsContent(theme: String, language: String, units: String, temperature: String,
                            accent: String, onTheme: (String) -> Unit,
                            onLanguage: (String) -> Unit, onUnits: (String) -> Unit,
                            onTemperature: (String) -> Unit, onAccent: (String) -> Unit,
                            onClose: () -> Unit) {
    var editedAccent by remember(accent) { mutableStateOf(accent) }
    Column(Modifier.width(318.dp).fillMaxHeight().background(panel).verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized(if (englishLanguage.value) "Settings" else "Cài đặt"), color = primaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            ChoiceChip("✕", false, onClose)
        }
        Spacer(Modifier.height(24.dp))
        Text(localized(if (englishLanguage.value) "Language" else "Ngôn ngữ"), color = primaryText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LanguageDropdown(language, onLanguage)
        Spacer(Modifier.height(22.dp))
        Text(localized(if (englishLanguage.value) "Appearance" else "Giao diện"), color = primaryText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("system" to "System", "dark" to "Dark", "light" to "Light").forEach { (key, label) -> ChoiceChip(label, theme == key) { onTheme(key) } }
        }
        Spacer(Modifier.height(14.dp))
        Text(localized(if (englishLanguage.value) "Accent color" else "Màu chủ đạo"), color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        AccentDropdown(accent, onAccent)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = editedAccent, onValueChange = { editedAccent = it.take(7) }, label = { Text("#RRGGBB") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onAccent(editedAccent) }, enabled = Regex("#[0-9A-Fa-f]{6}").matches(editedAccent), colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = accentText)) {
            Text(localized(if (englishLanguage.value) "Apply color" else "Áp dụng màu"))
        }
        Spacer(Modifier.height(18.dp))
        Text(localized(if (englishLanguage.value) "Units" else "Đơn vị đo"), color = primaryText, fontWeight = FontWeight.Bold)
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
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun LanguageDropdown(language: String, onLanguage: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "system" to (localized(if (englishLanguage.value) "🌐 System language" else "🌐 Theo ngôn ngữ hệ thống")),
        "vi" to "🇻🇳 Việt Nam · Tiếng Việt",
        "en" to "🇺🇸 United States · English",
        "ja" to "🇯🇵 日本 · 日本語",
        "zh-Hans" to "🇨🇳 中国 · 简体中文",
        "fr" to "🇫🇷 France · Français"
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
    val options = listOf("" to (localized(if (englishLanguage.value) "Default" else "Mặc định")),
        "#5FE1D5" to "Mint", "#A78BFA" to "Violet", "#FFAB64" to "Amber",
        "#F46B9B" to "Rose", "#5EA6FF" to "Blue")
    val label = options.firstOrNull { it.first.equals(accent, ignoreCase = true) }?.second
        ?: (localized(if (englishLanguage.value) "Custom" else "Tùy chỉnh"))
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

// ==========================================
// JETPACK COMPOSE PREVIEWS FOR ANDROID STUDIO
// ==========================================

@Preview(name = "1. Header", showBackground = true)
@Composable
private fun PreviewHeader() {
    Surface(color = ink) {
        Header(onInfo = {}, onSettings = {})
    }
}

@Preview(name = "2. Overview Hero", showBackground = true)
@Composable
private fun PreviewOverviewHero() {
    Surface(color = ink, modifier = Modifier.padding(16.dp)) {
        OverviewHero()
    }
}

@Preview(name = "3. Liquid Tabs", showBackground = true)
@Composable
private fun PreviewLiquidTabs() {
    Surface(color = ink) {
        LiquidTabs(
            selected = 0,
            pagePosition = 0f,
            pagerMoving = false,
            tappedTab = null,
            scrollState = rememberScrollState(),
            onViewportWidth = {},
            onSelect = {}
        )
    }
}

@Preview(name = "4. Quick Stats (Wi-Fi & Battery)", showBackground = true)
@Composable
private fun PreviewQuickStats() {
    Surface(color = ink, modifier = Modifier.padding(16.dp)) {
        QuickStats(
            battery = BatteryLive(
                level = 85,
                currentMa = 1200.0,
                averageMa = 1100.0,
                voltageV = 4.1,
                powerW = 4.92,
                status = android.os.BatteryManager.BATTERY_STATUS_CHARGING
            ),
            wifi = WifiLive(
                connected = true,
                linkMbps = 866,
                frequencyMHz = 5180,
                bandwidthMbps = 80
            )
        )
    }
}

@Preview(name = "5. CPU Activity Card", showBackground = true)
@Composable
private fun PreviewCpuActivityCard() {
    Surface(color = ink, modifier = Modifier.padding(16.dp)) {
        CpuActivityCard(
            cores = listOf(
                CpuCore(0, 1800000L, 2400000L, "Core 1"),
                CpuCore(1, 1400000L, 2400000L, "Core 2"),
                CpuCore(2, 2200000L, 2800000L, "Core 3"),
                CpuCore(3, 2800000L, 2800000L, "Core 4")
            ),
            cpuHistory = listOf(0.18f, 0.25f, 0.42f, 0.35f, 0.22f, 0.55f, 0.38f),
            onClick = {}
        )
    }
}

@Preview(name = "6. Memory Card (RAM & Chart)", showBackground = true)
@Composable
private fun PreviewMemoryCard() {
    Surface(color = ink, modifier = Modifier.padding(16.dp)) {
        MemoryCard(
            sample = MemorySample(
                total = 8_000_000_000L,
                available = 3_200_000_000L,
                threshold = 500_000_000L,
                low = false,
                appUsed = 250_000_000L,
                advertised = 8_000_000_000L
            ),
            history = listOf(0.4f, 0.42f, 0.5f, 0.55f, 0.58f, 0.6f),
            expanded = false,
            storageTotal = 128_000_000_000L,
            storageDetail = listOf(
                Section(
                    title = "Phân loại dung lượng",
                    fields = listOf(
                        Field("Mã ứng dụng", "12.5%"),
                        Field("Dữ liệu ứng dụng (không cache)", "25.0%"),
                        Field("Cache ứng dụng", "8.0%"),
                        Field("Chưa phân loại", "14.5%"),
                        Field("Trống dùng được", "40.0%")
                    )
                )
            )
        )
    }
}

@Preview(name = "7. Storage Card", showBackground = true)
@Composable
private fun PreviewStorageCard() {
    Surface(color = ink, modifier = Modifier.padding(16.dp)) {
        StorageCard(
            total = 128_000_000_000L,
            detail = listOf(
                Section(
                    title = "Phân loại dung lượng",
                    fields = listOf(
                        Field("Mã ứng dụng", "12.5%"),
                        Field("Dữ liệu ứng dụng (không cache)", "25.0%"),
                        Field("Cache ứng dụng", "8.0%"),
                        Field("Chưa phân loại", "14.5%"),
                        Field("Trống dùng được", "40.0%")
                    )
                )
            )
        )
    }
}

@Preview(name = "8. Battery Dashboard", showBackground = true)
@Composable
private fun PreviewBatteryDashboard() {
    Surface(color = ink, modifier = Modifier.padding(16.dp)) {
        BatteryDashboard(
            live = BatteryLive(
                level = 92,
                currentMa = -450.0,
                averageMa = -400.0,
                voltageV = 4.2,
                powerW = -1.89,
                status = android.os.BatteryManager.BATTERY_STATUS_DISCHARGING
            ),
            values = listOf(1.5f, 1.8f, 2.0f, 1.9f, 1.89f)
        )
    }
}

@Preview(name = "9. Section Card", showBackground = true)
@Composable
private fun PreviewSectionCard() {
    Surface(color = ink, modifier = Modifier.padding(16.dp)) {
        SectionCard(
            section = Section(
                title = "Nhận diện phần cứng",
                fields = listOf(
                    Field("Nhà sản xuất", "Google"),
                    Field("Thương hiệu", "Google"),
                    Field("Model", "Pixel 8 Pro"),
                    Field("Mã thiết bị", "husky"),
                    Field("Bo mạch", "husky")
                )
            )
        )
    }
}

@Preview(name = "10. Sensor Card", showBackground = true)
@Composable
private fun PreviewSensorCard() {
    Surface(color = ink, modifier = Modifier.padding(16.dp)) {
        SensorCard(
            section = Section(
                title = "Gia tốc kế (Accelerometer)",
                fields = listOf(
                    Field("Sensor Type", "accelerometer"),
                    Field("Vendor / OEM", "Google Inc."),
                    Field("Resolution", "0.001 m/s²"),
                    Field("Power", "0.15 mA"),
                    Field("Sensor ID", "accel_0")
                )
            )
        )
    }
}

@Preview(name = "11. Settings Drawer", showBackground = true)
@Composable
private fun PreviewSettingsContent() {
    Surface(color = ink) {
        SettingsContent(
            theme = "system",
            language = "system",
            units = "decimal",
            temperature = "c",
            accent = "#5FE1D5",
            onTheme = {},
            onLanguage = {},
            onUnits = {},
            onTemperature = {},
            onAccent = {},
            onClose = {}
        )
    }
}
