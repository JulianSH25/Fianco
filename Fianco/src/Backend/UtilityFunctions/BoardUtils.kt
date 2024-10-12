package Backend.UtilityFunctions

import java.awt.Point
import Backend.PlayerToMove

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

fun copyBoard(board: Array<Array<Int>>): Array<Array<Int>> {
    return board.map { it.copyOf() }.toTypedArray()
}