package com.colorgame.app.utils

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("color_story_prefs", Context.MODE_PRIVATE)

    fun getColoredRegions(levelId: String): Set<Int> {
        val stringSet = prefs.getStringSet("colored_regions_$levelId", emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun saveColoredRegion(levelId: String, regionId: Int) {
        val current = prefs.getStringSet("colored_regions_$levelId", emptySet())?.toMutableSet()
            ?: mutableSetOf()
        current.add(regionId.toString())
        prefs.edit().putStringSet("colored_regions_$levelId", current).apply()
    }

    fun isLevelCompleted(levelId: String): Boolean {
        return prefs.getBoolean("completed_$levelId", false)
    }

    fun setLevelCompleted(levelId: String, completed: Boolean) {
        prefs.edit().putBoolean("completed_$levelId", completed).apply()
    }

    fun isLevelUnlocked(levelId: String, defaultUnlocked: Boolean): Boolean {
        return prefs.getBoolean("unlocked_$levelId", defaultUnlocked)
    }

    fun setLevelUnlocked(levelId: String, unlocked: Boolean) {
        prefs.edit().putBoolean("unlocked_$levelId", unlocked).apply()
    }
}
