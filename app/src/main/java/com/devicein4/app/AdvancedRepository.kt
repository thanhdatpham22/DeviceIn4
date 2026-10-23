package com.devicein4.app

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.NetworkStatsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.net.wifi.ScanResult
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.SystemClock
import android.media.MediaDrm
import java.util.UUID
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

private const val unavailable = "Không được Android công khai"

class AdvancedRepository(private val context: Context) {
    private val pm = context.packageManager

    fun device(): List<Section> {
        val wm = context.getSystemService(WindowManager::class.java)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return listOf(Section("Nhận diện thiết bị", listOf(
            Field("Manufacturer", Build.MANUFACTURER), Field("Brand", Build.BRAND),
            Field("Model", Build.MODEL), Field("Device", Build.DEVICE),
            Field("Product", Build.PRODUCT), Field("Board", Build.BOARD),
            Field("Hardware", Build.HARDWARE), Field("Bootloader", Build.BOOTLOADER),
            Field("Serial No", serialNumber()),
            Field("Android ID", Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: unavailable),
            Field("Screen Resolution", "${metrics.widthPixels} × ${metrics.heightPixels} px"),
            Field("User", Build.USER), Field("Host", Build.HOST)
        )), Section("Phần cứng", listOf(
            Field("Kiến trúc", Build.SUPPORTED_ABIS.joinToString()),
            Field("Số nhân logic", Runtime.getRuntime().availableProcessors().toString()),
            Field("RAM", bytes(ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }.totalMem)),
            Field("Màn hình", "${metrics.widthPixels} × ${metrics.heightPixels} px · ${metrics.densityDpi} dpi"),
            Field("Đối chiếu linh kiện", "Android không cung cấp nguồn để xác định linh kiện đã thay")
        )))
    }

    fun os(): List<Section> {
        val rootEvidence = listOf("/system/xbin/su", "/system/bin/su", "/sbin/su")
            .any { File(it).exists() } || Build.TAGS?.contains("test-keys") == true
        val uptime = SystemClock.elapsedRealtime() / 1000
        val environment = listOf(
            Field("User", Build.USER), Field("Host", Build.HOST),
            Field("ABI", Build.SUPPORTED_ABIS.joinToString()),
            Field("Device", Build.DEVICE), Field("Product", Build.PRODUCT),
            Field("Kernel", System.getProperty("os.version") ?: unavailable),
            Field("Kernel build", readFirstLine("/proc/version") ?: unavailable)
        )
        return listOf(Section("Phiên bản Android", listOf(
            Field("Version", Build.VERSION.RELEASE),
            Field("Version Name", versionName()),
            Field("API Level", Build.VERSION.SDK_INT.toString()),
            Field("Build ID", Build.ID)
        )), Section("Bản cập nhật", listOf(
            Field("Build Time", DateFormat.getDateTimeInstance().format(Date(Build.TIME))),
            Field("Fingerprint", Build.FINGERPRINT),
            Field("Security patch", Build.VERSION.SECURITY_PATCH),
            Field("Incremental", Build.VERSION.INCREMENTAL),
            Field("Base OS", Build.VERSION.BASE_OS.ifBlank { unavailable })
        )), Section("Bảo mật & khởi động", listOf(
            Field("Bootloader", Build.BOOTLOADER),
            Field("Root", if (rootEvidence) "Có dấu hiệu root (chưa xác nhận)" else "Chưa phát hiện dấu hiệu root")
        )), Section("Thời gian hoạt động", listOf(
            Field("Từ lần khởi động cuối", duration(uptime)),
            Field("Thời gian CPU hoạt động", duration(SystemClock.uptimeMillis() / 1000))
        )), Section("Môi trường", environment), widevine())
    }

    fun cpuGpu(): List<Section> {
        val cpuLines = try { File("/proc/cpuinfo").readLines() } catch (_: Exception) { emptyList() }
        fun cpuValue(key: String): String = cpuLines.firstOrNull { it.substringBefore(':').trim().equals(key, true) }
            ?.substringAfter(':')?.trim()?.takeIf { it.isNotEmpty() } ?: unavailable
        val cpu = Section("CPU · cấu trúc", listOf(
            Field("Processor", if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else cpuValue("Processor")),
            Field("SoC OEM", if (Build.VERSION.SDK_INT >= 31) Build.SOC_MANUFACTURER else unavailable),
            Field("Số nhân logic", Runtime.getRuntime().availableProcessors().toString()),
            Field("BogoMIPS", cpuValue("BogoMIPS")), Field("Features", cpuValue("Features")),
            Field("CPU Implementer", cpuValue("CPU implementer")),
            Field("CPU architecture", cpuValue("CPU architecture")),
            Field("CPU Variant", cpuValue("CPU variant")),
            Field("CPU part", cpuValue("CPU part")),
            Field("CPU revision", cpuValue("CPU revision")),
            Field("Hardware", cpuValue("Hardware").takeUnless { it == unavailable } ?: Build.HARDWARE),
            Field("Revision", cpuValue("Revision")), Field("Serial", cpuValue("Serial")),
            Field("ABI", Build.SUPPORTED_ABIS.joinToString()),
            Field("Xung tối đa từng nhân", (0 until Runtime.getRuntime().availableProcessors()).mapNotNull { core ->
                readFirstLine("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_max_freq")?.toLongOrNull()
                    ?.let { "CPU $core: ${"%.2f".format(Locale.US, it / 1_000_000.0)} GHz" }
            }.joinToString("\n").ifBlank { unavailable })
        ))
        val gl = context.getSystemService(ActivityManager::class.java).deviceConfigurationInfo.glEsVersion
        val feature = pm.systemAvailableFeatures
        val vulkanVersion = feature.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
            ?.version?.let { "${it ushr 22}.${(it shr 12) and 0x3ff}.${it and 0xfff}" } ?: unavailable
        val vulkanLevel = feature.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL }
            ?.version?.toString() ?: unavailable
        val gpuStrings = gpuStrings()
        val gpu = Section("GPU & đồ họa", listOf(
            Field("GPU renderer", gpuStrings[0]),
            Field("GPU vendor", gpuStrings[1]),
            Field("Driver OpenGL", gpuStrings[2]),
            Field("GLSL", gpuStrings[3]),
            Field("OpenGL ES", gl),
            Field("Vulkan version", vulkanVersion),
            Field("Vulkan level", vulkanLevel),
            Field("Vulkan compute", yesNo(pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_COMPUTE)))
        ))
        return listOf(cpu, gpu)
    }

    fun display(): List<Section> {
        val wm = context.getSystemService(WindowManager::class.java)
        @Suppress("DEPRECATION") val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION") display.getRealMetrics(metrics)
        val modes = display.supportedModes.joinToString(" · ") {
            "${it.physicalWidth}×${it.physicalHeight} @ ${"%.0f".format(Locale.US, it.refreshRate)}Hz"
        }
        val hdr = display.hdrCapabilities?.supportedHdrTypes?.joinToString { type ->
            when (type) {
                android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                android.view.Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                android.view.Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                else -> "Type $type"
            }
        }?.ifBlank { "Không" } ?: "Không"
        val brightness = try { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS).toString() + " / 255" }
            catch (_: Exception) { unavailable }
        val panel = if (Build.VERSION.SDK_INT >= 31) display.deviceProductInfo else null
        return listOf(Section("Tấm nền & hiển thị", listOf(
            Field("Độ phân giải thực", "${metrics.widthPixels} × ${metrics.heightPixels} px"),
            Field("Mật độ", "${metrics.densityDpi} dpi · ${"%.2f".format(Locale.US, metrics.density)}x"),
            Field("Kích thước xấp xỉ", "${"%.2f".format(Locale.US, kotlin.math.hypot(metrics.widthPixels / metrics.xdpi, metrics.heightPixels / metrics.ydpi))} inch"),
            Field("Refresh hiện tại", "${display.refreshRate} Hz"),
            Field("Chế độ hỗ trợ", modes.ifBlank { unavailable }),
            Field("HDR", hdr), Field("Wide color gamut", yesNo(display.isWideColorGamut)),
            Field("Độ sáng hệ thống", brightness),
            Field("Hướng", if (metrics.widthPixels > metrics.heightPixels) "Ngang" else "Dọc"),
            Field("Tên tấm nền", panel?.name ?: unavailable),
            Field("Mã OEM tấm nền", panel?.manufacturerPnpId ?: unavailable),
            Field("Mã sản phẩm tấm nền", panel?.productId ?: unavailable),
            Field("OEM tấm nền", "Mã PNP do hệ thống báo; không xác thực linh kiện vật lý"),
            Field("Tình trạng đã thay", "Không thể xác minh qua API Android")
        )))
    }

    fun storage(): Pair<Long, List<Section>> {
        val internal = StatFs(Environment.getDataDirectory().path)
        val totalBytes = internal.totalBytes
        val availableBytes = internal.availableBytes
        val freeBytes = internal.freeBytes
        val occupiedBytes = (totalBytes - freeBytes).coerceAtLeast(0)
        val reservedBytes = (freeBytes - availableBytes).coerceAtLeast(0)
        val used = (totalBytes - availableBytes).coerceAtLeast(0)
        val visiblePackages = pm.getInstalledPackages(0)
        val visibleSystem = visiblePackages.count { info ->
            ((info.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        }
        val storage = mutableListOf(Section("Bộ nhớ trong", listOf(
            Field("Tổng dung lượng phân vùng dữ liệu", bytes(totalBytes)),
            Field("Đã dùng / không khả dụng", "${bytes(used)} · ${percent(used, totalBytes)}"),
            Field("Tệp đang chiếm", "${bytes(occupiedBytes)} · ${percent(occupiedBytes, totalBytes)}"),
            Field("Trống nhưng dành riêng", "${bytes(reservedBytes)} · ${percent(reservedBytes, totalBytes)}"),
            Field("Còn trống dùng được", "${bytes(availableBytes)} · ${percent(availableBytes, totalBytes)}"),
            Field("Cách tính", "Tệp đang chiếm + trống dành riêng + trống dùng được = tổng; đã dùng / không khả dụng gồm 2 mục đầu"),
            Field("Ứng dụng nhìn thấy", "${visiblePackages.size} (Android giới hạn phạm vi)"),
            Field("Ứng dụng hệ thống nhìn thấy", visibleSystem.toString()),
            Field("Cache của DeviceIn4", "${bytes(directorySize(context.cacheDir))} (nằm trong phần đã dùng)"),
            Field("System / Other", "Cần Usage Access để ước lượng phần chưa phân loại; không thể tách chính xác")
        )))
        context.getExternalFilesDirs(null).drop(1).filterNotNull().forEachIndexed { index, path ->
            try {
                val fs = StatFs(path.path)
                storage += Section("Bộ nhớ ngoài ${index + 1}", listOf(
                    Field("Trạng thái", Environment.getExternalStorageState(path)),
                    Field("Tổng", bytes(fs.totalBytes)), Field("Còn trống", bytes(fs.availableBytes)),
                    Field("Có thể tháo rời", yesNo(Environment.isExternalStorageRemovable(path)))
                ))
            } catch (_: Exception) { storage += Section("Bộ nhớ ngoài ${index + 1}", listOf(Field("Lỗi", "Không đọc được ổ lưu trữ"))) }
        }
        return totalBytes to storage
    }

    fun ramExtras(): List<Section> = listOf(zramSection())

    fun storageAnalysis(): List<Section> {
        if (Build.VERSION.SDK_INT < 26 || !hasUsageAccess()) return listOf(
            Section("Ứng dụng & phân loại", listOf(Field("Trạng thái", "Cần Usage Access để xem dung lượng ứng dụng")))
        )
        val manager = context.getSystemService(StorageStatsManager::class.java)
        val internal = StatFs(Environment.getDataDirectory().path)
        val totalBytes = internal.totalBytes
        val used = (totalBytes - internal.availableBytes).coerceAtLeast(0)
        val result = mutableListOf<Section>()
        try {
            val total = manager.queryStatsForUser(StorageManager.UUID_DEFAULT, Process.myUserHandle())
            val codeBytes = total.appBytes.coerceAtLeast(0)
            val dataBytes = total.dataBytes.coerceAtLeast(0)
            val cacheBytes = total.cacheBytes.coerceIn(0, dataBytes)
            val dataWithoutCache = dataBytes - cacheBytes
            val accounted = codeBytes + dataBytes
            result += if (accounted <= used) {
                val otherBytes = used - accounted
                Section("Phân loại dung lượng", listOf(
                    Field("Mã ứng dụng", "${bytes(codeBytes)} · ${percent(codeBytes, totalBytes)}"),
                    Field("Dữ liệu ứng dụng (không cache)", "${bytes(dataWithoutCache)} · ${percent(dataWithoutCache, totalBytes)}"),
                    Field("Cache ứng dụng", "${bytes(cacheBytes)} · ${percent(cacheBytes, totalBytes)}"),
                    Field("Chưa phân loại", "${bytes(otherBytes)} · ${percent(otherBytes, totalBytes)} · gồm hệ thống, media, tệp khác và vùng dành riêng"),
                    Field("Trống dùng được", "${bytes(internal.availableBytes)} · ${percent(internal.availableBytes, totalBytes)}"),
                    Field("Giới hạn", "System, media và tệp của người dùng không thể tách chính xác bằng quyền hiện có")
                ))
            } else {
                Section("Phân loại dung lượng", listOf(
                    Field("Trạng thái", "Không thể đối chiếu: thống kê ứng dụng lớn hơn dung lượng đang dùng"),
                    Field("Mã ứng dụng (hệ thống báo)", bytes(codeBytes)),
                    Field("Dữ liệu ứng dụng (không cache)", bytes(dataWithoutCache)),
                    Field("Cache ứng dụng (trong dữ liệu)", bytes(cacheBytes)),
                    Field("System / Other", "Không thể tính tỷ lệ đáng tin cậy trên thiết bị này")
                ))
            }
        } catch (e: Exception) { result += Section("Phân loại dung lượng", listOf(Field("Lỗi", e.javaClass.simpleName))) }
        val started = SystemClock.elapsedRealtime()
        val apps = mutableListOf<Pair<android.content.pm.PackageInfo, android.app.usage.StorageStats>>()
        var scanned = 0
        for (info in pm.getInstalledPackages(0)) {
            if (SystemClock.elapsedRealtime() - started > 8_000) break
            try {
                val stats = manager.queryStatsForPackage(StorageManager.UUID_DEFAULT, info.packageName, Process.myUserHandle())
                apps += info to stats
                scanned++
            } catch (_: Exception) { }
        }
        result += Section("Ứng dụng dùng nhiều dung lượng", listOf(
            Field("Phạm vi", "${scanned} ứng dụng Android cho phép nhìn thấy; danh sách có thể thiếu")
        ))
        apps.sortedByDescending { it.second.appBytes + it.second.dataBytes }.take(12).forEach { (info, stats) ->
            val label = try { info.applicationInfo?.loadLabel(pm)?.toString() ?: info.packageName } catch (_: Exception) { info.packageName }
            val system = ((info.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            result += Section(label, listOf(
                Field("Loại", if (system) "Ứng dụng hệ thống" else "Ứng dụng người dùng"),
                Field("Package", info.packageName),
                Field("Tổng", bytes(stats.appBytes + stats.dataBytes)),
                Field("Mã", bytes(stats.appBytes)), Field("Dữ liệu", bytes(stats.dataBytes)),
                Field("Cache", bytes(stats.cacheBytes))
            ))
        }
        return result
    }

    fun sensors(): List<Section> = context.getSystemService(SensorManager::class.java)
        .getSensorList(Sensor.TYPE_ALL).map { s ->
            Section(s.name, listOf(
                Field("Sensor ID", s.id.toString()), Field("Sensor Name", s.name),
                Field("Sensor Type", "${s.type} · ${s.stringType.substringAfterLast('.') }"),
                Field("Vendor / OEM", s.vendor), Field("Version", s.version.toString()),
                Field("Resolution", s.resolution.toString()), Field("Power", "${s.power} mA"),
                Field("Maximum Range", s.maximumRange.toString()),
                Field("isDynamicSensor", yesNo(s.isDynamicSensor)),
                Field("isWakeUpSensor", yesNo(s.isWakeUpSensor)),
                Field("Reporting Mode", when (s.reportingMode) {
                    Sensor.REPORTING_MODE_CONTINUOUS -> "Continuous"
                    Sensor.REPORTING_MODE_ON_CHANGE -> "On change"
                    Sensor.REPORTING_MODE_ONE_SHOT -> "One shot"
                    Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> "Special trigger"
                    else -> "${s.reportingMode}"
                }),
                Field("FIFO tối đa", s.fifoMaxEventCount.toString()),
                Field("Độ trễ tối thiểu", "${s.minDelay} µs")
            ))
        }

    fun features(): List<Section> {
        val features = listOf(
            "Wi-Fi" to PackageManager.FEATURE_WIFI, "Wi-Fi Direct" to PackageManager.FEATURE_WIFI_DIRECT,
            "Bluetooth" to PackageManager.FEATURE_BLUETOOTH, "Bluetooth LE" to PackageManager.FEATURE_BLUETOOTH_LE,
            "GPS" to PackageManager.FEATURE_LOCATION_GPS, "Camera Flash" to PackageManager.FEATURE_CAMERA_FLASH,
            "Camera Front" to PackageManager.FEATURE_CAMERA_FRONT, "Microphone" to PackageManager.FEATURE_MICROPHONE,
            "NFC" to PackageManager.FEATURE_NFC, "USB Host" to PackageManager.FEATURE_USB_HOST,
            "USB Accessory" to PackageManager.FEATURE_USB_ACCESSORY, "MultiTouch" to PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH,
            "Audio Low Latency" to PackageManager.FEATURE_AUDIO_LOW_LATENCY,
            "Audio Output" to PackageManager.FEATURE_AUDIO_OUTPUT,
            "Professional Audio" to PackageManager.FEATURE_AUDIO_PRO,
            "Consumer IR" to PackageManager.FEATURE_CONSUMER_IR,
            "Gamepad" to PackageManager.FEATURE_GAMEPAD,
            "HIFI Sensor" to PackageManager.FEATURE_HIFI_SENSORS,
            "Printing" to PackageManager.FEATURE_PRINTING,
            "CDMA" to PackageManager.FEATURE_TELEPHONY_CDMA,
            "GSM" to PackageManager.FEATURE_TELEPHONY_GSM,
            "Fingerprint" to PackageManager.FEATURE_FINGERPRINT,
            "App Widgets" to PackageManager.FEATURE_APP_WIDGETS,
            "SIP" to PackageManager.FEATURE_SIP,
            "SIP VOIP" to PackageManager.FEATURE_SIP_VOIP
        )
        return listOf(Section("Tính năng phần cứng & hệ thống", features.map { (name, code) -> Field(name, yesNo(pm.hasSystemFeature(code))) }))
    }

    fun network(hasLocation: Boolean, hasPhone: Boolean): List<Section> {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val active = cm.activeNetwork
        val caps = active?.let(cm::getNetworkCapabilities)
        val link = active?.let(cm::getLinkProperties)
        val transport = when {
            caps == null -> "Ngoại tuyến"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Di động"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Khác"
        }
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        @Suppress("DEPRECATION") val wifiInfo = if (hasLocation) wifi?.connectionInfo else null
        val telephony = context.getSystemService(TelephonyManager::class.java)
        val mobileType = if (hasPhone) try { networkTypeName(telephony.dataNetworkType) } catch (_: SecurityException) { "Lỗi: thiếu quyền điện thoại" } else "Cần quyền điện thoại"
        val rows = listOf(
            Field("Connection Status", if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) "Có internet" else if (active != null) "Đã kết nối, chưa xác thực internet" else "Không kết nối"),
            Field("Data Type / Network Type", "$transport / $mobileType"),
            Field("IP Address", link?.linkAddresses?.joinToString { it.address.hostAddress ?: "" } ?: unavailable),
            Field("DNS", link?.dnsServers?.joinToString { it.hostAddress ?: "" } ?: unavailable),
            Field("SSID", if (hasLocation) wifiInfo?.ssid ?: unavailable else "Cần quyền vị trí"),
            Field("Wi-Fi MAC Address", "Android giới hạn MAC phần cứng; MAC kết nối có thể được ngẫu nhiên hóa"),
            Field("Link Speed", wifiInfo?.linkSpeed?.let { "$it Mbps" } ?: unavailable),
            Field("Wi-Fi frequency", wifiInfo?.frequency?.let { "$it MHz" } ?: unavailable),
            Field("Wi-Fi RSSI", wifiInfo?.rssi?.let { "$it dBm" } ?: unavailable),
            Field("Wi-Fi standard", if (Build.VERSION.SDK_INT >= 30 && wifiInfo != null) when (wifiInfo.wifiStandard) {
                ScanResult.WIFI_STANDARD_11N -> "Wi-Fi 4 (802.11n)"
                ScanResult.WIFI_STANDARD_11AC -> "Wi-Fi 5 (802.11ac)"
                ScanResult.WIFI_STANDARD_11AX -> if (wifiInfo.frequency >= 5925) "Wi-Fi 6E (802.11ax)" else "Wi-Fi 6 (802.11ax)"
                7 -> "802.11ad (60 GHz)"
                8 -> "Wi-Fi 7 (802.11be)"
                else -> "Mã ${wifiInfo.wifiStandard}"
            } else unavailable),
            Field("Nhà mạng", telephony.networkOperatorName?.takeIf { it.isNotBlank() } ?: unavailable),
            Field("Loại SIM", when (telephony.phoneType) { TelephonyManager.PHONE_TYPE_GSM -> "GSM"; TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"; else -> "Không rõ" }),
            Field("Phần cứng Wi-Fi", yesNo(pm.hasSystemFeature(PackageManager.FEATURE_WIFI))),
            Field("Phần cứng di động", yesNo(pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))),
            Field("Khả năng 4G/5G", "Android chỉ công khai loại mạng hiện đang dùng; thiết bị không tiết lộ đầy đủ băng tần modem")
        )
        val traffic = mutableListOf(
            Field("Thiết bị nhận từ lúc khởi động", trafficBytes(TrafficStats.getTotalRxBytes())),
            Field("Thiết bị gửi từ lúc khởi động", trafficBytes(TrafficStats.getTotalTxBytes())),
            Field("App nhận từ lúc khởi động", trafficBytes(TrafficStats.getUidRxBytes(Process.myUid()))),
            Field("App gửi từ lúc khởi động", trafficBytes(TrafficStats.getUidTxBytes(Process.myUid())))
        )
        if (hasUsageAccess()) {
            val start = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            val end = System.currentTimeMillis()
            val manager = context.getSystemService(NetworkStatsManager::class.java)
            listOf(ConnectivityManager.TYPE_WIFI to "Wi-Fi tháng này", ConnectivityManager.TYPE_MOBILE to "Di động tháng này").forEach { (type, label) ->
                try {
                    val bucket = manager.querySummaryForDevice(type, null, start, end)
                    traffic += Field(label, "Nhận ${bytes(bucket.rxBytes)} · Gửi ${bytes(bucket.txBytes)}")
                } catch (e: Exception) { traffic += Field(label, "Lỗi ${e.javaClass.simpleName}: thiết bị không cung cấp") }
            }
        } else traffic += Field("Thống kê tháng", "Cần bật Usage Access trong cài đặt Android")
        return listOf(Section("Kết nối & phần cứng mạng", rows), Section("Dữ liệu sử dụng", traffic))
    }

    fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    }

    private fun networkTypeName(type: Int): String = when (type) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
        TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> "Không rõ"
        else -> "Loại $type"
    }

    private fun trafficBytes(value: Long) = if (value < 0) unavailable else bytes(value)
    private fun percent(value: Long, total: Long): String = if (total > 0) {
        "%.1f%%".format(Locale.US, value.toDouble() * 100.0 / total)
    } else unavailable

    private fun zramSection(): Section {
        // ZRAM is compressed swap in RAM, so its figures must never be added to disk usage.
        val devices = (0..7).mapNotNull { index ->
            File("/sys/block/zram$index").takeIf { it.exists() }?.let { index to it }
        }
        val rows = mutableListOf<Field>()
        for ((index, device) in devices) {
            val size = readFirstLine("${device.path}/disksize")?.trim()?.toLongOrNull()
            val stats = readFirstLine("${device.path}/mm_stat")
                ?.trim()?.split(Regex("\\s+"))?.mapNotNull { it.toLongOrNull() }
            if (size == null || stats == null || stats.size < 3) {
                rows += Field("ZRAM $index", "Không đọc được thông số; thiết bị có thể giới hạn quyền truy cập")
                continue
            }
            if (size <= 0) {
                rows += Field("ZRAM $index", "Chưa kích hoạt")
                continue
            }
            rows += Field("ZRAM $index · sức chứa logic", bytes(size))
            rows += Field("ZRAM $index · dữ liệu chưa nén", "${bytes(stats[0])} · ${percent(stats[0], size)} sức chứa")
            rows += Field("ZRAM $index · RAM thực dùng", bytes(stats[2]))
        }
        if (devices.isEmpty()) rows += Field("ZRAM", "Không tìm thấy hoặc Android không cho ứng dụng đọc")
        val memInfo = try { File("/proc/meminfo").readLines() } catch (_: Exception) { emptyList() }
        fun memInfoBytes(key: String): Long? = memInfo.firstOrNull { it.startsWith("$key:") }
            ?.substringAfter(':')?.trim()?.substringBefore(' ')?.toLongOrNull()?.times(1024)
        val swapTotal = memInfoBytes("SwapTotal")
        val swapFree = memInfoBytes("SwapFree")
        if (swapTotal != null && swapFree != null) {
            rows += Field("Swap toàn hệ thống", "${bytes((swapTotal - swapFree).coerceAtLeast(0))} / ${bytes(swapTotal)} · có thể gồm vùng ngoài ZRAM")
        }
        rows += Field("Phân loại", "ZRAM là bộ nhớ hoán đổi nén trong RAM; không cộng vào tỷ lệ bộ nhớ trong")
        return Section("ZRAM & swap · ngoài bộ nhớ trong", rows)
    }

    private fun duration(seconds: Long): String = "${seconds / 86_400} ngày ${seconds % 86_400 / 3_600} giờ ${seconds % 3_600 / 60} phút"
    private fun widevine(): Section {
        val uuid = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")
        return try {
            if (!MediaDrm.isCryptoSchemeSupported(uuid)) Section("DRM / Widevine", listOf(Field("Trạng thái", "Không hỗ trợ")))
            else {
                val drm = MediaDrm(uuid)
                try {
                    Section("DRM / Widevine", listOf(
                        Field("Vendor", drm.getPropertyString(MediaDrm.PROPERTY_VENDOR)),
                        Field("Version", drm.getPropertyString(MediaDrm.PROPERTY_VERSION)),
                        Field("Security level", drm.getPropertyString("securityLevel")),
                        Field("Description", drm.getPropertyString(MediaDrm.PROPERTY_DESCRIPTION))
                    ))
                } finally { drm.release() }
            }
        } catch (e: Exception) { Section("DRM / Widevine", listOf(Field("Lỗi", "${e.javaClass.simpleName}: không đọc được DRM"))) }
    }
    private fun gpuStrings(): List<String> {
        var display = EGL14.EGL_NO_DISPLAY
        var surface = EGL14.EGL_NO_SURFACE
        var glContext = EGL14.EGL_NO_CONTEXT
        return try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY || !EGL14.eglInitialize(display, null, 0, null, 0)) throw IllegalStateException("EGL unavailable")
            val config = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            val attrs = intArrayOf(EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE)
            if (!EGL14.eglChooseConfig(display, attrs, 0, config, 0, 1, count, 0) || count[0] == 0) throw IllegalStateException("No GLES config")
            surface = EGL14.eglCreatePbufferSurface(display, config[0], intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0)
            glContext = EGL14.eglCreateContext(display, config[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
            if (!EGL14.eglMakeCurrent(display, surface, surface, glContext)) throw IllegalStateException("No GL context")
            listOf(GLES20.GL_RENDERER, GLES20.GL_VENDOR, GLES20.GL_VERSION, GLES20.GL_SHADING_LANGUAGE_VERSION)
                .map { GLES20.glGetString(it) ?: unavailable }
        } catch (e: Exception) { listOf("Lỗi: ${e.javaClass.simpleName}", unavailable, unavailable, unavailable) }
        finally {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
                if (glContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, glContext)
                EGL14.eglTerminate(display)
            }
        }
    }
    private fun yesNo(value: Boolean) = if (value) "Có" else "Không"
    private fun versionName(): String = when (Build.VERSION.SDK_INT) {
        24, 25 -> "Nougat"; 26, 27 -> "Oreo"; 28 -> "Pie"; 29 -> "Android 10"
        30 -> "Android 11"; 31, 32 -> "Android 12"; 33 -> "Android 13"
        34 -> "Android 14"; 35 -> "Android 15"; 36 -> "Android 16"
        37 -> "Android 17 · Cinnamon Bun"
        else -> "Android ${Build.VERSION.RELEASE}"
    }
    @Suppress("DEPRECATION")
    private fun serialNumber(): String = if (Build.VERSION.SDK_INT >= 29) {
        "Android 10+ giới hạn cho ứng dụng thường"
    } else try {
        (if (Build.VERSION.SDK_INT >= 26) Build.getSerial() else Build.SERIAL)
            .takeUnless { it.isBlank() || it == Build.UNKNOWN } ?: unavailable
    } catch (e: SecurityException) { "Lỗi quyền: ${e.javaClass.simpleName}" }
    private fun readFirstLine(path: String) = try { File(path).bufferedReader().use { it.readLine() } } catch (_: Exception) { null }
    private fun directorySize(file: File): Long = try { file.walkBottomUp().filter { it.isFile }.sumOf { it.length() } } catch (_: Exception) { 0 }
}
