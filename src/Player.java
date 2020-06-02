//imports
import java.io.Serializable;
import java.util.ArrayList;

/*
Stores all info about a player
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 6942069;

    private static Player artist;//the current artist (the player that gets to draw)
    private static ArrayList<Player> winners = new ArrayList<>();//the list of winners for this round
    private static ArrayList<Player> previousArtists = new ArrayList<>();//the list of players that got to be artists this game

    private boolean gotUserName = false;//if the players name was inputted
    private String name = "";//the players username
    private int score = 0;//the players score
    private boolean isArtist = false;//if this player is the artist
    private ArrayList<String> messages = new ArrayList<>();//the list of messages that this player can see
    private int iconImageNumber = Client.randint(1, 6);//the number that dictates which of the 6 icon images is used
    private int uniqueID = Client.randint(1, 10000);

    private int pointsGainedLastRound;//the amount of points this player got (resets when the new round starts)
    private int placeLastRound;//the pos of the player (resets when the new round starts)

    //===============================Static Methods===================================

    /*
    returns the current artist
     */
    public static Player getArtist(){return artist;}

    /*
    removes artist
     */
    public static void nullifyArtist(){
        if (artist != null){
            artist.isArtist = false;
            artist = null;
        }
    }

    /*
    selects new artist
     */
    public static Player chooseAndSetArtist(ArrayList<Player> players){
        while (true) {
            for (Player p : players) {
                if (!previousArtists.contains(p)) {//checking if player was not artist
                    if (artist != null) {//checking if there is already a set artist
                        artist.isArtist = false;
                    }
                    artist = p;//setting artist
                    artist.isArtist = true;
                    previousArtists.add(artist);
                    return artist;
                }
            }
            previousArtists.clear();//clearing if all players where the artist
        }
    }

    /*
    removes all players from previous artists list
     */
    public static void clearPreviousArtists(){previousArtists.clear();}

    /*
    returns list of winners
     */
    public static ArrayList<Player> getWinners(){return winners;}

    //===========================Non Static Methods===================================

    /*
    returns got user name
     */
    public boolean gotUserName(){return gotUserName;}

    /*
    sets the players name
     */
    public void setName(String name){
        gotUserName = true;
        this.name = name;
    }

    /*
    returns players user name
     */
    public String getName() {return name;}

    /*
    returns players score
     */
    public int getScore(){return score;}

    /*
    returns is artist
     */
    public boolean isArtist(){return isArtist;}

    /*
    adds a message to the messages list
     */
    public void addMessage(String msg){messages.add(msg);}

    /*
    returns messages list
     */
    public ArrayList<String> getMessages() {return messages;}

    /*
    returns icon image number
     */
    public int getIconImageNumber(){return iconImageNumber;}

    public int getUniqueID(){return uniqueID;}

    /*
    returns points gained last round
     */
    public int getPointsGainedLastRound(){return pointsGainedLastRound;}

    /*
    updates the players score using the points gained last round
     */
    public void updateScore(){
        if (this.isArtist || winners.contains(this)) {
            this.score += pointsGainedLastRound;
        }
        pointsGainedLastRound = 0;
        placeLastRound = 0;
    }

    public void resetScore(){
        this.score = 0;
    }

    /*
    clears list of winners
     */
    public static void clearWinners(){winners.clear();}

    /*

     */
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
