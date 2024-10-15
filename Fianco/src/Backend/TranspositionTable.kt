package Backend

import java.awt.Point

//val max_buckets = 1024000

var newlyAddedEntries = 0
var collisions = 0
var totalEntries = 0

var primaries = 0
var secondaries = 0

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
enum class ScoreType {
    EXACT, LOWER_BOUND, UPPER_BOUND
}

class TableEntry {
    /**
     * Enum representing the type of score stored.
     *
     * - ACCURATE: An exact score has been calculated.
     * - FAIL_LOW: The score is a lower bound (alpha cut-off).
     * - FAIL_HIGH: The score is an upper bound (beta cut-off).
     */

    /** The Zobrist hash value representing the board position. */
    var hashValue: ULong = 0u

    /** The type of score stored (accurate, fail-low, fail-high). */
    var scoreType: ScoreType = ScoreType.EXACT

    /** The score of the board position evaluated at a certain depth. */
    var score: Int = 0

    /** The best move from this position, as determined by search algorithms. */
    var bestMove: Pair<Point, Point>? = null

    /** The depth of the search at which this score was calculated. */
    var searchDepth: Byte = 0

    var age: Byte = 0
}

class BucketItem {
    /** Primary and Secondary entries in the bucket */
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
            primaries++
            return primaryEntry
        } else if (secondaryEntry?.hashValue == hashValue) {
            secondaries++
            return secondaryEntry
        }
        return null
    }

    /**
     * Adds a new entry to the linked list or replaces an existing entry with the same hash value.
     * TWO-DEEP Replacement SCHEME
     * @param newEntry The new `TableEntry` to add.
     */
    fun storeEntry(newEntry: TableEntry) {
        if (primaryEntry == null) {
            newlyAddedEntries++
            // No primary entry; store here
            primaryEntry = newEntry
        } else if (primaryEntry!!.hashValue == newEntry.hashValue) {
            // Replace existing primary entry with the same hash
            newlyAddedEntries++
            primaryEntry = newEntry
        } else if (newEntry.searchDepth >= primaryEntry!!.searchDepth) {
            // Shift primary to secondary and store new entry as primary
            newlyAddedEntries++
            secondaryEntry = primaryEntry
            primaryEntry = newEntry
        } else if (secondaryEntry == null || secondaryEntry!!.hashValue == newEntry.hashValue) {
            // Store in secondary slot
            newlyAddedEntries++
            secondaryEntry = newEntry
        } else if (newEntry.searchDepth >= secondaryEntry!!.searchDepth) {
            // Replace secondary entry
            newlyAddedEntries++
            secondaryEntry = newEntry
        }
        else{
            collisions++
        }
    }

    /**
     * Clears the linked list starting from this bucket item.
     */
    fun clear() {
        primaryEntry = null
        secondaryEntry = null
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
    fun getEntry(hashValue: ULong): TableEntry? {
        //println("TT Lookup for hash: $hashValue")
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
        const val MAX_BUCKETS: UInt = 536870912u //268435456u //1024000u //1073741824u
    }

    /** The array of buckets representing the transposition table. */
    private val buckets: Array<BucketItem?> = arrayOfNulls(MAX_BUCKETS.toInt())

    /**
     * Retrieves an entry from the transposition table based on the hash value.
     *
     * @param hashValue The Zobrist hash value of the board position.
     * @return The corresponding `TableEntry` if found; otherwise, `null`.
     */
    fun getEntry(hashValue: ULong): TableEntry? {
        //println("hash array Lookup for hash: $hashValue")
        val index = (hashValue % MAX_BUCKETS.toUInt()).toInt()
        //println("hash array Lookup for index: $index")
        val bucket = buckets[index]
        return bucket?.getEntry(hashValue)
    }

    /**
     * Stores an entry in the transposition table.
     *
     * Handles collisions by chaining entries in a linked list within the bucket.
     *
     * @param entry The `TableEntry` to store.
     */
    fun storeEntry(entry: TableEntry) {
        val index = (entry.hashValue % MAX_BUCKETS.toUInt()).toInt()
        val bucket = buckets[index]
        if (bucket == null) {
            // No collision; create a new bucket
            val newBucket = BucketItem()
            newBucket.storeEntry(entry)
            buckets[index] = newBucket
        } else {
            // Use the Two Deep Replacement Scheme to store the entry
            bucket.storeEntry(entry)
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