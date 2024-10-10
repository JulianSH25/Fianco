package Backend

import java.awt.Point

val max_buckets = 1024000

/**
 * Represents an entry in the transposition table.
 *
 * Stores information about a specific board position, including:
 * - The Zobrist hash value of the board position.
 * - The score associated with the position.
 * - The best move from this position.
 * - The depth at which the score was calculated.
 * - The type of score (accurate, fail-low, fail-high).
 */
class TableEntry {
    /**
     * Enum representing the type of score stored.
     *
     * - ACCURATE: An exact score has been calculated.
     * - FAIL_LOW: The score is a lower bound (alpha cut-off).
     * - FAIL_HIGH: The score is an upper bound (beta cut-off).
     */
    enum class ScoreType {
        ACCURATE, FAIL_LOW, FAIL_HIGH
    }

    /** The Zobrist hash value representing the board position. */
    var hashValue: Long = 0L

    /** The type of score stored (accurate, fail-low, fail-high). */
    var scoreType: ScoreType = ScoreType.ACCURATE

    /** The score of the board position evaluated at a certain depth. */
    var score: Int = 0

    /** The best move from this position, as determined by search algorithms. */
    var bestMove: Pair<Point, Point>? = null

    /** The depth of the search at which this score was calculated. */
    var depth: Int = 0
}

/**
 * Represents a node in a linked list within a hash table bucket.
 *
 * Used to handle collisions in the hash table by chaining entries with the same bucket index.
 */
class BucketItem(
    /** The table entry stored in this bucket item. */
    val entry: TableEntry,
    /** The next bucket item in the linked list. */
    var next: BucketItem? = null
) {
    /**
     * Recursively searches for an entry with the matching hash value in the linked list.
     *
     * @param hashValue The Zobrist hash value to search for.
     * @return The matching `TableEntry` if found; otherwise, `null`.
     */
    fun getElement(hashValue: Long): TableEntry? {
        return if (entry.hashValue == hashValue) {
            entry
        } else {
            next?.getElement(hashValue)
        }
    }

    /**
     * Adds a new entry to the linked list or replaces an existing entry with the same hash value.
     *
     * @param newEntry The new `TableEntry` to add.
     */
    fun addOrReplaceEntry(newEntry: TableEntry) {
        if (entry.hashValue == newEntry.hashValue) {
            // Replace the existing entry
            entry.score = newEntry.score
            entry.scoreType = newEntry.scoreType
            entry.bestMove = newEntry.bestMove
            entry.depth = newEntry.depth
        } else {
            if (next == null) {
                next = BucketItem(newEntry)
            } else {
                next!!.addOrReplaceEntry(newEntry)
            }
        }
    }

    /**
     * Clears the linked list starting from this bucket item.
     */
    fun clear() {
        next?.clear()
        next = null
    }
}

/**
 * Manages the transposition table used in game AI.
 *
 * Acts as a wrapper around `HashArray` to provide additional functionality if needed.
 */
class TranspositionTable {
    /** The underlying hash array that stores the table entries. */
    private val hashArray = HashArray()

    /**
     * Retrieves an entry from the transposition table based on the hash value.
     *
     * @param hashValue The Zobrist hash value of the board position.
     * @return The corresponding `TableEntry` if found; otherwise, `null`.
     */
    fun getEntry(hashValue: Long): TableEntry? {
        return hashArray.getEntry(hashValue)
    }

    /**
     * Stores an entry in the transposition table.
     *
     * @param entry The `TableEntry` to store.
     */
    fun storeEntry(entry: TableEntry) {
        hashArray.storeEntry(entry)
    }

    /**
     * Clears all entries from the transposition table.
     */
    fun clear() {
        hashArray.clear()
    }
}

class HashArray {
    companion object {
        /** The number of buckets in the hash table (size of the table). */
        const val MAX_BUCKETS = 1_024_000
    }

    /** The array of buckets representing the transposition table. */
    private val buckets: Array<BucketItem?> = arrayOfNulls(MAX_BUCKETS)

    /**
     * Retrieves an entry from the transposition table based on the hash value.
     *
     * @param hashValue The Zobrist hash value of the board position.
     * @return The corresponding `TableEntry` if found; otherwise, `null`.
     */
    fun getEntry(hashValue: Long): TableEntry? {
        val index = (hashValue % MAX_BUCKETS).toInt()
        val bucket = buckets[index]
        return bucket?.getElement(hashValue)
    }

    /**
     * Stores an entry in the transposition table.
     *
     * Handles collisions by chaining entries in a linked list within the bucket.
     *
     * @param entry The `TableEntry` to store.
     */
    fun storeEntry(entry: TableEntry) {
        val index = (entry.hashValue % MAX_BUCKETS).toInt()
        val bucket = buckets[index]
        if (bucket == null) {
            // No collision; place the entry directly
            buckets[index] = BucketItem(entry)
        } else {
            // Collision; add or replace the entry in the linked list
            bucket.addOrReplaceEntry(entry)
        }
    }

    /**
     * Clears all entries from the transposition table.
     */
    fun clear() {
        for (i in buckets.indices) {
            buckets[i]?.clear()
            buckets[i] = null
        }
    }
}