package Backend

import Backend.UtilityFunctions.*
import java.awt.Point
import java.io.Serializable

import kotlin.collections.List
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min

class AlphaBetaEngine(pieceManager: PieceManager) {

    val pm = pieceManager

    val tt = TranspositionTable()
    val zb = Zobrist()

    // Some debugging/statistics values:
    var maxAdaptiveDepth = 0
    var maxAchievedDepth: Byte = 0
    var adaptiveIterations = 0
    var nodesExplored = 0
    var successfullTTlookups = 0
    var collisions = 0
    var newlyAddedEntries = 0

    var alphabetacycles: Byte = 0


    var timeUp = false

    val adaptiveDepthIncrease: Byte = 5 // implementing nominal depth;

    val killerMoves = HashMap<Pair<Point, Point>, Int>()

    /**
     * Alpha-beta pruning algorithm with time constraints and adaptive search depth.
     * Incorporates enhancements like quiescence search, move ordering (history heuristic), adaptive search depth, and killer moves.
     *
     * @param board The current board state.
     * @param depth The remaining depth to search.
     * @param alpha The alpha value for pruning.
     * @param beta The beta value for pruning.
     * @param player The current player.
     * @param timeObject Object to track if time is up.
     * @param zobrHash The Zobrist hash of the current board state.
     * @param adaptiveFlag Flag to indicate if adaptive depth has been applied.
     * @param currentDepth The current depth achieved in the search tree.
     * @return A pair containing the best score and the best move found.
     */
    fun alphaBetaWithTime(
    board: Array<Array<Int>>,
    depth: Byte,
    alpha: Int,
    beta: Int,
    player: PlayerToMove,  // 1 for AI, -1 for human
    timeObject: TimeKeeper,
    zobrHash: ULong = 0u,
    adaptiveFlag: Boolean? = false, // set to true when continuing search under the adaptive scheme
    currentDepth: Byte? = 0  // Added parameter to track the current depth
): Pair<Int, Pair<Point, Point>?> {
        // Multiplier to adjust evaluation based on player
        val playerMultiplier = if (player == PlayerToMove.PlayerOne) -1 else 1

        var alpha = alpha
        val oldAlpha = alpha
        var beta = beta
        var adaptiveFlag = adaptiveFlag
        var depth = depth

        // Update max achieved depth
        maxAchievedDepth = max(maxAchievedDepth.toInt(), currentDepth?.toInt() ?: 0).toByte()

        // Transposition Table Lookup
        val n = tt.getEntry(zobrHash)
        if (n != null && n.searchDepth >= depth && n.age < alphabetacycles-n.searchDepth) {
            successfullTTlookups++
            when (n.scoreType) {
                ScoreType.EXACT -> return Pair(n.score, n.bestMove)
                ScoreType.LOWER_BOUND -> alpha = max(alpha, n.score)
                ScoreType.UPPER_BOUND -> beta = min(beta, n.score)
            }

            if (alpha >= beta) {
                return Pair(n.score, n.bestMove)
            }
        }

        // Check if time is up
        if (timeObject.timeUp) {
            timeUp = true
            // return Pair(0, null)  // Return neutral value
        }

        // Terminal node check
        if (checkTerminal(board)) {
            val winner = checkVictory(pm, board)
            return when {
                winner == null -> Pair(0, null)  // Draw
                (winner == PlayerToMove.PlayerTwo && playerMultiplier == 1) ||
                (winner == PlayerToMove.PlayerOne && playerMultiplier == -1) -> Pair(Int.MAX_VALUE, null)
                else -> Pair(Int.MIN_VALUE + depth, null)
            }
        } else if (depth == 0.toByte() || timeUp) {
            // Evaluate leaf node
            val evaluationResult = evaluate(board, true)
            var evaluationScore = playerMultiplier * evaluationResult.first

            val contWithAdaptiveDepth = adaptiveSchemeEvaluation(evaluationScore, evaluationResult.second!!)

            // Adaptive Search Depth check
            if (adaptiveFlag == true || contWithAdaptiveDepth == false || timeUp){
                if (isQuiescentPosition(pm, board, player) || timeUp){
                    return Pair(evaluationScore, null)  // Do not continue with increased depth
                } else {
                    depth++
                    adaptiveFlag = true
                }
            }
            else{
                adaptiveFlag = true  // Continue search with increased depth
                val newDepth = (max(1, (ln(depth.toDouble()) * depth).toInt())).toByte()

                // Statistics
                adaptiveIterations++
                maxAdaptiveDepth = max(maxAdaptiveDepth.toInt(), currentDepth?.toInt()?.plus(newDepth) ?: 0)

                depth = newDepth
            }
        }

        nodesExplored++
        var bestValue = Int.MIN_VALUE
        var bestFoundMove: Pair<Point, Point>? = null

        val currentPlayerID = if (playerMultiplier == 1) 2 else 1

        // Generate all possible moves
        val allMoves = successors(board, currentPlayerID).map { it.second }

        // Sort moves based on History Heuristic and Killer Moves
        val sortedMoves = allMoves.sortedByDescending {
            val moveScore = HistoryHeuristic.getScore(it)
            val killerScore = killerMoves.getOrDefault(it, 0)
            moveScore + killerScore * 1000  // Prioritize killer moves
        }

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
                newHash,
                adaptiveFlag,
                (currentDepth!! + 1).toByte()  // Increment current depth
            )
            val eval = -newEval.first

