package com.saunaltech.mindgate.app.model

/**
 * Représente la configuration d'un quiz.
 * [difficulties] est la liste ordonnée des niveaux de difficulté (1–5) pour chaque question.
 * Ex : [1, 2, 3, 4, 5] = 5 questions, une par niveau.
 *      [1, 1, 2, 3, 3, 4, 4, 5] = 8 questions, pondérées vers le bas.
 */
data class QuizConfig(
    val difficulties: List<Int> = DEFAULT_DIFFICULTIES
) {
    val questionCount: Int get() = difficulties.size
    val averageDifficulty: Float get() = difficulties.average().toFloat()
    val minDifficulty: Int get() = difficulties.min()
    val maxDifficulty: Int get() = difficulties.max()

    /** Retourne un map difficulté → nombre de questions requises */
    fun difficultyDistribution(): Map<Int, Int> =
        difficulties.groupingBy { it }.eachCount()

    companion object {
        val DEFAULT_DIFFICULTIES = listOf(1, 2, 3, 4, 5)

        val PRESET_BALANCED = QuizConfig(listOf(1, 2, 3, 4, 5))
        val PRESET_PROGRESSIVE = QuizConfig(listOf(1, 1, 2, 3, 4, 5, 5))
        val PRESET_EASY = QuizConfig(listOf(1, 1, 1, 2, 3))
        val PRESET_EXPERT = QuizConfig(listOf(4, 4, 5, 5, 5))

        val DIFFICULTY_LABELS = mapOf(
            1 to "Novice",
            2 to "Initié",
            3 to "Intermédiaire",
            4 to "Avancé",
            5 to "Expert"
        )

        val DIFFICULTY_DESCRIPTIONS = mapOf(
            1 to "Réponse évidente pour un débutant",
            2 to "Connu de la plupart des personnes familiarisées",
            3 to "La moitié des initiés pourrait répondre",
            4 to "Nécessite une connaissance approfondie",
            5 to "Souvent méconnu même des experts"
        )
    }
}