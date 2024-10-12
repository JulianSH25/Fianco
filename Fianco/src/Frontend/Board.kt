package Frontend

import Backend.AlphaBetaEngine
import Backend.PieceManager
import Backend.UtilityFunctions.*
import Backend.UtilityFunctions.createPiecePositionsFromBoard
import Backend.generateMoves
import Backend.makeMove

import javax.swing.SwingWorker
import javax.swing.SwingUtilities
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.LineBorder
import Backend.PlayerTypes
import Backend.Player.setPlayers
import Backend.Player.getPlayerToMove
import Backend.PlayerToMove
import Backend.Player as player

class Board : JComponent() {

    private val AIstarting = false
    private val pm = PieceManager()

    private var initialiseAI = true

    private val playerOne = PlayerTypes.HUMAN
    private val playerTwo = PlayerTypes.AI_ENGINE

    private var selectedPiece: Point? = null
    //private var currentPlayer = playerOne //Color.WHITE

    private val messageLabel = JLabel()
    private val infoLabel = JLabel()
    private val redPawnImage: BufferedImage? = ImageIO.read(if (AIstarting) javaClass.getResource("figures/blackPawn.png") else javaClass.getResource("figures/whitePawn.png"))
    private val blackPawnImage: BufferedImage? = ImageIO.read(if (AIstarting) javaClass.getResource("figures/whitePawn.png") else javaClass.getResource("figures/blackPawn.png"))


