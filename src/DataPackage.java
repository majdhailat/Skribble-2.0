import java.io.Serializable;
import java.util.ArrayList;
//a class that stores all information from the server in order to be distributed to all clients
public class DataPackage implements Serializable {
    private String message;
    private Player myPlayer;//the player that is receiving the package
    private ArrayList<Player> players;
    public DataPackage(String message, ArrayList<Player> players, Player myPlayer){
        this.message = message;
        this.players = players;
        this.myPlayer = myPlayer;
    }

    public String getMessage() {
        return message;
    }

    public synchronized ArrayList<Player> getPlayers() {
        return players;
    }

    public Player getMyPlayer() {
        return myPlayer;
    }
}
