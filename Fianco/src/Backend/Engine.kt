package Backend

import Backend.UtilityFunctions.*

import Frontend.PieceManager.piecePositions
import java.awt.Color
import java.awt.Point
import java.lang.IndexOutOfBoundsException
//import java.util.List;
import kotlin.collections.List
import kotlin.collections.mutableMapOf

class Engine {
    fun alphaBeta(successors: Pair<Long, Array<Long>>, depth: Int, alpha: Int, beta: Int): Int{
        var alpha = alpha
        var beta = beta
        if (checkTerminal(successors.first) || depth == 0){
            // successors.first is the current node (i.e. root of the subtree)
            return evaluate(successors)
        }
        var score = Int.MIN_VALUE

        for (succ in successors.second){
            // successors.second is an array of all the actual successor positions of the current node/root, obtained from @generateMoves
            val value = -alphaBeta(Pair(succ, successors(succ)), depth-1, -beta, -alpha)

            if (value > score){
                score = value
            }

            if (score > alpha){
                alpha = score
            }
            else if (score >= beta){
                beta = score
            }
        }
        return score
    }

    private fun checkTerminal(node: Long): Boolean{
        return checkVictory() != null
        TODO("Make less general")
    }

    fun evaluate(successors: Pair<Long, Array<Long>>): Int{

        return TODO("Provide the return value")
    }

    fun successors(rootNode: Long): Array<Long>{
        //succ_array = generateMoves()
        return TODO("Provide the return value")
    }

    fun addMoveToArray(pieceArray: Array<Array<Int>>, newMove: Pair<Point, Point>): Array<Array<Int>>{
        TODO("Make deep copy?")
        pieceArray[newMove.second.x][newMove.second.y] = pieceArray[newMove.first.x][newMove.first.y]
        pieceArray[newMove.first.x][newMove.first.y] = 0

        return pieceArray
    }


}

fun generateMoves(
    playerID: Int,
    pieceArray: Array<Array<Int>>,
    positions: Map<Point, Color>? = null
): Pair<Map<Point, List<Point>>, String> {

    val piecePositions = positions ?: createPiecePositionsFromBoard(pieceArray)
    val colour = if (playerID == 1) Color.WHITE else Color.BLACK

    val validMoves = checkCapture(piecePositions, colour, true, pieceArray)
    if (validMoves != null) {
        println("Move: CAPTURE")
        printPositionMap(validMoves)
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
    println("Move: REGULAR")
    printPositionMap(positionMap)
    return if (positionMap.isNotEmpty()) Pair(positionMap, "Regular") else throw NullPointerException("No valid moves")
}

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

fun printPositionMap(positionMap: Map<Point, List<Point>>) {
    for ((position, moves) in positionMap) {
        println("Piece at position $position can move to the following positions:")
        for (move in moves) {
            println("  - $move")
        }
    }
}