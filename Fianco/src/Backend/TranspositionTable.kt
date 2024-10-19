package Backend

import java.awt.Point

/**
 * Enum representing the type of score stored in the transposition table.
 *
 * - EXACT: An exact minimax score.
 * - LOWER_BOUND: A lower bound score (alpha cutoff).
 * - UPPER_BOUND: An upper bound score (beta cutoff).
 */
enum class ScoreType {
    EXACT, LOWER_BOUND, UPPER_BOUND
}

/**
 * Represents an entry in the transposition table.
 *
 * Stores information about a specific board position, including:
 * - The Zobrist hash value of the board position.
 * - The score associated with the position.
 * - The best move from this position.
 * - The depth at which the score was calculated.
 * - The type of score (EXACT, LOWER_BOUND, UPPER_BOUND).
 */
class TableEntry {
    var hashValue: ULong = 0u
    var scoreType: ScoreType = ScoreType.EXACT
    var score: Int = 0
    var bestMove: Pair<Point, Point>? = null
    var searchDepth: Byte = 0

    var age: Byte = 0
}

/**
 * Manages the transposition table used in the alpha-beta search.
 *
 * Implements a hash table with a two-deep replacement scheme to handle collisions.
 */
class TranspositionTable {
    private val hashArray = HashArray()

    /**
     * Retrieves an entry from the transposition table based on the hash value.
     *
     * @param hashValue The Zobrist hash value of the board position.
     * @return The corresponding `TableEntry` if found; otherwise, `null`.
     */
    fun getEntry(hashValue: ULong): TableEntry? {
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

/**
 * Represents a bucket in the hash table for the transposition table.
 *
 * Uses a two-deep replacement scheme to store primary and secondary entries.
 */
class BucketItem {
    var primaryEntry: TableEntry? = null
    var secondaryEntry: TableEntry? = null

    /**
     * Retrieves an entry from the bucket based on the hash value.
     *
     * @param hashValue The Zobrist hash value to search for.
     * @return The corresponding `TableEntry` if found; otherwise, `null`.
     */
    fun getEntry(hashValue: ULong): TableEntry? {
        if (primaryEntry?.hashValue == hashValue) {
            Backend.primaries++
            return primaryEntry
        } else if (secondaryEntry?.hashValue == hashValue) {
            Backend.secondaries++
            return secondaryEntry
        }
        return null
    }

    /**
     * Adds a new entry to the bucket using the two-deep replacement scheme.
     *
     * @param newEntry The new `TableEntry` to add.
     */
    fun storeEntry(newEntry: TableEntry) {
        if (primaryEntry == null) {
            Backend.newlyAddedEntries++
            primaryEntry = newEntry
        } else if (primaryEntry!!.hashValue == newEntry.hashValue) {
            Backend.newlyAddedEntries++
            primaryEntry = newEntry
        } else if (newEntry.searchDepth >= primaryEntry!!.searchDepth) {
            Backend.newlyAddedEntries++
            secondaryEntry = primaryEntry
            primaryEntry = newEntry
        } else if (secondaryEntry == null || secondaryEntry!!.hashValue == newEntry.hashValue) {
            Backend.newlyAddedEntries++
            secondaryEntry = newEntry
        } else if (newEntry.searchDepth >= secondaryEntry!!.searchDepth) {
            Backend.newlyAddedEntries++
            secondaryEntry = newEntry
        }
        else{
            Backend.collisions++
        }
    }

    /**
     * Clears the bucket entries.
     */
    fun clear() {
        primaryEntry = null
        secondaryEntry = null
    }
}

/**
 * Hash array representing the transposition table.
     *
 * Uses Zobrist hashing to index entries.
     */
class HashArray {
    companion object {
        /** The number of buckets in the hash table. */
        const val MAX_BUCKETS: UInt = 536870912u
    }

    private val buckets: Array<BucketItem?> = arrayOfNulls(MAX_BUCKETS.toInt())

    /**
     * Retrieves an entry from the hash array based on the hash value.
     *
     * @param hashValue The Zobrist hash value.
     * @return The corresponding `TableEntry` if found; otherwise, `null`.
     */
    fun getEntry(hashValue: ULong): TableEntry? {
        val index = (hashValue % MAX_BUCKETS.toUInt()).toInt()
        val bucket = buckets[index]
        return bucket?.getEntry(hashValue)
    }

    /**
     * Stores an entry in the hash array.
     *
     * @param entry The `TableEntry` to store.
     */
    fun storeEntry(entry: TableEntry) {
        val index = (entry.hashValue % MAX_BUCKETS.toUInt()).toInt()
        val bucket = buckets[index]
        if (bucket == null) {
            val newBucket = BucketItem()
            newBucket.storeEntry(entry)
            buckets[index] = newBucket
        } else {
            bucket.storeEntry(entry)
        }
    }

    /**
     * Clears all entries from the hash array.
     */
    fun clear() {
        for (i in buckets.indices) {
            buckets[i]?.clear()
            buckets[i] = null
        }
    }
}

// Global variables for statistics
var newlyAddedEntries = 0
var collisions = 0
var totalEntries = 0
var primaries = 0
var secondaries = 0