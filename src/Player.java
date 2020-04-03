import java.io.Serializable;
//stores information about each player
public class Player implements Serializable {
    private boolean connected = true;
    private String name;
    private int score;
    public Player(String name){
        this.name = name;
        this.score = 0;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isConnected(){
        System.out.println("player: "+connected);
        return connected;
    }

    public void setConnected(boolean connected){
        this.connected = connected;
    }

}
