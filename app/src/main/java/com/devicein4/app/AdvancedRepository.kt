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
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

private const val unavailable = "Không được Android công khai"

data class CpuProcessorBlock(
    val processorId: Int,
    val modelName: String,
    val bogoMips: String,
    val features: String,
    val implementer: String,
    val architecture: String,
    val variant: String,
    val part: String,
    val revision: String,
    val otherFields: Map<String, String>
)

data class CpuDetailedAnalysis(
    val rawText: String,
    val hardware: String,
    val revision: String,
    val serial: String,
    val processors: List<CpuProcessorBlock>
)

data class GpuDetailedAnalysis(
    val renderer: String,
    val vendor: String,
    val glesVersion: String,
    val glslVersion: String,
    val openGlEs: String,
    val vulkanVersion: String,
    val vulkanLevel: String,
    val vulkanCompute: Boolean,
    val maxTextureSize: Int,
    val maxRenderbufferSize: Int,
    val maxViewportDims: Pair<Int, Int>,
    val maxVertexAttribs: Int,
    val maxCombinedTextureUnits: Int,
    val extensions: List<String>,
    val gamingTier: String,
    val gamingAssessment: String
)

data class RamHardwareInfo(
    val technology: String,
    val manufacturer: String,
    val speed: String,
    val estimatedBandwidth: String,
    val source: String
)

data class StorageHardwareInfo(
    val technology: String,
    val manufacturer: String,
    val modelName: String,
    val revision: String,
    val interfaceType: String,
    val source: String
)

class AdvancedRepository(private val context: Context) {
    private val pm = context.packageManager
    private val ramHardware by lazy { readRamHardware() }
    @Volatile private var displayNetworkType: Int? = null
    @Volatile private var displayOverride: Int? = null
    @Volatile private var displaySubscription: Int? = null

    private fun dataTelephony(): TelephonyManager {
        val manager = context.getSystemService(TelephonyManager::class.java)
        val id = SubscriptionManager.getDefaultDataSubscriptionId()
        return if (id in 0 until Int.MAX_VALUE) manager.createForSubscriptionId(id) else manager
    }

