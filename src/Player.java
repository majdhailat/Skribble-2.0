import java.io.Serializable;

public class Player implements Serializable {
    private String name;
    private int score;
    public Player(String name){
        this.name = name;
        this.score = 0;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
