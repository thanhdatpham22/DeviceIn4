package com.devicein4.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

data class Field(val label: String, val value: String)
data class Section(val title: String, val fields: List<Field>)
data class MemorySample(
    val total: Long,
    val available: Long,
    val threshold: Long,
    val low: Boolean,
    val appUsed: Long,
    val advertised: Long?
) {
    val used: Long get() = (total - available).coerceAtLeast(0)
    val percent: Float get() = if (total > 0) used.toFloat() / total else 0f
}

data class BatteryLive(val level: Int, val currentMa: Double?, val averageMa: Double?, val voltageV: Double?, val powerW: Double?, val status: Int)
data class WifiLive(val connected: Boolean, val linkMbps: Int?, val frequencyMHz: Int?, val bandwidthMbps: Int?)
data class CpuCore(
    val index: Int,
    val currentKhz: Long?,
    val maxKhz: Long?,
    val name: String,
    val minKhz: Long? = null,
    val governor: String? = null
) {
    val fraction get() = if (currentKhz != null && maxKhz != null && maxKhz > 0) (currentKhz.toFloat() / maxKhz).coerceIn(0f, 1f) else 0f
    val cluster get() = when {
        maxKhz == null -> "Logic"
        maxKhz >= 2_800_000L -> "Prime"
        maxKhz >= 2_200_000L -> "Big"
        else -> "Little"
    }
}

class DeviceRepository(private val context: Context) {
    private val activityManager = context.getSystemService(ActivityManager::class.java)
    private val batteryManager = context.getSystemService(BatteryManager::class.java)
    private val coreNames by lazy {
        readText("/proc/cpuinfo").orEmpty().split(Regex("\\n\\s*\\n")).mapNotNull { block ->
            val fields = block.lineSequence().mapNotNull { line ->
                if (':' in line) line.substringBefore(':').trim().lowercase() to line.substringAfter(':').trim()
                else null
            }.toMap()
            val index = fields["processor"]?.toIntOrNull() ?: return@mapNotNull null
            val name = fields["model name"]?.takeIf { it.isNotBlank() }
                ?: fields["cpu model"]?.takeIf { it.isNotBlank() }
            index to (name?.take(18) ?: "Core ${index + 1}")
        }.toMap()
    }

    fun memory(): MemorySample {
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        val runtime = Runtime.getRuntime()
        return MemorySample(
            info.totalMem, info.availMem, info.threshold, info.lowMemory,
            runtime.totalMemory() - runtime.freeMemory(),
            if (Build.VERSION.SDK_INT >= 34) info.advertisedMem.takeIf { it > 0 } else null
        )
    }

    fun cpuLive(): List<CpuCore> = (0 until Runtime.getRuntime().availableProcessors()).map { index ->
        val base = "/sys/devices/system/cpu/cpu$index/cpufreq/"
        val cur = readText(base + "scaling_cur_freq")?.trim()?.toLongOrNull()
        val max = readText(base + "cpuinfo_max_freq")?.trim()?.toLongOrNull()
            ?: readText(base + "scaling_max_freq")?.trim()?.toLongOrNull()
        val min = readText(base + "cpuinfo_min_freq")?.trim()?.toLongOrNull()
            ?: readText(base + "scaling_min_freq")?.trim()?.toLongOrNull()
        val gov = readText(base + "scaling_governor")?.trim()
        CpuCore(index, cur, max, coreNames[index] ?: "Core ${index + 1}", min, gov)
    }

    fun wifiLive(): WifiLive {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.activeNetwork?.let(cm::getNetworkCapabilities)
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return WifiLive(false, null, null, null)
        val manager = context.applicationContext.getSystemService(WifiManager::class.java)
        @Suppress("DEPRECATION")
        val info = (if (Build.VERSION.SDK_INT >= 29) caps.transportInfo as? WifiInfo else null)
            ?: manager?.connectionInfo
        return WifiLive(true, info?.linkSpeed?.takeIf { it > 0 }, info?.frequency?.takeIf { it > 0 },
            caps.linkDownstreamBandwidthKbps.takeIf { it > 0 }?.div(1000))
    }

