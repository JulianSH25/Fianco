package Backend

import java.awt.Point
import java.util.concurrent.ConcurrentHashMap

object HistoryHeuristic {
    private val historyTable = ConcurrentHashMap<Pair<Point, Point>, Int>()
    private val killerMoves = ConcurrentHashMap<Pair<Point, Point>, Int>()
    private const val HISTORY_INCREMENT = 1
    private const val MAX_HISTORY_SCORE = Int.MAX_VALUE
    private const val DECAY_FACTOR = 0.95

    /**
     * Increment the history score for a move.
     */
    fun increment(move: Pair<Point, Point>) {
        historyTable[move] = (historyTable.getOrDefault(move, 0) + HISTORY_INCREMENT).coerceAtMost(MAX_HISTORY_SCORE)
    }

    /**
     * Get the history score for a move.
     */
    fun getScore(move: Pair<Point, Point>): Int {
        return historyTable.getOrDefault(move, 0)
    }

    /**
     * Decay all history scores to prevent overflow.
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
     * Reset the history table (optional).
     */
    fun reset() {
        historyTable.clear()
    }
}