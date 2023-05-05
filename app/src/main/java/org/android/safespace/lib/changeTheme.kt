package org.android.safespace.lib

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate


fun changeTheme(firstRun: Boolean, prefName: String, sharedPref: SharedPreferences) {

    // get the stored preference for dark mode
    val isDark = sharedPref.getBoolean(prefName, false)
    if (firstRun) {
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    } else {

        // store the new preference in shared preferences and change the theme
        val editor = sharedPref.edit()
        if (isDark) {
            editor.putBoolean(prefName, false)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            editor.putBoolean(prefName, true)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        editor.apply()
    }
}
