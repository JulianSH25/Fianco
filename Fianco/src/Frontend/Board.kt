package Frontend

import Backend.Engine
import Backend.UtilityFunctions.*
import Frontend.PieceManager.setPiece
import Frontend.PieceManager.getPiece
import Frontend.PieceManager.getBoardCopy
import Frontend.PieceManager.piecePositions
import Frontend.PieceManager.movePiece
import Frontend.PieceManager.capturePiece

import Backend.Engine.*

import javax.swing.SwingWorker
import javax.swing.SwingUtilities
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.LineBorder
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class Board : JComponent() {

    private var initialiseAI = true

    private var selectedPiece: Point? = null
    //private val piecePositions = mutableMapOf<Point, Color>()
    private val whitePositions = mutableMapOf<Point, Color>()
    private val blackPositions = mutableMapOf<Point, Color>()
    private var currentPlayer = Color.WHITE

    private val messageLabel = JLabel()
    private val infoLabel = JLabel()
    private val redPawnImage: BufferedImage? = ImageIO.read(javaClass.getResource("figures/whitePawn.png"))
    private val blackPawnImage: BufferedImage? = ImageIO.read(javaClass.getResource("figures/blackPawn.png"))
    //val pieceArray = Array(9) { Array(9) { 0 } }

    private var globalCaptureMap = mutableMapOf<Point, MutableList<Point>>()


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

        initializeBoard()

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val row = e.y / Constants.SQUARE_SIZE
                val column = e.x / Constants.SQUARE_SIZE

                print("Column value: $column, row: $row")

                val position = Point(row, column)

                if (position in piecePositions && piecePositions[position] == currentPlayer) {
                    selectedPiece = position
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (selectedPiece != null) {
                    val newRow = e.y / Constants.SQUARE_SIZE
                    val newColumn = e.x / Constants.SQUARE_SIZE
                    val newPosition = Point(newRow, newColumn)

                    // Log for debugging
                    println("Attempting to move piece from $selectedPiece to $newPosition")

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
            val timeLimit = 20000L  // Time limit in milliseconds (e.g., 5 seconds)
            val boardCopy = PieceManager.getBoardCopy()
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

    fun randomAI(){
        val playerIDINT = if (currentPlayer == Color.WHITE) 1 else if (currentPlayer == Color.BLACK) 2 else throw IndexOutOfBoundsException("Player ID error - check implementation of current player in Frontend")
        val pieceArray = getBoardCopy()

        val (positionMap, typeOfMove) = generateMoves(playerIDINT, pieceArray)

        val (randomkey, randommove) = pickRandomMove(positionMap)

        handleMoveAI(randomkey, randommove, if (typeOfMove == "Capture") positionMap else null)

    }

    fun pickRandomMove(pieceMap: Map<Point, List<Point>>): Pair<Point, Point> {
        if (pieceMap.isEmpty()) throw IOException("Empty piece map")

        // Step 1: Get a random key (Point) from the map
        val keys = pieceMap.keys.toList()
        val randomKey = keys[Random.nextInt(keys.size)]

        // Step 2: Get the associated list of points
        val moves = pieceMap[randomKey] ?: throw IOException("piece map key does not exist")

        // Step 3: Get a random move from the list of moves
        if (moves.isEmpty()) return throw IOException("piece map move does not exist (key error)")
        val randomMove = moves[Random.nextInt(moves.size)]

        return Pair(randomKey, randomMove)
    }


    private fun initializeBoard() {
        for (i in 0 until 9) {
            setPiece(8, i, 1)
            setPiece(0, i, 2)
        }
        for (i in 1 until 4) {
            setPiece(8-i, i, 1)
            setPiece(i, i, 2)

            setPiece(i, 8 - i, 2)
            setPiece(8 - i, 8 - i, 1)
        }
    }

    fun getBestMove(
    board: Array<Array<Int>>,
    positions: Map<Point, Color>,
    maxDepth: Int,
    timeLimit: Long = 20000  // Time limit in milliseconds
): Pair<Triple<Point, Point, Map<Point, List<Point>>?>?, Int> {
    val engine = Engine()
    engine.nodesExplored = 0  // Reset the node counter

    var bestMove: Triple<Point, Point, Map<Point, List<Point>>?>? = null
    var bestValue = Int.MIN_VALUE
    val startTime = System.currentTimeMillis()

    for (depth in 1..maxDepth) {
        val timeElapsed = System.currentTimeMillis() - startTime
        if (timeElapsed >= timeLimit) {
            println("Time limit reached during depth $depth")
            break
        }

        val moves = generateMoves(2, board, positions)  // AI is player 2 (black)
        var currentBestValue = Int.MIN_VALUE
        var currentBestMove: Triple<Point, Point, Map<Point, List<Point>>?>? = null

        for ((fromPosition, toPositions) in moves.first) {
            for (toPosition in toPositions) {
                val newBoard = copyBoard(board)
                makeMove(newBoard, fromPosition, toPosition, moves.second == "Capture")
                val eval = -engine.alphaBetaWithTime(newBoard, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, -1, startTime, timeLimit)
                if (eval > currentBestValue) {
                    currentBestValue = eval
                    currentBestMove = Triple(fromPosition, toPosition, if (moves.second == "Capture") moves.first else null)
                }
                if (engine.timeUp) {
                    break
                }
            }
            if (engine.timeUp) {
                break
            }
        }

        if (!engine.timeUp) {
            bestValue = currentBestValue
            bestMove = currentBestMove
            println("Depth $depth completed. Best value: $bestValue")
        } else {
            println("Time limit reached during depth $depth")
            break
        }
    }

    // Return the best move found within the time limit
    return Pair(bestMove, engine.nodesExplored)
}

    fun handleMoveAI(oldPosition: Point, newPosition: Point, captureMap: Map<Point, List<Point>>? = null) {

        if (captureMap != null) {
            capturePiece(oldPosition, newPosition)
        }
        else{
            movePiece(oldPosition, newPosition)
        }

        // Switch player
        currentPlayer = if (currentPlayer == Color.WHITE) Color.BLACK else Color.WHITE
        repaint()

        checkForWinner()
    }

    fun handleMove(oldPosition: Point, newPosition: Point): Boolean {
        val (validMove, captureMap) = isValidMove(oldPosition, newPosition, piecePositions, currentPlayer)

        if (validMove) {
            movePiece(oldPosition, newPosition)
            print("Old position: $oldPosition, New position: $newPosition")

            // Handle captures
            captureMap?.get(oldPosition)?.let { capturedPieces ->
                val dx = newPosition.x - oldPosition.x
                val dy = newPosition.y - oldPosition.y
                val capturedX = oldPosition.x + dx / 2
                val capturedY = oldPosition.y + dy / 2

                capturedPieces.find { it.x == capturedX && it.y == capturedY }?.let { capturedPiece ->
                    piecePositions.remove(capturedPiece)
                    setPiece(capturedPiece.x, capturedPiece.y, 0)
                    repaint()
                }
                print("Moved manually from $oldPosition to ${newPosition.x + dx}, ${newPosition.y} and removed piece at $capturedX, $capturedY")
            }

            // Switch player
            currentPlayer = if (currentPlayer == Color.WHITE) Color.BLACK else Color.WHITE
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
        val winner = checkVictory()
        if (winner != null) {
            val message: String = if (winner == Color.WHITE) "White Won!" else "Black Won!"
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
                val piece = piecePositions[Point(column, row)]
                if (piece != null) {
                    val image = if (piece == Color.WHITE) redPawnImage else blackPawnImage
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
                        g.color = piece
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

private fun Engine.copyBoard(arrays: Array<Array<Int>>) {}
