package Backend.Utilities

import kotlin.concurrent.thread

/**
 * Manages a countdown timer running on a separate thread.
 *
 * Sets the [timeUp] flag to true when the specified time limit is reached.
 *
 * @property timeLimitMillis The time limit in milliseconds.
 */
class TimeKeeper(private val timeLimitMillis: Short) {
    /**
     * Flag indicating whether the time limit has been reached.
     * Marked as @Volatile to ensure visibility across threads.
     */
    @Volatile
    var timeUp: Boolean = false
        private set

    // Internal thread that manages the countdown
    private val timerThread: Thread = thread(start = true, isDaemon = true, name = "TimeKeeperThread") {
        try {
            // Sleep for the specified time limit
            Thread.sleep(timeLimitMillis.toLong())
            // After waking up, set the timeUp flag to true
            timeUp = true
            println("TimeKeeper: Time is up!")
        } catch (e: InterruptedException) {
            // Timer was canceled before the time limit was reached
            println("TimeKeeper: Timer was canceled.")
        }
    }

    /**
     * Cancels the timer if it's still running.
     * Interrupts the internal thread to prevent the timeUp flag from being set.
     */
    fun cancel() {
        if (timerThread.isAlive) {
            timerThread.interrupt()
        }
    }
}