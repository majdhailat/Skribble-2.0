import java.io.Serializable;
import java.awt.Color;

//stores information about each player
public class Player implements Serializable {
    private static final long serialVersionUID = 6942069;
    private String name;
    private int score;
    private Color color;
    private boolean isArtist;
    public Player(String name){
        this.name = name;
        this.score = 0;
        this.isArtist = false;
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

    public boolean isArtist(){
        return isArtist;
    }

    public void setArtist(boolean artist){
        isArtist = artist;
    }

}
