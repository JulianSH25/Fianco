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

fun generateMoves(playerID: Int, pieceArray: Array<Array<Int>>, positions: MutableMap<Point, Color>? = null): Pair<Map<Point, List<Point>>, String> {

    val piecePositions = if (positions != null) positions else piecePositions
    //val pieceArrayCopy = getBoardCopy()
    val colour = if (playerID == 1) Color.WHITE else Color.BLACK
    // map the received player ID (CURRENT PLAYER TO MOVE) to the respective colour (white/black)

    val validMoves = checkCapture(piecePositions, colour, true) // call checkCapture to understand whether the next move will be a capture move or a regular one

    if (validMoves != null){ //check if pieces can be captured, if so the resulting moves are the only valid ones
        println("Move: CAPTURE")
        printPositionMap(validMoves)
        return Pair(validMoves, "Capture")
    }

    val i = if (playerID == 1) -1 else if (playerID == 2) 1 else throw IndexOutOfBoundsException()
    // set up a multiplicator which is used to decide on the right direction to move, in order to generate the correct moves


    if (playerID == 1 || playerID == 2){
        val positionMap = mutableMapOf<Point, List<Point>>()
        for (piece in piecePositions){
            if (piece.value == colour){
                val x = piece.key.x
                val y = piece.key.y

                val positionList = arrayOf(Point(x, y - 1), Point(x + (i * 1), y), Point(x, y + 1))
                var returnPositionList = mutableListOf<Point>()

                for (position in positionList){
                    if (checkValidMove(Point(x, y), position, piecePositions, colour)){
                        returnPositionList.add(position)
                    }
                    else{
                        //throw Exception("Invalid move position. CHECK IMPLEMENTATION" + piece.key + " " + position)
                        //returnPositionList.add(position)
                    }
                }
                if (returnPositionList.isNotEmpty()) {
                    positionMap[Point(x, y)] = returnPositionList
                    returnPositionList = mutableListOf()
                }
            }
            //var positionList = mutableListOf<Point>()
        }
        println("Move: REGULAR")
        printPositionMap(positionMap)
        return if (positionMap.isNotEmpty()) Pair(positionMap, "Regular") else throw NullPointerException("Empty position array; Engine.kt")
    }
    throw IndexOutOfBoundsException("Index out of bounds")
}

fun printPositionMap(positionMap: Map<Point, List<Point>>) {
    for ((position, moves) in positionMap) {
        println("Piece at position $position can move to the following positions:")
        for (move in moves) {
            println("  - $move")
        }
    }
}