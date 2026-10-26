package com.devicein4.app

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

internal object AppLanguages {
    private fun tags(context: Context): String = if (Build.VERSION.SDK_INT >= 33) {
        context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
    } else AppCompatDelegate.getApplicationLocales().toLanguageTags()

    fun selection(context: Context): String = tags(context).substringBefore(',').let {
        when { it.isBlank() -> "system"; it.startsWith("zh") -> "zh-Hans"; else -> it.substringBefore('-') }
    }

    fun select(language: String) {
        val tags = if (language == "system") "" else if (language == "zh") "zh-Hans" else language
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
    }

    fun migrate(context: Context) {
        val preferences = context.getSharedPreferences("devicein4_preferences", Context.MODE_PRIVATE)
        val previous = Preferences.language(context)
        // Restore before Activity.onCreate on older Android; the framework persists API 33+ locales.
        if (Build.VERSION.SDK_INT < 33) {
            select(previous)
            return
        }
        if (preferences.getBoolean("android_locales_migrated", false)) return
        if (tags(context).isBlank() && previous != "system") {
            if (Build.VERSION.SDK_INT >= 33) {
                context.getSystemService(LocaleManager::class.java).applicationLocales =
                    android.os.LocaleList.forLanguageTags(if (previous == "zh") "zh-Hans" else previous)
            } else select(previous)
        }
        preferences.edit().putBoolean("android_locales_migrated", true).apply()
    }
}

/** Repository-label compatibility bridge; Android resources own the translations. */
object LocalizationManager {
    val currentLang = mutableStateOf("en")
    val isEnglish get() = currentLang.value == "en"
    @Volatile private var localizedContext: Context? = null
    val locale: Locale get() = localizedContext?.resources?.configuration?.locales?.get(0) ?: Locale.getDefault()

    fun configure(context: Context) {
        val configuration = Configuration(context.resources.configuration)
        if (localizedContext?.resources?.configuration != configuration) {
            localizedContext = context.applicationContext.createConfigurationContext(configuration)
        }
        currentLang.value = configuration.locales[0].language
    }

    fun localize(text: String): String = localizedContext?.let { AppStrings.localize(it, text) } ?: text
}

internal object AppStrings {
    private val templateRules by lazy {
        LegacyStringResources.templates.map { (source, resource) ->
            val placeholder = Regex("%[0-9]+" + Regex.escape("$") + "s")
            val pattern = StringBuilder("^")
            var offset = 0
            placeholder.findAll(source).forEach { match ->
                pattern.append(Regex.escape(source.substring(offset, match.range.first)))
                pattern.append("(.+?)")
                offset = match.range.last + 1
            }
            pattern.append(Regex.escape(source.substring(offset))).append('$')
            Regex(pattern.toString(), RegexOption.DOT_MATCHES_ALL) to resource
        }
    }

    fun localize(context: Context, text: String): String {
        val resources = context.resources
        LegacyStringResources.aliases[text]?.let { return resources.getString(it) }
        if ('\n' in text) return text.lines().joinToString("\n") { localize(context, it) }
        templateRules.forEach { (pattern, id) ->
            pattern.matchEntire(text)?.let { match -> return resources.getString(id, *match.groupValues.drop(1).toTypedArray()) }
        }
        Regex("^(\\d+) ngày (\\d+) giờ (\\d+) phút$").matchEntire(text)?.let {
            val days = it.groupValues[1].toLongOrNull() ?: return text
            val hours = it.groupValues[2].toLongOrNull() ?: return text
            val minutes = it.groupValues[3].toLongOrNull() ?: return text
            return resources.getString(R.string.duration_format, days, hours, minutes)
        }
        val boundaries = Regex(" · | / |: | \\(").findAll(text).map { it.range.first }.toList().asReversed()
        for (end in boundaries) {
            LegacyStringResources.aliases[text.substring(0, end)]?.let { id ->
                return resources.getString(id) + text.substring(end)
            }
        }
        return Regex("\\(([^()]*)\\)").replace(text) { match ->
            val inner = match.groupValues[1]
            val id = LegacyStringResources.aliases[inner]
            if (id != null && inner.any { it.isLetter() && it.code > 127 }) "(${resources.getString(id)})" else match.value
        }
    }
}
