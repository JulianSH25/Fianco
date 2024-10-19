package Backend.UtilityFunctions

import java.awt.Point
import Backend.PlayerToMove

/**
 * Creates a map of piece positions from the given board array.
 *
 * @param pieceArray The game board represented as a 2D array.
 * @return A mutable map where keys are Points representing positions on the board,
 *         and values are the PlayerToMove owning the piece at that position.
 */
fun createPiecePositionsFromBoard(pieceArray: Array<Array<Int>>): MutableMap<Point, PlayerToMove> {
    val positions = mutableMapOf<Point, PlayerToMove>()
    for (i in pieceArray.indices) {
        for (j in pieceArray[i].indices) {
            val value = pieceArray[i][j]
            if (value != 0) {
                positions[Point(i, j)] = if (value == 1) PlayerToMove.PlayerOne else PlayerToMove.PlayerTwo
            }
        }
    }
    return positions
}

/**
 * Creates a deep copy of the given board array.
 *
 * @param board The original game board.
 * @return A new 2D array that is a copy of the original board.
 */
fun copyBoard(board: Array<Array<Int>>): Array<Array<Int>> {
    return board.map { it.copyOf() }.toTypedArray()
}