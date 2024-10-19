package Backend

import java.awt.Point
import java.io.IOException
import Backend.UtilityFunctions.isValidMove

/**
 * RandomEngine selects moves randomly from the available moves.
 *
 * It ensures that the selected move is valid according to the game rules by
 * performing an `isValidMove` check before returning the move.
 */
class RandomEngine {

    /**
     * Picks a random valid move from the provided piece map.
     *
     * @param pieceMap A map of pieces to their possible moves.
     * @param pm The PieceManager instance to access the board state.
     * @param currentPlayer The current player making the move.
     * @return A Pair containing the starting position and the destination position.
     * @throws IOException If no valid moves are found.
     */
    fun pickRandomMove(
        pieceMap: Map<Point, List<Point>>,
        pm: PieceManager,
        currentPlayer: PlayerToMove
    ): Pair<Point, Point> {
        if (pieceMap.isEmpty()) throw IOException("Empty piece map")

        // Flatten the pieceMap into a list of all possible moves
        val allMoves = pieceMap.flatMap { (fromPosition, toPositions) ->
            toPositions.map { toPosition -> Pair(fromPosition, toPosition) }
        }

        if (allMoves.isEmpty()) throw IOException("No moves available in piece map")

        // Shuffle the list to ensure randomness
        val shuffledMoves = allMoves.shuffled()

        // Get the current piece positions
        val piecePositions = pm.piecePositions

        // Iterate through the shuffled moves and return the first valid one
        for ((fromPosition, toPosition) in shuffledMoves) {
            val (isValid, _) = isValidMove(pm, fromPosition, toPosition, piecePositions, currentPlayer)
            if (isValid) {
                return Pair(fromPosition, toPosition)
            }
        }

        // If no valid moves are found after checking, throw an exception
        throw IOException("No valid moves available after checking")
    }
}