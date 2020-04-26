//Imports
import javax.swing.Timer;
import javax.xml.crypto.Data;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.TimeUnit;

//Stores all the global information about the game and starts the listening thread
public class Server {
    private boolean running = true;//if the server is running
    private String gameStatus = DataPackage.WAITINGTOSTART;

    private int roundLength = 90;
    private int timeRemaining = roundLength;//the time remaining in the round

    private ArrayList<Player> players = new ArrayList<>();//the players playing
    private Map<Player, Integer> winners = new HashMap<>();//this dictionary store all players who guessed the correct word
    //for the round as well as the length of time it took them to guess the word

    private DrawingComponent[] drawingComponents;//the list of all individual pieces that make up the
    //drawing. It is iterated through in the GUI of the client and each component in the array is drawn onto the canvas.
    private Player artist;//the current artist that has access to drawing
    private ArrayList<Player> previousArtists = new ArrayList<>();//the players that have already been the artists
    //this is reset when all players have had a turn

    private String currentMagicWord;//the word that the artist is responsible for drawing
    private int lengthOfMagicWord;
    private ArrayList<String> magicWords = new ArrayList<>();//all possible magic words loaded from the txt file

    public static void main(String[] args){
        Server server = new Server();
        new ListenForClients(server).start();
    }

    public boolean isRunning(){return running;}

    public String getGameStatus(){return gameStatus;}

    //sets up for a brand new game of x rounds (not determined yet)
    public void newGame(){
        System.out.println("game started");
        try {
            loadMagicWords("words");
        } catch (IOException e) {
            e.printStackTrace();
        }
        previousArtists.clear();
        newRound();
    }

    //cleans up variables from the last round and starts a new round
    public void endRound(){
        gameStatus = DataPackage.BETWEENROUND;
        gameTimer.stop();
        drawingComponents = null;
        calculateAndUpdatePoints();
        artist = null;
        winners.clear();
        try {
            TimeUnit.MILLISECONDS.sleep(5000);
        } catch (InterruptedException e) {e.printStackTrace();}
        newRound();
    }

    public void calculateAndUpdatePoints(){
        Player[] winnersArray = this.winners.keySet().toArray(new Player[this.winners.keySet().size()]);

        int artistsPoints = 90 * lengthOfMagicWord;
        for (int pos = 0; pos < winnersArray.length; pos++){
            int timeTaken = winners.get(winnersArray[pos]);
            int playersPoints = Math.max((int) ((lengthOfMagicWord*(90 - timeTaken)) / (0.75 * Math.pow(pos + 1, 1.3))), 0);
            winnersArray[pos].setScore(winnersArray[pos].getScore() + playersPoints);
            artistsPoints -= timeTaken * 35/lengthOfMagicWord/Math.pow(pos + 1, 2.5);
        }
        artist.setScore(artist.getScore() + Math.max(artistsPoints, 0));
    }

    //starts the timer, chooses an artist and magic word
    public void newRound(){
        gameStatus = DataPackage.ROUNDINPROGRESS;
        timeRemaining = roundLength;
        gameTimer.start();
        currentMagicWord = magicWords.get(randint(0,magicWords.size()-1));//getting random magic word
        magicWords.remove(currentMagicWord);
        lengthOfMagicWord = currentMagicWord.length();

        if (previousArtists.size() >= players.size()){//checking if all the players have been an artist already
            previousArtists.clear();
        }

        Player randArtist;
        while (true) {
            randArtist = players.get(randint(0, players.size() - 1));//getting random player to be the artist
            if (!previousArtists.contains(randArtist)) {//checking if the player has already been the artist
                artist = randArtist;
                previousArtists.add(randArtist);
                break;
            }
        }

        //alerting the rest of the players of who the new artist is
        for (Player p :players){
            if (p != randArtist){
                p.addMessage("#FF0000~"+randArtist.getName()+" Is the artist");
            }else {
                //alerting artist that he is the artist and what his word is
                p.addMessage("#FF0000~You are the artist");
                p.addMessage("#FF0000~you must draw: " + currentMagicWord);
            }
        }
    }

    //returns players
    public List<Player> getPlayers() {return players;}

    //called after the user enters their username
    public synchronized void playerConnected(Player player) {
        players.add(player);
        player.addMessage("#008000~Welcome to Skribble " + player.getName());//greeting new player
        //alerting the rest of the players of the new player
        for (Player p : players){
            if (p != player){
                p.addMessage("#FF0000~" + player.getName() + " has joined the game");
            }
        }
    }

    //called when a player leaves
    public synchronized void playerDisconnected(Player player){
        players.remove(player);
        //setting the server message rather than adding it to "message only for me" because the disconnecting player has already left so the message is for everyone
        newMessage("#FF0000~" + player.getName() + " has left the game", player);
    }

    public void newMessage(String msg, Player sender){
        String message;
        if (msg.contains("~")){
            message = msg;
        }else{
            if (currentMagicWord != null && msg.toLowerCase().equals(currentMagicWord.toLowerCase())){
                message = ("#FF0000~"+sender.getName() + " has guessed correctly!");
                sender.addMessage("#FF0000~You have guessed correctly!");
                winners.put(sender, roundLength - timeRemaining);
                if(winners.size() == players.size() - 1){//checking if all the players have guessed the correct word (-1 because the artist doesn't count)
                    endRound();
                }
            }
            else{
                message = (sender.getName()) + ": " + msg;
            }
        }
        for (Player p : players){
            if (p != sender){
                p.addMessage(message);
            }
        }
    }

