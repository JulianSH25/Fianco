package Frontend

import java.awt.Color
import java.awt.Point
import javax.swing.text.Position

object PieceManager {
    private val pieceArray: Array<Array<Int>> = Array(9) { Array(9) { 0 } }

    val piecePositions = mutableMapOf<Point, Color>()

    // Get a piece at a specific row and column
    fun getPiece(row: Int, column: Int): Int = pieceArray[row][column]

    // Set a piece at a specific row and column
    fun setPiece(row: Int, column: Int, value: Int) {
        pieceArray[row][column] = value
        if (value == 0) piecePositions.remove(Point(row,column)) else
            piecePositions[Point(row,column)] = if (value == 1) Color.WHITE else Color.BLACK

        //printBoard() // Print the board for debugging
    }

    fun movePiece(oldPosition: Point, newPosition: Point) {
        setPiece(newPosition.x, newPosition.y, getPiece(oldPosition.x, oldPosition.y))
        setPiece(oldPosition.x, oldPosition.y, 0)
    }

    fun capturePiece(oldPosition: Point, newPosition: Point) {
        setPiece(newPosition.x, newPosition.y, getPiece(oldPosition.x, oldPosition.y))
        setPiece(oldPosition.x, oldPosition.y, 0)

        val dx = (newPosition.x - oldPosition.x)/2
        val dy = (newPosition.y - oldPosition.y)/2

        setPiece(oldPosition.x + dx, oldPosition.y + dy, 0)

        //print("Old Position: $oldPosition, Captured Position: ${oldPosition.x + dx}, ${oldPosition.y + dy}, New Position: $newPosition")
    }

    // Get a copy of the current board
    fun getBoardCopy(): Array<Array<Int>> {
        return pieceArray.map { it.copyOf() }.toTypedArray()
    }

    // Optional: Add a function to print the board in a readable format
    private fun printBoard() {
        //println("Current Board:")
        for (row in pieceArray) {
            for (cell in row) {
                print("$cell ")
            }
            println()
        }
        println()
    }
}

