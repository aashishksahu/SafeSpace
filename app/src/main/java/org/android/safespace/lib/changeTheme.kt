package org.android.safespace.lib

import android.content.Context
import android.content.SharedPreferences
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import org.android.safespace.R


fun changeTheme(context: Context, firstRun: Boolean, prefName: String, sharedPref: SharedPreferences, menuItem: MenuItem) {

    // get the stored preference for dark mode
    val isDark = sharedPref.getBoolean(prefName, false)
    if (firstRun) {
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            menuItem.title = context.getString(R.string.light_mode)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            menuItem.title = context.getString(R.string.dark_mode)
        }
    } else {

        // store the new preference in shared preferences and change the theme
        val editor = sharedPref.edit()
        if (isDark) {
            editor.putBoolean(prefName, false)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            menuItem.title = context.getString(R.string.dark_mode)
        } else {
            editor.putBoolean(prefName, true)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            menuItem.title = context.getString(R.string.light_mode)
        }
        editor.apply()
    }
}
