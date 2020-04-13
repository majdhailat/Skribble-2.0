import java.io.Serializable;
import java.util.ArrayList;
//a class that stores all information from the server in order to be distributed to all clients
public class DataPackage implements Serializable {
    private String message;
    private Player myPlayer;//the player that is receiving the package
    private ArrayList<Player> players;
    //add a list of winners? i think there might have been one before
    private ArrayList<Player> winners;
    private Player artist;
    public DataPackage(String message, ArrayList<Player> players, ArrayList<Player> winners, Player myPlayer, Player artist){
        this.message = message;
        this.players = players;
        this.winners = winners;
        this.myPlayer = myPlayer;
        this.artist = artist;
    }

    public String getMessage() {
        return message;
    }

    public synchronized ArrayList<Player> getPlayers() {
        return players;
    }

    public ArrayList<Player> getWinners(){
        return winners;
    }

    public Player getMyPlayer() {
        return myPlayer;
    }

    public Player getArtist(){
        return artist;
    }
}
