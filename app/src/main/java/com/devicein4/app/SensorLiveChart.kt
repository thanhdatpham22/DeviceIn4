package com.devicein4.app

import android.Manifest
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import com.devicein4.app.LocalizedText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

internal data class SensorPoint(val timestamp: Long, val values: List<Float>)

/** A bounded history in event time; never invent values for axes the sensor does not report. */
internal fun appendSensorPoint(history: List<SensorPoint>, point: SensorPoint): List<SensorPoint> =
    (history + point).filter { it.timestamp >= point.timestamp - 10_000_000_000L }.takeLast(120)

internal fun sensorChannelName(type: Int, index: Int): String = when {
    type == Sensor.TYPE_ORIENTATION -> listOf("Azimuth", "Pitch", "Roll").getOrNull(index) ?: "C${index + 1}"
    type in setOf(1, 2, 4, 9, 10, 11, 14, 15, 16, 20, 35) && index < 3 -> listOf("X", "Y", "Z")[index]
    type in setOf(11, 15, 20) && index == 3 -> "W"
    else -> "C${index + 1}"
}

@Composable
internal fun SensorLiveChart(section: Section, enabled: Boolean, accent: Color, text: Color, muted: Color, english: Boolean) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val manager = remember(context) { context.getSystemService(SensorManager::class.java) }
    val sensor = remember(section.title, section.fields) {
        val id = section.fields.firstOrNull { it.label == "Sensor ID" }?.value?.toIntOrNull()
        val type = section.fields.firstOrNull { it.label == "Sensor Type" }?.value?.substringBefore('·')?.trim()?.toIntOrNull()
        val wakeUp = section.fields.firstOrNull { it.label == "isWakeUpSensor" }?.value == "Có"
        (manager.getSensorList(Sensor.TYPE_ALL) + manager.getDynamicSensorList(Sensor.TYPE_ALL))
            .firstOrNull { it.id == id && it.type == type && it.name == section.title && it.isWakeUpSensor == wakeUp }
    }
    var points by remember(sensor) { mutableStateOf(emptyList<SensorPoint>()) }
    var status by remember(sensor) { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    val permission = when {
        sensor?.type in listOf(Sensor.TYPE_STEP_COUNTER, Sensor.TYPE_STEP_DETECTOR) && Build.VERSION.SDK_INT >= 29 -> Manifest.permission.ACTIVITY_RECOGNITION
        sensor?.type in listOf(Sensor.TYPE_HEART_RATE, Sensor.TYPE_HEART_BEAT) ->
            if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS
        else -> null
    }
    var missingPermission by remember(sensor) { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { revision++ }

    DisposableEffect(sensor, enabled, owner, revision, english) {
        val mainHandler = Handler(Looper.getMainLooper())
        var listening = false
        var lastTimestamp = 0L
        fun record(timestamp: Long, values: FloatArray) {
            if (!listening) return
            // Continuous sensors may be delivered faster than requested by another app.
            if (sensor?.reportingMode == Sensor.REPORTING_MODE_CONTINUOUS && timestamp - lastTimestamp < 100_000_000L) return
            lastTimestamp = timestamp
            points = appendSensorPoint(points, SensorPoint(timestamp, values.toList()))
            status = LocalizationManager.localize(if (english) "Live · last 10 s" else "Trực tiếp · 10 giây gần nhất")
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) = record(event.timestamp, event.values)
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val trigger = object : TriggerEventListener() {
            override fun onTrigger(event: TriggerEvent) {
                val timestamp = event.timestamp
                val values = event.values.copyOf()
                mainHandler.post {
                    record(timestamp, values)
                    if (listening && sensor != null) {
                        try {
                            if (!manager.requestTriggerSensor(this, sensor)) {
                                listening = false
                                status = LocalizationManager.localize(if (english) "Unable to rearm trigger" else "Không thể đăng ký lại cảm biến kích hoạt")
                            }
                        } catch (_: SecurityException) {
                            listening = false
                            status = LocalizationManager.localize(if (english) "Sensor access restricted" else "Quyền đọc cảm biến bị giới hạn")
                        } catch (_: IllegalArgumentException) {
                            listening = false
                            status = LocalizationManager.localize(if (english) "Sensor disconnected" else "Cảm biến đã ngắt kết nối")
                        }
                    }
                }
            }
        }
        fun stop() {
            listening = false
            manager.unregisterListener(listener)
            if (sensor != null) manager.cancelTriggerSensor(trigger, sensor)
            mainHandler.removeCallbacksAndMessages(null)
        }
        fun start() {
            if (listening || !enabled) return
            if (sensor == null) {
                status = LocalizationManager.localize(if (english) "Sensor disconnected" else "Cảm biến đã ngắt kết nối")
                return
            }
            missingPermission = permission != null && ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
            if (missingPermission) {
                status = LocalizationManager.localize(if (english) "Permission required to receive data" else "Cần cấp quyền để nhận dữ liệu")
                return
            }
            try {
                listening = true
                val registered = if (sensor.reportingMode == Sensor.REPORTING_MODE_ONE_SHOT) manager.requestTriggerSensor(trigger, sensor)
                    else manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, mainHandler)
                if (!registered) {
                    stop()
                    status = LocalizationManager.localize(if (english) "Device declined this sensor stream" else "Thiết bị không cho đăng ký luồng cảm biến này")
                } else status = LocalizationManager.localize(if (english) "Listening · waiting for a sensor event" else "Đang nghe · chờ cảm biến gửi sự kiện")
            } catch (_: SecurityException) {
                stop()
                status = LocalizationManager.localize(if (english) "Sensor access restricted by Android/OEM" else "Android/OEM giới hạn quyền đọc cảm biến")
            } catch (_: IllegalArgumentException) {
                stop()
                status = LocalizationManager.localize(if (english) "Sensor is no longer available" else "Cảm biến không còn khả dụng")
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) start()
            if (event == Lifecycle.Event.ON_PAUSE) {
                stop()
                status = LocalizationManager.localize(if (english) "Paused" else "Tạm dừng")
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            stop()
        }
    }

    val palette = listOf(accent, Color(0xFFFFB454), Color(0xFFA78BFA), Color(0xFFED729B), Color(0xFF81C784), Color(0xFF64B5F6))
    val finite = points.flatMap { it.values }.filter { it.isFinite() }
    val lower = finite.minOrNull() ?: 0f
    val upper = finite.maxOrNull() ?: 1f
    val padding = ((upper - lower) * .1f).coerceAtLeast(.01f)
    val min = lower - padding
    val max = upper + padding
    val channels = points.maxOfOrNull { it.values.size } ?: 0
    Text(status, color = muted, fontSize = 10.sp, lineHeight = 14.sp)
    if (missingPermission && permission != null) Button(onClick = {
        requestDevicePermission(context, permission) { launcher.launch(permission) }
    }) { Text(LocalizationManager.localize(if (english) "Allow sensor access" else "Cấp quyền cảm biến"), fontSize = 11.sp) }
    Text("${formatSensorValue(max)} / ${formatSensorValue(min)}", color = muted, fontSize = 9.sp)
    Canvas(Modifier.fillMaxWidth().height(112.dp)) {
        repeat(4) { row ->
            val y = size.height * row / 3f
            drawLine(muted.copy(alpha = .15f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        val end = points.lastOrNull()?.timestamp ?: 0L
        val start = end - 10_000_000_000L
        repeat(channels) { channel ->
            val path = Path()
            var connected = false
            points.forEach { point ->
                val value = point.values.getOrNull(channel)
                if (value != null && value.isFinite()) {
                    val x = ((point.timestamp - start) / 10_000_000_000.0).toFloat() * size.width
                    val y = size.height * (1f - ((value - min) / (max - min)).coerceIn(0f, 1f))
                    if (connected) path.lineTo(x, y) else path.moveTo(x, y)
                    connected = true
                    drawCircle(palette[channel % palette.size], 2.dp.toPx(), Offset(x, y))
                } else connected = false
            }
            drawPath(path, palette[channel % palette.size], style = Stroke(1.5.dp.toPx()))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("−10 s", color = muted, fontSize = 9.sp)
        Text(LocalizationManager.localize(if (english) "Latest event" else "Sự kiện mới nhất"), color = muted, fontSize = 9.sp)
    }
    (0 until channels).toList().chunked(3).forEach { group ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            group.forEach { channel ->
                val value = points.lastOrNull()?.values?.getOrNull(channel)
                Text("${sensorChannelName(sensor?.type ?: -1, channel)} ${value?.let(::formatSensorValue) ?: "—"}",
                    color = palette[channel % palette.size], fontSize = 10.sp, modifier = Modifier.weight(1f))
            }
        }
    }
    val unit = when (sensor?.type) {
        1, 9, 10, 35 -> "m/s²"
        2, 14 -> "µT"
        3 -> "°"
        4, 16 -> "rad/s"
        5 -> "lx"
        6 -> "hPa"
        8 -> "cm"
        7, 13 -> "°C"
        12 -> "%"
        21 -> "bpm"
        else -> LocalizationManager.localize(if (english) "Raw sensor values" else "Giá trị gốc từ cảm biến")
    }
    Text(unit, color = text.copy(alpha = .6f), fontSize = 9.sp)
}

private fun formatSensorValue(value: Float): String = if (!value.isFinite()) "—"
    else if (kotlin.math.abs(value) >= 1_000_000f) String.format(Locale.US, "%.2e", value)
    else String.format(Locale.US, "%.2f", value)
