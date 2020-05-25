import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
//Stores all information that the client will need about the game.
//An object of this class is constantly created in the server output thread with new info and sent to the clients
//input thread

//variables comments available in the server
public class DataPackage implements Serializable {
    private static final long serialVersionUID = 69420;
    private int timeRemaining;

    private ArrayList<Player> players;
    private Player myPlayer;//the player that is receiving the package

    private String magicWord;
    private DrawingComponent[] drawingComponents;

    private String gameStatus;
    public static final String WAITINGTOSTART = "waiting", BETWEENROUND = "between round", ROUNDINPROGRESS = "in progress";

    private int totalNumOfRounds, roundsLeft;

    public DataPackage(String gameStatus, int timeRemaining, int totalNumOfRounds, int roundsLeft, ArrayList<Player>players, Player myPlayer, DrawingComponent[] drawingComponents, String magicWord){
        this.gameStatus = gameStatus;
        this.timeRemaining = timeRemaining;
        this.players = players;
        this.myPlayer = myPlayer;
        this.drawingComponents = drawingComponents;
        this.totalNumOfRounds = totalNumOfRounds;
        this.roundsLeft = roundsLeft;
        this.magicWord = magicWord;
    }

    public int getTimeRemaining(){return timeRemaining;}

    public synchronized ArrayList<Player> getPlayers() {return players;}

    public Player getMyPlayer() {return myPlayer;}

    public DrawingComponent[] getDrawingComponents() {return drawingComponents;}

    public String getGameStatus() {
        return gameStatus;
    }

    public int getTotalNumOfRounds() {
        return totalNumOfRounds;
    }

    public int getRoundsLeft() {
        return roundsLeft;
    }

    public String getMagicWord(){
        return magicWord;
    }
}
