package com.example.crimeapp.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconSwitcher {

    enum class CrimeTheme(val aliasName: String) {
        DEFAULT("com.example.crimeapp.DefaultLauncher"),
        THEFT("com.example.crimeapp.TheftLauncher"),
        VIOLENCE("com.example.crimeapp.ViolenceLauncher")
    }

    fun setTheme(context: Context, theme: CrimeTheme) {
        val packageManager = context.packageManager
        
        CrimeTheme.entries.forEach { entry ->
            val state = if (entry == theme) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            packageManager.setComponentEnabledSetting(
                ComponentName(context, entry.aliasName),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
