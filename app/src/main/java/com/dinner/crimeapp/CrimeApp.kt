package com.dinner.crimeapp

import android.app.Application
import com.dinner.crimeapp.ui.theme.ThemePrefs
import org.osmdroid.config.Configuration

class CrimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePrefs.load(this)
        
        // Initialize osmdroid configuration globally
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        // Required by OSM's tile usage policy
        Configuration.getInstance().userAgentValue = packageName
    }
}
