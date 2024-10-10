package Backend.UtilityFunctions

import java.awt.Color
import java.awt.Point

fun createPiecePositionsFromBoard(pieceArray: Array<Array<Int>>): MutableMap<Point, Color> {
    val positions = mutableMapOf<Point, Color>()
    for (i in pieceArray.indices) {
        for (j in pieceArray[i].indices) {
            val value = pieceArray[i][j]
            if (value != 0) {
                positions[Point(i, j)] = if (value == 1) Color.WHITE else Color.BLACK
            }
        }
    }
    return positions
}

fun copyBoard(board: Array<Array<Int>>): Array<Array<Int>> {
    return board.map { it.copyOf() }.toTypedArray()
}