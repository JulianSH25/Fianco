package Backend

import java.awt.*
import java.io.IOException
import kotlin.random.Random

class RandomEngine {
    fun pickRandomMove(pieceMap: Map<Point, List<Point>>): Pair<Point, Point> {
        if (pieceMap.isEmpty()) throw IOException("Empty piece map")

        // Step 1: Get a random key (Point) from the map
        val keys = pieceMap.keys.toList()
        val randomKey = keys[Random.nextInt(keys.size)]

        // Step 2: Get the associated list of points
        val moves = pieceMap[randomKey] ?: throw IOException("piece map key does not exist")

        // Step 3: Get a random move from the list of moves
        if (moves.isEmpty()) return throw IOException("piece map move does not exist (key error)")
        val randomMove = moves[Random.nextInt(moves.size)]

        return Pair(randomKey, randomMove)
    }
}