package Frontend

import java.awt.*
import javax.swing.*
import javax.swing.border.LineBorder
import Backend.PlayerTypes
import java.io.OutputStream
import java.io.PrintStream

class FiancoGUI : JFrame() {

    private val gameNameLabel = JLabel("FIANCO")
    private val playerOneClockLabel = JLabel("05:00")
    private val playerTwoClockLabel = JLabel("05:00")
    private val currentPlayerLabel = JLabel("Current Player: PlayerOne")
    private val board = Board(this)
    private val clock = Clock(playerOneClockLabel, playerTwoClockLabel, board)
    private var playerOne = PlayerTypes.HUMAN
    private var playerTwo = PlayerTypes.AI_ENGINE

    // Get the screen dimensions
    private val screenSize = Toolkit.getDefaultToolkit().screenSize
    // Calculate 80% of the screen dimensions
    private val windowWidth = (screenSize.width * 0.8).toInt()
    private val windowHeight = (screenSize.height * 0.8).toInt()

    // Move history title
    private val moveHistoryTitle = JLabel("List of moves:").apply {
        font = Font("Arial", Font.BOLD, 16)
        horizontalAlignment = JLabel.CENTER
    }

    // Move history area
    private val moveHistoryArea = JTextPane().apply {
        isEditable = false
    }
    private val moveHistoryScrollPane = JScrollPane(moveHistoryArea)

    // Left pane containing the title and move history
    private val leftPane = JPanel().apply {
        layout = BorderLayout()
        preferredSize = Dimension(200, 0)
        add(moveHistoryTitle, BorderLayout.NORTH)
        add(moveHistoryScrollPane, BorderLayout.CENTER)
    }

    // Output area for terminal output
    private val outputTextArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }
    private val outputScrollPane = JScrollPane(outputTextArea).apply {
        preferredSize = Dimension(200, 0)
    }

    // Clocks pane
    private val clocksPane = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        preferredSize = Dimension(100, 0) // Adjust as needed
        add(playerTwoClockLabel)
        add(Box.createVerticalGlue()) // Pushes the next component to the bottom
        add(playerOneClockLabel)
    }

    // Right pane containing clocks and output area
    private val rightPane = JPanel().apply {
        layout = BorderLayout()
        preferredSize = Dimension(300, 0) // Adjust width as needed
        add(clocksPane, BorderLayout.WEST)
        // The outputScrollPane will be added dynamically to the CENTER
    }

    private val playerOneTypeButton = JButton("Player One: ${playerOne.name}")
    private val playerTwoTypeButton = JButton("Player Two: ${playerTwo.name}")
    private val startButton = JButton("Start Game")
    private val displayStatsCheckbox = JCheckBox("Display Statistics (Terminal output)")

    // For redirecting console output
    private var standardOut: PrintStream? = null

    init {
        UIManager.put("Serif", Font("Georgia", Font.PLAIN, 12))

        title = "Fianco Game"
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()


        gameNameLabel.font = Font("Arial", Font.BOLD, 24)
        val namePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(gameNameLabel)
            add(currentPlayerLabel)
        }
        add(namePanel, BorderLayout.NORTH)

        board.border = LineBorder(Color.BLACK)

        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(board, BorderLayout.CENTER)
        add(mainPanel, BorderLayout.CENTER)

        // Add the left pane
        add(leftPane, BorderLayout.WEST)

        // Add the right pane
        add(rightPane, BorderLayout.EAST)

        // Control Panel for Buttons
        val controlPanel = JPanel()
        controlPanel.add(playerOneTypeButton)
        controlPanel.add(playerTwoTypeButton)
        controlPanel.add(startButton)
        controlPanel.add(displayStatsCheckbox) // Add the checkbox here
        add(controlPanel, BorderLayout.SOUTH)

        // Action Listeners for Buttons
        playerOneTypeButton.addActionListener {
            playerOne = playerOne.next()
            playerOneTypeButton.text = "Player One: ${playerOne.name}"
            board.setPlayerOneType(playerOne)
        }

        playerTwoTypeButton.addActionListener {
            playerTwo = playerTwo.next()
            playerTwoTypeButton.text = "Player Two: ${playerTwo.name}"
            board.setPlayerTwoType(playerTwo)
        }

        startButton.addActionListener {
            startGame()
        }

        // Action Listener for the checkbox
        displayStatsCheckbox.addActionListener {
            if (displayStatsCheckbox.isSelected) {
                redirectConsoleOutput()
                // Add the output area to the CENTER of rightPane
                rightPane.add(outputScrollPane, BorderLayout.CENTER)
                rightPane.revalidate()
                rightPane.repaint()
            } else {
                // Restore the standard output stream
                if (standardOut != null) {
                    System.setOut(standardOut)
                }
                outputTextArea.text = ""
                // Remove the output area from the right pane
                rightPane.remove(outputScrollPane)
                rightPane.revalidate()
                rightPane.repaint()
            }
        }

        // Set the preferred size of the JFrame
        setPreferredSize(Dimension(windowWidth, windowHeight))

        pack()
        setSize(windowWidth, windowHeight)
        setLocationRelativeTo(null) // Center the window
        isVisible = true
    }

    private fun redirectConsoleOutput() {
        // Save the standard output stream
        if (standardOut == null) {
            standardOut = System.out
        }

        val outputStream = object : OutputStream() {
            override fun write(b: Int) {
                SwingUtilities.invokeLater {
                    outputTextArea.append(b.toChar().toString())
                    outputTextArea.caretPosition = outputTextArea.document.length
                }
            }
        }

        val printStream = PrintStream(outputStream, true)
        System.setOut(printStream)
    }

    fun startGame() {
        board.initializeBoard()
        board.setPlayers(playerOne, playerTwo)
        clock.reset()
        clock.start()
        // If the current player is AI, start the AI move
        board.checkAndStartAI()
    }

    fun updateCurrentPlayerLabel(playerName: String) {
        currentPlayerLabel.text = "Current Player: $playerName"
    }

    fun getMoveHistoryArea(): JTextPane {
        return moveHistoryArea
    }

    fun getClock(): Clock {
        return clock
    }
}

// Add the main function to run the application
fun main() {
    SwingUtilities.invokeLater { FiancoGUI() }
}