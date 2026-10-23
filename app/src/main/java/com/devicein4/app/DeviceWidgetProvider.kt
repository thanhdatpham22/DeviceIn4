package com.devicein4.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.widget.RemoteViews

object Preferences {
    private const val FILE = "devicein4_preferences"
    fun theme(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("theme", "system") ?: "system"
    fun widgetMetric(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("widget_metric", "ram") ?: "ram"
    fun language(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("language", "system") ?: "system"
    fun units(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("units", "decimal") ?: "decimal"
    fun temperature(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("temperature", "c") ?: "c"
    fun accent(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("accent", "") ?: ""
    fun setTheme(context: Context, value: String) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("theme", value).apply()
    fun setWidgetMetric(context: Context, value: String) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("widget_metric", value).apply()
    fun setLanguage(context: Context, value: String) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("language", value).apply()
    fun setUnits(context: Context, value: String) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("units", value).apply()
    fun setTemperature(context: Context, value: String) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("temperature", value).apply()
    fun setAccent(context: Context, value: String) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("accent", value).apply()
}

class DeviceWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DeviceWidgetProvider::class.java))
            ids.forEach { update(context, manager, it) }
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            FormatOptions.binary.value = Preferences.units(context) == "binary"
            val language = Preferences.language(context)
            val english = (if (language == "system") java.util.Locale.getDefault().language else language) == "en"
            val views = RemoteViews(context.packageName, R.layout.device_widget)
            val metric = Preferences.widgetMetric(context)
            val value: String
            val label: String
            val percent: Int
            when (metric) {
                "device" -> {
                    percent = 0
                    value = Build.MODEL
                    label = "${Build.MANUFACTURER} · ANDROID ${Build.VERSION.RELEASE}"
                }
                "battery" -> {
                    val battery = DeviceRepository(context).batteryLive()
                    percent = battery.level.coerceIn(0, 100)
                    value = if (battery.level >= 0) "${battery.level}%" else "—"
                    label = if (english) "BATTERY" else "PIN"
                }
                "storage" -> {
                    val fs = StatFs(Environment.getDataDirectory().path)
                    percent = if (fs.totalBytes > 0) ((fs.totalBytes - fs.availableBytes) * 100 / fs.totalBytes).toInt() else 0
                    value = bytes(fs.totalBytes - fs.availableBytes)
                    label = if (english) "STORAGE USED" else "BỘ NHỚ ĐÃ DÙNG"
                }
                else -> {
                    val ram = DeviceRepository(context).memory()
                    percent = (ram.percent * 100).toInt()
                    value = bytes(ram.used)
                    label = if (english) "RAM IN USE" else "RAM ĐANG DÙNG"
                }
            }
            views.setTextViewText(R.id.widget_title, "DEVICEIN4")
            views.setTextViewText(R.id.widget_metric, label)
            views.setTextViewText(R.id.widget_value, value)
            views.setProgressBar(R.id.widget_progress, 100, percent, false)
            views.setViewVisibility(R.id.widget_progress, if (metric == "device") android.view.View.GONE else android.view.View.VISIBLE)
            val isLight = when (Preferences.theme(context)) {
                "light" -> true
                "dark" -> false
                else -> (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) != android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
            views.setInt(R.id.widget_root, "setBackgroundColor", if (isLight) Color.rgb(244, 247, 252) else Color.rgb(21, 29, 48))
            views.setTextColor(R.id.widget_value, if (isLight) Color.rgb(17, 25, 44) else Color.WHITE)
            views.setTextColor(R.id.widget_metric, if (isLight) Color.rgb(81, 96, 115) else Color.rgb(153, 169, 189))
            val accent = try { Color.parseColor(Preferences.accent(context)) } catch (_: Exception) { if (isLight) Color.rgb(8, 126, 130) else Color.rgb(95, 225, 213) }
            views.setTextColor(R.id.widget_title, accent)
            val intent = Intent(context, MainActivity::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 0, intent, flags))
            manager.updateAppWidget(id, views)
        }
    }
}
