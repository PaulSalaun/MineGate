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

    fun saveLangue(langue: String) {
        prefs.edit { putString(KEY_LANGUE, langue) }
    }

    fun loadLangue(): String {
        return prefs.getString(KEY_LANGUE, "FR") ?: "FR"
    }

    fun saveActiveThemeIds(ids: Set<Long>) {
        val strings = ids.map { it.toString() }.toSet()
        prefs.edit { putStringSet(KEY_ACTIVE_THEME_IDS, strings) }
    }

    fun loadActiveThemeIds(): List<Long> {
        val strings = prefs.getStringSet(KEY_ACTIVE_THEME_IDS, emptySet()) ?: emptySet()
        return strings.map { it.toLong() }
    }

    companion object {
        private const val PREFS_NAME = "mindgate_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_ACTIVE_THEMES = "active_themes"
        private const val KEY_ACTIVE_THEME_IDS = "active_theme_ids"
        private const val KEY_LANGUE = "langue"
    }
}