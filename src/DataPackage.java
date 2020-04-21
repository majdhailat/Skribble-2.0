import java.io.Serializable;
import java.util.ArrayList;
//Stores all information that the client will need about the game.
//An object of this class is constantly created in the server output thread with new info and sent to the clients
//input thread

//variables comments available in the server
public class DataPackage implements Serializable {
    private static final long serialVersionUID = 69420;
    private int timeRemaining;

    private ArrayList<Player> players;
    private Player myPlayer;//the player that is receiving the package
    private String message;

    private DrawingComponent[] drawingComponents;
    private Player artist;
    private ArrayList<Player> winners;

    public DataPackage(int timeRemaining, ArrayList<Player>players, Player myPlayer, String message, DrawingComponent[] drawingComponents, Player artist, ArrayList<Player> winners){
        this.timeRemaining = timeRemaining;
        this.players = players;
        this.myPlayer = myPlayer;
        this.message = message;
        this.drawingComponents = drawingComponents;
        this.artist = artist;
        this.winners = winners;
    }

    public int getTimeRemaining(){return timeRemaining;}

    public synchronized ArrayList<Player> getPlayers() {return players;}

    public Player getMyPlayer() {return myPlayer;}

    public String getMessage() {return message;}

    public DrawingComponent[] getDrawingComponents() {return drawingComponents;}

    public boolean amIArtist(){return (myPlayer == artist);}

    public ArrayList<Player> getWinners(){return winners;}
}
