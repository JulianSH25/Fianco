package Backend

import Backend.UtilityFunctions.*

import java.awt.Color
//import java.util.List;
import kotlin.collections.List

class AlphaBetaEngine(pieceManager: PieceManager) {

    val pm = pieceManager

    var nodesExplored = 0
    var timeUp = false

    fun alphaBetaWithTime(
        board: Array<Array<Int>>,
        depth: Int,
        alpha: Int,
        beta: Int,
        player: Int,  // 1 for AI, -1 for human
        startTime: Long,
        timeLimit: Long
    ): Int {

        val currentTime = System.currentTimeMillis()

        if (currentTime - startTime >= timeLimit) {
            timeUp = true
            return 0  // Return a neutral value since time is up
        }

        if (checkTerminal(board)) {
            val winner = checkVictory(pm, board)
            return when {
                winner == null -> 0  // Draw
                (winner == Color.BLACK && player == 1) || (winner == Color.WHITE && player == -1) -> Int.MAX_VALUE - depth
                else -> Int.MIN_VALUE + depth
            }
        } else if (depth == 0) {
            return player * evaluate(board)
        }

        nodesExplored++
        var alpha = alpha
        var beta = beta
        var value = Int.MIN_VALUE

        val currentPlayerID = if (player == 1) 2 else 1

        for (child in successors(board, currentPlayerID)) {
            val eval = -alphaBetaWithTime(child, depth - 1, -beta, -alpha, -player, startTime, timeLimit)
            value = maxOf(value, eval)
            alpha = maxOf(alpha, value)
            if (alpha >= beta) {
                break
            }
            if (timeUp) {
                break
            }
        }
        return value
    }

    /**fun alphaBeta(
    board: Array<Array<Int>>,
    depth: Int,
    alpha: Int,
    beta: Int,
    player: Int  // 1 for AI (black), -1 for human (white)
): Int {
    if (checkTerminal(board)) {
        val winner = checkVictory(board)
        return when {
            winner == null -> 0  // Draw
            (winner == Color.BLACK && player == 1) || (winner == Color.WHITE && player == -1) -> Int.MAX_VALUE - depth  // Current player wins
            else -> Int.MIN_VALUE + depth  // Current player loses
        }
    } else if (depth == 0) {
        return player * evaluate(board)
    }

    nodesExplored++
    var alpha = alpha
    var beta = beta
    var value = Int.MIN_VALUE

    val currentPlayerID = if (player == 1) 2 else 1  // AI is player ID 2 (black), human is player ID 1 (white)

    for (child in successors(board, currentPlayerID)) {
        val eval = -alphaBeta(child, depth - 1, -beta, -alpha, -player)
        value = maxOf(value, eval)
        alpha = maxOf(alpha, value)
        if (alpha >= beta) {
            break  // Alpha-beta pruning
        }
    }
    return value
}**/

    private fun checkTerminal(board: Array<Array<Int>>): Boolean {
        return checkVictory(pm, board) != null
    }

    private fun evaluate(board: Array<Array<Int>>): Int {
        var score = 0

        for (x in board.indices) {
            for (y in board[x].indices) {
                when (board[x][y]) {
                    1 -> { // Human piece (white)
                        score -= 100  // Each human piece is bad for AI
                        score -= (8 - x) * 10  // Closer to AI's side is worse
                    }
                    2 -> { // AI piece (black)
                        score += 100  // Each AI piece is good for AI
                        score += x * 10  // The further down the board, the better
                    }
                }
            }
        }
        return score
    }

    private fun successors(board: Array<Array<Int>>, playerID: Int): List<Array<Array<Int>>> {
        val positions = createPiecePositionsFromBoard(board)
        val moves = generateMoves(pm, playerID, board, positions)
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
}