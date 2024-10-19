package Backend

import java.awt.Point
import java.util.concurrent.ConcurrentHashMap

/**
 * Object implementing the History Heuristic for move ordering in alpha-beta pruning.
 *
 * The History Heuristic keeps track of how often certain moves have caused cutoffs during the search.
 * Moves that frequently cause cutoffs are considered "good" and are prioritized in move ordering.
 */
object HistoryHeuristic {
    private val historyTable = ConcurrentHashMap<Pair<Point, Point>, Int>()
    private const val HISTORY_INCREMENT = 1
    private const val MAX_HISTORY_SCORE = Int.MAX_VALUE
    private const val DECAY_FACTOR = 0.95

    /**
     * Increments the history score for a given move.
     *
     * @param move The move to increment the score for.
     */
    fun increment(move: Pair<Point, Point>) {
        historyTable[move] = (historyTable.getOrDefault(move, 0) + HISTORY_INCREMENT).coerceAtMost(MAX_HISTORY_SCORE)
    }

    /**
     * Retrieves the history score for a given move.
     *
     * @param move The move to get the score for.
     * @return The history score of the move.
     */
    fun getScore(move: Pair<Point, Point>): Int {
        return historyTable.getOrDefault(move, 0)
    }

    /**
     * Decays all history scores to prevent overflow and to reduce the influence of older moves.
     */
    fun decay() {
        val keys = historyTable.keys.toList()
        for (move in keys) {
            val newScore = (historyTable[move]?.times(DECAY_FACTOR))?.toInt() ?: 0
            if (newScore > 0) {
                historyTable[move] = newScore
            } else {
                historyTable.remove(move)
            }
        }
    }

    /**
     * Resets the history table by clearing all stored history scores.
     */
    fun reset() {
        historyTable.clear()
    }
}