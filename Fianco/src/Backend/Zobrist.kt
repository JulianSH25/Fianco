package Backend

import java.awt.Point
import kotlin.random.Random
import kotlin.random.nextULong

/**
 * Implements Zobrist hashing for efficient board state hashing.
 *
 * Used in the transposition table to uniquely identify board states.
 */
class Zobrist {
    private val zobristMap: Array<Array<Array<ULong>>> = Array(9) {
        Array(9) {
            Array(2) {
                Random.nextULong()
            }
        }
    }

    var currentBoardHash: ULong = 0u

    /**
     * Calculates the initial hash value for a given board state.
     *
     * @param board The game board.
     * @return The Zobrist hash value of the board.
     */
    fun calculateInitialHash(board: Array<Array<Int>>): ULong {
        var zobristHash: ULong = 0u

        for (row in 0..8){
            for (column in 0..8){
                val piece = board[row][column]
                if (piece != 0) {
                    zobristHash = zobristHash xor zobristMap[row][column][piece - 1]
                }
            }
        }
        return zobristHash
    }

    /**
     * Updates the hash value after a move is made.
     *
     * @param oldHash The previous hash value.
     * @param move The move made.
     * @param playerToMove The player making the move.
     * @return The new hash value.
     */
    fun updateHash(oldHash: ULong, move: Pair<Point, Point>, playerToMove: Int): ULong {
        val oldPosition = move.first
        val newPosition = move.second

        var newHash = oldHash xor zobristMap[oldPosition.x][oldPosition.y][playerToMove-1]
        newHash = newHash xor zobristMap[newPosition.x][newPosition.y][playerToMove-1]

        return newHash
    }

    /**
     * Updates the hash value after a capture move.
     *
     * @param oldHash The previous hash value.
     * @param removedPosition The position of the captured piece.
     * @param playerToMove The player making the move.
     * @return The new hash value.
     */
    fun updateHashCapture(oldHash: ULong, removedPosition: Point, playerToMove: Int): ULong {
        val opponentPlayer = if (playerToMove == 1) 2 else 1
        val newHash = oldHash xor zobristMap[removedPosition.x][removedPosition.y][opponentPlayer-1]
        return newHash
    }
}