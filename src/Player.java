import java.io.Serializable;
import java.awt.Color;
import java.util.ArrayList;

//stores information about each player
public class Player implements Serializable {
    private static final long serialVersionUID = 6942069;

    private volatile static int numOfPlayers;
    private volatile static Player artist;
    private static ArrayList<Player> winners = new ArrayList<>();
    private static ArrayList<Player> previousArtists = new ArrayList<>();

    private String name;
    private int score;
    private ArrayList<String> messages = new ArrayList<>();
    private boolean isArtist;

    private int pointsGainedLastRound;
    private int secondsTakenToGuessWordLastRound;
    private int placeLastRound;

    public Player(){
        this.name = "";
        this.score = 0;
        this.pointsGainedLastRound = 0;
        isArtist = false;
    }

    public synchronized static void incrementNumOfPlayer(){numOfPlayers ++;}

    public static Player chooseAndSetArtist(ArrayList<Player> players){
        if (previousArtists.size() >= numOfPlayers){//checking if all the players have been an artist already
            previousArtists.clear();
        }
        Player randArtist;
        while (true) {
            randArtist = players.get(Server.randint(0, players.size() - 1));//getting random player to be the artist
            if (!previousArtists.contains(randArtist)) {//checking if the player has already been the artist

                if (artist != null) {
                    artist.isArtist = false;
                }
                artist = randArtist;
                artist.isArtist = true;
                previousArtists.add(randArtist);
                return randArtist;
            }
        }
    }

    public static void clearPreviousArtists(){previousArtists.clear();}

    public static Player getArtist(){return artist;}

    public static ArrayList<Player> getWinners(){return winners;}

    //==========================================================================

    public void setName(String name){this.name = name;}

    public String getName() {return name;}

    public int getScore(){return score;}

    public void addMessage(String msg){messages.add(msg);}

    public boolean isArtist(){
        return isArtist;
    }


    public ArrayList<String> getMessages() {return this.messages;}

    //==========================================================================

    public void setPointsGainedDuringRound(int points){pointsGainedLastRound = points;}

    public int getPointsGainedLastRound(){return pointsGainedLastRound;}

    public void updateScore(){
        if (artist == this || winners.contains(this)) {
            this.score += pointsGainedLastRound;
        }
        this.pointsGainedLastRound = 0;
        this.secondsTakenToGuessWordLastRound = 0;
        this.placeLastRound = 0;
        winners.clear();
    }

    public int getSecondsTakenToGuessWordLastRound(){return secondsTakenToGuessWordLastRound;}

    public int getPlaceLastRound(){return placeLastRound;}



    public void calculatePoints(int wordLen, int secondsPassed){
        this.secondsTakenToGuessWordLastRound = secondsPassed;
        winners.add(this);
        placeLastRound = winners.size();
        if (this != artist) {
            double points = 50 * Math.pow(wordLen, 1 / 1.5);
            points -= (1.3 * secondsTakenToGuessWordLastRound);
            points *= Math.pow(numOfPlayers - placeLastRound, 1 + ((double) numOfPlayers / 10)) / 5;
            this.pointsGainedLastRound = (Math.max((int) points, 0));
        }else{
            if (winners.size() == 0){
                this.pointsGainedLastRound = 0;
            }else{
                double points = 50 * Math.pow(wordLen, 1/1.5);
                for (Player p : winners){
                    points -= Math.pow((1.3 * p.getSecondsTakenToGuessWordLastRound()), 1/(double)p.getPlaceLastRound());
                }
                this.pointsGainedLastRound = (Math.max((int) points, 0));
            }
        }
    }
}
