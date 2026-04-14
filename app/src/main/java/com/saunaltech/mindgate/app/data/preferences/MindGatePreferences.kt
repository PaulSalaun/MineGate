package com.saunaltech.mindgate.app.data.preferences

import android.content.Context
import androidx.core.content.edit

class MindGatePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveBlockedApps(packages: Set<String>) {
        prefs.edit { putStringSet(KEY_BLOCKED_APPS, packages) }
    }

    fun loadBlockedApps(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    fun loadActiveThemes(): List<String> {
        val set = prefs.getStringSet(KEY_ACTIVE_THEMES, emptySet()) ?: emptySet()
        return set.toList()  // Liste vide = tous les thèmes
    }

    fun saveActiveThemes(themes: Set<String>) {
        prefs.edit { putStringSet(KEY_ACTIVE_THEMES, themes) }
    }

    companion object {
        private const val PREFS_NAME = "mindgate_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_ACTIVE_THEMES = "active_themes"
    }
}