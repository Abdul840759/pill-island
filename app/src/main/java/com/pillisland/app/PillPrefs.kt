package com.pillisland.app

import android.content.Context

object PillPrefs {
    private const val PREFS_NAME = "pill_island_prefs"
    private const val KEY_SIZE = "pill_size"
    private const val KEY_SHAPE = "pill_shape"

    fun getSize(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_SIZE, 1.0f)
    }

    fun setSize(context: Context, size: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_SIZE, size).apply()
    }

    fun getShape(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SHAPE, "ios") ?: "ios"
    }

    fun setShape(context: Context, shape: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SHAPE, shape).apply()
    }
}