package com.dinner.crimeapp.ui

import androidx.compose.ui.graphics.Color

object CrimeCategoryColors {
    private val colorMap = mapOf(
        "violent-crime" to Color(0xFFC62839),
        "shoplifting" to Color(0xFFE0567F),
        "anti-social-behaviour" to Color(0xFFF2A93B),
        "criminal-damage-arson" to Color(0xFFE0632B),
        "public-order" to Color(0xFF3D8FA6),
        "other-theft" to Color(0xFF7B4FC9),
        "burglary" to Color(0xFF4E7D3B),
        "drugs" to Color(0xFF2E8B57),
        "vehicle-crime" to Color(0xFF616161),
        "robbery" to Color(0xFF8B0000),
        "theft-from-the-person" to Color(0xFF9C27B0),
        "possession-of-weapons" to Color(0xFF37474F),
        "bicycle-theft" to Color(0xFF00838F),
        "other-crime" to Color(0xFF757575)
    )

    fun colorFor(category: String): Color = colorMap[category] ?: Color(0xFF9E9E9E)

    fun displayName(category: String): String =
        category.replace("-", " ").split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}