package Backend

import java.awt.Point
import kotlin.random.Random

class Zobrist {
    private val zobristMap: Array<Array<Array<Long>>> = Array(9) { Array(9) { Array(3) { Random.nextLong() } } }

    fun calculateInitialHash(board: Array<Array<Int>>): Long {
        var zobristHash: Long = 0L

        for (row in 0..8){
            for (column in 0..8){
                val thirdDim = board[row][column]
                if (thirdDim != 0){
                    zobristHash = zobristHash xor zobristMap[row][column][thirdDim]
                }
            }
        }
        return zobristHash
    }

    fun updateHash(oldHash: Long, move: Pair<Point, Point>, playerToMove: Int): Long {
        val oldPosition = move.first
        val newPosition = move.second

        var newHash = oldHash xor zobristMap[oldPosition.x][oldPosition.y][playerToMove]
        newHash = newHash xor zobristMap[newPosition.x][newPosition.y][playerToMove]

        return newHash
    }

    fun updateHashCapture(oldHash: Long, removedPosition: Point, playerToMove: Int): Long {
        val opponentPlayer = if (playerToMove == 1) 2 else 1
        val newHash = oldHash xor zobristMap[removedPosition.x][removedPosition.y][opponentPlayer]
        return newHash
    }
}