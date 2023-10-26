package org.privacymatters.safespace.lib

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import org.privacymatters.safespace.R

class ThemeManager(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) {

    init {

        val theme = sharedPreferences.getInt(Constants.THEME, 0)

        context.setTheme(theme)

    }

    fun changeTheme(themeId: Int) {

        when (themeId) {
            R.string.dark_theme_text -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            R.string.light_theme_text -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            R.string.amoled_theme_text -> {
                context.setTheme(R.style.Theme_SafeSpace_AMOLED)
            }

            R.string.pride_theme_text -> {
                context.setTheme(R.style.Theme_SafeSpace_Pride)
            }
        }

        val editor = sharedPreferences.edit()

        editor.putInt(Constants.THEME, themeId)

        editor.apply()
    }



}