            if (eval > bestValue) {
                bestValue = eval
                bestFoundMove = move
            }
            alpha = max(alpha, bestValue)

            if (alpha >= beta || timeUp) {
                if (alpha >= beta && bestFoundMove != null) {
                    HistoryHeuristic.increment(bestFoundMove)
                    // Incorporate killer moves
                    killerMoves[bestFoundMove] = killerMoves.getOrDefault(bestFoundMove, 0) + 1
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

        // Store result in Transposition Table
        val entry = TableEntry().apply {
            hashValue = zobrHash
            score = bestValue
            scoreType = flag
            searchDepth = depth
            bestMove = bestFoundMove
            age = alphabetacycles
        }
        tt.storeEntry(entry)

        return Pair(bestValue, bestFoundMove)
}

    /**
     * Checks if the current board state is a terminal state (win, loss, or draw).
     *
     * @param board The game board.
     * @return True if terminal state, False otherwise.
     */
    private fun checkTerminal(board: Array<Array<Int>>): Boolean {
        return checkVictory(pm, board) != null
    }

    /**
     * Evaluates the board state and returns a score.
     * If adaptiveThreshold is true, returns individual component values for adaptive depth decision.
     *
     * @param board The game board.
     * @param adaptiveThreshold If true, returns individual evaluation components.
     * @return Pair of evaluation score and individual components (if requested).
     */
    private fun evaluate(board: Array<Array<Int>>, adaptiveThreshold: Boolean? = false): Pair<Int, IntArray?> {
        var score = 0

        var pieceDifference = 0
        var distanceToOtherSideAdvantage = 0

        var noPiecesOpponent = (Short.MAX_VALUE).toInt()
        var noPiecesOwn = (Short.MIN_VALUE).toInt()

        for (x in board.indices) {
            for (y in board[x].indices) {
                when (board[x][y]) {
                    1 -> { // Human piece (white)
                        noPiecesOpponent = 0
                        //Piece difference
                        pieceDifference -= 1  // Each human piece is bad for AI

                        // Advancement
                        distanceToOtherSideAdvantage -= exp((9 - x).toDouble()).toInt()
                    }
                    2 -> { // AI piece (black)
                        noPiecesOwn = 0
                        //Piece difference
                        pieceDifference += 1  // Each AI piece is good for AI

                        // Advancement
                        distanceToOtherSideAdvantage += exp(x.toDouble()).toInt()
                    }
                }
            }
        }

        score = 50 * pieceDifference + 10 * distanceToOtherSideAdvantage + noPiecesOwn + noPiecesOpponent

        if (adaptiveThreshold == true){
            val individualValues = intArrayOf(pieceDifference, distanceToOtherSideAdvantage)
            return Pair(score, individualValues)
        }
        return Pair(score, null)
    }

    /**
     * Decides whether to continue searching deeper based on the evaluation score.
     * Implements the adaptive scheme for adaptive search depth.
     *
     * @param score The evaluation score.
     * @param individualValues The individual evaluation components.
     * @return True if should deepen search, False otherwise.
     */
    fun adaptiveSchemeEvaluation(score: Int, individualValues: IntArray): Boolean {
        val pieceD = individualValues[0]
        val forwardM = individualValues[1]

        // Thresholds for significant advantage
        val pieceDifferenceThreshold = 2   // Significant material advantage
        val distanceThreshold = 20         // Significant positional advantage

        // Decide whether to continue searching deeper
        val shouldDeepen = (score > 0 && (pieceD >= pieceDifferenceThreshold) || (forwardM >= distanceThreshold))

        return shouldDeepen
    }

    /**
     * Prints statistics for debugging and analysis.
     */
    fun printStatistics(){
        println("Nodes explored: $nodesExplored")
        println("Successful TT-lookups: $successfullTTlookups")
        println("Newly Added Entries = ${Backend.newlyAddedEntries - Backend.collisions}")
        println("Adaptive Search depth - #TimesCalled: $adaptiveIterations, Maximum Search Depth achieved: $maxAdaptiveDepth")

        // Reset statistics
        Backend.collisions = 0
        Backend.newlyAddedEntries = 0
        adaptiveIterations = 0
        maxAdaptiveDepth = 0
        Backend.primaries = 0
        Backend.secondaries = 0
    }

    /**
     * Generates all possible successor states from the current board state.
     *
     * @param board The game board.
     * @param playerID The ID of the current player.
     * @return A list of pairs containing new board states and the moves leading to them.
     */
    private fun successors(board: Array<Array<Int>>, playerID: Int): List<Pair<Array<Array<Int>>, Pair<Point, Point>>> {
        val positions = createPiecePositionsFromBoard(board)
        val moves = generateMoves(pm, playerID, board, positions)
        val successorsList = mutableListOf<Pair<Array<Array<Int>>, Pair<Point, Point>>>()

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