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
//        System.out.println(this.getName()+"   tryint to updates score");
        if (this.isArtist || winners.contains(this)) {
//            System.out.println(this.getName()+"   updates score");
            this.score += pointsGainedLastRound;
        }
        pointsGainedLastRound = 0;
        secondsTakenToGuessWordLastRound = 0;
        placeLastRound = 0;
    }

    public static void clearWinners(){
        winners.clear();
    }

    public void calculatePoints(int wordLen, int secondsLeft) {
        //time it took to guess
        //the order you guessed in
        //length of the word only matters if <5 letters
        winners.add(this);
        placeLastRound = winners.size();
        double points;
        double exponent;
        if (this != artist) {
            exponent = Math.max(1, 1.3 - (placeLastRound * 0.1));
            points = Math.ceil(Math.pow(secondsLeft, exponent));
            pointsGainedLastRound = (int) points;
        } else {
            if (winners.size() == 1) { //the artist is added to the list of winners at the beginning of the round
                pointsGainedLastRound = 0;
            } else {
                exponent = 1 + wordLen/15.0;
                points = Math.ceil(Math.pow(secondsLeft/2.0, exponent));
                pointsGainedLastRound = (int) points;
            }
        }
    }
}