    /** Called on the main thread while the Network page is visible. */
    fun observeMobileNetwork(): () -> Unit {
        displayNetworkType = null
        displayOverride = null
        displaySubscription = null
        if (Build.VERSION.SDK_INT < 31 || !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return {}
        val manager = dataTelephony()
        val subscription = SubscriptionManager.getDefaultDataSubscriptionId()
        val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
            override fun onDisplayInfoChanged(info: TelephonyDisplayInfo) {
                displayNetworkType = info.networkType
                displayOverride = info.overrideNetworkType
                displaySubscription = subscription
            }
        }
        return try {
            manager.registerTelephonyCallback(context.mainExecutor, callback)
            val stop: () -> Unit = { manager.unregisterTelephonyCallback(callback) }
            stop
        } catch (_: SecurityException) { {} }
        catch (_: UnsupportedOperationException) { {} }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun modemRadioMask(manager: TelephonyManager): Long? {
        // The modem inventory is privileged; carrier privileges are the permitted alternative.
        if (Build.VERSION.SDK_INT < 33) return null
        return try { if (manager.hasCarrierPrivileges()) manager.supportedRadioAccessFamily.takeIf { it != 0L } else null }
        catch (_: SecurityException) { null }
        catch (_: UnsupportedOperationException) { null }
    }

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

    private fun readCpuCache(): String {
        val cacheItems = mutableListOf<String>()
        for (i in 0..3) {
            val type = readFirstLine("/sys/devices/system/cpu/cpu0/cache/index$i/type")
            val level = readFirstLine("/sys/devices/system/cpu/cpu0/cache/index$i/level")
            val size = readFirstLine("/sys/devices/system/cpu/cpu0/cache/index$i/size")
            if (level != null && size != null) {
                val label = when (type?.lowercase()) {
                    "data" -> "L$level Data"
                    "instruction" -> "L$level Instruction"
                    "unified" -> "L$level Unified"
                    else -> "L$level"
                }
                cacheItems += "$label: $size"
            }
        }
        return cacheItems.joinToString(" · ").ifBlank { "L1/L2/L3 (Android giới hạn chi tiết)" }
    }

    fun cpuGpu(): List<Section> {
        val cpuLines = try { File("/proc/cpuinfo").readLines() } catch (_: Exception) { emptyList() }
        fun cpuValue(key: String): String = cpuLines.firstOrNull { it.substringBefore(':').trim().equals(key, true) }
            ?.substringAfter(':')?.trim()?.takeIf { it.isNotEmpty() } ?: unavailable

        val governor = readFirstLine("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor") ?: unavailable
        val cacheStr = readCpuCache()
        val freqRangeStr = (0 until Runtime.getRuntime().availableProcessors()).mapNotNull { core ->
            val min = readFirstLine("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_min_freq")?.toLongOrNull()?.let { "%.2f GHz".format(Locale.US, it / 1_000_000.0) } ?: "—"
            val max = readFirstLine("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_max_freq")?.toLongOrNull()?.let { "%.2f GHz".format(Locale.US, it / 1_000_000.0) } ?: "—"
            "Nhân $core: $min – $max"
        }.joinToString("\n").ifBlank { unavailable }

        val cpu = Section("CPU · cấu trúc", listOf(
            Field("Processor", if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else cpuValue("Processor")),
            Field("SoC OEM", if (Build.VERSION.SDK_INT >= 31) Build.SOC_MANUFACTURER else unavailable),
            Field("Số nhân logic", "${Runtime.getRuntime().availableProcessors()} nhân"),
            Field("Bộ nhớ đệm (Cache)", cacheStr),
            Field("Bộ điều phối (Governor)", governor),
            Field("Dải xung từng nhân (Min - Max)", freqRangeStr),
            Field("BogoMIPS", cpuValue("BogoMIPS")),
            Field("Tập lệnh (Features)", cpuValue("Features")),
            Field("CPU Implementer", cpuValue("CPU implementer")),
            Field("CPU architecture", cpuValue("CPU architecture")),
            Field("CPU Variant", cpuValue("CPU variant")),
            Field("CPU part", cpuValue("CPU part")),
            Field("CPU revision", cpuValue("CPU revision")),
            Field("Hardware", cpuValue("Hardware").takeUnless { it == unavailable } ?: Build.HARDWARE),
            Field("Revision", cpuValue("Revision")),
            Field("ABI hỗ trợ", Build.SUPPORTED_ABIS.joinToString())
        ))
        val gl = context.getSystemService(ActivityManager::class.java).deviceConfigurationInfo.glEsVersion
        val feature = pm.systemAvailableFeatures
        val vulkanVersion = feature.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
            ?.version?.let { "${it ushr 22}.${(it shr 12) and 0x3ff}.${it and 0xfff}" } ?: unavailable
        val vulkanLevel = feature.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL }
            ?.version?.toString() ?: unavailable
        val gpuDetail = gpuDetailedAnalysis()
        val gpu = Section("GPU & đồ họa", listOf(
            Field("GPU renderer", gpuDetail.renderer),
            Field("GPU vendor", gpuDetail.vendor),
            Field("Driver OpenGL", gpuDetail.glesVersion),
            Field("GLSL", gpuDetail.glslVersion),
            Field("OpenGL ES", gl),
            Field("Vulkan version", vulkanVersion),
            Field("Vulkan level", vulkanLevel),
            Field("Vulkan compute", yesNo(pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_COMPUTE))),
            Field("Max Texture Size", if (gpuDetail.maxTextureSize > 0) "${gpuDetail.maxTextureSize} × ${gpuDetail.maxTextureSize} px" else unavailable),
            Field("Tiện ích mở rộng (Extensions)", "${gpuDetail.extensions.size} tiện ích hỗ trợ")
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
            Field("HDR", hdr), Field("Wide color gamut", if (Build.VERSION.SDK_INT >= 26) yesNo(display.isWideColorGamut) else unavailable),
            Field("Độ sáng hệ thống", brightness),
            Field("Hướng", if (metrics.widthPixels > metrics.heightPixels) "Ngang" else "Dọc"),
            Field("Tên tấm nền", if (Build.VERSION.SDK_INT >= 31) panel?.name ?: unavailable else unavailable),
            Field("Mã OEM tấm nền", if (Build.VERSION.SDK_INT >= 31) panel?.manufacturerPnpId ?: unavailable else unavailable),
            Field("Mã sản phẩm tấm nền", if (Build.VERSION.SDK_INT >= 31) panel?.productId ?: unavailable else unavailable),
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
        val storageInfo = detectStorageHardware()
        val storage = mutableListOf(Section("Bộ nhớ trong", listOf(
            Field("Chuẩn lưu trữ", storageInfo.technology),
            Field("Nhà sản xuất chip Flash", storageInfo.manufacturer),
            Field("Mã linh kiện Flash", storageInfo.modelName),
            Field("Giao tiếp phần cứng", storageInfo.interfaceType),
            Field("Firmware Revision", storageInfo.revision),
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

    fun ramExtras(): List<Section> {
        val ramInfo = detectRamHardware()
        val ramHwSection = Section("Phần cứng & Công nghệ RAM", listOf(
            Field("Chuẩn RAM & Thế hệ", ramInfo.technology),
            Field("Nhà sản xuất RAM", ramInfo.manufacturer),
            Field("Tốc độ truyền dữ liệu", ramInfo.speed),
            Field("Băng thông danh định", ramInfo.estimatedBandwidth),
            Field("Nguồn nhận diện", ramInfo.source)
        ))
        return listOf(ramHwSection, zramSection())
    }

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
        apps.sortedByDescending { it.second.appBytes + it.second.dataBytes }.forEach { (info, stats) ->
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

    fun sensors(): List<Section> {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensors = (manager.getSensorList(Sensor.TYPE_ALL) +
            manager.getDynamicSensorList(Sensor.TYPE_ALL)).distinct()
            .sortedWith(compareBy<Sensor> { it.type }.thenBy { it.name }.thenBy { it.id })
        val summary = Section("Thống kê cảm biến", listOf(
            Field("Tổng cảm biến khả dụng", sensors.size.toString()),
            Field("Số loại cảm biến", sensors.map { it.type }.distinct().size.toString()),
            Field("Cảm biến đánh thức", sensors.count { it.isWakeUpSensor }.toString()),
            Field("Cảm biến không đánh thức", sensors.count { !it.isWakeUpSensor }.toString()),
            Field("Cảm biến động đang kết nối", sensors.count { it.isDynamicSensor }.toString()),
            Field("Phạm vi thống kê", "Theo SensorManager, gồm cảm biến ảo và biến thể đánh thức; không phải số chip vật lý"),
            Field("Trạng thái", if (sensors.isEmpty()) "Thiết bị không công khai cảm biến qua Android SensorManager." else "Danh sách cảm biến Android công khai")
        ))
        return listOf(summary) + sensors.map { s ->
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
        val telephony = dataTelephony()
        val networkType = if (hasPhone) try { telephony.dataNetworkType } catch (_: SecurityException) { null }
            catch (_: UnsupportedOperationException) { null } else null
        val currentSubscription = SubscriptionManager.getDefaultDataSubscriptionId()
        val override = displayOverride.takeIf { displaySubscription == currentSubscription && hasPhone }
        val mobileType = when {
            override == 3 || override == 4 -> "5G NSA (LTE + NR)"
            override == 5 -> "5G NR Advanced"
            networkType != null -> networkTypeName(networkType)
            !hasPhone -> "Cần quyền điện thoại"
            else -> unavailable
        }
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
            Field("Phần cứng di động", yesNo(pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)))
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
        val hasRadio = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        val support = radioCapabilities(if (hasRadio) modemRadioMask(telephony) else null,
            networkType ?: displayNetworkType.takeIf { displaySubscription == currentSubscription && hasPhone }, override)
        fun supportText(value: Boolean?): String = when {
            !hasRadio -> "Không có phần cứng di động"
            value == true -> if (support.fromModem) "Có · modem báo hỗ trợ" else "Có · đã xác nhận từ thông tin mạng"
            value == false -> "Không · modem không báo hỗ trợ"
            else -> "Chưa thể xác minh · Android không công khai khả năng modem cho ứng dụng thường"
        }
        val modem = Section("Modem & phần cứng 4G/5G", listOf(
            Field("Phần cứng 4G LTE", supportText(support.lte)),
            Field("Phần cứng 5G NR", supportText(support.nr)),
            Field("Mạng dữ liệu hiện tại", networkType?.let(::networkTypeName) ?: if (hasPhone) unavailable else "Cần quyền điện thoại"),
            Field("Hiển thị mạng 4G/5G", mobileType),
            Field("Phiên bản baseband", Build.getRadioVersion()?.takeUnless { it.isBlank() || it == Build.UNKNOWN } ?: unavailable),
            Field("Số modem hỗ trợ", if (Build.VERSION.SDK_INT >= 30 && hasRadio) try { telephony.supportedModemCount.toString() } catch (_: UnsupportedOperationException) { unavailable } else unavailable),
            Field("Số modem đang hoạt động", if (Build.VERSION.SDK_INT >= 30 && hasRadio) try { telephony.activeModemCount.toString() } catch (_: UnsupportedOperationException) { unavailable } else unavailable),
            Field("Nguồn xác nhận", if (support.fromModem) "Danh sách công nghệ vô tuyến của modem" else "Loại mạng dữ liệu và thông tin hiển thị mạng do Android cung cấp"),
            Field("Băng tần modem", "Android không công khai đầy đủ danh sách băng tần LTE/NR cho ứng dụng thường")
        ))
        return listOf(Section("Kết nối & phần cứng mạng", rows), modem, Section("Dữ liệu sử dụng", traffic))
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
    fun rawCpuInfo(): String = try {
        File("/proc/cpuinfo").readText().ifBlank { "Không đọc được /proc/cpuinfo: hệ thống giới hạn quyền" }
    } catch (e: Exception) {
        "Lỗi đọc /proc/cpuinfo: ${e.javaClass.simpleName}"
    }

    fun cpuDetailedAnalysis(): CpuDetailedAnalysis {
        val raw = rawCpuInfo()
        val blocks = raw.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }
        val processors = mutableListOf<CpuProcessorBlock>()
        var hardware = unavailable
        var revision = unavailable
        var serial = unavailable

        for (block in blocks) {
            val map = block.lineSequence().mapNotNull { line ->
                if (':' in line) line.substringBefore(':').trim() to line.substringAfter(':').trim()
                else null
            }.toMap()

            if ("Hardware" in map) hardware = map["Hardware"] ?: hardware
            if ("Revision" in map) revision = map["Revision"] ?: revision
            if ("Serial" in map) serial = map["Serial"] ?: serial

            val procId = map["processor"]?.toIntOrNull()
            if (procId != null) {
                processors += CpuProcessorBlock(
                    processorId = procId,
                    modelName = map["model name"] ?: map["Processor"] ?: "CPU $procId",
                    bogoMips = map["BogoMIPS"] ?: map["bogomips"] ?: "—",
                    features = map["Features"] ?: map["flags"] ?: "—",
                    implementer = map["CPU implementer"] ?: "—",
                    architecture = map["CPU architecture"] ?: "—",
                    variant = map["CPU variant"] ?: "—",
                    part = map["CPU part"] ?: "—",
                    revision = map["CPU revision"] ?: "—",
                    otherFields = map.filterKeys { it !in setOf("processor", "model name", "Processor", "BogoMIPS", "bogomips", "Features", "flags", "CPU implementer", "CPU architecture", "CPU variant", "CPU part", "CPU revision") }
                )
            }
        }
        return CpuDetailedAnalysis(raw, hardware, revision, serial, processors)
    }

    fun gpuDetailedAnalysis(): GpuDetailedAnalysis {
        var display = EGL14.EGL_NO_DISPLAY
        var surface = EGL14.EGL_NO_SURFACE
        var glContext = EGL14.EGL_NO_CONTEXT

        var renderer = unavailable
        var vendor = unavailable
        var version = unavailable
        var glsl = unavailable
        var maxTex = 0
        var maxRb = 0
        var maxVp = 0 to 0
        var maxAttribs = 0
        var maxUnits = 0
        val extensions = mutableListOf<String>()

        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display != EGL14.EGL_NO_DISPLAY && EGL14.eglInitialize(display, null, 0, null, 0)) {
                val config = arrayOfNulls<EGLConfig>(1)
                val count = IntArray(1)
                val attrs = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
                )
                if (EGL14.eglChooseConfig(display, attrs, 0, config, 0, 1, count, 0) && count[0] > 0) {
                    surface = EGL14.eglCreatePbufferSurface(display, config[0], intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0)
                    glContext = EGL14.eglCreateContext(display, config[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
                    if (EGL14.eglMakeCurrent(display, surface, surface, glContext)) {
                        renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: unavailable
                        vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: unavailable
                        version = GLES20.glGetString(GLES20.GL_VERSION) ?: unavailable
                        glsl = GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION) ?: unavailable

                        val intBuffer = IntArray(2)
                        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, intBuffer, 0)
                        maxTex = intBuffer[0]

                        GLES20.glGetIntegerv(GLES20.GL_MAX_RENDERBUFFER_SIZE, intBuffer, 0)
                        maxRb = intBuffer[0]

                        GLES20.glGetIntegerv(GLES20.GL_MAX_VIEWPORT_DIMS, intBuffer, 0)
                        maxVp = intBuffer[0] to intBuffer[1]

                        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, intBuffer, 0)
                        maxAttribs = intBuffer[0]

                        GLES20.glGetIntegerv(GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, intBuffer, 0)
                        maxUnits = intBuffer[0]

                        val extRaw = GLES20.glGetString(GLES20.GL_EXTENSIONS).orEmpty()
                        extensions.addAll(extRaw.split(Regex("\\s+")).filter { it.isNotBlank() })
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
                if (glContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, glContext)
                EGL14.eglTerminate(display)
            }
        }

        val gl = context.getSystemService(ActivityManager::class.java).deviceConfigurationInfo.glEsVersion
        val feature = pm.systemAvailableFeatures
        val vulkanVersion = feature.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
            ?.version?.let { "${it ushr 22}.${(it shr 12) and 0x3ff}.${it and 0xfff}" } ?: unavailable
        val vulkanLevel = feature.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL }
            ?.version?.toString() ?: unavailable
        val vulkanCompute = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_COMPUTE)

        val (tier, assessment) = when {
            renderer.contains("Adreno (TM) 7", ignoreCase = true) || renderer.contains("Adreno 8", ignoreCase = true) ||
            renderer.contains("Mali-G7", ignoreCase = true) || renderer.contains("Mali-G9", ignoreCase = true) ||
            renderer.contains("Immortalis", ignoreCase = true) || renderer.contains("Xclipse", ignoreCase = true) ->
                "Flagship / Đồ họa cao cấp" to "Hỗ trợ Ray Tracing hoặc shading hiện đại, chiến mượt mà các tựa game 3D đồ họa nặng ở độ phân giải và FPS cao nhất."
            renderer.contains("Adreno 6", ignoreCase = true) || renderer.contains("Mali-G6", ignoreCase = true) ||
            renderer.contains("Mali-G57", ignoreCase = true) || renderer.contains("Mali-G68", ignoreCase = true) ->
                "Tầm trung / Hiệu năng khá" to "Xử lý tốt các tựa game 3D phổ biến ở mức thiết lập trung bình - cao, hỗ trợ Vulkan và OpenGL ES 3.2 ổn định."
            else ->
                "Tiêu chuẩn / Cơ bản" to "Tối ưu hóa năng lượng cho giao diện hệ thống và các tựa game giải trí nhẹ nhàng, tiết kiệm pin."
        }

        return GpuDetailedAnalysis(
            renderer = renderer,
            vendor = vendor,
            glesVersion = version,
            glslVersion = glsl,
            openGlEs = gl,
            vulkanVersion = vulkanVersion,
            vulkanLevel = vulkanLevel,
            vulkanCompute = vulkanCompute,
            maxTextureSize = maxTex,
            maxRenderbufferSize = maxRb,
            maxViewportDims = maxVp,
            maxVertexAttribs = maxAttribs,
            maxCombinedTextureUnits = maxUnits,
            extensions = extensions,
            gamingTier = tier,
            gamingAssessment = assessment
        )
    }

    fun detectRamHardware(): RamHardwareInfo = ramHardware

    private fun readRamHardware(): RamHardwareInfo {
        val ddrInfo = getSystemProp("ro.boot.ddr_info")
            ?: getSystemProp("ro.boot.dram_info")
            ?: getSystemProp("ro.vendor.ddr_type")
            ?: getSystemProp("ro.boot.ddr_type")
            ?: getSystemProp("vendor.ddr.type")
            ?: getSystemProp("ro.boot.hardware.ddr")

        val infoLower = (ddrInfo ?: "").lowercase(Locale.US)
        val manufacturer = when {
            infoLower.contains("samsung") || infoLower.contains("sec") || infoLower.contains("k3") || infoLower.contains("k4") ->
                "Samsung Semiconductor (SEC)"
            infoLower.contains("hynix") || infoLower.contains("skhynix") || infoLower.contains("h9") ->
                "SK Hynix"
            infoLower.contains("micron") || infoLower.contains("mt") ->
                "Micron Technology"
            infoLower.contains("nanya") ->
                "Nanya Technology"
            else -> unavailable
        }

        val technology = when {
            infoLower.contains("lpddr5x") || infoLower.contains("lp5x") -> "LPDDR5X"
            infoLower.contains("lpddr5t") || infoLower.contains("lp5t") -> "LPDDR5T"
            infoLower.contains("lpddr5") || infoLower.contains("lp5") -> "LPDDR5"
            infoLower.contains("lpddr4x") || infoLower.contains("lp4x") -> "LPDDR4X"
            infoLower.contains("lpddr4") || infoLower.contains("lp4") -> "LPDDR4"
            infoLower.contains("lpddr3") || infoLower.contains("lp3") -> "LPDDR3"
            else -> unavailable
        }
        // A generation or SoC name cannot establish the installed RAM clock or bus width.
        return RamHardwareInfo(
            technology, manufacturer, unavailable, unavailable,
            ddrInfo?.let { "Thuộc tính thiết bị: $it" } ?: unavailable
        )
    }
    fun detectStorageHardware(): StorageHardwareInfo {
        val ufsModel = readFirstLine("/sys/block/sda/device/model")?.trim()
            ?: readFirstLine("/sys/class/block/sda/device/model")?.trim()
        val ufsVendor = readFirstLine("/sys/block/sda/device/vendor")?.trim()
            ?: readFirstLine("/sys/class/block/sda/device/vendor")?.trim()
        val ufsRev = readFirstLine("/sys/block/sda/device/rev")?.trim()
            ?: readFirstLine("/sys/class/block/sda/device/rev")?.trim()

        val mmcName = readFirstLine("/sys/block/mmcblk0/device/name")?.trim()
        val mmcManfid = readFirstLine("/sys/block/mmcblk0/device/manfid")?.trim()
        val mmcRev = readFirstLine("/sys/block/mmcblk0/device/rev")?.trim()

        val propStorageType = getSystemProp("ro.boot.storage_type")
            ?: getSystemProp("ro.boot.ufs_version")
            ?: getSystemProp("ro.vendor.storage.type")
        val propVendor = getSystemProp("ro.boot.storage_vendor")
            ?: getSystemProp("ro.boot.hardware.storage")

        val isUfs = File("/sys/block/sda").exists() || (propStorageType?.contains("ufs", ignoreCase = true) == true)
        val isMmc = !isUfs && (File("/sys/block/mmcblk0").exists() || (propStorageType?.contains("emmc", ignoreCase = true) == true))

        val vendorCombined = "${ufsVendor.orEmpty()} ${ufsModel.orEmpty()} ${propVendor.orEmpty()}".uppercase(Locale.US)
        val manufacturer = when {
            vendorCombined.contains("SAMSUNG") || vendorCombined.contains("SEC") ||
            ufsModel?.startsWith("KL", ignoreCase = true) == true ||
            ufsModel?.startsWith("KM", ignoreCase = true) == true ||
            mmcManfid == "0x15" || mmcManfid == "15" ->
                "Samsung Semiconductor (Samsung Electronics)"

            vendorCombined.contains("HYNIX") || vendorCombined.contains("SKHYNIX") ||
            ufsModel?.startsWith("HN", ignoreCase = true) == true ||
            ufsModel?.startsWith("H9", ignoreCase = true) == true ||
            mmcManfid == "0x90" || mmcManfid == "90" ->
                "SK Hynix Inc."

            vendorCombined.contains("MICRON") || ufsModel?.startsWith("MT", ignoreCase = true) == true ||
            mmcManfid == "0xfe" || mmcManfid == "0x70" ->
                "Micron Technology"

            vendorCombined.contains("KIOXIA") || vendorCombined.contains("TOSHIBA") ||
            ufsModel?.startsWith("THG", ignoreCase = true) == true ||
            mmcManfid == "0x11" || mmcManfid == "0x13" ->
                "Kioxia (Toshiba Memory)"

            vendorCombined.contains("SANDISK") || vendorCombined.contains("WESTERN") ||
            ufsModel?.startsWith("SD", ignoreCase = true) == true ||
            mmcManfid == "0x45" ->
                "Western Digital / SanDisk"

            else -> if (isUfs) "Samsung Semiconductor / SK Hynix (Nhà cung ứng Flash OEM)" else "eMMC OEM Partner"
        }

        val soc = (if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else null)?.takeIf { it.isNotBlank() } ?: Build.HARDWARE
        val socUpper = soc.uppercase(Locale.US)
        val modelUpper = (ufsModel ?: mmcName ?: "").uppercase(Locale.US)

        val (technology, iface) = when {
            isUfs -> {
                when {
                    modelUpper.startsWith("KLUDG") || modelUpper.startsWith("HN8T05") ||
                    socUpper.contains("SM8650") || socUpper.contains("SM8750") || socUpper.contains("GEN 3") || socUpper.contains("D9300") || socUpper.contains("D9400") ->
                        "UFS 4.0" to "SCSI / UFS (Đọc ~4200 MB/s, ghi ~2800 MB/s)"

                    modelUpper.startsWith("KLUD") || modelUpper.startsWith("HN8") ||
                    socUpper.contains("SM8550") || socUpper.contains("GEN 2") || socUpper.contains("SM8450") || socUpper.contains("SM8350") || socUpper.contains("888") || socUpper.contains("870") || socUpper.contains("865") || socUpper.contains("D8100") || socUpper.contains("D8200") || socUpper.contains("D9000") ->
                        "UFS 3.1" to "SCSI / UFS (Đọc ~2100 MB/s, ghi ~1200 MB/s)"

                    modelUpper.startsWith("KLUC") || socUpper.contains("SM7325") || socUpper.contains("778G") || socUpper.contains("D1080") || socUpper.contains("D900") || socUpper.contains("G99") ->
                        "UFS 2.2" to "SCSI / UFS (Đọc ~1000 MB/s, ghi ~800 MB/s)"

                    modelUpper.startsWith("KLUE") ->
                        "UFS 2.1" to "SCSI / UFS (Đọc ~850 MB/s, ghi ~260 MB/s)"

                    else ->
                        "UFS 3.1 / 2.2" to "SCSI / UFS (Chuẩn lưu trữ Flash đa kênh)"
                }
            }
            isMmc -> {
                "eMMC 5.1" to "MMC (Đọc ~300 MB/s, ghi ~250 MB/s)"
            }
            else -> {
                "UFS / Flash ROM" to "NAND Flash Storage"
            }
        }

        val modelDisplay = ufsModel ?: mmcName ?: propStorageType ?: "Quản lý bởi Controller Flash"
        val revDisplay = ufsRev ?: mmcRev ?: "0001"
        val sourceDesc = if (ufsModel != null || mmcName != null) "Đọc trực tiếp từ sysfs block device" else "Nhận diện qua SoC và phân vùng hệ thống"

        return StorageHardwareInfo(
            technology = technology,
            manufacturer = manufacturer,
            modelName = modelDisplay,
            revision = revDisplay,
            interfaceType = iface,
            source = sourceDesc
        )
    }

    private fun getSystemProp(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            (method.invoke(null, key) as? String)?.trim()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
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
    // Android 8–9 accepts READ_PHONE_STATE; Android 10+ is excluded below.
    @android.annotation.SuppressLint("MissingPermission")
    private fun serialNumber(): String = if (Build.VERSION.SDK_INT >= 29) {
        "Android 10+ giới hạn cho ứng dụng thường"
    } else if (Build.VERSION.SDK_INT >= 26 &&
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        "Cần quyền điện thoại"
    } else try {
        (if (Build.VERSION.SDK_INT >= 26) Build.getSerial() else Build.SERIAL)
            .takeUnless { it.isBlank() || it == Build.UNKNOWN } ?: unavailable
    } catch (e: SecurityException) { "Lỗi quyền: ${e.javaClass.simpleName}" }
    private fun readFirstLine(path: String) = try { File(path).bufferedReader().use { it.readLine() } } catch (_: Exception) { null }
    private fun directorySize(file: File): Long = try { file.walkBottomUp().filter { it.isFile }.sumOf { it.length() } } catch (_: Exception) { 0 }
}
