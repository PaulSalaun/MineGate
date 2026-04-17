package com.saunaltech.mindgate.app.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.saunaltech.mindgate.app.model.QuizConfig

class MindGatePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Apps bloquées ────────────────────────────────────────────────────────

    fun saveBlockedApps(packages: Set<String>) {
        prefs.edit { putStringSet(KEY_BLOCKED_APPS, packages) }
    }

    fun loadBlockedApps(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    // ── Thèmes ───────────────────────────────────────────────────────────────

    fun loadActiveThemes(): List<String> {
        val set = prefs.getStringSet(KEY_ACTIVE_THEMES, emptySet()) ?: emptySet()
        return set.toList()
    }

    fun saveActiveThemes(themes: Set<String>) {
        prefs.edit { putStringSet(KEY_ACTIVE_THEMES, themes) }
    }

    fun saveActiveThemeIds(ids: Set<Long>) {
        val strings = ids.map { it.toString() }.toSet()
        prefs.edit { putStringSet(KEY_ACTIVE_THEME_IDS, strings) }
    }

    fun loadActiveThemeIds(): List<Long> {
        val strings = prefs.getStringSet(KEY_ACTIVE_THEME_IDS, emptySet()) ?: emptySet()
        return strings.map { it.toLong() }
    }

    // ── Langue ───────────────────────────────────────────────────────────────

    fun saveLangue(langue: String) {
        prefs.edit { putString(KEY_LANGUE, langue) }
    }

    fun loadLangue(): String {
        return prefs.getString(KEY_LANGUE, "FR") ?: "FR"
    }

    // ── Configuration du quiz ────────────────────────────────────────────────

    /**
     * Sauvegarde la config du quiz sous forme de chaîne CSV.
     * Ex : [1,2,3,4,5] → "1,2,3,4,5"
     */
    fun saveQuizConfig(config: QuizConfig) {
        val csv = config.difficulties.joinToString(",")
        prefs.edit { putString(KEY_QUIZ_CONFIG, csv) }
    }

    /**
     * Charge la config du quiz. Retourne [QuizConfig.DEFAULT_DIFFICULTIES] si absente ou invalide.
     */
    fun loadQuizConfig(): QuizConfig {
        val csv = prefs.getString(KEY_QUIZ_CONFIG, null) ?: return QuizConfig()
        return try {
            val difficulties = csv.split(",").map { it.trim().toInt() }.filter { it in 1..5 }
            if (difficulties.isEmpty()) QuizConfig() else QuizConfig(difficulties)
        } catch (_: Exception) {
            QuizConfig()
        }
    }

    // ────────────────────────────────────────────────────────────────────────

    companion object {
        private const val PREFS_NAME = "mindgate_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_ACTIVE_THEMES = "active_themes"
        private const val KEY_ACTIVE_THEME_IDS = "active_theme_ids"
        private const val KEY_LANGUE = "langue"
        private const val KEY_QUIZ_CONFIG = "quiz_config"
    }
}