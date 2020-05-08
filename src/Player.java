import java.io.Serializable;
import java.util.ArrayList;

//stores information about each player
public class Player implements Serializable {
    private static final long serialVersionUID = 6942069;

    private static int numOfPlayers = 0;
    private static Player artist;
    private static ArrayList<Player> winners = new ArrayList<>();
    private static ArrayList<Player> previousArtists = new ArrayList<>();

    private boolean gotUserName = false;
    private String name = "";
    private int score = 0;
    private boolean isArtist = false;
    private ArrayList<String> messages = new ArrayList<>();

    private int pointsGainedLastRound;
    private int secondsTakenToGuessWordLastRound;
    private int placeLastRound;

    //===============================Static Methods===================================

    public static void incrementNumOfPlayer(){numOfPlayers ++;}

    public static void decrementNumOfPlayers(){numOfPlayers --;}

    public static Player getArtist(){return artist;}

    public static void nullifyArtist(){
        if (artist != null){
            artist.isArtist = false;
            artist = null;
        }
    }

    public static Player chooseAndSetArtist(ArrayList<Player> players){
        while (true) {
            for (Player p : players) {
                if (!previousArtists.contains(p)) {
                    if (artist != null) {
                        artist.isArtist = false;
                    }
                    artist = p;
                    artist.isArtist = true;
                    previousArtists.add(artist);
                    return artist;
                }
            }
            previousArtists.clear();
        }
    }

    public static void clearPreviousArtists(){previousArtists.clear();}

    public static ArrayList<Player> getWinners(){return winners;}

    //===========================Non Static Methods===================================

    public boolean gotUserName(){
        return gotUserName;
    }

    public void setName(String name){
        gotUserName = true;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getScore(){return score;}

    public boolean isArtist(){return isArtist;}

    public void addMessage(String msg){messages.add(msg);}

    public ArrayList<String> getMessages() {return messages;}

    public int getPointsGainedLastRound(){return pointsGainedLastRound;}

    public void updateScore(){
        System.out.println(this.name+": "+this.pointsGainedLastRound);
        System.out.println("artists: "+artist.name);
        if (artist == this || winners.contains(this)) {
            this.score += pointsGainedLastRound;
        }
        pointsGainedLastRound = 0;
        secondsTakenToGuessWordLastRound = 0;
        placeLastRound = 0;
        winners.clear();
    }

    public void calculatePoints(int wordLen, int secondsPassed){
        secondsTakenToGuessWordLastRound = secondsPassed;
        winners.add(this);
        placeLastRound = winners.size();
        if (this != artist) {
            double points = 50 * Math.pow(wordLen, 1 / 1.5);
            points -= (1.3 * secondsTakenToGuessWordLastRound);
            points *= Math.pow(numOfPlayers - placeLastRound, 1 + ((double) numOfPlayers / 10));
            pointsGainedLastRound = (Math.max((int) points, 0));
        }else{
            if (winners.size() == 0){
                pointsGainedLastRound = 0;
            }else{
                double points = 50 * Math.pow(wordLen, 1/1.5);
                for (Player p : winners){
                    points -= Math.pow((1.3 * p.secondsTakenToGuessWordLastRound), 1/(double)p.placeLastRound);
                }
                pointsGainedLastRound = (Math.max((int) points, 0));
            }
        }
    }
}
