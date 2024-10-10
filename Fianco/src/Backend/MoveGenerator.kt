package Backend

import Backend.UtilityFunctions.checkCapture
import Backend.UtilityFunctions.checkValidMove
import Backend.UtilityFunctions.createPiecePositionsFromBoard
import java.awt.Color
import java.awt.Point

fun generateMoves(pm: PieceManager,
    playerID: Int,
    pieceArray: Array<Array<Int>>,
    positions: Map<Point, Color>? = null
): Pair<Map<Point, List<Point>>, String> {

    val piecePositions = positions ?: createPiecePositionsFromBoard(pieceArray)
    val colour = if (playerID == 1) Color.WHITE else Color.BLACK

    val validMoves = checkCapture(pm, piecePositions, colour, true, pieceArray)
    if (validMoves != null) {
        //println("Move: CAPTURE")
        //printPositionMap(validMoves)
        return Pair(validMoves, "Capture")
    }

    val i = if (playerID == 1) -1 else 1
    val positionMap = mutableMapOf<Point, List<Point>>()

    for ((position, pieceColor) in piecePositions) {
        if (pieceColor == colour) {
            val x = position.x
            val y = position.y

            val potentialMoves = arrayOf(
                Point(x + (i * 1), y),  // Forward
                Point(x, y - 1),        // Left
                Point(x, y + 1)         // Right
            )
            val validPositions = potentialMoves.filter {
                checkValidMove(position, it, piecePositions, colour)
            }
            if (validPositions.isNotEmpty()) {
                positionMap[position] = validPositions
            }
        }
    }
    //println("Move: REGULAR")
    //printPositionMap(positionMap)
    return if (positionMap.isNotEmpty()) Pair(positionMap, "Regular") else throw NullPointerException("No valid moves")
}