    //sets the servers drawing components
    public synchronized void setDrawingComponents(DrawingComponent[] components) {
        this.drawingComponents = components;
    }

    //returns true if the player is the artist
    public boolean isArtist(Player player){
        return (player == artist);
    }

    //groups all of the global information into 1 object for distribution to all clients
    public synchronized DataPackage getDataPackage(Player player){
        try {
            TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {e.printStackTrace();}
        return new DataPackage(gameStatus, timeRemaining, players, player, drawingComponents, artist, winners);
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
            timeRemaining--;
            if (timeRemaining <= 0){
                endRound();
            }
        }
    }
}

// ------------ Listening Thread ------------------------------------------
//Listens for new clients and creates the I/O threads when a client joins
class ListenForClients extends Thread{
    private Server server;
    public ListenForClients(Server server) {
        this.server = server;
    }

    public void run( ){
        int portNumber = 4445;
        boolean listening = true;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (listening) {
                Socket socket = serverSocket.accept();//waiting for clients to connect
                Player player = new Player();//creating the clients player object
                //creating input and output threads used to communicate with the client
                new OutputThread(server, player, socket).start();
                try {
                    TimeUnit.MILLISECONDS.sleep(5000);
                } catch (InterruptedException e) {e.printStackTrace();}
                new InputThread(server, player, socket).start();
            }
        } catch (IOException e) {
            System.err.println("Port error");
            System.exit(-1);
        }
    }
}

// ------------ I/O TO Client Threads ------------------------------------------
/*
Gets input from client:

If the client is an artist this thread gets the drawing components (the canvas instructions) from the client
and updates it to the server.

If the client is not an artist this thread simply reads messages from the client, these messages are either
text messages typed by the client in the chat box or commands form the client code to do things like start
the game or set the players user name
 */
class InputThread extends Thread {
    private Server server;
    private Player player;
    private Socket socket;
    //This stream is used for both the drawing components array and the string text messages
    private ObjectInputStream in;
    private boolean gotUserName = false;//if the player has input their user name

    public InputThread(Server server, Player player, Socket socket) throws IOException {
        this.socket = socket;
        this.server = server;
        this.player = player;
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void run() {
        try {
            while (server.isRunning()) {
                //ARTIST MODE
                if (server.isArtist(player)){
                    try {
                        //WAITING for the client to send a drawing component array, then reading it from the client
                        DrawingComponent[] shapesArray = (DrawingComponent[]) in.readObject();
                        server.setDrawingComponents(shapesArray);//setting the drawing components in the server
                    }catch(ClassCastException ignored){}
                    //this exception WILL happen when the user goes from artist to non artist because the in stream
                    //will be waiting for a drawing component array and then will receive a string and try to cast
                    //the string to a drawing component array. The string that causes this exception will
                    //not reach the server
                } else {
                    //NON ARTIST MODE
                    String inputLine;
                    try {
                        //WAITING for the client to send a string then reading that string
                        if ((inputLine = (String) in.readObject()) != null) {
                            if (!gotUserName) {//checking if user name has not been obtained
                                player.setName(inputLine);//setting user name
                                server.playerConnected(player);//alerting server of new player
                                gotUserName = true;
                                //checking if the user triggered the game to start
                            } else if (server.getGameStatus().equals(DataPackage.WAITINGTOSTART )&& inputLine.equals("/START") && server.getPlayers().get(0) == player) {
                                server.newGame();//starting new game
                            } else {//just a message - not a command
                                server.newMessage(inputLine, player);
                            }
                        }
                    }catch (SocketException | ClassCastException ignored){}
                    //this exception is here for the same exact reason as the class cast exception above except
                    //now a drawing component array is trying to be casted to a string
                }
            }
        } catch(IOException | ClassNotFoundException e){e.printStackTrace();}
        finally{
            server.playerDisconnected(player);//disconnecting player
            try {
                socket.close();//closing socket
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}

/*
sends output to client:
all this class does is get a data package from the server containing all the info the client will need and then
sends that package to the client
 */
class OutputThread extends Thread{
    private Server server;
    private Player player;
    private Socket socket;
    private ObjectOutputStream objectOutputStream;//this stream is used to send the data package object
    public OutputThread(Server server, Player player, Socket socket) throws IOException {
        this.socket = socket;
        this.server = server;
        this.player = player;
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
    }

    public void run(){
        try {
            while (server.isRunning()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {e.printStackTrace();}
                DataPackage dataPackage = server.getDataPackage(player);//getting data package
                objectOutputStream.writeUnshared(dataPackage);//sending data package
                objectOutputStream.flush();
                //must reset output stream otherwise certain items in the data package might not get update
                objectOutputStream.reset();
            }
        }
        catch (IOException e) {e.printStackTrace();}
        finally {
            server.playerDisconnected(player);//disconnecting player
            try {
                socket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}
