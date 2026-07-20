package com.example.crimeapp

import android.app.Application
import org.osmdroid.config.Configuration

class CrimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize osmdroid configuration globally
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        // Required by OSM's tile usage policy
        Configuration.getInstance().userAgentValue = packageName
    }
}
