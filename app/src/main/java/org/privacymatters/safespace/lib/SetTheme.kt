package org.privacymatters.safespace.lib

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import org.privacymatters.safespace.R

class SetTheme {
    companion object {
        fun setTheme(delegate: AppCompatDelegate, context: Context, theme: String) {
            when (theme) {
                context.getString(R.string.System) -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }

                context.getString(R.string.Light) -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }

                context.getString(R.string.Dark) -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
            delegate.applyDayNight()
        }
    }
}