    init {
        infoLabel.font = infoLabel.font.deriveFont(Font.PLAIN, 14f)
        infoLabel.border = LineBorder(Color.BLACK)
        infoLabel.horizontalAlignment = JLabel.CENTER
        infoLabel.isOpaque = true
        add(infoLabel, BorderLayout.NORTH)

        messageLabel.font = messageLabel.font.deriveFont(Font.BOLD)
        messageLabel.border = LineBorder(Color.BLACK)
        messageLabel.horizontalAlignment = JLabel.CENTER
        messageLabel.isOpaque = true
        add(messageLabel, BorderLayout.SOUTH)

        setPlayers(playerOne, playerTwo)

        initializeBoard()
        

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                // Save position at which the mouse was PRESSED
                val row = e.y / Constants.SQUARE_SIZE
                val column = e.x / Constants.SQUARE_SIZE
                val position = Point(row, column)

                if (position in pm.piecePositions && pm.piecePositions[position] == getPlayerToMove()) {
                    selectedPiece = position
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (selectedPiece != null) {
                    // Save position at which the mouse was RELEASED
                    val newRow = e.y / Constants.SQUARE_SIZE
                    val newColumn = e.x / Constants.SQUARE_SIZE
                    val newPosition = Point(newRow, newColumn)

                    // Handle the move and potentially trigger AI move asynchronously
                    if (handleMove(selectedPiece!!, newPosition) && initialiseAI) {
                        selectedPiece = null
                        aiMove()  // Changed from aiMove() to aiMoveAsync()
                    } else {
                        selectedPiece = null
                    }
                }
            }
        })

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                repaint()
            }
        })
    }


    fun aiMove() {
        val aiWorker = object : SwingWorker<Unit, Unit>() {
            var nodesExplored = 0

        override fun doInBackground() {
            val maxDepth = 10  // Set a reasonable maximum depth
            val timeLimit = 5000L  // Time limit in milliseconds (e.g., 5 seconds)
            val boardCopy = pm.getBoardCopy()
            val positionsCopy = createPiecePositionsFromBoard(boardCopy)
            val (bestMove, nodeCount) = getBestMove(boardCopy, positionsCopy, maxDepth, timeLimit)
            nodesExplored = nodeCount
            if (bestMove != null) {
                SwingUtilities.invokeLater {
                    handleMoveAI(bestMove.first, bestMove.second, bestMove.third)
                    repaint()
                    checkForWinner()
                }
            } else {
                println("AI has no valid moves!")
            }
        }

        override fun done() {
            // Optionally update GUI components after AI move is completed
            SwingUtilities.invokeLater {
                infoLabel.text = "Nodes explored: $nodesExplored | Depth: variable (iterative deepening)"
            }
        }
    }
    aiWorker.execute()
}


    private fun initializeBoard() {
        for (i in 0 until 9) {
            pm.setPiece(8, i, 1)
            if (AIstarting && i < 8) {
                pm.setPiece(0, i, 2)
            }
            else if (!AIstarting)
            {
                pm.setPiece(0, i, 2)
            }
        }
        if (AIstarting){
            pm.setPiece(1, 8, 2)
        }
        for (i in 1 until 4) {
            pm.setPiece(8-i, i, 1)
            pm.setPiece(i, i, 2)

            pm.setPiece(i, 8 - i, 2)
            pm.setPiece(8 - i, 8 - i, 1)
        }
    }

    fun getBestMove(
    board: Array<Array<Int>>,
    positions: Map<Point, PlayerToMove>,
    maxDepth: Int,
    timeLimit: Long = 20000  // Time limit in milliseconds
    ) : Pair<Triple<Point, Point, Map<Point, List<Point>>?>?, Int> {

        val alphaBetaEngine = AlphaBetaEngine(pm)
        alphaBetaEngine.nodesExplored = 0  // Reset the node counter

        var bestMove: Triple<Point, Point, Map<Point, List<Point>>?>? = null
        var bestValue = Int.MIN_VALUE
        val startTime = System.currentTimeMillis()

        for (depth in 1..maxDepth) {
            val timeElapsed = System.currentTimeMillis() - startTime
            if (timeElapsed >= timeLimit) {
                println("Time limit reached during depth $depth")
                break
            }

            val (moves, type_of_move) = generateMoves(pm, 2, board, positions)  // AI is player 2 (black)
            var currentBestValue = Int.MIN_VALUE
            var currentBestMove: Triple<Point, Point, Map<Point, List<Point>>?>? = null

            // Check if there's only one move and that move has exactly one destination
            if (moves.size == 1 && moves.values.first().size == 1) {
                // Get the key (fromPosition) and the single destination (toPosition)
                val fromPosition = moves.keys.first()
                val toPosition = moves.values.first().first()

                // Set the best move directly
                bestMove = Triple(fromPosition, toPosition, if (type_of_move == "Capture") moves else null)
                break  // Exit the loop since we found our move
            }

            for ((fromPosition, toPositions) in moves) {
                for (toPosition in toPositions) {
                    val newBoard = copyBoard(board)
                    makeMove(newBoard, fromPosition, toPosition, type_of_move == "Capture")
                    val eval = -alphaBetaEngine.alphaBetaWithTime(
                        newBoard,
                        depth - 1,
                        Int.MIN_VALUE,
                        Int.MAX_VALUE,
                        -1,
                        startTime,
                        timeLimit
                    )
                    if (eval > currentBestValue) {
                        currentBestValue = eval
                        currentBestMove =
                            Triple(fromPosition, toPosition, if (type_of_move == "Capture") moves else null)
                    }
                    if (alphaBetaEngine.timeUp) {
                        break
                    }
                }
                if (alphaBetaEngine.timeUp) {
                    break
                }
            }

            if (!alphaBetaEngine.timeUp) {
                bestValue = currentBestValue
                bestMove = currentBestMove
                println("Depth $depth completed. Best value: $bestValue")
            } else {
                println("Time limit reached during depth $depth")
                break
            }
        }
        alphaBetaEngine.printStatistics()
        // Return the best move found within the time limit
        return Pair(bestMove, alphaBetaEngine.nodesExplored)
    }

    fun handleMoveAI(oldPosition: Point, newPosition: Point, captureMap: Map<Point, List<Point>>? = null) {

        if (captureMap != null) {
            pm.capturePiece(oldPosition, newPosition)
        }
        else{
            pm.movePiece(oldPosition, newPosition)
        }

        // Switch player
        player.switchPlayerToMove()
        repaint()

        checkForWinner()
    }

    fun handleMove(oldPosition: Point, newPosition: Point): Boolean {
        val (validMove, captureMap) = isValidMove(pm, oldPosition, newPosition, pm.piecePositions, getPlayerToMove())

        if (validMove) {
            pm.movePiece(oldPosition, newPosition)
            print("Old position: $oldPosition, New position: $newPosition")

            // Handle captures
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
                print("Moved manually from $oldPosition to ${newPosition.x + dx}, ${newPosition.y} and removed piece at $capturedX, $capturedY")
            }

            // Switch player
            player.switchPlayerToMove()
            repaint()

            checkForWinner()
            return true // allow AI opponent to move
        } else {
            // Handle illegal move (e.g., display a message, reset selectedPiece)
            println("Illegal move!")
            return false // move has to be repeated
        }
    }

    fun checkForWinner() {
        val winner = checkVictory(pm)
        if (winner != null) {
            val message: String = if (winner == PlayerToMove.PlayerOne) "White Won!" else "Black Won!"
            JOptionPane.showMessageDialog(this@Board, message, "Game Over", JOptionPane.INFORMATION_MESSAGE)

            val options: Array<String> = arrayOf("Yes", "No")
            val choice = JOptionPane.showOptionDialog(
                this@Board,
                "Do you want to restart the game?",
                "Game Over",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            )

            if (choice == JOptionPane.YES_OPTION) {
                // restartGame()  // Implement this method to reset the game
            } else {
                SwingUtilities.getWindowAncestor(this@Board)?.dispatchEvent(WindowEvent(SwingUtilities.getWindowAncestor(this@Board), WindowEvent.WINDOW_CLOSING))
            }
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val currentSquareSize = minOf(width / 9, height / 9)

        for (row in 0 until 9) {
            for (column in 0 until 9) {
                // Rotate 90 degrees right and then mirror horizontally
                val rotatedRow = column
                val rotatedColumn = row

                // Convert to new positions after the transformation
                val x = rotatedColumn * currentSquareSize
                val y = rotatedRow * currentSquareSize

                // Draw the squares
                if ((row + column) % 2 == 0) {
                    g.color = Color.WHITE
                } else {
                    g.color = Color.LIGHT_GRAY
                }
                g.fillRect(x, y, currentSquareSize, currentSquareSize)

                // Draw the pieces in their new positions
                val piece = pm.piecePositions[Point(column, row)]
                if (piece != null) {
                    val image = if (piece == PlayerToMove.PlayerOne) redPawnImage else blackPawnImage
                    if (image != null) {
                        val imageWidth = image.width
                        val imageHeight = image.height
                        val scaleFactor = minOf(
                            currentSquareSize.toDouble() / imageWidth,
                            currentSquareSize.toDouble() / imageHeight
                        )
                        val scaledWidth = (imageWidth * scaleFactor).toInt()
                        val scaledHeight = (imageHeight * scaleFactor).toInt()
                        val xOffset = (currentSquareSize - scaledWidth) / 2
                        val yOffset = (currentSquareSize - scaledHeight) / 2

                        g.drawImage(image, x + xOffset, y + yOffset, scaledWidth, scaledHeight, null)
                    } else {
                        g.color = if (piece == PlayerToMove.PlayerOne) Color.WHITE else Color.BLACK
                        g.fillOval(
                            x + (currentSquareSize - Constants.PIECE_SIZE) / 2,
                            y + (currentSquareSize - Constants.PIECE_SIZE) / 2,
                            Constants.PIECE_SIZE,
                            Constants.PIECE_SIZE
                        )
                    }
                }
            }
        }
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(9 * Constants.SQUARE_SIZE, 9 * Constants.SQUARE_SIZE)
    }
}