    fun batteryLive(): BatteryLive {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        fun property(key: Int) = batteryManager.getIntProperty(key).takeUnless { it == Int.MIN_VALUE }
        val current = property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.div(1000.0)
        val average = property(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)?.div(1000.0)
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }?.div(1000.0)
        return BatteryLive(if (scale > 0) level * 100 / scale else -1, current, average, voltage,
            if (current != null && voltage != null) current * voltage / 1000.0 else null,
            intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                ?: BatteryManager.BATTERY_STATUS_UNKNOWN)
    }

    fun device(): Section {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        context.getSystemService(WindowManager::class.java).defaultDisplay.getRealMetrics(metrics)
        val storage = StatFs(Environment.getDataDirectory().path)
        return Section("Thiết bị & hệ thống", listOf(
            Field("Hãng / mẫu", "${Build.MANUFACTURER} ${Build.MODEL}"),
            Field("Tên sản phẩm", Build.PRODUCT),
            Field("Thiết bị / bo mạch", "${Build.DEVICE} / ${Build.BOARD}"),
            Field("Android", "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}"),
            Field("Bản dựng", Build.DISPLAY),
            Field("Màn hình", "${metrics.widthPixels} × ${metrics.heightPixels} px · ${metrics.densityDpi} dpi"),
            Field("Bộ nhớ trong", "${bytes(storage.availableBytes)} trống / ${bytes(storage.totalBytes)}")
        ))
    }

    fun cpu(): Section {
        val raw = readText("/proc/cpuinfo")
        val model = raw?.lineSequence()?.firstOrNull {
            it.startsWith("Hardware", true) || it.startsWith("model name", true) || it.startsWith("Processor", true)
        }?.substringAfter(':')?.trim()?.takeIf { it.isNotEmpty() }
        val cores = Runtime.getRuntime().availableProcessors()
        val frequencies = (0 until cores).mapNotNull { core ->
            val khz = readText("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_max_freq")
                ?.trim()?.toLongOrNull()
            khz?.takeIf { it > 0 }?.let { "CPU $core: ${"%.2f".format(Locale.US, it / 1_000_000.0)} GHz" }
        }
        return Section("CPU & kiến trúc", listOf(
            Field("Chip / nền tảng", (if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL.takeIf { it.isNotBlank() } else null) ?: model ?: Build.HARDWARE),
            Field("Nhà sản xuất SoC", if (Build.VERSION.SDK_INT >= 31) Build.SOC_MANUFACTURER else "Không được cung cấp"),
            Field("Nhân logic", cores.toString()),
            Field("ABI hỗ trợ", Build.SUPPORTED_ABIS.joinToString()),
            Field("Kiến trúc 64 bit", if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "Có" else "Không"),
            Field("Xung tối đa từng nhân", frequencies.joinToString("\n").ifEmpty { "Thiết bị không công khai" }),
            Field("Hardware", Build.HARDWARE)
        ))
    }

    fun battery(): Section {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent == null) return Section("Pin", listOf(Field("Trạng thái", "Không đọc được dữ liệu pin")))
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Đang sạc"
            BatteryManager.BATTERY_STATUS_FULL -> "Đã đầy"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Đang xả"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Không sạc"
            else -> "Không rõ"
        }
        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Tốt"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Quá nóng"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Hỏng"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Quá áp"
            BatteryManager.BATTERY_HEALTH_COLD -> "Quá lạnh"
            else -> "Không rõ"
        }
        val plugged = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Nguồn AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Không dây"
            else -> "Không cắm sạc"
        }
        val current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            .takeUnless { it == Int.MIN_VALUE }
        val charge = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            .takeUnless { it == Int.MIN_VALUE }
        val cycleCount = if (Build.VERSION.SDK_INT >= 34) intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) else -1
        return Section("Pin & nguồn điện", listOf(
            Field("Dung lượng còn lại", if (level >= 0 && scale > 0) "${level * 100 / scale}%" else "Không rõ"),
            Field("Trạng thái", status), Field("Sức khỏe", health), Field("Nguồn", plugged),
            Field("Nhiệt độ", intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1).takeIf { it >= 0 }?.let(::temperature) ?: "Không rõ"),
            Field("Điện áp", intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).takeIf { it >= 0 }?.let { "$it mV" } ?: "Không rõ"),
            Field("Dòng điện hiện tại", current?.let { "${"%.0f".format(Locale.US, it / 1000.0)} mA" } ?: "Không được cung cấp"),
            Field("Điện tích còn lại", charge?.let { "${"%.0f".format(Locale.US, it / 1000.0)} mAh" } ?: "Không được cung cấp"),
            Field("Dòng điện trung bình", batteryLive().averageMa?.let { "%.1f mA".format(Locale.US, it) } ?: "Không được cung cấp"),
            Field("Công suất tức thời", batteryLive().powerW?.let { "%+.2f W".format(Locale.US, it) } ?: "Không được cung cấp"),
            Field("Chu kỳ sạc", if (cycleCount >= 0) cycleCount.toString() else "Không được cung cấp"),
            Field("Battery Scale", scale.toString()),
            Field("Nhà sản xuất cell pin", "Không công khai qua Android"),
            Field("Công nghệ", intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Không rõ")
        ))
    }

    fun sensors(): List<Section> {
        val manager = context.getSystemService(SensorManager::class.java)
        return manager.getSensorList(Sensor.TYPE_ALL).map { sensor ->
            Section(sensor.name, listOf(
                Field("Loại", sensor.stringType.substringAfterLast('.')),
                Field("Nhà sản xuất", sensor.vendor),
                Field("Phiên bản", sensor.version.toString()),
                Field("Dải đo tối đa", sensor.maximumRange.toString()),
                Field("Độ phân giải", sensor.resolution.toString()),
                Field("Điện năng", "${sensor.power} mA"),
                Field("Độ trễ tối thiểu", "${sensor.minDelay} µs"),
                Field("Chế độ báo thức", if (sensor.isWakeUpSensor) "Có" else "Không")
            ))
        }
    }

    fun cameras(): List<Section> {
        val manager = context.getSystemService(CameraManager::class.java) ?: return emptyList()
        val candidateIds = linkedSetOf<String>()
        val logicalIds = try { manager.cameraIdList.toList() } catch (_: Exception) { emptyList() }
        candidateIds.addAll(logicalIds)

        // Query physical cameras backing logical cameras
        for (id in logicalIds) {
            try {
                val c = manager.getCameraCharacteristics(id)
                if (Build.VERSION.SDK_INT >= 28) {
                    candidateIds.addAll(c.physicalCameraIds)
                }
            } catch (_: Exception) {}
        }

        // Probe common auxiliary IDs (0..9, 20..21) commonly exposed by OEMs
        for (i in 0..9) {
            val candidate = i.toString()
            if (candidate !in candidateIds) {
                try {
                    manager.getCameraCharacteristics(candidate)
                    candidateIds.add(candidate)
                } catch (_: Exception) {}
            }
        }
        for (candidate in listOf("20", "21")) {
            if (candidate !in candidateIds) {
                try {
                    manager.getCameraCharacteristics(candidate)
                    candidateIds.add(candidate)
                } catch (_: Exception) {}
            }
        }

        data class CameraItem(val section: Section, val sortRank: Int)
        val cameraItems = mutableListOf<CameraItem>()
        val seenSensors = mutableSetOf<String>()

        for (id in candidateIds) {
            try {
                val c = manager.getCameraCharacteristics(id)
                val facing = c.get(CameraCharacteristics.LENS_FACING)
                val focals = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focal = focals?.firstOrNull() ?: 0f
                val apertures = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                val aperture = apertures?.firstOrNull()?.let { "f/$it" } ?: ""
                val size = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val pixelSize = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                val activeSize = c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                val map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                val bestOutput = map?.getOutputSizes(android.graphics.ImageFormat.JPEG)?.maxByOrNull { it.width.toLong() * it.height }
                    ?: map?.getOutputSizes(android.graphics.ImageFormat.YUV_420_888)?.maxByOrNull { it.width.toLong() * it.height }
                    ?: map?.getOutputSizes(android.graphics.ImageFormat.RAW_SENSOR)?.maxByOrNull { it.width.toLong() * it.height }
                    ?: pixelSize
                    ?: activeSize?.let { android.util.Size(it.width(), it.height()) }

                val pixelWidth = bestOutput?.width ?: pixelSize?.width ?: 0
                val pixelHeight = bestOutput?.height ?: pixelSize?.height ?: 0
                val mpDouble = if (pixelWidth > 0 && pixelHeight > 0) (pixelWidth.toLong() * pixelHeight) / 1_000_000.0 else 0.0
                val mp = if (mpDouble > 0) "%.1f MP".format(Locale.US, mpDouble) else ""

                val fov = if (size != null && size.width > 0f && focal > 0f) {
                    2.0 * kotlin.math.atan(size.width.toDouble() / (2.0 * focal)) * 180.0 / Math.PI
                } else null

                val diagonal = size?.let { kotlin.math.hypot(it.width.toDouble(), it.height.toDouble()) }
                val focal35mm = if (diagonal != null && diagonal > 0 && focal > 0f) {
                    (focal * 43.27 / diagonal).roundToInt()
                } else null

                val minFocusDist = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
                val minFocusCm = if (minFocusDist != null && minFocusDist > 0f) (100f / minFocusDist) else null

                val physicalIds = if (Build.VERSION.SDK_INT >= 28) c.physicalCameraIds else emptySet()
                val isPhysical = id !in logicalIds || (Build.VERSION.SDK_INT >= 28 && logicalIds.any { lid ->
                    lid != id && try { manager.getCameraCharacteristics(lid).physicalCameraIds.contains(id) } catch (_: Exception) { false }
                })

                val caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList().orEmpty()
                val isDepthOutput = caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT)
                val isMultiLogical = caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
                val colorFilter = c.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)
                val isMono = colorFilter == CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_MONO

                // Precise classification of lens role
                val (titleRole, sortRank) = when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> when {
                        (fov != null && fov >= 85.0) || (focal35mm != null && focal35mm <= 22) ->
                            "Camera Trước · Góc Siêu Rộng (Ultra-Wide Selfie)" to 71
                        else ->
                            "Camera Trước · Chân Dung & Selfie (Front Camera)" to 70
                    }
                    CameraCharacteristics.LENS_FACING_EXTERNAL ->
                        "Camera Phụ / Gắn Ngoài (External USB)" to 80
                    else -> when {
                        isDepthOutput || (mpDouble in 0.3..2.5 && (minFocusDist == null || minFocusDist == 0f)) ->
                            "Camera Sau · Cảm Biến Chiều Sâu (Depth Sensor)" to 50
                        (minFocusDist != null && minFocusDist >= 15f) || (mpDouble in 1.0..5.0 && focal in 1.5f..3.0f && (minFocusDist ?: 0f) >= 10f) ->
                            "Camera Sau · Cận Cảnh (Macro Lens)" to 40
                        isMono ->
                            "Camera Sau · Cảm Biến Đơn Sắc (Monochrome B&W)" to 45
                        (focal35mm != null && focal35mm <= 19) || (fov != null && fov >= 88.0) || (focal in 0.1f..2.8f && mpDouble >= 5.0) ->
                            "Camera Sau · Góc Siêu Rộng (Ultra-Wide)" to 20
                        (focal35mm != null && focal35mm >= 50) || (fov != null && fov <= 46.0) || (focal >= 6.8f) ->
                            if ((focal35mm != null && focal35mm >= 100) || focal >= 14f)
                                "Camera Sau · Telephoto Periscope (Zoom Xa)" to 35
                            else
                                "Camera Sau · Ống Kính Telephoto (Zoom Quang Học)" to 30
                        isMultiLogical && !isPhysical ->
                            "Cụm Camera Sau Đa Ống Kính (Logical Multi-Camera)" to 15
                        else ->
                            "Camera Sau · Góc Rộng Chính (Main Wide)" to 10
                    }
                }

                // Avoid exact duplicate sensor entries
                val sensorFingerprint = "$facing-$pixelWidth-$pixelHeight-$focal-$aperture"
                if (sensorFingerprint in seenSensors && isPhysical) {
                    continue
                }
                seenSensors.add(sensorFingerprint)

                val specsList = listOfNotNull(
                    mp.ifBlank { null },
                    focal35mm?.let { "${it}mm eq" } ?: (if (focal > 0f) "${focal}mm" else null),
                    aperture.ifBlank { null }
                ).joinToString(" · ")

                val idLabel = if (isPhysical) "Lens $id" else "ID $id"
                val title = if (specsList.isNotBlank()) "$titleRole ($specsList) · $idLabel" else "$titleRole · $idLabel"

                val level = when (c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy (Cơ bản)"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limited (Giới hạn)"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Full (Đầy đủ tính năng Camera2)"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3 (Chuyên nghiệp / RAW & YUV)"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "External (Thiết bị ngoài)"
                    else -> "Không rõ"
                }

                val fps = c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    ?.joinToString { "${it.lower}–${it.upper} fps" } ?: "Không công khai"
                val capsStr = caps.joinToString { capCode ->
                    when (capCode) {
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Tương thích ngược"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Chỉnh tay thông số cảm biến"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> "Xử lý hậu kỳ tay"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "Chụp ảnh RAW"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS -> "Đọc thông số cảm biến"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Chụp liên tục tốc độ cao"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> "Cụm đa camera logic"
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Xuất bản đồ độ sâu"
                        else -> "Mã $capCode"
                    }
                }.ifBlank { "Không công khai" }

                val sensorFormat = size?.let {
                    val diagMm = kotlin.math.hypot(it.width.toDouble(), it.height.toDouble())
                    if (diagMm > 0) "~1/${"%.1f".format(Locale.US, 18.0 / diagMm)}\"" else null
                }

                val fields = listOf(
                    Field("Chức năng ống kính", titleRole),
                    Field("Góc nhìn thực tế (FOV)", fov?.let { "%.1f° (%s)".format(Locale.US, it, if (it >= 88) "Góc siêu rộng" else if (it >= 60) "Góc rộng tiêu chuẩn" else "Góc hẹp / Zoom") } ?: "Không công khai"),
                    Field("Tiêu cự quy đổi 35mm", focal35mm?.let { "$it mm" } ?: (if (focal > 0f) "${focal}mm (vật lý)" else "Không công khai")),
                    Field("Khẩu độ", aperture.ifBlank { "Không công khai" }),
                    Field("Độ phân giải thực cảm biến", if (pixelWidth > 0 && pixelHeight > 0) "$pixelWidth × $pixelHeight ($mp)" else "Không công khai"),
                    Field("Khoảng cách lấy nét tối thiểu", minFocusCm?.let { "%.1f cm %s".format(Locale.US, it, if (it <= 6.6) "(Hỗ trợ chụp cận cảnh Macro)" else "") }
                        ?: if (minFocusDist == null) "Không công khai hoặc cần quyền camera" else "Vô cực / Cố định"),
                    Field("Kích thước cảm biến vật lý", size?.let { "${it.width} × ${it.height} mm ${sensorFormat?.let { sf -> "· Định dạng $sf" } ?: ""}" } ?: "Không công khai"),
                    Field("Định danh phần cứng", if (isPhysical) "Ống kính vật lý (Lens ID $id)" else "Camera logic (ID $id)"),
                    Field("Cấp phần cứng Camera2", level),
                    Field("Chống rung quang học (OIS)", if (c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true) "Có phần cứng OIS" else "Không"),
                    Field("Đèn flash trợ sáng", if (c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) "Có" else "Không"),
                    Field("Hướng cảm biến", "${c.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0}°"),
                    Field("Tốc độ khung hình (FPS)", fps),
                    Field("Zoom kỹ thuật số tối đa", c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)?.let { "${it}×" } ?: "Không công khai"),
                    Field("Dải nhạy sáng ISO", c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.toString() ?: "Không công khai"),
                    Field("Dải thời gian phơi sáng", c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.let { "${it.lower / 1_000_000}ms – ${it.upper / 1_000_000}ms" } ?: "Không công khai"),
                    Field("Nhận diện khuôn mặt", c.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)?.let { modes ->
                        if (modes.any { it != CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_OFF }) "Có hỗ trợ phần cứng" else "Không"
                    } ?: "Không công khai"),
                    Field("Ống kính vật lý liên kết", if (Build.VERSION.SDK_INT >= 28) physicalIds.joinToString().ifBlank { "Không (Camera đơn lẻ)" } else "Android < 9"),
                    Field("Tính năng chuyên sâu", capsStr)
                )

                cameraItems += CameraItem(Section(title, fields), sortRank)
            } catch (e: Exception) {
                cameraItems += CameraItem(Section("Camera $id", listOf(Field("Lỗi", "${e.javaClass.simpleName}: ${e.message ?: "không đọc được đặc tính"}"))), 99)
            }
        }

        return cameraItems.sortedBy { it.sortRank }.map { it.section }
    }

    private fun readText(path: String): String? = try { File(path).takeIf { it.canRead() }?.readText() } catch (_: Exception) { null }
    private fun effectName(value: Int) = when (value) {
        CameraCharacteristics.CONTROL_EFFECT_MODE_OFF -> "Off"
        CameraCharacteristics.CONTROL_EFFECT_MODE_MONO -> "Mono"
        CameraCharacteristics.CONTROL_EFFECT_MODE_NEGATIVE -> "Negative"
        CameraCharacteristics.CONTROL_EFFECT_MODE_SEPIA -> "Sepia"
        CameraCharacteristics.CONTROL_EFFECT_MODE_AQUA -> "Aqua"
        CameraCharacteristics.CONTROL_EFFECT_MODE_POSTERIZE -> "Posterize"
        else -> "Mã $value"
    }
}

object FormatOptions {
    val binary = androidx.compose.runtime.mutableStateOf(false)
    val fahrenheit = androidx.compose.runtime.mutableStateOf(false)
}

fun bytes(value: Long): String = if (FormatOptions.binary.value) "%.2f GiB".format(LocalizationManager.locale, value / 1_073_741_824.0)
    else "%.2f GB".format(LocalizationManager.locale, value / 1_000_000_000.0)

fun temperature(tenthsC: Int): String = if (FormatOptions.fahrenheit.value) "%.1f °F".format(LocalizationManager.locale, tenthsC / 10.0 * 9 / 5 + 32)
    else "%.1f °C".format(LocalizationManager.locale, tenthsC / 10.0)
