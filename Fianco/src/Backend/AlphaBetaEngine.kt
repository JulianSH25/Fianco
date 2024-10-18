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

    val adaptiveDepthIncrease: Byte = 5 // implementing nominal depth; // TODO Would it be a good idea to switch randomly between e.g. depth 9 and 10 (to weigh pessimistic - optimistic view)?

    val killerMoves = HashMap<Pair<Point, Point>, Int>()

    fun alphaBetaWithTime(
    board: Array<Array<Int>>,
    depth: Byte,
    alpha: Int,
    beta: Int,
    player: PlayerToMove,  // 1 for AI, -1 for human
    timeObject: TimeKeeper,
    zobrHash: ULong = 0u,
    adaptiveFlag: Boolean? = false // set to true when continuing search under the adaptive scheme, to not continue search until a definite win or loss is found.
): Pair<Int, Pair<Point, Point>?> {
        //println("executing alpha-beta")
        val playerMultiplier = if (player == PlayerToMove.PlayerOne) -1 else 1
        val currentTime = System.currentTimeMillis()

        var alpha = alpha
        val oldAlpha = alpha
        var beta = beta
        var adaptiveFlag = adaptiveFlag
        var depth = depth


        //println("initiating tt lookup")
        val n = tt.getEntry(zobrHash)
        //("finished tt lookup")
        if (n != null && n.searchDepth >= depth && n.age < alphabetacycles-n.searchDepth) {
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
            val evaluationResult = evaluate(board, true)
            //println("Evaluation result received: $evaluationResult")
            var evaluationScore = playerMultiplier * evaluationResult.first
            //println("Evaluation score calculated: $evaluationScore, Adaptive Flag: $adaptiveFlag")

            val contWithAdaptiveDepth = adaptiveSchemeEvaluation(evaluationScore, evaluationResult.second!!)

            //print("Continue with adaptive depth? $contWithAdaptiveDepth")

            // Adaptive Search Depth check:
            if (adaptiveFlag == true || contWithAdaptiveDepth == false){
                //println("exciting node, returning eval value")
                return Pair(evaluationScore, null)  // do not continue with increase search depth
            }
            else{
                adaptiveFlag = true  // set the flag to true as to continue the search below. Depth to nominal increased search depth (added)
                val newDepth = (max(1, (ln(depth.toDouble()) * depth).toInt())).toByte()

                // Statistics:
                adaptiveIterations++
                maxAdaptiveDepth = max(maxAdaptiveDepth, depth + newDepth)
                //maxAchievedDepth = max(maxAchievedDepth, depth)

                depth = newDepth

                //println("Continuing search with new depth: $depth")
            }
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
                newHash,
                adaptiveFlag
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
            age = alphabetacycles
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

    private fun evaluate(board: Array<Array<Int>>, adaptiveThreshold: Boolean? = false): Pair<Int, IntArray?> {
        //println("Evaluating")
        var score = 0

        var pieceDifference = 0
        var distanceToOtherSideAdvantage = 0

        val pieceDifferenceWeight = 0.5
        val distanceWeight = 0.5
        var noPiecesOpponent = (Short.MAX_VALUE).toInt()
        var noPiecesOwn = (Short.MIN_VALUE).toInt()

        for (x in board.indices) {
            for (y in board[x].indices) {
                when (board[x][y]) {
                    1 -> { // Human piece (white)
                        noPiecesOpponent = 0
                        //Piece difference
                        pieceDifference -= 1  // Each human piece is bad for AI

                        /**Advancement (tried out different non-linear weighting methods to emphasize forward movement of individual pieces over 'whole army' movements and make the game faster & more exciting):**/
                        //distanceToOtherSideAdvantage -= (8 - x) * 10  // Closer to AI's side is worse
                        distanceToOtherSideAdvantage -= exp((9 - x).toDouble()).toInt() // using 9 instead of 8 experimentally, to punish enemy movements over own movements
                        //distanceToOtherSideAdvantage -= (log((8 - x + 1).toDouble(),base = 2.14) * 10).toInt()
                    }
                    2 -> { // AI piece (black)
                        noPiecesOwn = 0
                        //Piece difference
                        pieceDifference += 1  // Each AI piece is good for AI

                        //Advancement:
                        //distanceToOtherSideAdvantage += x * 10  // The further down the board, the better
                        distanceToOtherSideAdvantage += exp(x.toDouble()).toInt()
                        //distanceToOtherSideAdvantage += (log(x.toDouble() + 1,base = 2.14) * 10).toInt()
                    }
                }
            }
        }

        //println("Evaluation finished")

        score = 50 * pieceDifference + 10 * distanceToOtherSideAdvantage + noPiecesOwn + noPiecesOpponent

        //println("Score: $score, individual weights: $pieceDifference, $distanceToOtherSideAdvantage")

        /** If evaluation used to decide on continuing the search (Adaptive Scheme Framework - Adaptive Search Depth**/
        if (adaptiveThreshold == true){
            val individualValues = intArrayOf(pieceDifference, distanceToOtherSideAdvantage)
            //println("returning evaluation, no error here")
            return Pair(score, individualValues)
        }
        /**Else (regular evaluation & return of value) **/
        return Pair(score, null)  // currently not in use, but who knows when it might become useful (saving memory by not creating the list in the first place)
    }

    /** Method to decide whether to continue searching in an ADAPTIVE SEARCH DEPTH style, using the (most sophisticated) ADAPTIVE SCHEME from the lecture**/
    fun adaptiveSchemeEvaluation(score: Int, individualValues: IntArray): Boolean {
        //println("Starting Adaptive evaluation")
        // Safely cast the returned Serializable to Pair<Int, IntArray>
        val pieceD = individualValues[0]
        val forwardM = individualValues[1]

        //println("Evaluating $pieceD, $forwardM")

        // Define your threshold based on domain knowledge
        // Example thresholds (adjust these based on your specific needs)
        val pieceDifferenceThreshold = 2   // e.g., significant material advantage
        val distanceThreshold = 20         // e.g., significant positional advantage

        // Decide whether to continue searching deeper
        val shouldDeepen = (score > 0 && (pieceD >= pieceDifferenceThreshold) || (forwardM >= distanceThreshold))

        //print("Adaptive Scheme: $shouldDeepen")

        return shouldDeepen
    }

    fun printStatistics(){
        println("Nodes explored: $nodesExplored")
        println("Successfull TT-lookups: $successfullTTlookups of which primary: $primaries and secondary: $secondaries")
        println("Collisions: ${Backend.collisions}")
        println("newly Added Entries = ${Backend.newlyAddedEntries - Backend.collisions}")
        println("Adaptive Search depth - #TimesCalled: $adaptiveIterations, Maximum Search Depth achieved: $maxAdaptiveDepth")

        Backend.collisions = 0
        Backend.newlyAddedEntries = 0
        adaptiveIterations = 0
        maxAdaptiveDepth = 0
        Backend.primaries = 0
        secondaries = 0
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