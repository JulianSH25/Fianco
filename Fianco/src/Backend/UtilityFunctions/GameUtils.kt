package Backend.UtilityFunctions

import Backend.PieceManager as PieceManager
import java.awt.Color
import java.awt.Point
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.mutableMapOf
import kotlin.math.abs

/*
PieceArray is the only current attribute not passed as an argument but instead accessible globally as a singleton.
 */

fun getSquareName(col: Int, row: Int): String {
    val rowLetter = 'A' + row
    val colNumber = col + 1
    return "$rowLetter$colNumber"
}

fun checkVictory(pm: PieceManager): Color? {
    for (i in 0 until 9) {
        if (pm.getPiece(8, i) == 2) { // Black piece reached the bottom
            return Color.BLACK
        }
        if (pm.getPiece(0, i) == 1) { // White piece reached the top
            return Color.WHITE
        }
    }
    return null // No winner yet
}

fun checkVictory(pm: PieceManager, pieceArray: Array<Array<Int>> = pm.getBoardCopy()): Color? {
    for (i in 0 until 9) {
        if (pieceArray[8][i] == 2) { // Black piece reached the bottom
            return Color.BLACK
        }
        if (pieceArray[0][i] == 1) { // White piece reached the top
            return Color.WHITE
        }
    }
    return null // No winner yet
}

fun checkCapture(
    pm: PieceManager,
    piecePositions: Map<Point, Color> = pm.piecePositions,
    currentPlayer: Color,
    AIMove: Boolean? = false,
    pieceArray: Array<Array<Int>> = pm.getBoardCopy()
): Map<Point, List<Point>>? {

    val opponentColour = if (currentPlayer == Color.WHITE) 2 else 1
    val multiplier = if (currentPlayer == Color.WHITE) -1 else 1
    val captureMap = mutableMapOf<Point, MutableList<Point>>()

    for ((point, color) in piecePositions) {
        if (color == currentPlayer) {
            val x = point.x
            val y = point.y

            val directions = listOf(-1, 1)

            for (i in directions) {
                val newX = x + multiplier
                val newY = y + (1 * i)
                val newX2 = x + (2 * multiplier)
                val newY2 = y + (2 * i)

                if (newX in 1 until 8 && newY in 1 until 8 &&
                    newX2 in 0 until 9 && newY2 in 0 until 9
                ) {
                    if (pieceArray[newX][newY] == opponentColour && pieceArray[newX2][newY2] == 0) {
                        val currentPosition = Point(x, y)
                        val capturePoints = captureMap.getOrPut(currentPosition) { mutableListOf() }
                        capturePoints.add(if (AIMove == true) Point(newX2, newY2) else Point(newX, newY))
                        //println("found capture move: $currentPosition via $newX, $newY, to $newX2, $newY2")
                    }
                }
            }
        }
    }
    return if (captureMap.isNotEmpty()) captureMap else null
}

fun isValidMove(pm: PieceManager, currentPosition: Point, newPosition: Point, piecePositions: Map<Point, Color>, currentPlayer: Color): Pair<Boolean, Map<Point, List<Point>>?> {
    //println("New Position: $newPosition, Current Position: $currentPosition")
    //println("New Position x: ${newPosition.x}, New Position y: ${newPosition.y}")
    // 1. Check if the new position is within the board bounds.
    if (newPosition.x !in 0..8 || newPosition.y !in 0..8) return Pair(false, null)

    // 2. Check if the new position is occupied.
    val capturingPieces = checkCapture(pm, piecePositions, currentPlayer)
    if (capturingPieces != null) {
        val dx = abs(newPosition.x - currentPosition.x)
        val dy = abs(newPosition.y - currentPosition.y)

        if (currentPosition !in capturingPieces) return Pair(false, null)
        if (dx != 2 || dy != 2) return Pair(false, null)

        return Pair(true, capturingPieces) // Capture move is valid
    } else {
        // If not occupied, it's a normal move (one straight or one to the side)
        val dx = abs(newPosition.x - currentPosition.x)
        val dy = abs(newPosition.y - currentPosition.y)
        //println("dx: $dx, dy: $dy")

        if (piecePositions[currentPosition] != currentPlayer) return Pair(false, null)
        if (piecePositions.containsKey(newPosition)){
            //print("Position blocked: $newPosition with value ${piecePositions[newPosition]}")
            return Pair(false, null)
        }

        // check whether a move of manhattan distance greater 1 or no move has been made (Distance 0)
        if (dx + dy != 1) return Pair(false, null)

        // Check for backward movement (assuming RED moves "up" the board and BLACK moves "down")
        if (currentPlayer == Color.BLACK && newPosition.x < currentPosition.x) return Pair(false, null)
        if (currentPlayer == Color.WHITE && newPosition.x > currentPosition.x) return Pair(false, null)
    }

    // 4. (Optional) Add more complex rules here (special moves, etc.)

    return Pair(true, null)
}

fun checkValidMove(
    currentPosition: Point,
    newPosition: Point,
    piecePositions: Map<Point, Color>,
    currentPlayer: Color,
): Boolean {
    // 1. Check if the new position is within the board bounds.
    if (newPosition.x !in 0..8 || newPosition.y !in 0..8) return false

    // 2. Check if the new position is occupied.
    if (piecePositions.containsKey(newPosition)) {
        //println("Position blocked: $newPosition with value ${piecePositions[newPosition]}")
        return false
    }

    val dx = abs(newPosition.x - currentPosition.x)
    val dy = abs(newPosition.y - currentPosition.y)
    //println("dx: $dx, dy: $dy")

    if (piecePositions[currentPosition] != currentPlayer) return false

    // Check whether a move of Manhattan distance greater than 1 or no move has been made (distance 0)
    if (dx + dy != 1) return false

    // Check for backward movement
    if (currentPlayer == Color.BLACK && newPosition.x < currentPosition.x) return false
    if (currentPlayer == Color.WHITE && newPosition.x > currentPosition.x) return false

    return true
}