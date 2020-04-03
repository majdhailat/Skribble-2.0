import java.io.Serializable;
import java.util.ArrayList;

public class DataPackage implements Serializable {
    private String message;
    private Player myPlayer;
    private ArrayList<Player> players = new ArrayList<>();
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
