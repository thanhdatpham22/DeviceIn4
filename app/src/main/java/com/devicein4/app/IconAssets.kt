package com.devicein4.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** File names in app/src/main/assets/icons, without their image extension. */
object IconAssets {
    const val appLogo = "app_logo"

    fun androidVersion(release: String): String {
        val major = release.substringBefore('.').toIntOrNull() ?: return "android_version_default"
        return "android_version_$major"
    }

    private val brands = listOf("xiaomi", "redmi", "oppo", "samsung", "google",
        "oneplus", "vivo", "huawei", "qualcomm", "mediatek", "arm", "nvidia")
    private val pages = mapOf(
        "Device" to "page_device", "OS" to "page_os", "CPU & GPU" to "page_cpu_gpu",
        "RAM trực tiếp" to "page_ram", "Storage" to "page_storage",
        "Display" to "page_display", "Battery" to "page_battery",
        "Network" to "page_network", "Camera" to "page_camera",
        "Sensor" to "page_sensor", "Features" to "page_features"
    )
    private val sensors = listOf("accelerometer", "gyroscope", "light", "proximity",
        "pressure", "magnetic", "step", "heart", "temperature")

    fun brand(name: String): String = brands.firstOrNull {
        name.contains(it, ignoreCase = true)
    }?.let { "brand_$it" }.orEmpty()

    fun page(title: String): String = pages[title].orEmpty()

    fun sensor(type: String): String = sensors.firstOrNull {
        type.contains(it, ignoreCase = true)
    }?.let { "sensor_$it" }.orEmpty()
}

private object LocalIconCache {
    private val images = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private var availableFiles: Set<String>? = null

    fun get(context: Context, key: String): Bitmap? {
        if (key.isBlank()) return null
        synchronized(images) { images.get(key)?.let { return it } }
        val files = synchronized(this) {
            availableFiles ?: context.assets.list("icons").orEmpty().toSet().also { availableFiles = it }
        }
        val name = listOf("png", "webp", "jpg", "jpeg")
            .map { "$key.$it" }.firstOrNull { it in files } ?: return null
        return try {
            val path = "icons/$name"
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 512) sample *= 2
            val bitmap = context.assets.open(path).use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null
            synchronized(images) { images.put(key, bitmap) }
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun AssetIcon(key: String, modifier: Modifier = Modifier, description: String? = null,
              fallback: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = key, key2 = context) {
        value = withContext(Dispatchers.IO) { LocalIconCache.get(context, key) }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = description,
            modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        fallback()
    }
}
