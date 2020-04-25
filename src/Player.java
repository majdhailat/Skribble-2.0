import java.io.Serializable;
import java.awt.Color;
import java.util.ArrayList;

//stores information about each player
public class Player implements Serializable {
    private static final long serialVersionUID = 6942069;

    private ArrayList<String> messages = new ArrayList<>();//the list of the players messages
    private String name;//the user name of the player selected by the user
    private int score;
    //this is the color used when displaying the box around the players name and stats in each users GUI
    //(purely aesthetic)
    private Color color;

    private int pointsGainedLastRound;

    public Player(){
        this.name = "";
        this.score = 0;
        this.pointsGainedLastRound = 0;
        //getting random color that is not too dark
        this.color = new Color(50 + (int)(Math.random() * ((255 - 50) + 1)), 50 + (int)(Math.random() * ((255 - 50) + 1)),  50 + (int)(Math.random() * ((255 - 50) + 1)));
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Color getColor(){
        return color;
    }

    public ArrayList<String> getMessages() {
        return this.messages;
    }

    public void addMessage(String msg){
        messages.add(msg);
    }

    public int getScore(){
        return score;
    }

    public void setScore(int score){
        pointsGainedLastRound = this.score - score;
        this.score = score;
    }
}
