package Backend

enum class PlayerToMove{
    PlayerOne, PlayerTwo
}

enum class PlayerTypes{
    HUMAN, AI_ENGINE
}

object Player{
    private var PlayerOne: PlayerTypes? = null
    private var PlayerTwo: PlayerTypes? = null

    private var CurrentPlayer = PlayerToMove.PlayerOne

    fun setPlayers(playerOne: PlayerTypes, playerTwo: PlayerTypes){
        PlayerOne = playerOne
        PlayerTwo = playerTwo
    }

    fun getPlayerToMove(): PlayerToMove {
        return CurrentPlayer
    }

    fun getPlayers(): Pair<PlayerTypes?, PlayerTypes?> {
        return Pair(PlayerOne, PlayerTwo)
    }

    fun getPlayerType(player: PlayerToMove? = null): PlayerTypes? {
        if(player == null){
            return getPlayerType(CurrentPlayer)
        }
        else {
            return (if (player == PlayerToMove.PlayerOne) PlayerOne else PlayerTwo)
        }
    }

    fun switchPlayerToMove(){
        CurrentPlayer = if (CurrentPlayer == PlayerToMove.PlayerOne) PlayerToMove.PlayerTwo else PlayerToMove.PlayerOne
    }

    fun getOtherPlayer(player: PlayerToMove): PlayerToMove {
        return if(player == PlayerToMove.PlayerOne) PlayerToMove.PlayerTwo else PlayerToMove.PlayerOne
    }
}

