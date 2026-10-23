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
data class CpuCore(val index: Int, val currentKhz: Long?, val maxKhz: Long?, val name: String) {
    val fraction get() = if (currentKhz != null && maxKhz != null && maxKhz > 0) (currentKhz.toFloat() / maxKhz).coerceIn(0f, 1f) else 0f
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
        CpuCore(index, readText(base + "scaling_cur_freq")?.trim()?.toLongOrNull(),
            readText(base + "cpuinfo_max_freq")?.trim()?.toLongOrNull(), coreNames[index] ?: "Core ${index + 1}")
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
        val manager = context.getSystemService(CameraManager::class.java)
        return manager.cameraIdList.map { id -> try {
            val c = manager.getCameraCharacteristics(id)
            val facing = when (c.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Trước"
                CameraCharacteristics.LENS_FACING_BACK -> "Sau"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "Ngoài"
                else -> "Không rõ"
            }
            val level = when (c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limited"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Full"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "External"
                else -> "Không rõ"
            }
            val size = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            val jpeg = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(android.graphics.ImageFormat.JPEG)?.maxByOrNull { it.width.toLong() * it.height }
            val fps = c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                ?.joinToString { "${it.lower}–${it.upper} fps" } ?: "Không công khai"
            val effects = c.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)
                ?.joinToString { effectName(it) } ?: "Không công khai"
            val caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?.joinToString() ?: "Không công khai"
            val map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val highSpeed = try { map?.highSpeedVideoFpsRanges?.joinToString { "${it.lower}–${it.upper} fps" } } catch (_: Exception) { null }
            val cameraVersion = if (Build.VERSION.SDK_INT >= 28) {
                c.get(CameraCharacteristics.INFO_VERSION)?.takeIf { it.isNotBlank() }
            } else null
            Section("Camera $id · $facing", listOf(
                Field("Hãng điện thoại", Build.MANUFACTURER.takeIf { it.isNotBlank() && !it.equals(Build.UNKNOWN, true) } ?: "Không công khai"),
                Field("Hãng module camera", "Không được Camera2 công khai"),
                Field("Phiên bản camera do OEM báo", cameraVersion ?: "Không công khai"),
                Field("Cấp phần cứng", level),
                Field("Độ phân giải JPEG tối đa", jpeg?.let { "${it.width} × ${it.height} (${"%.1f".format(Locale.US, it.width.toLong() * it.height / 1_000_000.0)} MP)" } ?: "Không công khai"),
                Field("Kích thước cảm biến", size?.let { "${it.width} × ${it.height} mm" } ?: "Không công khai"),
                Field("Tiêu cự", c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.joinToString { "$it mm" } ?: "Không công khai"),
                Field("Khẩu độ", c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.joinToString { "f/$it" } ?: "Không công khai"),
                Field("Chống rung quang học", if (c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true) "Có" else "Không"),
                Field("Đèn flash", if (c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) "Có" else "Không"),
                Field("Hướng cảm biến", "${c.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0}°")
                ,Field("Target FPS", fps)
                ,Field("Effects", effects)
                ,Field("AF modes", c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.joinToString() ?: "Không công khai")
                ,Field("AWB modes", c.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.joinToString() ?: "Không công khai")
                ,Field("AE modes", c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)?.joinToString() ?: "Không công khai")
                ,Field("Scene modes", c.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)?.joinToString() ?: "Không công khai")
                ,Field("Video stabilization", c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)?.joinToString() ?: "Không công khai")
                ,Field("ISO range", c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.toString() ?: "Không công khai")
                ,Field("Exposure time range", c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.toString() ?: "Không công khai")
                ,Field("Focus distance", c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)?.let { "$it diopters" } ?: "Không công khai")
                ,Field("Độ phân giải cảm biến", c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)?.let { "${it.width} × ${it.height}" } ?: "Không công khai")
                ,Field("Vùng cảm biến hoạt động", c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)?.toShortString() ?: "Không công khai")
                ,Field("Zoom số tối đa", c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)?.let { "${it}×" } ?: "Không công khai")
                ,Field("Exposure compensation", c.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)?.toString() ?: "Không công khai")
                ,Field("Exposure step", c.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)?.toString() ?: "Không công khai")
                ,Field("High-speed FPS", highSpeed?.ifBlank { "Không hỗ trợ" } ?: "Không công khai")
                ,Field("Face detection modes", c.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)?.joinToString() ?: "Không công khai")
                ,Field("AF regions tối đa", c.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)?.toString() ?: "Không công khai")
                ,Field("AE regions tối đa", c.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)?.toString() ?: "Không công khai")
                ,Field("Pipeline depth", c.get(CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH)?.toString() ?: "Không công khai")
                ,Field("Physical camera IDs", if (Build.VERSION.SDK_INT >= 28) c.physicalCameraIds.joinToString().ifBlank { "Không có" } else "Android < 9")
                ,Field("Capabilities (mã)", caps)
            ))
        } catch (e: Exception) {
            Section("Camera $id", listOf(Field("Lỗi", "${e.javaClass.simpleName}: ${e.message ?: "không đọc được đặc tính"}")))
        }
        }
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

fun bytes(value: Long): String = if (FormatOptions.binary.value) "%.2f GiB".format(Locale.US, value / 1_073_741_824.0)
    else "%.2f GB".format(Locale.US, value / 1_000_000_000.0)

fun temperature(tenthsC: Int): String = if (FormatOptions.fahrenheit.value) "%.1f °F".format(Locale.US, tenthsC / 10.0 * 9 / 5 + 32)
    else "%.1f °C".format(Locale.US, tenthsC / 10.0)
