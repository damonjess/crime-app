package com.dinner.crimeapp.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconSwitcher {

    enum class CrimeTheme(val aliasName: String) {
        DEFAULT("com.dinner.crimeapp.DefaultLauncher"),
        THEFT("com.dinner.crimeapp.TheftLauncher"),
        VIOLENCE("com.dinner.crimeapp.ViolenceLauncher")
    }

    fun setTheme(context: Context, theme: CrimeTheme) {
        val packageManager = context.packageManager
        
        CrimeTheme.entries.forEach { entry ->
            val componentName = ComponentName(context, entry.aliasName)
            val currentState = packageManager.getComponentEnabledSetting(componentName)
            
            val targetState = if (entry == theme) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            if (currentState != targetState) {
                packageManager.setComponentEnabledSetting(
                    componentName,
                    targetState,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
