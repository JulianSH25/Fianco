package Backend

import java.awt.Point

/**fun printPositionMap(positionMap: Map<Point, List<Point>>) {
    for ((position, moves) in positionMap) {
        println("Piece at position $position can move to the following positions:")
        for (move in moves) {
            println("  - $move")
        }
    }
}**/

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