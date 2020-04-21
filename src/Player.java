import java.io.Serializable;
import java.awt.Color;
import java.util.ArrayList;

//stores information about each player
public class Player implements Serializable {
    private static final long serialVersionUID = 6942069;

    private ArrayList<String>messagesOnlyForMe = new ArrayList<>();
    private String name;
    private int score;
    private Color color;

    public Player(){
        this.name = "";
        this.score = 0;
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

    public ArrayList<String> getMessageOnlyForMe() {
        return this.messagesOnlyForMe;
    }
    public void addMessageOnlyForMe(String msg){
        messagesOnlyForMe.add(msg);
    }
}
