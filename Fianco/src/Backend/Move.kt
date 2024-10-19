package Backend

import java.awt.Point

/**
 * Makes a move on the board, updating the board state.
 *
 * @param board The game board represented as a 2D array.
 * @param from The starting position of the piece.
 * @param to The destination position of the piece.
 * @param isCapture Indicates whether the move is a capture move.
 */
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