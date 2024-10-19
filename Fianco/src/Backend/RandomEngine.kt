package Backend

import java.awt.Point
import java.io.IOException
import kotlin.random.Random

/**
 * RandomEngine selects moves randomly from the available moves.
 */
class RandomEngine {
    /**
     * Picks a random move from the provided piece map.
     *
     * @param pieceMap A map of pieces to their possible moves.
     * @return A Pair containing the starting position and the destination position.
     * @throws IOException If the piece map is empty or invalid.
     */
    fun pickRandomMove(pieceMap: Map<Point, List<Point>>): Pair<Point, Point> {
        if (pieceMap.isEmpty()) throw IOException("Empty piece map")

        // Step 1: Get a random starting position (key) from the map
        val keys = pieceMap.keys.toList()
        val randomKey = keys[Random.nextInt(keys.size)]

        // Step 2: Get the list of possible moves for that position
        val moves = pieceMap[randomKey] ?: throw IOException("Piece map key does not exist")

        // Step 3: Get a random move from the list of moves
        if (moves.isEmpty()) throw IOException("Piece map move does not exist (key error)")
        val randomMove = moves[Random.nextInt(moves.size)]

        return Pair(randomKey, randomMove)
    }
}