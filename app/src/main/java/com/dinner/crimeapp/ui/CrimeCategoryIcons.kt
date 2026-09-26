package com.dinner.crimeapp.ui

/**
 * Maps police.uk category codes to friendly emoji for markers and chips.
 * Emoji render identically everywhere with zero asset work, and give the
 * map a modern, glanceable feel compared to plain coloured dots.
 */
object CrimeCategoryIcons {

    fun emojiFor(category: String): String = when {
        category.contains("anti-social", ignoreCase = true) -> "🔊"
        category.contains("burglary", ignoreCase = true) -> "🏠"
        category.contains("criminal-damage", ignoreCase = true) || category.contains("arson", ignoreCase = true) -> "🔥"
        category.contains("drugs", ignoreCase = true) -> "💊"
        category.contains("violent", ignoreCase = true) -> "⚔️"
        category.contains("shoplifting", ignoreCase = true) -> "🛍️"
        category.contains("theft-from-the-person", ignoreCase = true) -> "🎒"
        category.contains("bicycle", ignoreCase = true) -> "🚲"
        category.contains("vehicle", ignoreCase = true) -> "🚗"
        category.contains("robbery", ignoreCase = true) -> "💰"
        category.contains("public-order", ignoreCase = true) -> "📣"
        category.contains("weapons", ignoreCase = true) -> "🔪"
        category.contains("other-theft", ignoreCase = true) -> "👜"
        category.contains("other-crime", ignoreCase = true) -> "❓"
        else -> "📍"
    }

    /** Short display name, e.g. "Violent crime" instead of "Violence and sexual offences". */
    fun shortName(category: String): String = when {
        category.contains("anti-social", ignoreCase = true) -> "Anti-social behaviour"
        category.contains("bike", ignoreCase = true) -> "Bicycle theft"
        category.contains("criminal-damage", ignoreCase = true) -> "Criminal damage"
        category.contains("other-crime", ignoreCase = true) -> "Other crime"
        category.contains("other-theft", ignoreCase = true) -> "Other theft"
        category.contains("possession-of-weapons", ignoreCase = true) -> "Weapons"
        category.contains("public-order", ignoreCase = true) -> "Public order"
        category.contains("robbery", ignoreCase = true) -> "Robbery"
        category.contains("shoplifting", ignoreCase = true) -> "Shoplifting"
        category.contains("theft-from-the-person", ignoreCase = true) -> "Pickpocketing"
        category.contains("vehicle", ignoreCase = true) -> "Vehicle crime"
        category.contains("violent", ignoreCase = true) -> "Violent crime"
        category.contains("drugs", ignoreCase = true) -> "Drugs"
        category.contains("burglary", ignoreCase = true) -> "Burglary"
        else -> CrimeCategoryColors.displayName(category)
    }
}
