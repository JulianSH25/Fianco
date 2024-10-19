package Backend

import java.awt.Point

/**
 * Manages the game pieces and board state.
 *
 * Provides methods to get, set, move, and capture pieces on the board.
 */
class PieceManager {
    private val pieceArray: Array<Array<Int>> = Array(9) { Array(9) { 0 } }
    val piecePositions = mutableMapOf<Point, PlayerToMove>()

    /**
     * Gets the piece at a specific row and column.
     *
     * @param row The row index.
     * @param column The column index.
     * @return The piece value (0 for empty, 1 for PlayerOne, 2 for PlayerTwo).
     */
    fun getPiece(row: Int, column: Int): Int = pieceArray[row][column]

    /**
     * Sets the piece at a specific row and column.
     *
     * @param row The row index.
     * @param column The column index.
     * @param value The piece value to set (0 for empty, 1 for PlayerOne, 2 for PlayerTwo).
     */
    fun setPiece(row: Int, column: Int, value: Int) {
        pieceArray[row][column] = value
        val point = Point(row, column)
        if (value == 0) {
            piecePositions.remove(point)
        } else {
            piecePositions[point] = if (value == 1) PlayerToMove.PlayerOne else PlayerToMove.PlayerTwo
        }
    }

    /**
     * Moves a piece from one position to another.
     *
     * @param oldPosition The starting position of the piece.
     * @param newPosition The destination position of the piece.
     */
    fun movePiece(oldPosition: Point, newPosition: Point) {
        setPiece(newPosition.x, newPosition.y, getPiece(oldPosition.x, oldPosition.y))
        setPiece(oldPosition.x, oldPosition.y, 0)
    }

    /**
     * Captures a piece by moving to the new position and removing the captured piece.
     *
     * @param oldPosition The starting position of the piece.
     * @param newPosition The destination position of the piece.
     */
    fun capturePiece(oldPosition: Point, newPosition: Point) {
        setPiece(newPosition.x, newPosition.y, getPiece(oldPosition.x, oldPosition.y))
        setPiece(oldPosition.x, oldPosition.y, 0)

        val dx = (newPosition.x - oldPosition.x) / 2
        val dy = (newPosition.y - oldPosition.y) / 2

        setPiece(oldPosition.x + dx, oldPosition.y + dy, 0)
    }

    /**
     * Gets a copy of the current board state.
     *
     * @return A 2D array representing the board.
     */
    fun getBoardCopy(): Array<Array<Int>> {
        return pieceArray.map { it.copyOf() }.toTypedArray()
    }

    /**
     * Resets the board to an empty state.
     */
    fun reset() {
        // Clear the piece array
        for (row in pieceArray.indices) {
            for (col in pieceArray[row].indices) {
                pieceArray[row][col] = 0
            }
        }
        // Clear the piece positions
        piecePositions.clear()
    }
}