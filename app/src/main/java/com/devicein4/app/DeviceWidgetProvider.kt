package com.devicein4.app

import android.content.Context

object Preferences {
    private const val FILE = "devicein4_preferences"
    fun theme(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("theme", "system") ?: "system"
    fun language(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("language", "system") ?: "system"
    fun units(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("units", "decimal") ?: "decimal"
    fun temperature(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("temperature", "c") ?: "c"
    fun accent(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("accent", "") ?: ""
    fun setTheme(context: Context, value: String) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("theme", value).apply()
    fun setLanguage(context: Context, value: String) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("language", value).apply()
    fun setUnits(context: Context, value: String) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("units", value).apply()
    fun setTemperature(context: Context, value: String) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("temperature", value).apply()
    fun setAccent(context: Context, value: String) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("accent", value).apply()
}
