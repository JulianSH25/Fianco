package Backend

import java.awt.Point
import kotlin.random.Random
import kotlin.random.nextULong

class Zobrist {
    private val zobristMap: Array<Array<Array<ULong>>> = Array(9) {
        Array(9) {
            Array(2) {
                Random.nextULong()
            }
        }
    }

    var currentBoardHash: ULong = 0u

    fun calculateInitialHash(board: Array<Array<Int>>): ULong {
        var zobristHash: ULong = 0u

        for (row in 0..8){
            for (column in 0..8){
                val thirdDim = board[row][column]
                if (thirdDim != 0){
                    zobristHash = zobristHash xor zobristMap[row][column][thirdDim-1]
                }
            }
        }
        return zobristHash
    }

    fun updateHash(oldHash: ULong, move: Pair<Point, Point>, playerToMove: Int): ULong {
        val oldPosition = move.first
        val newPosition = move.second

        var newHash = oldHash xor zobristMap[oldPosition.x][oldPosition.y][playerToMove-1]
        newHash = newHash xor zobristMap[newPosition.x][newPosition.y][playerToMove-1]

        return newHash
    }

    fun updateHashCapture(oldHash: ULong, removedPosition: Point, playerToMove: Int): ULong {
        val opponentPlayer = if (playerToMove == 1) 2 else 1
        val newHash = oldHash xor zobristMap[removedPosition.x][removedPosition.y][opponentPlayer-1]
        return newHash
    }
}