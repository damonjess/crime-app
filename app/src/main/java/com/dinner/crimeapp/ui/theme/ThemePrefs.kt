package com.dinner.crimeapp.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Tracks which Material theme the app should use. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

object ThemePrefs {
    private const val PREFS = "crimeapp_theme"
    private const val KEY_MODE = "mode"

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _mode.value = runCatching { ThemeMode.valueOf(prefs.getString(KEY_MODE, null) ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    fun setMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode.name).apply()
        _mode.value = mode
    }
}
