package Frontend

import Backend.Player
import Backend.PlayerTypes
import Backend.PlayerToMove
import Backend.AlphaBetaEngine
import Backend.HistoryHeuristic
import Backend.PieceManager
import Backend.RandomEngine
import Backend.generateMoves
import Backend.Utilities.TimeKeeper
import Backend.UtilityFunctions.*
import Backend.Zobrist
import Backend.makeMove
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.LineBorder
import javax.swing.text.BadLocationException
import javax.swing.text.StyleConstants

/**
 * Represents the game board and handles user interactions and game logic.
 *
 * @param gui Reference to the main GUI.
 */
class Board(private val gui: FiancoGUI) : JComponent() {

    // Piece Manager to manage game pieces
    private val pm = PieceManager()
    private val zb = Zobrist()

    private var returnedWinner = false

    private var selectedPiece: Point? = null

    // Images for the pawns
    private val redPawnImage: BufferedImage? = ImageIO.read(javaClass.getResource("figures/whitePawn.png"))
    private val blackPawnImage: BufferedImage? = ImageIO.read(javaClass.getResource("figures/blackPawn.png"))

    private var playerOne = PlayerTypes.HUMAN
    private var playerTwo = PlayerTypes.AI_ENGINE

    private var gameEnded = false

    init {
        // Mouse listener for human moves
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (gameEnded) return

                val currentPlayer = Player.getPlayerToMove()
                if ((playerOne == PlayerTypes.HUMAN && currentPlayer == PlayerToMove.PlayerOne) ||
                    (playerTwo == PlayerTypes.HUMAN && currentPlayer == PlayerToMove.PlayerTwo)
                ) {

                    val currentSquareSize = minOf(width / 9, height / 9)
                    val row = e.y / currentSquareSize
                    val column = e.x / currentSquareSize
                    val position = Point(row, column)

                    if (position in pm.piecePositions && pm.piecePositions[position] == currentPlayer) {
                        selectedPiece = position
                    }
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (selectedPiece != null) {
                    val currentSquareSize = minOf(width / 9, height / 9)
                    val newRow = e.y / currentSquareSize
                    val newColumn = e.x / currentSquareSize
                    val newPosition = Point(newRow, newColumn)

                    if (handleMove(selectedPiece!!, newPosition)) {
                        selectedPiece = null
                        checkAndStartAI()
                    } else {
                        selectedPiece = null
                    }
                }
            }
        })

        // Listener to repaint when the component is resized
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                repaint()
            }
        })
    }

    /**
     * Checks if there is a winner or if the game has ended due to stalemate or no pieces.
     *
     * @return True if the game has ended; False otherwise.
     */
    fun checkForWinner(): Boolean {
        val winner = checkVictory(pm)
        if (winner != null) {
            val message: String = if (winner == PlayerToMove.PlayerOne) "White Won!" else "Black Won!"
            if (!returnedWinner) {
                JOptionPane.showMessageDialog(this@Board, message, "Game Over", JOptionPane.INFORMATION_MESSAGE)
                returnedWinner = true
            }
            endGame()
            return true
        }

        // Check if the current player has no pieces left
        val currentPlayer = Player.getPlayerToMove()
        val currentPlayerPieces = pm.piecePositions.filterValues { it == currentPlayer }
        if (currentPlayerPieces.isEmpty()) {
            val message = "Player ${if (currentPlayer == PlayerToMove.PlayerOne) "White" else "Black"} has no pieces left! They lose."
            if (!returnedWinner) {
                JOptionPane.showMessageDialog(this@Board, message, "Game Over", JOptionPane.INFORMATION_MESSAGE)
                returnedWinner = true
            }
            return true
        }

        // Check if the current player cannot make a move (stalemate)
        val boardCopy = pm.getBoardCopy()
        val positionsCopy = createPiecePositionsFromBoard(boardCopy)
        val currentPlayerID = if (currentPlayer == PlayerToMove.PlayerOne) 1 else 2
        val (moves, _) = try {
            generateMoves(pm, currentPlayerID, boardCopy, positionsCopy)
        } catch (e: Exception) {
            Pair(emptyMap<Point, List<Point>>(), "")
        }
        if (moves.isEmpty()) {
            val message = "Player ${if (currentPlayer == PlayerToMove.PlayerOne) "White" else "Black"} cannot make a move! They lose."
            if (!returnedWinner) {
                JOptionPane.showMessageDialog(this@Board, message, "Game Over", JOptionPane.INFORMATION_MESSAGE)
                returnedWinner = true
            }
            return true
        }

        return false
    }

    /**
     * Sets the player type for Player One.
     *
     * @param type The player type (HUMAN, AI_ENGINE, RANDOM_ENGINE).
     */
    fun setPlayerOneType(type: PlayerTypes) {
        playerOne = type
    }

    /**
     * Sets the player type for Player Two.
     *
     * @param type The player type (HUMAN, AI_ENGINE, RANDOM_ENGINE).
     */
    fun setPlayerTwoType(type: PlayerTypes) {
        playerTwo = type
    }

    /**
     * Sets both players' types and updates the Player object.
     *
     * @param p1 Player One's type.
     * @param p2 Player Two's type.
     */
    fun setPlayers(p1: PlayerTypes, p2: PlayerTypes) {
        playerOne = p1
        playerTwo = p2
        Player.setPlayers(playerOne, playerTwo)
    }

    /**
     * Initializes the board with the starting positions.
     */
    fun initializeBoard() {
        gameEnded = false
        returnedWinner = false
        pm.reset()
        Player.setPlayerToMove(PlayerToMove.PlayerOne)

        // Initialize the board pieces
        // Place Player One's pieces (value 1)
        for (i in 0 until 9) {
            pm.setPiece(8, i, 1)
        }
        for (i in 1 until 4) {
            pm.setPiece(8 - i, i, 1)
            pm.setPiece(8 - i, 8 - i, 1)
        }

        // Place Player Two's pieces (value 2)
        for (i in 0 until 9) {
            pm.setPiece(0, i, 2)
        }
        for (i in 1 until 4) {
            pm.setPiece(i, i, 2)
            pm.setPiece(i, 8 - i, 2)
        }

        // Update the GUI
        repaint()
        gui.updateCurrentPlayerLabel(Player.getPlayerToMove().name)
        gui.getClock().reset()
    }

    /**
     * Checks if AI needs to make a move and starts the AI move if necessary.
     */
    fun checkAndStartAI() {
        if (gameEnded) return

        val currentPlayer = Player.getPlayerToMove()
        val currentPlayerType = if (currentPlayer == PlayerToMove.PlayerOne) playerOne else playerTwo

        if (currentPlayerType == PlayerTypes.AI_ENGINE) {
            aiMove()
        } else if (currentPlayerType == PlayerTypes.RANDOM_ENGINE) {
            aiRandomMove()
        }

        // Update the clock's current player
        gui.getClock().switchPlayer(currentPlayer)
        gui.updateCurrentPlayerLabel(currentPlayer.name)
    }

    /**
     * Ends the game and stops the clock.
     */
    fun endGame() {
        gameEnded = true
        gui.getClock().stop()
    }

    /**
     * Makes a move using the RandomEngine for AI.
     */
    fun aiRandomMove() {
        println("AI MOVE - picking random move!")
        val aiWorker = object : SwingWorker<Void, Void>() {
            override fun doInBackground(): Void? {
                val boardCopy = pm.getBoardCopy()
                val positionsCopy = createPiecePositionsFromBoard(boardCopy)
                val currentPlayerID = if (Player.getPlayerToMove() == PlayerToMove.PlayerOne) 1 else 2
                val (moves, typeOfMove) = generateMoves(pm, currentPlayerID, boardCopy, positionsCopy)
                if (moves.isNotEmpty()) {
                    val randomEngine = RandomEngine()
                    val (fromPosition, toPosition) = randomEngine.pickRandomMove(moves, pm, Player.getPlayerToMove())
                    SwingUtilities.invokeLater {
                        handleMoveAI(fromPosition, toPosition, if (typeOfMove == "Capture") moves else null)
                        checkForWinner()
                    }
                } else {
                    println("AI has no valid moves!")
                }
                return null
            }
        }
        aiWorker.execute()
    }

    /**
     * Makes a move using the AlphaBetaEngine for AI.
     */
    fun aiMove() {
        println("AlphaBeta MOVE generation invoked for player: ${Player.getPlayerToMove()} (${if (Player.getPlayerToMove() == PlayerToMove.PlayerOne) "White" else "Black"})!")
        val aiWorker = object : SwingWorker<Void, Void>() {
            var nodesExplored = 0

            override fun doInBackground(): Void? {
                val maxDepth: Byte = 30
                val timeLimit: Short = 10000
                val boardCopy = pm.getBoardCopy()
                val positionsCopy = createPiecePositionsFromBoard(boardCopy)
                val (bestMove, nodeCount) = getBestMove(boardCopy, positionsCopy, maxDepth, timeLimit)
                nodesExplored = nodeCount
                if (bestMove != null) {
                    SwingUtilities.invokeLater {
                        handleMoveAI(bestMove.first, bestMove.second, bestMove.third)
                        checkForWinner()
                    }
                } else {
                    println("AI has no valid moves!")
                }
                return null
            }
        }
        aiWorker.execute()
    }

    /**
     * Gets the best move for the AI using iterative deepening and alpha-beta pruning.
     *
     * @param board The current board state.
     * @param positions The positions of the pieces.
     * @param maxDepth The maximum depth to search.
     * @param timeLimit The time limit in milliseconds.
     * @return A Pair containing the best move and the number of nodes explored.
     */
    fun getBestMove(
        board: Array<Array<Int>>,
        positions: Map<Point, PlayerToMove>,
        maxDepth: Byte,
        timeLimit: Short = 20000
    ): Pair<Triple<Point, Point, Map<Point, List<Point>>?>?, Int> {

        val alphaBetaEngine = AlphaBetaEngine(pm)
        alphaBetaEngine.nodesExplored = 0
        alphaBetaEngine.successfullTTlookups = 0

        var bestMove: Triple<Point, Point, Map<Point, List<Point>>?>? = null
        var bestValue = Int.MIN_VALUE
        val tk = TimeKeeper(timeLimit)

        val currentPlayer = Player.getPlayerToMove()

        for (depth in 1..maxDepth) {
            if (tk.timeUp) {
                println("Time limit reached during depth $depth")
                break
            }

            val (moves, type_of_move) = generateMoves(
                pm,
                if (currentPlayer == PlayerToMove.PlayerOne) 1 else 2,
                board,
                positions
            )
            var currentBestValue = Int.MIN_VALUE
            var currentBestMove: Triple<Point, Point, Map<Point, List<Point>>?>? = null

            if (moves.size == 1 && moves.values.first().size == 1) {
                val fromPosition = moves.keys.first()
                val toPosition = moves.values.first().first()
                bestMove = Triple(fromPosition, toPosition, if (type_of_move == "Capture") moves else null)
                break
            }

            zb.currentBoardHash = zb.calculateInitialHash(board)

            var firstMove = true
            for ((fromPosition, toPositions) in moves) {
                for (toPosition in toPositions) {
                    val newBoard = copyBoard(board)
                    makeMove(newBoard, fromPosition, toPosition, type_of_move == "Capture")

                    val hash = zb.updateHash(
                        zb.currentBoardHash,
                        Pair(fromPosition, toPosition),
                        if (currentPlayer == PlayerToMove.PlayerOne) 1 else 2
                    )

                    val newEval: Pair<Int, Pair<Point, Point>?> =
                        if (firstMove) {
                            alphaBetaEngine.alphaBetaWithTime(
                                newBoard,
                                (depth - 1).toByte(),
                                Int.MIN_VALUE,
                                Int.MAX_VALUE,
                                Player.getOtherPlayer(currentPlayer),
                                tk,
                                hash
                            )
                        } else {
                            val score = alphaBetaEngine.alphaBetaWithTime(
                                newBoard,
                                (depth - 1).toByte(),
                                -currentBestValue,
                                -currentBestValue + 1,
                                Player.getOtherPlayer(currentPlayer),
                                tk,
                                hash
                            )
                            if (-score.first > currentBestValue) {
                                alphaBetaEngine.alphaBetaWithTime(
                                    newBoard,
                                    (depth - 1).toByte(),
                                    Int.MIN_VALUE,
                                    Int.MAX_VALUE,
                                    Player.getOtherPlayer(currentPlayer),
                                    tk,
                                    hash
                                )
                            } else {
                                score
                            }
                        }

                    val eval = -newEval.first
                    if (eval > currentBestValue) {
                        currentBestValue = eval
                        currentBestMove =
                            Triple(fromPosition, toPosition, if (type_of_move == "Capture") moves else null)
                    }

                    if (alphaBetaEngine.timeUp) {
                        break
                    }
                    firstMove = false
                }
                if (alphaBetaEngine.timeUp) {
                    break
                }
            }

            if (!alphaBetaEngine.timeUp) {
                bestValue = currentBestValue
                bestMove = currentBestMove
                println("Depth $depth completed. Best value: $bestValue")
                HistoryHeuristic.decay()
            } else {
                println("Time limit reached during depth $depth")
                break
            }
        }
        alphaBetaEngine.printStatistics()
        return Pair(bestMove, alphaBetaEngine.nodesExplored)
    }

    /**
     * Handles the AI's move on the board.
     *
     * @param oldPosition The starting position of the move.
     * @param newPosition The destination position of the move.
     * @param captureMap The map of capture moves if applicable.
     */
    fun handleMoveAI(oldPosition: Point, newPosition: Point, captureMap: Map<Point, List<Point>>? = null) {
        if (gameEnded) return

        if (captureMap != null) {
            pm.capturePiece(oldPosition, newPosition)
        } else {
            pm.movePiece(oldPosition, newPosition)
        }

        updateMoveHistory(oldPosition, newPosition, Player.getPlayerToMove())

        // Switch player
        Player.switchPlayerToMove()
        repaint()

        gui.updateCurrentPlayerLabel(Player.getPlayerToMove().name)
        gui.getClock().switchPlayer(Player.getPlayerToMove())

        if (!checkForWinner()) {
            checkAndStartAI()
        }
    }

    /**
     * Handles a human player's move.
     *
     * @param oldPosition The starting position of the move.
     * @param newPosition The destination position of the move.
     * @return True if the move was successful; False otherwise.
     */
    fun handleMove(oldPosition: Point, newPosition: Point): Boolean {
        val (validMove, captureMap) = isValidMove(
            pm,
            oldPosition,
            newPosition,
            pm.piecePositions,
            Player.getPlayerToMove()
        )

        if (validMove) {
            if (captureMap != null) {
                pm.capturePiece(oldPosition, newPosition)
            } else {
                pm.movePiece(oldPosition, newPosition)
            }

            println("Old position: $oldPosition, New position: $newPosition")

            captureMap?.get(oldPosition)?.let { capturedPieces ->
                val dx = newPosition.x - oldPosition.x
                val dy = newPosition.y - oldPosition.y
                val capturedX = oldPosition.x + dx / 2
                val capturedY = oldPosition.y + dy / 2

                capturedPieces.find { it.x == capturedX && it.y == capturedY }?.let { capturedPiece ->
                    pm.piecePositions.remove(capturedPiece)
                    pm.setPiece(capturedPiece.x, capturedPiece.y, 0)
                    repaint()
                }
                println("Moved manually from $oldPosition to ${newPosition.x + dx}, ${newPosition.y} and removed piece at $capturedX, $capturedY")
            }

            updateMoveHistory(oldPosition, newPosition, Player.getPlayerToMove())

            // Switch player
            Player.switchPlayerToMove()
            repaint()

            gui.updateCurrentPlayerLabel(Player.getPlayerToMove().name)
            gui.getClock().switchPlayer(Player.getPlayerToMove())

            return !checkForWinner()
        } else {
            println("Illegal move!")
            return false
        }
    }

    /**
     * Converts a Point to chess notation.
     *
     * @return The position in chess notation (e.g., "A1").
     */
    fun Point.toChessNotation(): String {
        val columnChar = ('A' + this.y)
        val rowNumber = 9 - this.x
        return "$columnChar$rowNumber"
    }

    /**
     * Updates the move history displayed in the GUI.
     *
     * @param from The starting position of the move.
     * @param to The destination position of the move.
     * @param player The player who made the move.
     */
    fun updateMoveHistory(from: Point, to: Point, player: PlayerToMove) {
        val moveString = "${player.name}: ${from.toChessNotation()} -> ${to.toChessNotation()}\n"
        val doc = gui.getMoveHistoryArea().styledDocument
        val style = gui.getMoveHistoryArea().addStyle(null, null)
        StyleConstants.setForeground(style, if (player == PlayerToMove.PlayerOne) Color.BLUE else Color.RED)
        try {
            doc.insertString(doc.length, moveString, style)
        } catch (e: BadLocationException) {
            e.printStackTrace()
        }
    }

    /**
     * Paints the game board and pieces.
     *
     * @param g The Graphics object.
     */
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        // Enable anti-aliasing for smoother text
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Calculate the size of each square
        val boardSize = minOf(width, height)
        val squareSize = boardSize / 9

        // Draw the board squares and pieces
        for (row in 0 until 9) {
            for (column in 0 until 9) {
                val x = column * squareSize
                val y = row * squareSize

                // Draw the square
                if ((row + column) % 2 == 0) {
                    g2d.color = Color.WHITE
                } else {
                    g2d.color = Color.LIGHT_GRAY
                }
                g2d.fillRect(x, y, squareSize, squareSize)

                // Draw the index in the top-left corner of the square
                val indexLabel = "${('A' + column)}${9 - row}"
                g2d.font = Font("Arial", Font.PLAIN, squareSize / 5)
                g2d.color = Color.BLACK
                val fontMetrics = g2d.fontMetrics
                val labelX = x + squareSize / 20  // Small margin from the left edge
                val labelY = y + fontMetrics.ascent + squareSize / 20  // Small margin from the top edge
                g2d.drawString(indexLabel, labelX.toInt(), labelY.toInt())

                // Draw the piece if present
                val piece = pm.piecePositions[Point(row, column)]
                if (piece != null) {
                    val image = if (piece == PlayerToMove.PlayerOne) redPawnImage else blackPawnImage
                    if (image != null) {
                        val imageWidth = image.width
                        val imageHeight = image.height
                        val scaleFactor = minOf(
                            squareSize.toDouble() / imageWidth,
                            squareSize.toDouble() / imageHeight
                        ) * 0.8  // Reduce size to leave space for indices
                        val scaledWidth = (imageWidth * scaleFactor).toInt()
                        val scaledHeight = (imageHeight * scaleFactor).toInt()
                        val xOffsetImage = x + (squareSize - scaledWidth) / 2
                        val yOffsetImage = y + (squareSize - scaledHeight) / 2 + squareSize / 10  // Adjust for index label
                        g2d.drawImage(image, xOffsetImage, yOffsetImage, scaledWidth, scaledHeight, null)
                    } else {
                        g2d.color = if (piece == PlayerToMove.PlayerOne) Color.WHITE else Color.BLACK
                        val pieceSize = (squareSize * 0.6).toInt()
                        g2d.fillOval(
                            x + (squareSize - pieceSize) / 2,
                            y + (squareSize - pieceSize) / 2 + squareSize / 10,  // Adjust for index label
                            pieceSize,
                            pieceSize
                        )
                    }
                }
            }
        }
    }

    /**
     * Gets the preferred size of the component.
     *
     * @return The preferred Dimension.
     */
    override fun getPreferredSize(): Dimension {
        val size = minOf(parent.width, parent.height)
        return Dimension(size, size)
    }
}