//Imports
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
/*
- Stores all the game data such as:
    - The game status
    - The length of round and # of rounds
    - The list of players as player objects
    - The drawing components array
    - The magic word

- Distributes all this information to clients through the getDataPackage() method which creates a data package containing all
    the necessary info
- Handles any new messages from users OR new drawing component arrays from the artist
    - (Client -> client output thread -> server input thread -> server (here) -> server output thread -> all clients input threads -> all clients
- Handles the game logic (i.e. new round, new game, end round, end game etc) **
- Handles what needs to happen when players join and leave
- Handles user commands (i.e starting game) as well as looks to see when a user guessed the right word
- Loads the magic words file and chooses the word
 */
public class Server {
    public final String orange = "#d35400", blue = "#2980b9", green = "#27ae60", red = "#c0392b";
    private boolean running = true;//if the server is running
    private String gameStatus = DataPackage.WAITINGTOSTART;

    private int roundTimeLength = 90;
    private int timeRemainingInRound = roundTimeLength;//the time remaining in the round

    private int totalNumOfRounds = 2;
    private int roundsLeft = totalNumOfRounds;

    private ArrayList<Player> players = new ArrayList<>();//the players playing
    //for the round as well as the length of time it took them to guess the word

    private DrawingComponent[] drawingComponents;//the list of all individual pieces that make up the
    //drawing. It is iterated through in the GUI of the client and each component in the array is drawn onto the canvas.

    private String currentMagicWord;//the word that the artist is responsible for drawing
    private int charLengthOfMagicWord;
    private ArrayList<String> magicWords = new ArrayList<>();//all possible magic words loaded from the txt file

    public static void main(String[] args){
        Server server = new Server();
        new ListenForClients(server).start();
    }

    public boolean isRunning(){return running;}

    //sets the servers drawing components
    public synchronized void setDrawingComponents(DrawingComponent[] components){
        this.drawingComponents = components;
    }

    //sets up for a brand new game of x rounds (not determined yet)
    public void newGame(){
        try {
            loadMagicWords("word lists/minecraft.txt");
        } catch (IOException e) {e.printStackTrace();}
        Player.clearPreviousArtists();
        newRound();
    }

    //starts the timer, chooses an artist and magic word
    public void newRound(){
        gameStatus = DataPackage.ROUNDINPROGRESS;
        currentMagicWord = magicWords.get(randint(0,magicWords.size()-1));//getting random magic word
        magicWords.remove(currentMagicWord);
        charLengthOfMagicWord = currentMagicWord.length();
        Player artist = Player.chooseAndSetArtist(players);
        //alerting the rest of the players of who the new artist is
        for (Player p : players){
            if (p != artist){
                p.addMessage(blue+"~"+artist.getName()+" Is the artist");
            }else {
                //alerting artist that he is the artist and what his word is
                p.addMessage(blue+"~You are the artist");
                p.addMessage(blue+"~you must draw: " + currentMagicWord);
            }
        }
        gameTimer.start();
    }

    public void endGame(){
        newMessage(red+"~The game has ended");
        roundsLeft = totalNumOfRounds;
        gameStatus = DataPackage.WAITINGTOSTART;
        Player winner = players.get(0);
        for (Player p : players){
            if (p.getScore() > winner.getScore()){
                winner = p;
            }
        }
        winner.addMessage(green+"~You Win!");
        for (Player p : players){
            if (p != winner){
                p.addMessage(green+"~"+winner.getName()+" has won!");
            }
            p.resetScore();
        }
    }

    //cleans up variables from the last round and starts a new round
    public void endRound(){
        gameTimer.stop();
        if (Player.getArtist() != null) {
            Player.getArtist().calculatePoints(charLengthOfMagicWord, timeRemainingInRound);
        }
        timeRemainingInRound = roundTimeLength;
        gameStatus = DataPackage.BETWEENROUND;
        drawingComponents = null;
        currentMagicWord = null;
        try {
            TimeUnit.MILLISECONDS.sleep(5000);
        } catch (InterruptedException e) {e.printStackTrace();}
        for (Player p : players){
            p.updateScore();
        }
        Player.clearWinners();
        Player.nullifyArtist();
        if (roundsLeft == 1){
            endGame();
        }else {
            roundsLeft --;
            newRound();
        }
    }

    //called after the user enters their username
    public synchronized void playerConnected(Player player) {
        players.add(player);
        player.addMessage(orange+"~Welcome to Skribble " + player.getName());//greeting new player
        //alerting the rest of the players of the new player
        for (Player p : players){
            if (p != player){
                p.addMessage(red+"~" + player.getName() + " has joined the game");
            }
        }
    }

    //called when a player leaves
    public synchronized void playerDisconnected(Player player){
        players.remove(player);
        if (players.size() <= 1){
            roundsLeft = 1;
            endRound();
        }
        else if (player.isArtist()){
            endRound();
        }
        //setting the server message rather than adding it to "message only for me" because the disconnecting player has already left so the message is for everyone
        newMessage(red+"~" + player.getName() + " has left the game");
    }

    public void newMessage(String msg, Player sender){
        boolean endRound = false;
        String message = null;
        if (!sender.gotUserName()){
            sender.setName(msg);//setting user name
            playerConnected(sender);//alerting server of new player
        }
        else if(msg.equals("/START") && gameStatus.equals(DataPackage.WAITINGTOSTART) && players.get(0) == sender){
            newGame();
        }
        else if (msg.contains("~")){
            message = msg;
        }else if (currentMagicWord != null && msg.toLowerCase().equals(currentMagicWord.toLowerCase())){
            message = (green+"~"+sender.getName() + " has guessed correctly!");
            sender.addMessage(green+"~You have guessed correctly!");
            sender.calculatePoints(charLengthOfMagicWord, timeRemainingInRound);
            if(Player.getWinners().size() == players.size() - 1){//checking if all the players have guessed the correct word (-1 because the artist doesn't count)
                endRound = true;
            }
        }else{
            message = (sender.getName()) + ": " + msg;
        }

        if (message != null) {
            for (Player p : players) {
                if (p != sender) {
                    p.addMessage(message);
                }
            }
        }
        if (endRound){
            endRound();
        }
    }

    public void newMessage(String message){
        if (message != null){
            for (Player p : players){
                p.addMessage(message);
            }
        }
    }

    //groups all of the global information into 1 object for distribution to all clients
    public synchronized DataPackage getDataPackage(Player player){
        try {
            TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {e.printStackTrace();}
        return new DataPackage(gameStatus, timeRemainingInRound, totalNumOfRounds, roundsLeft, players, player, drawingComponents, currentMagicWord);
    }

    //loads magic words from txt file and stores them in magic words array
    public void loadMagicWords(String filename) throws IOException{
        Scanner inFile = new Scanner(new BufferedReader(new FileReader(filename)));
        int numWords = inFile.nextInt();
        inFile.nextLine();
        for(int i = 0; i < numWords; i++){
            magicWords.add(inFile.nextLine());
        }
        inFile.close();
    }

    //returns a random int between the low and high args inclusive
    public static int randint(int low, int high){
        return (int)(Math.random()*(high-low+1)+low);
    }

    //a timer that calls the triggers the action performed ** every second **, is used to control the round timer
    private Timer gameTimer = new Timer(1000, new TickListener());
    //triggered every second; subtracts 1 from time remaining. checks if timer ran out -> ends the round
    class TickListener implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            timeRemainingInRound--;
            if (timeRemainingInRound <= 0){
                endRound();
            }
        }
    }
}