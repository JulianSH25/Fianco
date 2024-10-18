package Backend.UtilityFunctions

import Backend.PieceManager as PieceManager
import java.awt.Point
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.mutableMapOf
import kotlin.math.abs
import Backend.Player as player
import Backend.PlayerToMove

/*
PieceArray is the only current attribute not passed as an argument but instead accessible globally as a singleton.
 */

fun getSquareName(col: Int, row: Int): String {
    val rowLetter = 'A' + row
    val colNumber = col + 1
    return "$rowLetter$colNumber"
}

fun checkVictory(pm: PieceManager): PlayerToMove? {
    for (i in 0 until 9) {
        if (pm.getPiece(8, i) == 2) { // Black piece reached the bottom
            return PlayerToMove.PlayerTwo
        }
        if (pm.getPiece(0, i) == 1) { // White piece reached the top
            return PlayerToMove.PlayerOne
        }
    }
    return null // No winner yet
}

fun checkVictory(pm: PieceManager, pieceArray: Array<Array<Int>> = pm.getBoardCopy()): PlayerToMove? {
    for (i in 0 until 9) {
        if (pieceArray[8][i] == 2) { // Black piece reached the bottom
            return PlayerToMove.PlayerTwo
        }
        if (pieceArray[0][i] == 1) { // White piece reached the top
            return PlayerToMove.PlayerOne
        }
    }
    return null // No winner yet
}

fun generateTacticalMoves(
    pm: PieceManager,
    playerID: Int,
    board: Array<Array<Int>>,
    positions: Map<Point, PlayerToMove>
): Pair<Map<Point, List<Point>>, String> {
    val player = if (playerID == 1) PlayerToMove.PlayerOne else PlayerToMove.PlayerTwo
    val captureMoves = checkCapture(pm, positions, player, pieceArray = board)
    return if (captureMoves != null) Pair(captureMoves, "Capture") else Pair(emptyMap(), "NoCapture")
}

fun isQuiescentPosition(
    pm: PieceManager,
    board: Array<Array<Int>>,
    player: PlayerToMove
): Boolean {
    val piecePositions = createPiecePositionsFromBoard(board)
    val captures = checkCapture(pm, piecePositions, player, pieceArray = board)
    return captures == null  // If no captures are available, the position is quiescent
}

fun checkCapture(
    pm: PieceManager,
    piecePositions: Map<Point, PlayerToMove> = pm.piecePositions,
    currentPlayer: PlayerToMove,
    AIMove: Boolean? = false,
    pieceArray: Array<Array<Int>> = pm.getBoardCopy()
): Map<Point, List<Point>>? {

    val opponentIdentifier = if (currentPlayer == PlayerToMove.PlayerOne) 2 else 1
    val multiplier = if (currentPlayer == PlayerToMove.PlayerOne) -1 else 1
    val captureMap = mutableMapOf<Point, MutableList<Point>>()

    for ((point, playerToMove) in piecePositions) {
        if (playerToMove == currentPlayer) {
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
                    if (pieceArray[newX][newY] == opponentIdentifier && pieceArray[newX2][newY2] == 0) {
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

fun isValidMove(pm: PieceManager, currentPosition: Point, newPosition: Point, piecePositions: Map<Point, PlayerToMove>, currentPlayer: PlayerToMove): Pair<Boolean, Map<Point, List<Point>>?> {
    //println("New Position: $newPosition, Current Position: $currentPosition")
    //println("New Position x: ${newPosition.x}, New Position y: ${newPosition.y}")
    // 1. Check if the new position is within the board bounds.
    if (newPosition.x !in 0..8 || newPosition.y !in 0..8) return Pair(false, null)

    println("currentPosition: $currentPosition, to newPosition: $newPosition, as player $currentPlayer")

    // 2. Check if the new position is occupied.
    val capturingPieces = checkCapture(pm, piecePositions, currentPlayer)
    if (capturingPieces != null) {
        println("Pieces to capture")
        val dx = abs(newPosition.x - currentPosition.x)
        val dy = abs(newPosition.y - currentPosition.y)

        if (currentPosition !in capturingPieces) return Pair(false, null)
        if (dx != 2 || dy != 2) return Pair(false, null)

        return Pair(true, capturingPieces) // Capture move is valid
    } else {
        println("Regular Move")
        // If not occupied, it's a normal move (one straight or one to the side)
        val dx = abs(newPosition.x - currentPosition.x)
        val dy = abs(newPosition.y - currentPosition.y)
        //println("dx: $dx, dy: $dy")

        if (piecePositions[currentPosition] != currentPlayer){
            println("Player Association Error")
            return Pair(false, null)
        }
        if (piecePositions.containsKey(newPosition)){
            println("Position blocked: $newPosition with value ${piecePositions[newPosition]}")
            return Pair(false, null)
        }

        // check whether a move of manhattan distance greater 1 or no move has been made (Distance 0)
        if (dx + dy != 1){
            println("Manhattan Distance Error")
            return Pair(false, null)
        }

        // Check for backward movement (assuming RED moves "up" the board and BLACK moves "down")
        if (currentPlayer == PlayerToMove.PlayerTwo && newPosition.x < currentPosition.x) return Pair(false, null)
        if (currentPlayer == PlayerToMove.PlayerOne && newPosition.x > currentPosition.x){
            println("Backwards movement detected")
            return Pair(false, null)
        }
    }

    // 4. (Optional) Add more complex rules here (special moves, etc.)

    return Pair(true, null)
}

fun checkValidMove(
    currentPosition: Point,
    newPosition: Point,
    piecePositions: Map<Point, PlayerToMove>,
    currentPlayer: PlayerToMove,
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
    if (currentPlayer == PlayerToMove.PlayerTwo && newPosition.x < currentPosition.x) return false
    if (currentPlayer == PlayerToMove.PlayerOne && newPosition.x > currentPosition.x) return false

    return true
}