package Backend

import java.awt.Point
import Backend.PlayerToMove

class PieceManager {
    private val pieceArray: Array<Array<Int>> = Array(9) { Array(9) { 0 } }
    val piecePositions = mutableMapOf<Point, PlayerToMove>()

    // Get a piece at a specific row and column
    fun getPiece(row: Int, column: Int): Int = pieceArray[row][column]

    // Set a piece at a specific row and column
    fun setPiece(row: Int, column: Int, value: Int) {
        pieceArray[row][column] = value
        val point = Point(row, column)
        if (value == 0) {
            piecePositions.remove(point)
        } else {
            piecePositions[point] = if (value == 1) PlayerToMove.PlayerOne else PlayerToMove.PlayerTwo
        }
    }

    fun movePiece(oldPosition: Point, newPosition: Point) {
        setPiece(newPosition.x, newPosition.y, getPiece(oldPosition.x, oldPosition.y))
        setPiece(oldPosition.x, oldPosition.y, 0)
    }

    fun capturePiece(oldPosition: Point, newPosition: Point) {
        setPiece(newPosition.x, newPosition.y, getPiece(oldPosition.x, oldPosition.y))
        setPiece(oldPosition.x, oldPosition.y, 0)

        val dx = (newPosition.x - oldPosition.x) / 2
        val dy = (newPosition.y - oldPosition.y) / 2

        setPiece(oldPosition.x + dx, oldPosition.y + dy, 0)
    }

    // Get a copy of the current board
    fun getBoardCopy(): Array<Array<Int>> {
        return pieceArray.map { it.copyOf() }.toTypedArray()
    }

    // Initialize the board
    fun initializeBoard() {
        // Implement your board initialization logic here
    }
}