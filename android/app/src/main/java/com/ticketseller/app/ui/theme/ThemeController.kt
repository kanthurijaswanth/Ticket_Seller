package com.ticketseller.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the current theme key + light/dark choice, persisted in SharedPreferences.
 * Backed by Compose state so any change recolors the whole app live (the values are
 * read inside [com.ticketseller.app.ui.theme.AppTheme]).
 */
@Singleton
class ThemeController @Inject constructor(
    @ApplicationContext ctx: Context,
) {
    private val prefs = ctx.getSharedPreferences("ts_theme", Context.MODE_PRIVATE)

    var themeKey by mutableStateOf(prefs.getString(K_THEME, "sunshine") ?: "sunshine")
        private set

    /** null = follow the system setting; true/false = explicit user choice. */
    var darkOverride by mutableStateOf(
        if (prefs.contains(K_DARK)) prefs.getBoolean(K_DARK, false) else null,
    )
        private set

    fun setTheme(key: String) {
        if (key !in THEME_KEYS) return
        themeKey = key
        prefs.edit().putString(K_THEME, key).apply()
    }

    fun setDark(dark: Boolean) {
        darkOverride = dark
        prefs.edit().putBoolean(K_DARK, dark).apply()
    }

    companion object {
        private const val K_THEME = "theme_key"
        private const val K_DARK = "dark_mode"
    }
}
