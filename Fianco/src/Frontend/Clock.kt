package Frontend

import javax.swing.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import Backend.PlayerToMove
import java.util.concurrent.atomic.AtomicBoolean

class Clock(
    private val playerOneLabel: JLabel,
    private val playerTwoLabel: JLabel,
    private val board: Board
) {

    private var playerOneSeconds = Constants.INITIAL_TIME_SECONDS
    private var playerTwoSeconds = Constants.INITIAL_TIME_SECONDS
    private var currentPlayer: PlayerToMove = PlayerToMove.PlayerOne
    private val timer = Timer(1000, null) // Timer ticks every 1000 milliseconds (1 second)
    private val running = AtomicBoolean(false)

    init {
        timer.addActionListener {
            if (currentPlayer == PlayerToMove.PlayerOne) {
                playerOneSeconds--
                updateLabel(playerOneLabel, playerOneSeconds)
                if (playerOneSeconds == 0) {
                    timer.stop()
                    JOptionPane.showMessageDialog(board, "Player One's time is up! Player Two wins!", "Time Up", JOptionPane.INFORMATION_MESSAGE)
                    board.endGame()
                }
            } else {
                playerTwoSeconds--
                updateLabel(playerTwoLabel, playerTwoSeconds)
                if (playerTwoSeconds == 0) {
                    timer.stop()
                    JOptionPane.showMessageDialog(board, "Player Two's time is up! Player One wins!", "Time Up", JOptionPane.INFORMATION_MESSAGE)
                    board.endGame()
                }
            }
        }
    }

    /**
     * Updates the clock label with the formatted time.
     *
     * @param label The JLabel to update.
     * @param seconds The number of seconds remaining.
     */
    fun updateLabel(label: JLabel, seconds: Int) {
        val minutes = seconds / 60
        val secs = seconds % 60
        label.text = String.format("%02d:%02d", minutes, secs)
    }

    /**
     * Starts the timer if it's not already running.
     */
    fun start() {
        if (running.compareAndSet(false, true)) {
            timer.start()
        }
    }

    /**
     * Stops the timer if it's running.
     */
    fun stop() {
        if (running.compareAndSet(true, false)) {
            timer.stop()
        }
    }

    /**
     * Switches the current player whose clock is ticking.
     *
     * @param player The player to switch to.
     */
    fun switchPlayer(player: PlayerToMove) {
        currentPlayer = player
    }

    /**
     * Resets both players' clocks to the initial time.
     */
    fun reset() {
        stop()
        playerOneSeconds = Constants.INITIAL_TIME_SECONDS
        playerTwoSeconds = Constants.INITIAL_TIME_SECONDS
        updateLabel(playerOneLabel, playerOneSeconds)
        updateLabel(playerTwoLabel, playerTwoSeconds)
    }
}