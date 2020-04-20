import java.io.Serializable;
import java.util.ArrayList;
//a class that stores all information from the server in order to be distributed to all clients
public class DataPackage implements Serializable {
    private static final long serialVersionUID = 69420;
    public static final String WAITINGFORPLAYERS = "waiting for players", ROUNDINPROGRESS = "round in progress", GAMESTARTING = "game starting";
    private String message;
    private Player myPlayer;//the player that is receiving the package
    private ArrayList<Player> players;
    //add a list of winners? i think there might have been one before
    private ArrayList<Player> winners;
    private Player artist;
    private String gameState;

    private DrawingComponent[] drawingComponents;
    public DataPackage(String message, ArrayList<Player> players, ArrayList<Player> winners, Player myPlayer, Player artist, DrawingComponent[] drawingComponents, String gameState){
        this.message = message;
        this.players = players;
        this.winners = winners;
        this.myPlayer = myPlayer;
        this.artist = artist;
        this.drawingComponents = drawingComponents;
        this.gameState = gameState;
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

    public DrawingComponent[] getDrawingComponents() {
        return drawingComponents;
    }

    public String getGameState(){return gameState;}


    public boolean amIArtist(){
        return (myPlayer == artist);
    }
}
