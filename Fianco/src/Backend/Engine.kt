package Backend

import Backend.UtilityFunctions.*

import Frontend.PieceManager.piecePositions
import java.awt.Color
import java.awt.Point
import java.lang.IndexOutOfBoundsException
//import java.util.List;
import kotlin.collections.List
import kotlin.collections.mutableMapOf
import kotlin.compareTo

class Engine {
    var nodesExplored = 0
    fun alphaBeta(
        board: Array<Array<Int>>,
        depth: Int,
        alpha: Int,
        beta: Int,
        maximizingPlayer: Boolean
    ): Int {
        nodesExplored++  // Increment the node counter
        var alpha = alpha
        var beta = beta

        if (checkTerminal(board) || depth == 0) {
            return evaluate(board)
        }

        if (maximizingPlayer) {
            var maxEval = Int.MIN_VALUE
            for (child in successors(board, 2)) { // AI is player 2 (black)
                val eval = alphaBeta(child, depth - 1, alpha, beta, false)
                maxEval = maxOf(maxEval, eval)
                alpha = maxOf(alpha, eval)
                if (beta <= alpha) {
                    break
                }
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (child in successors(board, 1)) { // Human is player 1 (white)
                val eval = alphaBeta(child, depth - 1, alpha, beta, true)
                minEval = minOf(minEval, eval)
                beta = minOf(beta, eval)
                if (beta <= alpha) {
                    break
                }
            }
            return minEval
        }
    }

    private fun checkTerminal(board: Array<Array<Int>>): Boolean {
        return checkVictory(board) != null
    }

    private fun evaluate(board: Array<Array<Int>>): Int {
        var aiPieces = 0
        var humanPieces = 0

        for (row in board) {
            for (cell in row) {
                when (cell) {
                    1 -> humanPieces++  // Assuming 1 represents human (white)
                    2 -> aiPieces++     // Assuming 2 represents AI (black)
                }
            }
        }

        // Simple evaluation: difference in the number of pieces
        return aiPieces - humanPieces
    }

    private fun successors(board: Array<Array<Int>>, playerID: Int): List<Array<Array<Int>>> {
        val positions = createPiecePositionsFromBoard(board)
        val moves = generateMoves(playerID, board, positions)
        val successorsList = mutableListOf<Array<Array<Int>>>()

        for ((fromPosition, toPositions) in moves.first) {
            for (toPosition in toPositions) {
                val newBoard = copyBoard(board)
                makeMove(newBoard, fromPosition, toPosition, moves.second == "Capture")
                successorsList.add(newBoard)
            }
        }
        return successorsList
    }

    private fun addMoveToArray(pieceArray: Array<Array<Int>>, newMove: Pair<Point, Point>): Array<Array<Int>>{
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

fun copyBoard(board: Array<Array<Int>>): Array<Array<Int>> {
    return board.map { it.copyOf() }.toTypedArray()
}

fun makeMove(
    board: Array<Array<Int>>,
    from: Point,
    to: Point,
    isCapture: Boolean
) {
    val piece = board[from.x][from.y]
    board[to.x][to.y] = piece
    board[from.x][from.y] = 0

    if (isCapture) {
        val captureX = (from.x + to.x) / 2
        val captureY = (from.y + to.y) / 2
        board[captureX][captureY] = 0
    }
}