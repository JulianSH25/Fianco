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
    private val redPawnImage: BufferedImage? = ImageIO.read(javaClass.getResource("figures/whitePawn.png"))
    private val blackPawnImage: BufferedImage? = ImageIO.read(javaClass.getResource("figures/blackPawn.png"))
    //val pieceArray = Array(9) { Array(9) { 0 } }

    private var globalCaptureMap = mutableMapOf<Point, MutableList<Point>>()


    init {
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

                    // Handle the move and potentially trigger AI move
                    if (handleMove(selectedPiece!!, newPosition) && initialiseAI) {
                        selectedPiece = null
                        randomAI()
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
    fun handleMoveAIOLD(oldPosition: Point, newPosition: Point, captureMap: Map<Point, List<Point>>? = null) {
        val dx = if (captureMap != null) newPosition.x - oldPosition.x else 0
        val dy = if (captureMap != null) newPosition.y - oldPosition.y else 0

        movePiece(oldPosition, newPosition)

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
            print("Moved from $oldPosition to ${newPosition.x + dx}, ${newPosition.y} and removed piece at $capturedX, $capturedY")
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

    /*private fun restartGame() {
        // Clear all piece positions
        piecePositions.clear()

        // Reset the pieceArray
        for (i in 0 until 9) {
            pieceArray[i][0] = 1
            piecePositions[Point(i, 0)] = Color.WHITE
            pieceArray[i][8] = 2
            piecePositions[Point(i, 8)] = Color.BLACK
        }

        // Reset additional pieces based on your original setup
        for (i in 1 until 4) {
            piecePositions[Point(i, i)] = Color.WHITE
            pieceArray[i][i] = 1
            piecePositions[Point(8 - i, i)] = Color.WHITE
            pieceArray[8 - i][i] = 1

            piecePositions[Point(i, 8 - i)] = Color.BLACK
            pieceArray[i][8 - i] = 2
            piecePositions[Point(8 - i, 8 - i)] = Color.BLACK
            pieceArray[8 - i][8 - i] = 2
        }

        // Reset the current player
        currentPlayer = Color.WHITE

        // Clear any global captures
        globalCaptureMap.clear()

        // Repaint the board to reflect the reset
        repaint()
    }*/
}
