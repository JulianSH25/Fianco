package Frontend

import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.LineBorder

class FiancoGUI : JFrame() {

    private val gameNameLabel = JLabel("FIANCO")
    private val clockLabel = JLabel("00:00")
    private val board = Board()
    private val clock = Clock(clockLabel)

    init {
        UIManager.put("Serif", Font("Georgia", Font.PLAIN, 12))

        title = "Fianco Game"
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()


        gameNameLabel.font = Font("Arial", Font.BOLD, 24)
        val namePanel = JPanel()
        namePanel.add(gameNameLabel)
        add(namePanel, BorderLayout.NORTH)

        clockLabel.font = Font("Arial", Font.PLAIN, 18)
        val clockBox = JPanel()
        clockBox.border = LineBorder(Color.BLACK)
        clockBox.add(clockLabel)
        add(clockBox, BorderLayout.EAST)

        board.border = LineBorder(Color.BLACK)

        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(board, BorderLayout.CENTER)
        add(mainPanel, BorderLayout.CENTER)

        clock.startCountdown()

        pack()
        isVisible = true
    }
}

fun main() {
    SwingUtilities.invokeLater { FiancoGUI() }
}