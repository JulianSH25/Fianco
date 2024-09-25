package Frontend

import javax.swing.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

class Clock(private val clockLabel: JLabel) {

    private var secondsRemaining = Constants.INITIAL_TIME_SECONDS
    private val timer = Timer(1000, null)

    init {
        timer.addActionListener(ActionListener {
            secondsRemaining--
            val minutes = secondsRemaining / 60
            val seconds = secondsRemaining % 60
            clockLabel.text = String.format("%02d:%02d", minutes, seconds)

            if (secondsRemaining == 0) {
                timer.restart()
                // Handle time's up
            }
        })
    }

    fun startCountdown() {
        timer.start()
    }
}