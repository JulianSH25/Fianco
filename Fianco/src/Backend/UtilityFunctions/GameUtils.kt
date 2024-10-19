package Backend.UtilityFunctions

import Backend.PieceManager
import java.awt.Point
import kotlin.math.abs
import Backend.PlayerToMove

/**
 * Retrieves the square name based on its column and row indices.
 *
 * @param col The column index.
 * @param row The row index.
 * @return The name of the square in chess notation.
 */

fun getSquareName(col: Int, row: Int): String {
    val rowLetter = 'A' + row
    val colNumber = col + 1
    return "$rowLetter$colNumber"
}

/**
 * Checks if a player has won the game by reaching the opponent's baseline.
 *
 * @param pm The PieceManager instance.
 * @return The PlayerToMove who won, or null if no one has won yet.
 */
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

/**
 * Checks if a player has won the game by reaching the opponent's baseline.
 *
 * @param pm The PieceManager instance.
 * @param pieceArray The game board array.
 * @return The PlayerToMove who won, or null if no one has won yet.
 */
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

/**
 * Determines if a position is quiescent (i.e., no captures are possible).
 *
 * @param pm The PieceManager instance.
 * @param board The game board array.
 * @param player The current player.
 * @return True if the position is quiescent; False otherwise.
 */
fun isQuiescentPosition(
    pm: PieceManager,
    board: Array<Array<Int>>,
    player: PlayerToMove
): Boolean {
    val piecePositions = createPiecePositionsFromBoard(board)
    val captures = checkCapture(pm, piecePositions, player, pieceArray = board)
    return captures == null  // If no captures are available, the position is quiescent
}

/**
 * Checks for possible capture moves for the current player.
 *
 * @param pm The PieceManager instance.
 * @param piecePositions The positions of the pieces.
 * @param currentPlayer The current player.
 * @param AIMove Optional flag to adjust coordinates for AI moves.
 * @param pieceArray The game board array.
 * @return A map of positions to lists of possible capture moves, or null if none are available.
 */
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
                    }
                }
            }
        }
    }
    return if (captureMap.isNotEmpty()) captureMap else null
}

/**
 * Validates a move from the current position to a new position.
 *
 * @param pm The PieceManager instance.
 * @param currentPosition The current position of the piece.
 * @param newPosition The desired new position.
 * @param piecePositions The positions of the pieces.
 * @param currentPlayer The current player.
 * @return A Pair where the first element is a Boolean indicating if the move is valid,
 *         and the second element is a map of capture moves if applicable.
 */
fun isValidMove(
    pm: PieceManager,
    currentPosition: Point,
    newPosition: Point,
    piecePositions: Map<Point, PlayerToMove>,
    currentPlayer: PlayerToMove
): Pair<Boolean, Map<Point, List<Point>>?> {
    // 1. Check if the new position is within the board bounds.
    if (newPosition.x !in 0..8 || newPosition.y !in 0..8) return Pair(false, null)

    println("currentPosition: $currentPosition, to newPosition: $newPosition, as player $currentPlayer")

    // 2. Check for mandatory captures.
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
        // If no captures, check for regular moves.
        val dx = abs(newPosition.x - currentPosition.x)
        val dy = abs(newPosition.y - currentPosition.y)

        if (piecePositions[currentPosition] != currentPlayer){
            println("Player Association Error")
            return Pair(false, null)
        }
        if (piecePositions.containsKey(newPosition)){
            println("Position blocked: $newPosition with value ${piecePositions[newPosition]}")
            return Pair(false, null)
        }

        // Check for valid move distance.
        if (dx + dy != 1){
            println("Manhattan Distance Error")
            return Pair(false, null)
        }

        // Check for backward movement.
        if (currentPlayer == PlayerToMove.PlayerTwo && newPosition.x < currentPosition.x) return Pair(false, null)
        if (currentPlayer == PlayerToMove.PlayerOne && newPosition.x > currentPosition.x){
            println("Backwards movement detected")
            return Pair(false, null)
        }
    }

    return Pair(true, null)
}

/**
 * Checks if a move from the current position to a new position is valid.
 *
 * @param currentPosition The current position of the piece.
 * @param newPosition The desired new position.
 * @param piecePositions The positions of the pieces.
 * @param currentPlayer The current player.
 * @return True if the move is valid; False otherwise.
 */
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
        return false
    }

    val dx = abs(newPosition.x - currentPosition.x)
    val dy = abs(newPosition.y - currentPosition.y)

    if (piecePositions[currentPosition] != currentPlayer) return false

    // Check whether a move of Manhattan distance greater than 1 or no move has been made (distance 0)
    if (dx + dy != 1) return false

    // Check for backward movement
    if (currentPlayer == PlayerToMove.PlayerTwo && newPosition.x < currentPosition.x) return false
    if (currentPlayer == PlayerToMove.PlayerOne && newPosition.x > currentPosition.x) return false

    return true
}