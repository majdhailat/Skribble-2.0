import java.io.Serializable;
import java.util.ArrayList;

public class DataPackage implements Serializable {
    private String message;
    private ArrayList<Player> players;
    public DataPackage(String message, ArrayList<Player> players){
        this.message = message;
        this.players = players;
    }

    public String getMessage() {
        return message;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }
}
