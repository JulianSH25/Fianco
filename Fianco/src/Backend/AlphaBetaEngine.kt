package Backend

import Backend.UtilityFunctions.*
import java.awt.Point

import kotlin.collections.List
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class AlphaBetaEngine(pieceManager: PieceManager) {

    val pm = pieceManager

    val tt = TranspositionTable()
    val zb = Zobrist()

    var nodesExplored = 0
    var successfullTTlookups = 0
    var collisions = 0
    var newlyAddedEntries = 0
    var timeUp = false

    val killerMoves = Array(20) { mutableListOf<Pair<Point, Point>>() }

    fun alphaBetaWithTime(
    board: Array<Array<Int>>,
    depth: Byte,
    alpha: Int,
    beta: Int,
    player: PlayerToMove,  // 1 for AI, -1 for human
    timeObject: TimeKeeper,
    zobrHash: ULong = 0u
): Pair<Int, Pair<Point, Point>?> {
        //println("executing alpha-beta")
        val playerMultiplier = if (player == PlayerToMove.PlayerOne) -1 else 1
        val currentTime = System.currentTimeMillis()

        var alpha = alpha
        val oldAlpha = alpha
        var beta = beta


        //println("initiating tt lookup")
        val n = tt.getEntry(zobrHash)
        //("finished tt lookup")
        if (n != null && n.searchDepth >= depth) {
            //println("successfull tt lookup")
            successfullTTlookups++
            when (n.scoreType) {
                ScoreType.EXACT -> return Pair(n.score, n.bestMove)
                ScoreType.LOWER_BOUND -> alpha = max(alpha, n.score)
                ScoreType.UPPER_BOUND -> beta = min(beta, n.score)  // Corrected here
            }

            if (alpha >= beta) {
                return Pair(n.score, n.bestMove)
            }
        }

        if (timeObject.timeUp) {
            timeUp = true
            return Pair(0, null)  // Return a neutral value since time is up
        }

        if (checkTerminal(board)) {
            val winner = checkVictory(pm, board)
            return when {
                winner == null -> Pair(0, null)  // Draw
                (winner == PlayerToMove.PlayerTwo && playerMultiplier == 1) ||
                (winner == PlayerToMove.PlayerOne && playerMultiplier == -1) -> Pair(Int.MAX_VALUE - depth, null)
                else -> Pair(Int.MIN_VALUE + depth, null)
            }
        } else if (depth == 0.toByte()) {
            return Pair(playerMultiplier * evaluate(board), null)
        }

        nodesExplored++
        var bestValue = Int.MIN_VALUE
        var bestFoundMove: Pair<Point, Point>? = null

        val currentPlayerID = if (playerMultiplier == 1) 2 else 1

        // Generate all possible moves
        val allMoves = successors(board, currentPlayerID).map { it.second }

        // Sort moves based on History Heuristic
        val sortedMoves = allMoves.sortedByDescending { HistoryHeuristic.getScore(it) }

        for (move in sortedMoves) {
            val (from, to) = move
            val newBoard = copyBoard(board)
            makeMove(newBoard, from, to, /* isCapture = */ true)  // Adjust if necessary
            val newHash = zb.updateHash(
                zobrHash,
                move,
                if (player == PlayerToMove.PlayerOne) 2 else 1
            )
            val newEval = alphaBetaWithTime(
                newBoard,
                (depth - 1).toByte(),
                -beta,
                -alpha,
                Player.getOtherPlayer(player),
                timeObject,
                newHash
            )
            val eval = -newEval.first

            if (eval > bestValue) {
                //println("Found better value $eval")
                bestValue = eval
                bestFoundMove = move
            }
            alpha = max(alpha, bestValue)

            if (alpha >= beta || timeUp) {
                if (alpha >= beta && bestFoundMove != null) {
                    HistoryHeuristic.increment(bestFoundMove)
                }
                break
            }
        }

        var flag: ScoreType = ScoreType.EXACT
        if (bestValue <= oldAlpha){
            flag = ScoreType.UPPER_BOUND
        }
        else if (bestValue >= beta){
            flag = ScoreType.LOWER_BOUND
        }

        // Create a new TableEntry instance instead of reusing
        val entry = TableEntry().apply {
            hashValue = zobrHash
            score = bestValue
            scoreType = flag
            searchDepth = depth
            bestMove = bestFoundMove
        }
        tt.storeEntry(entry)

        return Pair(bestValue, bestFoundMove)
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
                        //Piece difference
                        score -= 100  // Each human piece is bad for AI

                        //Advancement:
                        //score -= (8 - x) * 10  // Closer to AI's side is worse
                        score -= exp((8 - x).toDouble()).toInt()
                        //score -= (log((8 - x + 1).toDouble()) * 10).toInt()
                    }
                    2 -> { // AI piece (black)
                        //Piece difference
                        score += 100  // Each AI piece is good for AI

                        //Advancement:
                        //score += x * 10  // The further down the board, the better
                        score += exp(x.toDouble()).toInt()
                        //score += (log(x.toDouble() + 1) * 10).toInt()
                    }
                }
            }
        }
        return score
    }

    fun printStatistics(){
        println("Nodes explored: $nodesExplored")
        println("Successfull TT-lookups: $successfullTTlookups")
        println("Collisions: ${Backend.collisions}")
        println("newly Added Entries = ${Backend.newlyAddedEntries - Backend.collisions}")

        Backend.collisions = 0
        Backend.newlyAddedEntries = 0
    }

    private fun successors(board: Array<Array<Int>>, playerID: Int): List<Pair<Array<Array<Int>>, Pair<Point, Point>>> {
        val positions = createPiecePositionsFromBoard(board)
        val moves = generateMoves(pm, playerID, board, positions)
        val successorsList = mutableListOf<Pair<Array<Array<Int>>, Pair<Point, Point>>>()
        val moveList = mutableListOf<Pair<Point, Point>>()

        for ((fromPosition, toPositions) in moves.first) {
            for (toPosition in toPositions) {
                val newBoard = copyBoard(board)
                makeMove(newBoard, fromPosition, toPosition, moves.second == "Capture")
                successorsList.add(Pair(newBoard, Pair(fromPosition, toPosition)))
            }
        }
        return successorsList
    }
}