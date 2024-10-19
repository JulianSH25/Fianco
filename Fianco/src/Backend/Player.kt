package Backend

/**
 * Enum representing which player's turn it is to move.
 */
enum class PlayerToMove{
    PlayerOne, PlayerTwo
}

/**
 * Enum representing the types of players available.
 */
enum class PlayerTypes {
    HUMAN,
    AI_ENGINE,
    RANDOM_ENGINE;

    /**
     * Cycles to the next player type.
     *
     * @return The next PlayerType in the enum.
     */
    fun next(): PlayerTypes {
        val values = PlayerTypes.values()
        val nextOrdinal = (this.ordinal + 1) % values.size
        return values[nextOrdinal]
    }
}

/**
 * Singleton object managing player states and turns.
 */
object Player{
    private var PlayerOne: PlayerTypes? = null
    private var PlayerTwo: PlayerTypes? = null

    private var CurrentPlayer = PlayerToMove.PlayerOne

    /**
     * Sets the types for both players.
     *
     * @param playerOne The type for Player One.
     * @param playerTwo The type for Player Two.
     */
    fun setPlayers(playerOne: PlayerTypes, playerTwo: PlayerTypes){
        PlayerOne = playerOne
        PlayerTwo = playerTwo
    }

    /**
     * Gets the player whose turn it is to move.
     *
     * @return The current PlayerToMove.
     */
    fun getPlayerToMove(): PlayerToMove {
        return CurrentPlayer
    }

    /**
     * Gets the types of both players.
     *
     * @return A Pair containing the types of Player One and Player Two.
     */
    fun getPlayers(): Pair<PlayerTypes?, PlayerTypes?> {
        return Pair(PlayerOne, PlayerTwo)
    }

    /**
     * Gets the type of the specified player or the current player if none is specified.
     *
     * @param player The player to get the type for; defaults to the current player.
     * @return The PlayerType of the specified player.
     */
    fun getPlayerType(player: PlayerToMove? = null): PlayerTypes? {
        return if (player == null) {
            getPlayerType(CurrentPlayer)
        } else {
            if (player == PlayerToMove.PlayerOne) PlayerOne else PlayerTwo
        }
    }

    /**
     * Switches the turn to the next player.
     */
    fun switchPlayerToMove(){
        CurrentPlayer = if (CurrentPlayer == PlayerToMove.PlayerOne) PlayerToMove.PlayerTwo else PlayerToMove.PlayerOne
    }

    /**
     * Gets the opponent of the specified player.
     *
     * @param player The player whose opponent is to be found.
     * @return The opponent PlayerToMove.
     */
    fun getOtherPlayer(player: PlayerToMove): PlayerToMove {
        return if(player == PlayerToMove.PlayerOne) PlayerToMove.PlayerTwo else PlayerToMove.PlayerOne
    }

    /**
     * Sets the current player to move.
     *
     * @param move The player to set as the current player.
     */
    fun setPlayerToMove(move: PlayerToMove) {
        CurrentPlayer = move
    }
}

