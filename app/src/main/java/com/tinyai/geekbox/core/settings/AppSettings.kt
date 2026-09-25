package com.tinyai.geekbox.core.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { SYSTEM, DARK, LIGHT }

enum class AiOpenMode { SYSTEM_BROWSER, WEBVIEW }

object AppSettings {

    private const val PREFS = "geekbox_settings"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_DYNAMIC = "dynamic_color"
    private const val KEY_AI_MODE = "ai_open_mode"

    private var prefs: SharedPreferences? = null
    private var initialized = false

    private var themeModeState by mutableStateOf(ThemeMode.SYSTEM)
    private var dynamicColorState by mutableStateOf(false)
    private var aiOpenModeState by mutableStateOf(AiOpenMode.SYSTEM_BROWSER)

    val themeMode: ThemeMode get() = themeModeState
    val dynamicColor: Boolean get() = dynamicColorState
    val aiOpenMode: AiOpenMode get() = aiOpenModeState

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        themeModeState = runCatching {
            ThemeMode.valueOf(p.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        dynamicColorState = p.getBoolean(KEY_DYNAMIC, false)
        aiOpenModeState = runCatching {
            AiOpenMode.valueOf(p.getString(KEY_AI_MODE, AiOpenMode.SYSTEM_BROWSER.name) ?: AiOpenMode.SYSTEM_BROWSER.name)
        }.getOrDefault(AiOpenMode.SYSTEM_BROWSER)
    }

    fun setThemeMode(mode: ThemeMode) {
        themeModeState = mode
        prefs?.edit()?.putString(KEY_THEME, mode.name)?.apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        dynamicColorState = enabled
        prefs?.edit()?.putBoolean(KEY_DYNAMIC, enabled)?.apply()
    }

    fun setAiOpenMode(mode: AiOpenMode) {
        aiOpenModeState = mode
        prefs?.edit()?.putString(KEY_AI_MODE, mode.name)?.apply()
    }
}
