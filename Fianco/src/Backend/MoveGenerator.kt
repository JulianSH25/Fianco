package Backend

import Backend.UtilityFunctions.checkCapture
import Backend.UtilityFunctions.checkValidMove
import Backend.UtilityFunctions.createPiecePositionsFromBoard
import java.awt.Point

fun generateMoves(pm: PieceManager,
    playerID: Int,
    pieceArray: Array<Array<Int>>,
    positions: Map<Point, PlayerToMove>? = null
): Pair<Map<Point, List<Point>>, String> {

    val piecePositions = positions ?: createPiecePositionsFromBoard(pieceArray)
    val player = if (playerID == 1) PlayerToMove.PlayerOne else PlayerToMove.PlayerTwo

    val validMoves = checkCapture(pm, piecePositions, player, true, pieceArray)
    if (validMoves != null) {
        return Pair(validMoves, "Capture")
    }

    val i = if (playerID == 1) -1 else 1
    val positionMap = mutableMapOf<Point, List<Point>>()

    for ((position, piecePlayer) in piecePositions) {
        if (piecePlayer == player) {
            val x = position.x
            val y = position.y

            val potentialMoves = arrayOf(
                Point(x + (i * 1), y),  // Forward
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