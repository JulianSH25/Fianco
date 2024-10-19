package Backend

import Backend.UtilityFunctions.checkCapture
import Backend.UtilityFunctions.checkValidMove
import Backend.UtilityFunctions.createPiecePositionsFromBoard
import java.awt.Point

/**
 * Generates all possible moves for the given player.
 *
 * @param pm The PieceManager instance.
 * @param playerID The ID of the player (1 for PlayerOne, 2 for PlayerTwo).
 * @param pieceArray The current board state.
 * @param positions Optional map of piece positions; if not provided, it will be created from the board.
 * @return A Pair containing a map of positions to lists of valid moves and a String indicating the type of move ("Capture" or "Regular").
 * @throws NullPointerException If no valid moves are found.
 */
fun generateMoves(pm: PieceManager,
    playerID: Int,
    pieceArray: Array<Array<Int>>,
    positions: Map<Point, PlayerToMove>? = null
): Pair<Map<Point, List<Point>>, String> {

    val piecePositions = positions ?: createPiecePositionsFromBoard(pieceArray)
    val player = if (playerID == 1) PlayerToMove.PlayerOne else PlayerToMove.PlayerTwo

    // Check for mandatory capture moves
    val validMoves = checkCapture(pm, piecePositions, player, true, pieceArray)
    if (validMoves != null) {
        return Pair(validMoves, "Capture")
    }

    // No captures available, generate regular moves
    val direction = if (playerID == 1) -1 else 1
    val positionMap = mutableMapOf<Point, List<Point>>()

    for ((position, piecePlayer) in piecePositions) {
        if (piecePlayer == player) {
            val x = position.x
            val y = position.y

            val potentialMoves = arrayOf(
                Point(x + (direction * 1), y),  // Forward
                Point(x, y - 1),        // Left
                Point(x, y + 1)         // Right
            )
            val validPositions = potentialMoves.filter {
                checkValidMove(position, it, piecePositions, player)
            }
            if (validPositions.isNotEmpty()) {
                positionMap[position] = validPositions
            }
        }
    }
    return if (positionMap.isNotEmpty()) Pair(positionMap, "Regular") else throw NullPointerException("No valid moves")
}